'use strict';

angular.module('arachne.directives')

/**
 * @author: David Neugebauer
 */
.directive('arMapMenuSearchInfo', ['$modal', '$location', 'searchService', 'placesService', 'mapService',
function($modal, $location, searchService, placesService, mapService) {
    return {
        restrict: 'A',
        scope: {
            // "grid" or "places", depending on the map's type, different
            // search results are required
            type: '@'
        },
        templateUrl: 'partials/directives/ar-map-menu-search-info.html',
        link: function(scope) {

            // renders a modal that contains a link to the current map's view
            scope.showLinkModal = function() {
                // construct the link's reference from the current location and the map's query
                var host = $location.host();
                var port = $location.port();
                port = (port == 80) ? "" : ":"+port;
                var baseLinkRef = document.getElementById('baseLink').getAttribute("href");
                var path = $location.path().substring(1);
                var query = mapService.getMapQuery().toString();
                scope.linkText = host + port + baseLinkRef + path + query;

                var modalInstance = $modal.open({
                    templateUrl: 'partials/Modals/mapLink.html',
                    scope: scope
                });

                modalInstance.close = function(){
                    modalInstance.dismiss();
                };

                // Select and focus the link after the modal rendered
                modalInstance.rendered.then(function(what) {
                    var elem = document.getElementById('link-display');
                    elem.setSelectionRange(0, elem.firstChild.length);
                    elem.focus();
                })
            }

            // basic information about the search depends on the type of the map
            // (either a geogrid or a map with Place objects)
            scope.entityCount = null;
            scope.placesCount = null;
            if (scope.type == "grid") {
                searchService.getCurrentPage().then(function (entities) {
                    scope.entityCount = searchService.getSize();
                });
            } else if (scope.type == "places") {
                placesService.getCurrentPlaces().then(function(places) {
                    scope.entityCount = placesService.getEntityCount();
                    scope.placesCount = places.length;
                });
            }
        }
    }
}]);