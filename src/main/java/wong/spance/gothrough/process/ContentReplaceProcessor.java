package wong.spance.gothrough.process;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import wong.spance.gothrough.proxy.ProxyContext;
import wong.spance.gothrough.utils.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by spance on 14/6/4.
 */
public class ContentReplaceProcessor extends ContentProcessor {

    @JSONCreator
    protected ContentReplaceProcessor(
            @JSONField(name = "targetUriPattern") String targetUriPattern,
            @JSONField(name = "contentTypePattern") String contentType,
            @JSONField(name = "contentProcessors") List<ProcessRule> processRules) {
        super(targetUriPattern, contentType, processRules);
    }

    @Override
    protected void process(ProxyContext proxyContext, ProcessState processState) throws IOException {
        HttpEntity entity = proxyContext.getHttpEntity();
        Charset currentCharset = HttpUtils.getCharset(entity);
        InputStream inputStream = entity.getContent();
        // toString
        String content = HttpUtils.toString(entity, inputStream, currentCharset); // toString already close inputStream
        //System.out.println(content);
        // 循环ProcessRule 多次处理
        for (ProcessRule rule : processRules) {
            content = rule.process(content, processState);
        }
        StringEntity newEntity = new StringEntity(content, currentCharset);
        Header contentType = entity.getContentType();
        if (contentType != null)
            newEntity.setContentType(contentType);
        proxyContext.setHttpEntity(newEntity);
    }


}
