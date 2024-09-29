package com.info7255.demo.validator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SSLUtil {
    public static void disableCertificateValidation() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{new CustomTrustManager()};

        // Create a SSLContext with the custom trust manager
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        // Set the default SSLContext for the current JVM
        SSLContext.setDefault(sslContext);
    }
}

