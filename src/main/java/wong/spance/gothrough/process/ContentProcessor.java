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
    protected final List<ProcessRule> processRules;

    protected ContentProcessor(String contentType, List<ProcessRule> processRules) {
        this.contentTypePattern = Pattern.compile(contentType);
        this.processRules = processRules;
    }

    public boolean accept(String contentType) {
        return contentTypePattern.matcher(contentType).find();
    }

    public void process(ProxyContext proxyContext) throws IOException {
        proxyContext.setProcessed(true);
        ProcessState processState = new ProcessState();
        process(proxyContext, processState);
        proxyContext.setProcessState(processState);
    }

    protected abstract void process(ProxyContext proxyContext, ProcessState processState) throws IOException;
}
