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
package eu.stratosphere.api.functions.aggregators;

import eu.stratosphere.types.Value;

/**
 * Simple class describing an aggregator with the name it is registered under.
 */
public class AggregatorWithName<T extends Value> {

	private final String name;
	
	private final Class<? extends Aggregator<T>> aggregator;

	public AggregatorWithName(String name, Class<Aggregator<T>> aggregator) {
		this.name = name;
		this.aggregator = aggregator;
	}
	
	public String getName() {
		return name;
	}
	
	public Class<? extends Aggregator<T>> getAggregator() {
		return aggregator;
	}
}
