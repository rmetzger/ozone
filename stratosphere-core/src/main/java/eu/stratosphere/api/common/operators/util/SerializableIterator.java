package eu.stratosphere.api.common.operators.util;

import java.io.Serializable;
import java.util.Iterator;

import eu.stratosphere.api.common.operators.CollectionDataSource;

/**
 * Interface for a serializable iterator.
 * Used in @link {@link CollectionDataSource}
 * 
 */
public abstract class SerializableIterator<E> implements Iterator<E>, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public final void remove() {}
		
}
