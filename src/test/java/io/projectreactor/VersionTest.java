package io.projectreactor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Simon Baslé
 */
public class VersionTest {

	@Test
	public void naturalOrderIsOldestFirst() {
		Version oldest = Version.parse("1.2.3.RELEASE");
		Version older = Version.parse("1.2.4-SNAPSHOT");
		Version newer = Version.parse("1.2.4");
		Version newest = Version.parse("1.2.5.BUILD-SNAPSHOT");

		List<Version> versions = Arrays.asList(older, newest, newer, oldest);
		versions.sort(Comparator.naturalOrder());

		assertThat(versions).containsExactly(oldest, older, newer, newest);
	}

	@Test
	public void reversedOrderIsNewestFirst() {
		Version oldest = Version.parse("1.2.3.RELEASE");
		Version older = Version.parse("1.2.4-SNAPSHOT");
		Version newer = Version.parse("1.2.4");
		Version newest = Version.parse("1.2.5.BUILD-SNAPSHOT");

		List<Version> versions = Arrays.asList(older, newest, newer, oldest);
		versions.sort(Comparator.reverseOrder());

		assertThat(versions).containsExactly(newest, newer, older, oldest);
	}


	@Test
	public void compareQualifier() {
		List<Version> versions = Stream.of("1.2.3.BUILD-SNAPSHOT", "1.2.3.M3", "1.2.3.RC1", "1.2.3.RELEASE")
		                               .map(Version::parse).collect(Collectors.toList());

		assertThat(versions.stream().sorted(Comparator.naturalOrder()).map(Object::toString))
				.as("natural, oldest first")
				.containsExactly("1.2.3.M3", "1.2.3.RC1", "1.2.3.BUILD-SNAPSHOT", "1.2.3.RELEASE");

		assertThat(versions.stream().sorted(Comparator.reverseOrder()).map(Object::toString))
				.as("reverse, newest first")
				.containsExactly("1.2.3.RELEASE", "1.2.3.BUILD-SNAPSHOT", "1.2.3.RC1", "1.2.3.M3");
	}

	@Test
	public void compareDifferentPatches() {
		List<Version> versions = Stream.of("1.2.0.RELEASE", "1.2.3", "1.2.1.RELEASE", "1.2.2.RELEASE")
		                               .map(Version::parse).collect(Collectors.toList());

		assertThat(versions.stream().sorted(Comparator.naturalOrder()).map(Object::toString))
				.as("natural, oldest first")
				.containsExactly("1.2.0.RELEASE", "1.2.1.RELEASE", "1.2.2.RELEASE", "1.2.3");

		assertThat(versions.stream().sorted(Comparator.reverseOrder()).map(Object::toString))
				.as("reverse, newest first")
				.containsExactly("1.2.3", "1.2.2.RELEASE", "1.2.1.RELEASE", "1.2.0.RELEASE");
	}

	@Test
	public void compareDifferentMinors() {
		List<Version> versions = Stream.of("1.2.0.RELEASE", "1.3.0.RELEASE", "1.4.0", "1.1.0.RELEASE")
		                               .map(Version::parse).collect(Collectors.toList());

		assertThat(versions.stream().sorted(Comparator.naturalOrder()).map(Object::toString))
				.as("natural, oldest first")
				.containsExactly("1.1.0.RELEASE", "1.2.0.RELEASE", "1.3.0.RELEASE", "1.4.0");

		assertThat(versions.stream().sorted(Comparator.reverseOrder()).map(Object::toString))
				.as("reverse, newest first")
				.containsExactly("1.4.0", "1.3.0.RELEASE", "1.2.0.RELEASE", "1.1.0.RELEASE");
	}

	@Test
	public void compareDifferentMajors() {
		List<Version> versions = Stream.of("1.2.0.RELEASE", "2.2.0.RELEASE", "4.0.1", "3.2.0.RELEASE")
		                               .map(Version::parse).collect(Collectors.toList());

		assertThat(versions.stream().sorted(Comparator.naturalOrder()).map(Object::toString))
				.as("natural, oldest first")
				.containsExactly("1.2.0.RELEASE", "2.2.0.RELEASE", "3.2.0.RELEASE", "4.0.1");

		assertThat(versions.stream().sorted(Comparator.reverseOrder()).map(Object::toString))
				.as("reverse, newest first")
				.containsExactly("4.0.1", "3.2.0.RELEASE", "2.2.0.RELEASE", "1.2.0.RELEASE");
	}

