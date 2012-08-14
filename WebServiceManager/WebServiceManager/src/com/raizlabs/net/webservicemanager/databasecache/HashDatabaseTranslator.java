package com.raizlabs.net.webservicemanager.databasecache;

public interface HashDatabaseTranslator<HashType, HashDBType> {
	HashDBType toDatabaseValue(HashType hash);
	HashType fromDatabaseValue(HashDBType dbValue);
}
