/**
 * Created with JetBrains WebStorm.
 * User: atwoki
 * Date: 2013/10/28
 * Time: 11:28 PM
 * To change this template use File | Settings | File Templates.
 */
angular

.module('hydro',
    ['ngResource',
    'ngRoute',
    'hydro.services',
    'hydro.controllers'],
    function() {
        // configure services
    }
)

.config(['$routeProvider', function($routeProvider) {
    $routeProvider
        .when('/publish/event/view', {
            templateUrl: '/pages/views/event_form.html',
            controller: 'SessionCtrl'
        })
        .when('/home/view', {
            templateUrl: '/pages/views/splash_screen.html',
            controller: 'SessionCtrl'
        })
        .when('/events/db/view', {
            templateUrl: '/pages/views/events_view.html',
            controller: 'SessionCtrl'
        });
}]);

