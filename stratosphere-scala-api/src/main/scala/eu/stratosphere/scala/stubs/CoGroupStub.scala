/**
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package eu.stratosphere.scala.stubs

import eu.stratosphere.scala.analysis.{UDTSerializer, UDF2, UDT}
import eu.stratosphere.api.record.functions.{CoGroupFunction => JCoGroupStub}
import eu.stratosphere.types.PactRecord
import eu.stratosphere.util.Collector
import eu.stratosphere.configuration.Configuration;
import java.util.{Iterator => JIterator}

abstract class CoGroupStubBase[LeftIn: UDT, RightIn: UDT, Out: UDT] extends JCoGroupStub with Serializable {
  val leftInputUDT = implicitly[UDT[LeftIn]]
  val rightInputUDT = implicitly[UDT[RightIn]]
  val outputUDT = implicitly[UDT[Out]]
  val udf: UDF2[LeftIn, RightIn, Out] = new UDF2(leftInputUDT, rightInputUDT, outputUDT)

  protected val outputRecord = new PactRecord()

  protected lazy val leftIterator: DeserializingIterator[LeftIn] = new DeserializingIterator(udf.getLeftInputDeserializer)
  protected lazy val leftForwardFrom: Array[Int] = udf.getLeftForwardIndexArrayFrom
  protected lazy val leftForwardTo: Array[Int] = udf.getLeftForwardIndexArrayTo
  protected lazy val rightIterator: DeserializingIterator[RightIn] = new DeserializingIterator(udf.getRightInputDeserializer)
  protected lazy val rightForwardFrom: Array[Int] = udf.getRightForwardIndexArrayFrom
  protected lazy val rightForwardTo: Array[Int] = udf.getRightForwardIndexArrayTo
  protected lazy val serializer: UDTSerializer[Out] = udf.getOutputSerializer

  override def open(config: Configuration) = {
    super.open(config)

    this.outputRecord.setNumFields(udf.getOutputLength)
  }
}

abstract class CoGroupStub[LeftIn: UDT, RightIn: UDT, Out: UDT] extends CoGroupStubBase[LeftIn, RightIn, Out] with Function2[Iterator[LeftIn], Iterator[RightIn], Out] {
  override def coGroup(leftRecords: JIterator[PactRecord], rightRecords: JIterator[PactRecord], out: Collector[PactRecord]) = {
    val firstLeftRecord = leftIterator.initialize(leftRecords)
    val firstRightRecord = rightIterator.initialize(rightRecords)

    if (firstRightRecord != null) {
      outputRecord.copyFrom(firstRightRecord, rightForwardFrom, rightForwardTo)
    }
    if (firstLeftRecord != null) {
      outputRecord.copyFrom(firstLeftRecord, leftForwardFrom, leftForwardTo)
    }

    val output = apply(leftIterator, rightIterator)

    serializer.serialize(output, outputRecord)
    out.collect(outputRecord)
  }
}

abstract class FlatCoGroupStub[LeftIn: UDT, RightIn: UDT, Out: UDT] extends CoGroupStubBase[LeftIn, RightIn, Out] with Function2[Iterator[LeftIn], Iterator[RightIn], Iterator[Out]] {
  override def coGroup(leftRecords: JIterator[PactRecord], rightRecords: JIterator[PactRecord], out: Collector[PactRecord]) = {
    val firstLeftRecord = leftIterator.initialize(leftRecords)
    outputRecord.copyFrom(firstLeftRecord, leftForwardFrom, leftForwardTo)

    val firstRightRecord = rightIterator.initialize(rightRecords)
    outputRecord.copyFrom(firstRightRecord, rightForwardFrom, rightForwardTo)

    val output = apply(leftIterator, rightIterator)

    if (output.nonEmpty) {

      for (item <- output) {
        serializer.serialize(item, outputRecord)
        out.collect(outputRecord)
      }
    }
  }
}