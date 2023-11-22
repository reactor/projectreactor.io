/*
 * Copyright (c) 2019-2023 VMware LLC or its affiliates, All Rights Reserved.
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
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaders;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import org.springframework.core.io.ClassPathResource;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
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

		Flux.fromArray(moduleNames)
		    .filter(modules::containsKey)
		    .map(modules::get)
		    .flatMap(module -> {
			    final String params = "/versions?g=" + module.getGroupId() + "&a=" + module.getArtifactId() + repos;
			    LOGGER.info("Loading version information for {} via GET {}", module.getName(), params);

			    return client.get()
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
							                               module.getName(), r.status(), errorBody));
				                 }
			                 })
			                 .then(Mono.fromCallable(module::sortAndDeduplicateVersions));
		    })
		    .blockLast();
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

		versions.forEach(v -> tryAddVersion(module, v));
	}

	public static void fetchVersionsFromSonatype(Map<String, Module> modules, String... moduleNames) {
		final HttpClient client = HttpClient.create()
		                                    .baseUrl("https://s01.oss.sonatype.org/service/local/lucene")
		                                    .headers(h -> h.set("accept", "application/json"));

		Flux.fromArray(moduleNames)
		    .filter(modules::containsKey)
		    .map(modules::get)
		    .flatMap(module -> {
			    final String params = "/search?g=" + module.getGroupId() + "&a=" + module.getArtifactId();
			    LOGGER.info("Loading version information for {} via GET {}", module.getName(), params);

			    return client.get()
			                 .uri(params)
			                 .response((r, content) -> {
				                 if (r.status().code() < 400) {
					                 return content.aggregate()
					                               .asString()
					                               .doOnNext(json -> loadModuleVersionsFromSonatypeVersionsSearch(json, module));
				                 }
				                 else {
					                 return content.aggregate()
					                               .asString()
					                               .doOnNext(errorBody -> LOGGER.warn("Couldn't scrape versions for {}: {} - {}",
							                               module.getName(), r.status(), errorBody));
				                 }
			                 })
			                 .then(Mono.fromCallable(module::sortAndDeduplicateVersions));
		    })
		    .blockLast();
	}

	public static void loadModuleVersionsFromSonatypeVersionsSearch(String json, Module module) {
		ObjectMapper mapper = new ObjectMapper();
		final JsonNode node;
		try {
			node = mapper.readTree(json);
		}
		catch (JsonProcessingException e) {
			throw Exceptions.propagate(e);
		}

		JsonNode data = node.findValue("data");
		List<String> versions = data.findValuesAsText("version");

		versions.forEach(v -> tryAddVersion(module, v));
	}

	/**
	 * Try to add a version to a module, with some exclusions:
	 * <ul>
	 *     <li>versions with custom version on top of GEN.MAJOR.MINOR are ignored</li>
	 *     <li>for core, only consider gen 3.x.y</li>
	 *     <li>ignore version if it is known to be {@link Module#isBadVersion(String) bad}</li>
	 * </ul>
	 *
	 * @implNote this method is extracted for testing purposes
	 *
	 * @param module the target module in which to add versions
	 * @param versionLiteral the version to add
	 */
	static void tryAddVersion(Module module, String versionLiteral) {
		if (module.isBadVersion(versionLiteral)) return;

		try {
			Version version = Version.parse(versionLiteral);

			//only consider core gen 3+
			if ("core".equals(module.getName()) && version.major < 3) return;
			//don't show custom snapshots
			if (version.customQualifier != null) return;

			module.addVersion(version);
		}
		catch (IllegalArgumentException e) {
			LOGGER.warn("Unable to parse and add version {} for module {}: {}", versionLiteral, module.name, e.toString());
		}
	}

}
