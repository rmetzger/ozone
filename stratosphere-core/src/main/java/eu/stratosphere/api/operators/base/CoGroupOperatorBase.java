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

package eu.stratosphere.api.operators.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import eu.stratosphere.api.functions.GenericCoGrouper;
import eu.stratosphere.api.operators.DualInputOperator;
import eu.stratosphere.api.operators.util.UserCodeClassWrapper;
import eu.stratosphere.api.operators.util.UserCodeObjectWrapper;
import eu.stratosphere.api.operators.util.UserCodeWrapper;

/**
 * CoGroupContract represents a CoGroup InputContract of the PACT Programming Model.
 * InputContracts are second-order functions. They have one or multiple input sets of records and a first-order
 * user function (stub implementation).
 * <p> 
 * CoGroup works on two inputs and calls the first-order user function of a {@link GenericCoGrouper} 
 * with the groups of records sharing the same key (one group per input) independently.
 * 
 * @see GenericCoGrouper
 */
public class CoGroupOperatorBase<T extends GenericCoGrouper<?, ?, ?>> extends DualInputOperator<T> {
	
	public CoGroupOperatorBase(UserCodeWrapper<T> udf, int[] keyPositions1, int[] keyPositions2, String name) {
		super(udf, keyPositions1, keyPositions2, name);
	}
	
	public CoGroupOperatorBase(T udf, int[] keyPositions1, int[] keyPositions2, String name) {
		this(new UserCodeObjectWrapper<T>(udf), keyPositions1, keyPositions2, name);
	}
	
	public CoGroupOperatorBase(Class<? extends T> udf, int[] keyPositions1, int[] keyPositions2, String name) {
		this(new UserCodeClassWrapper<T>(udf), keyPositions1, keyPositions2, name);
	}

	public boolean isCombinableFirst() {
		return getUserCodeAnnotation(CombinableFirst.class) != null;
	}
	
	public boolean isCombinableSecond() {
		return getUserCodeAnnotation(CombinableSecond.class) != null;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface CombinableFirst {};
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface CombinableSecond {};
}