	@Test
	public void comparePreReleases() {
		List<Version> versions = Stream.of("1.2.3-M2", "1.2.3.RC3", "1.2.3-M1", "1.2.3-RC1", "1.2.3.M1")
		                               .map(Version::parse).collect(Collectors.toList());

		assertThat(versions.stream().sorted(Comparator.naturalOrder()).map(Object::toString))
				.as("natural, oldest first")
				.containsExactly("1.2.3.M1", "1.2.3-M1", "1.2.3-M2", "1.2.3-RC1", "1.2.3.RC3");

		assertThat(versions.stream().sorted(Comparator.reverseOrder()).map(Object::toString))
				.as("reverse, newest first")
				.containsExactly("1.2.3.RC3", "1.2.3-RC1", "1.2.3-M2", "1.2.3-M1", "1.2.3.M1");
	}

	@Test
	public void compareCustomVersionQualifier_oldScheme() {
		List<Version> versions = Stream.of("1.2.3.customVersionA.BUILD-SNAPSHOT", "1.2.3.BUILD-SNAPSHOT", "1.2.3.customVersionB.BUILD-SNAPSHOT")
		                               .map(Version::parse).collect(Collectors.toList());

		assertThat(versions.stream().sorted(Comparator.naturalOrder()).map(Object::toString))
				.as("natural, oldest first")
				.containsExactly("1.2.3.customVersionA.BUILD-SNAPSHOT", "1.2.3.customVersionB.BUILD-SNAPSHOT", "1.2.3.BUILD-SNAPSHOT");

		assertThat(versions.stream().sorted(Comparator.reverseOrder()).map(Object::toString))
				.as("reverse, newest first")
				.containsExactly("1.2.3.BUILD-SNAPSHOT", "1.2.3.customVersionB.BUILD-SNAPSHOT", "1.2.3.customVersionA.BUILD-SNAPSHOT");
	}

	@Test
	public void compareCustomVersionQualifier() {
		List<Version> versions = Stream.of("1.2.3-SNAPSHOT-customVersionA", "1.2.3-SNAPSHOT", "1.2.3-SNAPSHOT-customVersionB")
		                               .map(Version::parse).collect(Collectors.toList());

		assertThat(versions.stream().sorted(Comparator.naturalOrder()).map(Object::toString))
				.as("natural, oldest first")
				.containsExactly("1.2.3-SNAPSHOT-customVersionA", "1.2.3-SNAPSHOT-customVersionB", "1.2.3-SNAPSHOT");

		assertThat(versions.stream().sorted(Comparator.reverseOrder()).map(Object::toString))
				.as("reverse, newest first")
				.containsExactly("1.2.3-SNAPSHOT", "1.2.3-SNAPSHOT-customVersionB", "1.2.3-SNAPSHOT-customVersionA");
	}

	@Test
	public void matchesReleaseVersion() {
		Condition<Version> aRelease = new Condition<>(v -> v.qualifier == Version.Qualifier.RELEASE && v.qualifierVersion == 0 && v.customQualifier == null,
				"a release version");

		assertThat(Version.parse("1.2.3.RELEASE")).as("old scheme").is(aRelease);
		assertThat(Version.parse("1.2.3")).as("new scheme").is(aRelease);
		assertThat(Version.parse("Californium-RELEASE")).as("old train release").is(aRelease);
		assertThat(Version.parse("Californium-SR2")).as("old train service release").is(aRelease);
		assertThat(Version.parse("2020.0.0")).as("new train first release").is(aRelease);
		assertThat(Version.parse("2020.0.1")).as("new train service release").is(aRelease);
	}

	@Test
	public void matchesReleaseCandidateVersion() {
		Condition<Version> aReleaseCandidate = new Condition<>(v -> v.qualifier == Version.Qualifier.RELEASE_CANDIDATE
				&& v.qualifierVersion == 11 && v.customQualifier == null,
				"a release candidate version");

		assertThat(Version.parse("1.2.3.RC11")).as("old scheme").is(aReleaseCandidate);
		assertThat(Version.parse("1.2.3-RC11")).as("new scheme").is(aReleaseCandidate);
		assertThat(Version.parse("Californium-RC11")).as("old train").is(aReleaseCandidate);
		assertThat(Version.parse("2020.0.0-RC11")).as("new train").is(aReleaseCandidate);
	}

