package eu.stratosphere.pact.test.localDistributed;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.stratosphere.nephele.client.AbstractJobResult.ReturnCode;
import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.client.JobSubmissionResult;
import eu.stratosphere.nephele.configuration.ConfigConstants;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.configuration.GlobalConfiguration;
import eu.stratosphere.nephele.instance.local.LocalTaskManagerThread;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.nephele.jobmanager.JobManager;
import eu.stratosphere.nephele.jobmanager.JobManager.ExecutionMode;
import eu.stratosphere.pact.client.minicluster.NepheleMiniCluster;
import eu.stratosphere.pact.common.contract.FileDataSink;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.compiler.DataStatistics;
import eu.stratosphere.pact.compiler.PactCompiler;
import eu.stratosphere.pact.compiler.plan.candidate.OptimizedPlan;
import eu.stratosphere.pact.compiler.plantranslate.NepheleJobGraphGenerator;
import eu.stratosphere.pact.compiler.util.DummyOutputFormat;

public class LocalDistributedExecutor  {
	private static int NUM_TASKMANAGERS = 2; // + 1 from the regular local mode
	
	private static int JOBMANAGER_RPC_PORT = 6498;
	
	public static class JobManagerThread extends Thread {
		JobManager jm;
		
		public JobManagerThread(JobManager jm) {
			this.jm = jm;
		}
		@Override
		public void run() {
			jm.runTaskLoop();
		//	jm.shutdown();
		}
	}
	public void run(Plan plan) throws Exception {
		Configuration conf = NepheleMiniCluster.getMiniclusterDefaultConfig(JOBMANAGER_RPC_PORT, 6500,
				7501, 7533, null, true);
		GlobalConfiguration.includeConfiguration(conf);
			
		// start job manager
		JobManager jobManager;
		try {
			jobManager = new JobManager(ExecutionMode.CLUSTER);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
	
		
		JobManagerThread jobManagerThread = new JobManagerThread(jobManager);
		jobManagerThread.setDaemon(true);
		jobManagerThread.start();
		
		List<LocalTaskManagerThread> tms = new ArrayList<LocalTaskManagerThread>();
		for(int tm = 0; tm < NUM_TASKMANAGERS; tm++) {
			Configuration tmConf = new Configuration();
			tmConf.setInteger(ConfigConstants.TASK_MANAGER_IPC_PORT_KEY,
						ConfigConstants.DEFAULT_TASK_MANAGER_IPC_PORT+tm+100);
			tmConf.setInteger(ConfigConstants.TASK_MANAGER_DATA_PORT_KEY, ConfigConstants.DEFAULT_TASK_MANAGER_DATA_PORT+tm); // taskmanager.data.port
			System.err.println("new conf "+tmConf);
			GlobalConfiguration.includeConfiguration(tmConf);
			
			System.err.println("Setting new Port "+GlobalConfiguration.getConfiguration());
			LocalTaskManagerThread t = new LocalTaskManagerThread();
			t.start();
			tms.add(t);
		}
		
		try {
			Thread.sleep(1000*3);  // Sleep for 5 secs
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		PactCompiler pc = new PactCompiler(new DataStatistics());
		OptimizedPlan op = pc.compile(plan);
		
		NepheleJobGraphGenerator jgg = new NepheleJobGraphGenerator();
		JobGraph jobGraph = jgg.compileJobGraph(op);
	
		try {
			JobClient jobClient = getJobClient(jobGraph);
			
			JobSubmissionResult res = jobClient.submitJob();
			if( res.getReturnCode() == ReturnCode.ERROR) {
				System.err.println("Job submission/execution failed with message: "+res.getDescription());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public JobClient getJobClient(JobGraph jobGraph) throws Exception {
		Configuration configuration = jobGraph.getJobConfiguration();
		configuration.setString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY, "localhost");
		configuration.setInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY, JOBMANAGER_RPC_PORT);
		return new JobClient(jobGraph, configuration);
	}
	
	public static void main(String args[]) throws Exception {
		LocalDistributedExecutor lde = new LocalDistributedExecutor();
		
		FileDataSink sink = new FileDataSink(DummyOutputFormat.class, "file:///tmp/test", "file sink");
		Plan plan = new Plan(sink, "Branching Plans With Multiple Data Sinks");
		lde.run(plan);
	}
}
