package de.nikem.dataj;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author dda1ak
 *
 * @param <T> ResultSet Type
 */
public abstract class Gruppenwechsel<T> {

	/** 
	 * Konstante, mit der das Last-Objekt gekennzeichnet wird,
	 * wenn ein Gruppenwechsel erfolgen soll
	 */
	private final Object NULL = new Object();
	private Set<Object> lastSet = new HashSet<Object>();
	private Object actual = NULL;
	
	protected Set<Gruppenwechsel<T>> children = new HashSet<Gruppenwechsel<T>>();
	protected Gruppenwechsel<T> parent = null;
	
	public Gruppenwechsel(Gruppenwechsel<T> parent) {
		if (parent != null) {
			parent.children.add(this);
		}
		this.parent = parent;
	}

	/**
	 * Eine Zeile des ResultSets verarbeiten
	 * @param rs ResultSet
	 */
	public void processRow(T rs) throws SQLException {
		lastSet.add(actual);
		actual = getActual(rs);
		for (Gruppenwechsel<T> child : children) {
			child.processRow(rs);
		}
		
		//die abh??ngigen Children zur??cksetzen
		if (parent == null && hasGruppenwechsel()) {
			reset();
		}
	}
		
	/**
	 * Aktuellen Wert der aktuellen Row zur??ckgeben, mit dem festgestellt wird, ob ein Gruppenwechsel
	 * stattgefunden hat
	 * @param rs ResultSet
	 * @return Objekt der aktuellen Row.
	 */
	protected abstract Object getActual(T rs) throws SQLException;

	/**
	 * @return <code>true</code> wenn die aktuelle Zeile einen Gruppenwechsel darstellt.
	 */
	public boolean hasGruppenwechsel() {
		return !lastSet.contains(actual);
	}
	
	protected void reset() {
		lastSet.clear();
		for (Gruppenwechsel<T> child : children) {
			child.reset();
		}
	}
}
