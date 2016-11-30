package io.projectreactor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

import org.springframework.core.io.ClassPathResource;

/**
 * Main Application for the Project Reactor home site.
 */
public class Application {

	public static void main(String... args) throws Exception {
		Path p = resolveContentPath();

		HttpServer s = HttpServer.create("0.0.0.0");
		s.newRouter(r -> r.file("/favicon.ico", p.resolve("favicon.ico"))
		                  .get("/docs/api/**", rewrite("/docs/", "/old/"))
		                  .get("/docs/reference/**", rewrite("/docs/", "/old/"))
		                  .get("/docs/raw/**", rewrite("/docs/", "/old/"))
		                  .get("/docs/{dir}/api", rewrite("api", "release"))
		                  .get("/core/docs/reference/**", (req, resp) -> resp.sendRedirect("https://github.com/reactor/reactor-core/blob/master/README.md"))
		                  .get("/ext/docs/api/**/adapter/**", rewrite("/ext/docs/", "/docs/adapter/release/"))
		                  .get("/ipc/docs/api/**", rewrite("/ipc/docs/", "/docs/ipc/release/"))
		                  .get("/ext/docs/api/**/test/**", rewrite("/ext/docs/", "/docs/test/release/"))
		                  .get("/netty/docs/api/**", rewrite("/netty/docs/", "/docs/netty/release/"))
		                  .index((req, res) -> res.sendFile(p.resolve(res.path())
		                                                     .resolve("index.html")))
		                  .directory("/docs", p.resolve("docs"))
		                  .directory("/assets", p.resolve("assets"))
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

	static void startLog(NettyContext c) {
		System.out.printf("Server started in %d ms on: %s\n",
				Duration.ofNanos(ManagementFactory.getThreadMXBean()
				                                  .getThreadCpuTime(Thread.currentThread()
				                                                          .getId()))
				        .toMillis(),
				c.address());
	}

	static Path resolveContentPath() throws Exception {
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
