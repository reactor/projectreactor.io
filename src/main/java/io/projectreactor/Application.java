/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import org.reactivestreams.Publisher;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.function.Tuple2;

import org.springframework.core.io.ClassPathResource;

/**
 * Main Application for the Project Reactor home site.
 */
public final class Application {

	private final Map<String, Module> modules     = new HashMap<>();
	private final HttpClient          client      = HttpClient.create();
	private final Path                contentPath = resolveContentPath();

	private final Mono<? extends DisposableServer> context;
	private final TemplateEngine templateEngine;
	private final Map<String, Object> docsModel = new HashMap<>();

	private static final Logger LOGGER = Loggers.getLogger(Application.class);

	Application() throws IOException {
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("/static/templates/");
		templateResolver.setSuffix(".html");
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);

		//evaluate the boms.yml file first and add it to thymeleaf's model
		Yaml bomYaml = new Yaml(new Constructor(Bom.class));
		bomYaml.loadAll(new ClassPathResource("boms.yml").getInputStream())
		       .forEach(o -> {
			       Bom bom = (Bom) o;
			       docsModel.put(bom.getType(), bom);
		       });
		//evaluate modules, add oldboms to thymeleaf's model
		Yaml yaml = new Yaml(new Constructor(Module.class));
		yaml.loadAll(new ClassPathResource("modules.yml").getInputStream()).forEach(o -> {
			Module module = (Module)o;
			modules.put(module.getName(), module);
		});
		docsModel.put("oldBoms", modules.get("olderBoms"));
		//templates will be resolved and parsed below during route setup

		context = HttpServer.create()
		                    .host("0.0.0.0")
		                    .port(8080)
		                    .route(r -> r.file("/favicon.ico", contentPath.resolve("favicon.ico"))
		                                 .get("/", template("home"))
		                                 .get("/docs", template("docs"))
		                                 .get("/learn", template("learn"))
		                                 //.get("/project", template("project"))
		                                 .get("/docs/{module}/{version}/api", rewrite("/api", "/api/index.html"))
		                                 .get("/docs/{module}/{version}/reference/docs/**", rewrite("/reference/docs/", "/reference/"))
		                                 .get("/docs/{module}/{version}/reference", rewrite("/reference", "/reference/index.html"))
		                                 .get("/docs/{module}/{version}/api/**", this::repoProxy)
		                                 .get("/docs/{module}/{version}/reference/**", this::repoProxy)
		                                 //TODO this is a hack due to the dokka css being imported as `../style.css` in the html
		                                 .get("/docs/{module}/{version}/style.css",  rewrite("/style.css", "/kdoc-api/style.css"))
		                                 .get("/docs/{module}/{version}/kdoc-api",  rewrite("/kdoc-api", "/kdoc-api/index.html"))
		                                 .get("/docs/{module}/{version}/kdoc-api/**", this::repoProxy)
		                                 .get("/core/docs/reference/**", (req, resp) -> resp.sendRedirect("https://github.com/reactor/reactor-core/blob/master/README.md"))
		                                 .get("/ext/docs/api/**/adapter/**", rewrite("/ext/docs/", "/docs/adapter/release/"))
		                                 .get("/ipc/docs/api/**", rewrite("/ipc/docs/", "/docs/ipc/release/"))
		                                 .get("/ext/docs/api/**/test/**", rewrite("/ext/docs/", "/docs/test/release/"))
		                                 .get("/netty/docs/api/**", rewrite("/netty/docs/", "/docs/netty/release/"))
		                                 .get("/2.x/{module}/api", this::legacyProxy)
		                                 .get("/2.x/reference/", (req, res) -> res.sendFile(contentPath.resolve("legacy/ref/index.html")))
//		                                 .index((req, res) -> res.sendFile(contentPath.resolve(res.path()).resolve("index.html")))
		                                 .directory("/old", contentPath.resolve("legacy"))
		                                 .directory("/docs", contentPath.resolve("docs"))
		                                 .directory("/assets", contentPath.resolve("assets"), this::cssInterceptor)
		                                 .get("**.html", pageNotFound()))
		                    .bind();
	}

	public static void main(String... args) throws Exception {
		Application app = new Application();
		app.startAndAwait();
	}

	public void startAndAwait() {
		context.doOnNext(this::startLog)
		 .block()
		 .onDispose()
		 .block();
	}

	private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> rewrite(
			String originalPath, String newPath) {
		return (req, resp) -> resp.sendRedirect(req.uri().replace(originalPath, newPath));
	}

	private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> template(
			String templateName) {

		//the template parsing happens at app's initialization
		long start = System.nanoTime();
		final String content = templateEngine.process(templateName, new Context(Locale.US, docsModel));
		long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		LOGGER.info("Parsed template {} in {}ms", templateName, duration);

		return (req, resp) -> resp.sendString(Mono.just(content));
	}

	private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> pageNotFound() {

		//the template parsing is dynamic to inject the requested url
		return (req, resp) -> resp
				.status(404)
			    .sendString(
			    		Mono.just(templateEngine.process("404",
							    new Context(Locale.US, Collections.singletonMap("requestedPage", req.path()))
					    ))
			    );
	}

	private Publisher<Void> repoProxy(HttpServerRequest req, HttpServerResponse resp) {
		String requestedModule = req.param("module");
		String requestedVersion = req.param("version");

		String reqUri = req.uri();
		String versionType = DocUtils.findVersionType(requestedVersion);

		Tuple2<Module, String> module = DocUtils.findModuleAndVersion(modules, requestedModule, requestedVersion);
		if (module == null) {
			return pageNotFound().apply(req, resp);
		}

		String url = DocUtils.moduleToUrl(reqUri, versionType,
				requestedModule, requestedVersion,
				module.getT1(), module.getT2());
		if (url == null) {
			return pageNotFound().apply(req, resp);
		}

		return client.headers(h -> filterXHeaders(req.requestHeaders()))
		             .get()
		             .uri(url)
		             .response((r, body) -> {
		             	if (r.status().code() == 404) {
			                return pageNotFound().apply(req, resp);
		                }
		                else {
			                resp.headers(r.responseHeaders());

			                if (reqUri.endsWith(".svg")) {
				               resp.header(HttpHeaderNames.CONTENT_TYPE,
						               CONTENT_TYPE_IMAGE_SVG);
			                }

		             		return resp.status(r.status())
			                           .send(body.retain())
			                           .then();
		                }
		             });
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

	private HttpServerResponse cssInterceptor(HttpServerResponse resp) {
		if (resp.path().endsWith(".css"))
			resp.header("Content-Type", "text/css");
		return resp;
	}

	private void startLog(DisposableServer c) {
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


	static final AsciiString CONTENT_TYPE_IMAGE_SVG = AsciiString.cached("image/svg+xml");
}
