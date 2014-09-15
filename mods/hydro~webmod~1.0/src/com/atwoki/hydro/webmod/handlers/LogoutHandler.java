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
 * User: ezrak
 * Date: 2014/02/23
 * Time: 11:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogoutHandler implements Handler<HttpServerRequest> {

    private Logger _logger;
    private JsonObject _config;
    private String _webRoot, _webRootPrefix, _indexPage, _mainPage;
    private EventBus _eb;
    private boolean _trace;

    public LogoutHandler(Vertx vertx, Container container, Map<String, Object> params) {
        _logger = container.logger();
        _config = container.config();
        _eb = vertx.eventBus();
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
        request.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer buffer) {
                __body.appendBuffer(buffer);
            }
        });
        request.endHandler(new VoidHandler() {
            public void handle() {
                try {
                    if (_trace) trace(request, __body);
                    JsonObject authMessage = marshall(__body);
                    processLogoutRequest(authMessage, request, __body);
                    _logger.info("handler, logout: signout request, msg=" + authMessage);
                } catch (Exception e) {
                    _logger.error("handler, logout: unexpected error, msg=" + e);
                }
                if (_trace) Factory.traceResponse(request.response(), _logger, "handler, logout");
            }
        });
    }

    private void processLogoutRequest(final JsonObject authMsg, final HttpServerRequest request, Buffer body) {
        _logger.info("handler, logout: received request, msg=" + Helper.jsonPrettyPrint(authMsg));
        String logoutAddress = _config.getObject("auth").getString("address") + ".logout";
        _logger.info("handler, logout: sending auth, address=" + logoutAddress);
        _eb.send(logoutAddress, authMsg, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                _logger.info("handler, logout: response, msg=" + Helper.jsonPrettyPrint(json));
                Factory.serveResource(request, _indexPage);
            }
        });
    }

    private JsonObject marshall(Buffer body) {
        JsonObject msg = null;
        if (body != null && !body.toString().isEmpty()) {
            msg = new JsonObject();
            String [] entries = body.toString().split("&");
            for (String entry : entries) {
                String [] pair = entry.split("=");
                if (pair.length >= 2) msg.putString(pair[0], pair[1]);
            }
        }
        return msg;
    }


    private void trace(final HttpServerRequest request, Buffer body) {
        Factory.traceRequest(request, _logger, "handler, logout");
        _logger.info("handler, logout: request, body=" + body + ", size=" + body.length());
    }
}
