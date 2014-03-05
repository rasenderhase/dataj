package de.nikem.dataj;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;


public abstract class ScrollQuery<T> {
	
	private class CountRunner implements Runnable {
		private int totalDisplayRecords = -1;
		private boolean ready = true;
		public CountRunner() {
		}

		@Override
		public void run() {
			ready = false;
			totalDisplayRecords = countTotalDisplayRecords();
			synchronized(lock){
				ready = true;
				lock.notifyAll();
			}
		}
		
		protected int countTotalDisplayRecords() {

			final long start = System.currentTimeMillis();
			String tmpString =  getQueryStringForCount();
			int count = 0;
			String tmpGroupBy = "";

			if (countGroupBy != null && !countGroupBy.isEmpty()) {
				tmpGroupBy = " group by " + countGroupBy;
			}
			tmpString = "select count(1) from (select 1 from (" +tmpString+") xy " + tmpGroupBy +") xy";

			Connection aCon = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				aCon = getDataSource().getConnection();
				stmt = prepareStatement(aCon, tmpString);
				rs = stmt.executeQuery();
				rs.next();
				count = rs.getInt(1);
				debug("                         count fertig:                          " + (System.currentTimeMillis() - start) + " ms");
				return count;
			} catch (SQLException e) {
				error("Count konnte nicht ermittelt werden", e);
			} finally {
				try { rs.close(); } catch (Exception e) { error("close", e); }
				try { stmt.close(); } catch (Exception e) { error("close", e); }
				try { aCon.close(); } catch (Exception e) { error("close", e); }
			}
			return count;
		}

		public int getTotalDisplayRecords() {
			return totalDisplayRecords;
		}

