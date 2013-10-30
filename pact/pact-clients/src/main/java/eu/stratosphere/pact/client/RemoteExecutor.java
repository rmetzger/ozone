package eu.stratosphere.pact.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import eu.stratosphere.pact.client.nephele.api.Client;
import eu.stratosphere.pact.client.nephele.api.PlanWithJars;
import eu.stratosphere.pact.common.plan.Plan;

/**
 * Execute a Stratosphere Plan on a (remote) JobManager.
 * This executor establishes a connection to the given JobManager
 * to submit the job.
 *
 */
public class RemoteExecutor implements PlanExecutor {
	
	private Client client;
	private List<String> jarFiles;
	
	public RemoteExecutor(String jobManagerHostname, int port, List<String> jarFiles) {
		this.client = new Client(new InetSocketAddress(jobManagerHostname, port));
		this.jarFiles = jarFiles;

	}

	public RemoteExecutor(String jobManagerHostname, int port, String jarFile) {
		this(jobManagerHostname, port, Collections.singletonList(jarFile));

	}

	@Override
	public long executePlan(Plan plan) throws Exception {
		PlanWithJars p = new PlanWithJars(plan, this.jarFiles);
		return this.client.run(p, true);
	}
}
