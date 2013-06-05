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
		
		
		
		MemoryBuffer buf = new MemoryBuffer(512*4, new MemorySegment(new byte[512*4], 0, 512*4), bufferPoolConnector);
		
		ByteBuffer src = ByteBuffer.allocate(4);
		// write some data into buf:
		for(int i = 0; i < 512; ++i) {
			src.putInt(0,i);
			src.rewind();
			int written = buf.write(src);
			System.err.println("Put int i="+i+" Written "+written);
		}
		
		buf.finishWritePhase();
		
		ByteBuffer target = ByteBuffer.allocate(512*4);
		// call to be tested!
		buf.read(target);
		
		
		ByteBuffer ref = ByteBuffer.allocate(4);
		
		for(int i = 0; i < 4*512; ++i) {
			ref.putInt(0,i / 4);
			assertEquals("Byte at position "+i+" is different", ref.get(i%4), target.get(i));
		}
		buf.close(); // make eclipse happy
	}

}
