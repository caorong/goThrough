package wong.spance.gothrough.process;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by spance on 14/6/4.
 */
public class ReplaceRule implements ProcessRule {

    private Pattern pattern;
    private String replacement;

    @JSONCreator
    public ReplaceRule(
            @JSONField(name = "pattern") String pattern,
            @JSONField(name = "replacement") String replacement) {
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
    }

    @Override
    public String process(String content, ProcessState processState) {
        StringBuffer buffer = new StringBuffer();
        Matcher ma = pattern.matcher(content);
        while (ma.find()) {
            ma.appendReplacement(buffer, replacement);
        }
        ma.appendTail(buffer);
        processState.record(content.length(), buffer.length(), getClass());
        return buffer.toString();
    }
}
