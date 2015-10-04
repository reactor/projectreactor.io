package io.projectreactor;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import io.projectreactor.ratpack.EnableRatpack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ratpack.error.ClientErrorHandler;
import ratpack.func.Action;
import ratpack.groovy.Groovy;
import ratpack.groovy.template.internal.MarkupTemplateRenderer;
import ratpack.handling.Chain;

import java.util.HashMap;
import java.util.Map;

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
			LOG.error("client error: {}", statusCode);
		};
	}

	@Bean
	public Action<Chain> ratpack() {
		return chain -> {
			chain.get("docs/reference/streams.html", ctx -> {
				ctx.redirect("index.html");
			});
		};
	}

	public static void main(String... args) {
		SpringApplication.run(Application.class, args);
	}

}
