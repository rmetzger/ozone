package eu.stratosphere.yarn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.NMClient;
import org.apache.hadoop.yarn.util.Records;

import eu.stratosphere.nephele.jobmanager.JobManager;

public class ApplicationMaster {

	public static class JobManagerRunner extends Thread {
		private String pathToNepheleConfig = "";
		private JobManager jm;
		
		public JobManagerRunner(String pathToNepheleConfig) {
			super("Job manager runner");
			this.pathToNepheleConfig = pathToNepheleConfig;
		}

		public void run() {
			String[] args = {"-executionMode","cluster", "-configDir", pathToNepheleConfig};
			this.jm = JobManager.initialize( args );
		}
		public void shutdown() {
			this.jm.shutdown();
		}
	}
	public static void main(String[] args) throws Exception {
		final int n = Integer.valueOf(args[0]);
		
		JobManagerRunner jmr = new JobManagerRunner("stratosphere-conf.yaml");
		
		System.err.println("Starting JobManager");
		jmr.start();
		
		// Initialize clients to ResourceManager and NodeManagers
		Configuration conf = Utils.initializeYarnConfiguration();

		AMRMClient<ContainerRequest> rmClient = AMRMClient.createAMRMClient();
		rmClient.init(conf);
		rmClient.start();

		NMClient nmClient = NMClient.createNMClient();
		nmClient.init(conf);
		nmClient.start();

		// Register with ResourceManager
		System.out.println("registerApplicationMaster 0");
		rmClient.registerApplicationMaster("", 0, "");
		System.out.println("registerApplicationMaster 1");

		// Priority for worker containers - priorities are intra-application
		Priority priority = Records.newRecord(Priority.class);
		priority.setPriority(0);

		// Resource requirements for worker containers
		Resource capability = Records.newRecord(Resource.class);
		capability.setMemory(512);
		capability.setVirtualCores(1);

		// Make container requests to ResourceManager
		for (int i = 0; i < n; ++i) {
			ContainerRequest containerAsk = new ContainerRequest(capability,
					null, null, priority);
			System.out.println("Making res-req " + i);
			rmClient.addContainerRequest(containerAsk);
		}

		LocalResource stratosphereJar = Records.newRecord(LocalResource.class);
		LocalResource stratosphereConf = Records.newRecord(LocalResource.class);
		Utils.setupLocalResource(conf, new Path("./stratosphere.jar"), stratosphereJar);
		Utils.setupLocalResource(conf, new Path("./stratosphere.jar"), stratosphereConf);
		
		// Obtain allocated containers and launch
		int allocatedContainers = 0;
		while (allocatedContainers < n) {
			AllocateResponse response = rmClient.allocate(0);
			for (Container container : response.getAllocatedContainers()) {
				++allocatedContainers;

				// Launch container by create ContainerLaunchContext
				ContainerLaunchContext ctx = Records
						.newRecord(ContainerLaunchContext.class);
				String tmCommand = "$JAVA_HOME/bin/java " 
						+ "eu.stratosphere.nephele.taskmanager.TaskManager -configDir . "
						+ " 1>"
						+ ApplicationConstants.LOG_DIR_EXPANSION_VAR
						+ "/stdout" 
						+ " 2>"
						+ ApplicationConstants.LOG_DIR_EXPANSION_VAR
						+ "/stderr";
				ctx.setCommands(Collections.singletonList(tmCommand));
				
				System.err.println("TM command="+tmCommand);
				
				// copy resources to the TaskManagers.
				Map<String, LocalResource> localResources = new HashMap<String, LocalResource>(2);
				localResources.put("stratosphere.jar", stratosphereJar);
				localResources.put("stratosphere-conf.yaml", stratosphereConf);
				
				ctx.setLocalResources(localResources);
				
				// Setup CLASSPATH for Container (=TaskTracker)
				Map<String, String> containerEnv = new HashMap<String, String>();
				Utils.setupEnv(conf, containerEnv);
				ctx.setEnvironment(containerEnv);
				
				System.out.println("Launching container " + allocatedContainers);
				nmClient.startContainer(container, ctx);
			}
			Thread.sleep(100);
		}

		// Now wait for containers to complete
		int completedContainers = 0;
		while (completedContainers < n) {
			AllocateResponse response = rmClient.allocate(completedContainers
					/ n);
			for (ContainerStatus status : response.getCompletedContainersStatuses()) {
				++completedContainers;
				System.out.println("Completed container " + completedContainers);
			}
			Thread.sleep(100);
		}

		jmr.shutdown();
		jmr.join(2000L);
		
		// Un-register with ResourceManager
		rmClient.unregisterApplicationMaster(FinalApplicationStatus.SUCCEEDED, "", "");
	}
}
