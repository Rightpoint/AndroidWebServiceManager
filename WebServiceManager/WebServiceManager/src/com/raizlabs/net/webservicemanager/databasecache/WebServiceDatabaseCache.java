package com.raizlabs.net.webservicemanager.databasecache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.raizlabs.net.requests.HttpUriRequestable;
import com.raizlabs.net.webservicemanager.ResultInfo;
import com.raizlabs.net.webservicemanager.caching.IWebServiceCache;
import com.raizlabs.net.webservicemanager.caching.WebServiceCacheHasher;

public class WebServiceDatabaseCache<RequestType extends HttpUriRequestable, HashType, CacheType, HashDBType> implements IWebServiceCache<RequestType, CacheType>{

	private SQLiteDatabase database;
	private WebServiceCacheHasher<RequestType, HashType, CacheType> hasher;
	private HashDatabaseTranslator<HashType, HashDBType> hashDBTranslator;
	private Databaser<CacheType> databaser;
	
	public WebServiceDatabaseCache(WebServiceCacheHasher<RequestType, HashType, CacheType> hasher,
			HashDatabaseTranslator<HashType, HashDBType> hashDBTranslator,
			Databaser<CacheType> databaser) {
		this.hasher = hasher;
		this.hashDBTranslator = hashDBTranslator;
		this.databaser = databaser;
	}
	
	@Override
	public ResultInfo<CacheType> getFromCache(RequestType request) {
		return getFromCacheHash(hasher.getHash(request));
	}
	
	public ResultInfo<CacheType> getFromCacheHash(HashType hash) {
		ResultInfo<CacheType> result = checkDatabase(hash);
		return result;
	}
	
	@Override
	public void putInCache(RequestType request,
			ResultInfo<CacheType> obj) {
		ContentValues values = databaser.getContentValuesForObject(obj.getResult());
		// TODO Add Hash values
		database.insert(databaser.getTableName(), null, values);
	}
	
	private ResultInfo<CacheType> checkDatabase(HashType hash) {
		HashDBType dbValue = hashDBTranslator.toDatabaseValue(hash);
		String[] columns = ColumnNamesFromContentValuesMap(databaser.getContentValuesMap());
		Cursor cursor = database.query(databaser.getTableName(), columns, "Hash=" + hash, null, null, null, null);
		ResultInfo<CacheType> result = null;
		if (cursor.moveToFirst()) {
			CacheType cacheData = databaser.getObjectFromContentValues(getContentValuesFromCursor(cursor));
			result = loadRequestInfo(cursor);
		}
		cursor.close();
		
		return result;
	}
	
	private String[] ColumnNamesFromContentValuesMap(ContentValues values) {
		Set<Entry<String, Object>> valueSet = values.valueSet();
		List<String> names = new ArrayList<String>(valueSet.size());
		for(Entry<String, Object> value : valueSet) {
			names.add(value.getKey());
		}
		
		return names.toArray(new String[names.size()]);
	}

	private ContentValues getContentValuesFromCursor(Cursor cursor) {
		ContentValues values = new ContentValues();
		// TODO Populate via the columns
		return values;
	}
	
	private ResultInfo<CacheType> loadRequestInfo(Cursor cursor) {
		// TODO Get the request info from the cursor
		return null;
	}
}
