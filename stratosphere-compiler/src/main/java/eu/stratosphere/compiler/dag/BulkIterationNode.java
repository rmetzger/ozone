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

package eu.stratosphere.compiler.dag;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import eu.stratosphere.api.operators.BulkIteration;
import eu.stratosphere.compiler.CompilerException;
import eu.stratosphere.compiler.DataStatistics;
import eu.stratosphere.compiler.PactCompiler.InterestingPropertyVisitor;
import eu.stratosphere.compiler.costs.CostEstimator;
import eu.stratosphere.compiler.dataproperties.GlobalProperties;
import eu.stratosphere.compiler.dataproperties.InterestingProperties;
import eu.stratosphere.compiler.dataproperties.LocalProperties;
import eu.stratosphere.compiler.dataproperties.RequestedGlobalProperties;
import eu.stratosphere.compiler.dataproperties.RequestedLocalProperties;
import eu.stratosphere.compiler.operators.NoOpDescriptor;
import eu.stratosphere.compiler.operators.OperatorDescriptorSingle;
import eu.stratosphere.compiler.plan.BulkIterationPlanNode;
import eu.stratosphere.compiler.plan.BulkPartialSolutionPlanNode;
import eu.stratosphere.compiler.plan.Channel;
import eu.stratosphere.compiler.plan.PlanNode;
import eu.stratosphere.util.Visitor;

/**
 * A node in the optimizer's program representation for a bulk iteration.
 */
public class BulkIterationNode extends SingleInputNode implements IterationNode {
	
	private BulkPartialSolutionNode partialSolution;
	
	private OptimizerNode nextPartialSolution;
	
	private PactConnection rootConnection;
	
	private int costWeight;

	// --------------------------------------------------------------------------------------------
	
	/**
	 * Creates a new node with a single input for the optimizer plan.
	 * 
	 * @param pactContract The PACT that the node represents.
	 */
	public BulkIterationNode(BulkIteration iteration) {
		super(iteration);
		
		if (iteration.getMaximumNumberOfIterations() <= 0) {
			throw new CompilerException("BulkIteration must have a maximum number of iterations specified.");
		}
		
		this.costWeight = iteration.getMaximumNumberOfIterations();
	}

	// --------------------------------------------------------------------------------------------
	
	public BulkIteration getIterationContract() {
		return (BulkIteration) getPactContract();
	}
	
	/**
	 * Gets the partialSolution from this BulkIterationNode.
	 *
	 * @return The partialSolution.
	 */
	public BulkPartialSolutionNode getPartialSolution() {
		return partialSolution;
	}
	
	/**
	 * Sets the partialSolution for this BulkIterationNode.
	 *
	 * @param partialSolution The partialSolution to set.
	 */
	public void setPartialSolution(BulkPartialSolutionNode partialSolution) {
		this.partialSolution = partialSolution;
	}

	
	/**
	 * Gets the nextPartialSolution from this BulkIterationNode.
	 *
	 * @return The nextPartialSolution.
	 */
	public OptimizerNode getNextPartialSolution() {
		return nextPartialSolution;
	}

	
	/**
	 * Sets the nextPartialSolution for this BulkIterationNode.
	 *
	 * @param nextPartialSolution The nextPartialSolution to set.
	 */
	public void setNextPartialSolution(OptimizerNode nextPartialSolution, PactConnection rootingConnection) {
		this.nextPartialSolution = nextPartialSolution;
		this.rootConnection = rootingConnection;
	}
	
	public int getCostWeight() {
		return this.costWeight;
	}

	// --------------------------------------------------------------------------------------------
	
	@Override
	public String getName() {
		return "Bulk Iteration";
	}
	
	@Override
	public boolean isFieldConstant(int input, int fieldNumber) {
		return false;
	}
	
	protected void readStubAnnotations() {
	}
	
	public void computeOutputEstimates(DataStatistics statistics) {
		// simply copy from the inputs
		final OptimizerNode n = this.inConn.getSource();
		this.estimatedCardinality = n.estimatedCardinality;
		this.estimatedOutputSize = n.estimatedOutputSize;
		this.estimatedNumRecords = n.estimatedNumRecords;
	}
	
	// --------------------------------------------------------------------------------------------
	//                             Properties and Optimization
	// --------------------------------------------------------------------------------------------
	
	protected List<OperatorDescriptorSingle> getPossibleProperties() {
		return Collections.<OperatorDescriptorSingle>singletonList(new NoOpDescriptor());
	}
	
