package wong.spance.gothrough.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wong.spance.gothrough.proxy.ProxyContext;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by spance on 14/6/4.
 */
public abstract class ContentProcessor {

    protected final static Logger log = LoggerFactory.getLogger(ContentProcessor.class);

    protected final Pattern contentTypePattern;
    protected final Pattern targetUriPattern;
    protected final List<ProcessRule> processRules;

    protected ContentProcessor(String targetUriPattern, String contentType, List<ProcessRule> processRules) {
        this.targetUriPattern = targetUriPattern == null ? null : Pattern.compile(targetUriPattern);
        this.contentTypePattern = contentType == null ? null : Pattern.compile(contentType);
        this.processRules = processRules;
    }

    public boolean accept(String targetUri, String contentType) {
        if (targetUriPattern == null || !targetUriPattern.matcher(targetUri).find())
            return false;
        if (contentTypePattern == null || !contentTypePattern.matcher(contentType).find())
            return false;
        return true;
    }

    public void process(ProxyContext proxyContext) throws IOException {
        proxyContext.setProcessed(true);
        ProcessState processState = new ProcessState();
        process(proxyContext, processState);
        proxyContext.setProcessState(processState);
    }

    protected abstract void process(ProxyContext proxyContext, ProcessState processState) throws IOException;
}
