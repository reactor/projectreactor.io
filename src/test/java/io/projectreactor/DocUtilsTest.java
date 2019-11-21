package io.projectreactor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/fooArtifact/index.html");
	}

	@Test
	public void moduleToUrlKdocNoFileFinalSlash() {
		String uri = "/docs/test/release/kdoc-api/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/fooArtifact/index.html");
	}

	@Test
	public void moduleToUrlKdocAndFile() {
		String uri = "/docs/test/release/kdoc-api/some/path/in/Doc.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/fooArtifact/some/path/in/Doc.html");
	}

	@Test
	public void moduleToUrlKdocStyleCss() {
		String uri = "/docs/test/release/kdoc-api/style.css";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-kdoc.zip!/style.css");
	}

	@Test
	public void moduleToUrlJavadocNoFile() {
		String uri = "/docs/test/release/api";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlJavadocNoFileFinalSlash() {
		String uri = "/docs/test/release/api/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-javadoc.jar!/index.html");
	}

	@Test
	public void moduleToUrlJavadocAndFile() {
		String uri = "/docs/test/release/api/some/path/in/Doc.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-testVersion-javadoc.jar!/some/path/in/Doc.html");
	}

	@Test
	public void moduleToUrlReferenceNoFile() {
		String uri = "/docs/test/release/reference";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-docs-testVersion.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlReferenceNoFileFinalSlash() {
		String uri = "/docs/test/release/reference/";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-docs-testVersion.zip!/docs/index.html");
	}

	@Test
	public void moduleToUrlReferenceAndFile() {
		String uri = "/docs/test/release/reference/some/absolute/reference/page.html";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "release",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
				"testVersion/fooArtifact-docs-testVersion.zip!/docs/some/absolute/reference/page.html");
	}

	@Test
	public void moduleToUrlReferenceHighlight() {
		String uri = "/docs/core/snapshot/reference/highlight/styles/railscasts.min.css";
		String url = DocUtils.moduleToUrl(uri, "repoType",
				"test", "snapshot",
				urlModule, "testVersion");

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/fooGroup/fooArtifact/" +
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

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/kafka.artifact-docs/" +
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

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/kafka.artifact/" +
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

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/old/artifact/3.0.7.RELEASE/artifact-3.0.7.RELEASE-javadoc.jar!/index.html");
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

		assertThat(url).isEqualTo("https://repo.spring" +
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

		assertThat(url).isEqualTo("https://repo.spring.io/repoType/group/artifact/3.1.0.BUILD-SNAPSHOT/artifact-3.1.0.BUILD-SNAPSHOT-javadoc.jar!/index.html");
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
	}

	@Test
	public void checkModuleVersionReleaseCandidateIsMilestone() {
		assertThat(DocUtils.checkModuleVersion("3.1.0.RC2", "MILESTONE")).isTrue();
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
	public void kotlinExtensionsDysprosiumReleaseAndAboveIsKotlinDocSpecial() {
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.0.RELEASE"))
				.as("1.0.0.RELEASE").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.1.BUILD-SNAPSHOT"))
				.as("1.0.1.BUILD-SNAPSHOT").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.1.M1"))
				.as("1.0.1.M1").isTrue();
		assertThat(DocUtils.isKDocSpecialCases("kotlin", "1.0.1.RC1"))
				.as("1.0.1.RC1").isTrue();
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
	public void validateNewVersionEarlyFails() {
		Map<String, Module> modules = new HashMap<>();
		Module module = new Module("example", "org.example", "example-module");
		modules.put(module.getName(), module);

		List<HttpResponseStatus> responses = Flux.concat(
				DocUtils.validateNewVersion(null, "1.1.0.RELEASE", modules, uri -> Mono.error(new IllegalStateException("should not be reached"))),
				DocUtils.validateNewVersion(module.getName(), null, modules, uri -> Mono.error(new IllegalStateException("should not be reached"))),
				DocUtils.validateNewVersion(module.getName() + "BAD", "1.1.0.RELEASE", modules, uri -> Mono.error(new IllegalStateException("should not be reached"))),
				DocUtils.validateNewVersion(module.getName(), "someBadVersion", modules, uri -> Mono.error(new IllegalStateException("should not be reached"))),
				DocUtils.validateNewVersion(module.getName(), "1.3.10b.RELEASE", modules, uri -> Mono.error(new IllegalStateException("should not be reached"))))
		                                         .collectList()
		                                         .block();

		assertThat(responses).allMatch(s -> s == HttpResponseStatus.BAD_REQUEST);
	}

	@Test
	public void validateNewVersionNoContentIfVersionKnown() {
		Map<String, Module> modules = new HashMap<>();
		Module module = new Module("example", "org.example", "example-module");
		module.addVersion("1.1.0.RELEASE")
		      .sortVersions();
		modules.put(module.getName(), module);

		assertThat(DocUtils.validateNewVersion(module.getName(), "1.1.0.RELEASE", modules, uri -> Mono.error(new IllegalStateException("should not be reached"))).block())
				.isEqualTo(HttpResponseStatus.NO_CONTENT);
	}

	@Test
	public void validateNewVersionArtifactoryUrl() {
		Map<String, Module> modules = new HashMap<>();
		Module module = new Module("example", "org.example", "example-module");
		modules.put(module.getName(), module);

		AtomicReference<String> artifactoryUrl = new AtomicReference<>();
		Function<String, Mono<Integer>> remoteCheck = uri -> {
			artifactoryUrl.set(uri);
			return Mono.just(200);
		};

		DocUtils.validateNewVersion(module.getName(), "1.1.0.RELEASE", modules, remoteCheck).block();
		assertThat(artifactoryUrl.get()).as("release")
		                                .isEqualTo("https://repo.spring.io/release/org/example/example-module/1.1.0.RELEASE/example-module-1.1.0.RELEASE-javadoc.jar");

		DocUtils.validateNewVersion(module.getName(), "1.1.0.RC1", modules, remoteCheck).block();
		assertThat(artifactoryUrl.get()).as("rc")
		                                .isEqualTo("https://repo.spring.io/milestone/org/example/example-module/1.1.0.RC1/example-module-1.1.0.RC1-javadoc.jar");

		DocUtils.validateNewVersion(module.getName(), "1.1.0.M3", modules, remoteCheck).block();
		assertThat(artifactoryUrl.get()).as("milestone")
		                                .isEqualTo("https://repo.spring.io/milestone/org/example/example-module/1.1.0.M3/example-module-1.1.0.M3-javadoc.jar");

		DocUtils.validateNewVersion(module.getName(), "1.1.0.BUILD-SNAPSHOT", modules, remoteCheck).block();
		assertThat(artifactoryUrl.get()).as("snapshot")
		                                .isEqualTo("https://repo.spring.io/snapshot/org/example/example-module/1.1.0.BUILD-SNAPSHOT/example-module-1.1.0.BUILD-SNAPSHOT-javadoc.jar");
	}

	@Test
	public void validateNewVersionRemoteCheckOkReturnsCreatedAndAddsSorted() {
		Map<String, Module> modules = new HashMap<>();
		Module module = new Module("example", "org.example", "example-module");
		module.addVersion("1.1.0.RELEASE");
		modules.put(module.getName(), module);

		Function<String, Mono<Integer>> remoteCheck = uri -> Mono.just(200);

		assertThat(DocUtils.validateNewVersion(module.getName(), "1.1.1.RELEASE", modules, remoteCheck).block())
				.isEqualTo(HttpResponseStatus.CREATED);

		assertThat(module.getVersions()).containsExactly("1.1.1.RELEASE", "1.1.0.RELEASE");
	}

	@Test
	public void validateNewVersionRemoteCheckFailsReturnsForbiddenAndNothingAdded() {
		Map<String, Module> modules = new HashMap<>();
		Module module = new Module("example", "org.example", "example-module");
		module.addVersion("1.1.0.RELEASE");
		modules.put(module.getName(), module);

		Function<String, Mono<Integer>> remoteCheck = uri -> Mono.just(201);

		assertThat(DocUtils.validateNewVersion(module.getName(), "1.1.1.RELEASE", modules, remoteCheck).block())
				.isEqualTo(HttpResponseStatus.FORBIDDEN);

		assertThat(module.getVersions()).containsExactly("1.1.0.RELEASE");
	}

}