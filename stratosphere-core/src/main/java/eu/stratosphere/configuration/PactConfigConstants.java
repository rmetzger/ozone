/***********************************************************************************************************************
 *
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.configuration;

/**
 * A collection of all configuration constants, such as config keys and default values.
 */
public final class PactConfigConstants {
	
	// ------------------------------------------------------------------------
	//                          Configuration Keys
	// ------------------------------------------------------------------------

	// ---------------------------- Parallelism -------------------------------

	/**
	 * The key for the config parameter defining the default degree of parallelization for user functions.
	 */
	public static final String DEFAULT_PARALLELIZATION_DEGREE_KEY = "pact.parallelization.degree";

	/**
	 * The key for the config parameter defining the default intra-node degree of parallelization
	 * for user functions.
	 */
	public static final String PARALLELIZATION_MAX_INTRA_NODE_DEGREE_KEY = "pact.parallelization.max-intra-node-degree";

	/**
	 * The key for the config parameter defining the number of nodes to use for the pact program execution.
	 */
	public static final String MAXIMUM_NUMBER_MACHINES_KEY = "pact.parallelization.maxmachines";

	/**
	 * The key for the config parameter defining the instance that are booked for pact tasks.
	 */
	public static final String DEFAULT_INSTANCE_TYPE_KEY = "pact.parallelization.default-instance-type";
	
	// ---------------------------- Compiler -------------------------------

	/**
	 * The key for the maximum number of line samples to be taken by the delimited input format, if no
	 * other value is specified for the data source.
	 */
	public static final String DELIMITED_FORMAT_MAX_LINE_SAMPLES_KEY = "pact.delimited-informat.max-line-samples";

	/**
	 * The key for the minimum number of line samples to be taken by the delimited input format, if no
	 * other value is specified for the data source.
	 */
	public static final String DELIMITED_FORMAT_MIN_LINE_SAMPLES_KEY = "pact.delimited-informat.min-line-samples";

	/**
	 * The key for the maximum length of a single sampled record before the sampling is aborted.
	 */
	public static final String DELIMITED_FORMAT_MAX_SAMPLE_LENGTH_KEY = "pact.delimited-informat.max-sample-len";
	
	// ------------------------------- Runtime --------------------------------
	
	/**
	 * Key for the default maximum fan. Corresponds to the maximum fan-in for merge-sorts and the maximum fan-out
	 * for hybrid hash joins. 
	 */
	public static final String DEFAULT_SPILLING_MAX_FAN_KEY = "pact.runtime.max-fan";

	/**
	 * Key for the default spilling threshold. When more than the threshold memory of the sort buffers is full, the
	 * sorter will start spilling to disk.
	 */
	public static final String DEFAULT_SORT_SPILLING_THRESHOLD_KEY = "pact.runtime.sort-spilling-threshold";
	
	/**
	 * The key for the config parameter defining the timeout for filesystem stream opening.
	 * A value of 0 indicates infinite waiting.
	 */
	public static final String FS_STREAM_OPENING_TIMEOUT_KEY = "pact.runtime.fs_timeout";
	
	/**
	 * The key for the config parameter defining whether to use the special multicast logic
	 * for broadcasts.
	 */
	public static final String USE_MULTICAST_FOR_BROADCAST = "pact.runtime.multicast-for-broadcast";
	
	// ----------------------------- Web Frontend -----------------------------

	/**
	 * The key for Stratosphere's base dir path
	 */
	public static final String STRATOSPHERE_BASE_DIR_PATH_KEY = "stratosphere.base.dir.path";
	
	/**
	 * The key for the config parameter defining port for the pact web-frontend server.
	 */
	public static final String WEB_FRONTEND_PORT_KEY = "pact.web.port";

	/**
	 * The key for the config parameter defining the directory containing the web documents.
	 */
	public static final String WEB_ROOT_PATH_KEY = "pact.web.rootpath";

	/**
	 * The key for the config parameter defining the temporary data directory.
	 */
	public static final String WEB_TMP_DIR_KEY = "pact.web.temp";

	/**
	 * The key for the config parameter defining the directory that programs are uploaded to.
	 */
	public static final String WEB_JOB_UPLOAD_DIR_KEY = "pact.web.uploaddir";

