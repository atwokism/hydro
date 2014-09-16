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

    .controller('SessionCtrl', function($scope, $route, authFactory, termFactory, messageFactory, apiRESTFactory, dbFactory) {

        messageFactory.touch();

        $scope.publish_response_message = '';
        $scope.client_id = termFactory.terminal();
        $scope.profile = termFactory.profile();
        $scope.user_id = termFactory.identity();
        $scope.session_id = termFactory.session();
        $scope.expiry_value = termFactory.expiry();
        $scope.event_db_row_count = 0;

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
            $scope.testPubWithTimeout(event);
        };

        $scope.testPubWithTimeout = function(event){

            var success = function(data, status, headers, config) {
                $scope.publish_response_message = JSON.stringify(data, undefined, 2);
                dbFactory.insert(data);
                termFactory.log('received publish response: ' + JSON.stringify(data));
            };
            var error = function(data, status, headers, config) {
                dbFactory.insert(data);
                termFactory.log('error: ' + JSON.stringify(data) + ', status=' + status);
            };
            var timeout = function(data, status, headers, config) {
                dbFactory.insert(data);
                success(data, status, headers, config);
            };
            var envelope = termFactory.envelope(event, 'hydro.event.publish', Helper.action.execute, Helper.transact.promise, event.correlation);
            dbFactory.insert(envelope);
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



        $scope.populateEventsView = function() {

            var grid = jQuery("#grid_1").jqGrid({
                datatype: "json",
                colNames:['Process','Correlation','Event','Version','Key','ID','Action','Address','Type','Source'],
                colModel:[
                    {name:'process', index:'process', width:80},
                    {name:'correlation', index:'correlation', width:90},
                    {name:'name', index:'name asc, invdate', width:80},
                    {name:'version', width:20, align: "right"},
                    {name:'key', index:'key', width:60},
                    {name:'id', index:'id', width:60},
                    {name:'action', width:50},
                    {name:'address', index:'address', width:100},
                    {name:'transact', width:50},
                    {name:'source', width:120}
                ],
                rowNum:10,
                rowList:[5,10,20],
                pager: '#pager_1',
                sortname: 'process',
                viewrecords: true,
                sortorder: "asc",
                caption:"Event Viewer"
            });

            var c = function(resultset) {
                termFactory.log(resultset);
                $scope.event_db_row_count = resultset.total_rows;

                var data = [];
                resultset.rows.forEach(function(row) {
                    var d = {
                        "id": row.doc.id,
                        "key": row.key,
                        "correlation": row.doc.correlation,
                        "name": row.doc.data.name,
                        "version": row.doc.data.version,
                        "action": row.doc.action,
                        "address": row.doc.address,
                        "process": row.doc.data.process,
                        "transact": row.doc.transact,
                        "source": row.doc.source
                    };
                    data.push(d);
                });
                grid[0].addJSONData(data);
            };
            dbFactory.retrieve(c);
        };

        $scope.clear = function(event) {
            event.name = '';
            event.version = '';
            event.process = '';
            event.payload = '';
        };
    });


