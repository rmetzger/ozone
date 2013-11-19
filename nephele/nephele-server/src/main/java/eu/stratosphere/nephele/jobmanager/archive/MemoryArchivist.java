package eu.stratosphere.nephele.jobmanager.archive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.util.log.Log;

import eu.stratosphere.nephele.event.job.AbstractEvent;
import eu.stratosphere.nephele.event.job.ExecutionStateChangeEvent;
import eu.stratosphere.nephele.event.job.JobEvent;
import eu.stratosphere.nephele.event.job.RecentJobEvent;
import eu.stratosphere.nephele.event.job.VertexEvent;
import eu.stratosphere.nephele.execution.ExecutionState;
import eu.stratosphere.nephele.jobgraph.JobID;
import eu.stratosphere.nephele.jobgraph.JobStatus;
import eu.stratosphere.nephele.jobgraph.JobVertexID;
import eu.stratosphere.nephele.managementgraph.ManagementGraph;
import eu.stratosphere.nephele.managementgraph.ManagementGroupVertex;
import eu.stratosphere.nephele.managementgraph.ManagementGroupVertexID;
import eu.stratosphere.nephele.managementgraph.ManagementVertexID;
import eu.stratosphere.nephele.topology.NetworkTopology;

public class MemoryArchivist implements ArchiveListener {
	
	/**
	 * The map which stores all collected events until they are either
	 * fetched by the client or discarded.
	 */
	private final Map<JobID, List<AbstractEvent>> collectedEvents = new HashMap<JobID, List<AbstractEvent>>();
	
	/**
	 * Map of recently started jobs with the time stamp of the last received job event.
	 */
	private final Map<JobID, RecentJobEvent> oldJobs = new HashMap<JobID, RecentJobEvent>();
	
	/**
	 * Map of management graphs belonging to recently started jobs with the time stamp of the last received job event.
	 */
	private final Map<JobID, ManagementGraph> managementGraphs = new HashMap<JobID, ManagementGraph>();

	/**
	 * Map of network topologies belonging to recently started jobs with the time stamp of the last received job event.
	 */
	private final Map<JobID, NetworkTopology> networkTopologies = new HashMap<JobID, NetworkTopology>();
	
	public void archiveEvent(JobID jobId, AbstractEvent event) {
		
		if(!collectedEvents.containsKey(jobId)) {
			collectedEvents.put(jobId, new ArrayList<AbstractEvent>());
		}
		
		collectedEvents.get(jobId).add(event);
	}
	
	public void archiveJobevent(JobID jobId, RecentJobEvent event) {
		oldJobs.put(jobId, event);
	}
	
	public void archiveManagementGraph(JobID jobId, ManagementGraph graph) {
		
		managementGraphs.put(jobId, graph);
	}
	
	public void archiveNetworkTopology(JobID jobId, NetworkTopology topology) {
		
		networkTopologies.put(jobId, topology);
	}
	
	public List<RecentJobEvent> getJobs() {

		return new ArrayList<RecentJobEvent>(oldJobs.values());
	}
	
	public RecentJobEvent getJob(String JobId) {

		for(JobID id : oldJobs.keySet()) {
			if(JobId.equals(id.toString()))
				return oldJobs.get(id);
		}
		return null;
	}
	
	public ManagementGraph getManagementGraph(final JobID jobID) {

		synchronized (this.managementGraphs) {
			return this.managementGraphs.get(jobID);
		}
	}
	
	public List<AbstractEvent> getEvents(JobID jobID) {
		return collectedEvents.get(jobID);
	}
	
	public long getTime(JobID jobID, JobStatus jobStatus) {
		for(AbstractEvent event : this.getEvents(jobID)) {
			if(event instanceof JobEvent)
			{
				if(((JobEvent) event).getCurrentJobStatus() == jobStatus) {
					return event.getTimestamp();
				}
			}
		}
		return 0;
	}
	
	public long getVertexTime(JobID jobID, ManagementVertexID jobVertexID, ExecutionState executionState) {
		for(AbstractEvent event : this.getEvents(jobID)) {
			if(event instanceof ExecutionStateChangeEvent)
			{
				if(((ExecutionStateChangeEvent) event).getVertexID().equals(jobVertexID) && ((ExecutionStateChangeEvent) event).getNewExecutionState().equals(executionState)) {
					System.out.println("hit "+event.getTimestamp());
					return event.getTimestamp();
				}
			}
		}
		return 0;
	}

}
