package wong.spance.gothrough.proxy;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import wong.spance.gothrough.process.ContentProcessor;
import wong.spance.gothrough.process.ProcessState;
import wong.spance.gothrough.utils.HttpUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by spance on 14/6/3.
 */
public class ProxyFetchAction extends ProxyAction {

    @JSONCreator
    protected ProxyFetchAction(
            @JSONField(name = "proxyPathSelectors") List<ProxyPathSelector> proxyPathSelectors,
            @JSONField(name = "contentRules") List<ContentProcessor> contentRules) {
        super(proxyPathSelectors, contentRules);
    }

    @Override
    public void doProxy(ProxyContext proxyContext) throws IOException {
        try {
            proxyContext.doFetch();
            boolean noEntity = HttpUtils.sendRedirectOrNotModified(proxyContext);
            HttpUtils.respondHeaders(proxyContext);
            if (noEntity) {// 300-304 >=400 直接返回
                proxyContext.response.flushBuffer();
                return;
            }
            HttpEntity entity = proxyContext.getHttpEntity();
            if (entity == null) {
                return;
            }
            String contentType = ContentType.getOrDefault(entity).toString();
            for (ContentProcessor processor : contentRules) {
                if (processor.accept(contentType))
                    processor.process(proxyContext);
            }
            if (proxyContext.isProcessed()) {
                ProcessState processState = proxyContext.getProcessState();
                log.debug("{} *** Process {}", proxyContext.id, processState.toString());
            }

            respondBody(proxyContext);

        } catch (Exception e) {
            log.error("", e);
            proxyContext.abort();
            throw new RuntimeException(e);
        } finally {
            proxyContext.releaseFetch();
        }
    }

    public void respondBody(ProxyContext proxyContext) throws IOException {
        String contentEncoding = null, contentType = null;
        OutputStream out = null;
        long t1 = System.currentTimeMillis(), t2;
        try {
            out = proxyContext.response.getOutputStream();
            switch (proxyContext.compressible) {
                case 2:
                    out = new GZIPOutputStream(out);
                    contentEncoding = "gzip";
                    break;
                case 1:
                    out = new DeflaterOutputStream(out);
                    contentEncoding = "deflate";
                    break;
                default:
                    contentEncoding = "PLAIN";
            }
            proxyContext.response.setHeader(HttpHeaders.CONTENT_ENCODING, contentEncoding);
            HttpEntity entity = proxyContext.getHttpEntity();
            contentType = HttpUtils.getContentType(entity);
            entity.writeTo(out);

        } finally {
            if (out != null) {
                if (out instanceof DeflaterOutputStream) {
                    ((DeflaterOutputStream) out).finish();
                }
                out.flush();
            }
            t2 = System.currentTimeMillis();
            log.debug("{} <-- Respond Time={} {}-{}", proxyContext.id, t2 - t1, contentEncoding, contentType);
        }
    }

}
