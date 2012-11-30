package com.raizlabs.net.json;

import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class for handling a JSON Array of pairs of Name/Value-arrays, of the format:
 * [
 *   {
 *     "name":"KEY_NAME1"
 *     "values": ["val1","val2","val3"]
 *   },
 *   {
 *     "name":"KEY_NAME2"
 *     "values": ["val1","val2","val3"]
 *   }
 * ]
 * 
 * Allows looking up the values JSONArray by their name.
 * 
 * @author Dylan James
 *
 */
public class JSONNameValuesMap {
	private Hashtable<String, JSONArray> valueTable;
	
	/**
	 * Creates a {@link JSONNameValuesMap} by parsing the given string
	 * as a {@link JSONArray}.
	 * @param string The string to parse.
	 * @throws JSONException if the given string cannot be parsed as a
	 * {@link JSONArray}
	 */
	public JSONNameValuesMap(String string) throws JSONException {
		this(new JSONArray(string));
	}
	
	/**
	 * Creates a {@link JSONNameValuesMap} from the given {@link JSONArray}.
	 * @param jsonArray The {@link JSONArray} to get the name/value pairs from.
	 */
	public JSONNameValuesMap(JSONArray jsonArray) {
		this();
		parseJSONArray(jsonArray);
	}
	
	private JSONNameValuesMap() {
		valueTable = new Hashtable<String, JSONArray>();
	}
	
	private void parseJSONArray(JSONArray array) {
		// Clear our values
		valueTable.clear();
		final int count = array.length();
		// Loop through all indices in the array
		for (int i = 0; i < count; ++i) {
			// Try adding the data from the current index
			// If we can't find any piece of data, ignore this item
			try {
				JSONObject currentObj = array.getJSONObject(i);
				String name = currentObj.getString("name");
				JSONArray values = currentObj.getJSONArray("values");
				valueTable.put(name, values);
			} catch (JSONException e) { }
		}
	}
	
	/**
	 * Gets the {@link JSONArray} for the given key, or throws an exception if
	 * the key does not exist.
	 * @param key The "name" to look up
	 * @return The {@link JSONArray} bound to the given key.
	 * @throws JSONException if the key does not exist.
	 */
	public JSONArray getValues(String key) throws JSONException {
		JSONArray values = valueTable.get(key);
		if (values == null) {
			throw new JSONException("Key not found in map: " + key);
		}
		return values;
	}
	
	/**
	 * Gets the {@link JSONArray} for the given key, or null if it does not
	 * exist.
	 * @param key The "name" to look up.
	 * @return The {@link JSONArray} bound to the given key, or null if it does
	 * not exist.
	 */
	public JSONArray optValues(String key) {
		return valueTable.get(key);
	}
	
	/**
	 * Gets the value for the given key at the given index.
	 * @param key The "name" to look up.
	 * @param index The index of the value to get from the values {@link JSONArray}.
	 * @return The string value at the given index of the given key.
	 * @throws JSONException If the key does not exist in the map or the value
	 * does not exist in the keys values.
	 */
	public String getStringValue(String key, int index) throws JSONException {
		return getValues(key).getString(index);
	}
	
	/**
	 * Gets the value for the given key at the given index.
	 * @param key The "name" to look up.
	 * @param index The index of the value to get from the values {@link JSONArray}.
	 * @return Null if the key does not exist or an empty string if there is no
	 * value at that index. If the value is not a string and is not null, then it
	 * is converted to a string.
	 */
	public String optStringValue(String key, int index) {
		JSONArray values = optValues(key);
		if (values != null) {
			return values.optString(index);
		}
		return null;
	}
	
	/**
	 * Convenience method which gets the string value at the first index of the
	 * given key.
	 * @param key The "name" to look up.
	 * @return The first string value in the array mapped to the given key.
	 * @throws JSONException If the key does not exist or the value does not exist
	 * in the keys values.
	 */
	public String getFirstString(String key) throws JSONException {
		return getStringValue(key, 0);
	}
	
	/**
	 * Convenience method which gets the string value at the first index of the
	 * given key, or null if it doesn't exist.
	 * @param key The "name" to look up.
	 * @return The first string value in the array mapped to the given key, or
	 * null if the key does not exist, maps to no values, or the first value
	 * is null.
	 */
	public String optFirstString(String key) {
		return optStringValue(key, 0);
	}
}
