package com.raizlabs.android.net.ssl;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;

import javax.net.ssl.SSLSession;

/**
 * Abstract base implementation of a {@link TrustDelegate} which will
 * only call the certificate check once per unique chain of certificates
 * and will only call the hostname check once per hostname.
 * @author Dylan James
 *
 */
public abstract class CachedTrustDelegate implements TrustDelegate {
	private static class CertificateChain {
		private X509Certificate[] chain;
		public CertificateChain(X509Certificate[] chain) {
			this.chain = chain;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof CertificateChain) {
				return Arrays.equals(chain, ((CertificateChain) o).chain);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			StringBuilder nameBuilder = new StringBuilder();
			for (X509Certificate cert : chain) {
				nameBuilder.append(":" + cert.toString());
			}
			return nameBuilder.toString().hashCode();
		}
	}
	
	private HashMap<CertificateChain, Boolean> cachedCertificates;
	private HashMap<String, Boolean> cachedHosts;
	
	public CachedTrustDelegate() {
		cachedCertificates = new HashMap<CertificateChain, Boolean>();
		cachedHosts = new HashMap<String, Boolean>();
	}
	
	@Override
	public boolean checkCertificateTrusted(X509Certificate[] chain,
			String authType, boolean isServer) {
		synchronized (cachedCertificates) {
			CertificateChain certChain = new CertificateChain(chain);
			Boolean result = cachedCertificates.get(certChain);
			if (result == null) {
				result = isCertificateTrusted(chain, authType, isServer);
				cachedCertificates.put(certChain, result);
			}
			return result;
		}
	}

	@Override
	public boolean checkHostnameTrusted(String hostname, SSLSession session) {
		synchronized (cachedHosts) {
			Boolean result = cachedHosts.get(hostname);
			if (result == null) {
				result = isHostNameTrusted(hostname, session);
				cachedHosts.put(hostname, result);
			}
			return result;
		}
	}

	/**
	 * Called to determine whether the given certificate chain should be
	 * trusted when it hasn't been queried before.
	 * @param chain the certificate chain to validate.
	 * @param authType the authentication type used.
	 * @param isServer true if the certificate is coming from a server,
	 * false if it is coming from a client.
	 * @return true to trust the certificate.
	 */
	public abstract boolean isCertificateTrusted(X509Certificate[] chain,
			String authType, boolean isServer);
	/**
	 * Verifies that the specified hostname is allowed within the specified SSL session.
	 * <br><br>
	 * NOTE: This will cache the result for the hostname and not the
	 * hostname/session pair.
	 * @param hostname the hostname.
	 * @param session the SSL session of the connection.
	 * @return true to trust the hostname for this session.
	 */
	public abstract boolean isHostNameTrusted(String hostname, SSLSession session);
}
