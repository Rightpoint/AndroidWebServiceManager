package com.raizlabs.android.net.ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;

/**
 * Interface for a class which handles trusting certificates and hostnames.
 * @author Dylan James
 *
 */
public interface TrustManager extends X509TrustManager, HostnameVerifier{

}
