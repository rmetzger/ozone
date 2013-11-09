package eu.stratosphere.nephele.yarn.client;

import java.io.IOException;
import java.net.InetSocketAddress;

import eu.stratosphere.nephele.client.JobCancelResult;
import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.client.JobExecutionException;
import eu.stratosphere.nephele.client.JobProgressResult;
import eu.stratosphere.nephele.client.JobSubmissionResult;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.jobgraph.JobGraph;


/**
 * This class is included if Stratosphere is not build
 * with Yarn Support.
 * @See {@link NonYarnPackageException}
 */
public class YarnJobClient implements JobClient {

	public YarnJobClient(final JobGraph jobGraph, final Configuration configuration) throws InterruptedException{
		throw new NonYarnPackageException();
	}
	@Override
	public int getRecommendedPollingInterval() throws IOException {
		throw new NonYarnPackageException();
	}

	@Override
	public JobProgressResult getJobProgress() throws IOException {
		throw new NonYarnPackageException();
	}

	@Override
	public JobSubmissionResult submitJob() throws IOException {
		throw new NonYarnPackageException();
	}

	@Override
	public long submitJobAndWait() throws IOException, JobExecutionException {
		throw new NonYarnPackageException();
	}

	@Override
	public JobCancelResult cancelJob() throws IOException {
		throw new NonYarnPackageException();
	}

	@Override
	public void close() {
		throw new NonYarnPackageException();
	}
	@Override
	public InetSocketAddress getJobManagerConnection() {
		throw new NonYarnPackageException();
	}
	@Override
	public void setJobGraph(JobGraph jg) {
		throw new NonYarnPackageException();
	}
}