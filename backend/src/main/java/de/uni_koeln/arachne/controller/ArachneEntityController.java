package de.uni_koeln.arachne.controller;


import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.uni_koeln.arachne.mapping.DatasetGroup;
import de.uni_koeln.arachne.response.BaseArachneEntity;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.response.FormattedArachneEntity;
import de.uni_koeln.arachne.response.ResponseFactory;
import de.uni_koeln.arachne.service.ContextService;
import de.uni_koeln.arachne.service.EntityIdentificationService;
import de.uni_koeln.arachne.service.ImageService;
import de.uni_koeln.arachne.service.SingleEntityDataService;
import de.uni_koeln.arachne.service.IUserRightsService;
import de.uni_koeln.arachne.util.EntityId;
import de.uni_koeln.arachne.util.StrUtils;

/**
 * Handles http requests (currently only get) for <code>/entity<code> and <code>/data</code>.
 */
@Controller
public class ArachneEntityController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ArachneEntityController.class);
	
	@Autowired
	private transient EntityIdentificationService entityIdentificationService;

	@Autowired
	private transient SingleEntityDataService singleEntityDataService;
	
	@Autowired
	private transient ContextService contextService;
	
	@Autowired
	private transient ResponseFactory responseFactory;
	
	@Autowired
	private transient ImageService imageService;
		
	@Autowired
	private transient IUserRightsService userRightsService; 
	
	/**
	 * Handles http requests for /solr/{entityId}
	 * This mapping should only be used by Solr for indexing. It wraps the standard entity request but disables authorization.
	 * Requests are only allowed from the same IP-address as the Solr server configured in <code>src/main/resources/config/application.properties</code> 
	 * or from localhost (to make sure it works even if Solr is running on the same server as the backend). 
	 */
	@RequestMapping(value="/entity/solr/{entityId}", method=RequestMethod.GET)
	public @ResponseBody BaseArachneEntity handleSolrIndexingRequest(final HttpServletRequest request
			, @PathVariable("entityId") final Long entityId, final HttpServletResponse response, final @Value("#{config.solrIp}") String solrIp) {
		
		try {
			LOGGER.debug(request.getLocalAddr());
			LOGGER.debug(request.getRemoteAddr());
			if (StrUtils.isValidIPAddress(solrIp) && StrUtils.isValidIPAddress(request.getRemoteAddr())) {
				if (solrIp.equals(request.getRemoteAddr()) || request.getRemoteAddr().equals(request.getLocalAddr())) {
					LOGGER.debug("Valid Solr request.");
					userRightsService.setUserSolr();					
					return getEntityRequestResponse(entityId, null, response);
				} else {
					response.setStatus(403);
				}
			} else {
				throw new MalformedURLException("Invalid IP address.");
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}
	
	/**
	 * Handles http request for /{entityId}.
	 * Requests for /entity/* return formatted data. This will be sent out either as JSON or as XML. The response format is set 
	 * by Springs content negotiation mechanism.
	 * @param entityId The unique entity id of the item to fetch.
     * @return A response object containing the data (this is serialized to JSON or XML depending on content negotiation).
     */
	@RequestMapping(value="/entity/{entityId}", method=RequestMethod.GET)
	public @ResponseBody BaseArachneEntity handleGetEntityIdRequest(final HttpServletRequest request
			, @PathVariable("entityId") final Long entityId, final HttpServletResponse response) {
		return getEntityRequestResponse(entityId, null, response);
	}
    
    /**
     * Handles http request for /{category}/{id}
     * Requests for /entity/* return formatted data. This will be sent out either as JSON or as XML. The response format is set 
	 * by Springs content negotiation mechanism.
     * @param category The database table to fetch the item from.
     * @param categoryId The internal id of the item to fetch
     * @return A response object containing the data (this is serialized to JSON or XML depending on content negotiation).
     */
    @RequestMapping(value="/entity/{category}/{categoryId}", method=RequestMethod.GET)
    public @ResponseBody BaseArachneEntity handleGetCategoryIdRequest(@PathVariable("category") final String category
    		, @PathVariable("categoryId") final Long categoryId, final HttpServletResponse response) {
    	LOGGER.debug("Request for category: " + category + " - id: " + categoryId);
    	return getEntityRequestResponse(categoryId, category, response);
    }

    /**
     * Internal function handling all http GET requests for <code>/entity/*</code>.
     * It fetches the data for a given entity and returns it as a response object.
     * <br>
     * If the entity is not found a HTTP 404 error message is returned.
     * <br>
     * If the user does not have permission to see an entity a HTTP 403 status message is returned.
     * @param id The unique entity ID if no category is given else the internal ID.
     * @param category The category to query or <code>null</code>.
     * @param response The <code>HttpServeletRsponse</code> object.
     * @return A response object derived from <code>BaseArachneEntity</code>.
     */
    private BaseArachneEntity getEntityRequestResponse(final Long id, final String category //NOPMD
    		, final HttpServletResponse response) { 
    	final Long startTime = System.currentTimeMillis();
        
    	EntityId arachneId;
    	
    	if (category == null) {
    		arachneId = entityIdentificationService.getId(id);
    	} else {
    		arachneId = entityIdentificationService.getId(category, id);
    	}
    	
    	if (arachneId == null) {
    		response.setStatus(404);
    		return null;
    	}
    	
    	LOGGER.debug("Request for entity: " + arachneId.getArachneEntityID() + " - type: " + arachneId.getTableName());
    	
    	if (arachneId.isDeleted()) {
    		return responseFactory.createResponseForDeletedEntity(arachneId);
    	}
    	
    	final String datasetGroupName = singleEntityDataService.getDatasetGroup(arachneId);
    	final DatasetGroup datasetGroup = new DatasetGroup(datasetGroupName);
    	
    	LOGGER.debug("Is Solr indexer: " + userRightsService.isUserSolr());
    	if ((!userRightsService.isUserSolr()) && (!userRightsService.userHasDatasetGroup(datasetGroup))) {
    		response.setStatus(403);
    		return null;
    	}
    	
    	final Dataset arachneDataset = singleEntityDataService.getSingleEntityByArachneId(arachneId);
    	
    	final long fetchTime = System.currentTimeMillis() - startTime;
    	long nextTime = System.currentTimeMillis();
    	
    	imageService.addImages(arachneDataset);
    	
    	final long imageTime = System.currentTimeMillis() - nextTime;
    	nextTime = System.currentTimeMillis();
    	
    	contextService.addMandatoryContexts(arachneDataset);
    	
    	final long contextTime = System.currentTimeMillis() - nextTime;
    	nextTime = System.currentTimeMillis();
    	
    	final FormattedArachneEntity result = responseFactory.createFormattedArachneEntity(arachneDataset);
    	
    	LOGGER.debug("-- Fetching entity took " + fetchTime + " ms");
    	LOGGER.debug("-- Adding images took " + imageTime + " ms");
    	LOGGER.debug("-- Adding contexts took " + contextTime + " ms");
    	LOGGER.debug("-- Creating response took " + (System.currentTimeMillis() - nextTime) + " ms");
    	LOGGER.debug("-----------------------------------");
    	LOGGER.debug("-- Complete response took " + (System.currentTimeMillis() - startTime) + " ms");
    	return result;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Handles http request for /doc/{id}
     * Not implemented!
     */
    @RequestMapping(value="/doc/{entityId}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDocEntityRequest(@PathVariable("entityId") final Long entityId) {
    	// TODO implement me
    	return null;
    }

    /**
     * Handles http request for /doc/{category}/{id}
     * Not implemented!
     */
    @RequestMapping(value="doc/{category}/{categoryId}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDocCategoryIdRequest(@PathVariable("category") final String category
    		, @PathVariable("categoryId") final Long categoryId) {
    	// TODO implement me
		return null;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Handles http request for /data/{id}.
     * Requests for /data/* return the raw data.
     * Currently the dataset can only be returned as JSON since JAXB cannot handle maps.
     * @param entityId The unique entityID.
     * @param response The outgoing HTTP response.
     * @return The <code>Dataset</code> of the requested entity.
     */
    @RequestMapping(value="/data/{entityId}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDataEntityRequest(@PathVariable("entityId") final Long entityId, final HttpServletResponse response) {
    	return getDataRequestResponse(entityId, null, response);
    }
    
    /**
     * Handles http request for /data/{category}/{id}.
     * Requests for /data/* return the raw data.
     * Currently the dataset can only be returned as JSON since JAXB cannot handle maps.
     * @param categoryId The internal ID of the requested entity.
     * @param category The category to query.
     * @param response The outgoing HTTP response.
     * @return The <code>Dataset</code> of the requested entity
     */
    @RequestMapping(value="data/{category}/{categoryId}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDataCategoryIdRequest(@PathVariable("category") final String category
    		, @PathVariable("categoryId") final Long categoryId, final HttpServletResponse response) {
    	return getDataRequestResponse(categoryId, category, response);
    }
    
    /**
     * Internal function handling all http GET requests for <code>/data/*</code>.
     * It fetches the data for a given entity and returns the <code>Dataset</code>.
     * <br>
     * If the entity is not found a HTTP 404 error message is returned.
     * <br>
     * If the user does not have permission to see an entity a HTTP 403 status message is returned.
     * @param id The unique entity ID if no category is given else the internal ID.
     * @param category The category to query or <code>null</code>.
     * @param response The <code>HttpServeletRsponse</code> object.
     * @return The <code>Dataset</code> of the requested entity.
     */
    private Dataset getDataRequestResponse(final Long id, final String category, final HttpServletResponse response) { // NOPMD
    	final Long startTime = System.currentTimeMillis();
        
    	EntityId arachneId;
    	
    	if (category == null) {
    		arachneId = entityIdentificationService.getId(id);
    	} else {
    		arachneId = entityIdentificationService.getId(category, id);
    	}
    	
    	if (arachneId == null) {
    		response.setStatus(404);
    		return null;
    	}
    	
    	LOGGER.debug("Request for entity: " + arachneId.getArachneEntityID() + " - type: " + arachneId.getTableName());
    	
    	final String datasetGroupName = singleEntityDataService.getDatasetGroup(arachneId);
    	final DatasetGroup datasetGroup = new DatasetGroup(datasetGroupName);
    	
    	if (!userRightsService.userHasDatasetGroup(datasetGroup) || !(userRightsService.getCurrentUser().getGroupID()>=500)) {
    		response.setStatus(403);
    		return null;
    	}
    	
    	final Dataset arachneDataset = singleEntityDataService.getSingleEntityByArachneId(arachneId);
    	
    	final long fetchTime = System.currentTimeMillis() - startTime;
    	long nextTime = System.currentTimeMillis();
    	
    	imageService.addImages(arachneDataset);
    	
    	final long imageTime = System.currentTimeMillis() - nextTime;
    	nextTime = System.currentTimeMillis();
    	
    	// TODO find a way to handle contexts or discuss if we want to add them at all
    	//contextService.addMandatoryContexts(arachneDataset);
    	
    	final long contextTime = System.currentTimeMillis() - nextTime;
    	nextTime = System.currentTimeMillis();
    	
    	LOGGER.debug("-- Fetching entity took " + fetchTime + " ms");
    	LOGGER.debug("-- Adding images took " + imageTime + " ms");
    	LOGGER.debug("-- Adding contexts took " + contextTime + " ms");
    	LOGGER.debug("-----------------------------------");
    	LOGGER.debug("-- Complete response took " + (System.currentTimeMillis() - startTime) + " ms");
    	LOGGER.debug("Dataset: " + arachneDataset);    	
    	
    	return arachneDataset;
    }
}