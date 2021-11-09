/*
 * Copyright (c) 2016-2021 VMware Inc. or its affiliates, All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import reactor.util.Logger;
import reactor.util.Loggers;

public class Module {

	private static final Logger LOG = Loggers.getLogger(Module.class);

	final Set<String> badVersions = new LinkedHashSet<>();
	String        name;
	String        groupId;
	String        artifactId;
	List<Version> versions;

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

	/**
	 * @return the version list as Strings
	 * @see #versions() for a {@link Version}-based list
	 */
	public List<String> getVersions() { //for yaml serialization purpose
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
		}
		return versions.stream().map(Version::toString).collect(Collectors.toList());
	}

	public List<Version> versions() {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
		}
		return this.versions;
	}

	public void setVersions(List<String> versions) {
		this.versions = new ArrayList<>(versions.size());
		versions.forEach(this::addVersion);
		sortVersions();
	}

	//to avoid set syntax in YAML we expose List getter/setter
	public List<String> getBadVersions() {
		return new ArrayList<>(this.badVersions);
	}

	public void setBadVersions(List<String> badVersions) {
		this.badVersions.clear();
		badVersions.sort(Comparator.reverseOrder());
		this.badVersions.addAll(badVersions);
	}

	public Module addVersion(String version) {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
		}
		try {
			Version v = Version.parse(version);
			this.versions.add(v);
		}
		catch (IllegalArgumentException e) {
			LOG.warn("Unable to parse and add version {} for module {}: {}", version, name, e.toString());
		}
		return this;
	}

	public Module addVersion(Version version) {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
		}
		this.versions.add(version);
		return this;
	}

	public boolean isBadVersion(String version) {
		return this.badVersions.contains(version);
	}

	public Module sortVersions() {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
			return this;
		}
		this.versions.sort(Comparator.reverseOrder());
		return this;
	}

	public Module sortAndDeduplicateVersions() {
		if (this.versions == null) {
			this.versions = new ArrayList<>(1);
			return this;
		}
		SortedSet<Version> sortedDeduplicated = new TreeSet<>(
				Comparator.reverseOrder());
		sortedDeduplicated.addAll(this.versions);
		this.versions = new ArrayList<>(sortedDeduplicated);
		return this;
	}

	@Override
	public String toString() {
		return "Module{" + "name='" + name + '\'' + ", groupId='" + groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", versions=" + versions + '}';
	}

}
