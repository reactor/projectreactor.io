package io.projectreactor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.assertj.core.util.Streams;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Simon Baslé
 */
public class ApplicationUtilsTest {

	@Test
	public void filterRepoProxyRequestHeaders() {
		DefaultHttpHeaders headers = new DefaultHttpHeaders();
		headers.add("Cf-Example", "remove");
		headers.add("CfExample", "keep");
		headers.add("Some-Cf-Example", "keep");

		ApplicationUtils.filterRepoProxyRequestHeaders(headers);

		assertThat(Streams.stream(headers)).allMatch(entry -> entry.getValue().equals("keep")).hasSize(2);
	}

	@Test
	public void filterRepoProxyResponseHeaders() {
		DefaultHttpHeaders headers = new DefaultHttpHeaders();
		headers.add("X-Artifactory-Example", "remove");
		headers.add("X-Node", "remove");
		headers.add("Content-Disposition", "remove");
		headers.add("X-Checksum", "keep");
		headers.add("X-Example", "keep");
		headers.add("Some-Example", "keep");

		ApplicationUtils.filterRepoProxyResponseHeaders(headers);

		assertThat(Streams.stream(headers)).allMatch(entry -> entry.getValue().equals("keep")).hasSize(3);
	}

	@Test
	public void checkMaintenance_startNull() {
		String maintenanceDate = null;
		String maintenanceEnd = "example";

		assertThatNullPointerException()
				.isThrownBy(() -> ApplicationUtils.checkMaintenanceIsNotOutdated(maintenanceDate, maintenanceEnd))
				.withMessage("maintenanceDate");
	}

	@Test
	public void checkMaintenance_endNull() {
		String maintenanceDate = "example";
		String maintenanceEnd = null;

		assertThatNullPointerException()
				.isThrownBy(() -> ApplicationUtils.checkMaintenanceIsNotOutdated(maintenanceDate, maintenanceEnd))
				.withMessage("maintenanceEnd");
	}

	@Test
	public void checkMaintenance_parsedToday() {
		String maintenanceDate = DateTimeFormatter.ofPattern("yyyy/MM/dd")
				.format(LocalDate.now());
		String maintenanceEnd = "";

		boolean check = ApplicationUtils.checkMaintenanceIsNotOutdated(maintenanceDate, maintenanceEnd);

		assertThat(check).as("acceptable").isTrue();
	}

	@Test
	public void checkMaintenance_parsedYesterday() {
		String maintenanceDate = DateTimeFormatter.ofPattern("yyyy/MM/dd")
				.format(LocalDate.now().minusDays(1));
		String maintenanceEnd = "";

		boolean check = ApplicationUtils.checkMaintenanceIsNotOutdated(maintenanceDate, maintenanceEnd);

		assertThat(check).as("acceptable").isTrue();
	}

	@Test
	public void checkMaintenance_parsedOlder() {
		String maintenanceDate = DateTimeFormatter.ofPattern("yyyy/MM/dd")
				.format(LocalDate.now().minusDays(2));
		String maintenanceEnd = "";

		boolean check = ApplicationUtils.checkMaintenanceIsNotOutdated(maintenanceDate, maintenanceEnd);

		assertThat(check).as("acceptable").isFalse();
	}

	@Test
	public void checkMaintenance_unparseableThrows() {
		String maintenanceDate = "example";
		String maintenanceEnd = "";

		assertThatExceptionOfType(DateTimeParseException.class)
				.isThrownBy(() -> ApplicationUtils.checkMaintenanceIsNotOutdated(maintenanceDate, maintenanceEnd))
				.withMessage("Text 'example' could not be parsed at index 0");
	}

}