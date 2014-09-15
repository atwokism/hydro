package com.atwoki.hydro.webmod;

import com.atwoki.hydro.system.Helper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2013/09/23
 * Time: 12:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class Factory {

    // protected static final String CONFIG_HOST = "host";
    // protected static final String CONFIG_PORT = "port";
    protected static final String CONFIG_ROUTES_HANDLER = "handler";
    protected static final String CONFIG_ROUTES_METHOD = "method";
    protected static final String CONFIG_ROUTES_REGEX = "regex";
    protected static final String CONFIG_ROUTES_PATH = "path";

    private static Map<String, Handler<HttpServerRequest>> _handlers;

    /**
     *
     * @param handlerClass
     * @param vertx
     * @param container
     * @param params
     * @return
     */
    private static Handler<HttpServerRequest> createHandler(String handlerClass, Vertx vertx, Container container, Map<String, Object> params) {
        Handler<HttpServerRequest> h = null;
        try {
            Class cls = Class.forName(handlerClass);
            Constructor cns = cls.getConstructor(Vertx.class, Container.class, Map.class);
            h = (Handler<HttpServerRequest>) cns.newInstance(vertx, container, params);
        } catch (Exception e) {
            container.logger().error("factory: handler, error=could not create handler, desc=" + e);
        }
        return h;
    }

    /**
     *
     * @param route
     * @param vertx
     * @param container
     * @param params
     * @return
     */
    public static Handler<HttpServerRequest> getHandler(JsonObject route, Vertx vertx, Container container, Map<String, Object> params) {
        Handler<HttpServerRequest> h;
        if (_handlers == null) _handlers = new HashMap<>();
        if (_handlers.containsKey(route.getString(CONFIG_ROUTES_HANDLER))) {
            h = _handlers.get(route.getString(CONFIG_ROUTES_HANDLER));
        } else {
            h = createHandler(route.getString(CONFIG_ROUTES_HANDLER), vertx, container, params);
            if (h != null) {
                _handlers.put(route.getString(CONFIG_ROUTES_HANDLER), h);
            }
        }
        return h;
    }

    /**
     *
     * @param r
     * @param routeConfig
     * @param handler
     */
    public static void chainHandler(RouteMatcher r, JsonObject routeConfig, Handler<HttpServerRequest> handler) {
        String method = routeConfig.getString(CONFIG_ROUTES_METHOD);
        Boolean regex = routeConfig.getBoolean(CONFIG_ROUTES_REGEX);
        String path = routeConfig.getString(CONFIG_ROUTES_PATH);
        switch (method) {
            case "GET": {
                if (regex) {
                    r.getWithRegEx(path, handler);
                } else {
                    r.get(path, handler);
                }
                break;
            }
            case "POST": {
                if (regex) {
                    r.postWithRegEx(path, handler);
                } else {
                    r.post(path, handler);
                }
                break;
            }
            case "PUT": {
                if (regex) {
                    r.putWithRegEx(path, handler);
                } else {
                    r.put(path, handler);
                }
                break;
            }
            case "ALL": {
                if (regex) {
                    r.allWithRegEx(path, handler);
                } else {
                    r.all(path, handler);
                }
                break;
            }
            case "NONE": {
                r.noMatch(handler);
                break;
            }
            case "DELETE": {
                if (regex) {
                    r.deleteWithRegEx(path, handler);
                } else {
                    r.delete(path, handler);
                }
                break;
            }
            case "TRACE": {
                if (regex) {
                    r.traceWithRegEx(path, handler);
                } else {
                    r.trace(path, handler);
                }
                break;
            }
            case "OPTIONS": {
                if (regex) {
                    r.optionsWithRegEx(path, handler);
                } else {
                    r.options(path, handler);
                }
                break;
            }
            case "CONNECT": {
                if (regex) {
                    r.connectWithRegEx(path, handler);
                } else {
                    r.connect(path, handler);
                }
                break;
            }
            case "HEAD": {
                if (regex) {
                    r.headWithRegEx(path, handler);
                } else {
                    r.head(path, handler);
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     *
     * @param request
     * @param resourceUri
     */
    public static void serveResource(HttpServerRequest request, String resourceUri) {
        request.response()
                .setChunked(true)
                .setStatusCode(200)
                .sendFile(resourceUri);
    }

    /**
     *
     * @param request
     * @param redirectUri
     */
    public static void serveRedirect(HttpServerRequest request, String redirectUri) {
        request.response()
                .setChunked(true)
                .setStatusCode(302)
                .sendFile(redirectUri);
    }

    /**
     *
     * @param request
     * @param resourceUri
     * @param config
     */
    public static void serveSecureRedirect(HttpServerRequest request, String resourceUri, JsonObject config) {
        request.response()
                .setChunked(true)
                .setStatusCode(302)
                .putHeader("Location", config.getString("route"))
                .putHeader("Set-Cookie", cookieDough(config))
                .sendFile(resourceUri);
    }

    /**
     *
     * @param request
     * @param jsonResponse
     */
    public static void serveJson(HttpServerRequest request, JsonObject jsonResponse) {
        request.response()
            .setChunked(true)
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
        .end(jsonResponse.encode());
    }

    public static void serveErrorJson(HttpServerRequest request, JsonObject jsonError) {
        request.response()
            .setChunked(true)
            .setStatusCode(503)
            .putHeader("Content-Type", "application/json")
        .end(jsonError.encode());
    }

    /**
     *
     * @param request
     * @param jsonResponse
     */
    public static void serveSecureJson(HttpServerRequest request, JsonObject jsonResponse) {
        request.response()
                .setChunked(true)
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .putHeader("Set-Cookie", cookieDough(jsonResponse))
                .end(jsonResponse.encode());
    }

    private static String getRouteDomain(String route) {
        if (!route.contains("..")) {
            StringTokenizer p = new StringTokenizer((route.startsWith("/")) ? route.substring(1) : route, "/");
            if (p.countTokens() >= 1) return p.nextToken();
        }
        return "";
    }

    private static StringBuffer cookieDough(JsonObject jsonResponse) {
        StringBuffer authCookie = new StringBuffer()
                .append("session=").append(jsonResponse.getString("session")).append("&")
                .append("client=").append(jsonResponse.getString("client")).append("&")
                .append("hash=").append(jsonResponse.getString("hash")).append(";")
                .append("Path=/").append(getRouteDomain(jsonResponse.getString("route")));
        return authCookie;
    }

    private static JsonObject cookieCutter(String cookieDough) {
        JsonObject j = new JsonObject();
        StringTokenizer f = new StringTokenizer(cookieDough, ";");
        while (f.hasMoreTokens()) {
            String crumb = f.nextToken();
            StringTokenizer i = new StringTokenizer(crumb, "&");
            if (i.countTokens() == 1) {
                j = loadCookieTray(j, i.nextToken());
            } else if (i.countTokens() > 1){
                while(i.hasMoreTokens()) {
                    String p = i.nextToken();
                    j = loadCookieTray(j, p);
                }
            }
        }
        return j;
    }

    private static JsonObject loadCookieTray(JsonObject j, String p) {
        StringTokenizer e = new StringTokenizer(p, "=");
        while (e.hasMoreTokens()) {
            String k = e.nextToken();
            String v= "";
            if (e.hasMoreTokens()) v = e.nextToken();
            j.putString(k, v);
        }
        return j;
    }

    /**
     *
     * @param request
     * @param sessions
     * @return
     */
    public static boolean isSecure(HttpServerRequest request, Map<String, JsonObject> sessions) {
        JsonArray cookies = getCookies(request);
        Iterator<Object> ci = cookies.iterator();
        while(ci.hasNext()) {
            JsonObject cookie = (JsonObject) ci.next();
            String session = cookie.getString("session");
            String client = cookie.getString("client");
            String hash = cookie.getString("hash");
            if (session != null && sessions.containsKey(session)) {
                JsonObject ticket = sessions.get(session);
                if (ticket != null && session.equals(ticket.getString("session")) && client != null && client.equals(ticket.getString("client"))) {
                    if (hash != null && hash.equals(ticket.getString("hash")))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param request
     * @return
     */
    public static JsonArray getCookies(HttpServerRequest request) {
        JsonArray cookies = new JsonArray();
        List<String> browserCookies = request.headers().getAll("Cookie");
        Iterator<String> ci = browserCookies.iterator();
        while(ci.hasNext()) {
            cookies.addObject(cookieCutter(ci.next()));
        }
        return cookies;
    }

    /**
     *
     * @param ticket
     * @param sessions
     * @return
     */
    public static JsonObject expireSession(JsonObject ticket, Map<String, JsonObject> sessions) {
        return sessions.remove(ticket.getString("session"));
    }

    /**
     *
     * @param ticket
     * @param sessions
     * @return
     */
    public static JsonObject cacheSession(JsonObject ticket, Map<String, JsonObject> sessions) {
        sessions.put(ticket.getString("session"), ticket);
        return ticket;
    }

    /**
     *
     * @param request
     * @param logger
     * @param label
     */
    public static void traceRequest(final HttpServerRequest request, Logger logger, String label) {
        if (request != null & logger != null) {
            logger.info(label + ": request, method=" + request.method() + ", path=" + request.path() + ", query=" + request.query());
            if (!request.headers().isEmpty()) logger.info(label + ": request, headers=" + Helper.multiMapPrettyPrint(request.headers()));
            if (!request.params().isEmpty()) logger.info(label + ": request, params=" + Helper.multiMapPrettyPrint(request.params()));
        }
    }

    /**
     *
     * @param response
     * @param logger
     * @param label
     */
    public static void traceResponse(final HttpServerResponse response, Logger logger, String label) {
        if (response != null & logger != null) {
            logger.info(label + ": response, status=" + response.getStatusCode());
            if (!response.headers().isEmpty()) logger.info(label + ": response, headers=" + Helper.multiMapPrettyPrint(response.headers()));
        }
    }

}
