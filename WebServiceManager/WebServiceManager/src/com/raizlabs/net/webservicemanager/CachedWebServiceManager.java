//package com.raizlabs.net.webservicemanager;
//
//import com.raizlabs.net.HttpUriRequestable;
//import com.raizlabs.net.webservicemanager.caching.IWebServiceCache;
//import com.raizlabs.net.webservicemanager.caching.WebServiceMemoryCache;
//
//public class CachedWebServiceManager<RequestType extends HttpUriRequestable, ReturnType> extends WebServiceManager {	
//	
//	private IWebServiceCache<RequestType, ReturnType> cache;
//	public IWebServiceCache<RequestType, ReturnType> getCache() {
//		if (cache == null)
//			cache = createDefaultCache();
//		
//		return cache;
//	}
//	public void setCache(IWebServiceCache<RequestType, ReturnType> cache) {
//		this.cache = cache;
//	}
//	
//	protected IWebServiceCache<RequestType, ReturnType> createDefaultCache() {
//		return new WebServiceMemoryCache<RequestType, ReturnType>();
//	}
//	
//	public CachedWebServiceManager(WebServiceMemoryCache<RequestType, ReturnType> cache) {
//		this.cache = cache;
//	}
//	
//	public <RequestType, ResultType> RequestInfo<ResultType> doRequest(RequestType request, WebServiceTranslator<RequestType,ResultType> translator) {
//		
//	}
//	
//	@Override
//	public RequestInfo<ReturnType> doRequest(RequestType request) {
//		return doRequest(request, true, true);
//	}
//	
//	public RequestInfo<ReturnType> doRequest(RequestType request, boolean checkCache, boolean updateCache) {
//		RequestInfo<ReturnType> result = null;
//		
//		if (checkCache)
//			result = getCache().getFromCache(request);
//		if (result == null) {
//			result = super.doRequest(request);
//			if (updateCache) {
//				getCache().putInCache(request, result);
//			}
//		}
//		
//		return result;
//	}
//}
