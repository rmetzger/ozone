/***********************************************************************************************************************
 *
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
 *
 **********************************************************************************************************************/

package eu.stratosphere.nephele.io.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.stratosphere.nephele.services.memorymanager.MemorySegment;

public final class MemoryBuffer extends Buffer {

	private final MemoryBufferRecycler bufferRecycler;

	private final MemorySegment internalMemorySegment;

	private final AtomicBoolean writeMode = new AtomicBoolean(true);
	
	/*
	 * 
	 *  PRIVATE NOTES BY ROBERT
	 *  
	 *  PLAN:
	 *  - make everything outside the MemoryBuffer compatible to it
	 *  - fix MemoryBuffer inside, even if it requres expensive operations such as copying from 
	 *  	ByteBuffers and byte-arrays.
	 *  - if the test cases are stable after this step, gradually replace the ByteBuffers with byte-arrays
	 *  	Wherever possible.
	 * 
	 * 
	 */

	MemoryBuffer(final int bufferSize, final MemorySegment memory, final MemoryBufferPoolConnector bufferPoolConnector) {

		if (bufferSize > memory.size()) {
			throw new IllegalArgumentException("Requested segment size is " + bufferSize
				+ ", but provided MemorySegment only has a capacity of " + memory.size());
		}

		this.bufferRecycler = new MemoryBufferRecycler(memory, bufferPoolConnector);
		
		this.internalMemorySegment = memory;
	}

	private MemoryBuffer(final int bufferSize, final MemorySegment memory, final MemoryBufferRecycler bufferRecycler) {

		this.bufferRecycler = bufferRecycler;
		this.internalMemorySegment = memory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(final Buffer dst) throws IOException {

		if (this.writeMode.get()) {
			throw new IOException("Buffer is still in write mode!");
		}

		if (!this.internalMemorySegment.hasRemaining()) {
			return -1;
		}
		if (!dst.hasRemaining()) {
			return 0;
		}

		final int oldPosition = this.internalMemorySegment.position();

		if (dst.remaining() < this.internalMemorySegment.remaining()) {
			final int excess = this.internalMemorySegment.remaining() - dst.remaining();
			this.internalMemorySegment.limit(this.internalMemorySegment.limit() - excess);
			dst.put(this.internalMemorySegment);
			this.internalMemorySegment.limit(this.internalMemorySegment.limit() + excess);
		} else {
			dst.put(this.internalMemorySegment);
		}

		return (this.internalMemorySegment.position() - oldPosition);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(final WritableByteChannel writableByteChannel) throws IOException {

		if (this.writeMode.get()) {
			throw new IOException("Buffer is still in write mode!");
		}

		if (!this.internalMemorySegment.hasRemaining()) {
			return -1;
		}

		return writableByteChannel.write(this.internalMemorySegment);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {

		this.internalMemorySegment.position(this.internalMemorySegment.limit());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {

		return this.internalMemorySegment.hasRemaining();
	}

	/**
	 * Resets the memory buffer.
	 * 
	 * @param bufferSize
	 *        the size of buffer in bytes after the reset
	 */
	public void reset(final int bufferSize) {

		this.writeMode.set(true);
		this.internalMemorySegment.position(0);
		this.internalMemorySegment.limit(bufferSize);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int write(final ByteBuffer src) throws IOException {

		if (!this.writeMode.get()) {
			throw new IOException("Cannot write to buffer, buffer already switched to read mode");
		}

		final int sourceRemaining = src.remaining();
		final int thisRemaining = this.internalMemorySegment.remaining();
		final int excess = sourceRemaining - thisRemaining;

		if (excess <= 0) {
			// there is enough space here for all the source data
			this.internalMemorySegment.put(src);
			return sourceRemaining;
		} else {
			// not enough space here, we need to limit the source
			final int oldLimit = src.limit();
			src.limit(src.position() + thisRemaining);
			this.internalMemorySegment.put(src);
			src.limit(oldLimit);
			return thisRemaining;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int write(final ReadableByteChannel readableByteChannel) throws IOException {

		if (!this.writeMode.get()) {
			throw new IOException("Cannot write to buffer, buffer already switched to read mode");
		}

		if (!this.internalMemorySegment.hasRemaining()) {
			return 0;
		}

		return readableByteChannel.read(this.internalMemorySegment);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int remaining() {
		return this.internalMemorySegment.remaining();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return this.internalMemorySegment.limit();
	}

	public MemorySegment getMemorySegment() {
		return this.internalMemorySegment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void recycle() {

		this.bufferRecycler.decreaseReferenceCounter();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finishWritePhase() {

		if (!this.writeMode.compareAndSet(true, false)) {
			throw new IllegalStateException("MemoryBuffer is already in read mode!");
		}

		//this.internalMemorySegment.flip();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isBackedByMemory() {

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MemoryBuffer duplicate() {

		if (this.writeMode.get()) {
			throw new IllegalStateException("Cannot duplicate buffer that is still in write mode");
		}

		final MemoryBuffer duplicatedMemoryBuffer = new MemoryBuffer(this.internalMemorySegment.limit(),
			this.internalMemorySegment.duplicate(), this.bufferRecycler);

		this.bufferRecycler.increaseReferenceCounter();
		duplicatedMemoryBuffer.writeMode.set(this.writeMode.get());
		return duplicatedMemoryBuffer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyToBuffer(final Buffer destinationBuffer) throws IOException {

		if (this.writeMode.get()) {
			throw new IllegalStateException("Cannot copy buffer that is still in write mode");
		}
		if (size() > destinationBuffer.size()) {
			throw new IllegalArgumentException("Destination buffer is too small to store content of source buffer: "
				+ size() + " vs. " + destinationBuffer.size());
		}

		final int oldPos = this.internalMemorySegment.position();
		this.internalMemorySegment.position(0);

		while (remaining() > 0) {
			destinationBuffer.write(this.internalMemorySegment);
		}

		this.internalMemorySegment.position(oldPos);

		destinationBuffer.finishWritePhase();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isInWriteMode() {

		return this.writeMode.get();
	}

	@Override
	public int readIntoBuffer(Buffer destination) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int write(Buffer source) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}


}
