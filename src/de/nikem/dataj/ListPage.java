package de.nikem.dataj;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author andreas
 * @version $Revision: 1.1 $
 *
 * @param <T> Datentyp eines Elements der Liste
 */
public class ListPage<T> implements Serializable {
	private static final long serialVersionUID = -7889719245186844185L;

	private String echo;
	private int totalRecords;
	private int totalDisplayRecords;
	private List<T> data = new ArrayList<T>();
	public String getEcho() {
		return echo;
	}
	public void setEcho(String echo) {
		this.echo = echo;
	}
	/**
	 * @return Total number of available records (may be displayed in "... filtered from <i><code>totalRecords</code></i> records")
	 */
	public int getTotalRecords() {
		return totalRecords;
	}
	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}
	/**
	 * @return total number of records (paging ends at this limit)
	 */
	public int getTotalDisplayRecords() {
		return totalDisplayRecords;
	}
	public void setTotalDisplayRecords(int totalDisplayRecords) {
		this.totalDisplayRecords = totalDisplayRecords;
	}
	public List<T> getData() {
		return data;
	}
	public void setData(List<T> aaData) {
		this.data = aaData;
	}
}