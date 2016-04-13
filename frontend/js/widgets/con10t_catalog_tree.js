'use strict';

/* Widget directives */
angular.module('arachne.widgets.directives')

/**
 * @author: Sebastian Cuy
 * @author: Thomas Kleinke
 */
.directive('con10tCatalogTree', ['Catalog', 'CatalogEntry', '$filter',
	function(Catalog, CatalogEntry, $filter) {
		return {
			restrict: 'E',
			scope: {
				catalogId: '@',
				heading: '='
			},
			templateUrl: 'partials/widgets/con10t-catalog-tree.html',
			link: function(scope) {
				
				scope.catalog = Catalog.get({id:scope.catalogId});
				scope.isShown = {};
				
				scope.escapePath = function(path){
					return $filter('escapeSlashes')(path)
				};
				
				scope.toggleCollapse = function(node){
				    if (node.hasChildren) {
						scope.isShown[node.id] = !scope.isShown[node.id];
				    	if (!node.hasOwnProperty('children')) {
				    		CatalogEntry.get({id:node.id}, function(newNode) {
				    			node.children = newNode.children;
				    		});
				    	}
				    }
				};
				
				scope.checkIfShown = function(node){
					return scope.isShown[node.id]; // at first load -> undefined, so it gets hidden but: ugly?
				};

			}
		};
	}
])