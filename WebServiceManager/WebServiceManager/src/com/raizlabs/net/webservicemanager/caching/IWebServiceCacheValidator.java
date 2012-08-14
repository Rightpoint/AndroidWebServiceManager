package com.raizlabs.net.webservicemanager.caching;

import com.raizlabs.net.webservicemanager.ResultInfo;

public interface IWebServiceCacheValidator<CacheType> {
	boolean isValid(ResultInfo<CacheType> cachedValue);
}
