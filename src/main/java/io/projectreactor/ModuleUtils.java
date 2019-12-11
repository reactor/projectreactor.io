package io.projectreactor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import org.springframework.core.io.ClassPathResource;

import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * @author Simon Baslé
 */
public class ModuleUtils {

	private static final Logger LOGGER = Loggers.getLogger(ModuleUtils.class);

	public static void loadModulesFromYmlInto(ClassPathResource resource, Map<String, Module> target) {
		Yaml yaml = new Yaml(new Constructor(Module.class));
		try {
			yaml.loadAll(resource.getInputStream()).forEach(o -> {
				Module module = (Module)o;
				module.sortAndDeduplicateVersions();
				target.put(module.getName(), module);
			});
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void fetchVersionsFromArtifactory(Map<String, Module> modules, String... moduleNames) {
		final HttpClient client = HttpClient.create().baseUrl("https://repo.spring.io/api/search");
		final String repos = "&repos=snapshot,milestone,release";

		for (String moduleName : moduleNames) {
			Module module = modules.get(moduleName);
			if (module == null) continue;

			final String params = "/versions?g=" + module.getGroupId() + "&a=" + module.getArtifactId() + repos;
			LOGGER.info("Loading version information for {} via GET {}", moduleName, params);

			client.get()
			      .uri(params)
			      .response((r, content) -> {
				      if (r.status().code() < 400) {
					      return content.aggregate()
					                    .asString()
					                    .doOnNext(json -> loadModuleVersionsFromArtifactoryVersionsSearch(json, module));
				      }
				      else {
					      return content.aggregate()
					                    .asString()
					                    .doOnNext(errorBody -> LOGGER.warn("Couldn't scrape versions for {}: {} - {}",
							                    moduleName, r.status(), errorBody));
				      }
			      })
			      .blockLast();

			module.sortAndDeduplicateVersions();
		}
	}

	public static void loadModuleVersionsFromArtifactoryVersionsSearch(String json, Module module) {
		ObjectMapper mapper = new ObjectMapper();
		final JsonNode node;
		try {
			node = mapper.readTree(json);
		}
		catch (JsonProcessingException e) {
			throw Exceptions.propagate(e);
		}
		List<String> versions = node.findValuesAsText("version");
		Set<String> seenGenerations = new CopyOnWriteArraySet<>();

		versions.forEach(v -> tryAddVersion(module, seenGenerations, v));
	}

	/**
	 * Try to add a version to a module, with some exclusions:
	 * <ul>
	 *     <li>only latest snapshot of each generation is added</li>
	 *     <li>versions with custom version on top of GEN.MAJOR.MINOR are ignored</li>
	 *     <li>for core, only consider gen 3.x.y</li>
	 *     <li>ignore version if it is known to be {@link Module#isBadVersion(String) bad}</li>
	 * </ul>
	 *
	 * @implNote this method is extracted for testing purposes
	 *
	 * @param module the target module in which to add versions
	 * @param seenGenerations the set of generations seen
	 * @param version the version to add
	 */
	static void tryAddVersion(Module module, Set<String> seenGenerations, String version) {
		if (module.isBadVersion(version)) return;
		if ("core".equals(module.getName()) && !version.startsWith("3.")) return;
		if (version.split("\\.").length != 4) return;
		if (version.endsWith("BUILD-SNAPSHOT")) {
			String gen = version.replaceFirst("\\.", "_");
			gen = gen.substring(0, gen.indexOf('.'));
			if (seenGenerations.contains(gen)) return;
			seenGenerations.add(gen);
			module.addVersion(version);
		}
		else {
			module.addVersion(version);
		}
	}

}
