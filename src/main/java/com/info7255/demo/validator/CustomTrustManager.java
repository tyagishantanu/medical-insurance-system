package com.info7255.demo.validator;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class CustomTrustManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // Trust always
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // Trust always
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
