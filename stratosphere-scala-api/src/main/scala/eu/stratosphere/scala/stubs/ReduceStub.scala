/**
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package eu.stratosphere.scala.stubs

import java.util.{ Iterator => JIterator }

import eu.stratosphere.scala.analysis.{UDTSerializer, UDF1, FieldSelector, UDT}
import eu.stratosphere.api.record.functions.{ReduceFunction => JReduceStub}
import eu.stratosphere.types.PactRecord
import eu.stratosphere.util.Collector
import scala.Iterator


abstract class ReduceStubBase[In: UDT, Out: UDT] extends JReduceStub with Serializable {
  val inputUDT: UDT[In] = implicitly[UDT[In]]
  val outputUDT: UDT[Out] = implicitly[UDT[Out]]
  val udf: UDF1[In, Out] = new UDF1(inputUDT, outputUDT)

  protected val reduceRecord = new PactRecord()

  protected lazy val reduceIterator: DeserializingIterator[In] = new DeserializingIterator(udf.getInputDeserializer)
  protected lazy val reduceSerializer: UDTSerializer[Out] = udf.getOutputSerializer
  protected lazy val reduceForwardFrom: Array[Int] = udf.getForwardIndexArrayFrom
  protected lazy val reduceForwardTo: Array[Int] = udf.getForwardIndexArrayTo
}

abstract class ReduceStub[In: UDT] extends ReduceStubBase[In, In] with Function2[In, In, In] {

  override def combine(records: JIterator[PactRecord], out: Collector[PactRecord]) = {
    reduce(records, out)
  }

  override def reduce(records: JIterator[PactRecord], out: Collector[PactRecord]) = {
    val firstRecord = reduceIterator.initialize(records)
    reduceRecord.copyFrom(firstRecord, reduceForwardFrom, reduceForwardTo)

    val output = reduceIterator.reduce(apply)

    reduceSerializer.serialize(output, reduceRecord)
    out.collect(reduceRecord)
  }
}

abstract class GroupReduceStub[In: UDT, Out: UDT] extends ReduceStubBase[In, Out] with Function1[Iterator[In], Out] {
  override def reduce(records: JIterator[PactRecord], out: Collector[PactRecord]) = {
    val firstRecord = reduceIterator.initialize(records)
    reduceRecord.copyFrom(firstRecord, reduceForwardFrom, reduceForwardTo)

    val output = apply(reduceIterator)

    reduceSerializer.serialize(output, reduceRecord)
    out.collect(reduceRecord)
  }
}

abstract class CombinableGroupReduceStub[In: UDT, Out: UDT] extends ReduceStubBase[In, Out] with Function1[Iterator[In], Out] {
  override def combine(records: JIterator[PactRecord], out: Collector[PactRecord]) = {
    val firstRecord = reduceIterator.initialize(records)
    reduceRecord.copyFrom(firstRecord, reduceForwardFrom, reduceForwardTo)

    val output = combine(reduceIterator)

    reduceSerializer.serialize(output, reduceRecord)
    out.collect(reduceRecord)
  }

  override def reduce(records: JIterator[PactRecord], out: Collector[PactRecord]) = {
    val firstRecord = reduceIterator.initialize(records)
    reduceRecord.copyFrom(firstRecord, reduceForwardFrom, reduceForwardTo)

    val output = reduce(reduceIterator)

    reduceSerializer.serialize(output, reduceRecord)
    out.collect(reduceRecord)
  }

  def reduce(records: Iterator[In]): Out
  def combine(records: Iterator[In]): Out

  def apply(record: Iterator[In]): Out = throw new RuntimeException("This should never be called.")
}