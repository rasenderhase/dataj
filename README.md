dataj
=====

Serverside pagination tool for JQuery datatables plug-in written in Java.

It interprets the control variables from datatables plug-in and cuts the desired page from the result set.

[Download it here](https://github.com/rasenderhase/dataj/releases).

Sample Usage
------------

Just pass a datasource, a select and the HTTP parameter map to JqPaginationQuery and create the output JSON. dataj creates the order by statements for you and cuts the desired piece from the result set. It also counts the total amount of available records.

See also [www.datatables.net with server side object data](http://www.datatables.net/release-datatables/examples/server_side/object_data.html)

It is important for dataj to have the JSON property have the same name as the table column:
`w.key("FIRST_NAME").value(rs.getString("FIRST_NAME"));`
It uses this information for automatic order.

Complete sample:

```java
public class Demo extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			DataSource dataSource = (DataSource) InitialContext.doLookup("java:/comp/env/JDBC");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("Content-Type", "application/json;charset=utf-8");
			final Writer writer = resp.getWriter();
			final JSONWriter w = new JSONWriter(writer);
			w.object();
			
			w.key("aaData");
			w.array();
			ListPage<Object> page = new JqPaginationQuery<Object>(dataSource, "select * from employees", req.getParameterMap()) {
				@Override
				protected Object mapRow(ResultSet rs) throws SQLException {
					w.object();
					w.key("FIRST_NAME").value(rs.getString("FIRST_NAME"));
					w.key("LAST_NAME").value(rs.getString("LAST_NAME"));
					w.key("BIRTHDAY").value(new SimpleDateFormat().format(rs.getDate("BIRTHDAY")));
					//... and so on
					w.endObject();
					return null; //return value not needed because content is directly put to JSON output
				}
			}.execute();
			w.endArray();
			w.key("iTotalRecords").value(page.getTotalRecords());
			w.key("iTotalDisplayRecords").value(page.getTotalDisplayRecords());
			w.key("sEcho").value(page.getEcho());
			w.endObject();
			writer.flush();
		} catch (SQLException ex) {
			throw new ServletException("DB access failed", ex);
		} catch (NamingException e) {
			throw new ServletException("Lookup failed", e);
		}
	}
}

```

This may result in an SQL statement like:
```SQL
select * from employees order by LAST_NAME asc fetch first 21 rows only
```

SQL count statement for total available records:
```SQL
select count(1) from (select 1 from (select * from employees) xy ) xy
```
The count query
* is executed in its own thread
* has no order by clause

for performance reasons.

For class JSONWriter refer to https://github.com/douglascrockford/JSON-java .
