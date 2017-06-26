'use strict';

angular.module('arachne.controllers')

/**
 * @author Patrick Jominet
 */
    .controller('DownloadController', ['$scope', '$uibModalInstance', 'searchService', 'arachneSettings',
        function ($scope, $uibModalInstance, searchService, arachneSettings) {

            $scope.currentQuery = searchService.currentQuery();
            $scope.resultSize = searchService.getSize();
            $scope.mode = 'csv';

            $scope.downloadAs = function () {
                var currentQuery = $scope.currentQuery;
                var currentMode = $scope.mode;
                $scope.filename = 'currentSearch.' + currentMode;
                return arachneSettings.dataserviceUri + '/search.' + currentMode + '?q=' + currentQuery.q;
            };

            $scope.cancel = function () {
                $uibModalInstance.dismiss();
            };
        }]);
