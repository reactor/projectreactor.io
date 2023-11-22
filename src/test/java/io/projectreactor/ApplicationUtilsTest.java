/*
 * Copyright (c) 2020-2021 VMware LLC or its affiliates, All Rights Reserved.
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