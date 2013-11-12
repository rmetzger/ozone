package eu.stratosphere.nephele.jobmanager.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.io.EofException;

import eu.stratosphere.nephele.event.job.RecentJobEvent;
import eu.stratosphere.nephele.execution.ExecutionState;
import eu.stratosphere.nephele.jobgraph.JobStatus;
import eu.stratosphere.nephele.jobmanager.JobManager;
import eu.stratosphere.nephele.managementgraph.ManagementGraph;
import eu.stratosphere.nephele.managementgraph.ManagementGroupVertex;
import eu.stratosphere.nephele.managementgraph.ManagementVertex;
import eu.stratosphere.nephele.util.StringUtils;

public class JobmanagerInfoServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * The log for this class.
	 */
	private static final Log LOG = LogFactory.getLog(JobmanagerInfoServlet.class);
	
	/**
	 * Underlying JobManager
	 */
	private final JobManager jobmanager;
	
	public JobmanagerInfoServlet(JobManager jobmanager) {
		this.jobmanager = jobmanager;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("application/json");
			
			try {
				if("archive".equals(req.getParameter("get"))) {
					writeJsonForArchive(resp.getWriter(), jobmanager.getOldJobs());
				}
				else if("job".equals(req.getParameter("get"))) {
					String jobId = req.getParameter("job");
					writeJsonForArchivedJob(resp.getWriter(), jobmanager.getArchive().getJob(jobId));
				}
				else{
					writeJsonForJobs(resp.getWriter(), jobmanager.getRecentJobs());
				}
				
			} catch (Exception e) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.getWriter().print(e.getMessage());
				if (LOG.isWarnEnabled()) {
					LOG.warn(StringUtils.stringifyException(e));
				}
			}
	}
	
	private void writeJsonForJobs(PrintWriter wrt, List<RecentJobEvent> jobs) {
		
		try {
		
		wrt.write("[");
		
		// Loop Jobs
		for (int i = 0; i < jobs.size(); i++) {
			RecentJobEvent jobEvent = jobs.get(i);
			ManagementGraph jobManagementGraph = jobmanager.getManagementGraph(jobEvent.getJobID());
			
			//Serialize job to json
			wrt.write("{");
			wrt.write("\"jobid\": \"" + jobEvent.getJobID() + "\",");
			wrt.write("\"jobname\": \"" + jobEvent.getJobName()+"\",");
			wrt.write("\"status\": \""+ jobEvent.getJobStatus() + "\",");
			wrt.write("\"time\": " + jobEvent.getTimestamp()+",");
			
			// Serialize ManagementGraph to json
			wrt.write("\"groupvertices\": [");
			boolean first = true;
			for(ManagementGroupVertex groupVertex : jobManagementGraph.getGroupVerticesInTopologicalOrder()) {
				//Write seperator between json objects
				if(first) {
					first = false;
				} else {
					wrt.write(","); }
				
				wrt.write(groupVertex.toJson());
			}
			wrt.write("]");
			wrt.write("}");
			
			//Write seperator between json objects
			if(i != jobs.size() - 1) {
				wrt.write(",");
			}
		}
		wrt.write("]");
		
		} catch (EofException eof) { // Connection closed by client
			LOG.info("Info server for jobmanager: Connection closed by client, EofException");
		} catch (IOException ioe) { // Connection closed by client	
			LOG.info("Info server for jobmanager: Connection closed by client, IOException");
		} 
		
	}
	
	private void writeJsonForArchive(PrintWriter wrt, List<RecentJobEvent> jobs) {
		
		wrt.write("[");
		
		// Loop Jobs
		for (int i = 0; i < jobs.size(); i++) {
			RecentJobEvent jobEvent = jobs.get(i);
			
			//Serialize job to json
			wrt.write("{");
			wrt.write("\"jobid\": \"" + jobEvent.getJobID() + "\",");
			wrt.write("\"jobname\": \"" + jobEvent.getJobName()+"\",");
			wrt.write("\"status\": \""+ jobEvent.getJobStatus() + "\",");
			wrt.write("\"time\": " + jobEvent.getTimestamp());
			
			wrt.write("}");
			
			//Write seperator between json objects
			if(i != jobs.size() - 1) {
				wrt.write(",");
			}
		}
		wrt.write("]");
		
	}
	
	private void writeJsonForArchivedJob(PrintWriter wrt, RecentJobEvent jobEvent) {
		
		try {
		
			
			
			wrt.write("[");
		
			ManagementGraph jobManagementGraph = jobmanager.getManagementGraph(jobEvent.getJobID());
			
			//Serialize job to json
			wrt.write("{");
			wrt.write("\"jobid\": \"" + jobEvent.getJobID() + "\",");
			wrt.write("\"jobname\": \"" + jobEvent.getJobName()+"\",");
			wrt.write("\"status\": \""+ jobEvent.getJobStatus() + "\",");
			wrt.write("\"FINISHED\": \""+ jobmanager.getArchive().getTime(jobEvent.getJobID(), JobStatus.FINISHED) + "\",");
			wrt.write("\"SCHEDULED\": \""+ jobmanager.getArchive().getTime(jobEvent.getJobID(), JobStatus.SCHEDULED) + "\",");
			wrt.write("\"RUNNING\": \""+ jobmanager.getArchive().getTime(jobEvent.getJobID(), JobStatus.RUNNING) + "\",");
			wrt.write("\"FAILED\": \""+ jobmanager.getArchive().getTime(jobEvent.getJobID(), JobStatus.FAILED) + "\",");
			wrt.write("\"CANCELED\": \""+ jobmanager.getArchive().getTime(jobEvent.getJobID(), JobStatus.CANCELED) + "\",");
			wrt.write("\"CREATED\": " + jobmanager.getArchive().getTime(jobEvent.getJobID(), JobStatus.CREATED)+",");
			
			// Serialize ManagementGraph to json
			wrt.write("\"groupvertices\": [");
			boolean first = true;
			for(ManagementGroupVertex groupVertex : jobManagementGraph.getGroupVerticesInTopologicalOrder()) {
				//Write seperator between json objects
				if(first) {
					first = false;
				} else {
					wrt.write(","); }
				
				wrt.write(groupVertex.toJson());
				
			}
			wrt.write("],");
			
			wrt.write("\"verticetimes\": {");
			first = true;
			for(ManagementGroupVertex groupVertex : jobManagementGraph.getGroupVerticesInTopologicalOrder()) {
				
				for(int j = 0; j < groupVertex.getNumberOfGroupMembers(); j++) {
					ManagementVertex vertex = groupVertex.getGroupMember(j);
					
					if(first) {
						first = false;
					} else {
						wrt.write(","); }
					
					wrt.write(vertex.getID()+": {");
					wrt.write("\"vertexid\": \"" + vertex.getID() + "\",");
					wrt.write("\"vertexname\": \"" + vertex + "\",");
					wrt.write("\"CREATED\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.CREATED) + "\",");
					wrt.write("\"SCHEDULED\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.SCHEDULED) + "\",");
					wrt.write("\"SCHEDULED\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.ASSIGNED) + "\",");
					wrt.write("\"READY\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.READY) + "\",");
					wrt.write("\"STARTING\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.STARTING) + "\",");
					wrt.write("\"RUNNING\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.RUNNING) + "\",");
					wrt.write("\"FINISHING\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.FINISHING) + "\",");
					wrt.write("\"FINISHED\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.FINISHED) + "\",");
					wrt.write("\"CANCELING\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.CANCELING) + "\",");
					wrt.write("\"CANCELED\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.CANCELED) + "\",");
					wrt.write("\"FAILED\": \""+ jobmanager.getArchive().getVertexTime(jobEvent.getJobID(), vertex.getID(), ExecutionState.FAILED) + "\"");
					wrt.write("}");
					wrt.write(",");
				}
				
			}
			wrt.write("}");
			
			wrt.write("}");
			
			
		wrt.write("]");
		
		} catch (EofException eof) { // Connection closed by client
			LOG.info("Info server for jobmanager: Connection closed by client, EofException");
		} catch (IOException ioe) { // Connection closed by client	
			LOG.info("Info server for jobmanager: Connection closed by client, IOException");
		} 
		
	}
}
