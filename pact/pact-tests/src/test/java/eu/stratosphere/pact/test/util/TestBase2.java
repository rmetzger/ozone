/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.pact.test.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.pact.client.minicluster.NepheleMiniCluster;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.util.LogUtils;
import eu.stratosphere.pact.compiler.DataStatistics;
import eu.stratosphere.pact.compiler.PactCompiler;
import eu.stratosphere.pact.compiler.plan.candidate.OptimizedPlan;
import eu.stratosphere.pact.compiler.plandump.PlanJSONDumpGenerator;
import eu.stratosphere.pact.compiler.plantranslate.NepheleJobGraphGenerator;
import eu.stratosphere.pact.test.util.filesystem.FilesystemProvider;
import eu.stratosphere.pact.test.util.filesystem.LocalFSProvider;

public abstract class TestBase2 {
	
	private static final int MINIMUM_HEAP_SIZE_MB = 192;

	protected final Configuration config;
	
	private final List<File> tempFiles;
	
	private NepheleMiniCluster executer;
	
	protected boolean printPlan = false;

	private FilesystemProvider fileSystemProvider;
	
	private static final Log LOG = LogFactory.getLog(TestBase2.class);


	public TestBase2(Configuration config) {
		verifyJvmOptions();
		this.config = config;
		this.tempFiles = new ArrayList<File>();
		
		LogUtils.initializeDefaultConsoleLogger(Level.WARN);
	}
	

	public TestBase2(Configuration testConfig, String clusterConfig) {
		this(testConfig);
	}


	private void verifyJvmOptions() {
		long heap = Runtime.getRuntime().maxMemory() >> 20;
		Assert.assertTrue("Insufficient java heap space " + heap + "mb - set JVM option: -Xmx" + MINIMUM_HEAP_SIZE_MB
				+ "m", heap > MINIMUM_HEAP_SIZE_MB - 50);
	}
	

	@Before
	public void startCluster() throws Exception {
		this.executer = new NepheleMiniCluster();
		this.executer.start();
		this.fileSystemProvider = new LocalFSProvider();
		this.fileSystemProvider.start();
	}

	@After
	public void stopCluster() throws Exception {
		try {
			if (this.executer != null) {
				this.executer.stop();
				this.executer = null;
				FileSystem.closeAll();
				System.gc();
			}
		} finally {
			deleteAllTempFiles();
		}
		this.fileSystemProvider.stop();
	}

	@Test
	public void testJob() throws Exception {
		// pre-submit
		try {
			preSubmit();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			Assert.fail("Pre-submit work caused an error: " + e.getMessage());
		}

		// submit job
		JobGraph jobGraph = null;
		try {
			jobGraph = getJobGraph();
		} catch(Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			Assert.fail("Failed to obtain JobGraph!");
		}
		
		Assert.assertNotNull("Obtained null JobGraph", jobGraph);
		
		try {
			JobClient client = this.executer.getJobClient(jobGraph);
			client.setConsoleStreamForReporting(getNullPrintStream());
			client.submitJobAndWait();
		} catch(Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			Assert.fail("Job execution failed!");
		}
		
		// post-submit
		try {
			postSubmit();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			Assert.fail("Post-submit work caused an error: " + e.getMessage());
		}
	}
	
	/**
	 * Returns the FilesystemProvider of the cluster setup
	 * 
	 * @see eu.stratosphere.pact.test.util.filesystem.FilesystemProvider
	 * Assert.
	 * @return The FilesystemProvider of the cluster setup
	 */
	public FilesystemProvider getFilesystemProvider() {
		return fileSystemProvider;
	}
	
	public String getTempDirPath(String dirName) throws IOException {
		File f = createAndRegisterTempFile(dirName);
		return "file://" + f.getAbsolutePath();
	}
	
	public String getTempFilePath(String fileName) throws IOException {
		File f = createAndRegisterTempFile(fileName);
		return "file://" + f.getAbsolutePath();
	}
	
	public String createTempFile(String fileName, String contents) throws IOException {
		File f = createAndRegisterTempFile(fileName);
		Files.write(contents, f, Charsets.UTF_8);
		return "file://" + f.getAbsolutePath();
	}
	
	private File createAndRegisterTempFile(String fileName) throws IOException {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		File f = new File(baseDir, fileName);
		
		if (f.exists()) {
			deleteRecursively(f);
		}
		
		File parentToDelete = f;
		while (true) {
			File parent = parentToDelete.getParentFile();
			if (parent == null) {
				throw new IOException("Missed temp dir while traversing parents of a temp file.");
			}
			if (parent.equals(baseDir)) {
				break;
			}
			parentToDelete = parent;
		}
		
		Files.createParentDirs(f);
		this.tempFiles.add(parentToDelete);
		return f;
	}
	
	public BufferedReader[] getResultReader(String resultPath) throws IOException {
		File[] files = getAllInvolvedFiles(resultPath);
		BufferedReader[] readers = new BufferedReader[files.length];
		for (int i = 0; i < files.length; i++) {
			readers[i] = new BufferedReader(new FileReader(files[i]));
		}
		return readers;
	}
	
