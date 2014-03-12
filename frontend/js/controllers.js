'use strict';

/* Controllers */

angular.module('arachne.controllers', ['ui.bootstrap'])
.controller('MenuCtrl',
	[ '$scope', '$modal', '$log', 'sessionService',
	function ($scope,  $modal, $log, sessionService){
		$scope.user = sessionService.user;

		$scope.openLoginModal = function () {
			var modalInstance = $modal.open({
				templateUrl: 'loginForm.html',
				controller: 'LoginCtrl'	
			});				    
		};
	}
	]
	)
.controller('LoginCtrl',
	['$scope', '$modalInstance', 'sessionService', 'md5Filter',
	function($scope, $modalInstance, sessionService, md5Filter){
		$scope.loginData = {};
		$scope.user = sessionService.user;
		$scope.login = function () {
			sessionService.login(
				{user: $scope.loginData.user, password: md5Filter($scope.loginData.password)}
				,
	            function(res) {
	                console.log("eingeloggt");
	            },
	            function(err) {
	                console.log("nicht eingeloggt");
	            });

		};

		$scope.cancel = function () {
			$modalInstance.dismiss();
		};	
	
	}
	]
	)
.controller('SearchCtrl',
	['$location', 'arachneSearch', '$scope', 
	function ( $location, arachneSearch, $scope) {

		this.parseUrlFQ = function (fqParam) {
			var facets = [];
			fqParam = fqParam.split(/\"\,/);
			for (var i = fqParam.length - 1; i >= 0; i--) {
				var facetNameAndVal = fqParam[i].replace(/"/g,'').split(':');

				facets.push({
					name: facetNameAndVal[0],
					value: facetNameAndVal[1]
				});
			};
			return facets;
		};

		$scope.activeFacets = $location.$$search.fq ? this.parseUrlFQ($location.$$search.fq) : [];

		this.append = function () {
			var hash = $location.$$search;

			if (hash.offset) {
				hash.offset = parseInt(hash.offset)+50;
			} else {
				hash.offset = 50;
			}
			$scope.search = arachneSearch.executeSearch(hash);

		};


		$scope.search = arachneSearch.executeSearch($location.$$search);

		this.addFacet = function (facetName, facetValue) {
					//Check if facet is already included
					for (var i = $scope.activeFacets.length - 1; i >= 0; i--) {
						if ($scope.activeFacets[i].name == facetName) return;
					};

					var hash = $location.$$search;

					if (hash.fq) {
						hash.fq += "," + facetName + ':"' + facetValue + '"';
					} else {
						hash.fq = facetName + ':"' + facetValue + '"';
					}

					$scope.results = [];
					$location.search(hash);
				}

				this.removeFacet = function (facet) {
					for (var i = $scope.activeFacets.length - 1; i >= 0; i--) {
						if ($scope.activeFacets[i].name == facet.name) {
							$scope.activeFacets.splice(i,1);
						}
					};
					$scope.results = [];
					var facets = $scope.activeFacets.map(function(facet){
						return facet.name + ':"' + facet.value + '"';
					}).join(",");
					var hash = $location.$$search;
					hash.fq = facets;
					$location.search(hash);
				}
			}
			]
			)
.controller('EntityCtrl',
	['$routeParams', 'arachneSearch', '$scope', '$modal', 'arachneEntity',
	function ( $routeParams, arachneSearch, $scope, $modal, arachneEntity) {
		if(typeof $scope.currentSearch !== "undefined" && $scope.currentSearch !== null) {
			$scope.currentSearch = arachneSearch.currentSearch;
		} else {
			$scope.currentSearch = JSON.parse(localStorage.getItem('currentSearch'));
		}
		$scope.isArray = function(value) {
			return angular.isArray(value);
		}
		$scope.typeOf = function(input) {
			var result = typeof input;
			console.log(input);
			return result;
		}

		$scope.entity = arachneEntity.get({id:$routeParams.id});
		$scope.bookmarks = ['test', 'Rom', 'Berlin'];
		$scope.selectedBK = 'test';
  		//$scope.bookmarkSelected = $scope.bookmarks[2];
		$scope.openBookmarkModal = function () {
			var modalInstance = $modal.open({
				templateUrl: 'bookmarkForm.html'
			});				    
		};

		console.log($scope.entity);
	}])

	.controller('EntityImgCtrl', ['$routeParams', '$scope', 'arachneEntityImg', 
		function ($routeParams, $scope, arachneEntityImg) {
		//$scope.img = arachneEntityImg.get({id:$routeParams.id});
		$scope.imgID = $routeParams.id; 	
	}])

	.controller('NewsController', ['$scope', 'newsFactory', 'teaserFactory', 'arachneSearch', function ($scope, newsFactory, teaserFactory, arachneSearch) {
	
		$scope.selection = 'search';
		var hash = new Object();
		hash.q = "*";
		hash.fl= "1500";

		$scope.search = arachneSearch.getMarkers(hash);

		newsFactory.getNews().success(function(data) { $scope.newsList = data;})		
		teaserFactory.getTeaser().success(function(data) {$scope.teaserList = data;})
	}])

	.controller('MapController', ['$scope', 'arachneSearch', function ($scope, arachneSearch) {
		var hash = new Object();
		hash.q = "*";
		hash.fl= "1500";
		$scope.map = arachneSearch.getMarkers(hash); 
	}]);;
