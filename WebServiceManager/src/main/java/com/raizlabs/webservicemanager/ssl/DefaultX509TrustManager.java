package com.raizlabs.webservicemanager.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * {@link com.raizlabs.webservicemanager.ssl.TrustManager} which wraps the default trust
 * manager and verifier.
 * @author Dylan James
 */
public class DefaultX509TrustManager implements com.raizlabs.webservicemanager.ssl.TrustManager {
	X509TrustManager defaultManager;
	HostnameVerifier defaultVerifier;
	
	public DefaultX509TrustManager(KeyStore keystore) {
		super();
		try {
			TrustManagerFactory factory = 
					TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init(keystore);
			TrustManager[] trustManagers = factory.getTrustManagers();
			if (trustManagers != null) {
				// Find the first X509TrustManager
				for (TrustManager trustManager : trustManagers) {
					if (trustManager instanceof X509TrustManager) {
						defaultManager = (X509TrustManager) trustManager;
						break;
					}
				}
			}
		} catch (NoSuchAlgorithmException e) {
		} catch (KeyStoreException e) { }
		
		defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
	}
	
	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		checkCertifacteTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		checkCertifacteTrusted(chain, authType);
	}
	
	protected void checkCertifacteTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		// Fail if we don't have a default manager
		if (defaultManager == null) {
			final String msg = String.format(
					"No trust manager in %s. Can't trust any certificates.",
					getClass().getName());
			throw new CertificateException(msg);
		} else {
			// Otherwise, check the certificate chain with the default manager
			defaultManager.checkClientTrusted(chain, authType);
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		if (defaultManager == null) {
			return new X509Certificate[0];
		} else {
			return defaultManager.getAcceptedIssuers();
		}
	}

	@Override
	public boolean verify(String hostname, SSLSession session) {
		if (defaultVerifier != null) {
			return defaultVerifier.verify(hostname, session);
		}
		return false;
	}

}
