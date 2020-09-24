package io.projectreactor;

import java.io.IOException;
import java.util.Map;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.assertj.core.util.Streams;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

}