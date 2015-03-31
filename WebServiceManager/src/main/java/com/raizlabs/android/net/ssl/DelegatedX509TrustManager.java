package com.raizlabs.android.net.ssl;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * {@link TrustManager} which initially checks a default manager and
 * hostname verifier and calls a delegate if they don't trust the source.
 * @author Dylan James
 */
public class DelegatedX509TrustManager implements TrustManager {	
	private X509TrustManager defaultManager;
	/**
	 * @return The {@link X509TrustManager} which is checked before the delegate
	 * is invoked.
	 */
	public X509TrustManager getTrustManager() { return defaultManager; }
	/**
	 * Sets the manager to check certificates against before invoking the
	 * delegate.
	 * @param trustManager The manager to check. Null to always call the delegate.
	 */
	public void setTrustManager(X509TrustManager trustManager) {
		this.defaultManager = trustManager;
	}
	
	private HostnameVerifier defaultVerifier;
	/**
	 * @return THe {@link HostnameVerifier} which is checked before the delegate
	 * is invoked.
	 */
	public HostnameVerifier getHostnameVerifier() { return defaultVerifier; }
	/**
	 * Sets the verifier to check hostnames against before invoking the
	 * delegate.
	 * @param verifier The verifier to check. Null to always call the delegate.
	 */
	public void setHostnameVerifier(HostnameVerifier verifier) {
		this.defaultVerifier = verifier;
	}
	
	
	private TrustDelegate certDelegate;
	/**
	 * @return The current {@link TrustDelegate} which is being used
	 * to determine whether certificates or hosts should be trusted.
	 */
	public TrustDelegate getCertificateCheckDelegate() { return certDelegate; }
	/**
	 * Sets the {@link TrustDelegate} to use to determine whether
	 * certificates should be trusted when the default manager doesn't trust
	 * the certificate or host.
	 * @param delegate The delegate to use.
	 */
	public void setCertificateCheckDelegate(TrustDelegate delegate) {
		this.certDelegate = delegate;
	}

	/**
	 * Constructs a new {@link DelegatedX509TrustManager} which first checks
	 * the default trust manager and then the given delegate.
	 * @param keystore The keystore to use to construct the default manager
	 * @param delegate The {@link TrustDelegate} to query when the
	 * default trust manager rejects a certificate.
	 */
	public DelegatedX509TrustManager(KeyStore keystore, TrustDelegate delegate) {
		this(new DefaultX509TrustManager(keystore), delegate);
	}
	
	/**
	 * Constructs a new {@link DelegatedX509TrustManager} which first checks
	 * the given trust manager and then the given delegate.
	 * @param trustManager The manager to check certificates against first,
	 * before checking the delegate. Passing null will always check the delegate. 
	 * @param delegate The {@link TrustDelegate} to query when the
	 * trust manager rejects a certificate.
	 */
	public DelegatedX509TrustManager(X509TrustManager trustManager, 
			TrustDelegate delegate) {
		super();
		setTrustManager(trustManager);
		setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
		setCertificateCheckDelegate(delegate);
	}
	

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		checkCertificateTrusted(chain, authType, false);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		checkCertificateTrusted(chain, authType, true);
	}

	protected void checkCertificateTrusted(X509Certificate[] chain, String authType, boolean isServer)
			throws CertificateException {
		// If we have a default manager, try it first
		if (defaultManager != null) {
			try {
				if (isServer)
					defaultManager.checkServerTrusted(chain, authType);
				else
					defaultManager.checkClientTrusted(chain, authType);
			} catch (CertificateException e) {
				// The default manager rejected the certificate!
				// Try the delegate
				if (!checkDelegateTrustsCertificate(chain, authType, isServer)) {
					// Delegate didn't trust it either! Throw the exception!
					throw e;
				}
			}
		} else {
			// Just check the delegate
			if (!checkDelegateTrustsCertificate(chain, authType, isServer)) {
				// Delegate didn't trust the certificate!
				throw new CertificateException("Delegate didn't trust the certificate and no default manager found!");
			}
		}
	}

	/**
	 * Checks if our delegate trusts the certificate
	 * @param chain
	 * @param authType
	 * @param isServer
	 * @return True if the delegate trusted the certficate, false if it didn't
	 * or we have no delegate
	 */
	private boolean checkDelegateTrustsCertificate(X509Certificate[] chain, String authType, boolean isServer) {
		if (certDelegate != null)
			return certDelegate.checkCertificateTrusted(chain, authType, isServer);
		else
			return false;
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		if (defaultManager != null) {
			return defaultManager.getAcceptedIssuers();
		}
		return null;
	}
	@Override
	public boolean verify(String hostname, SSLSession session) {
		if (certDelegate != null) {
			return certDelegate.checkHostnameTrusted(hostname, session);
		}
		
		if (defaultVerifier != null && defaultVerifier.verify(hostname, session)) {
				return true;
		}
		
		return false;
	}

}
