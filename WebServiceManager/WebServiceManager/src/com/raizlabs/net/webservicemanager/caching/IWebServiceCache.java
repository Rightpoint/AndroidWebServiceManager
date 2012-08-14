package com.raizlabs.net.webservicemanager.caching;

import com.raizlabs.net.webservicemanager.ResultInfo;

public interface IWebServiceCache<RequestType, CacheType> {
	ResultInfo<CacheType> getFromCache(RequestType request);
	void putInCache(RequestType request, ResultInfo<CacheType> obj);
}
