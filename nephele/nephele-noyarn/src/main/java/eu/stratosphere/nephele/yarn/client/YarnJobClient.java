package eu.stratosphere.nephele.yarn.client;

import java.io.IOException;

import eu.stratosphere.nephele.client.JobCancelResult;
import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.client.JobExecutionException;
import eu.stratosphere.nephele.client.JobProgressResult;
import eu.stratosphere.nephele.client.JobSubmissionResult;


/**
 * This class is included if Stratosphere is not build
 * with Yarn Support.
 * @See {@link NonYarnPackageException}
 */
public class YarnJobClient implements JobClient {

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
	
}