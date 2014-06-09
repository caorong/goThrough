package wong.spance.gothrough;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wong.spance.gothrough.proxy.ProxyAction;
import wong.spance.gothrough.proxy.ProxyContext;
import wong.spance.gothrough.proxy.ProxyManager;
import wong.spance.gothrough.proxy.ProxyPathSelector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;

@WebServlet(urlPatterns = "/*")
public class ProxyServlet extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ProxyManager proxyManager = ProxyManager.INSTANCE;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        proxyManager.init();
    }

    @Override
    public void destroy() {
        super.destroy();
        proxyManager.destroy();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProxyPathSelector urlSelector = proxyManager.select(request);
        if (urlSelector != null) {
            ProxyContext context = new ProxyContext(request, response, proxyManager);
            ProxyAction proxyAction = urlSelector.getProxyAction();
            try {
                urlSelector.afterSelected(context);
                proxyAction.doProxy(context);
            } catch (URISyntaxException e) {
                log.info("", e);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (Exception e) {
                log.error("", e);
                throw new ServletException(e);
            }
        }
    }

}