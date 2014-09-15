package com.atwoki.hydro.eventpub.postoffice;

import com.atwoki.framework.postoffice.DefaultPostOfficeWorker;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by Ezra.Kahimbaara on 9/12/14.
 */
public class CallbackWorker extends DefaultPostOfficeWorker {

    private Logger _logger;
    public CallbackWorker(Vertx v, Container c, String id) {
        super(id);
        _logger = c.logger();
    }
    public void work(Object message) {
        try {
            notifyListeners(message);
        } catch (Exception e) {
            _logger.error("callback worker: error working callback", e);
        }
    }
}
