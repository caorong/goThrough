package wong.spance.gothrough.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wong.spance.gothrough.proxy.ProxyAction;
import wong.spance.gothrough.proxy.ProxyManager;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by spance on 14/6/3.
 */
@SuppressWarnings("unchecked")
public class Configuration {

    private final static Logger log = LoggerFactory.getLogger(Configuration.class);
    public final static String CLASSES_PATH = "/WEB-INF/classes/";
    public final static String PROXY_CONFIG = "config.proxy.json";
    public final static String APP_CONFIG = "config.app.json";
    public final static Configuration THIS = new Configuration();
    private ServletContext context;
    private Map<String, Object> defined;

    private Configuration() {
    }

    public static void init(ServletContext context) {
        if (THIS.context != null) {
            throw new RuntimeException("Configuration has already initialized!");
        }
        THIS.context = context;
        String json = null;

        try (InputStream configInputStream = context.getResourceAsStream(CLASSES_PATH + APP_CONFIG)) {
            json = IOUtils.toString(configInputStream, "utf-8").replaceAll("/\\*.*?\\*/", "");
            THIS.defined = JSON.parseObject(json);
        } catch (Exception e) {
            log.error(APP_CONFIG + " can't be load.", e);
        }

        try (InputStream configInputStream = context.getResourceAsStream(CLASSES_PATH + PROXY_CONFIG)) {
            json = IOUtils.toString(configInputStream, "utf-8").replaceAll("/\\*.*?\\*/", "");
        } catch (Exception e) {
            log.error(PROXY_CONFIG + " can't be load.", e);
        }

        try {
            List<ProxyAction> config = JSON.parseArray(json, ProxyAction.class);
            ProxyManager.INSTANCE.configProxy(config);
        } catch (Exception e) {
            log.error(PROXY_CONFIG + " can't be parse.", e);
        }
    }

    public Set<String> scanLocal() throws IOException {
        Set<String> root = context.getResourcePaths("/");
        for (Iterator<String> it = root.iterator(); it.hasNext(); ) {
            String file = it.next();
            System.out.println(file);
            if (file.matches("/\\w+-INF/.*"))
                it.remove();
        }
        return root;
    }

    public <T> T get(Define key) {
        return (T) defined.get(key.name());
    }

    public static enum Define {
        ConnIdleAllowed,
        DefaultMaxPerRoute,
        MaxTotal
    }

}
