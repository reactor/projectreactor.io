package io.projectreactor;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
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
import ratpack.spring.config.EnableRatpack;

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
	private static final String CURRENT_VERSION = "2.0.3.RELEASE";

	@Bean
	public MarkupTemplateEngine markupTemplateEngine() {
		TemplateConfiguration config = new TemplateConfiguration();
		config.setAutoIndent(true);
		config.setAutoNewLine(true);
		config.setCacheTemplates(false);
		return new MarkupTemplateEngine(config);
	}

	@Bean
	public MarkupTemplateRenderer markupTemplateRenderer() {
		return new MarkupTemplateRenderer(markupTemplateEngine());
	}

	@Bean
	public ClientErrorHandler clientErrorHandler() {
		return (ctx, statusCode) -> {
			LOG.error("client error: {}", statusCode);
		};
	}

	@Bean
	public Action<Chain> ratpack() {
		return chain -> {
			chain.get("", ctx -> {
				Map<String, Object> model = createModel("Home", "home");
				ctx.render(Groovy.groovyMarkupTemplate(model, "templates/index.gtpl"));
			}).get("docs/reference/streams.html", ctx -> {
				ctx.redirect("index.html");
			});
		};
	}

	public static void main(String... args) {
		SpringApplication.run(Application.class, args);
	}

	private static final Map<String, Object> createModel(String title, String type) {
		Map<String, Object> model = new HashMap<>();
		model.put("title", title);
		model.put("type", type);
		model.put("currentVersion", CURRENT_VERSION);
		return model;
	}

}
