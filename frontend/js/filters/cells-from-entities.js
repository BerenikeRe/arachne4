angular.module('arachne.filters')

    .filter('cellsFromEntities', ['arachneSettings', 'categoryService', function (arachneSettings, categoryService) {
        return function (entities, query) {
            for (var i in entities) {
                entities[i].label = categoryService.getSingular(entities[i].type);
                entities[i].href = 'entity/' + entities[i].entityId;
                if (typeof query != 'undefined') {
                    entities[i].href += query.setParam("resultIndex", (parseInt(query.offset) + parseInt(i) + 1)).toString();
                }
                if (typeof entities[i].thumbnailId != 'undefined') {
                    entities[i].imgUri = arachneSettings.dataserviceUri + "/image/height/" + entities[i].thumbnailId + "?height=300";
                }
                if (typeof entities[i].highlights != 'undefined') {
                    var highlights = []
                    for (var highlightedField in entities[i].highlights) {
                        highlights.push(entities[i].highlights[highlightedField].join('...'))
                    }
                    entities[i].highlighting = highlights.join(', ')
                }
            }
            return entities;
        }
    }])