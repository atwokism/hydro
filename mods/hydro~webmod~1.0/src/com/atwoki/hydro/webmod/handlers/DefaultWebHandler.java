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
 * Date: 2013/09/23
 * Time: 12:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultWebHandler implements Handler<HttpServerRequest> {

    private JsonObject _config;
    private String _webRoot, _webRootPrefix, _indexPage;
    private Map<String, JsonObject> _userCache;
    private Logger _logger;
    private boolean _trace;

    public DefaultWebHandler(Vertx vertx, Container container, Map<String, Object> params) {
        _logger = container.logger();
        _config = container.config();
        _userCache = (Map<String, JsonObject>) params.get("sessions");
        if (params.containsKey("trace")) _trace = ((Boolean) params.get("trace")).booleanValue();
        init();
    }

    private void init() {
        _webRoot = _config.getString("web_root");
        _webRootPrefix = _webRoot + "/";
        _indexPage = _webRootPrefix + _config.getString("index_page");
    }

    @Override
    public void handle(HttpServerRequest request) {
        if (_trace) Factory.traceRequest(request, _logger, "handler, default web");
        if (!request.path().contains("..")) {
            boolean secure = Factory.isSecure(request, _userCache);
            _logger.info("handler, default web: request, secure=" + secure);
            String resource = _indexPage;
            if (secure) {
                resource = _webRoot + request.path();
            }
            _logger.info("handler, default web: response, resource=" + resource);
            Factory.serveResource(request, resource);
        } else {
            request.response().setStatusCode(404).end();
        }
        if (_trace) Factory.traceResponse(request.response(), _logger, "handler, default web");
    }
}
