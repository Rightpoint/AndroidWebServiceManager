package com.raizlabs.net.webservicemanager.caching;

import com.raizlabs.net.webservicemanager.ResultInfo;

public interface WebServiceCacheHasher<RequestType, HashType, CacheType> {
	HashType getHash(RequestType builder);
	boolean isValid(ResultInfo<CacheType> result);
}
