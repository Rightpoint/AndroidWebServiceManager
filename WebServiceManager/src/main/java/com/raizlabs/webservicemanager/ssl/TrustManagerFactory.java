package com.raizlabs.webservicemanager.ssl;

import java.security.KeyStore;

/**
 * Factory class which helps with the creation of {@link TrustManager}s.
 * @author Dylan James
 */
public class TrustManagerFactory {
	
	/**
	 * Gets a {@link TrustManager} which acts like the default trust
	 * of the system.
	 * @param keystore Optional: The {@link KeyStore} to create
	 * the trust manager with.
	 * @return The created {@link TrustManager}.
	 */
	public static TrustManager getDefaultTrustManager(KeyStore keystore) {
		if (keystore == null) keystore = getDefaultKeyStore();
		return new DefaultX509TrustManager(keystore);
	}

	/**
	 * Gets a {@link TrustManager} which accepts every certificate and host.
	 * @param keystore Optional: The {@link KeyStore} to create
	 * the trust manager with.
	 * @return The created {@link TrustManager}.
	 */
	public static TrustManager getTrustAllManager(KeyStore keystore) {
		if (keystore == null) keystore = getDefaultKeyStore();
		return new TrustAllX509TrustManager(keystore);
	}
	
	/**
	 * Gets a {@link TrustManager} which tries the default trust manager and
	 * queries the delegate if the default manager rejects it.
	 * @param delegate The {@link TrustDelegate} to call when the default
	 * trust manager rejects a certificate or host.
	 * @param keystore Optional: The {@link KeyStore} to create
	 * the trust manager with.
	 * @return The created {@link TrustManager}.
	 */
	public static TrustManager getDelegatedTrustManager(TrustDelegate delegate,
			KeyStore keystore) {
		if (keystore == null) keystore = getDefaultKeyStore();
		return new DelegatedX509TrustManager(keystore, delegate);
	}
	
	/**
	 * @return The default {@link KeyStore} that is used if nulls are supplied.
	 */
	public static KeyStore getDefaultKeyStore() {
		return null;
	}
}
