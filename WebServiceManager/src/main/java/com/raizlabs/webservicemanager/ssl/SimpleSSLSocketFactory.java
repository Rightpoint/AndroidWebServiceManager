package com.raizlabs.webservicemanager.ssl;

import android.support.annotation.NonNull;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * Simple {@link SocketFactory} implementation which creates SSL sockets
 * according to a given {@link TrustManager}.
 * <br><br>
 * Based on EasySSLSocketFactory by olamy
 * @author Dylan James
 *
 */
public class SimpleSSLSocketFactory implements SocketFactory, LayeredSocketFactory {

	private static SSLContext createSSLContext(@NonNull TrustManager trustManager, @NonNull TLS tls) {
		try {
			SSLContext context = SSLContext.getInstance(tls.getVersion());
			context.init(null, new TrustManager[] { trustManager }, new SecureRandom());
			return context;
		} catch (NoSuchAlgorithmException e) {
		} catch (KeyManagementException e) { }
		return null;
	}
	
	private TrustManager trustManager;
	private TLS tls;
	/**
	 * @return The {@link TrustManager} used to verify SSL Sockets.
	 */
	public TrustManager getTrustManager() { return trustManager; }
	/**
	 * Sets the {@link TrustManager} to use to verify SSL Sockets.
	 * @param manager The trust manager to use.
	 */
	public void setTrustManager(TrustManager manager, @NonNull TLS tls) {
		this.trustManager = manager;
		this.tls = tls;
		this.sslContext = null;
	}
	
	private SSLContext sslContext;
	/**
	 * @return The SSL Context to use to create sockets
	 */
	public SSLContext getSSLContext() {
		if (sslContext == null) {
			sslContext = createSSLContext(trustManager, tls);
		}
		return sslContext;
	}
	
	/**
	 * Constructs an {@link SimpleSSLSocketFactory} that uses the given
	 * trust manager to verify SSL Sockets.
	 * @param trustManager The trust manager to use.
	 */
	public SimpleSSLSocketFactory(TrustManager trustManager, @NonNull TLS tls) {
		this.trustManager = trustManager;
		this.tls = tls;
	}
	
	@Override
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException {
		SSLSocket sslSock = (SSLSocket) getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
		if (!verifySocket(host, sslSock)) {
			throw new IOException("Server was not trusted!");
		}
		
		return sslSock;
	}

	@Override
	public Socket createSocket() throws IOException {
		return getSSLContext().getSocketFactory().createSocket();
	}

	@Override
	public Socket connectSocket(Socket sock, String host, int port,
			InetAddress localAddress, int localPort, HttpParams params)
			throws IOException, UnknownHostException, ConnectTimeoutException {
		int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
		int soTimeout = HttpConnectionParams.getSoTimeout(params);

		InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
		SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());
		
		if (!verifySocket(host, sslsock)) {
			throw new IOException("Server was not trusted!");
		}
		
		if ((localAddress != null) || (localPort > 0)) {
			// we need to bind explicitly
			if (localPort < 0) {
				localPort = 0; // indicates "any"
			}
			InetSocketAddress isa = new InetSocketAddress(localAddress,
					localPort);
			sslsock.bind(isa);
		}
		sslsock.connect(remoteAddress, connTimeout);
		sslsock.setSoTimeout(soTimeout);
		return sslsock;
	}

	private boolean verifySocket(String host, SSLSocket socket) {
		return trustManager == null || trustManager.verify(host, socket.getSession());
	}
	
	@Override
	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		return true;
	}

	// -------------------------------------------------------------------
	// javadoc in org.apache.http.conn.scheme.SocketFactory says :
	// Both Object.equals() and Object.hashCode() must be overridden
	// for the correct operation of some connection managers
	// -------------------------------------------------------------------

	public boolean equals(Object obj) {
		if (obj != null && obj instanceof SimpleSSLSocketFactory) {
			SimpleSSLSocketFactory other = (SimpleSSLSocketFactory) obj;
			return (trustManager != null && trustManager.equals(other.getTrustManager())); 
		}
		return false;
	}

	public int hashCode() {
		return getTrustManager().hashCode();
	}
}
