package com.atwoki.hydro.system;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: ezrak
 * Date: 2014/04/19
 * Time: 1:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class TransactionFactory {

    protected static JsonObject marshall(Transaction t) {
        JsonObject json = null;
        if (t != null) {
            JsonObject graph = new JsonObject();// TODO - (Map) t.graph());
            JsonArray movement = new JsonArray();//TODO - (List) t.movement());
            json = new JsonObject()
                    .putString("type", t.type())
                    .putNumber("state_index", t.stateIndex())
                    .putString("id", t.id())
                    .putString("correlation", t.correlation())
                    .putObject("graph", graph)
                    .putArray("movement", movement);
        }
        return json;
    }

    protected static Transaction unmarshall(JsonObject json) {
        Transaction t = null;
        if (json != null) {
            String type = json.getString("type");
            String correlation = json.getString("correlation");
            int state = json.getInteger("state_index");
            t = new Transaction(type, correlation, state, json.getObject("graph"));
            // TODO - hydration of movement list
        }
        return t;
    }
}
