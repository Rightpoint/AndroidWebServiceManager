package com.raizlabs.net.webservicemanager.caching;

import android.util.SparseArray;

import com.raizlabs.net.requests.HttpUriRequestable;
import com.raizlabs.net.webservicemanager.ResultInfo;

public class WebServiceMemoryCache<RequestType extends HttpUriRequestable, CacheType> implements IWebServiceCache<RequestType, CacheType>{
	
	private SparseArray<ResultInfo<CacheType>> cache;
	private IWebServiceCacheValidator<CacheType> validator;
	public void setValidator(IWebServiceCacheValidator<CacheType> validator) {
		this.validator = validator;
	}

	/**
	 * Constructs a default WebServiceMemoryCache which assumes any cached value is
	 * always valid.
	 */
	public WebServiceMemoryCache() {
		this(new IWebServiceCacheValidator<CacheType>() {
			@Override
			public boolean isValid(ResultInfo<CacheType> cachedValue) {
				return true;
			}
		});
	}
	
	public WebServiceMemoryCache(IWebServiceCacheValidator<CacheType> validator) {
		cache = new SparseArray<ResultInfo<CacheType>>();
		this.validator = validator;
	}
	
	@Override
	public ResultInfo<CacheType> getFromCache(RequestType request) {
		ResultInfo<CacheType> cachedValue = cache.get(getHashCode(request));
		if (validator.isValid(cachedValue)) {
			return cachedValue;
		}
		
		return null;
	}
	
	public void putInCache(RequestType request, ResultInfo<CacheType> obj) {
		cache.put(getHashCode(request), obj);
	}
	
	protected int getHashCode(RequestType request) {
		return request.hashCode();
	}
}
