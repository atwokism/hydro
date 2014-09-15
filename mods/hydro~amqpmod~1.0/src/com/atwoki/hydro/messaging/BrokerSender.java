package com.atwoki.hydro.messaging;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created with IntelliJ IDEA.
 * User: ezrak
 * Date: 2014/03/05
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface BrokerSender {

    /**
     *
     * @param c
     * @param v
     */
    public void init(Container c, Vertx v);

    /**
     *
     * @param config
     */
    public void configure(JsonObject config);

    /**
     *
     * @return
     */
    public boolean isConfigured();

    /**
     *
     * @param message
     */
    public void send(StringBuffer message);

    public void cleanup();

    public String configuration();
}
