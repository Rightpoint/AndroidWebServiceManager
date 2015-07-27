package com.raizlabs.net.ssl;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSession;

/**
 * Interface which is used as a delegate to check whether certificates
 * and hostnames should be trusted.
 * @author Dylan James
 */
public interface TrustDelegate {
	/**
	 * Called to determine whether the given certificate chain should be
	 * trusted. 
	 * @param chain the certificate chain to validate.
	 * @param authType the authentication type used.
	 * @param isServer true if the certificate is coming from a server,
	 * false if it is coming from a client.
	 * @return true to trust the certificate.
	 */
	public boolean checkCertificateTrusted(X509Certificate[] chain, String authType, boolean isServer);

	/**
	 * Verifies that the specified hostname is allowed within the specified SSL session.
	 * @param hostname the hostname.
	 * @param session the SSL session of the connection.
	 * @return true to trust the hostname for this session.
	 */
	public boolean checkHostnameTrusted(String hostname, SSLSession session);
}