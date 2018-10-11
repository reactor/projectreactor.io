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

	@Override
	public String toString() {
		return "Bom{" + "type='" + type + '\'' + ", name='" + name + '\''
				+ ", coreVersion='" + coreVersion + '\''
				+ ", testVersion='" + testVersion + '\''
				+ ", nettyVersion='" + nettyVersion + '\''
				+ ", extraVersion='" + extraVersion + '\''
				+ ", adapterVersion='" + adapterVersion + '\''
				+ ", kafkaVersion='" + kafkaVersion + '\''
				+ '}';
	}
}
