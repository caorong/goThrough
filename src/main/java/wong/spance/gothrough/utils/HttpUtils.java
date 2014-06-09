package wong.spance.gothrough.utils;

import org.apache.http.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wong.spance.gothrough.proxy.ProxyContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Enumeration;

/**
 * Created by spance on 14/6/4.
 */
public abstract class HttpUtils {

    public final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    protected final static Logger log = LoggerFactory.getLogger(HttpUtils.class);

    /**
     * These are the "hop-by-hop" headers that should not be copied.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
     * I use an HttpClient HeaderGroup class instead of Set<String> because this
     * approach does case insensitive lookup faster.
     */
    private static final HeaderGroup HOP_BY_HOP_HEADERS;

    static {
        HOP_BY_HOP_HEADERS = new HeaderGroup();
        String[] headers = new String[]{
                HttpHeaders.CONNECTION,
                HttpHeaders.LOCATION,
                HttpHeaders.CONTENT_LENGTH,
                HttpHeaders.TRANSFER_ENCODING,
                HttpHeaders.PROXY_AUTHENTICATE,
                HttpHeaders.PROXY_AUTHORIZATION,
                "TE", "Trailers", "Upgrade"};
        for (String header : headers) {
            HOP_BY_HOP_HEADERS.addHeader(new BasicHeader(header, null));
        }
    }

    public static String processDefaultPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.equals("/index.jsp"))
            path = "/";
        return path;
    }

    public static String getContentType(final HttpEntity entity) {
        ContentType type = ContentType.get(entity);
        return type != null ? type.toString() : "";
    }


    public static HttpRequest buildHttpRequest(ProxyContext proxyContext) throws IOException {
        String method = proxyContext.request.getMethod(), targetUrl = proxyContext.getTargetUriString();
        HttpRequest targetRequest;
        if (Util.hasHeaderAnyOf(proxyContext.request, HttpHeaders.CONTENT_LENGTH, HttpHeaders.TRANSFER_ENCODING)) {
            //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
            HttpEntityEnclosingRequest eProxyRequest = new BasicHttpEntityEnclosingRequest(method, targetUrl);
            InputStreamEntity input = new InputStreamEntity(proxyContext.request.getInputStream(),
                    proxyContext.request.getContentLength());
            input.setContentType(proxyContext.request.getContentType());
            eProxyRequest.setEntity(input);
            targetRequest = eProxyRequest;
        } else {
            targetRequest = new BasicHttpRequest(method, targetUrl);
        }
        return targetRequest;
    }

    /**
     * Copy proxied response headers back to the servlet client.
     */
    public static void respondHeaders(ProxyContext proxyContext) {
        for (Header header : proxyContext.targetResponse.getAllHeaders()) {
            if (!HOP_BY_HOP_HEADERS.containsHeader(header.getName())) {
                proxyContext.response.addHeader(header.getName(), header.getValue());
            }
        }
    }

    public static boolean sendRedirectOrNotModified(ProxyContext proxyContext) throws ServletException, IOException {
        int statusCode = proxyContext.targetResponse.getStatusLine().getStatusCode();
        // response is a redirect 300-303
        if (statusCode >= 300 && statusCode <= 303) {
            Header locationHeader = proxyContext.targetResponse.getLastHeader(HttpHeaders.LOCATION);
            if (locationHeader == null) {
                throw new ServletException("status code: " + statusCode + " but the Location header was not found");
            }
            String newLocation = rewriteUrlFromResponse(proxyContext, locationHeader.getValue());
            proxyContext.response.sendRedirect(newLocation); // 发送新的Location
            return true;
        }
        // 304 SNOT_MODIFIED
        if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
            proxyContext.response.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
            proxyContext.response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        }
        if (statusCode >= 400) {
            log.info("Respond-Error[{}] {}", statusCode, proxyContext.targetRequest.getRequestLine().getUri());
            proxyContext.response.sendError(statusCode, proxyContext.targetResponse.getStatusLine().getReasonPhrase());
            return true;
        }
        return false;
    }

    /**
     * Copy request headers from the servlet client to the proxy request.
     */
    public static void copyRequestHeaders(ProxyContext proxyContext, HttpRequest proxyRequest) {
        for (Enumeration<String> iter = proxyContext.request.getHeaderNames(); iter.hasMoreElements(); ) {
            String headerName = iter.nextElement();
            // 不希望gzip响应时，过滤掉 ACCEPT_ENCODING
            if (HOP_BY_HOP_HEADERS.containsHeader(headerName)) {
                continue;
            }
            URIBuilder target = proxyContext.getTargetUri();
            //sometimes more than one value
            for (Enumeration<String> headers = proxyContext.request.getHeaders(headerName); headers.hasMoreElements(); ) {
                String headerValue = headers.nextElement();
                if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                    headerValue = target.getHost();
                    if (target.getPort() != -1 && target.getPort() != 80)
                        headerValue += ":" + target.getPort();
                }
                proxyRequest.addHeader(headerName, headerValue);
            }
        }
    }

    /**
     * For a redirect response from the target server, this translates {@code theUrl} to redirect to
     * and translates it to one the original client can use.
     */
    public static String rewriteUrlFromResponse(ProxyContext proxyContext, String theUrl) {
        //TODO document example paths
        String targetPrefix = proxyContext.getTargetUriPrefix();
        if (theUrl.startsWith(targetPrefix)) {
            String curUrl = proxyContext.getRequestURL();//no query
            if (proxyContext.pathInfo != null) {
                assert curUrl.endsWith(proxyContext.pathInfo);
                curUrl = curUrl.substring(0, curUrl.length() - proxyContext.pathInfo.length());//take pathInfo off
            }
            theUrl = curUrl + theUrl.substring(targetPrefix.length());
        }
        return theUrl;
    }


    public static Charset getCharset(final HttpEntity entity) throws UnsupportedEncodingException {
        Charset charset = null;
        try {
            ContentType contentType = ContentType.get(entity);
            if (contentType != null) {
                charset = contentType.getCharset();
            }
        } catch (final UnsupportedCharsetException ex) {
            throw new UnsupportedEncodingException(ex.getMessage());
        }
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        if (charset == null) {
            charset = HTTP.DEF_CONTENT_CHARSET;
        }
        return charset;
    }

    public static String toString(final HttpEntity entity, final InputStream instream, final Charset charset)
            throws IOException, ParseException {
        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        if (instream == null) {
            return null;
        }
        try {
            if (entity.getContentLength() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
            }
            int i = (int) entity.getContentLength();
            if (i < 0) {
                i = 4096;
            }
            Reader reader = new InputStreamReader(instream, charset);
            CharArrayBuffer buffer = new CharArrayBuffer(i);
            char[] tmp = new char[1024];
            int l;
            while ((l = reader.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
            return buffer.toString();
        } finally {
            instream.close();
        }
    }

}
