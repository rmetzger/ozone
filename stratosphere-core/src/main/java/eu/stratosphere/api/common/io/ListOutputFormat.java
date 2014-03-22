package eu.stratosphere.api.common.io;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.types.Record;

public class ListOutputFormat implements OutputFormat<Record> {
	private static final long serialVersionUID = 1L;
	
	public static List<Record> coll = new LinkedList<Record>();
	
	public ListOutputFormat() {
	}
	public List<Record> getCollection() {
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
