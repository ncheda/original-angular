angular.module('exampleApp', ['ngRoute', 'ngCookies', 'exampleApp.services', 'ngMessages'])
	.config(
		[ '$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider) {

			$routeProvider.when('/create', {
				templateUrl: 'partials/create.html',
				controller: CreateController
			});

			$routeProvider.when('/dns', {
				templateUrl: 'partials/dns.html',
				controller: DnsController
			});
			
			$routeProvider.when('/edit/:id', {
				templateUrl: 'partials/edit.html',
				controller: EditController
			});

			$routeProvider.when('/login', {
				templateUrl: 'partials/login.html',
				controller: LoginController
			});

			$routeProvider.when('/register', {
				templateUrl: 'partials/register.html',
				controller: RegisterController
			});
			
			$routeProvider.otherwise({
				templateUrl: 'partials/index.html',
				controller: IndexController
			});
			
			$locationProvider.hashPrefix('!');

			//Control para evitar CACHE de paginas dinamicas angularjs de INTERNET EXPLORER
			//initialize get if not there
			if (!$httpProvider.defaults.headers.get) {
				$httpProvider.defaults.headers.get = {};
			}
			//disable IE ajax request caching
			$httpProvider.defaults.headers.get['If-Modified-Since'] = 'Mon, 26 Jul 1997 05:00:00 GMT';
			// extras
			$httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
			$httpProvider.defaults.headers.get['Pragma'] = 'no-cache';

			/* Register error provider that shows message on failed requests or redirects to login page on
			 * unauthenticated requests */
		    $httpProvider.interceptors.push(function ($q, $rootScope, $location) {
			        return {
			        	'responseError': function(rejection) {
			        		var status = rejection.status;
			        		var config = rejection.config;
			        		var method = config.method;
			        		var url = config.url;
			      
			        		if (status == 401) {
			        			$location.path( "/login" );
								$rootScope.error = "Usuario o Contrasena incorrecta.";
			        		} else {
			        			$rootScope.error = method + " on " + url + " failed with status " + status;
			        		}
			              
			        		return $q.reject(rejection);
			        	}
			        };
			    }
		    );
		    
		    /* Registers auth token interceptor, auth token is either passed by header or by query parameter
		     * as soon as there is an authenticated user */
		    $httpProvider.interceptors.push(function ($q, $rootScope, $location) {
		        return {
		        	'request': function(config) {
		        		var isRestCall = config.url.indexOf('rest') == 0;
		        		if (isRestCall && angular.isDefined($rootScope.authToken)) {
		        			var authToken = $rootScope.authToken;
		        			if (exampleAppConfig.useAuthTokenHeader) {
		        				config.headers['X-Auth-Token'] = authToken;
		        			} else {
		        				config.url = config.url + "?token=" + authToken;
		        			}
		        		}
		        		return config || $q.when(config);
		        	}

		        };
		    }
	    );
		   
		} ]
		
	).run(function($rootScope, $location, $cookies, UserService) {
		
		/* Reset error when a new view is loaded */
		$rootScope.$on('$viewContentLoaded', function() {
			delete $rootScope.error;
		});
		
		$rootScope.hasRole = function(role) {
			
			if ($rootScope.user === undefined) {
				return false;
			}
			
			if ($rootScope.user.roles[role] === undefined) {
				return false;
			}
			
			return $rootScope.user.roles[role];
		};
		
		$rootScope.logout = function() {
			delete $rootScope.user;
			delete $rootScope.authToken;
			$cookies.remove('authToken');
			$location.path("/login");
		};
		
		 /* Try getting valid user from cookie or go to login page */
		var originalPath = $location.path();
		$location.path("/login");
		var authToken = $cookies.get('authToken');
		if (authToken !== undefined) {
			$rootScope.authToken = authToken;
			UserService.get(function(user) {
				$rootScope.user = user;
				$location.path(originalPath);
			});
		}
		
		$rootScope.initialized = true;
	}).directive("compare", function() {
	return {
		require: "ngModel",
		scope: {
			compare: "="
		},
		link: function(scope, element, attributes, ngModel) {

			ngModel.$validators.compare = function(modelValue) {
				return modelValue == scope.compare;
			};

			scope.$watch("compare", function() {
				ngModel.$validate();
			});
		}
	};
});


function IndexController($scope, NewsService, UserService) {
	
	$scope.newsEntries = NewsService.query();
	
	$scope.deleteEntry = function(newsEntry) {
		newsEntry.$remove(function() {
			$scope.newsEntries = NewsService.query();
		});
	};
};


function EditController($scope, $routeParams, $location, NewsService) {

	$scope.newsEntry = NewsService.get({id: $routeParams.id});
	
	$scope.save = function() {
		$scope.newsEntry.$save(function() {
			$location.path('/');
		});
	};
};


function CreateController($scope, $location, NewsService) {
	
	$scope.newsEntry = new NewsService();
	
	$scope.save = function() {
		$scope.newsEntry.$save(function() {
			$location.path('/');
		});
	};
};


function LoginController($scope, $rootScope, $location, $cookies, UserService) {
	
	$scope.rememberMe = false;
	
	$scope.login = function() {
		UserService.authenticate({name: $scope.username, password: $scope.password}, function(authenticationResult) {
			var authToken = authenticationResult.token;
			$rootScope.authToken = authToken;
			if ($scope.rememberMe) {
				$cookies.put('authToken', authToken);
			}
			UserService.get(function(user) {
				$rootScope.user = user;
				$location.path("/");
			});
		});
	};
};

function RegisterController($scope, $rootScope, $location, $cookies, UserService) {

	$scope.rememberMe = false;

	$scope.register = function() {
		UserService.register({name: $scope.username, password: $scope.password}, function(authenticationResult) {
			var authToken = authenticationResult.token;
			$rootScope.authToken = authToken;
			if ($scope.rememberMe) {
				$cookies.put('authToken', authToken);
			}
			UserService.get(function(user) {
				$rootScope.user = user;
				$location.path("/");
			});
		});
	};
};

function DnsController($scope, UserService) {
	$scope.resultado = UserService.get({action: 6});
	$scope.sortType     = 'dns'; // set the default sort type
	$scope.sortReverse  = false;  // set the default sort order
	$scope.buscar  = '';     // set the default search/filter term
};


var services = angular.module('exampleApp.services', ['ngResource']);

services.factory('UserService', function($resource) {
	
	return $resource('rest/user/:action', {},
			{
				authenticate: {
					method: 'POST',
					params: {'action' : 'authenticate'},
					headers : {'Content-Type': 'application/json'}
				},
				register: {
					method: 'POST',
					params: {'action' : 'register'},
					headers : {'Content-Type': 'application/json'}
				},
			}
		);
});

services.factory('NewsService', function($resource) {
	
	return $resource('rest/news/:id', {id: '@id'});
});