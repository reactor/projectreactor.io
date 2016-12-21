package io.projectreactor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaders;
import org.reactivestreams.Publisher;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;
import reactor.ipc.netty.resources.PoolResources;

import org.springframework.core.io.ClassPathResource;

/**
 * Main Application for the Project Reactor home site.
 */
public final class Application {

	private final Map<String, Module> modules     = new HashMap<>();
	private final HttpServer          server      = HttpServer.create("0.0.0.0");
	private final HttpClient          client      = HttpClient.create(opts -> opts.poolResources(PoolResources.elastic("proxy")));
	private final Path                contentPath = resolveContentPath();

	private final Mono<? extends NettyContext> context;


	Application() throws IOException {
		context = server.newRouter(r -> r.file("/favicon.ico", contentPath.resolve("favicon.ico"))
		                                 .get("/docs/{dir}/api", rewrite("api", "release"))
		                                 .get("/docs/{module}/{version}/api", rewrite("/api", "/api/index.html"))
		                                 .get("/docs/{module}/{version}/reference", rewrite("/reference", "/reference/docs/index.html"))
		                                 .get("/docs/{module}/{version}/reference/", rewrite("/reference/", "/reference/docs/index.html"))
		                                 .get("/docs/{module}/release/api/**", this::repoProxy)
		                                 .get("/docs/{module}/milestone/api/**", this::repoProxy)
		                                 .get("/docs/{module}/milestone/reference/**", this::repoProxy)
		                                 .get("/docs/{module}/snapshot/api/**", this::repoProxy)
		                                 .get("/docs/{module}/snapshot/reference/**", this::repoProxy)
		                                 .get("/core/docs/reference/**", (req, resp) -> resp.sendRedirect("https://github.com/reactor/reactor-core/blob/master/README.md"))
		                                 .get("/ext/docs/api/**/adapter/**", rewrite("/ext/docs/", "/docs/adapter/release/"))
		                                 .get("/ipc/docs/api/**", rewrite("/ipc/docs/", "/docs/ipc/release/"))
		                                 .get("/ext/docs/api/**/test/**", rewrite("/ext/docs/", "/docs/test/release/"))
		                                 .get("/netty/docs/api/**", rewrite("/netty/docs/", "/docs/netty/release/"))
		                                 .index((req, res) -> res.sendFile(contentPath.resolve(res.path()).resolve("index.html")))
		                                 .directory("/docs", contentPath.resolve("docs"))
		                                 .directory("/assets", contentPath.resolve("assets"))

		);
		Yaml yaml = new Yaml(new Constructor(Module.class));
		yaml.loadAll(new ClassPathResource("modules.yml").getInputStream()).forEach(o -> {
			Module module = (Module)o;
			modules.put(module.getName(), module);
		});

	}

	public static void main(String... args) throws Exception {
		Application app = new Application();
		app.startAndAwait();
	}

	public void startAndAwait() {
		context.doOnNext(this::startLog)
		 .block()
		 .onClose()
		 .block();
	}

	private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> rewrite(
			String originalPath, String newPath) {
		return (req, resp) -> resp.sendRedirect(req.uri().replace(originalPath, newPath));
	}

	private Publisher<Void> repoProxy(HttpServerRequest req, HttpServerResponse resp) {
		String name = req.param("module");

		String path = req.path();

		boolean isJavadoc = path.contains("/api/") || path.endsWith("/api");

		Module module = modules.get(name);

		if(module == null){
			return resp.sendNotFound();
		}

		String versionType = path.contains("/snapshot") ? "snapshot" :
				(path.contains("/milestone") ? "milestone" : "release");

		String version = module.getVersions()
		                       .stream()
		                       .filter(v -> versionType.equals("milestone") || v.endsWith(
				                       versionType.toUpperCase()))
		                       .findFirst().orElseGet((() -> "NA"));

		if(version.equals("NA")){
			return resp.sendNotFound();
		}

		int offset = isJavadoc ? 12 : 18;
		String file = req.uri()
		                 .substring(offset + versionType.length() + name.length());
		if (file.isEmpty()) {
			file = isJavadoc ? "index.html" : "docs/index.html";
		}
		String suffix = isJavadoc ? "-javadoc.jar" : ".zip";
		String artifactSuffix = isJavadoc ? "" : "-docs";
		String url = "http://repo.spring.io/" + versionType
				+ "/" + module.getGroupId().replace(".", "/")
				+ "/" + module.getArtifactId() + artifactSuffix
				+ "/" + version
				+ "/" + module.getArtifactId() + artifactSuffix
				+ "-" + version + suffix
				+ "!/" + file;

		return client.get(url, r -> r.failOnClientError(false)
		                             .headers(filterXHeaders(req.requestHeaders())))
		             .then(r -> resp.headers(r.responseHeaders())
		                            .status(r.status())
		                            .send(r.receive()
		                                   .retain())
		                            .then());
	}

	private HttpHeaders filterXHeaders(HttpHeaders headers){
		Iterator<Map.Entry<String, String>> it = headers.iteratorAsString();
		Map.Entry<String, String> current;
		while(it.hasNext()){
			current = it.next();
			if(current.getKey().startsWith("Cf-")){
				headers.remove(current.getKey());
			}
		}
		return headers;
	}

	private void startLog(NettyContext c) {
		System.out.printf("Server started in %d ms on: %s\n",
				Duration.ofNanos(ManagementFactory.getThreadMXBean()
						.getThreadCpuTime(Thread.currentThread().getId())).toMillis(), c.address());
	}

	private Path resolveContentPath() throws IOException {
		ClassPathResource cp = new ClassPathResource("static");
		if (cp.isFile()) {
			return Paths.get(cp.getURI());
		}
		FileSystem fs = FileSystems.newFileSystem(cp.getURI(), Collections.emptyMap());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try{
				fs.close();
			}
			catch (IOException io){
				//ignore
			}
		}));
		return fs.getPath("BOOT-INF/classes/static");
	}

}