	@Test
	public void matchesMilestoneVersion() {
		Condition<Version> aMilestone = new Condition<>(v -> v.qualifier == Version.Qualifier.MILESTONE
				&& v.qualifierVersion == 23 && v.customQualifier == null,
				"a milestone version");

		assertThat(Version.parse("1.2.3.M23")).as("old scheme").is(aMilestone);
		assertThat(Version.parse("1.2.3-M23")).as("new scheme").is(aMilestone);
		assertThat(Version.parse("Californium-M23")).as("old train").is(aMilestone);
		assertThat(Version.parse("2020.0.0-M23")).as("new train").is(aMilestone);
	}

	@Test
	public void matchesSnapshotVersion() {
		Condition<Version> aSnapshot = new Condition<>(v -> v.qualifier == Version.Qualifier.SNAPSHOT
				&& v.qualifierVersion == 0 && v.customQualifier == null,
				"a snapshot version");

		assertThat(Version.parse("1.2.3.BUILD-SNAPSHOT")).as("old scheme").is(aSnapshot);
		assertThat(Version.parse("1.2.3-SNAPSHOT")).as("new scheme").is(aSnapshot);
		assertThat(Version.parse("Californium-BUILD-SNAPSHOT")).as("old train snapshot").is(aSnapshot);
		assertThat(Version.parse("2020.0.0-SNAPSHOT")).as("new train snapshot").is(aSnapshot);
	}

	@Test
	public void distinguishCustomVersionInOldScheme() {
		Version v = Version.parse("1.2.3.0cusTom0.BUILD-SNAPSHOT");

		assertThat(v.customQualifier).as("custom qualifier").isEqualTo("0cusTom0");
		assertThat(v.getQualifier()).isEqualTo(Version.Qualifier.SNAPSHOT);
		assertThat(v.qualifierVersion).as("qualifier version").isEqualTo(0);
		assertThat(v.isInMajorMinor(1, 2)).as("majorMinor 1.2").isTrue();
		assertThat(v.patch).as("patch").isEqualTo(3);
		assertThat(v.style).isEqualTo(Version.VersionStyle.OLD_OSGI_COMPATIBLE);
	}

	@Test
	public void distinguishCustomVersionInNewScheme() {
		Version v = Version.parse("1.2.3-SNAPSHOT-0cusTom0");

		assertThat(v.customQualifier).as("custom qualifier").isEqualTo("0cusTom0");
		assertThat(v.getQualifier()).isEqualTo(Version.Qualifier.SNAPSHOT);
		assertThat(v.qualifierVersion).as("qualifier version").isEqualTo(0);
		assertThat(v.isInMajorMinor(1, 2)).as("majorMinor 1.2").isTrue();
		assertThat(v.patch).as("patch").isEqualTo(3);
		assertThat(v.style).isEqualTo(Version.VersionStyle.MAVEN_GRADLE);
	}

//	@Test
//	public void matchesCustomSnapshotVersions() {
//		assertThat("1.2.3.01.BUILD-SNAPSHOT").as("numeric only").matches(Module.VERSION_OLD_REGEXP);
//		assertThat("1.2.3.0cusTom0.BUILD-SNAPSHOT").as("alphanumeric").matches(Module.VERSION_OLD_REGEXP);
//		assertThat("1.2.3.cusTom.BUILD-SNAPSHOT").as("alphabetic only").matches(Module.VERSION_OLD_REGEXP);
//	}
//
//	@Test
//	public void matchesCustomReleaseVersions() {
//		assertThat("1.2.3.01.RELEASE").as("numeric only").matches(Module.VERSION_OLD_REGEXP);
//		assertThat("1.2.3.0cusTom0.RELEASE").as("alphanumeric").matches(Module.VERSION_OLD_REGEXP);
//		assertThat("1.2.3.cusTom.RELEASE").as("alphabetic only").matches(Module.VERSION_OLD_REGEXP);
//	}