	public BufferedInputStream[] getResultInputStream(String resultPath) throws IOException {
		File[] files = getAllInvolvedFiles(resultPath);
		BufferedInputStream[] inStreams = new BufferedInputStream[files.length];
		for (int i = 0; i < files.length; i++) {
			inStreams[i] = new BufferedInputStream(new FileInputStream(files[i]));
		}
		return inStreams;
	}
	
	public void readAllResultLines(List<String> target, String resultPath) throws IOException {
		for (BufferedReader reader : getResultReader(resultPath)) {
			String s = null;
			while ((s = reader.readLine()) != null) {
				target.add(s);
			}
		}
	}
	
	public void compareResultsByLinesInMemory(String expectedResultStr, String resultPath) throws Exception {
		ArrayList<String> list = new ArrayList<String>();
		readAllResultLines(list, resultPath);
		
		String[] result = (String[]) list.toArray(new String[list.size()]);
		Arrays.sort(result);
		
		String[] expected = expectedResultStr.split("\n");
		Arrays.sort(expected);
		
		Assert.assertEquals("Different number of lines in expected and obtained result.", expected.length, result.length);
		Assert.assertArrayEquals(expected, result);
	}
	
	private File[] getAllInvolvedFiles(String resultPath) {
		File result = asFile(resultPath);
		if (!result.exists()) {
			Assert.fail("Result file was not written");
		}
		if (result.isDirectory()) {
			return result.listFiles();
		} else {
			return new File[] { result };
		}
	}
	
	public File asFile(String path) {
		if (path.startsWith("file://")) {
			return new File(path.substring(7));
		} else {
			throw new IllegalArgumentException("This path does not denote a local file.");
		}
	}
	
	private void deleteAllTempFiles() throws IOException {
		for (File f : this.tempFiles) {
			if (f.exists()) {
				deleteRecursively(f);
			}
		}
	}
	
	// --------------------------------------------------------------------------------------------
	//  Methods to create the test program and for pre- and post- test work
	// --------------------------------------------------------------------------------------------

	protected JobGraph getJobGraph() throws Exception {
		Plan p = getPactPlan();
		if (p == null) {
			Assert.fail("Error: Cannot obtain Pact plan. Did the thest forget to override either 'getPactPlan()' or 'getJobGraph()' ?");
		}
		
		PactCompiler pc = new PactCompiler(new DataStatistics());
		OptimizedPlan op = pc.compile(p);
		
		if (printPlan) {
			System.out.println(new PlanJSONDumpGenerator().getOptimizerPlanAsJSON(op)); 
		}

		NepheleJobGraphGenerator jgg = new NepheleJobGraphGenerator();
		return jgg.compileJobGraph(op);
	}
	
	protected Plan getPactPlan() {
		return null;
	}

	protected void preSubmit() throws Exception {}

	
	protected void postSubmit() throws Exception {}
	
	

	
	// --------------------------------------------------------------------------------------------
	//  Miscellaneous helper methods
	// --------------------------------------------------------------------------------------------
	
	protected static Collection<Object[]> toParameterList(Configuration ... testConfigs) {
		ArrayList<Object[]> configs = new ArrayList<Object[]>();
		for (Configuration testConfig : testConfigs) {
			Object[] c = { testConfig };
			configs.add(c);
		}
		return configs;
	}
	/**
	 * Helper method to ease the pain to construct valid JUnit test parameter
	 * lists
	 * 
	 * @param tConfigs
	 *            list of PACT test configurations
	 * @return list of JUnit test configurations
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	protected static Collection<Object[]> toParameterList(Class<? extends TestBase2> parent,
		List<Configuration> testConfigs) throws FileNotFoundException, IOException {
		String testClassName = parent.getName();

		File configDir = new File(Constants.TEST_CONFIGS);

		List<String> clusterConfigs = new ArrayList<String>();

		if (configDir.isDirectory()) {
			for (File configFile : configDir.listFiles()) {
				Properties p = new Properties();
				p.load(new FileInputStream(configFile));

				for (String key : p.stringPropertyNames()) {
					if (key.endsWith(testClassName)) {
						for (String config : p.getProperty(key).split(",")) {
							clusterConfigs.add(config);
						}
					}
				}
			}
		}

		if (clusterConfigs.isEmpty()) {
			LOG.warn("No test config defined for test-class '" + testClassName + "'. Using default config: '"+Constants.DEFAULT_TEST_CONFIG+"'.");	
			clusterConfigs.add(Constants.DEFAULT_TEST_CONFIG);
		}

		LinkedList<Object[]> configs = new LinkedList<Object[]>();
		for (String clusterConfig : clusterConfigs) {
			for (Configuration testConfig : testConfigs) {
				Object[] c = { clusterConfig, testConfig };
				configs.add(c);
			}
		}

		return configs;
	}
	private static void deleteRecursively (File f) throws IOException {
		if (f.isDirectory()) {
			FileUtils.deleteDirectory(f);
		} else {
			f.delete();
		}
	}
	
	public static PrintStream getNullPrintStream() {
		return new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {}
		});
	}
}
