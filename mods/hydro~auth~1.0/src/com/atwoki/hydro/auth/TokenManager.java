package com.atwoki.hydro.auth;

import com.atwoki.framework.facade.Login;
import com.atwoki.framework.factory.GenericFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import za.co.mc.common.authenticate.TokenContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 3/26/13
 * Time: 9:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class TokenManager extends BusModBase {

    private Handler<Message<JsonObject>> _loginHandler;
    private Handler<Message<JsonObject>> _logoutHandler;
    private Handler<Message<JsonObject>> _authoriseHandler;
    private Handler<Message<JsonObject>> _sessionHandler;

    protected final Map<String, LoginProfile> _logins = new HashMap<>();
    protected final Map<String, String> _sessions = new HashMap<>();

    private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

    private Login _login;
    private String _authAddress, _authEndpoint, _keyStorePass, _runDir;
    private long _sessionTimeout;
    private JsonObject _ssl;

    public void start() {
        super.start();

        _authAddress = config.getString("address", "hydro.auth");
        _authEndpoint  = config.getString("endpoint");
        _sessionTimeout = getTimeout(config.getNumber("timeout"));
        _ssl = config.getObject("ssl");

        _runDir = container.env().get("MOD_RUN_DIR") + "/";

        _loginHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doLogin(message);
            }
        };
        eb.registerHandler(_authAddress + ".login", _loginHandler);

        _logoutHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doLogout(message);
            }
        };
        eb.registerHandler(_authAddress + ".logout", _logoutHandler);

        _authoriseHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                doAuthorise(message);
            }
        };
        eb.registerHandler(_authAddress + ".authorise", _authoriseHandler);

        _sessionHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                doSessionInfo(message);
            }
        };
        eb.registerHandler(_authAddress + ".session", _sessionHandler);

       /*
        if (_ssl != null) {
            _keyStorePass = _ssl.getString("keystore_password");
            _ssl.putString("keystore_password", "********");
        }
        */

        _login = new Login();
        boolean started = false;

        if (_ssl != null) {

            if (vertx.fileSystem().existsSync(_ssl.getString("keystore_path"))) {
                System.setProperty("javax.net.ssl.trustStore", _ssl.getString("keystore_path"));
                System.setProperty("javax.net.ssl.trustStorePassword", _ssl.getString("keystore_password"));
                System.setProperty("javax.net.ssl.trustStoreType", "JKS");
                logger.info("token manager: keystore, path=" + new File(_ssl.getString("keystore_path")).getAbsolutePath());
            } else {
                logger.info("token manager: keystore not found, keystore=" + _ssl.getString("keystore_path"));
            }

            /*
            String path = _ssl.getString("keystore_path");
            String type = _ssl.getString("keystore_type");
            String pass = _ssl.getString("keystore_password");
            if (vertx.fileSystem().existsSync(path)) try {
                _login.setSSL(type, path, pass);
                logger.info("token manager: keystore loaded, keystore=" + path);
                started = true;
            } catch (Exception e) {
                logger.error("token manager: error loading keystore, desc=" + e);
            }
            else {
                logger.info("token manager: keystore not found, keystore=" + path);
            }
            */

            logger.info("token manager: started, config=\n" + com.atwoki.hydro.system.Helper.jsonPrettyPrint(config));
        }

        /*
        if (started) {
        } else {
            logger.info("token manager: failed, config=\n" + com.atwoki.hydro.system.Helper.jsonPrettyPrint(config));
        }
        */
    }

    private void doSessionInfo(Message<JsonObject> message) {

        final String session = getMandatoryString("session", message);
        if (session == null) return;

        final String client = getMandatoryString("client", message);
        if (client == null) return;

        if (_sessions.containsKey(session)) {
            String user = _sessions.get(session);
            LoginProfile l = _logins.get(user);
            if (l.client().equals(client)) {
                JsonObject reply = Helper.authTicket(l, "session");
                sendOK(message, reply);
            } else {
                sendError(message, "token manager: terminal match failed, term=" + client + ", client=" + l.client());
            }
        } else {
            sendError(message, "token manager: not logged in");
        }
    }

    private long getTimeout(Number timeout) {
        if (timeout != null) {
            if (timeout instanceof Long) {
                return (Long)timeout;
            } else if (timeout instanceof Integer) {
                return (Integer)timeout;
            }
        }
        return DEFAULT_SESSION_TIMEOUT;
    }


    private void doLogin(final Message<JsonObject> message) {

        logger.info("token manager: login request, msg=" + message.body());

        final String domain = getMandatoryString("domain", message);
        if (domain == null) return;

        final String username = getMandatoryString("username", message);
        if (username == null) return;

        String password = getMandatoryString("password", message);
        if (password == null) return;

        String client = getMandatoryString("client", message);
        if (client == null) return;

        String sender = getMandatoryString("sender", message);
        if (sender == null) return;

        // Check if already logged in, if so logout of the old session
        LoginProfile p = _logins.get(username);
        if (p != null) {
            logout(p.session());
        }

        TokenContext ctx = _login.authenticate(domain, username, password, _authEndpoint); // web service call

        if (ctx != null) { // user found

            String principal = domain + "\\" + username;
            String token = ctx.getToken().getValue();
            long validity = ctx.getExpiryPeriod().longValue();
            if (validity < 0) {
                validity = _sessionTimeout;
            }
            final String session = GenericFactory.makeUID();

            long timerId = vertx.setTimer(validity * 1000, new Handler<Long>() {
                public void handle(Long timerID) {
                    LoginProfile p = _logins.get(username);
                    if (p != null && logout(session)) {
                        JsonObject authTicket = Helper.authTicket(p, "logout");
                        notifyAuth(authTicket);
                        logger.info("token manager: session timed out, user=" + p.principal() + ", client=" + p.client());
                    }
                }
            });

            LoginProfile profile = new LoginProfile(client, principal, session, validity, timerId, token, password, sender);

            // TODO - add properties to profile

            _logins.put(username, profile);
            _sessions.put(session, username);

            JsonObject jsonReply = Helper.authTicket(profile, "login");
            sendOK(message, jsonReply); // status=ok
            notifyAuth(jsonReply);

        } else { // user not found
            sendStatus("denied", message); // status=denied
        }
    }

    protected void doLogout(final Message<JsonObject> message) {
        logger.info("token manager: logging out, msg=" + message.body());
        final String sessionId = getMandatoryString("session", message);
        if (sessionId != null) {
            String username = _sessions.get(sessionId);
            LoginProfile p = _logins.get(username);
            if (p != null && logout(sessionId)) {
                JsonObject jsonReply = new JsonObject().putString("session", sessionId);
                sendOK(message, jsonReply); // status=ok
                JsonObject authTicket = Helper.authTicket(p, "logout");
                notifyAuth(authTicket);
                logger.info("token manager: log out successful, terminal=" + p.client() + ", secret=" + p.secret());
            } else {
                super.sendError(message, "token manager: not logged in"); // status=error
            }
        }
    }

    protected boolean logout(String sessionId) {
        String username = _sessions.get(sessionId);
        LoginProfile p = _logins.get(username);
        if (p != null) {
            _sessions.remove(p.session());
            _logins.remove(p.principal());
            vertx.cancelTimer(p.timer());
            return true;
        } else {
            return false;
        }
    }

    protected void doAuthorise(Message<JsonObject> message) {
        String sessionId = getMandatoryString("session", message);
        if (sessionId == null) {
            return;
        }
        String resource = getMandatoryString("resource", message);
        if (resource == null) {
            return;
        }
        LoginProfile p = isAuthorised(sessionId, resource);
        if (p != null) {
            JsonObject reply = Helper.authTicket(p, "authorise");
            reply.putString("assertions", p.token(p.hash()));
            reply.putString("resource", resource);
            sendOK(message, reply);
        } else {
            sendStatus("denied", message);
        }
    }

    private LoginProfile isAuthorised(String sessionId, String resource) {
        String user = _sessions.get(sessionId);
        LoginProfile p = _logins.get(user);
        if (p != null && p.timeToLive() > 0) {
            logger.info("token manager: authorising, principal=" + p.principal() + ", resource=" + resource);
            return p;
        }
        return null;
    }

    private void notifyAuth(JsonObject authTicket) {
        eb.publish(_authAddress + ".notify", authTicket);
    }

    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
            logger.error(e);
        }
    }

}
