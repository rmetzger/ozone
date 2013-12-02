/***********************************************************************************************************************
 *
 * Copyright (C) 2013 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.stratosphere.yarn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.util.Records;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import eu.stratosphere.nephele.configuration.ConfigConstants;
import eu.stratosphere.nephele.configuration.GlobalConfiguration;

/**
 * All classes in this package contain code taken from
 * https://github.com/apache/hadoop-common/blob/trunk/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell/src/main/java/org/apache/hadoop/yarn/applications/distributedshell/Client.java?source=cc
 * and
 * https://github.com/hortonworks/simple-yarn-app
 * 
 * The Stratosphere jar is uploaded to HDFS by this client. 
 * The application master and all the TaskManager containers get the jar file downloaded
 * by YARN into their local fs.
 * 
 */
public class Client {
	private static final Log LOG = LogFactory.getLog(Client.class);
	
	/**
	 * Command Line argument options
	 */
	private static final Option VERBOSE = new Option("v","verbose",false, "Verbose debug mode");
	private static final Option STRATOSPHERE_CONF = new Option("c","conf",true, "Path to Stratosphere configuration file");
	private static final Option STRATOSPHERE_JAR = new Option("j","jar",true, "Path to Stratosphere jar file");
	private static final Option JM_MEMORY = new Option("jm","jobManagerMemory",true, "Memory for JobManager Container [in MB]");
	private static final Option TM_MEMORY = new Option("tm","taskManagerMemory",true, "Memory per TaskManager Container [in MB]");
	private static final Option TM_CORES = new Option("tmc","taskManagerCores",true, "Virtual CPU cores per TaskManager");
	private static final Option CONTAINER = new Option("n","container",true, "Number of Yarn container to allocate (=Number of"
			+ " TaskTrackers)");
	
	static {
		CONTAINER.setRequired(true);
	}
	/**
	 * Constants
	 */
	// environment variable names 
	public final static String ENV_TM_MEMORY = "_CLIENT_TM_MEMORY";
	public final static String ENV_TM_CORES = "_CLIENT_TM_CORES";
	public final static String ENV_TM_COUNT = "_CLIENT_TM_COUNT";
	public final static String ENV_APP_ID = "_APP_ID";
	public final static String STRATOSPHERE_JAR_PATH = "_STRATOSPHERE_JAR_PATH"; // the stratosphere jar resource location (in HDFS).
	
	private Configuration conf;

