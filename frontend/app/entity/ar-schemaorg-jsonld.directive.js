export default function ($filter, $sce) {
    return {
        scope:
            { entity: '=', lastmodified: '=' },
        replace: true,
        link: function (scope, element) {

            scope.updateJsonContent = (entity, oldVal) => {

                if (!entity) return;

                element[0].outerHTML = '<script type="application/ld+json">' + this.getJson(entity) + '</script>'
            }

            scope.$watch('entity', scope.updateJsonContent);
        },
        getJson: (entity) => {

            let places = {};
            if (entity.places && entity.places.length > 0) {
                places = {
                    "spatialCoverage": {
                        "@context": "http://schema.org",
                        "@type": "Place",
                        "geo": {
                            "@type": "GeoCoordinates",
                            "latitude": entity.places[0].location.lat,
                            "longitude": entity.places[0].location.lon
                        },
                        "name": entity.places[0].name
                    },
                }
            };

            let image = {};
            if (entity.thumbnailId) {
                image = {
                    "image": "https://arachne.dainst.org/data/image/" + entity.thumbnailId,
                }
            }

            const coreFields = {
                "@type": "Article",
                "@id": "https://arachne.dainst.org/entity/" + entity.entityId,
                "author": "Arachne",
                "dateModified": entity.lastModified,
                "datePublished": entity.lastModified,
                "mainEntityOfPage": "https://arachne.dainst.org",
                "headline": entity.title.substring(0, 110),
                "publisher": {
                    "@type": "Organization",
                    "name": "Arachne - Archaeological Institute of the University of Cologne and the German Archaeological Institute",
                    "logo": {
                        "@type": "imageObject",
                        "url": "https://arachne.dainst.org/img/arachnelogo.png"
                    }
                }
            }

            return $sce.trustAsHtml($filter('json')({
                "@context": "http://schema.org",
                "@type": "WebPage",
                "name": "Arachne",
                "url": "https://arachne.dainst.org",
                "description": "Object database and cultural archives of the Archaeological Institute of the University of Cologne and the German Archaeological Institute",
                "breadcrumb": {
                    "@type": "BreadcrumbList",
                    "itemListElement": [
                        {
                            "@type": "ListItem",
                            "position": "1",
                            "item": {
                                "@type": "WebSite",
                                "@id": "https://arachne.dainst.org",
                                "name": "Arachne"
                            }
                        },
                        {
                            "@type": "ListItem",
                            "position": "2",
                            "item": {
                                "@type": "WebPage",
                                "@id": "https://arachne.dainst.org/categories",
                                "name": "Categories"
                            }
                        },
                        {
                            "@type": "ListItem",
                            "position": "3",
                            "item": {
                                "@type": "WebPage",
                                "@id": "https://arachne.dainst.org/" + entity.categoryHref,
                                "name": entity.type
                            }
                        }
                    ]
                },
                "mainEntity": { ...coreFields, ...image, ...places }
            }));
        }
    }
};
