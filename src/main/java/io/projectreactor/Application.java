/*
 * Copyright (c) 2015-2025 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;
import org.reactivestreams.Publisher;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import org.springframework.core.io.ClassPathResource;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

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
		long start = System.currentTimeMillis();
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
		//get at a minimum the list of modules, oldBom, artifacts and groupids from yml
		ModuleUtils.loadModulesFromYmlInto(new ClassPathResource("modules.yml"), modules);
		//then get the versions from Artifactory
		ModuleUtils.fetchVersionsFromArtifactory(modules, "core", "test", "adapter",
				"extra", "netty", "nettyArchive", "kafka", "rabbitmq", "BlockHound",
				"kotlin", "pool");
		//then get the versions from Sonatype
		ModuleUtils.fetchVersionsFromSonatype(modules, "core", "test", "adapter",
				"extra", "netty", "nettyArchive", "kafka", "rabbitmq", "BlockHound",
				"kotlin", "pool");
		LOGGER.info("Boms and modules loaded in " + (System.currentTimeMillis() - start) + "ms");

		docsModel.put("oldBoms", modules.get("olderBoms"));
		String maintenanceDate = System.getProperty("maintenanceDate", System.getenv("maintenanceDate"));
		String maintenanceEnd = System.getProperty("maintenanceEnd", System.getenv("maintenanceEnd"));
		if (maintenanceDate != null && maintenanceEnd != null) {
			boolean addMaintenanceInfo;
			try {
				if (ApplicationUtils.checkMaintenanceIsNotOutdated(maintenanceDate, maintenanceEnd)) {
					addMaintenanceInfo = true;
				}
				else {
					LOGGER.info("Maintenance information set but {} is more than one day in the past, ignored", maintenanceDate);
					addMaintenanceInfo = false;
				}
			}
			catch (DateTimeParseException dateTimeParseException) {
				//the date doesn't seem to be in YYYY/MM/DD format but let's still add it as text
				addMaintenanceInfo = true;
				LOGGER.warn("Maintenance information {} couldn't be parsed as a yyyy/MM/dd date, but still taken into account", maintenanceDate);
			}
			if (addMaintenanceInfo) {
				docsModel.put("maintenanceDate", maintenanceDate);
				docsModel.put("maintenanceEnd", maintenanceEnd);
			}
		}

		//templates will be resolved and parsed below during route setup

		int port = 1025;
		if (System.getenv("PORT") != null){
			port = Integer.parseInt(System.getenv("PORT"));
		}

		context = HttpServer.create()
		                    .host("0.0.0.0")
		                    .port(port)
		                    .route(r -> r.file("/favicon.ico", contentPath.resolve("favicon.ico"))
		                                 .get("/security-policy", template("security-policy"))
		                                 .file("/.well-known/security.txt", contentPath.resolve("well-known/security.txt"))
		                                 //the dot in .well-known is confusing to netty. that said, we're not expected to serve an index for this directory so let's cover the case explicitly
		                                 .get("/.well-known", pageNotFound())
		                                 .get("/", template("home"))
		                                 .get("/docs", template("docs"))
		                                 .get("/learn", template("learn"))
		                                 .get("/support", template("support"))
										 .file("/support.json", contentPath.resolve("support.json"))
		                                 .get("/maintenance", template("maintenance"))
		                                 //.get("/project", template("project"))
		                                 .get("/docs/{module}", this::listVersionsAndDocs)
		                                 .get("/docs/", rewrite("docs/", "docs"))
		                                 .get("/learn/", rewrite("learn/", "learn"))
		                                 .get("/docs/{module}/", (req, resp) -> resp.sendRedirect(req.uri().substring(0, req.uri().length() - 1)))
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
		                                 .get("/2.x/reference/", legacyGone())
		                                 .index(pageNotFound()) //any attempt to list an arbitrary directory is 404
		                                 .directory("/assets", contentPath.resolve("assets"), this::cssInterceptor)
		                                 .get("**", pageNotFound()))
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

	private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> legacyGone() {
		//the template parsing happens at app's initialization
		long start = System.nanoTime();
		final String content = templateEngine.process("410LegacyGone", new Context(Locale.US, docsModel));
		long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		LOGGER.info("Parsed template 410LegacyGone in {}ms", duration);

		return (req, resp) -> resp
				.status(410)
				.sendString(Mono.just(content));
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

	private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> kdocNotFound(
			String moduleAndVersionInfo) {
		String resolvedModule;
		String resolvedVersion;
		try {
			String[] split = moduleAndVersionInfo.split(":");
			resolvedModule = split[0];
			resolvedVersion = split[1];
		}
		catch (Exception e) {
			resolvedModule = "COULDNT_PARSE_MODULE: " + moduleAndVersionInfo;
			resolvedVersion = "COULDNT_PARSE_VERSION: " + moduleAndVersionInfo;
		}

		final Map<String, Object> variables = new HashMap<>();
		variables.put("actualModule", resolvedModule);
		variables.put("actualVersion", resolvedVersion);

		//the template parsing is dynamic to inject the requested url
		return (req, resp) -> {
			variables.put("requestedPage", req.path());
			return resp
					.status(404)
					.sendString(
							Mono.just(templateEngine.process("404NoKDoc",
									new Context(Locale.US, variables)
							))
					);
		};
	}

	//TODO cache the versions?
	private Publisher<Void> listVersionsAndDocs(HttpServerRequest req, HttpServerResponse resp) {
		String requestedModule = req.param("module");
		Module module = modules.get(requestedModule);

		if (module == null){
			return pageNotFound().apply(req, resp);
		}
		String moduleName = module.getName();
		Map<String, Object> model = new HashMap<>();
		LinkedHashMap<String, List<Tuple4<Version, String, String, String>>>
				versionsByMajor = new LinkedHashMap<>();
		Map<String, Tuple4<Version, String, String, String>> latestReleaseByTrain = new HashMap<>();
		Map<String, Tuple4<Version, String, String, String>> latestSnapshotByTrain = new HashMap<>();
		model.put("moduleName", moduleName);
		model.put("artifactId", module.getArtifactId());
		model.put("trains", versionsByMajor);
		model.put("latestReleases", latestReleaseByTrain);
		model.put("latestSnapshots", latestSnapshotByTrain);

		for (Version version : module.versions()) {
			String key = version.major + "." + version.minor + ".x";

			List<Tuple4<Version, String, String, String>> versions = versionsByMajor.computeIfAbsent(key, k -> new ArrayList<>());

			String javadocUrl = "/docs/" + moduleName + "/" + version + "/api";
			String refdocUrl = DocUtils.getRefDocPath(moduleName, version.toString()); //FIXME use the Version
			String kdocUrl = "";
			if (DocUtils.hasKDoc(moduleName, version.toString())) {  //FIXME use the Version
				kdocUrl = "/docs/" + moduleName + "/" + version + "/kdoc-api/";
			}

			Tuple4<Version, String, String, String> docInfo =
					Tuples.of(version, javadocUrl, refdocUrl, kdocUrl);

			//omit snapshot links other than the last snapshot in a release cycle
			if (version.qualifier == Version.Qualifier.SNAPSHOT) {
				if (!latestSnapshotByTrain.containsKey(key)) {
					latestSnapshotByTrain.put(key, docInfo);
					versions.add(docInfo);
				}
			}
			else {
				//for releases, release candidates and milestones, display a link
				versions.add(docInfo);
				//also, keep track of the newest release
				if (version.qualifier == Version.Qualifier.RELEASE && !latestReleaseByTrain.containsKey(key)) {
					latestReleaseByTrain.put(key, docInfo);
				}
			}
		}

		return resp.sendString(
				Mono.just(templateEngine.process("listVersions", new Context(Locale.US, model)))
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
		else if (url.startsWith(DocUtils.WARNING_NO_KDOC)) {
			return kdocNotFound(url.replace(DocUtils.WARNING_NO_KDOC, "")).apply(req, resp);
		}

		return client.headers(h -> ApplicationUtils.filterRepoProxyRequestHeaders(req.requestHeaders()))
		             .get()
		             .uri(url)
		             .response((r, body) -> {
		             	if (r.status().code() == 404) {
			                return pageNotFound().apply(req, resp);
		                }
		                else {
			                resp.headers(ApplicationUtils.filterRepoProxyResponseHeaders(r.responseHeaders()));

			                if (reqUri.endsWith(".svg")) {
				                resp.header(HttpHeaderNames.CONTENT_TYPE,
						                CONTENT_TYPE_IMAGE_SVG);
			                }
			                else if (reqUri.endsWith(".zip")) {
				                resp.header(HttpHeaderNames.CONTENT_TYPE,
						                CONTENT_TYPE_ZIP);
			                }
			                else if (reqUri.endsWith(".js")) {
				                resp.header(HttpHeaderNames.CONTENT_TYPE,
						                CONTENT_TYPE_JS);
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
		String url = "https://repo.spring.io/release"
				+ "/io/projectreactor"
				+ "/" + artefact
				+ "/2.0.8.RELEASE"
				+ "/" + artefact
				+ "-2.0.8.RELEASE-javadoc.jar"
				+ "!/index.html";

		return resp.sendRedirect(url);
	}

	private HttpServerResponse cssInterceptor(HttpServerResponse resp) {
		if (resp.path().endsWith(".css"))
			resp.header("Content-Type", "text/css");
		return resp;
	}

	private void startLog(DisposableServer c) {
		LOGGER.info("Server started in {} ms on: {}\n",
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
		return fs.getPath("static");
	}


	static final AsciiString CONTENT_TYPE_IMAGE_SVG = AsciiString.cached("image/svg+xml");
	static final AsciiString CONTENT_TYPE_ZIP = AsciiString.cached("application/zip");
	static final AsciiString CONTENT_TYPE_JS = AsciiString.cached("application/javascript");
}
