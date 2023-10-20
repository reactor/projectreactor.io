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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleTest {

	@Test
	public void addVersionOnInit() {
		Module module = new Module();
		module.addVersion("3.2.1.RELEASE");

		assertThat(module.getVersions()).as("after addVersion")
		                                .isNotNull()
		                                .containsExactly("3.2.1.RELEASE");
	}

	@Test
	public void getVersionOnInit() {
		Module module = new Module();
		assertThat(module.getVersions()).as("getVersions")
		                                .isNotNull()
		                                .isEmpty();
	}

	@Test
	public void sortVersionsOnInit() {
		Module module = new Module();
		module.sortVersions();

		assertThat(module.getVersions()).as("after sortVersions")
		                                .isNotNull()
		                                .isEmpty();
	}

	@Test
	public void sortVersions() {
		Module module = new Module();
		module.addVersion("3.2.1.RELEASE");
		module.addVersion("3.2.1.RELEASE");
		module.addVersion("3.2.1.BUILD-SNAPSHOT");
		module.addVersion("3.3.1.RELEASE");
		module.sortVersions();

		assertThat(module.getVersions()).as("after sortVersions")
		                                .containsExactly("3.3.1.RELEASE", "3.2.1.RELEASE", "3.2.1.RELEASE", "3.2.1.BUILD-SNAPSHOT");
	}

	@Test
	public void sortAndDeduplicateVersionsOnInit() {
		Module module = new Module();
		module.sortAndDeduplicateVersions();

		assertThat(module.getVersions()).as("after sortAndDeduplicateVersions")
		                                .isNotNull()
		                                .isEmpty();
	}

	@Test
	public void sortAndDeduplicateVersions() {
		Module module = new Module();
		module.addVersion("3.2.1.RELEASE");
		module.addVersion("3.2.1.RELEASE");
		module.addVersion("3.2.1.BUILD-SNAPSHOT");
		module.addVersion("3.3.1.RELEASE");

		assertThat(module.getVersions()).hasSize(4);

		module.sortAndDeduplicateVersions();

		assertThat(module.getVersions()).as("after sortAndDeduplicateVersions")
		                                .containsExactly("3.3.1.RELEASE", "3.2.1.RELEASE", "3.2.1.BUILD-SNAPSHOT");
	}

	@Test
	public void loadBadVersionSetFromListDeduplicatesAndReverseSorts() {
		Module module = new Module();
		module.setBadVersions(Arrays.asList("Aluminum-SR3", "Dysprosium-SR25", "Bismuth-SR17", "Dysprosium-SR25", "Californium-SR23"));

		assertThat(module.badVersions).containsExactly("Dysprosium-SR25", "Californium-SR23", "Bismuth-SR17", "Aluminum-SR3");
	}

	@Test
	public void isBadVersion() {
		Module module = new Module();
		module.setBadVersions(Arrays.asList("A", "C"));

		assertThat(module.isBadVersion("A")).as("A").isTrue();
		assertThat(module.isBadVersion("B")).as("B").isFalse();
		assertThat(module.isBadVersion("C")).as("C").isTrue();
	}

}