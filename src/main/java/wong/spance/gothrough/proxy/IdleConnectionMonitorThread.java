package wong.spance.gothrough.proxy;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.pool.ConnPoolControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by spance on 14/6/3.
 */
public class IdleConnectionMonitorThread extends Thread {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ClientConnectionManager connMgr;
    private final ConnPoolControl control;

    private volatile boolean shutdown;

    public IdleConnectionMonitorThread(ClientConnectionManager connMgr) {
        super();
        this.connMgr = connMgr;
        control = connMgr instanceof ConnPoolControl ? (ConnPoolControl) connMgr : null;
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(60000);
                    String old = null;
                    if (control != null) {
                        old = control.getTotalStats().toString();
                    }
                    // Close expired connections
                    connMgr.closeExpiredConnections();
                    // Optionally, close connections
                    // that have been idle longer than 30 sec
                    connMgr.closeIdleConnections(10, TimeUnit.MINUTES);
                    if (old != null) {
                        String newer = control.getTotalStats().toString();
                        if (!old.equals(newer)) {
                            log.debug("Pool stats changed OLD={}", old);
                            log.debug("Pool stats changed NEW={}", newer);
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            // terminate
        }
    }

    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }
}
