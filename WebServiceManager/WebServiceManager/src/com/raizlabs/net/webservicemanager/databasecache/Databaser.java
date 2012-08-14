package com.raizlabs.net.webservicemanager.databasecache;

import android.content.ContentValues;

public abstract class Databaser<T> {
	public abstract String getTableName();
	public abstract ContentValues getContentValuesMap();
	public abstract ContentValues getContentValuesForObject(T obj);
	public abstract T getObjectFromContentValues(ContentValues values);
}
