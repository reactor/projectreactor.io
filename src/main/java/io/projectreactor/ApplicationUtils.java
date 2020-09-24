package io.projectreactor;

import java.util.Iterator;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * @author Simon Baslé
 */
public class ApplicationUtils {

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
