package eu.stratosphere.nephele.jobmanager.archive;

import java.util.List;

import eu.stratosphere.nephele.event.job.AbstractEvent;
import eu.stratosphere.nephele.event.job.RecentJobEvent;
import eu.stratosphere.nephele.execution.ExecutionState;
import eu.stratosphere.nephele.jobgraph.JobID;
import eu.stratosphere.nephele.jobgraph.JobStatus;
import eu.stratosphere.nephele.jobgraph.JobVertexID;
import eu.stratosphere.nephele.managementgraph.ManagementGraph;
import eu.stratosphere.nephele.managementgraph.ManagementVertexID;
import eu.stratosphere.nephele.topology.NetworkTopology;

public interface ArchiveListener {
	
	void archiveEvent(JobID jobId, AbstractEvent event);
	void archiveJobevent(JobID jobId, RecentJobEvent event);
	void archiveManagementGraph(JobID jobId, ManagementGraph graph);
	void archiveNetworkTopology(JobID jobId, NetworkTopology topology);
	List<RecentJobEvent> getJobs();
	ManagementGraph getManagementGraph(JobID jobID);
	List<AbstractEvent> getEvents(JobID jobID);
	long getTime(JobID jobID, JobStatus jobStatus);
	long getVertexTime(JobID jobID, ManagementVertexID jobVertexID, ExecutionState executionState);
	RecentJobEvent getJob(JobID JobId);
}
