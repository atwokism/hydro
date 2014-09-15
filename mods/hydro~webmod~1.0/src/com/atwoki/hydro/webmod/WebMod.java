package com.atwoki.hydro.webmod;

import com.atwoki.hydro.system.Helper;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 7/16/13
 * Time: 8:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebMod extends BusModBase {

    private JsonObject _authCfg;
    private Map<String, JsonObject> _sessions;

    public void start() {
        super.start();
        logger.info("webmod: starting, config=\n" + Helper.jsonPrettyPrint(config));
        init();
        startInbound();
        startServer();
    }

    public void stop() {
        try {
            logger.info("webmod: stopping");
            super.stop();
            logger.info("webmod: stopped");
        } catch (Exception e) {
            logger.error("webmod: error, desc=stop", e);
        }
    }

    protected HttpServer loadRoutes(HttpServer server) {
        Map<String, Object> params;
        RouteMatcher r = new RouteMatcher();
        for (Object o : config.getArray("routes")) {
            JsonObject route = (JsonObject) o;
            params = new HashMap<>();
            params.put("route_name", route.getString("name"));
            params.put("post_office", config.getString("post_office"));
            params.put("eb_uri", config.getObject("sjs_config").getString("prefix"));
            if (route.getBoolean("session")) params.put("sessions", _sessions);
            if (route.getBoolean("trace", false)) params.put("trace", route.getBoolean("trace"));
            try {
                Factory.chainHandler(r, route, Factory.getHandler(route, getVertx(), getContainer(), params));
            } catch (Exception e) {
                logger.error("webmod: route error, desc=could not chain route, route=" + Helper.jsonPrettyPrint(route) + ", error=" + e);
            }
            logger.info("webmod: route chained, route=" + Helper.jsonPrettyPrint(route));
        }
        return server.requestHandler(r);
    }

    private void init() {
        _sessions = new HashMap<>();
        _authCfg = config.getObject("auth");
    }

    private void startInbound() {
        String authNotifyAddress = _authCfg.getString("address") + ".notify";
        logger.info("webmod: subscribing, desc=auth notify, address=" + authNotifyAddress);
        eb.registerHandler(authNotifyAddress, new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject ticket = jsonObjectMessage.body();
                logger.info("webmod: received, desc=auth notify, ticket=" + Helper.jsonPrettyPrint(ticket));
                switch(ticket.getString("action")) {
                    case "login":
                        logger.info("webmod: cached identity, ticket=" + Factory.cacheSession(ticket, _sessions));
                        break;
                    case "logout":
                        logger.info("webmod: trashed identity, ticket=" + Factory.expireSession(ticket, _sessions));
                    default:
                        break;
                }
            }
        });
    }

    private void startServer() {
        HttpServer server = vertx.createHttpServer();
        server = loadRoutes(server);
        SockJSServer sjsServer = vertx.createSockJSServer(server);
        sjsServer.bridge(
                config.getObject("sjs_config"),
                config.getArray("inbound_permitted"),
                config.getArray("outbound_permitted"),
                _authCfg.getLong("timeout"),
                _authCfg.getString("address")
        );
        server.listen(config.getInteger("port"), config.getString("host"));
        logger.info("webmod: started, sockjs=true");
    }
}
