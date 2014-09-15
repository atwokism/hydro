package com.atwoki.hydro.eventpub.postoffice;

import com.atwoki.framework.postoffice.DefaultWorkListener;
import com.atwoki.hydro.system.Helper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by Ezra.Kahimbaara on 9/12/14.
 */
public class CallbackListener extends DefaultWorkListener {

    private EventBus _eb;
    private Logger _logger;
    private String _address;

    public CallbackListener(Vertx v, Container c) {
        _eb = v.eventBus();
        _logger = c.logger();
    }

    @Override
    public void notified(Object source, Object message) {
        _logger.info("callback listener: dispatching callback, source=" + source.getClass().getName() + ", target=" + getAddress());
        JsonObject json = new JsonObject(message.toString()); // critical assumption that all callback messages are in JSON format
        _eb.send(getAddress(), json, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonMessage) {
                _logger.info("callback listener: dispatched callback, result=" + Helper.jsonPrettyPrint(jsonMessage.body()));
            }
        });
    }

    public String getAddress() {
        return _address;
    }

    public void setAddress(String address) {
        this._address = address;
    }
}
