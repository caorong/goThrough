package wong.spance.gothrough.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wong.spance.gothrough.process.ContentProcessor;

import java.io.IOException;
import java.util.List;

/**
 * Created by spance on 14/6/3.
 */
public abstract class ProxyAction {

    final static Logger log = LoggerFactory.getLogger(ProxyAction.class);
    protected final List<ContentProcessor> contentRules;
    protected final List<ProxyPathSelector> proxyPathSelectors;
    protected ProxyManager manager;

    protected ProxyAction(List<ProxyPathSelector> proxyPathSelectors, List<ContentProcessor> contentRules) {
        this.proxyPathSelectors = proxyPathSelectors;
        this.contentRules = contentRules;
    }

    public abstract void doProxy(ProxyContext proxyContext) throws IOException;

    public List<ContentProcessor> getContentRules() {
        return contentRules;
    }

    public List<ProxyPathSelector> getProxyPathSelectors() {
        return proxyPathSelectors;
    }

    public ProxyManager getManager() {
        return manager;
    }

    public void setManager(ProxyManager manager) {
        this.manager = manager;
    }
}
