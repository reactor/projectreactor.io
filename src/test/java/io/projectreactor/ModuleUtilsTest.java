/*
 * Copyright (c) 2019-2021 VMware LLC or its affiliates, All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
			module.badVersions.add("3.4.0-RC1");

			assertThat(module.getVersions()).as("initially empty").isEmpty();

			ModuleUtils.loadModuleVersionsFromArtifactoryVersionsSearch(json, module);

			assertThat(module.getVersions())
					.as("gen 3.4")
					.contains("3.4.0-SNAPSHOT")
					.doesNotContain("3.4.0-SNAPSHOT-verifFix123") //excluded custom
					.contains("3.4.0-RC2")
					.doesNotContain("3.4.0-RC1") //excluded badVersion
					.contains("3.4.0-M1");

			assertThat(module.getVersions())
					.as("gen 3.3")
					.contains("3.3.2.BUILD-SNAPSHOT")
					.contains("3.3.1.RELEASE")
					.contains("3.3.0.RELEASE")
					.doesNotContain("3.3.1.customVersion.BUILD-SNAPSHOT") //excluded custom
					.contains("3.3.1.BUILD-SNAPSHOT")
					.contains("3.3.0.BUILD-SNAPSHOT")
					.contains("3.3.0.RC1")
					.contains("3.3.0.M3")
					.doesNotContain("3.3.0.M2") //excluded badVersion
					.contains("3.3.0.M1");

			assertThat(module.getVersions())
					.as("gen 3.2")
					.contains("3.2.14.BUILD-SNAPSHOT")
					.contains("3.2.13.RELEASE")
					.contains("3.2.13.BUILD-SNAPSHOT")
					.contains("3.2.12.RELEASE");

			assertThat(module.getVersions())
					.as("gen 3.1")
					.contains("3.1.17.BUILD-SNAPSHOT")
					.contains("3.1.16.RELEASE")
					.contains("3.1.16.BUILD-SNAPSHOT")
					.contains("3.1.15.RELEASE");

			assertThat(module.getVersions())
					.as("gen 3.0")
					.contains("3.0.8.BUILD-SNAPSHOT")
					.contains("3.0.7.RELEASE")
					.contains("3.0.7.BUILD-SNAPSHOT");

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
		ModuleUtils.tryAddVersion(module, "2.5.3.RELEASE");

		assertThat(module.getVersions()).hasSize(1);
	}

	@Test
	public void genLessThanThreeIsExcludedIfCore() {
		Module module = new Module("core", "", "");
		ModuleUtils.tryAddVersion(module, "2.5.3.RELEASE");

		assertThat(module.getVersions()).isEmpty();
	}

	@Test
	public void recognizesOldSnapshotScheme() {
		Module module = new Module("core", "", "");
		ModuleUtils.tryAddVersion(module, "3.3.0.BUILD-SNAPSHOT");

		assertThat(module.getVersions()).containsExactly("3.3.0.BUILD-SNAPSHOT");
	}

	@Test
	public void recognizesNewSnapshotScheme() {
		Module module = new Module("core", "", "");
		ModuleUtils.tryAddVersion(module, "3.4.0-SNAPSHOT");

		assertThat(module.getVersions()).containsExactly("3.4.0-SNAPSHOT");
	}

	@Test
	public void recognizesNewMilestoneScheme() {
		Module module = new Module("core", "", "");
		ModuleUtils.tryAddVersion(module, "3.4.0-M1");

		assertThat(module.getVersions()).containsExactly("3.4.0-M1");
	}

	@Test
	public void recognizesNewReleaseCandidateScheme() {
		Module module = new Module("core", "", "");
		ModuleUtils.tryAddVersion(module, "3.4.0-RC1");

		assertThat(module.getVersions()).containsExactly("3.4.0-RC1");
	}

	@Test
	public void recognizesNewReleaseScheme() {
		Module module = new Module("core", "", "");
		ModuleUtils.tryAddVersion(module, "3.4.0");

		assertThat(module.getVersions()).containsExactly("3.4.0");
	}
}