package ${package};

import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Skeleton for a Flink Job.
 *
 * For a full example of a Flink Job, see the WordCountJob.java file in the
 * same package/directory or have a look at the website.
 *
 * You can also generate a .jar file that you can submit on your Flink
 * cluster.
 * Just type
 * 		mvn clean package
 * in the projects root directory.
 * You will find the jar in
 * 		target/flink-quickstart-0.1-SNAPSHOT-Sample.jar
 *
 */
public class Job {

	public static void main(String[] args) throws Exception {
		// set up the execution environment
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();


		/**
		 * Here, you can start creating your execution plan for Flink.
		 *
		 * Start with getting some data from the environment, like
		 * 	env.readTextFile(textPath);
		 *
		 * then, transform the resulting DataSet<String> using operations
		 * like
		 * 	.filter()
		 * 	.flatMap()
		 * 	.join()
		 * 	.group()
		 * and many more.
		 * Have a look at the programming guide for the Java API:
		 *
		 * http://flink.incubator.apache.org/docs/0.6-SNAPSHOT/java_api_guide.html
		 *
		 * and the examples
		 *
		 * http://flink.incubator.apache.org/docs/0.6-SNAPSHOT/java_api_examples.html
		 *
		 */

		// execute program
		env.execute("Stratosphere Java API Skeleton");
	}
}