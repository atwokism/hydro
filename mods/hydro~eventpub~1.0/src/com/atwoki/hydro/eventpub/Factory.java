package com.atwoki.hydro.eventpub;

import com.atwoki.framework.factory.GenericFactory;
import com.atwoki.framework.factory.Incident;
import org.vertx.java.core.json.JsonObject;
import za.co.mc.common.framework.EventFactory;
import za.co.mc.common.genericEventPublisher.*;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 3/25/13
 * Time: 12:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class Factory {

    public final static int EVENT_DEFAULT_VALIDITY = 86400;
    public final static String EVENT_DEFAULT_PRIORITY = "normal";
    public final static String EVENT_DEFAULT_VERSION = "v1";

    private JsonObject _config;

    public Factory(JsonObject cfg) {
        _config = cfg;
    }

    public JsonObject marshallException(String source, Throwable x) {
        return marshallError(source, x.getMessage() + "->" + x.toString());
    }

    public JsonObject marshallError(String source, String message) {
        return new JsonObject()
                .putBoolean("error", true)
                .putString("message", message)
                .putString("source", source);
    }

    public GenericEvent createDefaultEvent(String name, String source, String processName, String payloadData, String secureToken, Map<String, Object> processProps, Map<String, Object> paramProps) throws Incident {
        String priority = (_config.getString("priority") != null) ? _config.getString("priority") : EVENT_DEFAULT_PRIORITY;
        String version = (_config.getString("version") != null) ? _config.getString("version") : EVENT_DEFAULT_VERSION;
        Number validity = (_config.getNumber("valid_until") != null) ? _config.getNumber("valid_until") : EVENT_DEFAULT_VALIDITY;
        return createEvent(name, source, processName, payloadData, secureToken, processProps, paramProps, priority, version, validity);
    }

    public GenericEvent createEvent(String name, String source, String processName, String payloadData, String secureToken, Map<String, Object> processProps, Map<String, Object> paramProps, String priority, String version, Number validity)
    throws Incident {
        GenericEvent instance;
        try {
            Object payload = EventFactory.createPayload(payloadData);
            SecurityContext security = EventFactory.createSecurityContext(secureToken);
            EventContext event = EventFactory.createEventContext(source, priority, version, validity.intValue());
            ProcessContext process = EventFactory.createProcessContext(processName, GenericFactory.makeUID(), version, processProps);
            ContextParametersList params = EventFactory.createContextParametersList(paramProps);
            EventMetaData metadata = EventFactory.createEventMetadata(process, event, version, params);
            Header header = EventFactory.createHeader(metadata, security);
            instance = EventFactory.createEvent(name, header, payload);
        } catch (Exception x) {
            throw new Incident("factory", "could not create event", x);
        }
        return instance;
    }

    public GenericEvent createEvent(String correlationId, String name, String source, String sourceId, String processName, String payloadData, String secureToken, Map<String, Object> processProps, Map<String, Object> paramProps, String priority, String version, Number validity)
    throws Incident {
        GenericEvent instance;
        try {
            Object payload = EventFactory.createPayload(payloadData);
            SecurityContext security = EventFactory.createSecurityContext(secureToken);
            EventContext event = EventFactory.createEventContext(source, priority, version, validity.intValue());
            event.setUniqueSourceId(sourceId);
            ProcessContext process = EventFactory.createProcessContext(processName, correlationId, version, processProps);
            ContextParametersList params = EventFactory.createContextParametersList(paramProps);
            EventMetaData metadata = EventFactory.createEventMetadata(process, event, version, params);
            Header header = EventFactory.createHeader(metadata, security);
            instance = EventFactory.createEvent(name, header, payload);
        } catch (Exception x) {
            throw new Incident("factory", "could not create event", x);
        }
        return instance;
    }
}