	@Override
	public boolean isMemoryConsumer() {
		return true;
	}
	
	@Override
	public void computeInterestingPropertiesForInputs(CostEstimator estimator) {
		final InterestingProperties intProps = getInterestingProperties().clone();
		
		// we need to make 2 interesting property passes, because the root of the step function needs also
		// the interesting properties as generated by the partial solution
		
		// give our own interesting properties (as generated by the iterations successors) to the step function and
		// make the first pass
		this.rootConnection.setInterestingProperties(intProps);
		this.nextPartialSolution.accept(new InterestingPropertyVisitor(estimator));
		
		// take the interesting properties of the partial solution and add them to the root interesting properties
		InterestingProperties partialSolutionIntProps = this.partialSolution.getInterestingProperties();
		intProps.getGlobalProperties().addAll(partialSolutionIntProps.getGlobalProperties());
		intProps.getLocalProperties().addAll(partialSolutionIntProps.getLocalProperties());
		
		// clear all interesting properties to prepare the second traversal
		this.rootConnection.clearInterestingProperties();
		this.nextPartialSolution.accept(InterestingPropertiesClearer.INSTANCE);
		
		// 2nd pass
		this.rootConnection.setInterestingProperties(intProps);
		this.nextPartialSolution.accept(new InterestingPropertyVisitor(estimator));
		
		// now add the interesting properties of the partial solution to the input
		final InterestingProperties inProps = this.partialSolution.getInterestingProperties().clone();
		inProps.addGlobalProperties(new RequestedGlobalProperties());
		inProps.addLocalProperties(new RequestedLocalProperties());
		this.inConn.setInterestingProperties(inProps);
	}
	
	@Override
	protected void instantiateCandidate(OperatorDescriptorSingle dps, Channel in, List<PlanNode> target,
			CostEstimator estimator, RequestedGlobalProperties globPropsReq, RequestedLocalProperties locPropsReq)
	{
		// NOTES ON THE ENUMERATION OF THE STEP FUNCTION PLANS:
		// Whenever we instantiate the iteration, we enumerate new candidates for the step function.
		// That way, we make sure we have an appropriate plan for each candidate for the initial partial solution,
		// we have a fitting candidate for the step function (often, work is pushed out of the step function).
		// Among the candidates of the step function, we keep only those that meet the requested properties of the
		// current candidate initial partial solution. That makes sure these properties exist at the beginning of
		// the successive iteration.
		
		// 1) Because we enumerate multiple times, we may need to clean the cached plans
		//    before starting another enumeration
		this.nextPartialSolution.accept(PlanCacheCleaner.INSTANCE);
		
		// 2) Give the partial solution the properties of the current candidate for the initial partial solution
		this.partialSolution.setCandidateProperties(in.getGlobalProperties(), in.getLocalProperties());
		final BulkPartialSolutionPlanNode pspn = this.partialSolution.getCurrentPartialSolutionPlanNode();
		
		// 3) Get the alternative plans
		List<PlanNode> candidates = this.nextPartialSolution.getAlternativePlans(estimator);
		
		// 4) Throw away all that are not compatible with the properties currently requested to the
		//    initial partial solution
		for (Iterator<PlanNode> planDeleter = candidates.iterator(); planDeleter.hasNext(); ) {
			PlanNode candidate = planDeleter.next();
			if (!(globPropsReq.isMetBy(candidate.getGlobalProperties()) && locPropsReq.isMetBy(candidate.getLocalProperties()))) {
				planDeleter.remove();
			}
		}
		
		// 5) Create a candidate for the Iteration Node for every remaining plan of the step function.
		for (PlanNode candidate : candidates) {
			BulkIterationPlanNode node = new BulkIterationPlanNode(this, "BulkIteration ("+this.getPactContract().getName()+")", in, pspn, candidate);
			GlobalProperties gProps = candidate.getGlobalProperties().clone();
			LocalProperties lProps = candidate.getLocalProperties().clone();
			node.initProperties(gProps, lProps);
			target.add(node);
		}
	}
	
	// --------------------------------------------------------------------------------------------
	//                      Iteration Specific Traversals
	// --------------------------------------------------------------------------------------------

	public void acceptForStepFunction(Visitor<OptimizerNode> visitor) {
		this.nextPartialSolution.accept(visitor);
	}
}
