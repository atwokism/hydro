package com.atwoki.hydro.system;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 3/23/13
 * Time: 2:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransactionCache {

    private Vertx _v;
    private HashMap<String, Transaction> _completed, _registry, _errors;
    private TransactionListener _listener = new TransactionListener(this);
    private JsonObject _cfg;
    private Logger _l;
    private Container _c;

    public TransactionCache(Container c, Vertx v, JsonObject config) {
        _v = v;
        _c = c;
        _cfg = config;
        init();
    }

    public String name() {
        return _cfg.getString("tx_cache");
    }

    public void promise(String id, JsonObject data) {
        JsonObject tx = new JsonObject();
        tx.putObject("data", data);
        tx.putString("correlation", id);
        Transaction t = new Transaction(Transaction.PROMISE, id);
        t.setListener(listener());
        t.init(tx);
    }

    public void fulfill(String id, JsonObject data) {
        JsonObject tx = new JsonObject();
        tx.putObject("promise", data);
        tx.putString("data", id);
        Transaction t = transaction(id);
        t.setListener(listener());
        t.ack(tx);
    }

    public boolean test(String id) {
        return _v.sharedData().getMap(_cfg.getString("tx_cache")).containsKey(id);
    }

    protected Transaction register(String id, String txType) {
        Transaction t = new Transaction(txType, id);
        t.init(new JsonObject());
        JsonObject jsonTx = TransactionFactory.marshall(t);  // always marshall AFTER setting the correct tx state
        Buffer bufferTx = new Buffer(jsonTx.toString());
        _v.sharedData().getMap(_cfg.getString("tx_cache")).putIfAbsent(t.correlation(), bufferTx);
        _registry.put(t.correlation(),t);
        _l.trace("tx_cache: tx registered, " + logTx(t));
        return t;
    }

    protected Transaction complete(String id) {
        Transaction t = transaction(id);
        if (!t.type().equals(Transaction.VOID)) {
            _completed.put(t.correlation(), t);
            _registry.remove(t.correlation());
            _l.trace("tx_cache: tx fulfilled, " + logTx(t));
        }
        return t;
    }

    protected Transaction abort(String id) {
        Transaction t = transaction(id);
        if (!t.type().equals(Transaction.VOID)) {
            _errors.put(t.correlation(), t);
            _registry.remove(t.correlation());
            _l.trace("tx_cache: tx aborted, " + logTx(t));
        }
        return t;
    }

    protected TransactionListener listener() {
        return _listener;
    }

    protected Transaction transaction(String correlation) {
        Transaction t;
        if (_v.sharedData().getMap(_cfg.getString("tx_cache")).containsKey(correlation)) {
            Buffer bufferTx = (Buffer) _v.sharedData().getMap(_cfg.getString("tx_cache")).get(correlation);
            JsonObject jsonTx = new JsonObject(bufferTx.toString());
            t = TransactionFactory.unmarshall(jsonTx);
        } else {
            t = new Transaction(Transaction.VOID, correlation);
            t.setListener(_listener);
            JsonObject e = new JsonObject();
            e.putString("tx", "no corresponding transaction found in cache");
            e.putString("id", correlation);
            _errors.put(t.correlation(), t);
        }
        _l.trace("tx_cache: cached accessed, " + logTx(t));
        return t;
    }

    private void init() {
        _completed = new HashMap<>();
        _registry = new HashMap<>();
        _errors = new HashMap<>();
        _l = _c.logger();
        _l.debug("tx_cache: started, name=" + name());
    }

    private String logTx(Transaction t) {
        return
            new StringBuffer()
                .append("type=")
                .append(t.type())
                .append(", id=")
                .append(t.correlation())
                .append(", state=")
                .append(t.state())
                .append(", size=")
                .append(t.graph().size())
            .toString();
    }
}