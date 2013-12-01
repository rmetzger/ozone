package eu.stratosphere.yarn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
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

public class Client {
	
	private static final Option VERBOSE = new Option("v","verbose",false, "Verbose debug mode");
	private static final Option STRATOSPHERE_CONF = new Option("c","conf",true, "Path to Stratosphere configuration file");
	private static final Option STRATOSPHERE_JAR = new Option("j","jar",true, "Path to Stratosphere jar file");
	private static final Option CONTAINER = new Option("c","container",true, "Number of Yarn container to allocate (=Number of"
			+ " TaskTracker");
	
	
	Configuration conf;

	public void run(String[] args) throws Exception {
		
		//
		//	Command Line Options
		//
		OptionGroup optional = new OptionGroup();
		optional.addOption(VERBOSE);
		optional.addOption(STRATOSPHERE_CONF);
		optional.addOption(STRATOSPHERE_JAR);
		optional.setRequired(false);
		
		OptionGroup required = new OptionGroup();
		required.addOption(CONTAINER);
		required.setRequired(true);
		
		Options options = new Options();
		options.addOptionGroup(required);
		options.addOptionGroup(optional);
		
		
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch(MissingOptionException moe) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.setLeftPadding(5);
			formatter.setSyntaxPrefix("Required");
			Options req = new Options();
			req.addOptionGroup(required);
			formatter.printHelp(" ", req);
			
			formatter.setSyntaxPrefix("Optional");
			Options opt = new Options();
			opt.addOptionGroup(optional);
			formatter.printHelp(" ", opt);
			System.exit(1);
		}
		
		if(cmd.hasOption(VERBOSE.getOpt())) {
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
		
		final int taskManagerCount = Integer.valueOf(cmd.getOptionValue(CONTAINER.getOpt()));
		// new Path("file:///home/robert/Projekte/ozone/ozone/stratosphere-dist/target/stratosphere-dist-0.4-SNAPSHOT-jar-with-dependencies.jar");
		Path jarPath;
		if(cmd.hasOption(STRATOSPHERE_JAR.getOpt())) {
			jarPath = new Path(cmd.getOptionValue(STRATOSPHERE_JAR.getOpt()));
		} else {
			jarPath = new Path(Client.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		}
		Path confPath = null;
		if(cmd.hasOption(STRATOSPHERE_CONF.getOpt())) {
			confPath = new Path(cmd.getOptionValue(STRATOSPHERE_CONF.getOpt()));
					//new Path("file:///home/robert/Projekte/ozone/ozone/stratosphere-dist/src/main/stratosphere-bin/conf/stratosphere-conf.yaml");
		} else {
			System.out.println("No configuration file has been specified");
			
			// no configuration path given.
			// -> see if there is one in the current directory
			File currDir = new File("");
			File[] candidates = currDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return dir.equals(dir) && name != null && name.endsWith(".yaml");
				}
			});
			if(candidates.length > 1) {
				System.out.println("Multiple .yaml configuration files were found in the current directory\n"
						+ "Please specify one explicitly");
				System.exit(1);
			} else if(candidates.length == 1) {
				confPath = new Path(candidates[0].toURI());
			} else {
				//assuming candidates == 0
				System.out.println("No configuration file has been found in current directory.\n"
						+ "Copying default.");
				InputStream confStream = this.getClass().getResourceAsStream("stratosphere-conf.yaml");  
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
				confStream.close(); outputStream.close();
				confPath = new Path(outFile.toURI());
		}
		
		System.out.println("Using values:");
		System.out.println("\tContainer Count = "+taskManagerCount);
		System.out.println("\tJar Path = "+jarPath);
		System.out.println("\tConfiguration file = "+confPath);

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
				+ String.valueOf(taskManagerCount) + " 1>"
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
		System.err.println("JobManager is now running on "+appReport.getHost()+":"+6123);
		YarnApplicationState appState = appReport.getYarnApplicationState();
		while (appState != YarnApplicationState.FINISHED
				&& appState != YarnApplicationState.KILLED
				&& appState != YarnApplicationState.FAILED) {
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
