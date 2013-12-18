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

import eu.stratosphere.api.operators.WorksetIteration.SolutionSetPlaceHolder;
import eu.stratosphere.compiler.dataproperties.GlobalProperties;
import eu.stratosphere.compiler.dataproperties.LocalProperties;
import eu.stratosphere.compiler.plan.PlanNode;
import eu.stratosphere.compiler.plan.SolutionSetPlanNode;

/**
 * The optimizer's internal representation of the solution set of a workset iteration.
 */
public class SolutionSetNode extends AbstractPartialSolutionNode
{
	private final WorksetIterationNode iterationNode;
	
	
	public SolutionSetNode(SolutionSetPlaceHolder psph, WorksetIterationNode iterationNode) {
		super(psph);
		this.iterationNode = iterationNode;
	}

	// --------------------------------------------------------------------------------------------
	
	public void setCandidateProperties(GlobalProperties gProps, LocalProperties lProps) {
		if (this.cachedPlans != null) {
			throw new IllegalStateException();
		} else {
			this.cachedPlans = Collections.<PlanNode>singletonList(new SolutionSetPlanNode(this, "SolutionSet("+this.getPactContract().getName()+")", gProps, lProps));
		}
	}
	
	public SolutionSetPlanNode getCurrentSolutionSetPlanNode() {
		if (this.cachedPlans != null) {
			return (SolutionSetPlanNode) this.cachedPlans.get(0);
		} else {
			throw new IllegalStateException();
		}
	}
	
	public WorksetIterationNode getIterationNode() {
		return this.iterationNode;
	}
	
	// --------------------------------------------------------------------------------------------

	/**
	 * Gets the contract object for this data source node.
	 * 
	 * @return The contract.
	 */
	@Override
	public SolutionSetPlaceHolder getPactContract() {
		return (SolutionSetPlaceHolder) super.getPactContract();
	}

	@Override
	public String getName() {
		return "Solution Set";
	}
}