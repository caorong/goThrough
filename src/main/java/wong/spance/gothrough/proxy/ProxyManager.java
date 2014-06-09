package wong.spance.gothrough.proxy;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import wong.spance.gothrough.utils.Configuration;
import wong.spance.gothrough.utils.HttpUtils;

import javax.net.ssl.SSLHandshakeException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by spance on 14/6/3.
 */
public class ProxyManager {

    public final static ProxyManager INSTANCE = new ProxyManager();

    private PoolingClientConnectionManager connMgr;
    private IdleConnectionMonitorThread idleThread;
    private HttpParams httpParams;
    private HttpClient httpClient;
    private List<ProxyPathSelector> proxyPathSelectors;

    private ProxyManager() {
    }

    public void configProxy(List<ProxyAction> proxyActions) {
        this.proxyPathSelectors = new ArrayList<>(proxyActions.size());
        for (ProxyAction proxyAction : proxyActions) {
            List<ProxyPathSelector> selectors = proxyAction.getProxyPathSelectors();
            for (ProxyPathSelector selector : selectors) {
                selector.setProxyAction(proxyAction);
                proxyPathSelectors.add(selector);
            }
        }
    }

    //请求重试处理
    private HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {

        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            if (executionCount >= 2) // 如果超过最大重试次数
                return false;
            if (exception instanceof NoHttpResponseException) // 如果服务器丢掉了连接，那么就重试
                return true;
            if (exception instanceof SSLHandshakeException) // 不要重试SSL握手异常
                return false;
            HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            return idempotent;
        }
    };

    public void init() {
        connMgr = new PoolingClientConnectionManager();
        connMgr.setMaxTotal(Configuration.THIS.<Integer>get(Configuration.Define.MaxTotal));
        connMgr.setDefaultMaxPerRoute(Configuration.THIS.<Integer>get(Configuration.Define.DefaultMaxPerRoute));

        httpParams = new BasicHttpParams();
        HttpClientParams.setRedirecting(httpParams, true);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);//设定连接等待时间
        HttpConnectionParams.setSoTimeout(httpParams, 15 * 1000);//设定连接等待时间
        HttpConnectionParams.setConnectionTimeout(httpParams, 15 * 1000);//设定超时时间
        HttpConnectionParams.setSocketBufferSize(httpParams, 8192);

        if (!Configuration.THIS.<Boolean>get(Configuration.Define.ConnIdleAllowed)) {
            idleThread = new IdleConnectionMonitorThread(connMgr);
            idleThread.start();
        }
    }

    public void destroy() {
        if (idleThread != null)
            idleThread.shutdown();
        connMgr.shutdown();
    }

    public HttpClient createHttpClient() {
        DefaultHttpClient client = new DefaultHttpClient(connMgr, httpParams);
        //client.addRequestInterceptor(new RequestAcceptEncoding());
        client.addResponseInterceptor(new ResponseContentEncoding());
        client.setHttpRequestRetryHandler(httpRequestRetryHandler);
        return client;
    }

    public HttpClient getOrCreateHttpClient() {
        if (httpClient != null)
            return httpClient;
        return httpClient = createHttpClient();
    }

    public ProxyPathSelector select(HttpServletRequest request) {
        String pathInfo = HttpUtils.processDefaultPath(request);
        for (ProxyPathSelector urlSelector : proxyPathSelectors) {
            if (urlSelector.getSrc().matcher(pathInfo).matches()) {
                return urlSelector;
            }
        }
        return null;
    }

}