	@Test
	public void doesntParseNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse(null))
				.withMessage("Version string is null or empty");
	}

	@Test
	public void doesntParseEmpty() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse(""))
				.withMessage("Version string is null or empty");
	}

	@Test
	public void doesntParseSingleChar() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("A"))
				.withMessage("Unparseable codename version A");
	}

	@Test
	public void doesntParseInvalidCodename() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("FakeName-RELEASE"))
				.withMessage("Unparseable codename version FakeName-RELEASE");
	}

	@Test
	public void doesntParseInvalidCodenameQualifier() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("DefinitelyInRange-TOBEREJECTED"))
				.withMessage("Unparseable codename qualifier TOBEREJECTED");
	}

	@Test
	public void doesntParseNonAlphaCustomInOldSchemeStyle() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("1.2.3.0-1.RELEASE"))
				.as("old, numeric only")
				.withMessage("Cannot recognize versioning scheme for version 1.2.3.0-1.RELEASE");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("1.2.3.0cus-Tom0.RELEASE"))
				.as("old, alphanumeric")
				.withMessage("Cannot recognize versioning scheme for version 1.2.3.0cus-Tom0.RELEASE");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("1.2.3.cus-Tom.RELEASE"))
				.as("old, alphabetic only")
				.withMessage("Cannot recognize versioning scheme for version 1.2.3.cus-Tom.RELEASE");
	}

	@Test
	public void doesntParseNonAlphaCustomInNewSchemeStyle() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("1.2.3-SNAPSHOT-0-1"))
				.as("old, numeric only")
				.withMessage("Cannot recognize versioning scheme for version 1.2.3-SNAPSHOT-0-1");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("1.2.3-SNAPSHOT-0cus-Tom0"))
				.as("old, alphanumeric")
				.withMessage("Cannot recognize versioning scheme for version 1.2.3-SNAPSHOT-0cus-Tom0");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Version.parse("1.2.3-SNAPSHOT-cus-Tom"))
				.as("old, alphabetic only")
				.withMessage("Cannot recognize versioning scheme for version 1.2.3-SNAPSHOT-cus-Tom");
	}

	@Test
	public void isBeforeIsExclusive() {
		Version v = new Version(1, 2, 3, Version.Qualifier.MILESTONE, 3, null, Version.VersionStyle.MAVEN_GRADLE, "v");

		assertThat(v.isBefore(1, 2, 3, Version.Qualifier.MILESTONE, 3)).as("with qualifier version").isFalse();
		assertThat(v.isBefore(1, 2, 3, Version.Qualifier.MILESTONE)).as("without qualifier version").isFalse();

		assertThat(v.isBefore(1, 2, 3, Version.Qualifier.MILESTONE, 4)).as("with higher qualifier version").isTrue();
	}

	@Test
	public void isAfterIsExclusive() {
		Version v = new Version(1, 2, 3, Version.Qualifier.MILESTONE, 3, null, Version.VersionStyle.MAVEN_GRADLE, "v");

		assertThat(v.isAfter(1, 2, 3, Version.Qualifier.MILESTONE, 3)).as("with qualifier version").isFalse();
		assertThat(v.isAfter(1, 2, 3, Version.Qualifier.MILESTONE)).as("without qualifier version").isFalse();

		assertThat(v.isAfter(1, 2, 3, Version.Qualifier.MILESTONE, 2)).as("with lesser qualifier version").isTrue();
	}

	@Test
	public void compareToEqualsHashcodeDifferentiatesStyles() {
		Version snapshotNewScheme = new Version(1, 2, 3, Version.Qualifier.SNAPSHOT, 0, null, Version.VersionStyle.MAVEN_GRADLE, "newScheme");
		Version snapshotOldScheme = new Version(1, 2, 3, Version.Qualifier.SNAPSHOT, 0, null, Version.VersionStyle.OLD_OSGI_COMPATIBLE, "oldScheme");

		assertThat(snapshotNewScheme)
				.isGreaterThan(snapshotOldScheme)
				.isNotEqualTo(snapshotOldScheme);

		assertThat(snapshotNewScheme.hashCode()).as("hashcode").isNotEqualTo(snapshotOldScheme.hashCode());
	}

	@Test
	public void compareToPrioritizesQualifierVersionsOverStyles() {
		Version snapshotNewSchemeM1 = new Version(1, 2, 3, Version.Qualifier.MILESTONE, 1, null, Version.VersionStyle.MAVEN_GRADLE, "1.2.3-M1");
		Version snapshotOldSchemeM2 = new Version(1, 2, 3, Version.Qualifier.MILESTONE, 2, null, Version.VersionStyle.OLD_OSGI_COMPATIBLE, "1.2.3.M2");

		assertThat(snapshotNewSchemeM1)
				.isLessThan(snapshotOldSchemeM2);
	}

	@Test
	public void equalsFindsDifferenceInQualifierVersion() {
		Version v1 = new Version(1, 2, 3, Version.Qualifier.MILESTONE, 1, null, Version.VersionStyle.MAVEN_GRADLE, "1.2.3-M1");
		Version v2 = new Version(1, 2, 3, Version.Qualifier.MILESTONE, 2, null, Version.VersionStyle.MAVEN_GRADLE, "1.2.3-M2");

		assertThat(v1).isNotEqualTo(v2);
	}

}