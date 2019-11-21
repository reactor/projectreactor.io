/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.projectreactor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class Module {

	private String name;
	private String groupId;
	private String artifactId;
	private List<String> versions;

	public Module() { }

	public Module(String name, String groupId, String artifactId) {
		this.name = name;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public List<String> getVersions() {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
		}
		return versions;
	}

	public void setVersions(List<String> versions) {
		this.versions = versions;
		sortVersions();
	}

	public Module addVersion(String version) {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
		}
		this.versions.add(version);
		return this;
	}

	public Module sortVersions() {
		this.versions.sort(VERSION_COMPARATOR);
		return this;
	}

	@Override
	public String toString() {
		return "Module{" + "name='" + name + '\'' + ", groupId='" + groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", versions=" + versions + '}';
	}

	static final Pattern VERSION_REGEXP = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[a-zA-Z0-9_-]*");
	static final Comparator<String> VERSION_COMPARATOR = (o1, o2) -> {
		String[] o1Split = o1.split("\\.");
		String[] o2Split = o2.split("\\.");
		boolean o1IsVersion = o1Split.length == 4;
		boolean o2IsVersion = o2Split.length == 4;

		if (o1IsVersion && o2IsVersion) {
			int o1X= Integer.parseInt(o1Split[0]);
			int o1Y = Integer.parseInt(o1Split[1]);
			int o1Z = Integer.parseInt(o1Split[2]);
			String o1Qualifier = o1Split[3];

			int o2X= Integer.parseInt(o2Split[0]);
			int o2Y = Integer.parseInt(o2Split[1]);
			int o2Z = Integer.parseInt(o2Split[2]);
			String o2Qualifier = o2Split[3];

			if (o1X != o2X) {
				return o2X - o1X;
			}
			if (o1Y != o2Y) {
				return o2Y - o1Y;
			}
			if (o1Z != o2Z) {
				return o2Z - o1Z;
			}
			return o2Qualifier.compareTo(o1Qualifier);
		}

		if (!o1IsVersion && !o2IsVersion) return o1.compareTo(o2);
		if (!o1IsVersion) return 1; //put non version o1 at end
		return -1; //put non version o2 at end
	};
}
