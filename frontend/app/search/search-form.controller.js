'use strict';

angular.module('arachne.controllers')

    .controller('SearchFormController', ['$scope', '$location', 'arachneSettings', '$http', function ($scope, $location, arachneSettings, $http) {

        $scope.search = function (fq) {
            if ($scope.q) {
                var url = '/search?q=' + $scope.q;
                if (fq) url += "&fq=" + fq;
                $scope.q = null;
                $location.url(url);
            }
        };

        $scope.getSuggestions = function (value) {
            return $http.get(arachneSettings.dataserviceUri + '/suggest?q="' + value + '"')
                .then(function (response) {
                    return response.data.suggestions;
                });
        };
    }
    ]);