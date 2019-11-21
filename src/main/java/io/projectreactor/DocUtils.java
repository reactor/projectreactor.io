package io.projectreactor;

import java.util.Map;
import java.util.function.Function;

import io.netty.handler.codec.http.HttpResponseStatus;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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
		boolean matches = (upperVersionName.equals("MILESTONE") && toCheck.matches(".*\\.M[0-9]+"))
				|| (upperVersionName.equals("MILESTONE") && toCheck.matches(".*\\.RC[0-9]+"))
				|| toCheck.endsWith(upperVersionName);

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
		if (v.equals("MILESTONE") || v.matches(".*\\.M[0-9]+")) return "milestone";
		if (v.matches(".*\\.RC[0-9]+")) return "milestone";
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
			url = moduleToArtifactUrl(reqUri, versionType, requestedModuleName,
					requestedVersion, actualModule, actualVersion,
					18, "index.html", ".zip", "-docs",
					"docs/");
		}
		return url;
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
				//exception for 1.0.0 pre-releases
				//for now all kotlin >= 1.0.0.RELEASE have no kdoc
				//TODO restrict to a version range when kdoc are eventually generated
				return version.equals("1.0.0.RELEASE") || !version.startsWith("1.0.0");
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
				//exception for 1.0.0 pre-releases
				//for now all kotlin >= 1.0.0.RELEASE have no kdoc
				//TODO restrict to a version range when kdoc are eventually generated
				return !version.equals("1.0.0.RELEASE") && version.startsWith("1.0.0");
			default:
				return false;
		}
	}

	static String getRefDocRelativePath(String module, String version) {
		switch (module) {
			case "core":
			case "kafka":
			case "rabbimq":
				return version + "/reference/";
			case "test":
				return version + "/reference/docs/index.html#testing";
			case "netty":
				if (version.startsWith("0.9")) return version + "/reference/";
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

		String url = "https://repo.spring.io/" + versionType
				+ "/" + actualModule.getGroupId().replace(".", "/")
				+ "/" + actualModule.getArtifactId()
				+ "/" + actualVersion
				+ "/" + actualModule.getArtifactId()
				+ "-" + actualVersion + suffix
				+ "!/" + file;

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

		//tempfix for non generic kafka doc in M1
		boolean isKafkaM1 = actualModule.getArtifactId().contains("kafka")
				&& actualVersion.equals("1.0.0.M1");

		String url = "https://repo.spring.io/" + versionType
				+ "/" + actualModule.getGroupId().replace(".", "/")
				+ "/" + actualModule.getArtifactId() + (isKafkaM1 ? artifactSuffix : "")
				+ "/" + actualVersion
				+ "/" + actualModule.getArtifactId() + artifactSuffix
				+ "-" + actualVersion + suffix
				+ "!/" + rootDirInArtifact + file;

		return url;
	}

	static Mono<HttpResponseStatus> validateNewVersion(String requestedModule, String requestedVersion,
			Map<String, Module> modules, Function<String, Mono<Integer>> remoteCheck) {
		if (requestedModule == null || requestedVersion == null
				|| !modules.containsKey(requestedModule)
				|| !Module.VERSION_REGEXP.matcher(requestedVersion).matches()) {
			return Mono.just(HttpResponseStatus.BAD_REQUEST);
		}

		Module module = modules.get(requestedModule);
		if (module.getVersions().contains(requestedVersion)) {
			return Mono.just(HttpResponseStatus.NO_CONTENT);
		}

		//derive URL to javadoc jar
		String versionType = DocUtils.findVersionType(requestedVersion);
		String fakeUrl = "/docs/" + requestedModule + "/" + requestedVersion + "/api/";
		String artifactoryUrl = DocUtils.moduleToUrl(fakeUrl, versionType, requestedModule, requestedVersion, module, requestedVersion);
		artifactoryUrl = artifactoryUrl.substring(0, artifactoryUrl.indexOf('!'));

		LOGGER.debug("notified of release {} {}, will check {}", requestedModule, requestedVersion, artifactoryUrl);

		return remoteCheck.apply(artifactoryUrl)
		                  .map(status -> {
			                  if (status == 200) {
				                  module.addVersion(requestedVersion)
				                        .sortVersions();
				                  return HttpResponseStatus.CREATED;
			                  }
			                  return HttpResponseStatus.FORBIDDEN;
		                  });
	}

	/**
	 * static 404 we send to when a KDoc is requested for a special project+version combination that doesn't have them
	 */
	public static final  String WARNING_NO_KDOC = "warningNoKDoc:";

	private static final Logger LOGGER          = Loggers.getLogger(DocUtils.class);
}
