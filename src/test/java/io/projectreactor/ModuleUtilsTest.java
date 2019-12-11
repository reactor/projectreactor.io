package io.projectreactor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleUtilsTest {

	@Test
	public void loadModuleVersionsFromJson() throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				ModuleUtilsTest.class.getResourceAsStream("/core-versions.json")))) {
			String json = reader.lines()
			                       .collect(Collectors.joining());
			Module module = new Module("core", "io.projectreactor", "reactor-core");
			module.badVersions.add("3.3.0.M2");

			assertThat(module.getVersions()).as("initially empty").isEmpty();

			ModuleUtils.loadModuleVersionsFromArtifactoryVersionsSearch(json, module);

			assertThat(module.getVersions())
					.as("gen 3.3")
					.contains("3.3.2.BUILD-SNAPSHOT")
					.contains("3.3.1.RELEASE")
					.contains("3.3.0.RELEASE")
					.doesNotContain("3.3.1.customVersion.BUILD-SNAPSHOT") //excluded custom
					.doesNotContain("3.3.1.BUILD-SNAPSHOT")
					.doesNotContain("3.3.0.BUILD-SNAPSHOT")
					.contains("3.3.0.RC1")
					.contains("3.3.0.M3")
					.doesNotContain("3.3.0.M2") //excluded badVersion
					.contains("3.3.0.M1");

			assertThat(module.getVersions())
					.as("gen 3.2")
					.contains("3.2.14.BUILD-SNAPSHOT")
					.contains("3.2.13.RELEASE")
					.contains("3.2.12.RELEASE")
					.doesNotContain("3.2.13.BUILD-SNAPSHOT");

			assertThat(module.getVersions())
					.as("gen 3.1")
					.contains("3.1.17.BUILD-SNAPSHOT")
					.contains("3.1.16.RELEASE")
					.contains("3.1.15.RELEASE")
					.doesNotContain("3.1.16.BUILD-SNAPSHOT");

			assertThat(module.getVersions())
					.as("gen 3.0")
					.contains("3.0.8.BUILD-SNAPSHOT")
					.contains("3.0.7.RELEASE")
					.doesNotContain("3.0.7.BUILD-SNAPSHOT");

			assertThat(module.getVersions())
					.as("gen < 3 fully excluded")
					.doesNotContain("2.5.0.BUILD-SNAPSHOT")
					.doesNotContain("2.0.8.RELEASE")
					.doesNotContain("2.0.8.BUILD-SNAPSHOT");
		}
	}

	@Test
	public void genLessThanThreeIsNotExcludedIfNotCore() {
		Module module = new Module("test", "", "");
		ModuleUtils.tryAddVersion(module, new HashSet<>(), "2.5.3.RELEASE");

		assertThat(module.getVersions()).hasSize(1);
	}

	@Test
	public void genLessThanThreeIsExcludedIfCore() {
		Module module = new Module("core", "", "");
		ModuleUtils.tryAddVersion(module, new HashSet<>(), "2.5.3.RELEASE");

		assertThat(module.getVersions()).isEmpty();
	}
}