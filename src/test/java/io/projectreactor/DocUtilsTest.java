package io.projectreactor;

import java.util.Collections;
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
		module.addVersion("testVersion")
		      .addVersion("3.1.0.BUILD-SNAPSHOT")
		      .addVersion("3.1.0.M3");

		Module moduleArchive = new Module("testArchive", "group.old", "artifact");
		moduleArchive.addVersion("3.1.0.M1")
		             .addVersion("3.0.7.RELEASE")
		             .addVersion("3.0.6.BUILD-SNAPSHOT")
		             .addVersion("3.0.6.RELEASE")
		             .addVersion("3.0.5.RELEASE");

		modules = new HashMap<>(2);
		modules.put(module.getName(), module);
		modules.put(moduleArchive.getName(), moduleArchive);

		urlModule = new Module("foo", "fooGroup", "fooArtifact")
				.addVersion("testVersion");
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
	public void findModuleAndVersionLatestRelease() throws Exception {
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
	public void findModuleAndVersionLatestReleaseFallsBackToArchive() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"release");

		assertThat(result).isNotNull();
		assertThat(result.getT2()).isEqualTo("3.0.7.RELEASE");
		assertThat(result.getT1().getName()).isEqualTo("testArchive");
		assertThat(result.getT1().getGroupId()).isEqualTo("group.old");
	}

	@Test
	public void findModuleAndVersionLatestSnapshot() throws Exception {
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
	public void findModuleAndVersionLatestSnapshot2() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"snapshot");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("test");
		assertThat(version).isEqualTo("3.1.0.BUILD-SNAPSHOT");
	}

	@Test
	public void findModuleAndVersionLatestMilestone() throws Exception {
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
	public void findModuleAndVersionLatestMilestone2() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"milestone");

		assertThat(result).isNotNull();

		Module module = result.getT1();
		String version = result.getT2();

		assertThat(module.getName()).isEqualTo("test");
		assertThat(version).isEqualTo("3.1.0.M3");
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
		assertThat(DocUtils.findVersionType("snAPshOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("foosnAPshOT")).isEqualTo("snapshot");

		assertThat(DocUtils.findVersionType("3.1.0.M3")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("milEstOne")).isEqualTo("milestone");
		assertThat(DocUtils.findVersionType("anything.M234")).isEqualTo("milestone");
		//default to RELEASE
		assertThat(DocUtils.findVersionType("prefix.milEstOne")).isEqualTo("release");

		assertThat(DocUtils.findVersionType("3.1.0.RELEASE")).isEqualTo("release");
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
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/fooArtifact/index.html");
	}

	@Test
	public void moduleToUrlKdocNoFileFinalSlash() {
		String uri = "/docs/test/release/kdoc-api/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/fooArtifact/index.html");
	}

	@Test
	public void moduleToUrlKdocAndFile() {
		String uri = "/docs/test/release/kdoc-api/some/path/in/Doc.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/fooArtifact/some/path/in/Doc.html");
	}

	@Test
	public void moduleToUrlKdocStyleCss() {
		String uri = "/docs/test/release/kdoc-api/style.css";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/style.css");
	}

	@Test
	public void moduleToUrlJavadocNoFile() {
		String uri = "/docs/test/release/api";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlJavadocNoFileFinalSlash() {
		String uri = "/docs/test/release/api/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlJavadocAndFile() {
		String uri = "/docs/test/release/api/some/path/in/Doc.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-javadoc.jar!/some/path/in/Doc.html");
	}

	@Test
	public void moduleToUrlReferenceNoFile() {
		String uri = "/docs/test/release/reference";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-docs-testVersion.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlReferenceNoFileFinalSlash() {
		String uri = "/docs/test/release/reference/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-docs-testVersion.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlReferenceAndFile() {
		String uri = "/docs/test/release/reference/some/absolute/reference/page.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-docs-testVersion.zip!/docs/some/absolute/reference/page.html");
	}

	@Test
	public void moduleToUrlReferenceHighlight() {
		String uri = "/docs/core/snapshot/reference/highlight/styles/railscasts.min.css";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "snapshot",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-docs-testVersion.zip!/docs/highlight/styles/railscasts.min.css");
	}

	@Test
	public void moduleToReferenceUrlKafkaM1() {
		String uri = "/docs/test/foo/reference";
		Module kafka = new Module("kafka", "group", "kafka.artifact")
				.addVersion("1.0.0.M1");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "foo",
				kafka, "1.0.0.M1");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/group/kafka.artifact-docs/" +
				"1.0.0.M1/kafka.artifact-docs-1.0.0.M1.zip!/docs/index.html");
	}

	@Test
	public void moduleToReferenceUrlKafkaNotM1() {
		String uri = "/docs/test/foo/reference";
		Module kafka = new Module("kafka", "group", "kafka.artifact")
				.addVersion("notM1");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "foo",
				kafka, "notM1");

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/group/kafka.artifact/" +
				"notM1/kafka.artifact-docs-notM1.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlVersionRelease() {
		String reqModule = "test";
		String reqVersion = "reLEase";

		String uri = "/docs/" + reqModule + "/" + reqVersion + "/api";
		Tuple2<Module, String> moduleInfo =
				DocUtils.findModuleAndVersion(modules, reqModule, reqVersion);

		assertThat(moduleInfo.getT2()).isEqualTo("3.0.7.RELEASE");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				reqModule, reqVersion,
				moduleInfo.getT1(), moduleInfo.getT2());

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/group/old/artifact/3.0.7.RELEASE/artifact-3.0.7.RELEASE-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlVersionMilestone() {
		String reqModule = "test";
		String reqVersion = "mIlEsToNE";

		String uri = "/docs/" + reqModule + "/" + reqVersion + "/api";
		Tuple2<Module, String> moduleInfo =
				DocUtils.findModuleAndVersion(modules, reqModule, reqVersion);

		assertThat(moduleInfo.getT2()).isEqualTo("3.1.0.M3");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				reqModule, reqVersion,
				moduleInfo.getT1(), moduleInfo.getT2());

		assertThat(url).isEqualTo("http://repo.spring" +
				".io/repoType/group/artifact/3.1.0.M3/artifact-3.1.0.M3-javadoc" +
				".jar!/index.html");
	}

	@Test
	public void moduleToUrlVersionSnapshot() {
		String reqModule = "test";
		String reqVersion = "sNAPshOT";

		String uri = "/docs/" + reqModule + "/" + reqVersion + "/api";
		Tuple2<Module, String> moduleInfo =
				DocUtils.findModuleAndVersion(modules, reqModule, reqVersion);

		assertThat(moduleInfo.getT2()).isEqualTo("3.1.0.BUILD-SNAPSHOT");

		String url = DocUtils.moduleToUrl(uri, "repoType",
				reqModule, reqVersion,
				moduleInfo.getT1(), moduleInfo.getT2());

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/group/artifact/3.1.0.BUILD-SNAPSHOT/artifact-3.1.0.BUILD-SNAPSHOT-javadoc.jar!/index.html");
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

		assertThat(url).isEqualTo("http://repo.spring.io/repoType/group/old/artifact/3.0.5.RELEASE/artifact-3.0.5.RELEASE-javadoc.jar!/index.html");
	}

	@Test
	public void moduleReleaseCandidateToRepoMilestone() {
		assertThat(DocUtils.findVersionType("3.1.0.RC2")).isEqualTo("milestone");
	}

	@Test
	public void checkModuleVersionReleaseCandidateIsMilestone() {
		assertThat(DocUtils.checkModuleVersion("3.1.0.RC2", "MILESTONE")).isTrue();
	}

}