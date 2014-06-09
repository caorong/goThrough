package wong.spance.gothrough.proxy;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Created by spance on 14/6/4.
 */
public class ProxyPathSelector {

    private final Pattern src;
    private final String target;
    private ProxyAction proxyAction;

    @JSONCreator
    public ProxyPathSelector(
            @JSONField(name = "src") String src,
            @JSONField(name = "target") String target) {
        this.src = Pattern.compile(src);
        this.target = target;
    }

    public Pattern getSrc() {
        return src;
    }

    public String getTarget() {
        return target;
    }

    public ProxyAction getProxyAction() {
        return proxyAction;
    }

    public void setProxyAction(ProxyAction proxyAction) {
        this.proxyAction = proxyAction;
    }

    public String toTargetUrl(String pathInfo) {
        String url = src.matcher(pathInfo).replaceFirst(target);
        if (!url.contains("://"))
            url = "http://" + url;
        return url;
    }

    public void afterSelected(ProxyContext proxyContext) throws URISyntaxException {
        String url = toTargetUrl(proxyContext.pathInfo);
        URIBuilder targetUri = new URIBuilder(url);
        if (StringUtils.isBlank(targetUri.getHost())) {
            throw new URISyntaxException(targetUri.toString(), "not special host");
        }
        // Handle the query string
        String queryString = proxyContext.request.getQueryString();//ex:(following '?'): name=value&foo=bar#fragment
        if (StringUtils.isNotBlank(queryString)) {
            int fragIdx = queryString.indexOf('#');
            String queryNoFrag = (fragIdx < 0 ? queryString : queryString.substring(0, fragIdx));
            targetUri.setQuery(queryNoFrag);
            if (fragIdx >= 0) {
                targetUri.setFragment(queryString.substring(fragIdx + 1));
            }
        }
        proxyContext.setTargetUri(targetUri);
        String uriPrefix = StringUtils.substringBefore(target, "$");
        proxyContext.setTargetUriPrefix(uriPrefix);
    }
}
