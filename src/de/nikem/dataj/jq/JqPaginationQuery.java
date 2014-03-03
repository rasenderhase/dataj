package de.nikem.dataj.jq;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import de.nikem.dataj.Gruppenwechsel;
import de.nikem.dataj.ScrollQuery;

public abstract class JqPaginationQuery<T> extends ScrollQuery<JqListPage<T>, T> {

	private static final Pattern PATTERN_TRUE = Pattern.compile("on|true|1", Pattern.CASE_INSENSITIVE);
	protected static final String NO_GROUP_BY = "";
	
	private final String echo;
	private final String orderBy;
	private final String[] dataProps;


	private final List<T> data = new ArrayList<T>();
	
	public JqPaginationQuery(DataSource dataSource, String queryString, Map<String, String[]> requestParameterMap) {
		this(dataSource, queryString, requestParameterMap, null, null);
	}

	public JqPaginationQuery(DataSource dataSource, String queryString, Map<String, String[]> requestParameterMap, String countGroupBy, Gruppenwechsel<T> gruppenwechsel) {
		super(dataSource, 
				queryString, 
				Integer.parseInt(getParameter(requestParameterMap, "iDisplayStart")), 
				Integer.parseInt(getParameter(requestParameterMap, "iDisplayLength")), 
				gruppenwechsel,
				!getBoolean(requestParameterMap, "disableCount"),
				countGroupBy);
		
		// decode other JQuery request query parameters
		echo = getParameter(requestParameterMap, "sEcho");
		
		// Anzahl der dargestellten Spalten
		int columns = Integer.parseInt(getParameter(requestParameterMap, "iColumns"));
		dataProps = new String[columns];
		for (int i = 0; i < columns; i++) {
			dataProps[i] = getParameter(requestParameterMap, "mDataProp_" + i);
		}

		// Anzahl der Sortier-Spalten
		int sortingCols = Integer.parseInt(getParameter(requestParameterMap, "iSortingCols"));
		String[] sorts = new String[sortingCols];
		for (int i = 0; i < sortingCols; i++) {
			// Spaltenname
			String col = dataProps[Integer.parseInt(getParameter(requestParameterMap, "iSortCol_" + i))];
			// Sortierrichtung
			String dir = getParameter(requestParameterMap, "sSortDir_" + i);
			sorts[i] = col.toUpperCase() + " " + dir;
			if (i == 0) {
				sorts[i] = " order by " + sorts[i];
			}
		}

		StringBuilder orderByBuilder = new StringBuilder();
		for (String sort : sorts) {
			if (orderByBuilder.length() > 0) {
				orderByBuilder.append(',');
			}
			orderByBuilder.append(sort);
		}
		this.orderBy = orderByBuilder.toString();
	}
	
	@Override
	protected String getQueryString() {
		return super.getQueryString() + " " + orderBy;
	}
	
	@Override
	protected JqListPage<T> getResult() {
		JqListPage<T> page = new JqListPage<T>();
		page.setEcho(echo);
		page.setAaData(data);
		page.setTotalDisplayRecords(getTotalRecords());
		return page;
	}
	
	@Override
	protected boolean processRow(ResultSet rs, T row) {
		data.add(row);
		return true;
	}

	/**
	 * Holt einen einzelnen Parameter aus einer Request-Parameter-Map.
	 *
	 * @param parameterMap
	 * @param key
	 * @return
	 */
	protected static String getParameter(Map<String, String[]> parameterMap, String key) {
		String parameter = null;
		String[] parameters = parameterMap.get(key);
		if (parameters != null && parameters.length > 0) {
			parameter = parameters[0];
		}
		return parameter;
	}
	
	protected static boolean getBoolean(Map<String, String[]> parameterMap, String key) {
		final String booleanString = getParameter(parameterMap, key);
		final boolean b;
		if (booleanString != null) {
			b = PATTERN_TRUE.matcher(booleanString).matches();
		} else {
			b = false;
		}
		return b;
	}
}

