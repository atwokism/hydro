package com.atwoki.hydro.eventpub;

import com.atwoki.framework.factory.GenericFactory;
import com.atwoki.framework.factory.Incident;
import com.atwoki.hydro.system.Helper;
import com.atwoki.hydro.system.Transaction;
import com.atwoki.hydro.system.TransactionCache;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import za.co.mc.common.framework.Publish;
import za.co.mc.common.genericEventPublisher.GenericEvent;
import za.co.mc.common.genericEventPublisher.PublishResponseMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 1/1/13
 * Time: 2:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class Publisher extends BusModBase {

    private int _callbackPort;
    private Factory _factory;
    private TransactionCache _cache;
    private Handler<Message<JsonObject>> _publishHandler;
    private Publish _publish;
    private String _publishAddress, _callbackRoute, _callbackHost, _authNotifyAddress;
    private Map<String, Object> _callbackInstances = new HashMap();
    private Map<String, String> _tokens = new HashMap();

    public void start() {
        super.start();
        logger.info("publish: starting, config=\n " + Helper.jsonPrettyPrint(config));

        _cache = new TransactionCache(container, vertx, config);
        _factory = new Factory(config.getObject("factory"));

        startAuthNotify();
        startCallbackGateway(config.getObject("callback"));
        startPublishGateway(config);
    }

    private void startAuthNotify() {
        _authNotifyAddress = config.getString("auth_address", "hydro.auth.notify");
        eb.registerHandler(_authNotifyAddress, new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject ticket = jsonObjectMessage.body();
                String h = ticket.getString("hash");
                switch(ticket.getString("action")) {
                    case "login":
                        cacheToken(ticket);
                        break;
                    case "logout":
                        _tokens.remove(h);
                        logger.info("publish: trashed token, hash=" + h);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void cacheToken(JsonObject ticket) {
        JsonObject req = new JsonObject();
        req.putString("session", ticket.getString("session"));
        req.putString("resource", "hydro~eventpub~1.0");
        eb.send("hydro.auth.authorise", req, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject ticket = message.body();
                String a = ticket.getString("assertions");
                String h = ticket.getString("hash");
                _tokens.put(h, a);
                logger.info("publish: cached token, hash=" + h);
            }
        });
    }

    private void startCallbackGateway(JsonObject cfg) {
        _callbackRoute = cfg.getString("route");
        _callbackHost = cfg.getString("host");
        _callbackPort = cfg.getNumber("port").intValue();

        JsonArray callbackPorts = cfg.getArray("agents");
        Iterator i = callbackPorts.iterator();
        while (i.hasNext()) {
            Number n = (Number) i.next();
            final JsonObject callbackCfg = cfg.copy();

            // change port add address and proxy port
            final String callbackAddress = cfg.getString("address_prefix") + "." + n.toString();
            callbackCfg.putString("address", callbackAddress);
            callbackCfg.putNumber("port", n.intValue());
            callbackCfg.putNumber("proxy", _callbackPort);
            callbackCfg.putString("agent_id", GenericFactory.makeUID());

            // deploy callback agent
            container.deployVerticle(CallbackAgent.class.getName(), callbackCfg, 1, new Handler<AsyncResult<String>>() {
                @Override
                public void handle(AsyncResult<String> asyncResult) {
                    logger.info("publish: callback agent deployment, status=" + asyncResult.result() + ", id=" + callbackCfg.getString("agent_id"));
                    // register event bus on callback address to dispatch
                    eb.registerHandler(callbackCfg.getString("address"), new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            deliverCallback(message, callbackCfg);
                        }
                    });
                    logger.info("publish: registered callback handler, address=" + callbackCfg.getString("address"));
                    eb.registerHandler(callbackCfg.getString("address") + ".buffer", new Handler<Message<Buffer>>() {
                        @Override
                        public void handle(Message<Buffer> bufferMessage) {
                            logger.info("received buffer (" + callbackCfg.getString("agent_id") + "):\n" + bufferMessage.body());
                        }
                    });
                    logger.info("publish: registered buffer handler, address=" + callbackCfg.getString("address"));
                }
            });
            // register
            _callbackInstances.put(n.toString(), callbackCfg);
            logger.info("publish: service proxy, status=registered, location=[" + _callbackHost + ":" + n.toString() + "]" + _callbackRoute + ", config=\n" + Helper.jsonPrettyPrint(callbackCfg));
        }
        logger.info("publish: callback gateway started ..");
    }

    private void deliverCallback(Message<JsonObject> message, JsonObject callbackCfg) {
        logger.info("publish: received callback, agent=" + callbackCfg.getString("agent_id") + ", sender=" + message.replyAddress());
        JsonObject callback = message.body();
        logger.info("publish: received callback, message=" + Helper.jsonPrettyPrint(callback));
        String cid = callback.getObject("Header").getObject("EventMetaData").getObject("Process").getString("CorrelationID");
        String terminal = callback.getObject("Header").getObject("EventMetaData").getObject("Event").getString("Source");
        String id = callback.getObject("Header").getObject("EventMetaData").getObject("Event").getString("UniqueSourceId");
        _cache.fulfill(cid, callback);
        // TODO - transform into HydroMessage JsonObject and send to terminal, mongo and tx cache
        message.reply(
                new JsonObject()
                .putString("id", id)
                .putString("cid",cid)
                .putString("terminal", terminal)
                .putString("result", "ok"));
    }

    private void startPublishGateway(final JsonObject publishCfg) {
        _publishAddress = publishCfg.getString("address");
        _publish = new Publish();
        _publishHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject o = jsonObjectMessage.body();
                String transact = o.getString("transact");
                logger.debug("publish: received, sender=" + jsonObjectMessage.replyAddress() + ", type=" + transact);
                switch (transact) {
                    case Transaction.DISPATCH:
                        break;
                    case Transaction.PROMISE:
                        doPublishWithCallback(jsonObjectMessage, publishCfg.getObject("callback"));
                        break;
                    case Transaction.CONVERSATION:
                        break;
                    default:
                        sendError(jsonObjectMessage, "publish: failed, desc=invalid transact type, transact=" + transact);
                        break;
                }
            }
        };
        eb.registerHandler(_publishAddress, _publishHandler);
        logger.info("publish: gateway started ..");
    }

    private JsonObject doPublishAndWait(Message<JsonObject> jsonObjectMessage) {
        return new JsonObject();
    }

    private void doPublishWithCallback(Message<JsonObject> jsonObjectMessage, JsonObject callbackCfg) {
        GenericEvent eventObj = null;
        PublishResponseMessage msg = null;

        JsonObject publishCmd = jsonObjectMessage.body();
        JsonObject cbProps = callbackProperties(callbackCfg);

        if (!validCommand(jsonObjectMessage)) return;

        String id = publishCmd.getString("id");
        String correlation = publishCmd.getString("correlation", id);
        String source = publishCmd.getString("source");

        Number validity = publishCmd.getNumber("timeout");
        String hash = publishCmd.getString("credentials");
        String token = _tokens.get(hash);

        JsonObject event = publishCmd.getObject("data");
        String evtName = event.getString("name");
        String evtVersion = event.getString("version", "v1");
        String evtProcess = event.getString("process");
        String evtPriority = event.getString("priority", "Normal");
        String evtPayload = event.getString("payload");

        try {
            JsonObject piProps = new JsonObject().putString(correlation, source);
            eventObj = _factory.createEvent(correlation, evtName, source, id, evtProcess, evtPayload, token, piProps.toMap(), cbProps.toMap(), evtPriority, evtVersion, validity);
        } catch (Incident x) {
            logger.error("publish: error, desc=could not create event message, details=" + x);
            sendError(jsonObjectMessage, "publish: error, desc=could not create event, msg=" + _factory.marshallException(source, x).encode());
            return;
        }

        String endpoint = config.getString("endpoint");
        try {
            logger.info("publishing: event=" + evtName + ", endpoint=" + endpoint + "\n ->" + publishCmd);
            msg = _publish.publishGenericEvent(eventObj, endpoint);
            logger.info("publish: response,  id=" + msg.getResponseId() + ", msg=" + msg.getResponseMessage());
        } catch (Exception x) {
            logger.error("publish: error, desc=could not publish event, details=" + x);
            Incident i = new Incident(source, "publish: error, desc=publish failed", x);
            sendError(jsonObjectMessage, "publish: error, desc=could not publish event, msg=" + _factory.marshallException(source, i).encode());
            return;
        }

        if (msg != null && (msg.getResponseId() == null || msg.getResponseId().equals("error"))) {
            logger.error("publish: error, desc=could not create event, details=" + msg.getResponseMessage());
            sendError(jsonObjectMessage, "publish: failed, desc=publish response invalid, msg=" + _factory.marshallError(source, msg.getResponseMessage()).encode());
            return;
        }

        JsonObject reply = new JsonObject()
            .putString("ack", msg.getResponseId())
            .putString("correlation", correlation)
            .putString("message", msg.getResponseMessage());

        JsonObject r = new JsonObject();
        r.putObject("response", reply);

        _cache.promise(correlation, new JsonObject().putObject("request", publishCmd).putObject("ack", reply));
        sendOK(jsonObjectMessage, r);
    }

    private boolean validCommand(Message<JsonObject> jsonObjectMessage) {

        JsonObject command = jsonObjectMessage.body();

        mandatoryValue(jsonObjectMessage, "id", command.getString("id"));
        String id = command.getString("id"); // mandatory
        if (id == null || id.isEmpty()) return false;

        mandatoryValue(jsonObjectMessage, "source", command.getString("source"));
        String source = command.getString("source"); // mandatory
        if (source == null || source.isEmpty()) return false;

        mandatoryValue(jsonObjectMessage, "credentials", command.getString("credentials"));
        String hash = command.getString("credentials"); // mandatory
        if (hash == null || hash.isEmpty()) return false;

        mandatoryValue(jsonObjectMessage, "data", command.getObject("data"));
        JsonObject event = command.getObject("data"); // mandatory
        if (source == null) return false;

        mandatoryValue(jsonObjectMessage, "name", command.getString("name"));
        String evtName = event.getString("name"); // mandatory
        if (evtName == null || evtName.isEmpty()) return false;

        mandatoryValue(jsonObjectMessage, "process", command.getString("process"));
        String evtProcess = event.getString("process"); // mandatory
        if (evtProcess == null || evtProcess.isEmpty()) return false;

        mandatoryValue(jsonObjectMessage, "payload", command.getString("payload"));
        String evtPayload = event.getString("payload"); // mandatory
        if (evtPayload == null || evtPayload.isEmpty()) return false;

        return true;
    }

    private JsonObject callbackProperties(JsonObject callbackCfg) {
        return new JsonObject()
            .putString(
                    "callback_endpoint",
                    "http://" + callbackCfg.getString("host") + ":" + callbackCfg.getNumber("port") + callbackCfg.getString("route"));
    }

    private Object mandatoryValue(Message<JsonObject> jsonObjectMessage, String label, Object o) {
        if (o == null) {
            sendError(jsonObjectMessage, "publish: error, desc=field '" + label + "' must be present or not null");
        }
        return o;
    }

    public void stop() {
        eb.unregisterHandler(_publishAddress, _publishHandler);
        try {
            super.stop();
            logger.info("publish: stopped");
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
