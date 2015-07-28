package alien4cloud.paas.cloudify2.rest.external;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.exceptions.RestClientException;

public class HttpClientFactory {

    private static final String HTTPS = "https";

    public static DefaultHttpClient createHttpClient(final URL url) throws RestClientException {
        DefaultHttpClient httpClient;
        try {
            if (HTTPS.equals(url.getProtocol())) {
                httpClient = createSSLHttpClient(url);
            } else {
                httpClient = createSimpleHttpClient(null);
            }
            final HttpParams httpParams = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
        } catch (Exception e) {
            throw new RestClientException("failed_creating_client", "Failed creating http client", ExceptionUtils.getFullStackTrace(e));
        }
        return httpClient;
    }

    private static DefaultHttpClient createSimpleHttpClient(HttpParams httpParams) {
        PoolingClientConnectionManager connmgr = new PoolingClientConnectionManager(SchemeRegistryFactory.createSystemDefault());
        connmgr.setDefaultMaxPerRoute(10);
        connmgr.setMaxTotal(20);
        return new DefaultHttpClient(connmgr, httpParams);
    }

    /**
     * Returns a HTTP client configured to use SSL.
     *
     * @param url
     *
     * @return HTTP client configured to use SSL
     * @throws org.cloudifysource.restclient.exceptions.RestClientException
     *             Reporting different failures while creating the HTTP client
     */
    private static DefaultHttpClient createSSLHttpClient(final URL url) throws NoSuchAlgorithmException, KeyManagementException {
        final X509TrustManager trustManager = createTrustManager();
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] { trustManager }, null);
        final SSLSocketFactory ssf = new SSLSocketFactory(ctx, createHostnameVerifier());
        SyncBasicHttpParams params = new SyncBasicHttpParams();
        DefaultHttpClient.setDefaultHttpParams(params);
        DefaultHttpClient httpClient = createSimpleHttpClient(params);
        httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme(HTTPS, url.getPort(), ssf));
        return httpClient;
    }

    private static X509TrustManager createTrustManager() {
        final X509TrustManager tm = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            }
        };
        return tm;
    }

    private static X509HostnameVerifier createHostnameVerifier() {
        final X509HostnameVerifier verifier = new X509HostnameVerifier() {

            @Override
            public boolean verify(final String arg0, final SSLSession arg1) {
                return true;
            }

            @Override
            public void verify(final String host, final String[] cns, final String[] subjectAlts) throws SSLException {
            }

            @Override
            public void verify(final String host, final X509Certificate cert) throws SSLException {
            }

            @Override
            public void verify(final String host, final SSLSocket ssl) throws IOException {
            }
        };
        return verifier;
    }
}
