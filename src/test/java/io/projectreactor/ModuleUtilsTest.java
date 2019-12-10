package io.projectreactor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleUtilsTest {

	@Test
	public void loadModuleVersionsFromMavenMetadata() throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				ModuleUtilsTest.class.getResourceAsStream("/maven-metadata.xml")))) {
			String content = reader.lines()
			                       .collect(Collectors.joining());
			Module module = new Module("name", "groupId", "artifactId");

			ModuleUtils.loadModuleVersionsFromMavenMetadataInto(content,
					module,
					"typeOrUri");

			assertThat(module.getVersions())
					.contains("3.2.1.BUILD-SNAPSHOT") //contains classic versions
					.doesNotContain("3.3.0.boundedElastic.BUILD-SNAPSHOT"); //eliminates customVersions
		}
	}
}