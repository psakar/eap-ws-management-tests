package org.jboss.qa.management.common;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


/**
 * @author rhatlapa (rhatlapa@redhat.com)
 */
public class WebUtils {

    private static Logger log = Logger.getLogger(WebUtils.class.getName());

    public static enum HttpRequestMethod {GET, HEAD, OPTIONS, DELETE, TRACE, POST, PUT, PATCH}

    /**
     * Checks whether the URL is accessible by checking its response code, checks periodically for specified timeout
     *
     * @param url     web address to check
     * @param timeout maximum time in ms to check whether the URL will become accessible
     * @return true if specified url becomes accessible in specified time, false otherwise
     * @throws Exception
     */
    public static boolean verifyURL(URL url, long timeout) throws Exception {
        boolean available = false;
        long time = 0;
        while (!available && time < timeout) {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            available = (huc.getResponseCode() == HttpURLConnection.HTTP_OK);
            time += 1000;
            Thread.sleep(1000);
        }
        return available;
    }

    /**
     * returns response status code for specified URL
     *
     * @param url represents web address to check
     * @return response code for specified URL
     * @throws Exception
     */
    public static int getResponseCode(URL url) throws Exception {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        return huc.getResponseCode();
    }

    /**
     * Counts base url (url without any contexts)
     * @param url URL
     * @return as String the URL address without any context in format protocol://host:port/
     * @throws MalformedURLException
     */
    public static String getBaseURL(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), "/").toString();
    }

    /**
     * Returns the value of the content-encoding header field.
     * @param url refereneces URL
     * @return the content encoding of the resource that the URL references, or null if not known.
     * @throws IOException when an error occurs when opening connection to URL
     */
    public static String getContentEncoding(URL url) throws IOException {
        String encoding = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            encoding = conn.getContentEncoding();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return encoding;
    }

    /**
     * Retrieves http response based on request method used
     * @param url URL
     * @param requestMethod Request method to use (CONNECT HTTP request method is not supported)
     * @return HttpResponse when asked url with a specified request method
     * @throws IOException
     */
    public static HttpResponse getResponse(URL url, HttpRequestMethod requestMethod) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpRequestBase httpRequest;
        switch (requestMethod) {
            case HEAD:
                httpRequest = new HttpHead(url.toString());
                break;
            case OPTIONS:
                httpRequest = new HttpOptions(url.toString());
                break;
            case DELETE:
                httpRequest = new HttpDelete(url.toString());
                break;
            case TRACE:
                httpRequest = new HttpTrace(url.toString());
                break;
            case POST:
                httpRequest = new HttpPost(url.toString());
                break;
            case PUT:
                httpRequest = new HttpPut(url.toString());
                break;
            case PATCH:
                httpRequest = new HttpPatch(url.toString());
                break;
            case GET:
                httpRequest = new HttpGet(url.toString());
                break;
            default:
                throw new RuntimeException("Unknown request method: " + requestMethod);
        }
        return httpClient.execute(httpRequest);
    }

    /**
     * Retrieves all headers with specified header name returned by Head request on the specified URL
     * @param url URL
     * @param headerName name of header for which headers should be returned
     * @return Array of Headers corresponding to the specified header name retrieved when connecting to the URL using HEAD method
     * @throws Exception when error occurs while connecting to the URL
     */
    public static Header[] getResponseHeaders(URL url, String headerName) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        HttpHead httpHead = new HttpHead(url.toString());
        HttpResponse response = httpClient.execute(httpHead);
        return response.getHeaders(headerName);
    }


    /**
     * Retrieves all headers returned by Head request on the specified URL
     * @param url URL
     * @return Array of Headers retrieved when connecting to specified URL using HEAD method
     * @throws Exception when error occurs while connecting to the URL
     */
    public static Header[] getAllResponseHeaders(URL url) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        HttpHead httpHead = new HttpHead(url.toString());
        HttpResponse response = httpClient.execute(httpHead);
        return response.getAllHeaders();
    }

    /**
     * Retrieves content from the URL nevertheless the HTTP status code
     * @param url URL
     * @return content of the URL nevertheless the status code
     * @throws IOException when error occurs or if the URL is not accessible
     */
    public static String getResponseContent(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String encoding = conn.getContentEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
        }
        int responseCode = conn.getResponseCode();
        InputStream stream;
        if (responseCode != HttpURLConnection.HTTP_OK) {
            stream = conn.getErrorStream();
        } else {
            stream = conn.getInputStream();
        }
        try {
            return IOUtils.toString(stream, encoding);
        }
        finally {
            stream.close();
        }
    }


    /**
     * Retrieves content of the page at the URL
     * @param url URL
     * @return content of the page at the specified URL
     * @throws Exception when error occurs while connecting to the URL
     */
    public static String getContentFromUrl(URL url) throws Exception {
        HttpGet httpget = new HttpGet(url.toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();
        log.info("Executing request " + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);
        int statusCode = response.getStatusLine().getStatusCode();
        log.info("Response is with status code " + statusCode);
        Header[] errorHeaders = response.getHeaders("X-Exception");
        log.warning("Error headers are: " + Arrays.toString(errorHeaders));
        String content = EntityUtils.toString(response.getEntity());
        log.info("Content is: " + content);
        return content;
    }

   /**
    * sets HttpsURLConnection default HostNameVerifier to one which allows to connect to SSL host with hostname not corresponding to SSL certificate
    */
   public static void installHostNameVerifierNotValidatingSSLHostname()
   {
      HostnameVerifier hv = new HostnameVerifier()
      {
         @Override
         public boolean verify(String urlHostName, SSLSession session)
         {
            return true;
         }
      };
      HttpsURLConnection.setDefaultHostnameVerifier(hv);
   }

   /**
    * @return X509TrustManager which is not validating SSL host certificate
    */
   public static TrustManager createTrustManagerNotValidatingCertificateChains()
   {
      return new X509TrustManager()
      {
         @Override
         public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
               throws CertificateException
         {

         }

         @Override
         public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
               throws CertificateException
         {
         }

         @Override
         public X509Certificate[] getAcceptedIssuers()
         {
            return new X509Certificate[]
            {};
         }
      };
   }

   /**
    * sets HttpsURLConnection default SSLSocketFactory to one which allows to connect to SSL host with invalid certificate
    * @throws NoSuchAlgorithmException
    * @throws KeyManagementException
    */
   public static void installTrustManagerNotValidatingCertificateChains() throws NoSuchAlgorithmException,
         KeyManagementException
   {
      // see http://code.google.com/p/misc-utils/wiki/JavaHttpsUrl
      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, new TrustManager[] {createTrustManagerNotValidatingCertificateChains()}, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
   }

}
