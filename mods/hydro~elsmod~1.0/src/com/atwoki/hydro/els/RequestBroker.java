package com.atwoki.hydro.els;

import com.atwoki.hydro.system.Helper;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: ezrak
 * Date: 2014/04/07
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class RequestBroker extends BusModBase {

    // private HashMap <String, Object> _connectionList = null;
    private HttpClient _client;

    public void start() {
        super.start();
        logger.info("els broker: starting, config=\n" + Helper.jsonPrettyPrint(config));
        try {
            init();
            startRequestBroker();
        } catch (Exception e) {
            logger.error("els broker: failed to start", e);
            stop();
        }
    }

    public void stop() {
        try {
            logger.info("els broker: stopping");
            _client.close();
            super.stop();
            logger.info("els broker: stopped");
        } catch (Exception e) {
            logger.error("els broker: failed to stop", e);
        }
    }

    private void init() throws Exception {
        // _connectionList = new HashMap<>();
        _client = getClient();
    }

    private HttpClient getClient() {
        HttpClient client = vertx.createHttpClient()
                .setHost(config.getString("els_host"))
                .setPort(config.getNumber("els_port").intValue())
                .setConnectTimeout(config.getNumber("timeout", 200).intValue());
        if (config.getBoolean("keep_alive", false)) {
            client.setKeepAlive(true);
        } else {
            client.setMaxPoolSize(config.getNumber("connections", 10).intValue());
        }
        return client;
    }

    private void startRequestBroker() {
        eb.registerHandler(config.getString("address"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                logger.info("els broker: received message, msg=" + message);
                JsonObject reqObj = message.body();
                reqObj.putString("from", message.replyAddress()).putString("to", message.address());
                processRequestMessage(reqObj);
                message.reply(Boolean.TRUE);
            }
        });
    }

    private void processRequestMessage(JsonObject message) {
        String index = "hydro";
        String type = "eventtracker";
        String id = Helper.getID();
        String opType = "_create";
        String path = "/" + index + "/" + type + "/" + id + "/" + opType;
        JsonObject document = new JsonObject()
                .putObject(type, message);
        Buffer buffer = new Buffer(document.toString());
        HttpClientRequest request = _client.request("POST", path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                response.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        logger.info("els broker: response received, size=" + buffer.length() + ", msg=" + buffer.toString());
                    }
                });
            }
        });
        request.headers().set("Content-Type", "application/json");
        request.end(buffer);
        logger.info("els broker: client request, uri=" + config.getString("els_host") + ":" + config.getNumber("els_port") + "/" + path + ", buffer=" + buffer);
    }
}
