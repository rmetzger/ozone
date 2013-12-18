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
package eu.stratosphere.test.compiler.plandump;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.junit.Assert;
import org.junit.Test;

import eu.stratosphere.api.Job;
import eu.stratosphere.compiler.plan.OptimizedPlan;
import eu.stratosphere.compiler.plandump.PlanJSONDumpGenerator;
import eu.stratosphere.example.record.connectedcomponents.WorksetConnectedComponents;
import eu.stratosphere.example.record.kmeans.KMeansIterative;
import eu.stratosphere.example.record.kmeans.KMeansSingleStep;
import eu.stratosphere.example.record.relational.TPCHQuery3;
import eu.stratosphere.example.record.relational.WebLogAnalysis;
import eu.stratosphere.example.record.wordcount.WordCount;
import eu.stratosphere.test.compiler.CompilerTestBase;


/*
 * The tests in this class simply invokes the JSON dump code for the optimized plan.
 */
public class CompiledPlanDumpTest extends CompilerTestBase {
	
	@Test
	public void dumpWordCount() {
		dump(new WordCount().createJob(DEFAULT_PARALLELISM_STRING, IN_FILE, OUT_FILE));
	}
	
	@Test
	public void dumpTPCH3() {
		dump(new TPCHQuery3().createJob(DEFAULT_PARALLELISM_STRING, IN_FILE, IN_FILE, OUT_FILE));
	}
	
	@Test
	public void dumpKMeans() {
		dump(new KMeansSingleStep().createJob(DEFAULT_PARALLELISM_STRING, IN_FILE, IN_FILE, OUT_FILE));
	}
	
	@Test
	public void dumpWebLogAnalysis() {
		dump(new WebLogAnalysis().createJob(DEFAULT_PARALLELISM_STRING, IN_FILE, IN_FILE, IN_FILE, OUT_FILE));
	}

	@Test
	public void dumpBulkIterationKMeans() {
		dump(new KMeansIterative().createJob(DEFAULT_PARALLELISM_STRING, IN_FILE, OUT_FILE));
	}
	
	@Test
	public void dumpWorksetConnectedComponents() {
		dump(new WorksetConnectedComponents().createJob(DEFAULT_PARALLELISM_STRING, IN_FILE, IN_FILE, OUT_FILE));
	}
	
	private void dump(Job p) {
		try {
			OptimizedPlan op = compileNoStats(p);
			PlanJSONDumpGenerator dumper = new PlanJSONDumpGenerator();
			String json = dumper.getOptimizerPlanAsJSON(op);
			JsonParser parser = new JsonFactory().createJsonParser(json);
			while (parser.nextToken() != null);
		} catch (JsonParseException e) {
			e.printStackTrace();
			Assert.fail("JSON Generator produced malformatted output: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("An error occurred in the test: " + e.getMessage());
		}
	}
}
