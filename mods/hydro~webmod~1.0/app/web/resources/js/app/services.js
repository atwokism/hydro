/**
 * Created with JetBrains WebStorm.
 * User: atwoki
 * Date: 2013/10/28
 * Time: 11:40 PM
 * To change this template use File | Settings | File Templates.
 */
angular

    .module(
        'hydro.services',
        [],
        function() {
            // config
        }
    )

    .factory('authFactory', function($http) {

        var factory = {};
        var term = Terminal;

        term.init();

        factory.signin = function (credentials, posturi, target) {

            var message = {
                "action": 'login',
                "domain": credentials.domain,
                "username": credentials.user,
                "password": credentials.password,
                "client": term.client(),
                "resource": target
            };

            Helper.audit("auth request: login, message=" + JSON.stringify(message));

            $http.post(posturi, message)
                .success(function (data, status, headers, config) {
                    if (data.status == "ok") {
                        term.load(data.session, data.hash, data.principal, data.expiry, data.eb_uri, data.validity);
                        Helper.audit("auth success: login, profile=" + JSON.stringify(term.profile()));
                        // Helper.route(data.route);
                        Helper.page(data.route);
                    } else if (data.status == "denied") {
                        Helper.audit("auth denied: login, principal=" + credentials.domain + '\\' + credentials.user);
                        alert('Authentication unsuccessful');
                    } else if (data.status == "error") {
                        Helper.audit("auth error: login, desc=" + data.message + ", status=" + status);
                    }
                })
                .error(function (data, status, headers, config) {
                    Helper.audit("auth error: login, desc=" + data + ", status=" + status);
                });
        };

        factory.signout = function (posturi) {

            var message = {
                "action": 'logout',
                "session": term.session(),
                "client": term.client(),
                "secret": term.secret(),
                "resource": "/"
            };

            Helper.audit("auth request: logout, message=" + JSON.stringify(message));

            $http.post(posturi, message)
                .success(function (data, status, headers, config) {
                    if (data.status == "ok") {
                        Helper.audit("auth success: logout, session=" + data.session);
                    } else {
                        Helper.audit("auth error: logout, msg=" + JSON.stringify(data) + ", status=" + status);
                    }
                    // by hook or by crook or by design or by mistake we are throwing you out!!!
                    Helper.cacheClear();
                    Helper.cookieDelete("session", "/pages");
                    Helper.route(data.route);
                })
                .error(function (data, status, headers, config) {
                    Helper.audit("auth error: logout, msg=" + JSON.stringify(data) + ", status=" + status);
                    Helper.route(data.route);
                });
        };

        return factory;
    })

    .factory('termFactory', function() {

        var factory = {};
        var term = Terminal;

        window.onbeforeunload = function(e) {
            if (term.session() == "" && e.srcElement.URL == "http://localhost:24240/") {
                // return 'Already logged out';
                // window.open(e.srcElement.URL, "_top");
            }
        };

        window.onload = function(e) {
            if (e.srcElement.URL == "http://localhost:24240/") {
                //Helper.cacheClear();
                //term.init();
            }
        }

        if (!term.secure()) Helper.cacheClear();
        term.init();

        factory.metadata = function(transaction) {
            return {
                "endpoint": this.getproperty('publish_url'),
                "transact": transaction,
                "source": this.profile().address,
                "validity": this.validFor()
            };
        };

        factory.envelope = function(data, address, action, transact) {
            var i = this.seed();
            return {
                "id": i,
                "address": address,
                "action": action,
                "transact": transact,
                "timeout": term.defaultMessageTTL(),
                "source": term.baseAddress(),
                "correlation": i,
                "credentials": term.secret(),
                "data": data
            };
        };

        factory.envelope = function(data, address, action, transact, correlation) {
            return {
                "id": this.seed(),
                "address": address,
                "action": action,
                "transact": transact,
                "timeout": term.defaultMessageTTL(),
                "source": term.baseAddress(),
                "correlation": correlation,
                "credentials": term.secret(),
                "data": data
            };
        };

        factory.terminal = function() {
            return term.client();
        };

        factory.expiry = function() {
            return term.timeout();
        };

        factory.identity = function() {
            return term.principal();
        };

        factory.profile = function() {
            return term.profile();
        };

        factory.session = function() {
            return term.session();
        };

        factory.destination = function(route) {
            Helper.route(route);
        };

        factory.getproperty = function(key) {
            return term.property(key);
        };

        factory.setproperty = function(key, value) {
            return term.set(key, value);
        };

        factory.seed = function() {
            return Helper.uuid();
        };

        factory.post = function(sender, msg) {
            term.post(msg, sender);
        };

        factory.messages = function () {
            return term.posts();
        };

        factory.log = function(msg, level) {
            Helper.audit(msg);
        };

        factory.validFor = function() {
            return term.validity();
        };

        return factory;
    })

    .factory('messageFactory', function(termFactory) {

        var factory = {};
        var b = Broker;
        var profile = termFactory.profile();

        factory.metadata = function() {
            return {
                "endpoint": termFactory.getproperty('publish_url'),
                "transact": 'promise',
                "source": profile.address,
                "validity": termFactory.validFor()
            };
        };

        factory.event = function(event) {
            return {
                "eventname": event.name,
                "data": event.payload,
                "processname": event.process,
                "version": event.version,
                "correlation": event.correlation
            };
        };

        factory.touch = function() {
            if (!b.isOnline()) {
                var ih = function(msg, replyTo) {
                    // TODO - do something with received msg
                    termFactory.log("message factory: received, message=" + JSON.stringify(msg) + ", from=" + replyTo);
                };
                var lh = function(msg, replyTo) {
                    alert("Logging you out!");
                    termFactory.destination(msg.route);
                };
                var l = b.listener(
                    function(uri) {
                        b.subscribe(profile.inbound, ih);  // for terminal control
                        b.subscribe(profile.notify, lh); // for auth control
                        termFactory.log("message factory: event bus connected, uri=" + uri);
                    },
                    function(uri) {
                        termFactory.log("message factory: event bus disconnected, uri=" + uri);
                    }
                );
                b.listen(l);
                b.start(profile.bus);
            }
        };

        factory.send = function(message, handler, correlation) {

            var address = 'hydro.event.publish';
            termFactory.log("event bus: publish, address=" + address);

            if (!b.isOnline()) {
                termFactory.log("event bus: offline, status=" + b.status());
            } else {
                termFactory.log("event bus: send, msg=" + JSON.stringify(message) + ", address=" + address);
                b.send(address, message, function (msg, replyTo) {
                    termFactory.log("event bus: received, msg: " + JSON.stringify(msg) + ", replyto=" + replyTo);
                    handler.inbound({
                        "message": msg,
                        "address" : replyTo
                    });
                });
            }
        };

        factory.subscribe = function(address, handler, correlation) {
            termFactory.log("event bus: subscribe [todo], address=" + address);
        };

        factory.broadcast = function(address, message) {
            termFactory.log("event bus: broadcast [todo], message=" + JSON.stringify(message) + ", address=" + address);
        };

        factory.status = function() {
            return b.status();
        };

        return factory;
    })

    .factory('apiRESTFactory', function ($http, termFactory) {

        var factory = {};
        var profile = termFactory.profile();

        factory.submit = function(posturi, message, onapisuccess, onapierror) {
            $http.post(posturi, message)
                .success(function (data, status, headers, config) {
                    onapisuccess(data, status, headers, config);
                })
                .error(function (data, status, headers, config) {
                    onapierror(data, status, headers, config);
                });
        };

        factory.submitWithTimeout = function(posturi, message, onapisuccess, onapierror, ontimeout) {
            $http.post(posturi, message)
                .success(function (data, status, headers, config) {
                    if (data.status == 'fail' && data.fail_type == 'TIMEOUT') {
                        ontimeout(data, status, headers, config);
                    } else {
                        onapisuccess(data, status, headers, config);
                    }
                })
                .error(function (data, status, headers, config) {
                    onapierror(data, status, headers, config);
                });
        };

        factory.query = function(queryuri, message, onapisuccess, onapierror) {
            $http.get(queryuri, message)
                .success(function (data, status, headers, config) {
                    onapisuccess(data, status, headers, config);
                })
                .error(function (data, status, headers, config) {
                    onapierror(data, status, headers, config);
                });
        };

        factory.execute = function(apiuri, id, address, data, onsuccess, onerror) {
            var p = {
                "id": id,
                "address": address,
                "action": 'execute',
                "data": data
            };
            this.submit(apiuri, p, onsuccess, onerror);
        };

        factory.executeWithTimeout = function(apiuri, id, address, data, timeout, onsuccess, onerror, ontimeout) {
            var p = {
                "id": id,
                "address": address,
                "action": 'execute',
                "timeout": Number(timeout),
                "data": data
            };

            var p1 = termFactory.envelope(data, address, Helper.action.execute, Helper.transact.promise);

            this.submitWithTimeout(apiuri, p1, onsuccess, onerror, ontimeout);
        };

        factory.testWithTimeout = function(envelope, apiuri, onsuccess, onerror, ontimeout) {
            this.submitWithTimeout(apiuri, envelope, onsuccess, onerror, ontimeout);
        };

        factory.send = function(apiuri, id, address, data, onsuccess, onerror) {
            var p = {
                "id": id,
                "address": address,
                "action": 'send',
                "data": data
            };
            this.submit(apiuri, p, onsuccess, onerror);
        };

        return factory;

    })

    .factory('dbFactory', function(termFactory) {
        var factory = {};
        var remoteCouch = false;
        var db = new PouchDB('hydro');

        factory.subscribe = function(document, f) {

        };

        db.info(function(err, info) {
            termFactory.log(info);
            db
                .changes({
                    since: info.update_seq,
                    live: true
                })
                .on('change', function(change) {
                    termFactory.log(change);
                });
        });

        factory.insert = function(document) {
            db.post(document, { local_seq:true }, function(error, response) {
                if (error) {
                    termFactory.log(error);
                } else if (response && response.ok) {
                    termFactory.log(response);
                }
            });
        };

        factory.retrieve = function(onresults) {
            var o = {
                include_docs: true,
                descending: true
            };
            db.allDocs(o, function(err, doc) {
                onresults(doc);
            });
        };

        return factory;
    })

    .factory('gridFactory', function(termFactory) {

    });
