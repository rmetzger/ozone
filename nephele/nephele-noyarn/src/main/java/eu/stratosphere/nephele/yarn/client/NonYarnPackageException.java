package eu.stratosphere.nephele.yarn.client;


public class NonYarnPackageException extends Error {
	private static final long serialVersionUID = 1L;

	public NonYarnPackageException() {
		super("This feature is not avaiable in this Stratosphere Build."
				+ "Please activate the Hadoop 2.x (YARN) Profile using Maven.");
	}
}