	public void run(String[] args) throws Exception {
		
		//
		//	Command Line Options
		//
		Options options = new Options();
		options.addOption(VERBOSE);
		options.addOption(STRATOSPHERE_CONF);
		options.addOption(STRATOSPHERE_JAR);
		options.addOption(JM_MEMORY);
		options.addOption(TM_MEMORY);
		options.addOption(TM_CORES);
		options.addOption(CONTAINER);
		
		
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch(MissingOptionException moe) {
			System.err.println(moe.getMessage());
			System.out.println("Usage:");
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(200);
			formatter.setLeftPadding(5);
			formatter.setSyntaxPrefix("   Required");
			Options req = new Options();
			req.addOption(CONTAINER);
			formatter.printHelp(" ", req);
			
			formatter.setSyntaxPrefix("   Optional");
			Options opt = new Options();
			opt.addOption(VERBOSE);
			opt.addOption(STRATOSPHERE_CONF);
			opt.addOption(STRATOSPHERE_JAR);
			opt.addOption(JM_MEMORY);
			opt.addOption(TM_MEMORY);
			opt.addOption(TM_CORES);
			formatter.printHelp(" ", opt);
			System.exit(1);
		}
		
		if (System.getProperty("log4j.configuration") == null) {
			Logger root = Logger.getRootLogger();
			root.removeAllAppenders();
			PatternLayout layout = new PatternLayout("%d{HH:mm:ss,SSS} %-5p %-60c %x - %m%n");
			ConsoleAppender appender = new ConsoleAppender(layout, "System.err");
			root.addAppender(appender);
			if(cmd.hasOption(VERBOSE.getOpt())) {
				root.setLevel(Level.DEBUG);
				LOG.debug("CLASSPATH: "+System.getProperty("java.class.path"));
			} else {
				root.setLevel(Level.INFO);
			}
		}
		
		// TM Count
		final int taskManagerCount = Integer.valueOf(cmd.getOptionValue(CONTAINER.getOpt()));
		
		
		// Jar Path
		Path localJarPath;
		if(cmd.hasOption(STRATOSPHERE_JAR.getOpt())) {
			String userPath = cmd.getOptionValue(STRATOSPHERE_JAR.getOpt());
			if(!userPath.startsWith("file://")) {
				userPath = "file://" + userPath;
			}
			localJarPath = new Path(userPath);
		} else {
			localJarPath = new Path("file://"+Client.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		}
		
		// Conf Path 
		Path confPath = null;
		
		if(cmd.hasOption(STRATOSPHERE_CONF.getOpt())) {
			confPath = new Path(cmd.getOptionValue(STRATOSPHERE_CONF.getOpt()));
		} else {
			System.out.println("No configuration file has been specified");
			
			// no configuration path given.
			// -> see if there is one in the current directory
			File currDir = new File(".");
			File[] candidates = currDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name != null && name.endsWith(".yaml");
				}
			});
			if(candidates == null || candidates.length == 0) {
				System.out.println("No configuration file has been found in current directory.\n"
						+ "Copying default.");
				JarFile jar = null;
				try {
					jar = new JarFile(localJarPath.toUri().getPath());
				} catch(FileNotFoundException fne) {
					LOG.fatal("Unable to access jar file. Specify jar file or configuration file.", fne);
					System.exit(1);
				}
				InputStream confStream = jar.getInputStream(jar.getEntry("stratosphere-conf.yaml"));
				
				if(confStream == null) {
					LOG.warn("Given jar file does not contain yaml conf.");
					confStream = this.getClass().getResourceAsStream("stratosphere-conf.yaml"); 
					if(confStream == null) {
						throw new RuntimeException("Unable to find stratosphere-conf in jar file");
					}
				}
				File outFile = new File("stratosphere-conf.yaml");
				if(outFile.exists()) {
					throw new RuntimeException("File unexpectedly exists");
				}
				FileOutputStream outputStream = new FileOutputStream(outFile);
				int read = 0;
				byte[] bytes = new byte[1024];
				while ((read = confStream.read(bytes)) != -1) {
					outputStream.write(bytes, 0, read);
				}
				confStream.close(); outputStream.close(); jar.close();
				confPath = new Path(outFile.toURI());
			} else {
				if(candidates.length > 1) {
					System.out.println("Multiple .yaml configuration files were found in the current directory\n"
							+ "Please specify one explicitly");
					System.exit(1);
				} else if(candidates.length == 1) {
					confPath = new Path(candidates[0].toURI());
				} 
			}
		}
		
		// JobManager Memory
		int jmMemory = 512;
		if(cmd.hasOption(JM_MEMORY.getOpt())) {
			jmMemory = Integer.valueOf(cmd.getOptionValue(JM_MEMORY.getOpt()));
		}
		
		// Task Managers memory
		int tmMemory = 1000;
		if(cmd.hasOption(TM_MEMORY.getOpt())) {
			tmMemory = Integer.valueOf(cmd.getOptionValue(TM_MEMORY.getOpt()));
		}
		
		// Task Managers vcores
		int tmCores = 1;
		if(cmd.hasOption(TM_CORES.getOpt())) {
			tmCores = Integer.valueOf(cmd.getOptionValue(TM_CORES.getOpt()));
		}
		Utils.getStratosphereConfiguration(confPath.toUri().getPath());
		int jmPort = GlobalConfiguration.getInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY, 0);
		if(jmPort == 0) {
			LOG.warn("Unable to find job manager port in configuration!");
			jmPort = ConfigConstants.DEFAULT_JOB_MANAGER_IPC_PORT;
		}

		System.out.println("Using values:");
		System.out.println("\tContainer Count = "+taskManagerCount);
		System.out.println("\tJar Path = "+localJarPath.toUri().getPath());
		System.out.println("\tConfiguration file = "+confPath.toUri().getPath());
		System.out.println("\tJobManager memory = "+jmMemory);
		System.out.println("\tTaskManager memory = "+tmMemory);
		System.out.println("\tTaskManager cores = "+tmCores);

		conf = Utils.initializeYarnConfiguration();
		
		// intialize HDFS
	    LOG.info("Copy App Master jar from local filesystem and add to local environment");
	    // Copy the application master jar to the filesystem 
	    // Create a local resource to point to the destination jar path 
	    FileSystem fs = FileSystem.get(conf);
	    
		
	    // Create yarnClient
		YarnClient yarnClient = YarnClient.createYarnClient();
		yarnClient.init(conf);
		yarnClient.start();

		// Create application via yarnClient
		YarnClientApplication app = yarnClient.createApplication();

		// Set up the container launch context for the application master
		ContainerLaunchContext amContainer = Records
				.newRecord(ContainerLaunchContext.class);
		final String amCommand = "$JAVA_HOME/bin/java"
				+ " -Xmx"+jmMemory+"M" + " eu.stratosphere.yarn.ApplicationMaster" + " "
				+ " 1>"
				+ ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout"
				+ " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR
				+ "/stderr";
		amContainer.setCommands(Collections.singletonList(amCommand));

		System.err.println("amCommand="+amCommand);
		
		// Set-up ApplicationSubmissionContext for the application
		ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
		ApplicationId appId = appContext.getApplicationId();
		
		// Setup jar for ApplicationMaster
		LocalResource appMasterJar = Records.newRecord(LocalResource.class);
		LocalResource stratosphereConf = Records.newRecord(LocalResource.class);
		Path remotePathJar = Utils.setupLocalResource(conf, fs, appId.getId(), localJarPath, appMasterJar);
		Utils.setupLocalResource(conf, fs, appId.getId(), confPath, stratosphereConf);
		
		Map<String, LocalResource> localResources = new HashMap<String, LocalResource>(2);
		localResources.put("stratosphere.jar", appMasterJar);
		localResources.put("stratosphere-conf.yaml", stratosphereConf);
		
				
		amContainer.setLocalResources(localResources);
		

		// Setup CLASSPATH for ApplicationMaster
		Map<String, String> appMasterEnv = new HashMap<String, String>();
		Utils.setupEnv(conf, appMasterEnv);
		// set configuration values
		appMasterEnv.put(Client.ENV_TM_COUNT, String.valueOf(taskManagerCount));
		appMasterEnv.put(Client.ENV_TM_CORES, String.valueOf(tmCores));
		appMasterEnv.put(Client.ENV_TM_MEMORY, String.valueOf(tmMemory));
		appMasterEnv.put(Client.STRATOSPHERE_JAR_PATH, remotePathJar.toString() );
		appMasterEnv.put(Client.ENV_APP_ID, String.valueOf(appId.getId()));
		
		amContainer.setEnvironment(appMasterEnv);

		// Set up resource type requirements for ApplicationMaster
		Resource capability = Records.newRecord(Resource.class);
		capability.setMemory(jmMemory);
		capability.setVirtualCores(1);

		
		appContext.setApplicationName("Stratosphere"); // application name
		appContext.setAMContainerSpec(amContainer);
		appContext.setResource(capability);
		appContext.setQueue("default"); // queue

				
		System.out.println("Submitting application master " + appId);
		yarnClient.submitApplication(appContext);
		
		ApplicationReport appReport = yarnClient.getApplicationReport(appId);
		YarnApplicationState appState = appReport.getYarnApplicationState();
		boolean told = false;
		while (appState != YarnApplicationState.FINISHED
				&& appState != YarnApplicationState.KILLED
				&& appState != YarnApplicationState.FAILED) {
			if(!told && appState ==  YarnApplicationState.RUNNING) {
				System.err.println("JobManager is now running on "+appReport.getHost()+":"+jmPort);
				told = true;
			}
			System.err.println("JobManager is now running on "+appReport.getHost()+":"+jmPort+"\n"
					+ "Application report from ASM: \n" +
			        "\t application identifier: " + appId.toString() + "\n" +
			        "\t appId: " + appId.getId() + "\n" +
			        "\t appDiagnostics: " + appReport.getDiagnostics() + "\n" +
			        "\t appMasterHost: " + appReport.getHost() + "\n" +
			        "\t appQueue: " + appReport.getQueue() + "\n" +
			        "\t appMasterRpcPort: " + appReport.getRpcPort() + "\n" +
			        "\t appStartTime: " + appReport.getStartTime() + "\n" +
			        "\t yarnAppState: " + appReport.getYarnApplicationState() + "\n" +
			        "\t distributedFinalState: " + appReport.getFinalApplicationStatus() + "\n" +
			        "\t appTrackingUrl: " + appReport.getTrackingUrl() + "\n" +
			        "\t appUser: " + appReport.getUser());
			Thread.sleep(5000);
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
