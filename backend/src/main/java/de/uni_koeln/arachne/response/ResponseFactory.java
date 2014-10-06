package de.uni_koeln.arachne.response;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.uni_koeln.arachne.context.AbstractLink;
import de.uni_koeln.arachne.context.Context;
import de.uni_koeln.arachne.dao.GenericSQLDao;
import de.uni_koeln.arachne.service.Transl8Service;
import de.uni_koeln.arachne.util.EntityId;
import de.uni_koeln.arachne.util.JSONUtil;
import de.uni_koeln.arachne.util.StrUtils;
import de.uni_koeln.arachne.util.XmlConfigUtil;

/**
 * Factory class to create the different kinds of responses from a dataset.
 * The <code>createX</code> methods may access xml config files to create the response objects. These config files are found in the <code>WEB-INF/xml/</code> directory.
 * Currently only the <code>createFormattedArachneEntity</code> method uses these files so that the naming scheme <code>$(TYPE).xml</code> is sufficient. If other methods
 * want to use different xml config files a new naming scheme is needed.
 * <br>
 * This class can be autowired.
 */
@Component
@Configurable(preConstruction=true)
public class ResponseFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseFactory.class);
	
	@Autowired
	private transient XmlConfigUtil xmlConfigUtil;
	
	@Autowired
	private transient GenericSQLDao genericSQLDao;
	
	@Autowired
	private transient Transl8Service ts;
	
	@Autowired
	private transient JSONUtil jsonUtil;
	
	// needed for testing
	public void setXmlConfigUtil(final XmlConfigUtil xmlConfigUtil) {
		this.xmlConfigUtil = xmlConfigUtil;
	}
	
	/**
	 * Creates a formatted response object as used by the front-end. The structure of this object is defined in the xml config files.
	 * First the type of the object will be determined from the dataset (e.g. bauwerk). Based on the type the corresponding xml file <code>$(TYPE).xml</code> is read.
	 * The response is then created, according to the xml file, from the dataset.
	 * <br>
	 * The validity of the xml file is not checked!!!
	 * @param dataset The dataset which encapsulates the SQL query results.
	 * @return A <code>FormattedArachneEntity</code> as JSON (<code>String</code>).
	 */
	public String createFormattedArachneEntityAsJsonString(final Dataset dataset) {
		
		final EntityId arachneId = dataset.getArachneId(); 
		final String tableName = arachneId.getTableName();
		final Document document = xmlConfigUtil.getDocument(tableName);
		
		final FormattedArachneEntity response = createFormattedArachneEntity(dataset, arachneId, tableName);
				
		if (document != null) {
			//Set additional Content
			response.setAdditionalContent(dataset.getAdditionalContent());
			
			return getEntityAsJson(dataset, document, response).toString();
		}

		LOGGER.error("No xml document for '" + tableName + "' found.");
		return null;
	}

	/**
	 * Creates a formatted response object as used by the front-end. The structure of this object is defined in the xml config files.
	 * First the type of the object will be determined from the dataset (e.g. bauwerk). Based on the type the corresponding xml file <code>$(TYPE).xml</code> is read.
	 * The response is then created, according to the xml file, from the dataset.
	 * <br>
	 * The validity of the xml file is not checked!!!
	 * @param dataset The dataset which encapsulates the SQL query results.
	 * @return A <code>FormattedArachneEntity</code> as JSON (<code>raw bytes</code>).
	 */
	public byte[] createFormattedArachneEntityAsJson(final Dataset dataset) {
		
		final EntityId arachneId = dataset.getArachneId(); 
		final String tableName = arachneId.getTableName();
		final Document document = xmlConfigUtil.getDocument(tableName);
		
		final FormattedArachneEntity response = createFormattedArachneEntity(dataset, arachneId, tableName);
				
		if (document != null) {
			//Set additional Content
			response.setAdditionalContent(dataset.getAdditionalContent());
			
			byte[] json = null;
			try {
				json = jsonUtil.getObjectMapper().writeValueAsBytes(getEntityAsJson(dataset, document, response));
			} catch (JsonProcessingException e) {
				LOGGER.error("Failed to serialize entity " + arachneId.getArachneEntityID() + ".Cause: ", e);
				e.printStackTrace();
			}
			return json;
		}

		LOGGER.error("No xml document for '" + tableName + "' found.");
		return null;
	}
	
	/**
	 * @param dataset
	 * @param arachneId
	 * @param tableName
	 * @return
	 */
	private FormattedArachneEntity createFormattedArachneEntity(
			final Dataset dataset, final EntityId arachneId,
			final String tableName) {
		final FormattedArachneEntity response = new FormattedArachneEntity();
		
		// set id content
		response.setEntityId(arachneId.getArachneEntityID());
		response.setType(ts.transl8("type_" + tableName));
		response.setInternalId(arachneId.getInternalKey());
		
		// set thumbnailId
		response.setThumbnailId(dataset.getThumbnailId());
		
		// set connectedEntities
		final List<Long> connectedEntities = genericSQLDao.getConnectedEntityIds(arachneId.getArachneEntityID());  
		response.setConnectedEntities(connectedEntities);
		
		// set degree
		if (connectedEntities != null && !connectedEntities.isEmpty()) {
			response.setDegree(connectedEntities.size());
		}
						
		// set fields
		response.setFields(dataset.getFields().size() + dataset.getContexts().size());
				
		// set boost
		final double logFields = Math.log10(response.fields + 1.0d); 
		final double boost = (logFields * logFields * Math.log10(response.degree + 1.0d)) / 5.0d + 1.0d;
		response.setBoost(boost);
		
		// set dataset group
		// workaround for table marbilder as it does not adhere to the naming conventions
		String datasetGroupFieldName = null;
		if ("marbilder".equals(tableName)) {
			datasetGroupFieldName = "marbilder.DatensatzGruppeMARBilder";
		} else {
			datasetGroupFieldName = tableName+".DatensatzGruppe"+tableName.substring(0,1).toUpperCase()+tableName.substring(1);
		}
		response.setDatasetGroup(dataset.getFieldFromFields(datasetGroupFieldName));
		// set datasetGroup to "Arachne" (visible for all) for entities that do not have a datasetGroup like 'literatur' to
		// make the access control in the search easier/consistent
		if (response.getDatasetGroup() == null) {
			response.setDatasetGroup("Arachne");
		}

		// set lastModified
		Date lastModified;
		try {
			lastModified = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS", Locale.GERMAN).parse(
					dataset.getFieldFromFields(tableName + ".lastModified"));
		} catch (Exception e) {
			lastModified = null;
		}
		response.setLastModified(lastModified);
		
		// set geo information 
		// TODO make the place handling more consistent - needs changes to the db
		Context placeContext = dataset.getContext("ort");
		
		if (placeContext != null) {
			for (AbstractLink link: placeContext.getAllContexts()) {
				final String city = link.getFieldFromFields("ort.Stadt");
				final String country = link.getFieldFromFields("ort.Land");
				final String additionalInfo = link.getFieldFromFields("ort.Aufbewahrungsort");
				String place = null;
				if (!StrUtils.isEmptyOrNull(city)) {
					place = city;				
					if (!StrUtils.isEmptyOrNull(country)) {
						place += ", " + country;
						if (!StrUtils.isEmptyOrNull(additionalInfo)) {
							place += ", " + additionalInfo;
						}
					}
				}
				final String locationDescription = link.getFieldFromFields("ort.ArtOrtsangabe");
				final String lat = link.getFieldFromFields("ort.Latitude");
				final String lon = link.getFieldFromFields("ort.Longitude");
				String location = null;
				if (lat != null && lon != null) {
					location = lat + "," + lon;
				}
				if (!StrUtils.isEmptyOrNull(place) && !StrUtils.isEmptyOrNull(locationDescription)) {
					if ("Fundort".equals(locationDescription)) {
						response.setFindSpot(place);
						if (!StrUtils.isEmptyOrNull(location)) {
							response.setFindSpotLocation(location);
						}
					} else {
						if (locationDescription.contains("Aufbewahrung") && !locationDescription.contains("temporäre")
								&& !locationDescription.contains("vorheriger")) {
							response.setDepository(place);
							if (!StrUtils.isEmptyOrNull(location)) {
								response.setDepositoryLocation(location);
							}
						} else {
							if ("in situ".equals(locationDescription)) {
								response.setFindSpot(place);
								response.setDepository(place);
								if (!StrUtils.isEmptyOrNull(location)) {
									response.setFindSpotLocation(location);
									response.setDepositoryLocation(location);
								}
							}
						}
					}
				}
			}
		}
		return response;
	}
	
	/**
	 * Method to construct a response object for a deleted entity.
	 * @param entityId The ID of the entity.
	 * @return The JSON for the deleted entity as <code>String</code>. 
	 */
	public String createResponseForDeletedEntityAsJsonString(final EntityId entityId) {
		try {
			return jsonUtil.getObjectMapper().writeValueAsString(new DeletedArachneEntity(entityId));
		} catch (JsonProcessingException e) {
			LOGGER.error("Error serializing response for deleted entity [" + entityId + "]. Cause: ", e);
		}
		return null;
	}

	/**
	 * Method to construct a response object for a deleted entity.
	 * @param entityId The ID of the entity.
	 * @return The JSON for the deleted entity as <code>String</code>. 
	 */
	public byte[] createResponseForDeletedEntityAsJson(final EntityId entityId) {
		try {
			return jsonUtil.getObjectMapper().writeValueAsBytes(new DeletedArachneEntity(entityId));
		} catch (JsonProcessingException e) {
			LOGGER.error("Error serializing response for deleted entity [" + entityId + "]. Cause: ", e);
		}
		return null;
	}
	
	/**
	 * Retrieves the title for the response.
	 * @param dataset The current dataset.
	 * @param namespace The document namespace.
	 * @param display The display element.
	 * @return A <code>String</code> containing the concatenated values of the <code>title</code> tag.
	 */
	private String getTitleString(final Dataset dataset, final Namespace namespace, final Element display) {
		String result = "";
		final Element title = display.getChild("title", namespace);
    	if (title.getChild("field", namespace) == null) {
    		result = contentListToString(getContentList(dataset, namespace, title));
    	} else {
    		result = dataset.getField(title.getChild("field", namespace).getAttributeValue("datasource"));
    	}
    	return result;
	}
	
	/**
	 * Retrieves the subtitle for the response.
	 * @param dataset The current dataset.
	 * @param namespace The document namespace.
	 * @param display The display element.
	 * @return A <code>String</code> containing the concatenated values of the <code>subtitle</code> tag.
	 */
	private String getSubTitle(final Dataset dataset, final Namespace namespace, final Element display) {
		
		String result = "";
		final Element subtitle = display.getChild("subtitle", namespace);
		if (subtitle.getChild("field", namespace) == null) {
			result = contentListToString(getContentList(dataset, namespace, subtitle));
		} else {
			result = dataset.fields.get(subtitle.getChild("field", namespace).getAttributeValue("datasource"));
		}
		return result;
	}
	
	/**
	 * Sets the sections of the response according to the definitions in the corresponing xml config file. 
	 * @param dataset The current dataset.
	 * @param namespace The document namespace.
	 * @param display The display element.
	 * @param response The response object to add the sections to.
	 */
	private void setSections(final Dataset dataset, final Namespace namespace, final Element display
			, final FormattedArachneEntity response) {

		final Element sections = display.getChild("datasections", namespace);
		final List<AbstractContent> contentList = getContentList(dataset, namespace, sections);
		
		if (contentList != null) {
			if (contentList.size() == 1) {
				response.setSections(contentList.get(0));
			} else {
				final Section sectionContent = new Section();
				sectionContent.setLabel("ContainerSection");
				for (final AbstractContent content: contentList) {
					sectionContent.add(content);
				}
				response.setSections(sectionContent);
			}
		}
	}
	
	/**
	 * Sets the part of the response that is defined in the corresponding XML config file.
	 * @param dataset The current dataset.
	 * @param document The xml document describing the output format.
	 * @param response The response object to add the content to.
	 */
	private ObjectNode getEntityAsJson(final Dataset dataset, final Document document
			, final FormattedArachneEntity response) {
		
		final Namespace namespace = document.getRootElement().getNamespace();
		final Element display = document.getRootElement().getChild("display", namespace);

		// set title
		final String titleStr = getTitleString(dataset, namespace, display);
		response.setTitle(titleStr);

		// set subtitle
		final String subtitleStr = getSubTitle(dataset, namespace, display);
		response.setSubtitle(subtitleStr);

		// set datasection
		setSections(dataset, namespace, display, response);

		// Set images
		response.setImages(dataset.getImages());
		
		return getFacettedEntityAsJson(dataset, document, response, namespace);
	}

	/**
	 * This method serializes the <code>FormattedArachneEntity</code> to JSON and adds the facets.  
	 * @param dataset The current dataset.
	 * @param document The xml document describing the output format.
	 * @param response The response object to add the content to.
	 * @param namespace The document namespace.
	 * @return A Jackson ObjectNode representing the JSON as tree.
	 */
	private ObjectNode getFacettedEntityAsJson(final Dataset dataset, final Document document
			, final FormattedArachneEntity response, final Namespace namespace) {

		ObjectMapper oMapper = jsonUtil.getObjectMapper();
		ObjectNode json = jsonUtil.getObjectMapper().valueToTree(response);

		// set image facet
		if (dataset.getThumbnailId() == null) {
			json.set("facet_image", json.arrayNode().add("nein"));
		} else {
			json.set("facet_image", json.arrayNode().add("ja"));
		}

		// add the geo facets
		// TODO check if this can be moved to the xmls to get more control over positioning of the facets
		String place = response.getFindSpot();
		String location = response.getFindSpotLocation();
		if (place != null && location != null) {
			json.set("facet_fundort", json.arrayNode().add(place + "[" + location + "]"));
		}

		place = response.getDepository();
		location = response.getDepositoryLocation();
		if (place != null && location != null) {
			json.set("facet_aufbewahrungsort", json.arrayNode().add(place + "[" + location + "]"));
		}

		// add all other facets
		final Element facets = document.getRootElement().getChild("facets", namespace);
		final List<Facet> facetList = getFacets(dataset, namespace, facets).getList();

		for (final Facet facet: facetList ) {
			final String facetName = facet.getName();
			final String facetOutputName = "facet_" + facetName;
			List<String> facetValues = facet.getValues();

			// split multi value facets at ';' and look for facet translations
			final List<String> finalFacetValues = new ArrayList<String>();
			for (final String facetValue: facetValues) {
				if (facetValue.contains(";")) {
					final List<String> splitValues = new ArrayList<String>(Arrays.asList(facetValue.split(";")));
					finalFacetValues.addAll(splitValues);
				} else {
					finalFacetValues.add(facetValue);
				}
			}

			ArrayNode arrayNode = json.arrayNode();
			for (final String finalFacetValue: finalFacetValues) {
				arrayNode.add(ts.transl8Facet(facetName, finalFacetValue));
			}
			json.set(facetOutputName, arrayNode);
		}
		return json;
	}
	
	/**
	 * Internal function to retrieve the contents of a <code>section</code> or <code>context</code>.
	 * @param dataset The current dataset.
	 * @param namespace The document namespace.
	 * @param element The DOM element to retrieve the content of.
	 * @return A list containing the content of the passed in element.
	 */
	private List<AbstractContent> getContentList(final Dataset dataset, final Namespace namespace, final Element element) {

		final List<AbstractContent> contentList = new ArrayList<AbstractContent>();
		
		final List<Element> children = element.getChildren();
		for (final Element currentElement:children) {
			if (currentElement.getName().equals("section")) {
				final Section section = (Section)xmlConfigUtil.getContentFromSections(currentElement, namespace, dataset);
				if (section != null && !section.getContent().isEmpty()) {
					contentList.add(section);
				}
			} else {
				final Section section = (Section)xmlConfigUtil.getContentFromContext(currentElement, namespace, dataset);
				if (section != null && !section.getContent().isEmpty()) {
					contentList.add(section);
				}
			}
		}

		if (!contentList.isEmpty()) {
			return contentList;
		}
		
		return null;
	}
	
	/**
	 * Converts a list of <code>AbstractContent</code> objects to a <code>string</code>.
	 * @param contentList The list to convert.
	 * @return The flattened representation of the list content.
	 */
	private String contentListToString(final List<AbstractContent> contentList) {
		if (contentList != null) {
			String result = contentList.toString();
			if (!result.isEmpty()) {
				result = result.substring(1, result.length() - 1);
			}
			return result;
		}
		return "";
	}
	
	/**
	 * This function retrieves the facets from the current config document and the corresponding values from the dataset.
	 * @param dataset The current dataset.
	 * @param facets The facet element of the current config file.
	 * @return A list of facets.
	 */
	private FacetList getFacets(final Dataset dataset, final Namespace namespace, final Element facets) {
		final FacetList result = new FacetList();
		final List<Element> children = facets.getChildren();
		for (final Element element:children) {
			if ("facet".equals(element.getName())) {
				final String name = element.getAttributeValue("name");
				final String labelKey = element.getAttributeValue("labelKey");
				final Facet facet = new Facet(name, labelKey);
				final Element child = (Element)element.getChildren().get(0); 
				if (child != null) {
					final List<String> values = new ArrayList<String>();
					final String childName = child.getName();
					if ("field".equals(childName)) {
						String value = dataset.getField(child.getAttributeValue("datasource"));
						if (value == null) {
							final StringBuilder ifEmtpyValue = xmlConfigUtil.getIfEmptyFromField(child, namespace, dataset);
							if (!StrUtils.isEmptyOrNull(ifEmtpyValue)) {
								value = ifEmtpyValue.toString();
							}
						}
						
						if (value != null) {
							values.add(value);
						}
					} else {
						if ("context".equals(childName)) {
							getFacetContext(dataset, child, values);
						}
					}
					if (!values.isEmpty()) {
						facet.setValues(values);
					}
					if (!facet.getValues().isEmpty()) {
						result.add(facet);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * This function retrieves the facets from a context element and the corresponding values from the dataset.
	 * @param dataset The current dataset.
	 * @param child The context element of the current facet element.
	 * @param values A list of facets to add the new facets to.
	 * @param name The name of the current facet.
	 */
	private void getFacetContext(final Dataset dataset, final Element child, final List<String> values) {
		
		final Section section = xmlConfigUtil.getContentFromContext(child, null, dataset);
		if (section != null) {
			for (final AbstractContent content:section.getContent()) {
				if (content instanceof FieldList) {
					for (final String value: ((FieldList)content).getValue()) {
						if (value != null) {
							values.add(value);
						}
					}
				} else {
					final String value = content.toString();
					if (value != null) {
						values.add(value);
					}
				}
			} 	 								
		}
	}
}