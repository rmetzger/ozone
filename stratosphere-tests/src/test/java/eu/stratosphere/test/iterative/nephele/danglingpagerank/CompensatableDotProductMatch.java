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

package eu.stratosphere.test.iterative.nephele.danglingpagerank;

import eu.stratosphere.api.record.functions.JoinFunction;
import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.test.iterative.nephele.ConfigUtils;
import eu.stratosphere.types.PactDouble;
import eu.stratosphere.types.PactLong;
import eu.stratosphere.types.PactRecord;
import eu.stratosphere.util.Collector;

import java.util.Random;
import java.util.Set;

public class CompensatableDotProductMatch extends JoinFunction {

  private PactRecord record;
  private PactLong vertexID;
  private PactDouble partialRank;

  private PactDouble rank = new PactDouble();
  private LongArrayView adjacentNeighbors = new LongArrayView();

  private int workerIndex;
  private int currentIteration;
  private int failingIteration;

  private Set<Integer> failingWorkers;
  private double messageLoss;

  private Random random;

  @Override
  public void open(Configuration parameters) throws Exception {
    record = new PactRecord();
    vertexID = new PactLong();
    partialRank = new PactDouble();

    workerIndex = getRuntimeContext().getIndexOfThisSubtask();
    currentIteration = getIterationRuntimeContext().getSuperstepNumber();
    failingIteration = ConfigUtils.asInteger("compensation.failingIteration", parameters);
    failingWorkers = ConfigUtils.asIntSet("compensation.failingWorker", parameters);
    messageLoss = ConfigUtils.asDouble("compensation.messageLoss", parameters);

    random = new Random();
  }

  @Override
  public void match(PactRecord pageWithRank, PactRecord adjacencyList, Collector<PactRecord> collector)
      throws Exception {

    rank = pageWithRank.getField(1, rank);
    adjacentNeighbors = adjacencyList.getField(1, adjacentNeighbors);
    int numNeighbors = adjacentNeighbors.size();

    double rankToDistribute = rank.getValue() / (double) numNeighbors;

    partialRank.setValue(rankToDistribute);
    record.setField(1, partialRank);

    boolean isFailure = currentIteration == failingIteration && failingWorkers.contains(workerIndex);

    for (int n = 0; n < numNeighbors; n++) {
      vertexID.setValue(adjacentNeighbors.getQuick(n));
      record.setField(0, vertexID);

      if (isFailure) {
        if (random.nextDouble() >= messageLoss) {
          collector.collect(record);
        }
      } else {
        collector.collect(record);
      }
    }

  }


}
