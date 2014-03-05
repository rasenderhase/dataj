/*
 * $Id: ServerTableRowSorter.java,v 1.2 2014/02/27 17:14:47 knees Exp $
 *
 * Copyright (C) Siemens AG 2014. All Rights Reserved.
 */
package de.nikem.dataj.swing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Sorts the clicked column on the server creating the sorting and
 * pagination parameters in "<a href="http://www.datatables.net/release-datatables/examples/server_side/object_data.html">datatables.net</a> style".
 * @author andreas
 */
public class ServerTableRowSorter extends TableRowSorter<TableModel> {

	private final String[] dbColumnNames;

	private int iDisplayStart = 0;
	private int iDisplayLength;

	public ServerTableRowSorter() {
		this(10, null, null);
	}

	public ServerTableRowSorter(int iDisplayLength, TableModel model) {
		this(iDisplayLength, model, null);
	}

	public ServerTableRowSorter(int iDisplayLength, TableModel model, String[] dbColumnNames) {
		super(model);
		this.dbColumnNames = dbColumnNames;
		this.iDisplayLength = iDisplayLength;
	}

	@Override
	public void sort() {
		//do nothing. Server Side Sorting
	}
	
	/**
	 * get actual Sorting and Paging
	 */
	public Map<String, String[]> getParameters() {
		Map<String, String[]> parameters = new HashMap<String, String[]>();

		parameters.put("iDisplayStart", new String[] { Integer.toString(iDisplayStart) });
		parameters.put("iDisplayLength", new String[] { Integer.toString(iDisplayLength) });
		parameters.put("sEcho", new String[] { Long.toString(System.currentTimeMillis()) });

		// Anzahl der dargestellten Spalten
		parameters.put("iColumns", new String[] { Integer.toString(getModel().getColumnCount()) });
		// Column-Names der dargestellten Spalten
		for (int i = 0; i < getModel().getColumnCount(); i++) {
			String colName;
			if (dbColumnNames != null) {
				colName = dbColumnNames[i];
			} else {
				colName = Integer.toString(i + 1);
			}
			parameters.put("mDataProp_" + i, new String[] { colName });
		}

		List<? extends SortKey> sortKeys = getSortKeys();
		// Anzahl der Sortier-Spalten
		parameters.put("iSortingCols", new String[] { Integer.toString(sortKeys.size()) });

		for (int i = 0; i < sortKeys.size(); i++) {
			SortKey key = sortKeys.get(i);
			int column = key.getColumn();
			parameters.put("iSortCol_" + i, new String[] { Integer.toString(column) });
			String sortOrder = "asc";
			switch (key.getSortOrder()) {
			case ASCENDING : sortOrder = "asc";
			break;
			case DESCENDING : sortOrder = "desc";
			default :
			}
			parameters.put("sSortDir_" + i, new String[] { sortOrder });
		}
		return parameters;
	}

	/**
	 * @return First record of the displayed page (0-based)
	 */
	public int getiDisplayStart() {
		return iDisplayStart;
	}

	/**
	 * @param iDisplayStart First record of the page to be displayed (0-based)
	 */
	public void setiDisplayStart(int iDisplayStart) {
		this.iDisplayStart = iDisplayStart;
		fireRowSorterChanged(null);
	}

	/**
	 * @return desired length of the list page (number of records displayed in last page may be smaller)
	 */
	public int getiDisplayLength() {
		return iDisplayLength;
	}

	/**
	 * set the page size. Also sets <code>iDisplayStart</code> to 0.
	 * @param iDisplayLength desired length of the list page (number of records displayed in last page may be smaller)
	 */
	public void setiDisplayLength(int iDisplayLength) {
		this.iDisplayLength = iDisplayLength;
		this.iDisplayStart = 0;
		fireRowSorterChanged(null);
	}
	
	public void next() {
		setiDisplayStart(getiDisplayStart() + getiDisplayLength());
	}
	
	public void previous() {
		int start = getiDisplayStart() - getiDisplayLength();
		setiDisplayStart(start < 0 ? 0 : start);
	}
}