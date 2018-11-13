'use strict';

angular.module('arachne.controllers')

.controller('DownloadController', ['$scope', '$uibModalInstance', '$http', 'arachneSettings', 'downloadUrl', 'transl8',
    function ($scope, $uibModalInstance, $http, arachneSettings, downloadUrl, transl8) {

        $scope.mode = 'csv';
        $scope.formats = [];
        $scope.message = "";
        $scope.status = -1;

        function refresh() {
            if(!$scope.$$phase) {
                $scope.$apply();
            }
        }

        function transl8Response(msg) {
            if (msg.substr(0, 12) !== "data_export_") {
                return msg;
            }
            var msgParts = msg.split("|");
            var msgKey = msgParts.shift();
            try {
                var transl8edMsg = transl8.getTranslation(msgKey);
            } catch(e) {
                var transl8edMsg = '#' + msgKey;
            }
            return transl8edMsg + msgParts.join("|");
        }

        $http.get(arachneSettings.dataserviceUri + '/export/types').then(
            function(response) {
                $scope.formats = angular.isDefined(response.data) ? response.data : [];
                $scope.message = "";
                refresh();
            },
            function(response){
                $scope.message = transl8.getTranslation("data_export_no_export_formats_available");
                $scope.status = response.status;
                console.warn(response);
                refresh();
            }
        );

        $scope.downloadAs = function() {
            var connector = (downloadUrl.indexOf('?') > -1) ? '&' : '?';
            var url = arachneSettings.dataserviceUri + downloadUrl + connector +'mediaType=' + $scope.mode;
            $http.get(url).then(
                function(response) {
                    $scope.status = response.status;
                    if ($scope.status === 200) {
                        $scope.message = "";
                        var linkElem = document.querySelector('#hiddenDownloadLink');
                        var link = angular.element(linkElem);
                        link.prop("href", 'data: ' + $scope.mode + ';charset=utf-8,' + '\ufeff' + encodeURIComponent(response.data));
                        link.prop("download", 'currentSearch.' + $scope.mode);
                        linkElem.click();
                        $uibModalInstance.dismiss();
                    } else {
                        $scope.message = transl8Response(response.data);
                    }
                    refresh();
                },
                function(response) {
                    console.warn(response);
                    $scope.message = transl8Response(response.data);
                    $scope.status = response.status;
                    refresh();
                }
            );

        };

        $scope.changeMode = function(mode){
            $scope.mode = mode;
        };

        $scope.cancel = function() {
            $uibModalInstance.dismiss();
        };

        $scope.isSuccess = function() {
            if ($scope.status === -1) {
                return null;
            }
            return ($scope.status.toString().substr(0, 1) === "2");
        };

        $scope.reset = function() {
            $scope.status = -1;
            $scope.message = "";
        }
}]);