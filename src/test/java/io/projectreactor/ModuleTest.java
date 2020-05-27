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
	public void loadBadVersionSetFromList() {
		Module module = new Module();
		module.setBadVersions(Arrays.asList("A", "B", "B"));

		assertThat(module.badVersions).containsExactly("A", "B");
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