		public boolean isReady() {
			return ready;
		}

	}
	
	private static final boolean debugEnabled;
	private static final boolean errorEnabled;
	static {
		String debugLevel = System.getProperty("de.nikem.jdbc.ScrollQuery.DEBUG_LEVEL");
		errorEnabled = "ERROR".equals(debugLevel);
		debugEnabled = "DEBUG".equals(debugLevel);
	}
	
	private final boolean enableCount;
	private final Object lock = new Object();
	private final String countGroupBy;
	private int totalRecords = 0;
	
	private final String queryString;
	
	private final DataSource dataSource;
	
	private final int displayStart;
	private final int displayLength;
	private final Gruppenwechsel<T> gruppenwechsel;

	private int rowCnt = 0;
	private boolean moreResultsAvailable = false;
	
	private final List<T> data = new ArrayList<T>();
	
	/**
	 * Ausf?hren einer Named Query. Es werden alle Datens?tze verarbeitet bis {@link #processRow(Map)} <code>false</code> zur?ckgibt.
	 * @param dbManager
	 * @param queryString
	 */
	public ScrollQuery(DataSource dataSource, String queryString) {
		this(dataSource, queryString, 0, Integer.MAX_VALUE, null, false, null);
	}
	
	/**
	 *  Ausf?hren einer SQL Query. Es wird ein Ausschnitt aus dem Resultset verarbeitet, der mit <code>displayStart</code> und <code>displayLength</code> angegeben ist.
	 * @param dbManager
	 * @param queryString SQL-Query-String
	 * @param displayStart Start-Zeile, die verarbeitet werden soll (<code>0</code> = erste Zeile)
	 * @param displayLength Anzahl der zu verarbeitenden Zeilen
	 * @param gruppenwechsel optionales Gruppenwechsel-Objekt f?r die korrekte Z?hlung der Zeilen, falls mehrere Zeilen zu einer Ergebniszeile aggregiert werden sollen.
	 * @param enableCount Einschalten des Count-Threads, der ermittelt, wieviele Ergebnisse die Query liefert
	 * @param countGroupBy Spaltennamen, nach denen gruppiert wird, falls das Ergebnis Gruppen enth?lt (zusammen mit <code>gruppenwechsel</code>)
	 */
	public ScrollQuery(DataSource dataSource, String queryString, int displayStart, int displayLength, Gruppenwechsel<T> gruppenwechsel, boolean enableCount, String countGroupBy) {
		this.dataSource = dataSource;
		this.queryString = queryString;
		this.displayStart = displayStart;
		this.displayLength = displayLength;
		this.gruppenwechsel = gruppenwechsel;
		this.enableCount = enableCount;
		this.countGroupBy = countGroupBy;
	}
	
	protected String getLimitString(String sql) {
		if (getDisplayLength() == Integer.MAX_VALUE ) {		// - kein Limit
			return sql;
		}
		String limitString = System.getProperty("de.nikem.dataj.ScrollQuery.limitString", ":sql fetch first :total rows only");
		limitString = limitString.replaceAll(":total", Integer.toString(getDisplayStart() + getDisplayLength() + 1));
		limitString = limitString.replaceAll(":sql", sql);
		return limitString;
	}
	
	/**
	 * Query ausführen und Resultset verarbeiten.
	 * @return Ergebnis der Verarbeitung
	 * @throws DBException 
	 */
	public ListPage<T> execute() throws SQLException {
		final long start = System.currentTimeMillis();

		final CountRunner count = new CountRunner();
		
		if (enableCount) {
			final Thread countThread = new Thread(count);
			countThread.setDaemon(true);	//Deamon-Threads werden unterbrochen, wenn die VM beendet wird
			countThread.start();
		}
		
		final String tmpString;
		if (getGruppenwechsel() == null) {
			// fetch first... nur wenn kein Gruppenwechsel im ResultSet
			tmpString = getLimitString(getQueryString());
		} else {
			tmpString = getQueryString();
		}
		
		Connection aCon = null;
		try {
			aCon = getDataSource().getConnection();
			makeData(aCon, tmpString);
		} finally {
			try { aCon.close(); } catch (Exception e) { error("close", e);};
		}
		
		if (!enableCount) {
			if (isMoreResultsAvailable()) {
				//Wenn Count disabled, Anzahl der Datensätze schätzen
				// (immer 1 größer als die aktuelle Page, damit jQuery das Paging einblendet)
				totalRecords = getDisplayStart() + getDisplayLength() + 1;
			} else {
				//Ende des Resultsets erreicht...
				totalRecords = getDisplayStart() + getRowCnt();
			}
		} else {
			//Richtiges Paging mit Count-Query
			synchronized(lock) {
				while (!count.isReady()){
					try {
						lock.wait(10000);
					} catch (InterruptedException e) {
						error("Count Thread has been interrupted.", e);
					}
				}
			}
			totalRecords = count.getTotalDisplayRecords();
		}
		
		debug("gesamt fertig:                          " + (System.currentTimeMillis() - start) + " ms");

		return getResult();
	}
	
	protected void makeData(Connection aCon, final String tmpString) throws SQLException {
		final long start = System.currentTimeMillis();
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
				try {
					debug(tmpString);
					stmt = prepareStatement(aCon, tmpString);
					rs = stmt.executeQuery();
					ResultSetMetaData metaData = rs.getMetaData();
					boolean cont = true;

					processMetaData(metaData);
					
					while (rs.next()) {
						T row = mapRow(rs);
						
						increaseRowIdx(row);

						if (getRowCnt() > displayStart && (displayLength == Integer.MAX_VALUE || getRowCnt() <= displayStart + displayLength)) {
							cont = processRow(rs, row);
						}

						// ResultSet war noch nicht zu Ende: 
						// es gibt noch mehr Ergebnisse.
						if (displayLength == Integer.MAX_VALUE					// - Keine Page-Größenbeschränkung
								|| getRowCnt() > displayStart + displayLength	// - Page zu Ende
								|| !cont && rs.next()) {						// - Scrolling abgebrochen
							moreResultsAvailable = true;
							break;
						}
					}
				} finally {
					try { rs.close(); } catch (Exception e) { error("close", e); }
					try { stmt.close(); } catch (Exception e) { error("close", e); }
				}
		debug("  getData fertig:                        " + (System.currentTimeMillis() - start) + " ms");
	}
	
	/**
	 * Verarbeiten der Meta-Daten (z.B. um eine Header-Zeile einzuf?gen)
	 * @param metaData
	 * @throws SQLException 
	 */
	protected void processMetaData(ResultSetMetaData metaData) throws SQLException {
		//subclass Responsibility
	}
	
	/**
	 * Override this method to set your own query parameters (e.g. for filtering)
	 * @param aCon
	 * @param tmpString
	 * @return
	 * @throws SQLException 
	 */
	protected PreparedStatement prepareStatement(Connection aCon, String tmpString) throws SQLException {
		return aCon.prepareStatement(tmpString);
	}
	
	/**
	 * Ergbniszeile auf ein Ergebnisobjekt mappen.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected abstract T mapRow(ResultSet rs) throws SQLException;
	
	/**
	 * Verarbeiten einer Ergebnis-Zeile.
	 * @param rs ResultSet
	 * @param row
	 * @return <code>true</code>, wenn die ResultSet-Verarbeitung nach dieser Zeile fortgesetzt werden soll
	 * @throws SQLException 
	 */
	protected boolean processRow(ResultSet rs, T row) throws SQLException {
		data.add(row);
		return true;
	}
		
	protected ListPage<T> getResult() {
		ListPage<T> page = new ListPage<T>();
		page.setData(data);
		page.setTotalDisplayRecords(getTotalRecords());
		return page;
	}
	
	protected int getRowCnt() {
		return rowCnt;
	}

	protected void setRowCnt(int rowIdx) {
		this.rowCnt = rowIdx;
	}

	protected void increaseRowIdx(T row) throws SQLException {
		if (gruppenwechsel != null) {
			gruppenwechsel.processRow(row);
		}
		if (hasGruppenwechsel()) {
			setRowCnt(getRowCnt() + 1);
		}
	}
	
	protected boolean hasGruppenwechsel() {
		return gruppenwechsel == null || gruppenwechsel.hasGruppenwechsel();
	}

	protected String getQueryStringForCount() {
		return queryString;
	}
	
	protected String getQueryString() {
		return queryString;
	}

	public int getDisplayStart() {
		return displayStart;
	}

	public int getDisplayLength() {
		return displayLength;
	}

	public Gruppenwechsel<T> getGruppenwechsel() {
		return gruppenwechsel;
	}

	public boolean isMoreResultsAvailable() {
		return moreResultsAvailable;
	}

	public int getTotalRecords() {
		return totalRecords;
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
	protected static boolean isDebugEnabled() {
		return debugEnabled;
	}
	
	protected static boolean isErrorEnabled() {
		return errorEnabled;
	}
	
	/**
	 * @param e
	 */
	protected static void error(String msg, Throwable e) {
		if (errorEnabled) {
			System.err.println(msg);
			if (e != null) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * @param e
	 */
	protected static void debug(String msg) {
		debug(msg, null);
	}
	
	/**
	 * @param e
	 */
	protected static void debug(String msg, Throwable e) {
		if (debugEnabled) {
			System.out.println(msg);
			if (e != null) {
				e.printStackTrace();
			}
		}
	}
}