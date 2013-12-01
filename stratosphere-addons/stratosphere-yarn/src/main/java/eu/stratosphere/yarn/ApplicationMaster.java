package eu.stratosphere.yarn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static final Log LOG = LogFactory.getLog(ApplicationMaster.class);
	
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
	}
	public static void main(String[] args) throws Exception {
		final int taskManagerCount = Integer.valueOf(args[0]);
		
		final int memoryPerTaskManager = 1000; // what we request from yarn
		
		final int heapLimit = (int)((float)memoryPerTaskManager*0.8); // what we tell the heap
		
//		System.err.println("all envs:");
//	     Map<String, String> env = System.getenv();
//        for (String envName : env.keySet()) {
//            System.out.format("%s=%s%n", envName, env.get(envName));
//        }
//        System.err.println("done envs.");
        
		
		// Initialize clients to ResourceManager and NodeManagers
		Configuration conf = Utils.initializeYarnConfiguration();
		Map<String, String> envs = System.getenv();
		final String currDir = envs.get(Environment.PWD.key());
		final String ownHostname = envs.get(Environment.NM_HOST.key());
		final String localDirs = envs.get(Environment.LOCAL_DIRS.key());
		if(currDir == null) throw new RuntimeException("Current directory unknown");
		if(ownHostname == null) throw new RuntimeException("Own hostname ("+Environment.NM_HOST+") not set.");
		LOG.info("Working directory "+currDir);
		
		// Update yaml conf -> set jobManager address to this machine's address.
		// (I never know how to nicely do file i/o in java.)
		FileInputStream fs = new FileInputStream(currDir+"/stratosphere-conf.yaml");
		BufferedReader br = new BufferedReader(new InputStreamReader(fs));
		Writer output = new BufferedWriter(new FileWriter(currDir+"/stratosphere-conf-modified.yaml"));
		String line ;
		while ( (line = br.readLine()) != null) {
		    if(line.contains("jobmanager.rpc.address")) {
		    	output.append("jobmanager.rpc.address: "+ownHostname+"\n");
//		    } else if(line.contains("taskmanager.heap.mb")) {
//		    	output.append("taskmanager.heap.mb: "+(int)((float)memoryPerTaskManager*0.8)+"\n");
		    } else if(localDirs != null && line.contains("taskmanager.tmp.dirs")) {
		    	output.append("taskmanager.tmp.dirs: "+localDirs+"\n");
		    } else {
		    	output.append(line+"\n");
		    }
		}
		output.close();
		br.close();
		
		JobManagerRunner jmr = new JobManagerRunner(currDir+"/stratosphere-conf-modified.yaml");
		LOG.info("Starting JobManager");
		jmr.start();
		
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
		capability.setMemory(memoryPerTaskManager);
		capability.setVirtualCores(1);

		// Make container requests to ResourceManager
		for (int i = 0; i < taskManagerCount; ++i) {
			ContainerRequest containerAsk = new ContainerRequest(capability,
					null, null, priority);
			System.out.println("Making res-req " + i);
			rmClient.addContainerRequest(containerAsk);
		}

		
		
		LocalResource stratosphereJar = Records.newRecord(LocalResource.class);
		LocalResource stratosphereConf = Records.newRecord(LocalResource.class);

		Utils.setupLocalResource(conf, new Path("file://"+currDir+"/stratosphere.jar"), stratosphereJar);
		Utils.setupLocalResource(conf, new Path("file://"+currDir+"/stratosphere-conf-modified.yaml"), stratosphereConf);
		
		// Obtain allocated containers and launch
		int allocatedContainers = 0;
		while (allocatedContainers < taskManagerCount) {
			AllocateResponse response = rmClient.allocate(0);
			for (Container container : response.getAllocatedContainers()) {
				++allocatedContainers;

				// Launch container by create ContainerLaunchContext
				ContainerLaunchContext ctx = Records
						.newRecord(ContainerLaunchContext.class);
				String tmCommand = "$JAVA_HOME/bin/java -Xmx"+heapLimit+"m " 
						+ " eu.stratosphere.nephele.taskmanager.TaskManager -configDir . "
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
		while (completedContainers < taskManagerCount) {
			AllocateResponse response = rmClient.allocate(completedContainers
					/ taskManagerCount);
			for (ContainerStatus status : response.getCompletedContainersStatuses()) {
				++completedContainers;
				System.out.println("Completed container " + completedContainers);
			}
			Thread.sleep(3000);
		}
		
		jmr.stop(); // I know what I'm doing here.
		
		// Un-register with ResourceManager
		rmClient.unregisterApplicationMaster(FinalApplicationStatus.SUCCEEDED, "", "");
	}
}
