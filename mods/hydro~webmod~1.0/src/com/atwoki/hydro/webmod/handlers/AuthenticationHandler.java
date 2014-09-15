package com.atwoki.hydro.webmod.handlers;

import com.atwoki.hydro.system.Helper;
import com.atwoki.hydro.webmod.Factory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2013/09/28
 * Time: 11:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuthenticationHandler implements Handler<HttpServerRequest> {

    private Logger _logger;
    private JsonObject _config;
    private String _webRoot, _webRootPrefix, _indexPage, _mainPage;
    private EventBus _eb;
    private boolean _trace;
    private Map<String, Object> _params;

    public AuthenticationHandler(Vertx vertx, Container container, Map<String, Object> params) {
        _logger = container.logger();
        _config = container.config();
        _eb = vertx.eventBus();
        _params = params;
        if (params.containsKey("trace")) _trace = ((Boolean) params.get("trace")).booleanValue();
        init();
    }

    private void init() {
        _webRoot = _config.getString("web_root");
        _webRootPrefix = _webRoot + "/";
        _indexPage = _webRootPrefix + _config.getString("index_page");
        _mainPage = _webRootPrefix + _config.getString("main_page");
    }

    @Override
    public void handle(final HttpServerRequest request) {
        final Buffer __body = new Buffer(0);
        request.expectMultiPart(true);
        if (_trace) Factory.traceRequest(request, _logger, "handler, authentication");
        request.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer buffer) {
                __body.appendBuffer(buffer);
            }
        });
        request.endHandler(new VoidHandler() {
            public void handle() {
                try {
                    processAuthRequest(request, __body);
                } catch (Exception e) {
                    _logger.error("handler, authentication: unexpected error, msg=" + e);
                    Factory.serveResource(request, _indexPage);
                    /* TODO (fix) - this is a JSON handler so serve error JSON with 503
                    JsonObject errJson = new JsonObject()
                            .putString("message", e.getMessage())
                            .putString("exception", e.toString());
                    Factory.serveErrorJson(request, errJson);
                    */
                    _logger.info("handler, authentication: response, tgt=" + _indexPage);
                }
            }
        });
        if (_trace) Factory.traceResponse(request.response(), _logger, "handler, authentication");
    }

    private void processAuthRequest(final HttpServerRequest request, Buffer body) {
        JsonObject authMsg = new JsonObject(body.toString());
        authMsg.putString("sender", request.remoteAddress().getHostName());
        if (authMsg.getString("action").equals("login")) {
            processLoginRequest(authMsg, request, body);
        } else if (authMsg.getString("action").equals("logout")) {
            processLogoutRequest(authMsg, request, body);
        } else {
            Factory.serveJson(request, new JsonObject().putString("error", "unsupported auth request"));
        }
    }

    private void processLoginRequest(final JsonObject authMsg, final HttpServerRequest request, Buffer body) {
        _logger.info("handler, authentication: received request, msg=" + Helper.jsonPrettyPrint(authMsg));
        String loginAddress = _config.getObject("auth").getString("address") + ".login";
        _logger.info("handler, authentication: sending auth, address=" + loginAddress);
        _eb.send(loginAddress, authMsg, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                String r = _indexPage;
                if (json.getString("status").equals("ok")) {
                    r = authMsg.getString("resource");
                    json.putString("route", r);
                    json.putString("eb_uri", _params.get("eb_uri").toString());
                    Factory.serveSecureJson(request, json);
                } else {
                    json.putString("route", r);
                    Factory.serveJson(request, json);
                }
                _logger.info("handler, authentication: response, msg=" + Helper.jsonPrettyPrint(json));
            }
        });
    }

    private void processLogoutRequest(final JsonObject authMsg, final HttpServerRequest request, Buffer body) {
        _logger.info("handler, authentication: received request, msg=" + Helper.jsonPrettyPrint(authMsg));
        String logoutAddress = _config.getObject("auth").getString("address") + ".logout";
        _logger.info("handler, authentication: sending auth, address=" + logoutAddress);
        _eb.send(logoutAddress, authMsg, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                json.putString("route", authMsg.getString("resource"));
                Factory.serveJson(request, json);
                _logger.info("handler, authentication: response, msg=" + Helper.jsonPrettyPrint(json));
            }
        });
    }
}
