/*
 * Copyright (c) 2017-2023 VMware LLC or its affiliates, All Rights Reserved.
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

import java.util.Map;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static io.projectreactor.Version.Qualifier.*;

/**
 * @author Simon Baslé
 */
public class DocUtils {

	/**
	 * Find the correct module and version corresponding to the requested module and
	 * version, falling back to a module called `moduleNameArchive` if one isn't found for
	 * the originally requested module. Special versions are "snapshot", "milestone" and
	 * "version". All other string are interpreted as a specific version.
	 *
	 * @param modules the map of {@link Module} objects to search
	 * @param moduleName the module name
	 * @param versionName the version name
	 * @return a {@link Tuple2} with the resolved actual {@link Module} and version String,
	 * or null if none could be found, including in moduleNameArchive.
	 */
	public static Tuple2<Module,String> findModuleAndVersion(Map<String, Module> modules,
			String moduleName, String versionName) {
		Tuple2<Module, String> result = findModuleAndVersionDirect(modules, moduleName, versionName);
		if (result == null)
			result = findModuleAndVersionDirect(modules, moduleName + "Archive", versionName);
		return result;
	}

	protected static boolean checkModuleVersion(String toCheck, String upperVersionName) {
		boolean matches = (upperVersionName.equals("MILESTONE") && toCheck.matches(".*[.-]M[0-9]+"))
				|| (upperVersionName.equals("MILESTONE") && toCheck.matches(".*[.-]RC[0-9]+"))
				|| (upperVersionName.equals("SNAPSHOT") && toCheck.endsWith("-SNAPSHOT")) //common in both schemes
				|| (upperVersionName.equals("RELEASE") && (
						//old scheme for releases
						toCheck.endsWith(".RELEASE") ||
						//new scheme for releases
						toCheck.matches("[0-9]+\\.[0-9]+\\.[0-9]+")))
				|| toCheck.equalsIgnoreCase(upperVersionName);

		return matches;
	}

	private static Tuple2<Module,String> findModuleAndVersionDirect(Map<String, Module> modules,
			String moduleName, String versionName) {
		Module module = modules.get(moduleName);

		if(module == null){
			return null;
		}

		final String vn = versionName.toUpperCase();

		String version = module.getVersions()
		                       .stream()
		                       .map(String::toUpperCase)
		                       .filter(v -> checkModuleVersion(v, vn))
		                       .findFirst().orElseGet((() -> "NA"));

		if(version.equals("NA")){
			 return null;
		}
		return Tuples.of(module, version);
	}

	/**
	 * From a version string, resolve the version type, that is to say the repository to
	 * use in repo.spring.io.
	 *
	 * @param version the version, either specific or one of the codified "release",
	 * "snapshot" and "milestone". Milestones are expected to end in M[0-9]. Release
	 * candidates (ending in RC[0-9]) are also considered milestones.
	 * @return the repository type to use
	 */
	public static String findVersionType(String version) {
		String v = version.toUpperCase();
		if (v.endsWith("RELEASE")) return "release";
		if (v.endsWith("SNAPSHOT")) return "snapshot";
		if (v.equals("MILESTONE") || v.matches(".*[.-]M[0-9]+")) return "milestone";
		if (v.matches(".*[.-]RC[0-9]+")) return "milestone";
		return "release"; //default
	}

	/**
	 * Convert information about a request (path, requested module, requested version) and
	 * the corresponding resolved module information (actual module including archive ones,
	 * actual version for codified requested versions) into the url to proxy.
	 * <p>
	 * Detects the type of documentation (javadoc or reference guide) from the path (api
	 * vs reference). Shows the index if no further path is given, or the same relative path
	 * inside the documentation archives otherwise.
	 *
	 * @param reqUri the path requested
	 * @param versionType the repository to target in repo.spring.io
	 * @param requestedModuleName the module name (as found in the path)
	 * @param requestedVersion the requested version name (as found in the path)
	 * @param actualModule the actual module to use (as resolved by {@link #findModuleAndVersion(Map, String, String)})
	 * @param actualVersion the actual version to use (as resolved by {@link #findModuleAndVersion(Map, String, String)})
	 * @return the url to proxy for the documentation
	 */
	public static String moduleToUrl(String reqUri, String versionType,
			String requestedModuleName, String requestedVersion, Module actualModule,
			String actualVersion) {
		//protect against incomplete root api/reference path (offset is set with final / in mind)
		if (reqUri.endsWith("/api")
				|| reqUri.endsWith("/reference")
				|| reqUri.endsWith("/kdoc-api")) reqUri += "/";

		String url;
		if (reqUri.contains("/api/")) {
			url = moduleToArtifactUrl(reqUri, versionType, requestedModuleName,
					requestedVersion, actualModule, actualVersion,
					12, "index.html", "-javadoc.jar", "", "");
		}
		else if (reqUri.contains("/kdoc-api/")) {
			if (isKDocSpecialCases(actualModule.getName(), actualVersion)) {
				return WARNING_NO_KDOC + actualModule.getArtifactId() + ":" + actualVersion;
			}
			url = moduleToKdocUrl(reqUri, versionType, requestedModuleName,
					requestedVersion, actualModule, actualVersion);
		}
		else {
			if (isRefguideOldFormat(actualModule.getName(), actualVersion)) {
				url = moduleToArtifactUrl(reqUri, versionType, requestedModuleName,
						requestedVersion, actualModule, actualVersion,
						18, "index.html", ".zip", "-docs",
						"docs/");
			}
			else {
				url = moduleToArtifactUrl(reqUri, versionType, requestedModuleName,
						requestedVersion, actualModule, actualVersion,
						18, "index.html", "-docs.zip", "",
						"docs/");
			}
		}
		return url;
	}

