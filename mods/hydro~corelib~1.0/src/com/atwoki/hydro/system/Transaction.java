package com.atwoki.hydro.system;

import org.vertx.java.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 3/23/13
 * Time: 2:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class Transaction {

    public static final String VOID = "void";
    public static final String DISPATCH = "dispatch";
    public static final String PROMISE = "promise";
    public static final String CONVERSATION = "conversation";

    public static final String [] STATES =
            {
                    "pending", // on creation of tx
                    "running", // on registration of tx
                    "complete",// on acknowledgement of tx
                    "aborted", // on abort/cancel of tx
                    "failed" // on error of tx
            };

    private TransactionListener _listener;
    private final String _txId = Helper.getID(), _txType, _cId;
    private final ArrayList<JsonObject> _txMovement = new ArrayList<>();
    private final HashMap<String, JsonObject> _txGraph = new HashMap<>();
    private int _state = 0;

    public Transaction() {
        _txType = DISPATCH;
        _cId = _txId;
        _state = 0;
        _txGraph.put(state(), new JsonObject());
        txNotify();
    }

    public Transaction(String correlationId) {
        _txType = PROMISE;
        _cId = correlationId;
        _state = 0;
        _txGraph.put(state(), new JsonObject());
        txNotify();
    }

    public Transaction(String txType, String correlationId) {
        _txType = txType;
        _cId = correlationId;
        _state = 0;
        _txGraph.put(state(), new JsonObject());
        txNotify();
    }

    public Transaction(String txType, String correlationId, int state, JsonObject node) {
        _txType = txType;
        _cId = correlationId;
        _state = state;
        _txGraph.put(state(), node);
        txNotify();
    }

    public void setListener(TransactionListener l) {
        _listener = l;
    }

    public void init(JsonObject object) {
        _state = 1;
        _txGraph.put(state(), object);
        txNotify();
    }

    public void ack(JsonObject object) {
        _state = 2;
        _txGraph.put(state(), object);
        txNotify();
    }

    public void term(JsonObject object) {
        _state = 3;
        _txGraph.put(state(), object);
        txNotify();
    }

    public void err(JsonObject object) {
        _state = 4;
        _txGraph.put(state(), object);
        txNotify();
    }

    public String type() {
        return _txType;
    }

    public String state() {
        return STATES[_state];
    }

    public int stateIndex() {
        return _state;
    }

    public String id() {
        return _txId;
    }

    public String correlation() {
        return _cId;
    }

    public ArrayList<JsonObject> movement() {
        return _txMovement;
    }

    public HashMap<String, JsonObject> graph() {
        return _txGraph;
    }

    private void snap() {
        JsonObject o = new JsonObject()
                .putString("state", state())
                .putString("id", id())
                .putString("cid", correlation())
                .putNumber("stamp", System.currentTimeMillis())
                .putNumber("index", movement().size())
                .putObject("object", graph().get(state()));
        _txMovement.add(o);
    }

    private void txNotify() {
        if (_listener != null) {
            _listener.notified(this, state());
        }
        snap();
    }
}
