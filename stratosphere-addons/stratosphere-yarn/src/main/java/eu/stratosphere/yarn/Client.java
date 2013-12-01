package eu.stratosphere.yarn;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Client {

	Configuration conf;

	public void run(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("stratosphere-yarn.jar <pathToStratosphere.yaml> <NumberOfContainers>");
		}
		
		if (args.length == 2) {
			if (System.getProperty("log4j.configuration") == null) {
				Logger root = Logger.getRootLogger();
				root.removeAllAppenders();
				PatternLayout layout = new PatternLayout(
						"%d{HH:mm:ss,SSS} %-5p %-60c %x - %m%n");
				ConsoleAppender appender = new ConsoleAppender(layout,
						"System.err");
				root.addAppender(appender);
				root.setLevel(Level.DEBUG);
			}
		}
		final int n = Integer.valueOf(args[0]);
		final Path jarPath = new Path("file:///home/robert/Projekte/ozone/ozone/stratosphere-dist/target/stratosphere-dist-0.4-SNAPSHOT-jar-with-dependencies.jar");
				//new Path(Client.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		final Path confPath = new Path("file:///home/robert/Projekte/ozone/ozone/stratosphere-dist/src/main/stratosphere-bin/conf/stratosphere-conf.yaml");
		
		System.err.println("jarPath = " + jarPath);

		// Create yarnClient
		conf = Utils.initializeYarnConfiguration();
		
		YarnClient yarnClient = YarnClient.createYarnClient();
		yarnClient.init(conf);
		yarnClient.start();

		// Create application via yarnClient
		YarnClientApplication app = yarnClient.createApplication();

		// Set up the container launch context for the application master
		ContainerLaunchContext amContainer = Records
				.newRecord(ContainerLaunchContext.class);
		final String amCommand = "$JAVA_HOME/bin/java"
				+ " -Xmx256M" + " eu.stratosphere.yarn.ApplicationMaster" + " "
				+ String.valueOf(n) + " 1>"
				+ ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout"
				+ " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR
				+ "/stderr";
		amContainer.setCommands(Collections.singletonList(amCommand));

		System.err.println("amCommand="+amCommand);
		
		// Setup jar for ApplicationMaster
		LocalResource appMasterJar = Records.newRecord(LocalResource.class);
		LocalResource stratosphereConf = Records.newRecord(LocalResource.class);
		Utils.setupLocalResource(conf, jarPath, appMasterJar);
		Utils.setupLocalResource(conf, confPath, stratosphereConf);
		
		Map<String, LocalResource> localResources = new HashMap<String, LocalResource>(2);
		localResources.put("stratosphere.jar", appMasterJar);
		localResources.put("stratosphere-conf.yaml", stratosphereConf);
		
		amContainer.setLocalResources(localResources);

		// Setup CLASSPATH for ApplicationMaster
		Map<String, String> appMasterEnv = new HashMap<String, String>();
		Utils.setupEnv(conf, appMasterEnv);
		amContainer.setEnvironment(appMasterEnv);

		// Set up resource type requirements for ApplicationMaster
		Resource capability = Records.newRecord(Resource.class);
		capability.setMemory(256);
		capability.setVirtualCores(1);

		// Finally, set-up ApplicationSubmissionContext for the application
		ApplicationSubmissionContext appContext = app
				.getApplicationSubmissionContext();
		appContext.setApplicationName("Stratosphere"); // application name
		appContext.setAMContainerSpec(amContainer);
		appContext.setResource(capability);
		appContext.setQueue("default"); // queue

		// Submit application
		ApplicationId appId = appContext.getApplicationId();
		System.out.println("Submitting application " + appId);
		yarnClient.submitApplication(appContext);

		ApplicationReport appReport = yarnClient.getApplicationReport(appId);
		YarnApplicationState appState = appReport.getYarnApplicationState();
		while (appState != YarnApplicationState.FINISHED
				&& appState != YarnApplicationState.KILLED
				&& appState != YarnApplicationState.FAILED) {
			Thread.sleep(3000);
			appReport = yarnClient.getApplicationReport(appId);
			appState = appReport.getYarnApplicationState();
		}

		System.out.println("Application " + appId + " finished with"
				+ " state " + appState + " at " + appReport.getFinishTime());

	}


	public static void main(String[] args) throws Exception {
		Client c = new Client();
		c.run(args);
	}
}
