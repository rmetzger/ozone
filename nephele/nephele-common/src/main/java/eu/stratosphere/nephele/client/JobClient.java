package eu.stratosphere.nephele.client;

import java.io.IOException;
import java.net.InetSocketAddress;

import eu.stratosphere.nephele.jobgraph.JobGraph;

public interface JobClient {

	/**
	 * Returns the recommended interval in seconds in which a client
	 * is supposed to poll for progress information.
	 * 
	 * @return the interval in seconds
	 * @throws IOException
	 *         thrown if an error occurred while transmitting the request
	 */
	public abstract int getRecommendedPollingInterval() throws IOException;
	
	/**
	 * Retrieves the current status of the job assigned to this job client.
	 * 
	 * @return a <code>JobProgressResult</code> object including the current job progress
	 * @throws IOException
	 *         thrown if an error occurred while transmitting the request
	 */
	public abstract JobProgressResult getJobProgress() throws IOException;

	/**
	 * Submits the job assigned to this job client to the job manager.
	 * 
	 * @return a <code>JobSubmissionResult</code> object encapsulating the results of the job submission
	 * @throws IOException
	 *         thrown in case of submission errors while transmitting the data to the job manager
	 */
	public abstract JobSubmissionResult submitJob() throws IOException;
	
	/**
	 * Submits the job assigned to this job client to the job manager and queries the job manager
	 * about the progress of the job until it is either finished or aborted.
	 * 
	 * @return the duration of the job execution in milliseconds
	 * @throws IOException
	 *         thrown if an error occurred while transmitting the request
	 * @throws JobExecutionException
	 *         thrown if the job has been aborted either by the user or as a result of an error
	 */
	public abstract long submitJobAndWait() throws IOException,
			JobExecutionException;
	
	/**
	 * Cancels the job assigned to this job client.
	 * 
	 * @return a <code>JobCancelResult</code> object encapsulating the result of the job cancel request
	 * @throws IOException
	 *         thrown if an error occurred while transmitting the request to the job manager
	 */
	public abstract JobCancelResult cancelJob() throws IOException;
	
	/**
	 * Closes the <code>JobClient</code> by destroying the RPC stub object.
	 */
	public abstract void close();
	
	
	public abstract InetSocketAddress getJobManagerConnection();

	public abstract void setJobGraph(JobGraph jg);

}