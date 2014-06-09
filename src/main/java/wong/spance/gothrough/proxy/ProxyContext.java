package wong.spance.gothrough.proxy;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wong.spance.gothrough.process.ProcessState;
import wong.spance.gothrough.utils.HttpUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by spance on 14/6/4.
 */
public class ProxyContext {

    private final static Logger log = LoggerFactory.getLogger(ProxyContext.class);
    private final static AtomicInteger ID = new AtomicInteger(0);
    public final HttpServletRequest request;
    public final HttpServletResponse response;
    public final String pathInfo;
    public final HttpClient httpClient;
    public final String id;
    public final int compressible;
    private URIBuilder targetUri;
    private String targetUriString;
    private String targetUriPrefix;
    private String requestURL;
    private HttpEntity httpEntity;
    private boolean processed;
    private ProcessState processState;
    public HttpRequest targetRequest;
    public HttpResponse targetResponse;

    public ProxyContext(HttpServletRequest request, HttpServletResponse response, ProxyManager proxyManager) {
        id = String.format("%08X", ID.incrementAndGet());
        this.request = request;
        this.response = response;
        pathInfo = HttpUtils.processDefaultPath(request);
        requestURL = request.getRequestURL().toString();
        String acceptEncoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
        if (StringUtils.containsIgnoreCase(acceptEncoding, "gzip"))
            compressible = 2;
        else if (StringUtils.containsIgnoreCase(acceptEncoding, "deflate"))
            compressible = 1;
        else
            compressible = -1;
        httpClient = proxyManager.getOrCreateHttpClient();
    }

    public URIBuilder getTargetUri() {
        return targetUri;
    }

    public void setTargetUri(URIBuilder targetUri) {
        this.targetUri = targetUri;
        targetUriString = targetUri.toString();
        log.debug("{} ... Started srcIP={} userAgent=[{}] \t dest={}", id,
                request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT), targetUriString);
    }

    public String getTargetUriString() {
        return targetUriString;
    }

    public String getTargetUriPrefix() {
        return targetUriPrefix;
    }

    public void setTargetUriPrefix(String targetUriPrefix) {
        this.targetUriPrefix = targetUriPrefix;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public ProcessState getProcessState() {
        return processState;
    }

    public void setProcessState(ProcessState processState) {
        this.processState = processState;
    }

    public HttpEntity getHttpEntity() {
        return httpEntity;
    }

    public void setHttpEntity(HttpEntity httpEntity) {
        this.httpEntity = httpEntity;
    }

    public HttpResponse doFetch() throws IOException {
        long t1 = System.currentTimeMillis(), t2;
        targetRequest = HttpUtils.buildHttpRequest(this);
        HttpUtils.copyRequestHeaders(this, targetRequest);

        HttpHost targetHost = new HttpHost(targetUri.getHost(), targetUri.getPort());
        targetResponse = httpClient.execute(targetHost, targetRequest, (HttpContext) null);
        httpEntity = targetResponse.getEntity();
        t2 = System.currentTimeMillis();
        String contentType = HttpUtils.getContentType(httpEntity);
        log.debug("{} --> Fetch {}=[{} {}] Time={} {}", id,
                request.getMethod(),
                targetResponse.getStatusLine().getStatusCode(),
                targetResponse.getStatusLine().getReasonPhrase(),
                contentType,
                t2 - t1,
                StringUtils.abbreviate(targetRequest.getRequestLine().getUri(), 60)
        );
        return targetResponse;
    }

    public void releaseFetch() {
        try {
            EntityUtils.consume(httpEntity);
        } catch (IOException ex) {
        }
    }

    public void abort() {
        if (targetRequest instanceof AbortableHttpRequest) {
            ((AbortableHttpRequest) targetRequest).abort();
        }
    }

}
