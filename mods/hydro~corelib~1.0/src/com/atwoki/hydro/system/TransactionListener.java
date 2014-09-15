package com.atwoki.hydro.system;

import com.atwoki.framework.postoffice.DefaultWorkListener;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 3/23/13
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransactionListener extends DefaultWorkListener {

    private TransactionCache _cache;

    public TransactionListener(TransactionCache c) {
        _cache = c;
    }

    @Override
    public void notified(Object source, Object tag) {
        Transaction t = (Transaction) source;
        switch (tag.toString()) {
            case "pending":
                break;
            case "running":
                _cache.register(t.correlation(), t.type());
                break;
            case "complete":
                _cache.complete(t.correlation());
                break;
            case "aborted":
                break;
            case "failed":
                _cache.abort(t.correlation());
                break;
            default:
                break;
        }
    }
}
