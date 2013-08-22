package eu.stratosphere.pact.common.io;

import java.io.IOException;

import eu.stratosphere.pact.common.type.PactRecord;

public class AvroInputFormat extends FileInputFormat {

	@Override
	public boolean reachedEnd() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean nextRecord(PactRecord record) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

}
