package com.atwoki.hydro.webmod.handlers;

import com.atwoki.hydro.webmod.Factory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
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
public class StaticResourceHandler implements Handler<HttpServerRequest> {

    private Logger _logger;
    private JsonObject _config;
    private String _webRoot;
    private boolean _trace;

    public StaticResourceHandler(Vertx vertx, Container container, Map<String, Object> params) {
        _logger = container.logger();
        _config = container.config();
        if (params.containsKey("trace")) _trace = ((Boolean) params.get("trace")).booleanValue();
        init();
    }

    private void init() {
        _webRoot = _config.getString("web_root");
    }

    @Override
    public void handle(HttpServerRequest request) {
        if (_trace) Factory.traceRequest(request, _logger, "handler, static resource");
        if (!request.path().contains("..")) {
            Factory.serveResource(request, _webRoot + request.path());
        }
        if (_trace) Factory.traceResponse(request.response(), _logger, "handler, static resource");
    }
}
