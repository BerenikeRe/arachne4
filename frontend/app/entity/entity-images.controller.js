'use strict';

angular.module('arachne.controllers')

    .controller('EntityImagesController', ['$stateParams', '$scope', 'Entity', '$filter', 'searchService', '$rootScope', 'messageService',
        function ($stateParams, $scope, Entity, $filter, searchService, $rootScope, messages) {

            $rootScope.hideFooter = true;
            $scope.currentQuery = searchService.currentQuery();
            $scope.entityId = $stateParams.entityId;
            $scope.imageId = $stateParams.imageId;
            
            Entity.get({id: $stateParams.entityId}, function (data) {
                // call to filter detached from view in order to prevent unnecessary calls
                $scope.entity = data;
                $scope.cells = $filter('cellsFromImages')(data.images, data.entityId, $scope.currentQuery);
            }, function (response) {
                messages.add("entity_" + response.status);
            });
        }
    ]);