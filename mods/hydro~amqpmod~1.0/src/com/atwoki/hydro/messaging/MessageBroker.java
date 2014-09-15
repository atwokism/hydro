package com.atwoki.hydro.messaging;

import java.util.HashMap;
import com.atwoki.hydro.messaging.gateway.AMQPMessageReceiver;
import com.atwoki.hydro.messaging.gateway.AMQPMessageSender;
import com.atwoki.hydro.system.Helper;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2013/11/17
 * Time: 6:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageBroker extends BusModBase {

    private BrokerSender _sender;
    private BrokerReceiver _receiver;
    private HashMap<String, Object> _retryList;
    private JsonObject _sendCfg, _recvCfg;

    public void start() {
        super.start();
        logger.info("message broker: starting, config=\n" + Helper.jsonPrettyPrint(config));
        try {
            init();
            startSender();
            startReceiver();
        } catch (Exception e) {
            logger.error("message broker: failed to start", e);
            stop();
        }
    }

    public void stop() {
        try {
            logger.info("message broker: stopping");
            _sender.cleanup();
            super.stop();
            logger.info("message broker: stopped");
        } catch (Exception e) {
            logger.error("message broker: failed to stop", e);
        }
    }

    private void init() throws Exception {
        _retryList = new HashMap<>();
        _sendCfg = config.getObject("send").putString("broker", config.getString("broker"));
        _recvCfg = config.getObject("receive").putString("broker", config.getString("broker"));

        _sender = new AMQPMessageSender();
        _sender.init(getContainer(), getVertx());
        _sender.configure(_sendCfg);

        _receiver = new AMQPMessageReceiver();
        _receiver.init(getContainer(), getVertx());
        _receiver.configure(_recvCfg);
    }

    private void startSender() throws Exception {
        if (_sender.isConfigured()) {
            logger.info("message broker: sender starting, conf=" + _sendCfg);
            logger.info("message broker: sender configuration, uri=" + _sender.configuration());
            eb.registerHandler(_sendCfg.getString("address"), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    try {
                        JsonObject data = message.body();
                        if (data != null) {
                            _sender.send(new StringBuffer(data.toString()));
                        } else {
                            logger.warn("message broker: sender error, no data");
                        }
                    } catch (Exception e) {
                        String queue = _sendCfg.getString("exchange") + "." + _sendCfg.getString("route");
                        _retryList.put(Helper.getID() + "." + queue, message.body().toString());
                        logger.error("message broker: sender error, queue=" + _sendCfg.getString("exchange"), e);
                    }
                }
            });
            logger.info("message broker: sender ready");
        } else {
            logger.info("message broker: sender, not configured");
        }
    }

    private void startReceiver() throws Exception {
        if (_receiver.isConfigured()) {
            logger.info("message broker: receiver starting, conf=" + _recvCfg);
            logger.info("message broker: receiver configuration, uri=" + _sender.configuration());
            try {
                 _receiver.receive();
                logger.info("message broker: receiver ready");
            } catch (Exception e) {
                logger.error("message broker: receiver error, desc=" + e);
            }
        } else {
            logger.info("message broker: receiver, not configured");
        }
    }
}
