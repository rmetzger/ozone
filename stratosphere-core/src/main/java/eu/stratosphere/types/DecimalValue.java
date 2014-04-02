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

package eu.stratosphere.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class DecimalValue implements Value, JavaValue<BigDecimal> {
	private static final long serialVersionUID = 1L;

	private BigDecimal value;
	private byte[] landingZone;

	public DecimalValue() {
	}

	@Override
	public void write(DataOutput out) throws IOException {
		final byte[] bytes = value.unscaledValue().toByteArray();
		out.writeInt(bytes.length);
		out.writeInt(value.scale());
		out.write(bytes);
	}

	@Override
	public void read(DataInput in) throws IOException {
		final int len = in.readInt();
		final int scale = in.readInt();
		if(landingZone == null || landingZone.length != len) {
			// try to avoid at least some serialization.
			landingZone = new byte[len];
		}
		in.readFully(landingZone);
		value = new BigDecimal(new BigInteger(landingZone), scale);
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal d) {
		this.value = d;
	}

	@Override
	public BigDecimal getObjectValue() {
		return value;
	}

	@Override
	public void setObjectValue(BigDecimal object) {
		this.value = object;
	}


}
