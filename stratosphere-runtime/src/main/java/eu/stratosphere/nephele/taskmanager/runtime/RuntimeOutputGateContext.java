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

package eu.stratosphere.nephele.taskmanager.runtime;

import java.io.IOException;

import eu.stratosphere.core.io.IOReadableWritable;
import eu.stratosphere.nephele.io.AbstractID;
import eu.stratosphere.nephele.io.GateID;
import eu.stratosphere.nephele.io.OutputGate;
import eu.stratosphere.nephele.io.channels.AbstractOutputChannel;
import eu.stratosphere.nephele.io.channels.Buffer;
import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.io.channels.bytebuffered.AbstractByteBufferedOutputChannel;
import eu.stratosphere.nephele.taskmanager.bufferprovider.BufferAvailabilityListener;
import eu.stratosphere.nephele.taskmanager.bufferprovider.BufferProvider;
import eu.stratosphere.nephele.taskmanager.bytebuffered.AbstractOutputChannelForwarder;
import eu.stratosphere.nephele.taskmanager.bytebuffered.OutputChannelContext;
import eu.stratosphere.nephele.taskmanager.bytebuffered.OutputChannelForwardingChain;
import eu.stratosphere.nephele.taskmanager.bytebuffered.OutputGateContext;

final class RuntimeOutputGateContext implements BufferProvider, OutputGateContext {

	private final RuntimeTaskContext taskContext;

	private final OutputGate<? extends IOReadableWritable> outputGate;

	RuntimeOutputGateContext(final RuntimeTaskContext taskContext, final OutputGate<? extends IOReadableWritable> outputGate) {

		this.taskContext = taskContext;
		this.outputGate = outputGate;
	}

	AbstractID getFileOwnerID() {

		return this.taskContext.getFileOwnerID();
	}


	@Override
	public int getMaximumBufferSize() {

		return this.taskContext.getMaximumBufferSize();
	}


	@Override
	public Buffer requestEmptyBuffer(int minimumSizeOfBuffer) throws IOException {

		return this.taskContext.requestEmptyBuffer(minimumSizeOfBuffer);
	}


	@Override
	public Buffer requestEmptyBufferBlocking(int minimumSizeOfBuffer) throws IOException, InterruptedException {

		Buffer buffer = this.taskContext.requestEmptyBuffer(minimumSizeOfBuffer);

		// No memory-based buffer available
		if (buffer == null) {
			// Wait until a memory-based buffer is available
			buffer = this.taskContext.requestEmptyBufferBlocking(minimumSizeOfBuffer);
		}

		return buffer;
	}


	@Override
	public boolean isShared() {

		return this.taskContext.isShared();
	}


	@Override
	public void reportAsynchronousEvent() {

		this.taskContext.reportAsynchronousEvent();
	}


	@Override
	public GateID getGateID() {

		return this.outputGate.getGateID();
	}


	@Override
	public OutputChannelContext createOutputChannelContext(ChannelID channelID, OutputChannelContext previousContext,
			boolean isReceiverRunning, boolean mergeSpillBuffers) {

		if (previousContext != null) {
			throw new IllegalStateException("Found previous output context for channel " + channelID);
		}

		AbstractOutputChannel<? extends IOReadableWritable> channel = null;
		for (int i = 0; i < this.outputGate.getNumberOfOutputChannels(); ++i) {
			AbstractOutputChannel<? extends IOReadableWritable> candidateChannel = this.outputGate.getOutputChannel(i);
			if (candidateChannel.getID().equals(channelID)) {
				channel = candidateChannel;
				break;
			}
		}

		if (channel == null) {
			throw new IllegalArgumentException("Cannot find output channel with ID " + channelID);
		}

		if (!(channel instanceof AbstractByteBufferedOutputChannel)) {
			throw new IllegalStateException("Channel with ID" + channelID
				+ " is not of type AbstractByteBufferedOutputChannel");
		}

		// The output channel for this context
		final AbstractByteBufferedOutputChannel<? extends IOReadableWritable> outputChannel = (AbstractByteBufferedOutputChannel<? extends IOReadableWritable>) channel;

		// Construct the forwarding chain
		RuntimeOutputChannelBroker outputChannelBroker;
		AbstractOutputChannelForwarder last;
		// Construction for in-memory and network channels
		final RuntimeDispatcher runtimeDispatcher = new RuntimeDispatcher(
			this.taskContext.getTransferEnvelopeDispatcher());
		/*
		 * final SpillingBarrier spillingBarrier = new SpillingBarrier(isReceiverRunning, mergeSpillBuffers,
		 * runtimeDispatcher);
		 * final ForwardingBarrier forwardingBarrier = new ForwardingBarrier(channelID, spillingBarrier);
		 */
		final ForwardingBarrier forwardingBarrier = new ForwardingBarrier(channelID, runtimeDispatcher);
		outputChannelBroker = new RuntimeOutputChannelBroker(this, outputChannel, forwardingBarrier);
		last = runtimeDispatcher;

		final OutputChannelForwardingChain forwardingChain = new OutputChannelForwardingChain(outputChannelBroker, last);

		// Set forwarding chain for broker
		outputChannelBroker.setForwardingChain(forwardingChain);

		return new RuntimeOutputChannelContext(outputChannel, forwardingChain);
	}


	@Override
	public boolean registerBufferAvailabilityListener(final BufferAvailabilityListener bufferAvailabilityListener) {

		return this.taskContext.registerBufferAvailabilityListener(bufferAvailabilityListener);
	}
}
