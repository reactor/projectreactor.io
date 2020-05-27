package io.projectreactor;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reactor.util.annotation.Nullable;

/**
 * A representation of version components for comparisons. Also takes into account several versioning schemes
 * of Reactor (as the scheme changed eg. in 2020). Use {@link Version#parse(String)} to parse a string representation,
 * attempting to recognize the scheme.
 * <p>
 * Versions are {@link Comparable}, and they are sorted from oldest to newest in their {@link Comparator#naturalOrder() natural order}.
 *
 * @author Simon Baslé
 */
public class Version implements Comparable<Version> {

	public enum VersionStyle {
		/**
		 * The now deprecated scheme for release trains up to Dysprosium (included)
		 */
		OLD_CODENAME_TRAIN,
		/**
		 * The now deprecated scheme, up until Reactor Core 3.3.x (included)
		 */
		OLD_OSGI_COMPATIBLE,
		/**
		 * The new scheme as of 2020 / Reactor Core 3.4.x
		 */
		MAVEN_GRADLE
	}

	public enum Qualifier {
		LOWEST_PLACEHOLDER, MILESTONE, RELEASE_CANDIDATE, SNAPSHOT, RELEASE //order determines comparisons
	}

	/**
	 * A regexp that matches old scheme of versioning release trains with their codename, in the form of either {@code codename-RELEASE},
	 * {@code codename-SRx} or {@code codename-BUILD-SNAPSHOT}
	 * {@code MAJOR.MINOR.PATCH.CustomVersion.qualifier}. {@code CustomVersion} is expected to be alphanumerical only.
	 */
	static final Pattern OLD_TRAIN_REGEXP = Pattern.compile("([A-D][a-zA-Z]+)-([a-zA-Z0-9-]*)");

