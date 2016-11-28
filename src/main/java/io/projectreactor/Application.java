package io.projectreactor;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.time.Duration;
import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

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
		mainSpringWebFunctional();
		//or
//		mainReactorNetty();
	}

	static void mainSpringWebFunctional(){
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

	static void mainReactorNetty() {
		HttpServer s = HttpServer.create("0.0.0.0");
		s.newRouter(r -> r.file("/favicon.ico", getFile("static/favicon.ico"))
		                  .get("/docs/api/**", rewrite("/docs/", "/old/"))
		                  .get("/docs/reference/**", rewrite("/docs/", "/old/"))
		                  .get("/docs/raw/**", rewrite("/docs/", "/old/"))
		                  .get("/docs/{dir}/api", rewrite("api", "release"))
		                  .get("/core/docs/reference/**", (req, resp) -> resp.sendRedirect("https://github.com/reactor/reactor-core/blob/master/README.md"))
		                  .get("/ext/docs/api/**/adapter/**", rewrite("/ext/docs/", "/docs/adapter/release/"))
		                  .get("/ipc/docs/api/**", rewrite("/ipc/docs/", "/docs/ipc/release/"))
		                  .get("/ext/docs/api/**/test/**", rewrite("/ext/docs/", "/docs/test/release/"))
		                  .get("/netty/docs/api/**", rewrite("/netty/docs/", "/docs/netty/release/"))
		                  .index((req, res) -> res.sendFile(getFile("static" + req.uri() + "/index.html")))
		                  .directory("/docs", getFile("static/docs/"))
		                  .directory("/assets", getFile("static/assets"))
		)

		 .doOnNext(Application::startLog)
		 .block()
		 .onClose()
		 .block();
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> rewrite(
			String originalPath,
			String newPath) {
		return (req, resp) -> resp.sendRedirect(req.uri().replace(originalPath, newPath));
	}

	static File getFile(String classpath) {
		try {
			return new ClassPathResource(classpath).getFile();
		}
		catch (IOException ioe) {
			throw new IllegalStateException("Cannot link ["+classpath+"] to files from " +
					"classpath", ioe);
		}
	}

	static void startLog(NettyContext c) {
		System.out.printf("Server started in %d ms on: %s\n",
				Duration.ofNanos(ManagementFactory.getThreadMXBean()
				                                  .getThreadCpuTime(Thread.currentThread()
				                                                          .getId()))
				        .toMillis(),
				c.address());
	}

}
