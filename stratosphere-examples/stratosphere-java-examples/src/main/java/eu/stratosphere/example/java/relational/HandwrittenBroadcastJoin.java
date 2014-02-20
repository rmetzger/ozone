package eu.stratosphere.example.java.relational;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import eu.stratosphere.api.java.DataSet;
import eu.stratosphere.api.java.ExecutionEnvironment;
import eu.stratosphere.api.java.functions.FlatMapFunction;
import eu.stratosphere.api.java.functions.MapFunction;
import eu.stratosphere.api.java.io.CsvReader;
import eu.stratosphere.api.java.record.io.CsvInputFormat;
import eu.stratosphere.api.java.tuple.Tuple2;
import eu.stratosphere.api.java.tuple.Tuple3;
import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.types.IntValue;
import eu.stratosphere.types.StringValue;
import eu.stratosphere.util.Collector;

public class HandwrittenBroadcastJoin {
	
	public static class ArgUtiliy {
		private String[] args;
		int pos = 0;

		public ArgUtiliy(String... args) {
			this.args = args;
		}
		private void check() {
			if(pos >= args.length) {
				throw new RuntimeException("There are only "+args.length+" available");
			}
		}

		public int integer() {
			check();
			return Integer.parseInt(args[pos++]);
		}
		
		public int integer(int def) {
			if(pos < args.length) {
				return Integer.parseInt(args[pos++]);
			} else {
				return def;
			}
		}
		public String str() {
			check();
			return args[pos++];
		}
		
		public String str(String def) {
			if(pos < args.length) {
				return args[pos++];
			} else {
				return def;
			}
		}
	}
	
	public static class User {
		private int id;
		private String name;
		private String password;
		public User() {
			
		}
		public User(Integer id, String name, String password) {
			super();
			this.id = id;
			this.name = name;
			this.password = password;
		}
		public int getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
	}
	public static class Comment {
		private int uid;
		private String comment;
		public Comment() {
			
		}
		public Comment(int u) {
			uid = u;
			comment = "Hello Stratosphere!";
		}
		public int getUid() {
			return uid;
		}
	}
	
	public static class CommentsGenerator implements Iterator<Comment>, Serializable {
		private int i = 0;
		
		@Override
		public boolean hasNext() {
			return i++ < 500;
		}

		@Override
		public Comment next() {
			return new Comment(i);
		}

		@Override
		public void remove() {}
		
	}
	
	public static class ToUserObject extends MapFunction<Tuple3<Integer, String, String>, User> {

		@Override
		public User map(Tuple3<Integer, String, String> value)
				throws Exception {
			return new User( value.getFirst(), value.T2(), (String)value.getField(2));
		}
	}
	
	public static class HandJoin extends FlatMapFunction<User, Tuple2<User, Comment>> {

		private Collection<Comment> com;
		
		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			com = getRuntimeContext().getBroadcastVariable("comments");
		}
		@Override
		public void flatMap(User value, Collector<Tuple2<User, Comment>>  out) throws Exception {
			Iterator<Comment> it = com.iterator();
			while(it.hasNext()) {
				Comment c = it.next();
				if(c.getUid() == value.getId()) {
					out.collect( new Tuple2<User, Comment>(value, c));
				}
			}
			System.err.println("No match!");
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		ArgUtiliy argx = new ArgUtiliy(args);
		final String usersPath = argx.str();
		final String resultPath = argx.str();
		
		final ExecutionEnvironment context = ExecutionEnvironment.getExecutionEnvironment();
		
		DataSet<Comment> userComments = context.fromCollection(new CommentsGenerator(), Comment.class);
		DataSet<Tuple3<Integer, String, String>> usersTuples = context.readCsvFile(usersPath).types(Integer.class, String.class, String.class);
		DataSet<User> usersUsers = usersTuples.map(new ToUserObject());
		
		DataSet<Tuple2<User, Comment>> joined = usersUsers.flatMap(new HandJoin()).withBroadcastSet(userComments, "comments");
		
		joined.writeAsCsv(resultPath);
		
		joined.print();
		
		context.execute();
		
	}
}