	/**
	 * The key for the config parameter defining the directory that JSON plans are written to.
	 */
	public static final String WEB_PLAN_DUMP_DIR_KEY = "pact.web.plandump";

	/**
	 * The key for the config parameter defining the port to the htaccess file protecting the web server.
	 */
	public static final String WEB_ACCESS_FILE_KEY = "pact.web.access";

	// ------------------------------------------------------------------------
	//                             Default Values
	// ------------------------------------------------------------------------

	// ------------------------------ Parallelism -----------------------------

	/**
	 * The default degree of parallelism for PACT user functions.
	 */
	public static final int DEFAULT_PARALLELIZATION_DEGREE = -1;

	/**
	 * The default intra-node parallelism
	 */
	public static final int DEFAULT_MAX_INTRA_NODE_PARALLELIZATION_DEGREE = -1;

	/**
	 * The default maximal number of machines to use for the execution of a pact program. The -1 indicates
	 * that there is no such restriction and nephele will allocate as many as possible.
	 */
	public static final int DEFAULT_MAX_NUMBER_MACHINES = -1;

	/**
	 * The description of the default instance type that is booked for the execution of PACT tasks.
	 */
	public static final String DEFAULT_INSTANCE_TYPE_DESCRIPTION = "standard,2,1,300,10,0";
	
	// ---------------------------- Compiler -------------------------------

	/**
	 * The default maximum number of line samples taken by the delimited input format.
	 */
	public static final int DEFAULT_DELIMITED_FORMAT_MAX_LINE_SAMPLES = 10;
	
	/**
	 * The default minimum number of line samples taken by the delimited input format.
	 */
	public static final int DEFAULT_DELIMITED_FORMAT_MIN_LINE_SAMPLES = 2;
	
	/**
	 * The default maximum sample length before sampling is aborted (2 MiBytes).
	 */
	public static final int DEFAULT_DELIMITED_FORMAT_MAX_SAMPLE_LEN = 2 * 1024 * 1024;

	// ------------------------------- Runtime --------------------------------
	
	/**
	 * The default value for the maximum spilling fan in/out.
	 */
	public static final int DEFAULT_SPILLING_MAX_FAN = 128;
	
	/**
	 * The default percentage of the sort memory to be full before data is spilled.
	 */
	public static final float DEFAULT_SORT_SPILLING_THRESHOLD = 0.8f;
	
	/**
	 * The default timeout for filesystem stream opening: infinite (means max long milliseconds).
	 */
	public static final int DEFAULT_FS_STREAM_OPENING_TIMEOUT = 0;
	
	/**
	 * The key for the config parameter defining whether to use the special multicast logic
	 * for broadcasts.
	 */
	public static final boolean DEFAULT_USE_MULTICAST_FOR_BROADCAST = false;
	
	// ----------------------------- Web Frontend -----------------------------

	/**
	 * The default port to launch the web frontend server on.
	 */
	public static final int DEFAULT_WEB_FRONTEND_PORT = 8080;

	/**
	 * The default path of the directory containing the web documents.
	 */
	public static final String DEFAULT_WEB_ROOT_DIR = "./resources/web-docs/";

	/**
	 * The default directory to store temporary objects (e.g. during file uploads).
	 */
	public static final String DEFAULT_WEB_TMP_DIR = System.getProperty("java.io.tmpdir") == null ? "/tmp" : System
		.getProperty("java.io.tmpdir");

	/**
	 * The default directory for temporary plan dumps from the web frontend.
	 */
	public static final String DEFAULT_WEB_PLAN_DUMP_DIR = DEFAULT_WEB_TMP_DIR + "/pact-plans/";

	/**
	 * The default directory to store uploaded jobs in.
	 */
	public static final String DEFAULT_WEB_JOB_STORAGE_DIR = DEFAULT_WEB_TMP_DIR + "/pact-jobs/";

	/**
	 * The default path to the file containing the list of access privileged users and passwords.
	 */
	public static final String DEFAULT_WEB_ACCESS_FILE_PATH = null;

	// ------------------------------------------------------------------------

	/**
	 * Private default constructor to prevent instantiation.
	 */
	private PactConfigConstants() {
	}
}
