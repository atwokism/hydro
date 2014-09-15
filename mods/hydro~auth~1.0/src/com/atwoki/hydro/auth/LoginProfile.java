package com.atwoki.hydro.auth;

import com.atwoki.hydro.security.BCrypt;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2014/02/14
 * Time: 12:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoginProfile {

    private long _timerId, _validity, _tick;
    private String _session, _client, _token, _secret, _hash, _principal, _expiry, _sender;
    private HashMap<String, String> _properties;

    public LoginProfile(String client, String principal, String session, long validity, long timerId, String token, String password, String sender) {
        _client = client;
        _principal = principal;
        _session = session;
        _validity = validity;
        _timerId = timerId;
        _token = token;
        _sender = sender;

        _secret = BCrypt.hashpw(password, BCrypt.gensalt());
        _hash = com.atwoki.hydro.system.Helper.getID();
        _expiry = Helper.expiryTime((int) _validity);
        _properties = new HashMap<>();
        _tick = System.currentTimeMillis() + (_validity * 1000);
    }

    public String hash() {
        return _hash;
    }

    public String secret() {
        return _secret;
    }

    public long timer() {
        return _timerId;
    }

    public String client() {
        return _client;
    }

    public String principal() {
        return _principal;
    }

    public String session() {
        return _session;
    }

    public long validity() {
        return _validity;
    }

    public String expiry() {
        return _expiry;
    }

    public String sender() {
        return _sender;
    }

    public String authority(String password) {
        if (BCrypt.checkpw(password, _secret)) {
            return _token;
        }
        return null;
    }

    public String token(String hash) {
        if (hash.equals(hash())) {
            return _token;
        }
        return null;
    }

    public HashMap<String, String> properties() {
        return _properties;
    }

    public long timeToLive() {
        return _tick - System.currentTimeMillis();
    }
}
