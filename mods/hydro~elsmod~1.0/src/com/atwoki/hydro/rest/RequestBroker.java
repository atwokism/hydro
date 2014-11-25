package com.atwoki.hydro.rest;

import com.atwoki.hydro.system.Helper;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ezrak
 * Date: 2014/04/07
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class RequestBroker extends BusModBase {

    private HashMap<String, Object> _clientPool;
    private Handler<HttpClientResponse> _xmlHandler;
    private JSONHandler _jsonHandler;

    public void start() {
        super.start();
        logger.info("rest broker: starting, config=\n" + Helper.jsonPrettyPrint(config));
        try {
            init();
            startRequestBroker();
        } catch (Exception e) {
            logger.error("rest broker: failed to start", e);
            stop();
        }
    }

    public void stop() {
        try {
            logger.info("rest broker: stopping");
            // _client.close();
            super.stop();
            logger.info("rest broker: stopped");
        } catch (Exception e) {
            logger.error("rest broker: failed to stop", e);
        }
    }

    private void init() throws Exception {
        _clientPool = new HashMap<>();
        JsonArray targets = config.getArray("targets");
        for (int t = 0; t < targets.size(); t++) {
            JsonObject tObj = targets.get(t);
            HttpClient c = createClient(tObj);
            if (tObj != null) {
                _clientPool.put(tObj.getString("key"), c);
            }
        }
        _jsonHandler = new JSONHandler();
    }

    private void processJSONResponse(Message<JsonObject> msgInstance, JsonObject jsonRes) {
        // TODO robust logic required.
        sendOK(msgInstance, jsonRes);
    }

    private HttpClient createClient(JsonObject tObj) {
        try {
            HttpClient client = vertx.createHttpClient()
                    .setHost(tObj.getString("host"))
                    .setPort(tObj.getInteger("port"))
                    .setConnectTimeout(tObj.getNumber("timeout", 1000).intValue());
            if (tObj.getBoolean("keep_alive", false)) {
                client.setKeepAlive(true);
            } else {
                client.setMaxPoolSize(tObj.getNumber("connections", 10).intValue());
            }
            logger.info("rest broker: created client, cfg=" + tObj);
            return client;
        } catch (Exception e) {
            logger.error("rest broker: failed to create client", e);
        }
        return null;
    }

    private void startRequestBroker() {
        eb.registerHandler(config.getString("address"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                logger.info("rest broker: handle request, msg=" + message);
                processRequestMessage(message);
            }
        });
    }

    private void processRequestMessage(final Message<JsonObject> message) {

        logger.info("rest broker: request, from=" + message.replyAddress() + ", to=" + message.address());

        JsonObject json = message.body();
        String key = json.getString("key");
        String path = json.getString("path");
        String method = json.getString("method");
        String type = "application/" + json.getString("type");
        JsonObject data = json.getObject("data");

        HttpClient client = (HttpClient) _clientPool.get(key);
        _jsonHandler.tagRequestMessageInstance(message);
        HttpClientRequest request = client.request(method, path, _jsonHandler);
        request.headers().set("Content-Type", type);

        if (method.equalsIgnoreCase("POST") && data != null) {
            request.end(data.encode());
        } else {
            request.end();
        }

        logger.info("rest broker: HTTP request, uri=" + path);
    }

    protected class JSONHandler implements Handler<HttpClientResponse> {
        private Message<JsonObject> __msgInstance;
        public void tagRequestMessageInstance(Message<JsonObject> message) {
            __msgInstance = message;
        }
        @Override
        public void handle(HttpClientResponse response) {
            response.bodyHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                    logger.info("rest broker: response received, size=" + buffer.length() + ", msg=" + buffer.toString());
                    JsonObject jsonRes = new JsonObject(buffer.toString());
                    processJSONResponse(__msgInstance, jsonRes);
                }
            });
        }
    }
}
