package wong.spance.gothrough.process;

/**
 * Created by spance on 14/6/4.
 */
public interface ProcessRule {

    String process(String content, ProcessState processState);
}
