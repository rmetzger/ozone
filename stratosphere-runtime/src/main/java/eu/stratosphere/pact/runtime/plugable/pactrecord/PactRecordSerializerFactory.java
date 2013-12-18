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

package eu.stratosphere.pact.runtime.plugable.pactrecord;

import eu.stratosphere.api.typeutils.TypeSerializer;
import eu.stratosphere.api.typeutils.TypeSerializerFactory;
import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.types.PactRecord;

/**
 * A factory that create a serializer for the {@link PactRecord} data type.
 */
public class PactRecordSerializerFactory implements TypeSerializerFactory<PactRecord>
{
	private static final PactRecordSerializerFactory INSTANCE = new PactRecordSerializerFactory();
	
	/**
	 * Gets an instance of the serializer factory. The instance is shared, since the factory is a
	 * stateless class. 
	 * 
	 * @return An instance of the serializer factory.
	 */
	public static final PactRecordSerializerFactory get() {
		return INSTANCE;
	}
	
	// --------------------------------------------------------------------------------------------
	

	@Override
	public void writeParametersToConfig(Configuration config)
	{}

	/* (non-Javadoc)
	 * @see eu.stratosphere.pact.common.generic.types.TypeSerializerFactory#readParametersFromConfig(eu.stratosphere.nephele.configuration.Configuration, java.lang.ClassLoader)
	 */
	@Override
	public void readParametersFromConfig(Configuration config, ClassLoader cl) throws ClassNotFoundException
	{}
	

	@Override
	public TypeSerializer<PactRecord> getSerializer() {
		return PactRecordSerializer.get();
	}


	@Override
	public Class<PactRecord> getDataType() {
		return PactRecord.class;
	}
}
