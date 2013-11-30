package eu.stratosphere.yarn;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

public class Utils {
	
	public static Configuration initializeYarnConfiguration() {
		Configuration conf = new YarnConfiguration();
	
		if (conf.get(YarnConfiguration.RM_ADDRESS, null) == null) {
			String confPath = System.getenv("HADOOP_CONF_PATH");
			if (confPath != null) {
				conf.addResource(confPath);
			}
			if (conf.get(YarnConfiguration.RM_ADDRESS, null) == null) {
				throw new RuntimeException(
						"Configuration Variable for "
								+ YarnConfiguration.RM_ADDRESS
								+ " not set. Please add hadoop configuration directory to classpath"
								+ " or set HADOOP_CONF_PATH.");
			}
		}
		return conf;
	}
}
