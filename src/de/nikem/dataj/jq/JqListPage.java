package de.nikem.dataj.jq;

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
public class JqListPage<T> implements Serializable {
	private static final long serialVersionUID = -7889719245186844185L;

	private String echo;
	private int totalRecords;
	private int totalDisplayRecords;
	private List<T> aaData = new ArrayList<T>();
	public String getEcho() {
		return echo;
	}
	public void setEcho(String echo) {
		this.echo = echo;
	}
	public int getTotalRecords() {
		return totalRecords;
	}
	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}
	public int getTotalDisplayRecords() {
		return totalDisplayRecords;
	}
	public void setTotalDisplayRecords(int totalDisplayRecords) {
		this.totalDisplayRecords = totalDisplayRecords;
	}
	public List<T> getAaData() {
		return aaData;
	}
	public void setAaData(List<T> aaData) {
		this.aaData = aaData;
	}
}