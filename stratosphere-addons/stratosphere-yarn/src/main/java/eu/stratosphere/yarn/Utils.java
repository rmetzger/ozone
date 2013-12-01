package eu.stratosphere.yarn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;

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
	
	public static void setupEnv(Configuration conf, Map<String, String> appMasterEnv) {
		for (String c : conf.getStrings(YarnConfiguration.YARN_APPLICATION_CLASSPATH,YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
			Apps.addToEnvironment(appMasterEnv, Environment.CLASSPATH.name(), c.trim());
		}
		Apps.addToEnvironment(appMasterEnv, Environment.CLASSPATH.name(), Environment.PWD.$() + File.separator + "*");
	}
	
	public static void setupLocalResource(Configuration conf, Path jarPath, LocalResource appMasterJar)
			throws IOException {
		FileStatus jarStat = FileSystem.get(conf).getFileStatus(jarPath);
		appMasterJar.setResource(ConverterUtils.getYarnUrlFromPath(jarPath));
		appMasterJar.setSize(jarStat.getLen());
		appMasterJar.setTimestamp(jarStat.getModificationTime());
		appMasterJar.setType(LocalResourceType.FILE);
		appMasterJar.setVisibility(LocalResourceVisibility.APPLICATION);
	}
	
	
}
