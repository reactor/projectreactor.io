package io.projectreactor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import reactor.util.function.Tuple2;

import static org.assertj.core.api.Assertions.assertThat;

public class DocUtilsTest {

	private Map<String, Module> modules;
	private Module              urlModule;

	@Before
	public void setUp() throws Exception {
		Module module = new Module("test", "group", "artifact");
		module.addVersion("3.4.0")
		      .addVersion("3.4.0-SNAPSHOT")
		      .addVersion("3.4.0-RC1")
		      .addVersion("3.4.0-M2")
		      .addVersion("3.4.0-M3")
		      .addVersion("3.1.0.BUILD-SNAPSHOT")
		      .addVersion("3.1.0.M3")
		      .sortVersions();

		Module moduleArchive = new Module("testArchive", "group.old", "artifact");
		moduleArchive.addVersion("3.1.0.M1")
		             .addVersion("3.0.7.RELEASE")
		             .addVersion("3.0.6.BUILD-SNAPSHOT")
		             .addVersion("3.0.6.RELEASE")
		             .addVersion("3.0.5.RELEASE")
		             .sortVersions();

		modules = new HashMap<>(2);
		modules.put(module.getName(), module);
		modules.put(moduleArchive.getName(), moduleArchive);

		urlModule = new Module("foo", "fooGroup", "fooArtifact")
				.addVersion("1.2.3");
	}

