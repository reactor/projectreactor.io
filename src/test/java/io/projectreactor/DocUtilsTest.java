package io.projectreactor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.util.function.Tuple2;

import static org.assertj.core.api.Assertions.assertThat;

public class DocUtilsTest {

	Map<String, Module> modules;

	@Before
	public void setUp() throws Exception {
		Module module = new Module("test", "group", "id");
		module.addVersion("3.1.0.BUILD-SNAPSHOT")
		      .addVersion("3.1.0.M2");

		Module moduleArchive = new Module("testArchive", "group.old", "id");
		moduleArchive.addVersion("3.1.0.M1")
		             .addVersion("3.0.7.RELEASE")
		             .addVersion("3.0.6.BUILD-SNAPSHOT")
		             .addVersion("3.0.6.RELEASE")
		             .addVersion("3.0.5.RELEASE");

		modules = new HashMap<>(2);
		modules.put(module.getName(), module);
		modules.put(moduleArchive.getName(), moduleArchive);
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
	public void findModuleAndVersionLatestReleaseNoRelease() throws Exception {
		Tuple2<Module, String> result = DocUtils.findModuleAndVersion(modules,
				"test",
				"release");

		assertThat(result).isNull();
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
		assertThat(version).isEqualTo("3.1.0.M2");
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
	public void findVersionType() throws Exception {
		assertThat(DocUtils.findVersionType("3.1.0.BUILD-SNAPSHOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("snAPshOT")).isEqualTo("snapshot");
		assertThat(DocUtils.findVersionType("foosnAPshOT")).isEqualTo("snapshot");

		assertThat(DocUtils.findVersionType("3.1.0.M2")).isEqualTo("milestone");
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
	public void moduleToUrlJavadocNoFile() {
		//TODO
	}

	@Test
	public void moduleToUrlJavadocAndFile() {
		//TODO
	}

	@Test
	public void moduleToUrlNotJavadocNoFile() {
		//TODO
	}

	@Test
	public void moduleToUrlNotJavadocAndFile() {
		//TODO
	}

	@Test
	public void moduleToUrlKafkaM1() {
		//TODO
	}

	@Test
	public void moduleToUrlKafkaNotM1() {
		//TODO
	}

	@Test
	public void moduleToUrlVersionRelease() {
		//TODO
	}

	@Test
	public void moduleToUrlVersionMilestone() {
		//TODO
	}

	@Test
	public void moduleToUrlVersionSnapshot() {
		//TODO
	}

	@Test
	public void moduleToUrlVersionSpecific() {
		//TODO
	}

}