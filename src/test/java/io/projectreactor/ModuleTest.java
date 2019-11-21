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

}