	@Test
	public void findModuleAndVersionNoSuchModule() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules, "foo", "release");

		assertThat(result).isNull();
	}

	@Test
	public void findModuleAndVersionNoSuchVersion() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"foo");

		assertThat(result).isNull();
	}

	@Test
	public void findModuleAndVersionLatestReleaseOldScheme() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"testArchive",
				"release");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("testArchive");
		assertThat(version).isEqualTo("3.0.7.RELEASE");
	}

	@Test
	public void findModuleAndVersionLatestReleaseNewScheme() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"release");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("test");
		assertThat(version).isEqualTo("3.4.0");
	}

	@Test
	public void findModuleAndVersionVersionTypeFallsBackToArchive() throws Exception {
		//for this one we assume a main module that doesn't have any release yet
		Map<String, Module> customModules = new HashMap<>();
		Module limitedNewModule = new Module("test", "group", "artifact");
		limitedNewModule.addVersion("3.4.0-SNAPSHOT").addVersion("3.4.0-RC1");
		customModules.put("test", limitedNewModule);
		customModules.put("testArchive", modules.get("testArchive"));


		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(customModules,
				"test",
				"release");

		assertThat(result).isNotNull();
		assertThat(result.getT2()).isEqualTo("3.0.7.RELEASE");
		assertThat(result.getT1().getName()).isEqualTo("testArchive");
		assertThat(result.getT1().getGroupId()).isEqualTo("group.old");
	}

	@Test
	public void findModuleAndVersionLatestSnapshotOldScheme() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"testArchive",
				"snapshot");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("testArchive");
		assertThat(version).isEqualTo("3.0.6.BUILD-SNAPSHOT");
	}

	@Test
	public void findModuleAndVersionLatestSnapshot() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"snapshot");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("test");
		assertThat(version).isEqualTo("3.4.0-SNAPSHOT");
	}

	@Test
	public void findModuleAndVersionLatestMilestoneOldScheme() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"testArchive",
				"milestone");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("testArchive");
		assertThat(version).isEqualTo("3.1.0.M1");
	}

	@Test
	public void findModuleAndVersionLatestMilestoneNewScheme() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"milestone");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("test");
		assertThat(version).isEqualTo("3.4.0-RC1");
	}

	@Test
	public void findModuleAndVersionSpecificRelease() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"testArchive",
				"3.0.5.RELEASE");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("testArchive");
		assertThat(version).isEqualTo("3.0.5.RELEASE");
	}

	@Test
	public void findModuleAndVersionSpecificReleaseInArchive() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"3.0.5.RELEASE");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("testArchive");
		assertThat(version).isEqualTo("3.0.5.RELEASE");
	}

	@Test
	public void findVersionType() throws Exception {
		assertThat(DocUtils.findVersionType("3.1.0.BUILD-SNAPSHOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("3.4.0-SNAPSHOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("snAPshOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("foosnAPshOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("-SNAPSHOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType(".BUILD-SNAPSHOT")).isEqualTo("snapshot");

		assertThat(DocUtils.findVersionType("3.1.0.M3")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("3.4.0-M3")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("milEstOne")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("anything.M234")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("anything-M234")).isEqualTo("milestone");

		assertThat(DocUtils.findVersionType("3.1.0.RC123")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("3.4.0-RC123")).isEqualTo("milestone");

		//default to RELEASE
		assertThat(DocUtils.findVersionType("prefix.milEstOne")).isEqualTo("release");

		assertThat(DocUtils.findVersionType("3.1.0.RELEASE")).isEqualTo("release");
		assertThat(DocUtils.findVersionType("3.4.0")).isEqualTo("release");
		assertThat(DocUtils.findVersionType("anythingRELEASE")).isEqualTo("release");
		assertThat(DocUtils.findVersionType("reLEAse")).isEqualTo("release");

		//default to release
		assertThat(DocUtils.findVersionType("foobar")).isEqualTo("release");
	}

	@Test
	public void moduleToUrlKdocNoFile() {
		String uri = "/docs/test/release/kdoc-api";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-kdoc.zip!/fooArtifact/index.html");
	}

	@Test
	public void moduleToUrlKdocNoFileFinalSlash() {
		String uri = "/docs/test/release/kdoc-api/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-kdoc.zip!/fooArtifact/index.html");
	}

	@Test
	public void moduleToUrlKdocAndFile() {
		String uri = "/docs/test/release/kdoc-api/some/path/in/Doc.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-kdoc.zip!/fooArtifact/some/path/in/Doc.html");
	}

	@Test
	public void moduleToUrlKdocStyleCss() {
		String uri = "/docs/test/release/kdoc-api/style.css";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-kdoc.zip!/style.css");
	}

	@Test
	public void moduleToUrlJavadocNoFile() {
		String uri = "/docs/test/release/api";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlJavadocNoFileFinalSlash() {
		String uri = "/docs/test/release/api/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlJavadocAndFile() {
		String uri = "/docs/test/release/api/some/path/in/Doc.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-javadoc.jar!/some/path/in/Doc.html");
	}

	@Test
	public void moduleToUrlReferenceNoFile() {
		String uri = "/docs/test/release/reference";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-docs.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlReferenceNoFileFinalSlash() {
		String uri = "/docs/test/release/reference/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-docs.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlReferenceAndFile() {
		String uri = "/docs/test/release/reference/some/absolute/reference/page.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "1.2.3");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3/fooArtifact-1.2.3-docs.zip!/docs/some/absolute/reference/page.html");
	}

	@Test
	public void moduleToUrlReferenceHighlight() {
		String uri = "/docs/core/snapshot/reference/highlight/styles/railscasts.min.css";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "snapshot",
				urlModule, "1.2.3-SNAPSHOT");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"1.2.3-SNAPSHOT/fooArtifact-1.2.3-SNAPSHOT-docs.zip!/docs/highlight/styles/railscasts.min.css");
	}

	@Test
	public void moduleToReferenceUrlKafkaNewDocFilename() {
		String uri = "/docs/kafka/foo/reference";
		Module kafka = new Module("kafka", "group", "kafka.artifact")
				.addVersion("1.2.1.RELEASE");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				"kafka", "foo",
				kafka, "1.2.1.RELEASE");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/kafka.artifact/" +
				"1.2.1.RELEASE/kafka.artifact-1.2.1.RELEASE-docs.zip!/docs/index.html");
	}

	@Test
	public void moduleToReferenceUrlKafkaOldDocFilename() {
		String uri = "/docs/kafka/foo/reference";
		Module kafka = new Module("kafka", "group", "kafka.artifact")
				.addVersion("1.2.0.RELEASE");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				"kafka", "foo",
				kafka, "1.2.0.RELEASE");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/kafka.artifact/" +
				"1.2.0.RELEASE/kafka.artifact-docs-1.2.0.RELEASE.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlVersionRelease() {
		String reqModule = "test";
		String reqVersion = "reLEase";

		String uri = "/docs/" + reqModule + "/" + reqVersion + "/api";
		Tuple2<Module, String> moduleInfo =
				DocUtils.findModuleAndVersion(modules, reqModule, reqVersion);

		assertThat(moduleInfo.getT2()).isEqualTo("3.4.0");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				reqModule, reqVersion,
				moduleInfo.getT1(), moduleInfo.getT2());

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/artifact/3.4.0/artifact-3.4.0-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlVersionMilestone() {
		String reqModule = "test";
		String reqVersion = "mIlEsToNE";

		String uri = "/docs/" + reqModule + "/" + reqVersion + "/api";
		Tuple2<Module, String> moduleInfo =
				DocUtils.findModuleAndVersion(modules, reqModule, reqVersion);

		assertThat(moduleInfo.getT2()).isEqualTo("3.4.0-RC1");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				reqModule, reqVersion,
				moduleInfo.getT1(), moduleInfo.getT2());

		assertThat(url).isEqualTo("https://repo.spring" +
				".io/repoType/group/artifact/3.4.0-RC1/artifact-3.4.0-RC1-javadoc" +
				".jar!/index.html");
	}

	@Test
	public void moduleToUrlVersionSnapshot() {
		String reqModule = "test";
		String reqVersion = "sNAPshOT";

		String uri = "/docs/" + reqModule + "/" + reqVersion + "/api";
		Tuple2<Module, String> moduleInfo =
				DocUtils.findModuleAndVersion(modules, reqModule, reqVersion);

		assertThat(moduleInfo.getT2()).isEqualTo("3.4.0-SNAPSHOT");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				reqModule, reqVersion,
				moduleInfo.getT1(), moduleInfo.getT2());

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/artifact/3.4.0-SNAPSHOT/artifact-3.4.0-SNAPSHOT-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlVersionSpecific() {
		String reqModule = "test";
		String reqVersion = "3.0.5.RElease";

		String uri = "/docs/" + reqModule + "/" + reqVersion + "/api";
		Tuple2<Module, String> moduleInfo =
				DocUtils.findModuleAndVersion(modules, reqModule, reqVersion);

		assertThat(moduleInfo.getT2()).isEqualTo("3.0.5.RELEASE");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				reqModule, reqVersion,
				moduleInfo.getT1(), moduleInfo.getT2());

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/old/artifact/3.0.5.RELEASE/artifact-3.0.5.RELEASE-javadoc.jar!/index.html");
	}

	@Test
	public void moduleReleaseCandidateToRepoMilestone() {
		assertThat(DocUtils.findVersionType("3.1.0.RC2")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("3.4.0-RC2")).isEqualTo("milestone");
	}

	@Test
	public void checkModuleVersionReleaseCandidateIsMilestone() {
		assertThat(DocUtils.checkModuleVersion("3.1.0.RC2", "MILESTONE")).isTrue();
		assertThat(DocUtils.checkModuleVersion("3.4.0-RC2", "MILESTONE")).isTrue();
	}

	@Test
	public void moduleMilestoneToRepoMilestone() {
		assertThat(DocUtils.findVersionType("3.1.0.M1")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("3.4.0-M1")).isEqualTo("milestone");
	}

	@Test
	public void checkModuleVersionMilestoneIsMilestone() {
		assertThat(DocUtils.checkModuleVersion("3.1.0.M1", "MILESTONE")).isTrue();
		assertThat(DocUtils.checkModuleVersion("3.4.0-M1", "MILESTONE")).isTrue();
	}

	@Test
	public void moduleSnapshotToRepoSnapshot() {
		assertThat(DocUtils.findVersionType("3.1.0.BUILD-SNAPSHOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("3.4.0-SNAPSHOT")).isEqualTo("snapshot");
	}

	@Test
	public void checkModuleVersionSnapshotIsSnapshot() {
		assertThat(DocUtils.checkModuleVersion("3.1.0.BUILD-SNAPSHOT", "SNAPSHOT")).isTrue();
		assertThat(DocUtils.checkModuleVersion("3.4.0-SNAPSHOT", "SNAPSHOT")).isTrue();
	}

	@Test
	public void coreCaliforniumAndBelowIsNotKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("core", "3.0.0"))
				.as("3.0.0").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("core", "3.1.0"))
				.as("3.1.0").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("core", "3.2.0"))
				.as("3.2.0").isFalse();
	}

	@Test
	public void coreDysprosiumAndAboveIsKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("core", "3.3.0.M1"))
				.as("3.0.0.M1").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("core", "3.3.1.RELEASE"))
				.as("3.3.1.RELEASE").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("core", "3.4.0.x"))
				.as("3.4.0.x").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("core", "3.6.1.BUILD-SNAPSHOT"))
				.as("3.6.1.BUILD-SNAPSHOT").isTrue();
	}

	@Test
	public void extraCaliforniumAndBelowIsNotIsKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("extra", "3.0.0"))
				.as("3.0.0").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("extra", "3.1.0"))
				.as("3.1.0").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("extra", "3.2.0"))
				.as("3.2.0").isFalse();
	}

	@Test
	public void extraDysprosiumAndAboveIsKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("extra", "3.3.0.M1"))
				.as("3.0.0.M1").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("extra", "3.3.1.RELEASE"))
				.as("3.3.1.RELEASE").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("extra", "3.4.0.x"))
				.as("3.4.0.x").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("extra", "3.6.1.BUILD-SNAPSHOT"))
				.as("3.6.1.BUILD-SNAPSHOT").isTrue();
	}

	@Test
	public void testCaliforniumAndBelowIsNotKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("test", "3.0.0"))
				.as("3.0.0").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("test", "3.1.0"))
				.as("3.1.0").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("test", "3.2.0"))
				.as("3.2.0").isFalse();
	}

	@Test
	public void testDysprosiumAndAboveIsKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("test", "3.3.0.M1"))
				.as("3.0.0.M1").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("test", "3.3.1.RELEASE"))
				.as("3.3.1.RELEASE").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("test", "3.4.0.x"))
				.as("3.4.0.x").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("test", "3.6.1.BUILD-SNAPSHOT"))
				.as("3.6.1.BUILD-SNAPSHOT").isTrue();
	}

	@Test
	public void kotlinExtensionsDysprosiumFirstReleaseIsKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.0.RELEASE"))
				.as("1.0.0.RELEASE").isTrue();
	}

	@Test
	public void kotlinExtensionsDysprosiumAboveFirstReleaseAreNotKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.1.RELEASE"))
				.as("1.0.1.RELEASE").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.2.RELEASE"))
				.as("1.0.2.RELEASE").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.3.RELEASE"))
				.as("1.0.3.RELEASE").isFalse();
	}

	@Test
	public void kotlinExtensionsDysprosiumPreReleasesAreNotKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.0.BUILD-SNAPSHOT"))
				.as("1.0.0.BUILD-SNAPSHOT").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.0.M1"))
				.as("1.0.0.M1").isFalse();
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.0.RC1"))
				.as("1.0.0.RC1").isFalse();
	}

	@Test
	public void isRefGuideOldFormatCore() {
		assertThat(DocUtils.isRefguideOldFormat("core", "3.2.11.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("core", "3.2.12.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("core", "3.3.0.RELEASE")).isTrue();

		assertThat(DocUtils.isRefguideOldFormat("core", "3.2.13.RELEASE")).isFalse();
		assertThat(DocUtils.isRefguideOldFormat("core", "3.3.1.RELEASE")).isFalse();
	}

	@Test
	public void isRefGuideOldFormatNetty() {
		assertThat(DocUtils.isRefguideOldFormat("netty", "0.8.8.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("netty", "0.8.9.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("netty", "0.9.0.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("netty", "0.9.1.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("netty", "0.8.10.BUILD-SNAPSHOT")).isFalse();
		assertThat(DocUtils.isRefguideOldFormat("netty", "0.8.10.RELEASE")).isFalse();
		assertThat(DocUtils.isRefguideOldFormat("netty", "0.9.2.RELEASE")).isFalse();
	}

	@Test
	public void isRefGuideOldFormatKafka() {
		assertThat(DocUtils.isRefguideOldFormat("kafka", "1.0.0.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("kafka", "1.1.0.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("kafka", "1.1.1.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("kafka", "1.2.0.RELEASE")).isTrue();

		assertThat(DocUtils.isRefguideOldFormat("kafka", "1.1.2.RELEASE")).isFalse();
		assertThat(DocUtils.isRefguideOldFormat("kafka", "1.2.1.RELEASE")).isFalse();
	}

	@Test
	public void isRefGuideOldFormatRabbitmq() {
		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.0.0.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.1.0.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.2.0.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.3.0.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.3.1.RELEASE")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.4.0.RC1")).isTrue();
		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.4.0.BUILD-SNAPSHOT")).isTrue();

		assertThat(DocUtils.isRefguideOldFormat("rabbitmq", "1.4.0.RELEASE")).isFalse();
	}

	@Test
	public void getRefDocPathCore() {
		assertThat(DocUtils.getRefDocPath("core", "VERSION")).isEqualTo("/docs/core/VERSION/reference");
	}

	@Test
	public void getRefDocPathTest() {
		assertThat(DocUtils.getRefDocPath("test", "VERSION")).isEqualTo("/docs/core/VERSION/reference/index.html#testing");
	}

	@Test
	public void getRefDocPathKafka() {
		assertThat(DocUtils.getRefDocPath("kafka", "VERSION")).isEqualTo("/docs/kafka/VERSION/reference");
	}

	@Test
	public void getRefDocPathRabbit() {
		assertThat(DocUtils.getRefDocPath("rabbitmq", "VERSION")).isEqualTo("/docs/rabbitmq/VERSION/reference");
	}

	@Test
	public void getRefDocPathNettyDysprosium() {
		assertThat(DocUtils.getRefDocPath("netty", "0.9.1.RELEASE")).isEqualTo("/docs/netty/0.9.1.RELEASE/reference");
	}

	@Test
	public void getRefDocPathNettyPostDysprosium() {
		assertThat(DocUtils.getRefDocPath("netty", "0.10.5.RELEASE")).isEqualTo("/docs/netty/0.10.5.RELEASE/reference");
	}

	@Test
	public void getRefDocPathNettyPreDysprosium() {
		assertThat(DocUtils.getRefDocPath("netty", "0.8.9.RELEASE")).isEmpty();
	}

	@Test
	public void getRefDocPathArbitrary() {
		assertThat(DocUtils.getRefDocPath("foo", "0.8.9.RELEASE")).isEmpty();
	}

}