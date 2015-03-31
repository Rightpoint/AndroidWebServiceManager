package com.raizlabs.net.responses;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.text.TextUtils;

import com.raizlabs.android.coreutils.io.IOUtils;
import com.raizlabs.android.coreutils.listeners.ProgressListener;
import com.raizlabs.android.coreutils.logging.Logger;

/**
 * Abstract class which does some of the generic work for a response.
 * @author Dylan James
 *
 */
public abstract class BaseResponse implements Response {

	@Override
	public String getContentAsString() {
		InputStream content = null;
		try {
			content = getContentStream();
			if (content != null) {
				return IOUtils.readStream(content);
			}
		} catch (IOException e) {
			Logger.w(getClass().getName(), "IOException in getContentAsString: " + e.getMessage());
		} finally {
			IOUtils.safeClose(content);
		}
		return null;
	}

	@Override
	public Bitmap getContentAsBitmap(Rect outPadding, Options options) {
		InputStream content = null;
		try {
			content = getContentStream();
			if (content != null) {
				return BitmapFactory.decodeStream(content, outPadding, options);
			}
		} catch (IOException e) {
			Logger.w(getClass().getName(), "IOException in getContentAsBitmap: " + e.getMessage());
		} finally {
			IOUtils.safeClose(content);
		}

		return null;
	}
	
	@Override
	public JSONArray getContentAsJSONArray() {
		String content = getContentAsString();
		if (!TextUtils.isEmpty(content)) {
			try {
				return new JSONArray(content);
			} catch (JSONException e) {
				Logger.w(getClass().getName(), "JSONException in getContentAsJSONArray: " + e.getMessage());
			}
		}
		
		return null;
	}
	
	@Override
	public JSONObject getContentAsJSON() {
		String content = getContentAsString();
		if (!TextUtils.isEmpty(content)) {
			try {
				return new JSONObject(content);
			} catch (JSONException e) {
				Logger.w(getClass().getName(), "JSONException in getContentAsJSON: " + e.getMessage());
			}
		}
		
		return null;
	}

	@Override
	public boolean readContentToFile(File file, ProgressListener progressListener) {
		InputStream input;
		try {
			// Get the input stream from the content
			input = getContentStream();
			// If there was no content stream, fail
			if (input == null) {
				return false;
			}
		} catch (IOException e) {
			Logger.w(getClass().getName(), "IOException in getContentToFile", e);
			return false;
		}
		
		// Delete the file if it exists
		if (file.exists()) {
			file.delete();
		}
		// Create the directory for the file
		file.getParentFile().mkdirs();
		
		FileOutputStream out = null;
		try {
			// Get an output stream to the file
			out = new FileOutputStream(file);
		} catch (IOException e) {
			Logger.w(getClass().getName(), "IOException in readContentToFile", e);
			IOUtils.safeClose(input);
			return false;
		}
		
		long expectedSize = getContentLength();

		try {
			byte[] buffer = new byte[1024];
			long totalRead = 0;
			int read;
			// Pump all the data
			while ((read = input.read(buffer)) != -1) {
				out.write(buffer, 0, read);
				out.flush();
				totalRead += read;
				// Update the progress listener if we have one
				if (progressListener != null) {
					progressListener.onProgressUpdate(totalRead, expectedSize);
				}
			}
			// If the expected size matches, we succeeded.
			// If the expected size was not defined, we return true as we don't know if it failed.
			return expectedSize == -1 || totalRead == expectedSize;
		} catch (IOException ex) {
			Logger.w(getClass().getName(), "IOException in readContentToFile", ex);
			return false;
		} finally {
			// Close both our streams
			IOUtils.safeClose(out);
			IOUtils.safeClose(input);
		}
	}
}
