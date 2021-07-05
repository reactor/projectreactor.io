package io.projectreactor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * @author Simon Baslé
 */
public class ApplicationUtils {

	static boolean checkMaintenanceIsNotOutdated(String maintenanceDate, String maintenanceEnd) {
		Objects.requireNonNull(maintenanceDate, "maintenanceDate");
		Objects.requireNonNull(maintenanceEnd, "maintenanceEnd");

		//try to parse the date, and check if it is outdated
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

		LocalDate mDate = formatter.parse(maintenanceDate).query(LocalDate::from);
		return !mDate.isBefore(LocalDate.now().minusDays(1));
	}

	static HttpHeaders filterRepoProxyRequestHeaders(HttpHeaders headers) {
		Iterator<Map.Entry<String, String>> it = headers.iteratorAsString();
		Map.Entry<String, String> current;
		while (it.hasNext()) {
			current = it.next();
			if (current.getKey()
			           .startsWith("Cf-")) {
				headers.remove(current.getKey());
			}
		}
		return headers;
	}

	static HttpHeaders filterRepoProxyResponseHeaders(HttpHeaders headers) {
		Iterator<Map.Entry<String, String>> it = headers.iteratorAsString();
		Map.Entry<String, String> current;
		while (it.hasNext()) {
			current = it.next();
			String key = current.getKey();
			if (key.startsWith("X-Artifactory") || key.equalsIgnoreCase("X-Node") || key.equalsIgnoreCase(
					"Content-Disposition")) {
				headers.remove(key);
			}
		}
		return headers;
	}
}
