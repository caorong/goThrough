package wong.spance.gothrough;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wong.spance.gothrough.utils.Configuration;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Created by spance on 14/6/8.
 */
@WebFilter(urlPatterns = {"/*"})
public class DispatcherFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private RequestDispatcher defaultRequestDispatcher;
    private RequestDispatcher jspRequestDispatcher;
    private Set<String> local;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String path = ((HttpServletRequest) request).getPathInfo();
        if (local.contains(path)) {
            log.info("Request local=" + path);
            (path.endsWith(".jsp") ? jspRequestDispatcher : defaultRequestDispatcher).forward(request, response);
        } else
            chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.defaultRequestDispatcher = servletContext.getNamedDispatcher("default");
        this.jspRequestDispatcher = servletContext.getNamedDispatcher("jsp");
        try {
            Configuration.init(servletContext);
            local = Configuration.THIS.scanLocal();
        } catch (Exception e) {
            local = Collections.EMPTY_SET;
            log.error("", e);
        }
    }
}
