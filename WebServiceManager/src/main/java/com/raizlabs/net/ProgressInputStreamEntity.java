package com.raizlabs.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.InputStreamEntity;

import com.raizlabs.android.coreutils.listeners.ProgressListener;

/**
 * An {@link InputStreamEntity} which updates a given {@link ProgressListener}
 * of its progress.
 * 
 * @author Dylan James
 */
public class ProgressInputStreamEntity extends InputStreamEntity {

	private ProgressListener listener;
	private int updateInterval;
	private long length;
	
	/**
	 * Creates a {@link ProgressInputStreamEntity} with the given parameters.
	 * @param instream The {@link InputStream} to write.
	 * @param length The length of the {@link InputStream} in bytes.
	 * @param listener The {@link ProgressListener} to call when updates occur.
	 * @param updateInterval How frequently (in bytes) updates should be called.
	 */
	public ProgressInputStreamEntity(InputStream instream, long length, ProgressListener listener, int updateInterval) {
		super(instream, length);
		this.length = length;
		this.listener = listener;
		this.updateInterval = updateInterval;
	}
	
	@Override
	public void writeTo(final OutputStream outstream) throws IOException {
		super.writeTo(new OutputStream() {
			private long totalWritten = 0;
			private long lastUpdate = 0;
			
			@Override
			public void write(int oneByte) throws IOException {
				outstream.write(oneByte);
				if (listener != null && ++totalWritten - lastUpdate >= updateInterval) {
					listener.onProgressUpdate(totalWritten, length);
					lastUpdate = totalWritten;
				}
			}
		});
	}
	
}
