/**
 * Created with JetBrains WebStorm.
 * User: atwoki
 * Date: 2013/10/28
 * Time: 11:33 PM
 * To change this template use File | Settings | File Templates.
 */
angular

    .module(
        'hydro.controllers',
        [],
        function() {
            // config
        }
    )

    .controller('AuthCtrl', function($scope, authFactory, termFactory) {

        $scope.client_id = '';

        $scope.$watch('client_id', function() {
            $scope.client_id = termFactory.terminal();
        });

        $scope.login = function(credentials, authurl, resurl) {
            credentials.client_id = $scope.client_id;
            authFactory.signin(credentials, authurl, resurl);
        };

        $scope.clear = function(credentials) {
            credentials.domain = '';
            credentials.user = '';
            credentials.password = '';
            credentials.provider = '';
        };
    })

    .controller('SessionCtrl', function($scope, $route, authFactory, termFactory, messageFactory, apiRESTFactory) {

        messageFactory.touch();

        $scope.publish_response_message = '';
        $scope.client_id = termFactory.terminal();
        $scope.profile = termFactory.profile();
        $scope.user_id = termFactory.identity();
        $scope.session_id = termFactory.session();
        $scope.expiry_value = termFactory.expiry();

        $scope.$watch('profile', function() {
            // $scope.profile = termFactory.profile();
        });

        $scope.logout = function(authurl) {
            authFactory.signout(authurl);
        };

        $scope.publish = function(event) {
            var pubMsg = {
                "meta": termFactory.metadata('dispatch'),
                "event": messageFactory.event(event),
                "credentials": {
                    "token": $scope.profile.secret
                }
            };
            var success = function(data, status, headers, config) {
                $scope.publish_response_message = JSON.stringify(data);
                termFactory.log('received publish response: ' + JSON.stringify(data));
            };
            var error = function(data, status, headers, config) {
                termFactory.log('error: ' + JSON.stringify(data) + ', status=' + status);
            };
            apiRESTFactory.send('/json-rest/api/', Helper.uuid(), 'hydro.event.publish', pubMsg, success, error);
        };

        $scope.publishAndWait = function(event){
            /*
            var wait = 2000;
            var pubMsg = {
                "meta": termFactory.metadata('promise'),
                "event": messageFactory.event(event),
                "credentials": {
                    "token": termFactory.profile().secret
                }
            };
            var success = function(data, status, headers, config) {
                $scope.publish_response_message = JSON.stringify(data, undefined, 2);
                termFactory.log('received publish response: ' + JSON.stringify(data));
            };
            var error = function(data, status, headers, config) {
                termFactory.log('error: ' + JSON.stringify(data) + ', status=' + status);
            };
            var timeout = function(data, status, headers, config) {
                termFactory.log('received publish response: ' + JSON.stringify(data));
            };
            */
            // termFactory.envelope(data, address, Helper.action.execute, Helper.transact.promise);
            // apiRESTFactory.executeWithTimeout('/json-rest/api/', Helper.uuid(), 'hydro.event.publish', pubMsg, wait, success, error, timeout);
            $scope.testPubWithTimeout(event);
        };

        $scope.testPubWithTimeout = function(event){

            var success = function(data, status, headers, config) {
                $scope.publish_response_message = JSON.stringify(data, undefined, 2);
                termFactory.log('received publish response: ' + JSON.stringify(data));
            };
            var error = function(data, status, headers, config) {
                termFactory.log('error: ' + JSON.stringify(data) + ', status=' + status);
            };
            var timeout = function(data, status, headers, config) {
                success(data, status, headers, config);
            };
            var envelope = termFactory.envelope(event, 'hydro.event.publish', Helper.action.execute, Helper.transact.promise, event.correlation);
            apiRESTFactory.testWithTimeout(envelope, '/json-rest/api/', success, error, timeout);
        };

        $scope.publishAndReceive = function(event) {
            var pubMsg = {
                "meta": termFactory.metadata('conversation'),
                "event": messageFactory.event(event),
                "credentials": {
                    "token": termFactory.profile().secret
                }
            };
        };

        $scope.clear = function(event) {
            event.name = '';
            event.version = '';
            event.process = '';
            event.payload = '';
        };
    });


