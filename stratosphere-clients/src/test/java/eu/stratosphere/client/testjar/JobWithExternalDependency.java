package eu.stratosphere.client.testjar;

/**
 * Simulate a class that requires an external dependency
 *
 */
public class JobWithExternalDependency {
	
	public static final String EXTERNAL_CLASS = "org.apache.hadoop.hive.ql.io.RCFileInputFormat";

	public static void main(String[] args) throws ClassNotFoundException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class.forName(EXTERNAL_CLASS, false, cl);
	}
}
