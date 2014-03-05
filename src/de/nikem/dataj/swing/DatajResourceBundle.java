package de.nikem.dataj.swing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple PropertyResourceBundle implementation that reads "datatable.net style"
 * locale files.
 * @author andreas
 *
 */
public class DatajResourceBundle extends PropertyResourceBundle {
	private static final Map<Locale, ResourceBundle> map = new HashMap<Locale, ResourceBundle>();
	//.*"(.*)"\s*\:\s*"(.*)".*
	private static final Pattern PATTERN = Pattern.compile(".*\"(.*)\"\\s*\\:\\s*\"(.*)\".*");

	public DatajResourceBundle(InputStream stream) throws IOException {
		super(stream);
	}
	
	public static ResourceBundle getBundle(Locale locale){
		ResourceBundle instance = map.get(locale);
		if (instance == null) {

			String localeString = locale.toString();

			InputStream is = null;
			BufferedReader reader = null;
			ByteArrayInputStream in = null;
			try {
				is = DatajResourceBundle.class.getResourceAsStream("/de/nikem/dataj/swing/" + localeString + ".txt");
				if (is == null) {
					localeString = locale.getLanguage();
					is = DatajResourceBundle.class.getResourceAsStream("/de/nikem/dataj/swing/" + localeString + ".txt");
				}
				if (is == null) {
					localeString = locale.getLanguage();
					is = DatajResourceBundle.class.getResourceAsStream("/de/nikem/dataj/swing/default.txt");
				}

				reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String line = null;
				Matcher matcher;
				Properties properties = new Properties();
				while ((line = reader.readLine()) != null) {
					matcher = PATTERN.matcher(line);
					if (matcher.matches()) {
						String key = matcher.group(1);
						String text = matcher.group(2);
						properties.put(key, text);
					}
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				properties.store(out, null);
				in = new ByteArrayInputStream(out.toByteArray());
				instance = new DatajResourceBundle(in);

				map.put(locale, instance);
			} catch (IOException ex) {
				throw new RuntimeException("Cannot read texts", ex);
			} finally {
				if (is != null) try { is.close(); } catch (IOException ex) { System.err.println(ex); };
				if (reader != null) try { reader.close(); } catch (IOException ex) { System.err.println(ex); };
				if (in != null) try { in.close(); } catch (IOException ex) { System.err.println(ex); };
			}
		}
		return instance;
	}
}
