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
import reactor.util.function.Tuple2;

import org.springframework.core.io.ClassPathResource;

/**
 * Main Application for the Project Reactor home site.
 */
public final class Application {

	private final Map<String, Module> modules     = new HashMap<>();
	private final HttpServer          server      = HttpServer.create("0.0.0.0", 8080);
	private final HttpClient          client      = HttpClient.create(opts -> opts.poolResources(PoolResources.elastic("proxy")));
	private final Path                contentPath = resolveContentPath();

	private final Mono<? extends NettyContext> context;


	Application() throws IOException {
		context = server.newRouter(r -> r.file("/favicon.ico", contentPath.resolve("favicon.ico"))
		                                 .get("/docs/{module}/{version}/api", rewrite("/api", "/api/index.html"))
		                                 .get("/docs/{module}/{version}/reference", rewrite("/reference", "/reference/docs/index.html"))
		                                 .get("/docs/{module}/{version}/api/**", this::repoProxy)
		                                 .get("/docs/{module}/{version}/reference/**", this::repoProxy)
		                                 .get("/core/docs/reference/**", (req, resp) -> resp.sendRedirect("https://github.com/reactor/reactor-core/blob/master/README.md"))
		                                 .get("/ext/docs/api/**/adapter/**", rewrite("/ext/docs/", "/docs/adapter/release/"))
		                                 .get("/ipc/docs/api/**", rewrite("/ipc/docs/", "/docs/ipc/release/"))
		                                 .get("/ext/docs/api/**/test/**", rewrite("/ext/docs/", "/docs/test/release/"))
		                                 .get("/netty/docs/api/**", rewrite("/netty/docs/", "/docs/netty/release/"))
		                                 .get("/learn", (req, res) -> res.sendFile(contentPath.resolve("learn.html")))
		                                 .get("/2.x/{module}/api", this::legacyProxy)
		                                 .get("/2.x/reference/", (req, res) -> res.sendFile(contentPath.resolve("legacy/ref/index.html")))
		                                 //.get("/project", (req, res) -> res.sendFile(contentPath.resolve("project.html")))
		                                 .index((req, res) -> res.sendFile(contentPath.resolve(res.path()).resolve("index.html")))
		                                 .directory("/old", contentPath.resolve("legacy"))
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
		String requestedModule = req.param("module");
		String requestedVersion = req.param("version");

		String reqUri = req.uri();
		String versionType = DocUtils.findVersionType(requestedVersion);

		Tuple2<Module, String> module = DocUtils.findModuleAndVersion(modules, requestedModule, requestedVersion);
		if (module == null) {
			return resp.sendNotFound();
		}

		String url = DocUtils.moduleToUrl(reqUri, versionType,
				requestedModule, requestedVersion,
				module.getT1(), module.getT2());
		if (url == null) {
			return resp.sendNotFound();
		}

		return client.get(url, r -> r.failOnClientError(false)
		                             .headers(filterXHeaders(req.requestHeaders()))
		                             .sendHeaders())
		             .flatMap(r -> resp.headers(r.responseHeaders())
		                            .status(r.status())
		                            .send(r.receive()
		                                   .retain())
		                               .then());
	}

	private Publisher<Void> legacyProxy(HttpServerRequest req,
			HttpServerResponse resp) {
		String artefact = req.param("module");
		String url = "http://repo.spring.io/release"
				+ "/io/projectreactor"
				+ "/" + artefact
				+ "/2.0.8.RELEASE"
				+ "/" + artefact
				+ "-2.0.8.RELEASE-javadoc.jar"
				+ "!/index.html";

		return resp.sendRedirect(url);
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