	//FIXME switch all methods with String version parameter to Version
	static boolean isRefguideOldFormat(String module, String version) {
		Version v = Version.parse(version);
		switch (module) {
			case "core":
				return v.isInMajorMinor(3, 0) ||
						v.isInMajorMinor(3, 1) ||
						(v.isInMajorMinor(3, 2) && v.patch <= 12) ||
						(v.isInMajorMinor(3, 3) && v.patch == 0);
			case "netty":
				return (v.isInMajorMinor(0, 8) && v.patch <= 9) ||
						(v.isInMajorMinor(0, 9) && v.patch < 2);
			case "kafka":
				return v.isInMajorMinor(1, 0) ||
						(v.isInMajorMinor(1, 1) && v.patch <= 1) ||
						(v.isInMajorMinor(1, 2) && v.patch == 0);
			case "rabbitmq":
				return v.isInMajorMinor(1, 0) ||
						v.isInMajorMinor(1, 1) ||
						v.isInMajorMinor(1, 2) ||
						v.isInMajorMinor(1, 3) ||
						(v.isInMajorMinor(1, 4) && v.isBefore(1, 4, 0, RELEASE));
			default:
				return false;
		}
	}

	static boolean isKDocSpecialCases(String module, String version) {
		switch (module) {
			case "core":
			case "extra":
			case "test":
				//core/addons/test >= Dysprosium
				return !version.startsWith("3.0")
						&& !version.startsWith("3.1")
						&& !version.startsWith("3.2");
			case "kotlin":
				//only kotlin 1.0.0.RELEASE has no kdoc
				return version.equals("1.0.0.RELEASE");
			default:
				return false;
		}
	}

	static boolean hasKDoc(String module, String version) {
		switch (module) {
			case "core":
			case "extra":
			case "test":
				//core/addons/test < Dysprosium have KDoc
				return version.startsWith("3.0")
						|| version.startsWith("3.1")
						|| version.startsWith("3.2");
			case "kotlin":
				//reactor-kotlin-extensions Dysprosium
				//only 1.0.0.RELEASE have no kdoc
				return !version.equals("1.0.0.RELEASE");
			default:
				return false;
		}
	}

	//FIXME switch to Version instead of String
	static String getRefDocPath(String module, String version) {
		switch (module) {
			case "core":
			case "kafka":
			case "rabbitmq":
				return "/docs/" + module + "/" + version + "/reference";
			case "test":
				return "/docs/core/" + version + "/reference/index.html#testing";
			case "netty":
				Version nettyVersion = Version.parse(version);
				if (nettyVersion.isAfter(0, 9, 0, SNAPSHOT)) return "/docs/netty/" + version + "/reference";
				else return "";
			default:
				return "";
		}
	}

	static String moduleToKdocUrl(String reqUri, String versionType,
			String requestedModuleName, String requestedVersion, Module actualModule,
			String actualVersion) {
		int offset = 17;
		String indexFile = actualModule.getArtifactId() + "/index.html";
		String suffix = "-kdoc.zip";

		String file = reqUri.substring(offset + requestedModuleName.length() + requestedVersion.length());
		if (file.isEmpty()) {
			file = indexFile;
		}
		else if (!"style.css".equals(file)) {
			file = actualModule.getArtifactId() + "/" + file;
		}

		final boolean isForSpringRepo = versionType.equalsIgnoreCase("snapshot") ||  versionType.equalsIgnoreCase("milestone");
		String url = (isForSpringRepo ? ("https://repo.spring.io/" + versionType) : "https://s01.oss.sonatype.org/service/local/repositories/releases/archive")
				+ "/" + actualModule.getGroupId().replace(".", "/")
				+ "/" + actualModule.getArtifactId()
				+ "/" + actualVersion
				+ "/" + actualModule.getArtifactId()
				+ "-" + actualVersion + suffix
				+ (isForSpringRepo ? "!/" : "/!/") + file;

		return url;
	}

	static String moduleToArtifactUrl(String reqUri, String versionType,
			String requestedModuleName, String requestedVersion, Module actualModule,
			String actualVersion,
			int offset, String indexFile, String suffix, String artifactSuffix,
			String rootDirInArtifact) {
		String file = reqUri.substring(offset + requestedModuleName.length() + requestedVersion.length());
		if (file.isEmpty()) {
			file = indexFile;
		}

		final boolean isForSpringRepo = versionType.equalsIgnoreCase("snapshot") || versionType.equalsIgnoreCase("milestone");
		String url = (isForSpringRepo ? ("https://repo.spring.io/" + versionType) : "https://s01.oss.sonatype.org/service/local/repositories/releases/archive")
				+ "/" + actualModule.getGroupId().replace(".", "/")
				+ "/" + actualModule.getArtifactId()
				+ "/" + actualVersion
				+ "/" + actualModule.getArtifactId() + artifactSuffix
				+ "-" + actualVersion + suffix
				+ (isForSpringRepo ? "!/" : "/!/") + rootDirInArtifact + file;

		return url;
	}

	/**
	 * static 404 we send to when a KDoc is requested for a special project+version combination that doesn't have them
	 */
	public static final String WARNING_NO_KDOC = "warningNoKDoc:";
}
