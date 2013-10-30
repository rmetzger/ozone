package eu.stratosphere.pact.client;

import java.util.Collections;
import java.util.List;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.pact.client.nephele.api.Client;
import eu.stratosphere.pact.client.nephele.api.PlanWithJars;
import eu.stratosphere.pact.common.plan.Plan;

/**
 * Execute a Stratosphere Plan on a (remote) Hadoop Yarn Cluster.
 * 
 * TODO Untested
 *
 */
public class YarnRemoteExecutor implements PlanExecutor {
	
	private Client client;
	private List<String> jarFiles;
	
	public YarnRemoteExecutor(List<String> jarFiles) {
		Configuration conf = CliFrontend.getConfiguration();
		this.client = new Client(conf, true);
		this.jarFiles = jarFiles;

	}

	public YarnRemoteExecutor(String jarFile) {
		this(Collections.singletonList(jarFile));
	}

	@Override
	public long executePlan(Plan plan) throws Exception {
		PlanWithJars p = new PlanWithJars(plan, jarFiles);
		client.run(p, true);
		return 0;
	}
}
