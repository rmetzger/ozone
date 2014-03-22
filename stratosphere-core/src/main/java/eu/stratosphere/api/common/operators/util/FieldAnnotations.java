package eu.stratosphere.api.common.operators.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class FieldAnnotations {

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD}) 
	public static @interface SerializableField {
		
	}
}
