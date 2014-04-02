/***********************************************************************************************************************
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
 **********************************************************************************************************************/

package eu.stratosphere.pact.runtime.plugable.arrayrecord;

import java.io.IOException;

import eu.stratosphere.api.common.typeutils.TypeSerializer;
import eu.stratosphere.core.memory.DataInputView;
import eu.stratosphere.core.memory.DataOutputView;
import eu.stratosphere.types.CopyableValue;
import eu.stratosphere.types.Value;
import eu.stratosphere.util.InstantiationUtil;

/**
 * Implementation of the (de)serialization and copying logic for the {@link Record}.
 */
public final class ArrayRecordSerializer extends TypeSerializer<Value[]>
{
	private final Class<? extends CopyableValue<Value>>[] types;

	private final CopyableValue<Value>[] instances;

	private final int[] lengths;

	private final int len;

	private final Class<CopyableValue<Value>> clazz;


	@SuppressWarnings("unchecked")
	public ArrayRecordSerializer(Class<? extends Value>[] types) {
		this.types = new Class[types.length];
		this.instances = new CopyableValue[types.length];
		this.lengths = new int[types.length];

		int len = 0;

		for (int i = 0; i < types.length; i++) {
			if (CopyableValue.class.isAssignableFrom(types[i])) {
				this.types[i] = (Class<CopyableValue<Value>>) types[i];
				this.instances[i] = InstantiationUtil.instantiate(this.types[i], this.clazz);
				this.lengths[i] = this.instances[i].getBinaryLength();
				if (len >= 0) {
					if (this.lengths[i] > 0) {
						len += this.lengths[i];
					} else {
						len = -1;
					}
				}
			} else {
				throw new IllegalArgumentException("The array model currently supports only value types that implement the '" +
						CopyableValue.class.getName() + "'.");
			}
		}

		this.len = len;
		this.clazz = (Class<CopyableValue<Value>>) (Class<?>) CopyableValue.class;
	}

	// --------------------------------------------------------------------------------------------


	@Override
	public Value[] createInstance() {
		final Value[] vals = new Value[this.types.length];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = InstantiationUtil.instantiate(this.types[i], this.clazz);
		}
		return vals;
	}

	/* (non-Javadoc)
	 * @see eu.stratosphere.pact.runtime.plugable.TypeAccessors#copy(java.lang.Object, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Value[] copy(Value[] from, Value[] reuse) {
		for (int i = 0; i < from.length; i++) {
			((CopyableValue<Value>) from[i]).copyTo(reuse[i]);
		}
		return reuse;
	}


	@Override
	public int getLength() {
		return len;
	}

	// --------------------------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see eu.stratosphere.pact.runtime.plugable.TypeAccessorsV2#serialize(java.lang.Object, eu.stratosphere.nephele.services.memorymanager.DataOutputViewV2)
	 */
	@Override
	public void serialize(Value[] record, DataOutputView target) throws IOException {
		for (int i = 0; i < this.types.length; i++) {
			record[i].write(target);
		}
	}

	/* (non-Javadoc)
	 * @see eu.stratosphere.pact.runtime.plugable.TypeAccessorsV2#deserialize(java.lang.Object, eu.stratosphere.nephele.services.memorymanager.DataInputViewV2)
	 */
	@Override
	public Value[] deserialize(Value[] reuse, DataInputView source) throws IOException {
		for (int i = 0; i < this.types.length; i++) {
			reuse[i].read(source);
		}
		return reuse;
	}

	/* (non-Javadoc)
	 * @see eu.stratosphere.pact.runtime.plugable.TypeAccessorsV2#copy(eu.stratosphere.nephele.services.memorymanager.DataInputViewV2, eu.stratosphere.nephele.services.memorymanager.DataOutputViewV2)
	 */
	@Override
	public void copy(DataInputView source, DataOutputView target) throws IOException {
		if (this.len > 0) {
			target.write(source, this.len);
		} else {
			for (int i = 0; i < this.lengths.length; i++) {
				if (this.lengths[i] > 0) {
					target.write(source, this.lengths[i]);
				} else {
					this.instances[i].copy(source, target);
				}
			}
		}
	}
}
