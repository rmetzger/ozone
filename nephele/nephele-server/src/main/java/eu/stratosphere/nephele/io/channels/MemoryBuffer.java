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
	private void debug(String in) {
	//	System.err.println(this+"[index="+index+" limit="+limit+"] "+in);
	}

	MemoryBuffer(final int bufferSize, final MemorySegment memory, final MemoryBufferPoolConnector bufferPoolConnector) {
		if (bufferSize > memory.size()) {
			throw new IllegalArgumentException("Requested segment size is " + bufferSize
				+ ", but provided MemorySegment only has a capacity of " + memory.size());
		}

		this.bufferRecycler = new MemoryBufferRecycler(memory, bufferPoolConnector);
		this.internalMemorySegment = memory;
		this.position(0);
		this.limit(bufferSize);
		debug("Init bufferSize="+bufferSize);
	}

	private MemoryBuffer(final int bufferSize, final MemorySegment memory, final MemoryBufferRecycler bufferRecycler) {
		debug("private ctor limit = "+bufferSize);
		this.position(0);
		this.limit(bufferSize);
		this.bufferRecycler = bufferRecycler;
		this.internalMemorySegment = memory;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		debug("Read to byteBuiffer");
		
		if (this.writeMode.get()) {
			throw new IOException("Buffer is still in write mode!");
		}

		if (!this.hasRemaining()) {
			return -1;
		}
		
		if (!dst.hasRemaining()) {
			return 0;
		}
			
		final int oldIndex = index;
		for(; index < limit; index++) {
			if(!dst.hasRemaining()) {
				break;
			}
			dst.put(this.internalMemorySegment.get(index));
		}
		// System.err.println("have read "+(index-oldIndex)+" dst.hasRem()="+dst.hasRemaining());
		return index-oldIndex;
	}
	

	@Override
	public int read(WritableByteChannel writableByteChannel) throws IOException {
		debug("read(writeable) offset="+index+" length="+ (limit-index));
		
		if (this.writeMode.get()) {
			throw new IOException("Buffer is still in write mode!");
		}
		if (!this.hasRemaining()) {
			return -1;
		}
		
		final ByteBuffer wrapped = this.internalMemorySegment.wrap(index, limit-index);
		final int written = writableByteChannel.write(wrapped);
		debug("written = "+written);
	//	position(position() + written);
		position(wrapped.position());
		return written;
	}

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
		if(bufferSize > this.internalMemorySegment.size()) {
			throw new RuntimeException("Given buffer size exceeds buffer size");
		}
		debug("reset to "+bufferSize);
		this.writeMode.set(true);
		this.position(0);
		this.limit(bufferSize);
	}

	public final void position(final int i) {
		debug("Set pos to "+i);
		if(i > limit) {
			throw new IndexOutOfBoundsException("new position is larger than the limit");
		}
		index = i;
	}
	
	public final int position() {
		return index;
	}
	
	public final void limit(final int l) {
		if(limit > index) {
			throw new RuntimeException("Limit is behind the current position.");
		}
		if(limit >= internalMemorySegment.size()) {
			throw new RuntimeException("Limit is larger than MemoryBuffer size");
		}
		debug("Set limit to "+l);
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
	 * Put MemoryBuffer into read mode
	 */
	public final void flip() {
		limit = position();
		position(0);
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
		if(bufferRecycler.referenceCounter.get() == 0) {
			//Continue here
			clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finishWritePhase() {
		debug("Finish write phase");
		if (!this.writeMode.compareAndSet(true, false)) {
			throw new IllegalStateException("MemoryBuffer is already in read mode!");
		}

		this.flip();
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
		debug("Creating duplicate()");
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
		debug("CopyToBuffer (Dest idx="+target.index+" limit="+target.limit+")");
//		final int oldPos = this.position();
//		this.position(0);
		
		//(Object src, int srcPos, Object dest, int destPos, int length
		System.arraycopy(this.getMemorySegment().getBackingArray(), 0,
				target.getMemorySegment().getBackingArray(),target.position(), limit()- position() );
		target.position(limit()-position()); // even if we do not change the source (this), we change the destination!!
		destinationBuffer.finishWritePhase();
		//		while (remaining() > 0) {
//			destinationBuffer.write(this.internalMemorySegment);
//		}

	//	this.position(oldPos);
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



	/**
	 * Writes the given bytes to a random position.
	 * The internal index of MemoryBuffer is unaffected by this operation!
	 * 
	 * THIS METHOD HAS BEEN USED IN DefaultDeserializerTest.createByteChannel() to get rid
	 * of the ByteBuffers used there. But sadly, it seems that I've done some stuff wrong!
	 */
	@Override
	public void write(int idx, byte[] srcBuffer) {
		if(idx + srcBuffer.length > this.limit) {
			throw new RuntimeException("The given byteArray does not fit into this MemoryBuffer");
		}
		debug("Writinge byte array to "+idx);
		this.internalMemorySegment.put(idx, srcBuffer);
		// this.index += srcBuffer.length;
	}


	
	 
	/**
	 * ATTENTION: THESE METHODS ARE FOR LEGACY SUPPORT ONLY
	 */
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int write(final ByteBuffer src) throws IOException {
		debug("Writing ByteBuffer");
		if (!this.writeMode.get()) {
			throw new IOException("Cannot write to buffer, buffer already switched to read mode");
		}
		
//		if(src.remaining() > remaining()) {
//			// this exception is probably not necessary
//			throw new RuntimeException("Can not read "+src.remaining()+" bytes into a " +
//					"MemoryBuffer with "+remaining()+" bytes remaining");
//		}

		final int initialPos = position();
		while(src.hasRemaining()) {
			if(position() >= limit()) {
				// COMPARE TO ORIGINAL METHOD. Might be that there are some very crazy assumptions here
				debug("++++Preliminary end++++");
				return index-initialPos;
			}
			this.internalMemorySegment.put(position(), src.get());
			position(position()+1); // little optimizer exercise for index++
		}
		debug("write(byteBuffer) has written "+(position()-initialPos)+" bytes");
		return position()-initialPos;
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
		debug("Write (actually) read from readableByteChannel");
		if (!this.writeMode.get()) {
			throw new IOException("Cannot write to buffer, buffer already switched to read mode");
		}

		if (!this.hasRemaining()) {
			return 0;
		}
		ByteBuffer wrapper = this.internalMemorySegment.wrap(index, limit-index);
		final int written = readableByteChannel.read(wrapper);
		this.position(wrapper.position());
		this.limit(wrapper.limit());
//		if(written == -1) {
//			position(limit);
//		}
//		position(position()+written);
		return written;
	}

	public void clear() {
		this.limit = getTotalSize();
		this.position(0);
		this.writeMode.set(true);
	}
	


	
	
}
