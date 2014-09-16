/**
 * Created with JetBrains WebStorm.
 * User: ezrak
 * Date: 2014/02/19
 * Time: 12:26 AM
 * Author: Ezra Kahimbaara (c) Atwoki.Com 2012-2014
 */

/*
 * Terminal
 */
var Terminal = {
    "client_id": null,
    "secret_id": null,
    "session_id": null,
    "profile_obj": null,
    "board_obj": [],
    "keys": {
        "ref_secret": 'hydro.js.terminal.secret',
        "ref_client": 'hydro.js.terminal.client',
        "ref_session": 'hydro.js.terminal.session',
        "ref_profile": 'hydro.js.terminal.profile',
        "ref_eburi": 'hydro.js.terminal.eburi',
        "ref_timeout": 'hydro.js.terminal.timeout',
        "ref_principal": 'hydro.js.terminal.principal',
        "ref_validity": 'hydro.js.terminal.validity'
    },
    "init": function() {
        var l = Helper.cacheResolve(this.keys.ref_client);
        if (!l) {
            Helper.cacheClear();
            this.client_id = Helper.uuid();
            l = this.client_id;
            Helper.cacheStore(this.keys.ref_client, l);
        }
        return l;
    },
    "load": function(session, secret, principal, expiry, eburi, validity) {
        Helper.cacheStore(this.keys.ref_session, session);
        Helper.cacheStore(this.keys.ref_secret, secret);
        Helper.cacheStore(this.keys.ref_principal, principal);
        Helper.cacheStore(this.keys.ref_timeout, expiry);
        Helper.cacheStore(this.keys.ref_eburi, eburi);
        Helper.cacheStore(this.keys.ref_validity, validity);
    },
    "client": function() {
        if (!this.client_id) {
            this.client_id = this.init();
        }
        return this.client_id;
    },
    "set": function(key, value) {
        Helper.cacheStore(key, value);
    },
    "property": function(propKey) {
        var v = Helper.cacheResolve(propKey);
        if (!v || v == null) {
            return '';
        }
        return v;
    },
    "session": function() {
        return this.property(this.keys.ref_session);
    },
    "principal": function() {
        return this.property(this.keys.ref_principal)
    },
    "secret": function() {
        return this.property(this.keys.ref_secret);
    },
    "eventbus": function() {
        return this.property(this.keys.ref_eburi);
    },
    "timeout": function() {
        return this.property(this.keys.ref_timeout);
    },
    "validity": function() {
        return this.property(this.keys.ref_validity);
    },
    "defaultMessageTTL": function() {
        return 5000;
    },
    "profile": function() {
        var t = this;
        // TODO - store profile in indexedDB
        return {
            "client": t.client(),
            "session": t.session(),
            "address": t.baseAddress(),
            "inbound": t.inboundAddress(),
            "secret": t.secret(),
            "notify": t.notifyAddress(),
            "bus": t.eventbus(),
            "expiry": t.timeout()
        };
    },
    "baseAddress": function() {
        return 'hydro.terminal.' + this.client() + '.' + this.session();
    },
    "notifyAddress": function() {
        // TODO - load from auth profile
        return 'hydro.auth.notify';
    },
    "inboundAddress": function() {
        return this.baseAddress() + '.inbound';
    },
    "post": function(msg, sender) {
        var p = {
            "sender": sender,
            "message": msg
        };
        return this.board_obj.push(p);
    },
    "posts": function () {
        return this.board_obj;
    },
    "secure": function() {
        return (this.secret() != "");
    }
};

/*
 * Broker
 */
var Broker = {
    "eb": null,
    "el": null,
    "isOnline": function() {
        return (this.status() == 'online');
    },
    "status": function () {
        var level = ['pending', 'online', 'closing', 'offline']; // [WebSocket.CONNECTING, ... ,WebSocket.CLOSED]
        if (this.eb == null) return level[3];
        else return level[this.eb.readyState()];
    },
    "listener": function(fnConnected, fnDisconnected) {
        return {
            "connected": function(uri) {
                fnConnected(uri);
            },
            "disconnected": function(uri) {
                fnDisconnected(uri);
            }
        };
    },
    "listen": function(l) {
        this.el = l;
    },
    "start": function(uri) {
        if (!this.eb) {
            this.eb = new vertx.EventBus(uri);
            var l = this.el;
            this.eb.onopen = function() {
                if (l != null) {
                    l.connected(uri);
                }
            };
            this.eb.onclose = function() {
                if (l != null) {
                    l.disconnected(uri);
                }
            };
        }
    },
    "stop": function() {
        if (this.eb) {
            this.eb.close();
            this.eb = null;
        }
    },
    "send": function(address, msg, handler) {
        if (this.eb) {
            if (handler != null) {
                this.eb.send(address, msg, handler);
            } else {
                this.eb.send(address, msg);
            }
        }
    },
    "subscribe": function(address, handler) {
        if (this.eb) {
            if (handler != null) {
                this.eb.registerHandler(address, handler);
            }
        }
    }
};

/*
 * Helper
 */
var Helper = {
    "transact": {
        "promise": "promise",
        "dispatch": "dispatch",
        "void": "void",
        "conversation": "conversation"
    },
    "action": {
        "send": "send",
        "execute": "execute",
        "callback": "callback",
        "notify": "notify"
    },
    "audit": function(message) {
        window.console.log('[' + Terminal.client() + '] ' + message);
    },
    "page": function(route) {
        window.open(route, "_top");
    },
    "route": function(route) {
         // not yet sure, perhaps one of 'window.location.replace(route);'
         // or 'window.location.href = route;'
         // or 'window.location.assign(route);'
         // or 'window.open(route, "_top");'
        window.location.replace(route);
    },
    "uuid": function () {

        var parts = new Array(4); // parts 'a' to 'd'

        // a - unix timestamp
        var time = Math.round(new Date().getTime() / 1000);
        parts[0] = time.toString(16).substring(0, 8);

        // b - browser
        var ua_string = window.navigator.userAgent;
        var match = ua_string.match(/\d+/g);
        if (!match) {
            throw 'Invalid browser version string';
        }
        var sum = 0;
        for (var i = 0, ii = match.length; i < ii; ++i) {
            sum += parseInt(match[i], 10);
        }
        parts[1] = (sum * sum * sum).toString(16).substring(0, 6);

        // c - url
        var href = window.location.href;
        parts[2] = (href.length * href.length * href.length).toString(16).substring(0, 4);

        // d - random
        parts[3] = Math.random().toString().substring(2);
        parts[3] = parseInt(parts[3], 10).toString(16).substring(0, 6);

        return parts.join('');
    },
    "localStorageSupported": function () {
        try {
            return 'localStorage' in window && window['localStorage'] !== null;
        } catch (e) {
            return false;
        }
    },
    "cacheStore": function(key,obj) {
        if (this.localStorageSupported()) {
            localStorage.setItem(key, obj);
        }
    },
    "cacheResolve": function(key) {
        var o;
        if (this.localStorageSupported()) {
            o = localStorage.getItem(key);
        }
        return o;
    },
    "cacheDelete": function(key) {
        if (this.localStorageSupported()) {
            localStorage.removeItem(key);
        }
    },
    "cacheClear": function() {
        if (this.localStorageSupported()) {
            localStorage.clear();
        }
    },
    "cookieDelete": function(name, domain) {
        document.cookie = name + '=;Path=' + domain + ';expires=Thu, 01-Jan-70 00:00:01 GMT;';
    }
};
