package com.raizlabs.webservicemanager;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * Enum which represents each of the Http Methods.
 * Contains some helpers for getting the method
 * name and vice versa.
 * 
 * @author Dylan James
 *
 */
public enum HttpMethod {
	Get
	{
		@Override
		public HttpRequestBase createRequest() {
			return new HttpGet();
		}

		@Override
		public String getMethodName() {
			return "GET";
		}
	},
	Post
	{
		@Override
		public HttpRequestBase createRequest() {
			return new HttpPost();
		}

		@Override
		public String getMethodName() {
			return "POST";
		}
	},
	Put {
		@Override
		public HttpRequestBase createRequest() {
			return new HttpPut();
		}

		@Override
		public String getMethodName() {
			return "PUT";
		}
	},
	Delete {
		@Override
		public HttpRequestBase createRequest() {
			return new HttpDelete();
		}

		@Override
		public String getMethodName() {
			return "DELETE";
		}
	},
	Head {
		@Override
		public HttpRequestBase createRequest() {
			return new HttpHead();
		}

		@Override
		public String getMethodName() {
			return "HEAD";
		}
	};

	public abstract HttpRequestBase createRequest();
	public abstract String getMethodName();
	
	public static HttpMethod fromName(String name) {
		for (HttpMethod method : values()) {
			if (method.getMethodName().equalsIgnoreCase(name)) {
				return method;
			}
		}
		
		return null;
	}
}
