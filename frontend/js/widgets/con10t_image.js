'use strict';

angular.module('arachne.widgets.directives')

/**
 * @author: Jan G. Wieners
 */
    .directive('con10tImage', [function() {
        return {
            restrict: 'E',
            scope: {
                src: '@',
                alt: '@',
                align: '@',
                entityId: '@'
            },
            transclude: true,
            templateUrl: 'partials/widgets/con10t-image.html',

            link: function(scope, element, attrs, ctrl, $transclude) {

                $transclude(function(clone){
                    scope.showCaption = clone.length;
                });
            }
        }
    }]);