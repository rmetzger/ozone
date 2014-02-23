package eu.stratosphere.types;

public interface JavaValue<T> {
	public T getObjectValue();

	public void setObjectValue(T object);
}
