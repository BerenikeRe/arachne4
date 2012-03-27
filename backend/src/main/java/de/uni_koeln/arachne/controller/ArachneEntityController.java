package de.uni_koeln.arachne.controller;


import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.uni_koeln.arachne.response.BaseArachneEntity;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.response.FailureResponse;
import de.uni_koeln.arachne.response.FormattedArachneEntity;
import de.uni_koeln.arachne.response.ResponseFactory;
import de.uni_koeln.arachne.service.ContextService;
import de.uni_koeln.arachne.service.EntityIdentificationService;
import de.uni_koeln.arachne.service.ImageService;
import de.uni_koeln.arachne.service.SingleEntityDataService;
import de.uni_koeln.arachne.util.ArachneId;

/**
 * Handles http requests (currently only get) for <code>/entity<code>.
 */
@Controller
public class ArachneEntityController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ArachneEntityController.class);
	
	@Autowired
	transient private EntityIdentificationService entityIdentificationService;

	@Autowired
	transient private SingleEntityDataService singleEntityDataService;
	
	@Autowired
	transient private ContextService contextService;
	
	@Autowired
	transient private ResponseFactory responseFactory;
	
	@Autowired
	transient private ImageService imageService;
	
	/**
	 * Handles http request for /{id}
	 * @param itemId The unique entity id of the item to fetch.
     * @return A response object containing the data (currently this a serialized to JSON by Jackson).
     */
	@RequestMapping(value="/entity/{entityId}", method=RequestMethod.GET)
	public @ResponseBody BaseArachneEntity handleGetEntityIdRequest(final HttpServletRequest request
			, @PathVariable("entityId") final Long entityId) {
		return getEntityRequestResponse(entityId, null);
	}
    
    /**
     * Handles http request for /{category}/{id}
     * @param category The database table to fetch the item from.
     * @param categoryId The internal id of the item to fetch
     * @return A response object containing the data (currently this a serialized to JSON by Jackson).
     */
    @RequestMapping(value="/entity/{category}/{categoryId}", method=RequestMethod.GET)
    public @ResponseBody BaseArachneEntity handleGetCategoryIdRequest(@PathVariable("category") final String category
    		, @PathVariable("categoryId") final Long categoryId) {
    	LOGGER.debug("Request for category: " + category + " - id: " + categoryId);
    	return getEntityRequestResponse(categoryId, category);
    }

    /**
     * Internal function handling all http GET requests for <code>/entity/*</code>.
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it in a response object.
     * @param id The unique entity ID if no category is given else the internal ID.
     * @param category The category to query or <code>null</code>.
     * @return A response object derived from <code>BaseArachneEntity</code>.
     */
    private BaseArachneEntity getEntityRequestResponse(final Long id, final String category) { //NOPMD
    	final Long startTime = System.currentTimeMillis();
        
    	ArachneId arachneId;
    	
    	if (category == null) {
    		arachneId = entityIdentificationService.getId(id);
    	} else {
    		arachneId = entityIdentificationService.getId(category, id);
    	}
    	
    	if (arachneId == null) {
    		LOGGER.debug("Warning: Missing ArachneEntityID");
    		return new FailureResponse("Failure! ArachneEntityID not found.");
    	}
    	LOGGER.debug("Request for entity: " + arachneId.getArachneEntityID() + " - type: " + arachneId.getTableName());
    	
    	final Dataset arachneDataset = singleEntityDataService.getSingleEntityByArachneId(arachneId);
    	
    	final long fetchTime = System.currentTimeMillis() - startTime;
    	long nextTime = System.currentTimeMillis();
    	
    	imageService.addImages(arachneDataset);
    	
    	final long imageTime = System.currentTimeMillis() - nextTime;
    	nextTime = System.currentTimeMillis();
    	
    	contextService.addMandatoryContexts(arachneDataset);
    	
    	final long contextTime = System.currentTimeMillis() - nextTime;
    	nextTime = System.currentTimeMillis();
    	
    	final FormattedArachneEntity response = responseFactory.createFormattedArachneEntity(arachneDataset);
    	
    	LOGGER.debug("-- Fetching entity took " + fetchTime + " ms");
    	LOGGER.debug("-- Adding images took " + imageTime + " ms");
    	LOGGER.debug("-- Adding contexts took " + contextTime + " ms");
    	LOGGER.debug("-- Creating response took " + (System.currentTimeMillis() - nextTime) + " ms");
    	LOGGER.debug("-----------------------------------");
    	LOGGER.debug("-- Complete response took " + (System.currentTimeMillis() - startTime) + " ms");
    	return response;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Handles http request for /doc/{id}
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
     * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */
    @RequestMapping(value="/doc/{id}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDocEntityRequest(@PathVariable("id") final Long id) {
    	// TODO implement me
    	return null;
    }

    /**
     * Handles http request for /doc/{category}/{id}
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
     * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */
    @RequestMapping(value="doc/{category}/{id}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDocCategoryIdRequest(@PathVariable("category") final String category
    		, @PathVariable("id") final Long id) {
    	// TODO implement me
		return null;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Handles http request for /doc/{id}
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
     * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */
    @RequestMapping(value="/data/{id}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDataEntityRequest(@PathVariable("id") final Long id) {
    	// TODO implement me
		return null;
    }
    
    /**
     * Handles http request for /doc/{category}/{id}
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
     * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */
    @RequestMapping(value="data/{category}/{id}", method=RequestMethod.GET)
    public @ResponseBody Dataset handleGetDataCategoryIdRequest(@PathVariable("category") final String category
    		, @PathVariable("id") final Long id) {
    	// TODO implement me
    	return null;
    }
}