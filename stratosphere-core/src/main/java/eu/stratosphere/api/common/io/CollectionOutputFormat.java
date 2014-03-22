package eu.stratosphere.api.common.io;

import java.io.IOException;
import java.util.Collection;

import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.types.Record;

public class CollectionOutputFormat implements OutputFormat<Record> {
	private static final long serialVersionUID = 1L;
	
	private Collection<Record> coll;
	
	public CollectionOutputFormat(Collection<Record> out) {
		this.coll = out;
	}
	public Collection<Record> getCollection() {
		return coll;
	}
	
	@Override
	public void configure(Configuration parameters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void open(int taskNumber, int numTasks) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeRecord(Record record) throws IOException {
		System.err.println("CollectionOutputFormat is adding "+record);
		coll.add(record.createCopy());
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
