package io.projectreactor;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleTest {

	@Test
	public void compareNonVersionsAreAppendedAlphabetically() {
		List<String> versions = Arrays.asList("1.2.3", "example", "1.21.3.4.5.6", "examples", "1.2.3.BUILD-SNAPSHOT");
		versions.sort(Module.VERSION_COMPARATOR);

		assertThat(versions).containsExactly("1.2.3.BUILD-SNAPSHOT", "1.2.3", "1.21.3.4.5.6", "example", "examples");
	}

	@Test
	public void compareQualifier() {
		List<String> versions = Arrays.asList("1.2.3.BUILD-SNAPSHOT", "1.2.3.M3", "1.2.3.RC1", "1.2.3.RELEASE");
		versions.sort(Module.VERSION_COMPARATOR);

		assertThat(versions).containsExactly("1.2.3.RELEASE", "1.2.3.RC1", "1.2.3.M3", "1.2.3.BUILD-SNAPSHOT");
	}

	@Test
	public void compareDifferentPatches() {
		List<String> versions = Arrays.asList("1.2.0.RELEASE", "1.2.1.RELEASE", "1.2.2.RELEASE");
		versions.sort(Module.VERSION_COMPARATOR);

		assertThat(versions).containsExactly("1.2.2.RELEASE", "1.2.1.RELEASE", "1.2.0.RELEASE");
	}

	@Test
	public void compareDifferentMinors() {
		List<String> versions = Arrays.asList("1.2.0.RELEASE", "1.3.0.RELEASE", "1.1.0.RELEASE");
		versions.sort(Module.VERSION_COMPARATOR);

		assertThat(versions).containsExactly("1.3.0.RELEASE", "1.2.0.RELEASE", "1.1.0.RELEASE");
	}

	@Test
	public void compareDifferentMajors() {
		List<String> versions = Arrays.asList("1.2.0.RELEASE", "2.2.0.RELEASE", "3.2.0.RELEASE");
		versions.sort(Module.VERSION_COMPARATOR);

		assertThat(versions).containsExactly("3.2.0.RELEASE", "2.2.0.RELEASE", "1.2.0.RELEASE");
	}

	@Test
	public void compareCustomVersionSameQualifier() {
		List<String> versions = Arrays.asList("1.2.3.customVersionA.BUILD-SNAPSHOT", "1.2.3.BUILD-SNAPSHOT", "1.2.3.customVersionB.BUILD-SNAPSHOT");
		versions.sort(Module.VERSION_COMPARATOR);

		assertThat(versions).containsExactly("1.2.3.BUILD-SNAPSHOT", "1.2.3.customVersionA.BUILD-SNAPSHOT", "1.2.3.customVersionB.BUILD-SNAPSHOT");
	}

	@Test
	public void matchesReleaseVersion() {
		assertThat("1.2.3.RELEASE").matches(Module.VERSION_REGEXP);
	}

	@Test
	public void matchesReleaseCandidateVersion() {
		assertThat("1.2.3.RC11").matches(Module.VERSION_REGEXP);
	}

	@Test
	public void matchesMilestoneVersion() {
		assertThat("1.2.3.M23").matches(Module.VERSION_REGEXP);
	}

	@Test
	public void matchesSnapshotVersion() {
		assertThat("1.2.3.BUILD-SNAPSHOT").matches(Module.VERSION_REGEXP);
	}

	@Test
	public void matchesCustomSnapshotVersions() {
		assertThat("1.2.3.01.BUILD-SNAPSHOT").as("numeric only").matches(Module.VERSION_REGEXP);
		assertThat("1.2.3.0cusTom0.BUILD-SNAPSHOT").as("alphanumeric").matches(Module.VERSION_REGEXP);
		assertThat("1.2.3.cusTom.BUILD-SNAPSHOT").as("alphabetic only").matches(Module.VERSION_REGEXP);
	}

	@Test
	public void matchesCustomReleaseVersions() {
		assertThat("1.2.3.01.RELEASE").as("numeric only").matches(Module.VERSION_REGEXP);
		assertThat("1.2.3.0cusTom0.RELEASE").as("alphanumeric").matches(Module.VERSION_REGEXP);
		assertThat("1.2.3.cusTom.RELEASE").as("alphabetic only").matches(Module.VERSION_REGEXP);
	}

	@Test
	public void doesntMatchNonAlphaCustom() {
		assertThat("1.2.3.0-1.RELEASE").as("numeric only").doesNotMatch(Module.VERSION_REGEXP);
		assertThat("1.2.3.0cus-Tom0.RELEASE").as("alphanumeric").doesNotMatch(Module.VERSION_REGEXP);
		assertThat("1.2.3.cus-Tom.RELEASE").as("alphabetic only").doesNotMatch(Module.VERSION_REGEXP);
	}

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

}