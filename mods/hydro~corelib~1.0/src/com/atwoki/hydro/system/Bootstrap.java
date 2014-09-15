package com.atwoki.hydro.system;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2013/08/25
 * Time: 11:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class Bootstrap extends BusModBase {

    private TransactionCache _cache;

    public void start() {
        super.start();

        java.util.logging.Logger l = java.util.logging.Logger.getGlobal();
        l.setLevel(Level.INFO);

        logger.info("info");
        logger.debug("debug");
        logger.warn("warn");
        logger.error("error");
        logger.fatal("fatal");
        logger.trace("trace");

        logger.debug("starting bootstrap ...\nenvironment: " + Helper.mapPrettyPrint(container.env()));
        _cache = new TransactionCache(container, vertx, config);
        logger.debug("bootstrap: tx cache, name=" + _cache.name());
        Iterator modules = config.getArray("modules").iterator();
        while (modules.hasNext()) {
            final String modName = modules.next().toString();
            JsonObject modConf = config.getObject(modName).getObject("config");
            if (modConf != null) {
                Number modIns = config.getObject(modName).getInteger("instances");
                logger.debug("bootstrap: deploying, module=" + modName);
                deploy(modName, modConf, modIns);
            }
        }
    }

    private void deploy(final String modName, JsonObject modConf, Number modIns) {
        logger.debug("bootstrap: deploying, module=" + modName);
        container.deployModule(modName, modConf, modIns.intValue(), new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                if (asyncResult != null && asyncResult.succeeded()) {
                    logger.debug("bootstrap: deployment succeeded, mod=" + modName + ", result=" + asyncResult.result());
                } else if (asyncResult != null && asyncResult.failed()) {
                    logger.error("bootstrap: deployment failed, mod=" + modName + ", cause=" + asyncResult.cause());
                    logger.error("-> error:", asyncResult.cause());
                } else if (asyncResult == null) {
                    logger.warn("bootstrap: deployment anomaly, mod=" + modName + " cause=[unknown], result=failed");
                }
            }
        });
    }

    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
