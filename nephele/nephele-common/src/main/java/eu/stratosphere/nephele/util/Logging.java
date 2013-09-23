package eu.stratosphere.nephele.util;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Logging {
	
	/**
	 * Taken from:
	 * http://robertmaldon.blogspot.de/2007/09/programmatically-configuring
	 * -log4j-and.html
	 */
	public static void initialize() {
		Logger rootLogger = Logger.getRootLogger();
		if (!rootLogger.getAllAppenders().hasMoreElements()) {
			rootLogger.setLevel(Level.DEBUG);
			rootLogger.addAppender(new ConsoleAppender(new PatternLayout("%-5p [%t]: %m%n"), "system.err"));
		}
	}
}
