package eu.stratosphere.nephele.io.channels;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.stratosphere.nephele.services.memorymanager.MemorySegment;
import eu.stratosphere.nephele.util.BufferPoolConnector;

public class TestMemoryBuffer {

	private MemoryBufferPoolConnector bufferPoolConnector;
	private Queue<MemorySegment> bufferPool;
	
	private final static int INT_COUNT = 512;
	private final static int INT_SIZE = Integer.SIZE / Byte.SIZE;

	@Before
	public void setUp() throws Exception {
		bufferPool = new LinkedBlockingQueue<MemorySegment>();
		bufferPoolConnector = new BufferPoolConnector(bufferPool);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void readToByteBuffer() throws IOException {
		
		MemoryBuffer buf = fillMemoryBuffer();
		ByteBuffer target = ByteBuffer.allocate(INT_COUNT*INT_SIZE);
		// call to be tested!
		buf.read(target);
		
		validateByteBuffer(target);
		
		buf.close(); // make eclipse happy
	}
	
	/**
	 * CopyToBuffer uses system.arraycopy()
	 * 
	 * @throws IOException
	 */
	@Test
	public void copyToBufferTest() throws IOException {

		MemoryBuffer buf = fillMemoryBuffer();
		
		MemoryBuffer destination = new MemoryBuffer(INT_COUNT*INT_SIZE*2, 
					new MemorySegment(new byte[INT_COUNT*INT_SIZE*2],0,INT_COUNT*INT_SIZE*2), 
					bufferPoolConnector);
		assertEquals(INT_COUNT*INT_SIZE*2, destination.limit());
		buf.copyToBuffer(destination);
		assertEquals(INT_COUNT*INT_SIZE, destination.limit());
		ByteBuffer test = ByteBuffer.allocate(INT_COUNT*INT_SIZE);
		destination.read(test);
		
		validateByteBuffer(test);
		
		buf.close(); // make eclipse happy
	}

	private MemoryBuffer fillMemoryBuffer() throws IOException {
		MemoryBuffer buf = new MemoryBuffer(INT_COUNT*INT_SIZE, new MemorySegment(new byte[INT_COUNT*INT_SIZE], 0, INT_COUNT*INT_SIZE), bufferPoolConnector);
		
		ByteBuffer src = ByteBuffer.allocate(INT_SIZE);
		// write some data into buf:
		for(int i = 0; i < INT_COUNT; ++i) {
			src.putInt(0,i);
			src.rewind();
			int written = buf.write(src);
			System.err.println("Put int i="+i+" Written "+written);
		}
		
		buf.finishWritePhase();
		return buf;
	}
	
	/**
	 * Validates if the ByteBuffer contains the what fillMemoryBuffer has written!
	 * 
	 * @param target
	 */
	private void validateByteBuffer(ByteBuffer target) {
		ByteBuffer ref = ByteBuffer.allocate(INT_SIZE);
		
		for(int i = 0; i < INT_SIZE*INT_COUNT; ++i) {
			ref.putInt(0,i / INT_SIZE);
			assertEquals("Byte at position "+i+" is different", ref.get(i%INT_SIZE), target.get(i));
		}
	}
	
}
