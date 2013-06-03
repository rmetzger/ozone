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
	
	/**
	 * Internal index that points to the next byte to write
	 */
	private int index = 0;
	
	/**
	 * Internal limit to simulate ByteBuffer behavior of MemorySegment. index > limit is not allowed.
	 */
	private int limit = 0;
	
	
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
		this.index = 0;
		this.limit = bufferSize;
	}

	private MemoryBuffer(final int bufferSize, final MemorySegment memory, final MemoryBufferRecycler bufferRecycler) {
		this.index = 0;
		this.limit = bufferSize;
		this.bufferRecycler = bufferRecycler;
		this.internalMemorySegment = memory;
	}

//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public int read(final Buffer dst) throws IOException {
//
//		if (this.writeMode.get()) {
//			throw new IOException("Buffer is still in write mode!");
//		}
//
//		if (!this.internalMemorySegment.hasRemaining()) {
//			return -1;
//		}
//		if (!dst.hasRemaining()) {
//			return 0;
//		}
//
//		final int oldPosition = this.internalMemorySegment.position();
//
//		if (dst.remaining() < this.internalMemorySegment.remaining()) {
//			final int excess = this.internalMemorySegment.remaining() - dst.remaining();
//			this.internalMemorySegment.limit(this.internalMemorySegment.limit() - excess);
//			dst.put(this.internalMemorySegment);
//			this.internalMemorySegment.limit(this.internalMemorySegment.limit() + excess);
//		} else {
//			dst.put(this.internalMemorySegment);
//		}
//
//		return (this.internalMemorySegment.position() - oldPosition);
//	}

//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public int read(final WritableByteChannel writableByteChannel) throws IOException {
//
//		if (this.writeMode.get()) {
//			throw new IOException("Buffer is still in write mode!");
//		}
//
//		if (!this.internalMemorySegment.hasRemaining()) {
//			return -1;
//		}
//
//		return writableByteChannel.write(this.internalMemorySegment);
//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {

		this.position(this.limit());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {

		return this.hasRemaining();
	}

	/**
	 * Resets the memory buffer.
	 * 
	 * @param bufferSize
	 *        the size of buffer in bytes after the reset
	 */
	public void reset(final int bufferSize) {

		this.writeMode.set(true);
		this.position(0);
		this.limit(bufferSize);
	}

	public final void position(final int i) {
		if(i > limit) {
			throw new IndexOutOfBoundsException("new position is larger than the limit");
		}
		index = i;
	}
	
	public final int position() {
		return index;
	}
	
	public final void limit(final int l) {
		limit = l;
	}
	
	public final int limit() {
		return limit;
	}
	
	public final boolean hasRemaining() {
		return index < limit;
    }
	
	public final int remaining() {
		return limit - index;
	}



	/**
	 * 	limit() = size() <= getTotalSize()
	 * 
	 * @return Returns the size of the underlying MemorySegment
	 */
	public int getTotalSize() {
		return this.internalMemorySegment.size();
	}
	
	@Override
	public int size() {
		return this.limit();
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

		final MemoryBuffer duplicatedMemoryBuffer = new MemoryBuffer(this.limit(),
			this.internalMemorySegment, this.bufferRecycler);

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
		final MemoryBuffer target = (MemoryBuffer) destinationBuffer;
//		final int oldPos = this.position();
//		this.position(0);

		System.arraycopy(this.getMemorySegment().getBackingArray(), 0,
				target.getMemorySegment().getBackingArray(),target.index, this.position());
//		while (remaining() > 0) {
//			destinationBuffer.write(this.internalMemorySegment);
//		}

	//	this.position(oldPos);

		destinationBuffer.finishWritePhase();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isInWriteMode() {

		return this.writeMode.get();
	}
//
//	@Override
//	public int readIntoBuffer(Buffer destination) throws IOException {
//		return 0;
//	}

//	@Override
//	public int write(Buffer source) throws IOException {
//		this.internalMemorySegment.
//	}



	@Override
	public void write(int index, byte[] srcBuffer) {
		this.internalMemorySegment.put(index, srcBuffer);
	}


	
	 
	/**
	 * ATTENTION: THESE METHODS ARE FOR LEGACY SUPPORT ONLY
	 */
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int write(final ByteBuffer src) throws IOException {

		if (!this.writeMode.get()) {
			throw new IOException("Cannot write to buffer, buffer already switched to read mode");
		}

		final int sourceRemaining = src.remaining();
		final int thisRemaining = this.remaining();
		final int excess = sourceRemaining - thisRemaining;
		final int initialIndex = index;
		while(src.hasRemaining()) {
			this.internalMemorySegment.put(index, src.get());
			index++;
		}
		System.err.println("write(byteBuffer) has written "+(index-initialIndex)+" bytes");
		return index-initialIndex;
//		if (excess <= 0) {
//			// there is enough space here for all the source data
//			this.internalMemorySegment.put(src);
//			return sourceRemaining;
//		} else {
//			// not enough space here, we need to limit the source
//			final int oldLimit = src.limit();
//			src.limit(src.position() + thisRemaining);
//			this.internalMemorySegment.put(src);
//			src.limit(oldLimit);
//			return thisRemaining;
//		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int write(final ReadableByteChannel readableByteChannel) throws IOException {

		if (!this.writeMode.get()) {
			throw new IOException("Cannot write to buffer, buffer already switched to read mode");
		}

		if (!this.hasRemaining()) {
			return 0;
		}

		return readableByteChannel.read(this.internalMemorySegment.wrap(index, limit-index));
	}
	
	@Override
	public int read(ByteBuffer dst) throws IOException {
		final int oldIndex = index;
		for(; index < limit; index++) {
			if(!dst.hasRemaining()) {
				index--; // repair index
				break;
			}
			dst.put(this.internalMemorySegment.get(index));
		}
		return index-oldIndex;
	}

	@Override
	public void writeBufferToByteChannel(WritableByteChannel writableByteChannel) {
		try {
			writableByteChannel.write(this.internalMemorySegment.wrap(index, limit-index));
		} catch (IOException e) {
			throw new RuntimeException("Error while writing buffer to channel", e);
		}
	}
	
	
}
