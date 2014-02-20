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

// --------------------------------------------------------------
//  THIS IS A GENERATED SOURCE FILE. DO NOT EDIT!
//  GENERATED FROM eu.stratosphere.api.java.tuple.TupleGenerator.
// --------------------------------------------------------------


package eu.stratosphere.api.java.tuple;

import eu.stratosphere.util.StringUtils;

@SuppressWarnings({"restriction"})
public final class Tuple3<T1, T2, T3> extends Tuple {

	private static final long serialVersionUID = 1L;

	private T1 _1;
	private T2 _2;
	private T3 _3;

	public Tuple3() {}

	public Tuple3(T1 value1, T2 value2, T3 value3) {
		this._1 = value1;
		this._2 = value2;
		this._3 = value3;
	}

	public T1 T1() {
		return this._1;
	}
	public T2 T2() {
		return this._2;
	}
	public T3 T3() {
		return this._3;
	}
	public T1 getFirst() {
		return this._1;
	}
	public T2 getSecond() {
		return this._2;
	}
	public T3 getThird() {
		return this._3;
	}
	public void T1(T1 value) {
		this._1 = value;
	}
	public void T2(T2 value) {
		this._2 = value;
	}
	public void T3(T3 value) {
		this._3 = value;
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getField(int pos) {
		switch(pos) {
			case 0: return (T) this._1;
			case 1: return (T) this._2;
			case 2: return (T) this._3;
			default: throw new IndexOutOfBoundsException(String.valueOf(pos));
		}
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T> void setField(T value, int pos) {
		switch(pos) {
			case 0:
				this._1 = (T1) value;
				break;
			case 1:
				this._2 = (T2) value;
				break;
			case 2:
				this._3 = (T3) value;
				break;
			default: throw new IndexOutOfBoundsException(String.valueOf(pos));
		}
	}

	// -------------------------------------------------------------------------------------------------
	// standard utilities
	// -------------------------------------------------------------------------------------------------

	@Override
	public String toString() {
		return "(" + StringUtils.arrayAwareToString(this._1)
			+ ", " + StringUtils.arrayAwareToString(this._2)
			+ ", " + StringUtils.arrayAwareToString(this._3)
			+ ")";
	}

	// -------------------------------------------------------------------------------------------------
	// unsafe fast field access
	// -------------------------------------------------------------------------------------------------

	@SuppressWarnings({ "unchecked"})
	public <T> T getFieldFast(int pos) {
		return (T) UNSAFE.getObject(this, offsets[pos]);
	}

	private static final sun.misc.Unsafe UNSAFE = eu.stratosphere.core.memory.MemoryUtils.UNSAFE;

	private static final long[] offsets = new long[3];

	static {
		try {
			offsets[0] = UNSAFE.objectFieldOffset(Tuple3.class.getDeclaredField("_1"));
			offsets[1] = UNSAFE.objectFieldOffset(Tuple3.class.getDeclaredField("_2"));
			offsets[2] = UNSAFE.objectFieldOffset(Tuple3.class.getDeclaredField("_3"));
		} catch (Throwable t) {
			throw new RuntimeException("Could not initialize fast field accesses for tuple data type.");
		}
	}
}
