/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.projectreactor;

import java.util.ArrayList;
import java.util.List;

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
		return versions;
	}

	public void setVersions(List<String> versions) {
		this.versions = versions;
	}

	public Module addVersion(String version) {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
		}
		this.versions.add(version);
		return this;
	}

	@Override
	public String toString() {
		return "Module{" + "name='" + name + '\'' + ", groupId='" + groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", versions=" + versions + '}';
	}
}