	/**
	 * A regexp that matches version numbers in the form of either {@code MAJOR.MINOR.PATCH.qualifier} or
	 * {@code MAJOR.MINOR.PATCH.CustomVersion.qualifier}. {@code CustomVersion} is expected to be alphanumerical only.
	 */
	static final Pattern OLD_OSGI_COMPATIBLE_REGEXP = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)(\\.[a-zA-Z0-9]*)?(\\.[a-zA-Z0-9_-]*)");

	/**
	 * A regexp that matches version numbers in the form of either {@code MAJOR.MINOR.PATCH}, {@code GEN.MAJOR.MINOR.PATCH-qualifier} or
	 * {@code MAJOR.MINOR.PATCH-qualifier-CustomVersion}. {@code CustomVersion} is expected to be alphanumerical only.
	 */
	static final Pattern MAVEN_GRADLE_REGEXP     = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)(-[a-zA-Z0-9]*)?(-[a-zA-Z0-9]*)?");

	public static Version parse(String version) {
		if (version == null || version.length() < 1) {
			throw new IllegalArgumentException("Version string is null or empty");
		}
		char firstChar = version.charAt(0);
		if (firstChar >= 'A' && firstChar <= 'Z') { //we got a codename
			Matcher codenameMatcher = OLD_TRAIN_REGEXP.matcher(version);
			if (!codenameMatcher.matches()) {
				throw new IllegalArgumentException("Unparseable codename version " + version);
			}
			//codename versions are a bit strange: 1 snapshot PER CODENAME, only milestones/rcs are for .0, etc...
			//let's consider the MAJOR to be 3, the MINOR to be driven by the first char of the codename and the PATCH by the SR number.
			int minor = firstChar;
			int patch;
			Qualifier qualifier;
			int qualifierVersion = 0;
			String secondPart = codenameMatcher.group(2);
			if (secondPart.equalsIgnoreCase("RELEASE")) {
				patch = 0;
				qualifier = Qualifier.RELEASE;
			}
			else if (secondPart.startsWith("SR")) {
				patch = Integer.parseInt(secondPart.substring(2));
				qualifier = Qualifier.RELEASE;
			}
			else if (secondPart.startsWith("M")) {
				patch = 0;
				qualifier = Qualifier.MILESTONE;
				qualifierVersion = Integer.parseInt(secondPart.substring(1));
			}
			else if (secondPart.startsWith("RC")) {
				patch = 0;
				qualifier = Qualifier.RELEASE_CANDIDATE;
				qualifierVersion = Integer.parseInt(secondPart.substring(2));
			}
			else if (secondPart.equalsIgnoreCase("BUILD-SNAPSHOT")) {
				patch = 999;
				qualifier = Qualifier.SNAPSHOT;
			}
			else {
				throw new IllegalArgumentException("Unparseable codename qualifier " + secondPart);
			}

			return new Version(3, minor, patch, qualifier, qualifierVersion, null, VersionStyle.OLD_CODENAME_TRAIN, version);
		}

		Matcher newStyleMatcher = MAVEN_GRADLE_REGEXP.matcher(version);
		if (newStyleMatcher.matches()) {
			int major = Integer.parseInt(newStyleMatcher.group(1));
			int minor = Integer.parseInt(newStyleMatcher.group(2));
			int patch = Integer.parseInt(newStyleMatcher.group(3));

			String qualifierString = newStyleMatcher.group(4);
			Qualifier qualifier;
			int qualifierNumber;
			if (qualifierString == null) {
				qualifier = Qualifier.RELEASE;
				qualifierNumber = 0;
			}
			else if (qualifierString.equalsIgnoreCase("-SNAPSHOT")) {
				qualifier = Qualifier.SNAPSHOT;
				qualifierNumber = 0;
			}
			else if (qualifierString.startsWith("-M")) {
				qualifier = Qualifier.MILESTONE;
				qualifierNumber = Integer.parseInt(qualifierString.substring(2));
			}
			else if (qualifierString.startsWith("-RC")) {
				qualifier = Qualifier.RELEASE_CANDIDATE;
				qualifierNumber = Integer.parseInt(qualifierString.substring(3));
			}
			else {
				throw new IllegalArgumentException("Unrecognized qualifier " + qualifierString);
			}

			String customQualifierString = newStyleMatcher.group(5) == null ? null : newStyleMatcher.group(5).substring(1);
			return new Version(major, minor, patch, qualifier, qualifierNumber, customQualifierString, VersionStyle.MAVEN_GRADLE, version);
		}

		Matcher osgiStyleMatcher = OLD_OSGI_COMPATIBLE_REGEXP.matcher(version);
		if (osgiStyleMatcher.matches()) {
			int major = Integer.parseInt(osgiStyleMatcher.group(1));
			int minor = Integer.parseInt(osgiStyleMatcher.group(2));
			int patch = Integer.parseInt(osgiStyleMatcher.group(3));

			String qualifierString;
			String customQualifierString;
			if (osgiStyleMatcher.group(5) != null && osgiStyleMatcher.group(4) == null) {
				qualifierString = osgiStyleMatcher.group(5);
				customQualifierString = null;
			}
			else {
				//there's a custom qualifier in the middle
				customQualifierString = osgiStyleMatcher.group(4).substring(1);
				qualifierString = osgiStyleMatcher.group(5);
			}

			Qualifier qualifier;
			int qualifierNumber;
			if (qualifierString.equalsIgnoreCase(".RELEASE")) {
				qualifier = Qualifier.RELEASE;
				qualifierNumber = 0;
			}
			else if (qualifierString.equalsIgnoreCase(".BUILD-SNAPSHOT")) {
				qualifier = Qualifier.SNAPSHOT;
				qualifierNumber = 0;
			}
			else if (qualifierString.startsWith(".M")) {
				qualifier = Qualifier.MILESTONE;
				qualifierNumber = Integer.parseInt(qualifierString.substring(2));
			}
			else if (qualifierString.startsWith(".RC")) {
				qualifier = Qualifier.RELEASE_CANDIDATE;
				qualifierNumber = Integer.parseInt(qualifierString.substring(3));
			}
			else {
				throw new IllegalArgumentException("Unrecognized qualifier " + qualifierString);
			}

			return new Version(major, minor, patch, qualifier, qualifierNumber, customQualifierString, VersionStyle.OLD_OSGI_COMPATIBLE, version);
		}

		throw new IllegalArgumentException("Cannot recognize versioning scheme for version " + version);
	}

	final         int       major;
	final         int       minor;
	final         int       patch;
	final         Qualifier qualifier;
	final         int       qualifierVersion;

	@Nullable
	final         String       customQualifier;
	final         VersionStyle style;
	private final String       stringRepresentation;

	Version(int major,
			int minor,
			int patch,
			Qualifier qualifier,
			int qualifierVersion,
			@Nullable String customQualifier,
			VersionStyle style,
			String originalStringRepresentation) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.qualifier = qualifier;
		this.qualifierVersion = qualifierVersion;
		this.customQualifier = customQualifier;
		this.style = style;
		this.stringRepresentation = originalStringRepresentation;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	public Qualifier getQualifier() {
		return qualifier;
	}

	public int getQualifierVersion() {
		return qualifierVersion;
	}

	public Optional<String> getCustomQualifier() {
		return Optional.ofNullable(customQualifier);
	}

	public VersionStyle getStyle() {
		return style;
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

	/**
	 * @param major the expected MAJOR component of the MAJOR.MINOR.PATCH version
	 * @param minor the expected MINOR component of the MAJOR.MINOR.PATCH version
	 * @return true if this version belongs to the given MAJOR.MINOR (any patch, any qualifier)
	 */
	public boolean isInMajorMinor(int major, int minor) {
		return this.major == major && this.minor == minor;
	}

	/**
	 * Is this {@link Version} STRICTLY after the given version components ? Ie was this released more recently.
	 *
	 * @param major the MAJOR component
	 * @param minor the MINOR component
	 * @param patch the PATCH component
	 * @param qualifier the level of general availability to consider (eg. a SNAPSHOT is released after a MILESTONE)
	 * @param qualifierVersion the additional version number, if any, for that qualifier (eg. 2 for a second MILESTONE)
	 * @return true if this Version was released after the given version components
	 */
	public boolean isAfter(int major, int minor, int patch, Qualifier qualifier, int qualifierVersion) {
		//escape hatch for comparing codename-based vs numerical components, like 2020.x, which are ALWAYS after
		if (style == VersionStyle.OLD_CODENAME_TRAIN) return false;

		if (this.major != major) return this.major > major;
		if (this.minor != minor) return this.minor > minor;
		if (this.patch != patch) return this.patch > patch;
		if (this.qualifier != qualifier) return this.qualifier.ordinal() > qualifier.ordinal();
		return this.qualifierVersion > qualifierVersion;
	}

	/**
	 * Is this {@link Version} STRICTLY after the given version components ? Ie was this released more recently.
	 *
	 * @param major the MAJOR component
	 * @param minor the MINOR component
	 * @param patch the PATCH component
	 * @param qualifier the level of general availability to consider, without number (eg. a SNAPSHOT or RELEASE)
	 * @return true if this Version was released after the given version components
	 */
	public boolean isAfter(int major, int minor, int patch, Qualifier qualifier) {
		return isAfter(major, minor, patch, qualifier, 999);
	}

	/**
	 * Is this {@link Version} STRICTLY before the given version components ? Ie was this released less recently.
	 *
	 * @param major the MAJOR component
	 * @param minor the MINOR component
	 * @param patch the PATCH component
	 * @param qualifier the level of general availability to consider (eg. a RELEASE_CANDIDATE is released before a RELEASE)
	 * @param qualifierVersion the additional version number, if any, for that qualifier (eg. 2 for a second MILESTONE)
	 * @return true if this Version was released before the given version components
	 */
	public boolean isBefore(int major, int minor, int patch, Qualifier qualifier, int qualifierVersion) {
		//escape hatch for comparing codename-based vs numerical components, like 2020.x, which are ALWAYS after
		if (style == VersionStyle.OLD_CODENAME_TRAIN) return true;

		if (this.major != major) return this.major < major;
		if (this.minor != minor) return this.minor < minor;
		if (this.patch != patch) return this.patch < patch;
		if (this.qualifier != qualifier) return this.qualifier.ordinal() < qualifier.ordinal();
		return this.qualifierVersion < qualifierVersion;
	}

	/**
	 * Is this {@link Version} STRICTLY before the given version components ? Ie was this released less recently.
	 *
	 * @param major the MAJOR component
	 * @param minor the MINOR component
	 * @param patch the PATCH component
	 * @param qualifier the level of general availability to consider (eg. a RELEASE_CANDIDATE is released before a RELEASE)
	 * @return true if this Version was released before the given version components
	 */
	public boolean isBefore(int major, int minor, int patch, Qualifier qualifier) {
		return isBefore(major, minor, patch, qualifier, 0);
	}

	@Override
	public int compareTo(Version o) { //positive/greater is interpreted as "released AFTER / MORE RECENTLY"
		if (o == null) return 1;
		if (major != o.major) return major - o.major;
		if (minor != o.minor) return minor - o.minor;
		if (patch != o.patch) return patch - o.patch;
		if (qualifier != o.qualifier) return qualifier.ordinal() - o.qualifier.ordinal();
		if (qualifierVersion != o.qualifierVersion) return qualifierVersion - o.qualifierVersion;
		if (customQualifier == null && o.customQualifier != null) return 1;
		if (customQualifier != null && o.customQualifier == null) return -1;
		if (customQualifier != null) { //means both are non-null
			return customQualifier.compareToIgnoreCase(o.customQualifier);
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Version version = (Version) o;
		return major == version.major && minor == version.minor && patch == version.patch && qualifier == version.qualifier && Objects.equals(
				customQualifier,
				version.customQualifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, qualifier, qualifierVersion, customQualifier);
	}
}
