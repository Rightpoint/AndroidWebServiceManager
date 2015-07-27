package com.raizlabs.net.responses;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import com.raizlabs.coreutils.listeners.ProgressListener;
import com.raizlabs.net.HttpMethod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for a web response which abstracts the underlying implementation.
 * 
 * @author Dylan James
 *
 */
public interface Response {
	/**
	 * Returns true if the response contains a header with he given name.
	 * @param name The name of the header to look for.
	 * @return True if the header exists
	 */
	public boolean containsHeader(String name);
	/**
	 * Gets the value for the header with the given name, or null if no
	 * header exists with that name.
	 * @param name The name of the header to look up.
	 * @return The value of the header or null if none exists.
	 */
	public String getHeaderValue(String name);
	
	/**
	 * Gets the response code for this {@link Response}
	 * @return
	 */
	public int getResponseCode();
	/**
	 * Gets the response message for this {@link Response}.
	 * @return
	 */
	public String getResponseMessage();
	
	/**
	 * Gets the content encoding for this {@link Response}.
	 * @return The content encoding, or null if it wasn't defined.
	 */
	public String getContentEncoding();
	/**
	 * Gets the content length for this {@link Response}.
	 * @return The content length, or -1 if it was not defined.
	 */
	public long getContentLength();
	/**
	 * Gets the content type for this {@link Response}
	 * @return The content type, or null if it was not defined.
	 */
	public String getContentType();
	/**
	 * Gets the {@link InputStream} to the content of this
	 * {@link Response} or null if one does not exist.
	 * @return The {@link InputStream} to the content or null
	 * if it does not exist.
	 * @throws IOException
	 */
	public InputStream getContentStream() throws IOException;
	
	/**
	 * @return The {@link HttpMethod} that was used to get this
	 * {@link Response}.
	 */
	public HttpMethod getRequestMethod();
	
	/**
	 * Gets the content of this {@link Response} by parsing it
	 * into a string.
	 * @return The content, or null if there was none.
	 */
	public String getContentAsString();
	
	/**
	 * Gets the content of this {@link Response} by decoding
	 * the stream as a {@link Bitmap}.
	 * @see BitmapFactory#decodeStream(InputStream, Rect, BitmapFactory.Options)
	 * @param outPadding
	 * @param options
	 * @return The decoded {@link Bitmap}.
	 */
	public Bitmap getContentAsBitmap(Rect outPadding, BitmapFactory.Options options);
	
	/**
	 * Gets the content of this {@link Response} by parsing the
	 * stream as an {@link JSONArray}.
	 * @return The {@link JSONArray} parsed from the stream, or null if it
	 * couldn't be parsed.
	 */
	public JSONArray getContentAsJSONArray();
	
	/**
	 * Gets the content of this {@link Response} by parsing the
	 * stream as an {@link JSONObject}.
	 * @return The {@link JSONObject} parsed from the stream, or null if it
	 * couldn't be parsed.
	 */
	public JSONObject getContentAsJSON();
	
	/**
	 * Reads the contents of this {@link Response} into the given
	 * file, updating the given {@link ProgressListener} as data
	 * is read.
	 * @param file The {@link File} to read the content into. This
	 * file will be overwritten if it exists.
	 * @param progressListener The {@link ProgressListener} which will
	 * be called as data is read.
	 * @return True if all the data was read successfully, or false if
	 * there was an error, the content length wasn't defined, or the 
	 * content length didn't match. The {@link File} is not deleted if
	 * the lengths didn't match or weren't defined.
	 */
	public boolean readContentToFile(File file, ProgressListener progressListener);
	
	/**
	 * Closes any connections or resources connected to this {@link Response}.
	 */
	public void close();
}
