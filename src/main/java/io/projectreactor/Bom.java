/*
 * Copyright (c) 2011-Present Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.projectreactor;

/**
 * @author Simon Baslé
 */
public class Bom {

	private String type;
	private String name;
	private String coreVersion;
	private String testVersion;
	private String nettyVersion;
	private String extraVersion;
	private String adapterVersion;
	private String rabbitVersion;
	private String kafkaVersion;

	public Bom() {
	}

	public Bom(String type, String name, String coreVersion, String testVersion,
			String nettyVersion, String extraVersion, String adapterVersion,
			String kafkaVersion) {
		this.type = type;
		this.name = name;
		this.coreVersion = coreVersion;
		this.testVersion = testVersion;
		this.nettyVersion = nettyVersion;
		this.extraVersion = extraVersion;
		this.adapterVersion = adapterVersion;
		this.kafkaVersion = kafkaVersion;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCoreVersion() {
		return coreVersion;
	}

	public void setCoreVersion(String coreVersion) {
		this.coreVersion = coreVersion;
	}

	public String getTestVersion() {
		return testVersion;
	}

	public void setTestVersion(String testVersion) {
		this.testVersion = testVersion;
	}

	public String getNettyVersion() {
		return nettyVersion;
	}

	public void setNettyVersion(String nettyVersion) {
		this.nettyVersion = nettyVersion;
	}

	public String getExtraVersion() {
		return extraVersion;
	}

	public void setExtraVersion(String extraVersion) {
		this.extraVersion = extraVersion;
	}

	public String getAdapterVersion() {
		return adapterVersion;
	}

	public void setAdapterVersion(String adapterVersion) {
		this.adapterVersion = adapterVersion;
	}

	public String getKafkaVersion() {
		return kafkaVersion;
	}

	public void setKafkaVersion(String kafkaVersion) {
		this.kafkaVersion = kafkaVersion;
	}

	public String getRabbitVersion() {
		return rabbitVersion;
	}

	public void setRabbitVersion(String rabbitVersion) {
		this.rabbitVersion = rabbitVersion;
	}

	@Override
	public String toString() {
		return "Bom{" + "type='" + type + '\'' + ", name='" + name + '\''
				+ ", coreVersion='" + coreVersion + '\''
				+ ", testVersion='" + testVersion + '\''
				+ ", nettyVersion='" + nettyVersion + '\''
				+ ", extraVersion='" + extraVersion + '\''
				+ ", adapterVersion='" + adapterVersion + '\''
				+ ", rabbitVersion='" + rabbitVersion + '\''
				+ ", kafkaVersion='" + kafkaVersion + '\''
				+ '}';
	}
}
