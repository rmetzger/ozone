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
package eu.stratosphere.api.java.operators.translation;

import java.io.IOException;

import eu.stratosphere.api.common.io.OutputFormat;
import eu.stratosphere.api.common.operators.GenericDataSink;
import eu.stratosphere.api.java.typeutils.TypeInformation;
import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.util.Reference;


public class PlanDataSink<T> extends GenericDataSink {

	private final TypeInformation<T> inputDataType;

	public PlanDataSink(OutputFormat<T> format, String name, TypeInformation<T> inputDataType) {
		super(new ReferenceWrappingOutputFormat<T>(format), name);
		this.inputDataType = inputDataType;
	}

	public TypeInformation<T> getType() {
		return this.inputDataType;
	}

	// --------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------

	public static final class ReferenceWrappingOutputFormat<T> implements OutputFormat<Reference<T>> {

		private static final long serialVersionUID = 1L;


		private final OutputFormat<T> format;


		public ReferenceWrappingOutputFormat(OutputFormat<T> format) {
			this.format = format;
		}



		@Override
		public void configure(Configuration parameters) {
			format.configure(parameters);
		}

		@Override
		public void open(int taskNumber, int numTasks) throws IOException {
			format.open(taskNumber, numTasks);
		}

		@Override
		public void writeRecord(Reference<T> record) throws IOException {
			format.writeRecord(record.ref);

		}

		@Override
		public void close() throws IOException {
			format.close();
		}
	}
}
