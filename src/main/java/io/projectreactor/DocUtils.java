package io.projectreactor;

import java.util.Map;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * @author Simon Baslé
 */
public class DocUtils {

	public static Tuple2<Module,String> findModuleAndVersion(Map<String, Module> modules,
			String moduleName, String versionName) {
		Module module = modules.get(moduleName);

		if(module == null){
			return null;
		}

		String version = module.getVersions()
		                       .stream()
		                       .filter(v ->
				                       (versionName.equals("milestone") && v.matches(".*\\.M[0-9]+"))
						                       || v.endsWith(versionName.toUpperCase()))
		                       .findFirst().orElseGet((() -> "NA"));

		if(version.equals("NA")){
			 return null;
		}
		return Tuples.of(module, version);
	}

	public static String findVersionType(String version) {
		String v = version.toUpperCase();
		if (v.endsWith("RELEASE")) return "release";
		if (v.endsWith("SNAPSHOT")) return "snapshot";
		if (v.equals("MILESTONE") || v.matches(".*\\.M[0-9]+")) return "milestone";
		return "release"; //default
	}

	public static String moduleToUrl(String path, String reqUri, String versionType,
			String requestedModuleName, String requestedVersion, Module actualModule,
			String actualVersion) {
		boolean isJavadoc = path.contains("/api/") || path.endsWith("/api");

		int offset = isJavadoc ? 12 : 18;
		String file = reqUri.substring(offset + requestedVersion.length() + requestedModuleName.length());
		if (file.isEmpty()) {
			file = isJavadoc ? "index.html" : "docs/index.html";
		}
		String suffix = isJavadoc ? "-javadoc.jar" : ".zip";

		//tempfix for non generic kafka doc in M1
		boolean isKafkaM1 = actualModule.getArtifactId().contains("kafka") && actualVersion.contains("M1");

		String artifactSuffix = isJavadoc ? "" : "-docs";
		String url = "http://repo.spring.io/" + versionType
				+ "/" + actualModule.getGroupId().replace(".", "/")
				+ "/" + actualModule.getArtifactId() + (isKafkaM1 ? artifactSuffix : "")
				+ "/" + actualVersion
				+ "/" + actualModule.getArtifactId() + artifactSuffix
				+ "-" + actualVersion + suffix
				+ "!/" + file;

		return url;
	}
}
