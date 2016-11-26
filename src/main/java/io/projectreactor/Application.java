package io.projectreactor;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.time.Duration;

import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.BodyInserters;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.RouterFunction;
import org.springframework.web.reactive.function.RouterFunctions;

import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.web.reactive.function.RequestPredicates.GET;
import static org.springframework.web.reactive.function.RouterFunctions.route;
import static org.springframework.web.reactive.function.ServerResponse.ok;
import static org.springframework.web.reactive.function.ServerResponse.status;

/**
 * Main Application for the Project Reactor home site.
 */
public class Application {

	public static void main(String... args) throws InterruptedException {
		HttpHandler httpHandler = RouterFunctions.toHttpHandler(routes());

		HttpServer.create("0.0.0.0")
		          .newHandler(new ReactorHttpHandlerAdapter(httpHandler))
		          .doOnNext(Application::startLog)
		          .block()
		          .onClose()
		          .block();
	}

	private static RouterFunction<?> routes() {

		return route(GET("/docs/api/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/docs/", "/old/"))).build()
			).andRoute(GET("/docs/reference/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/docs/", "/old/"))).build()
			).andRoute(GET("/docs/raw/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/docs/", "/old/"))).build()
			).andRoute(GET("/docs/{dir}/api"), request ->
				status(FOUND).location(URI.create(request.path().replace("api", "release"))).build()
			).andRoute(GET("/core/docs/reference/**"), request ->
				status(FOUND).location(URI.create("https://github.com/reactor/reactor-core/blob/master/README.md")).build()
			).andRoute(GET("/core/docs/api/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/core/docs/","/docs/core/release/"))).build()
			).andRoute(GET("/netty/docs/api/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/netty/docs/","/docs/netty/release/"))).build()
			).andRoute(GET("/ipc/docs/api/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/ipc/docs/", "/docs/ipc/release/"))).build()
			).andRoute(GET("/ext/docs/api/**/test/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/ext/docs/", "/docs/test/release/"))).build()
			).andRoute(GET("/ext/docs/api/**/adapter/**"), request ->
				status(FOUND).location(URI.create(request.path().replace("/ext/docs/", "/docs/adapter/release/"))).build()
			).andRoute(GET("/"), request ->
				ok().body(BodyInserters.fromResource(new ClassPathResource("static/index.html")))
			).andRoute(GET("/docs"), request ->
				ok().body(BodyInserters.fromResource(new ClassPathResource("static/docs/index.html")))
			).andRoute(GET("/{dir}/"), request ->
				ok().body(BodyInserters.fromResource(new ClassPathResource("static/" + request.pathVariable("dir") + "/index.html")))
			).andRoute(GET("/{file}"), request ->
				ok().body(BodyInserters.fromResource(new ClassPathResource("static/" + request.pathVariable("file"))))
			)
			// TODO remove that with a more flexible path matching or when SPR-14913 will be fixed
			.andRoute(GET("/{dir}/{file}"), request -> {
					Resource resource = new ClassPathResource("static/" + request.pathVariable("dir") + "/" + request.pathVariable("file"));
					return ok().body(BodyInserters.fromResource(resource));
				}
			).andRoute(GET("/{dir1}/{dir2}/"), request ->
				ok().body(BodyInserters.fromResource(new ClassPathResource("static/" + request.pathVariable("dir1") + "/" + request.pathVariable("dir2")  + "/index.html")))
			).andRoute(GET("/{dir1}/{dir2}/{file}"), request -> {
					Resource resource = new ClassPathResource("static/" + request.pathVariable("dir1")
							+ "/" + request.pathVariable("dir2")
							+ "/" + request.pathVariable("file"));
					return ok().body(BodyInserters.fromResource(resource));
				}
			).andRoute(GET("/{dir1}/{dir2}/{dir3}/"), request ->
				ok().body(BodyInserters.fromResource(new ClassPathResource("static/" + request.pathVariable("dir1") + "/" + request.pathVariable("dir2") + "/" + request.pathVariable("dir3") + "/index.html")))
			).andRoute(GET("/{dir1}/{dir2}/{dir3}/{file}"), request -> {
					Resource resource = new ClassPathResource("static/" + request.pathVariable("dir1")
							+ "/" + request.pathVariable("dir2")
							+ "/" + request.pathVariable("dir3")
							+ "/" + request.pathVariable("file"));
					return ok().body(BodyInserters.fromResource(resource));
				}
			).andRoute(GET("/{dir1}/{dir2}/{dir3}/{dir4}/"), request -> {
					Resource resource = new ClassPathResource("static/" + request.pathVariable("dir1")
							+ "/" + request.pathVariable("dir2")
							+ "/" + request.pathVariable("dir3")
							+ "/" + request.pathVariable("dir4")
							+ "/index.html");
					return ok().body(BodyInserters.fromResource(resource));
				}
			).andRoute(GET("/{dir1}/{dir2}/{dir3}/{dir4}/{file}"), request -> {
					Resource resource = new ClassPathResource("static/" + request.pathVariable("dir1")
									+ "/" + request.pathVariable("dir2")
									+ "/" + request.pathVariable("dir3")
									+ "/" + request.pathVariable("dir4")
									+ "/" + request.pathVariable("file"));
					return ok().body(BodyInserters.fromResource(resource));
				}
			).andRoute(GET("/{dir1}/{dir2}/{dir3}/{dir4}/{dir5}/{file}"), request -> {
					Resource resource = new ClassPathResource("static/" + request.pathVariable("dir1")
							+ "/" + request.pathVariable("dir2")
							+ "/" + request.pathVariable("dir3")
							+ "/" + request.pathVariable("dir4")
							+ "/" + request.pathVariable("dir5")
							+ "/" + request.pathVariable("file"));
					return ok().body(BodyInserters.fromResource(resource));
				}
			);
	}

	static void startLog(NettyContext c) {
		System.out.printf("Server started in %d ms on: %s",
				Duration.ofNanos(ManagementFactory.getThreadMXBean()
				                                  .getThreadCpuTime(Thread.currentThread()
				                                                          .getId()))
				        .toMillis(),
				c.address());
	}

	static void mainReactor() {
		File docsAdapter, docsIpc, docsCore, docsTest, docsNetty, assetsDir, docsIndex,
				index, notFound, favicon;

		try {
			docsAdapter = new ClassPathResource("/static/docs").getFile();
			docsIpc = new ClassPathResource("/static/docs").getFile();
			docsCore = new ClassPathResource("/static/docs").getFile();
			docsTest = new ClassPathResource("/static/docs").getFile();
			docsNetty = new ClassPathResource("/static/docs").getFile();
			assetsDir = new ClassPathResource("/static/assets").getFile();
			docsIndex = new ClassPathResource("/static/docs/index.html").getFile();
			index = new ClassPathResource("/static/index.html").getFile();
			notFound = new ClassPathResource("/static/404.html").getFile();
			favicon = new ClassPathResource("/static/favicon.ico").getFile();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}
		HttpServer.create("0.0.0.0")
		          .newRouter(r -> r.file("/", index)
		                           .file("/favicon.ico", favicon)
		                           .file("/docs", docsIndex)
		                           .directory("/docs/adapter", docsAdapter)
		                           .directory("/docs/ipc", docsIpc)
		                           .directory("/docs/core", docsCore)
		                           .directory("/docs/test", docsTest)
		                           .directory("/docs/netty", docsNetty)
		                           .directory("/assets", assetsDir))
		          .doOnNext(Application::startLog)
		          .block()
		          .onClose()
		          .block();
	}

}
