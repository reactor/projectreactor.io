package io.projectreactor;

import io.projectreactor.ratpack.EnableRatpack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.error.ClientErrorHandler;
import ratpack.func.Action;
import ratpack.handling.Chain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Main Application for the Project Reactor home site.
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableRatpack
public class Application {

	private static final Logger LOG             = LoggerFactory.getLogger(Application.class);

	@Bean
	public ClientErrorHandler clientErrorHandler() {
		return (ctx, statusCode) -> {
			if(statusCode == 404) {
				ctx.redirect("/404.html");
			}
			else{
				LOG.error("client error: {}", statusCode);
			}
		};
	}

	@Bean
	public Action<Chain> ratpack() {
		return chain -> {
			chain.prefix("docs/api", c -> c.handler(h -> h.redirect(h.getRequest().getUri().replace("/docs/", "/old/")
			)))
			     .prefix("docs/reference", c -> c.handler(h -> h.redirect(h.getRequest().getUri().replace("/docs/",
					     "/old/")
			)))
			     .prefix("docs/raw", c -> c.handler(h -> h.redirect(h.getRequest().getUri().replace("/docs/", "/old/")
			)));
		};
	}

	public static void main(String... args) {
		SpringApplication.run(Application.class, args);
	}

}
