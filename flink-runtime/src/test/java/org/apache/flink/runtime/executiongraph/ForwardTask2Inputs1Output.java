/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.flink.runtime.executiongraph;

import org.apache.flink.core.io.StringRecord;
import org.apache.flink.runtime.io.network.api.RecordReader;
import org.apache.flink.runtime.io.network.api.RecordWriter;
import org.apache.flink.runtime.jobgraph.tasks.AbstractInvokable;

public class ForwardTask2Inputs1Output extends AbstractInvokable {

	private RecordReader<StringRecord> input1 = null;

	private RecordReader<StringRecord> input2 = null;

	private RecordWriter<StringRecord> output = null;

	@Override
	public void invoke() throws Exception {
		this.output.initializeSerializers();

		while (this.input1.hasNext()) {

			StringRecord s = input1.next();
			this.output.emit(s);
		}

		while (this.input2.hasNext()) {

			try {
				StringRecord s = input2.next();
				this.output.emit(s);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.output.flush();
	}

	@Override
	public void registerInputOutput() {
		this.input1 = new RecordReader<StringRecord>(this, StringRecord.class);
		this.input2 = new RecordReader<StringRecord>(this, StringRecord.class);
		this.output = new RecordWriter<StringRecord>(this);
	}
}
