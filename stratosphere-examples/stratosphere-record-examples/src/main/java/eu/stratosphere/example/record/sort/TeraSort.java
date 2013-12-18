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

package eu.stratosphere.example.record.sort;

import eu.stratosphere.api.Job;
import eu.stratosphere.api.Program;
import eu.stratosphere.api.ProgramDescription;
import eu.stratosphere.api.operators.FileDataSink;
import eu.stratosphere.api.operators.FileDataSource;
import eu.stratosphere.api.operators.Order;
import eu.stratosphere.api.operators.Ordering;
import eu.stratosphere.example.record.sort.terasort.TeraDistribution;
import eu.stratosphere.example.record.sort.terasort.TeraInputFormat;
import eu.stratosphere.example.record.sort.terasort.TeraKey;
import eu.stratosphere.example.record.sort.terasort.TeraOutputFormat;

/**
 * This is an example implementation of the well-known TeraSort benchmark using the Stratosphere system. The benchmark
 * requires the input data to be generated according to the rules of Jim Gray's sort benchmark. A possible way to such
 * input data is the Hadoop TeraGen program. For more details see <a
 * href="http://hadoop.apache.org/common/docs/current/api/org/apache/hadoop/examples/terasort/TeraGen.html">
 * http://hadoop.apache.org/common/docs/current/api/org/apache/hadoop/examples/terasort/TeraGen.html</a>.
 *
 * Note: this example job is currently not included in the build, because of problems with the RangePartioner (see
 * https://github.com/stratosphere/stratosphere/issues/7). It should be included again after fixing the issue.
 *
 * @author warneke
 */
public final class TeraSort implements Program, ProgramDescription {


	@Override
	public String getDescription() {

		return "Parameters: [numSubStasks] [input] [output]";
	}


	@Override
	public Job createJob(String... args) throws IllegalArgumentException {
		// parse job parameters
		final int numSubTasks = (args.length > 0 ? Integer.parseInt(args[0]) : 1);
		final String input = (args.length > 1 ? args[1] : "");
		final String output = (args.length > 2 ? args[2] : "");

		// This task will read the input data and generate the key/value pairs
		final FileDataSource source = 
				new FileDataSource(new TeraInputFormat(), input, "Data Source");
		source.setDegreeOfParallelism(numSubTasks);

		// This task writes the sorted data back to disk
		final FileDataSink sink = 
				new FileDataSink(new TeraOutputFormat(), output, "Data Sink");
		sink.setDegreeOfParallelism(numSubTasks);
		sink.setGlobalOrder(new Ordering(0, TeraKey.class, Order.ASCENDING), new TeraDistribution());

		sink.addInput(source);

		return new Job(sink, "TeraSort");
	}

}
