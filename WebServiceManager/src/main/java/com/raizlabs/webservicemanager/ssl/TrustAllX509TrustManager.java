package com.raizlabs.webservicemanager.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;

import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * {@link com.raizlabs.webservicemanager.ssl.TrustManager} which accepts all certificates
 * and hosts.
 * @author Dylan James
 */
public class TrustAllX509TrustManager implements com.raizlabs.webservicemanager.ssl.TrustManager  {
	TrustManager[] trustManagers;
	
	public TrustAllX509TrustManager(KeyStore keystore) {
		super();
		try {
			TrustManagerFactory factory = 
					TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init(keystore);
			trustManagers = factory.getTrustManagers();
		} catch (NoSuchAlgorithmException e) {
		} catch (KeyStoreException e) { }
	}
	
	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException { 
		// Don't do anything - we would throw an exception to indicate
		// that the client is not trusted
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		// Don't do anything - we would throw an exception to indicate
		// that the server is not trusted
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// Return all the accepted issuers of all the trust managers we found.
		if (trustManagers == null || trustManagers.length == 0) {
			return new X509Certificate[0];
		} else {
			HashSet<X509Certificate> allCerts = new HashSet<X509Certificate>();
			// Loop through all our trust managers
			for (TrustManager manager : trustManagers) {
				// Get the accepted issuers if we can
				if (manager instanceof X509TrustManager) {
					X509Certificate[] managerCerts = ((X509TrustManager) manager).getAcceptedIssuers();
					// Add all the certificates we found
					if (managerCerts != null) {
						for (X509Certificate cert : managerCerts) {
							allCerts.add(cert);
						}
					}
				}
			}
			// Return all the certificates we found
			return allCerts.toArray(new X509Certificate[0]);
		}
	}

	@Override
	public boolean verify(String hostname, SSLSession session) {
		return true;
	}

}
