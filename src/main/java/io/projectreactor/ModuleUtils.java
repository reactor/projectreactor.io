package io.projectreactor;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import org.springframework.core.io.ClassPathResource;

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

	public static void fetchMavenMetadata(Module... modules) {
		final HttpClient client = HttpClient.create().baseUrl("https://repo.spring.io");

		for (Module module : modules) {
			String path = module.getGroupId().replaceAll("\\.", "/") + "/" + module.getArtifactId() + "/maven-metadata.xml";

			client.get()
			      .uri("/snapshot/" + path)
			      .response((r, content) -> {
				      if (r.status().code() < 400) {
					      return content.aggregate()
					                    .asString()
					                    .doOnNext(mavenMetadata -> loadModuleVersionsFromMavenMetadataInto(mavenMetadata, module, r.path()));
				      }
				      else {
					      return content.aggregate().asString().ignoreElement();
				      }
			      })
			      .blockLast();
			client.get()
			      .uri("/milestone/" + path)
			      .response((r, content) -> {
				      if (r.status().code() < 400) {
					      return content.aggregate()
					                    .asString()
					                    .doOnNext(mavenMetadata -> loadModuleVersionsFromMavenMetadataInto(mavenMetadata, module, r.path()));
				      }
				      else {
					      return content.aggregate().asString().ignoreElement();
				      }
			      })
			      .blockLast();
			client.get()
			      .uri("/release/" + path)
			      .response((r, content) -> {
				      if (r.status().code() < 400) {
					      return content.aggregate()
					                    .asString()
					                    .doOnNext(mavenMetadata -> loadModuleVersionsFromMavenMetadataInto(mavenMetadata, module, r.path()));
				      }
				      else {
					      return content.aggregate().asString().ignoreElement();
				      }
			      })
			      .blockLast();

			module.sortAndDeduplicateVersions();
		}
	}

	public static void loadModuleVersionsFromMavenMetadataInto(String mavenMetadata, Module target, String type) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document doc = dBuilder.parse(new InputSource(new StringReader(mavenMetadata)));
			doc.getDocumentElement().normalize();

			NodeList versions = doc.getElementsByTagName("version");

			for (int temp = 0; temp < versions.getLength(); temp++) {
				Node nNode = versions.item(temp);
				String version = nNode.getTextContent();
				String[] split = version.split("\\.");
				if (split.length == 4) {
					target.addVersion(version);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Unable to load {} versions from maven metadata for {}.{}:\n{}", type, target.getGroupId(), target.getArtifactId(), e);
		}
	}

}
