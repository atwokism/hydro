package com.atwoki.hydro.webmod.handlers;

import com.atwoki.hydro.system.Helper;
import com.atwoki.hydro.system.TransactionCache;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2013/10/09
 * Time: 9:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONCommandHandler implements Handler<HttpServerRequest> {

    public static final String CONFIG_KEY = "handler_config";

    protected static int TIMEOUT = 100;

    private TransactionCache _cache;
    private Logger _logger;
    private JsonObject _config, _handlerConfig;
    private EventBus _eb;
    private String _postOffice;

    public JSONCommandHandler(Vertx vertx, Container container, Map<String, Object> params) {
        _logger = container.logger();
        _config = container.config();
        _eb = vertx.eventBus();
        _cache = new TransactionCache(container, vertx, _config);
        _postOffice = params.get("post_office").toString();
    }

    @Override
    public void handle(final HttpServerRequest request) {
        _logger.info("handler, json-post: received request, method=" + request.method() + ", path=" + request.path());
        final Buffer __body = new Buffer(0);
        request.expectMultiPart(true);
        request.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer buffer) {
                __body.appendBuffer(buffer);
            }
        });
        request.endHandler(new VoidHandler() {
            public void handle() {
                try {
                    JsonObject message = new JsonObject(__body.toString("UTF-8"));
                    message.putString("sender", request.remoteAddress().getHostName());
                    _logger.info("handler, json-post: handle message, msg=" + Helper.jsonPrettyPrint(message));
                    String action = message.getString("action");
                    if (action != null && !action.isEmpty()) {
                        switch(action) {
                            case "send":
                                processRequest(request.response(), message);
                                break;
                            case "execute":
                                executeRequest(request.response(), message); // blocking call
                                break;
                            case "callback":
                                processCallback(request.response(), message);
                                break;
                            default:
                                processOther(request.response(), message, "action not supported");
                                break;
                        }
                    } else {
                        processOther(request.response(), message, "action not specified");
                    }
                } catch (Exception e) {
                    _logger.error("handler, json-post: error, desc=" + e);
                    processError(request.response(), e, __body);
                }
            }
        });
    }

    private void processOther(HttpServerResponse response, JsonObject message, String label) {
        message.putString("id", message.getString("id", Helper.getID()));
        message.putString("status", "noop")
                .putString("tag", label);
        _eb.send(_postOffice, message);
        processResponse(response, message, 200);
    }

    private void processRequest(HttpServerResponse response, JsonObject message) throws Exception {
        String address = message.getString("address");
        String id = message.getString("id", Helper.getID());
        message.putString("id", id);
        String status = "noop"; // default no-address flag
        JsonObject resObj = new JsonObject();
        if (address != null && !address.isEmpty() && message != null) {
            _logger.info("handler, json-post: processing request, msg=" + Helper.jsonPrettyPrint(message));
            _eb.send(address, message);
            status = "ok";
        }
        message.putString("status", status);
        JsonObject env = new JsonObject()
                .putString("destination", message.getString("source"))
                .putObject("message", message);
        processResponse(response, resObj, 200);
    }

    private void executeRequest(final HttpServerResponse response, JsonObject message) throws Exception {
        String address = message.getString("address");
        String id = message.getString("id", Helper.getID());
        message.putString("id", id);
        Integer timeout = message.getInteger("timeout", new Integer(TIMEOUT));
        final JsonObject document = message;
        if (address != null && !address.isEmpty()) {
            _logger.info("handler, json-post: executing request, msg=" + Helper.jsonPrettyPrint(message));
            _eb.sendWithTimeout(address, message, timeout.intValue(), new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> result) {
                    JsonObject resObj = new JsonObject(document.toString());
                    if (result.succeeded()) {
                        resObj.putString("status", "ok");
                        processResponse(response, resObj, 200);
                    } else {
                        ReplyException e = (ReplyException) result.cause();
                        resObj.putString("fail_type", e.failureType().name())
                            .putString("status", "fail")
                            .putNumber("fail_code", e.failureCode())
                            .putString("fail_msg", e.getMessage());
                        processResponse(response, resObj, 200);
                    }
                }
            });
        } else {
            JsonObject resObj = new JsonObject(document.toString())
                    .putString("status", "fail")
                    .putString("fail_type", "NO_OPERATION")
                    .putNumber("fail_code", -1)
                    .putString("fail_msg", "No such operation");
            processResponse(response, resObj, 200);
        }
    }

    private void processCallback(final HttpServerResponse response, JsonObject message) throws Exception {
        _logger.info("handler, json-post: process callback, msg=" + Helper.jsonPrettyPrint(message));
        String correlation = message.getString("correlation", message.getString("id"));
        if (correlation != null && !correlation.isEmpty() && _cache.test(correlation)) {
            processRequest(response, message);
        } else {
            processOther(response, message, "no transaction for callback");
        }
    }

    private void processError(HttpServerResponse response, Throwable e, Buffer msg) {
        _logger.info("handler, json-post: process error, error=" + e + ", data=" + msg.toString());
        JsonObject errObj = new JsonObject()
                .putString("status", "error")
                .putString("error", e.getMessage());
        processResponse(response, errObj, 500);
    }

    private void processResponse(HttpServerResponse response, JsonObject json, int status) {
        _logger.info("handler, json-post: process response, msg=" + Helper.jsonPrettyPrint(json));
        response.setStatusCode(status)
                .putHeader("Content-Type", "application/json")
                .setChunked(true)
                .write(json.encode() + "\n")
                .end();
    }
}
