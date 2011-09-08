package de.uni_koeln.arachne.controller;


import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.uni_koeln.arachne.responseobjects.ArachneDataset;
import de.uni_koeln.arachne.responseobjects.FormattedArachneEntity;
import de.uni_koeln.arachne.service.ArachneEntityIdentificationService;
import de.uni_koeln.arachne.service.ArachneSingleEntityDataService;
import de.uni_koeln.arachne.service.UserRightsService;
import de.uni_koeln.arachne.util.ArachneId;

/**
 * Handles http requests (currently only get) for <code>/entity<code>.
 */
@Controller
public class ArachneEntityController {
	
	@Autowired
	ArachneEntityIdentificationService arachneEntityIdentificationService;

	@Autowired
	ArachneSingleEntityDataService arachneSingleEntityDataService;
	
	@Autowired
	UserRightsService userRightsService;
	
	/**
	 * Handles http request for /{id}
	 * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
	 * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */

	@RequestMapping(value="/entity/{id}", method=RequestMethod.GET)
	public @ResponseBody ArachneDataset handleGetEntityRequest(HttpServletRequest request, @PathVariable("id") Long id) {
		System.out.println(request);
		System.out.println(request.getRequestURL());
		
		userRightsService.initializeUserData();
		ArachneId temp = arachneEntityIdentificationService.getByEntityID(id);
		//JsonResponse response = new JsonResponse();
		//response.setItemId(temp.getInternalKey());
		ArachneDataset temp2 = arachneSingleEntityDataService.getSingleEntityByArachneId(temp);

		FormattedArachneEntity response = new FormattedArachneEntity();
		response.setId(temp2.getArachneId().getArachneEntityID());
		response.setCategory(temp2.getArachneId().getTableName());
		response.setCategoryId(temp2.getArachneId().getInternalKey());
		response.setContext(temp2.getContext());
		response.setImages(temp2.getImages());
		response.setLastModified(temp2.getLastModified());
		response.setSections(temp2.getSections());
		response.setTitle(temp2.getTitle());

		return temp2;
	}
    
    /**
     * Handles http request for /{category}/{id}
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
     * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */
    
    @RequestMapping(value="/entity/{category}/{id}", method=RequestMethod.GET)
    public @ResponseBody ArachneDataset handleGetCategoryIdRequest(@PathVariable("category") String category, @PathVariable("id") Long id) {
    		userRightsService.initializeUserData();
    		ArachneId temp = arachneEntityIdentificationService.getByTablenameAndInternalKey(category, id); 
            //JsonResponse response = new JsonResponse();
            //response.setItemId(temp.getInternalKey());
            ArachneDataset response = arachneSingleEntityDataService.getSingleEntityByArachneId(temp);
    		
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
    public @ResponseBody ArachneDataset handleGetDocEntityRequest(@PathVariable("id") Long id) {
    		userRightsService.initializeUserData();
    		ArachneId temp = arachneEntityIdentificationService.getByEntityID(id);
            //JsonResponse response = new JsonResponse();
            //response.setItemId(temp.getInternalKey());
            ArachneDataset response = arachneSingleEntityDataService.getSingleEntityByArachneId(temp);
    		
    		return response;
    }

    /**
     * Handles http request for /doc/{category}/{id}
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
     * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */
    
    @RequestMapping(value="doc/{category}/{id}", method=RequestMethod.GET)
    public @ResponseBody ArachneDataset handleGetDocCategoryIdRequest(@PathVariable("category") String category, @PathVariable("id") Long id) {
    		userRightsService.initializeUserData();
    		ArachneId temp = arachneEntityIdentificationService.getByEntityID(id);
            //JsonResponse response = new JsonResponse();
            //response.setItemId(temp.getInternalKey());
            ArachneDataset response = arachneSingleEntityDataService.getSingleEntityByArachneId(temp);
    		
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

    @RequestMapping(value="/data/{id}", method=RequestMethod.GET)
    public @ResponseBody ArachneDataset handleGetDataEntityRequest(@PathVariable("id") Long id) {
    		userRightsService.initializeUserData();
    		ArachneId temp = arachneEntityIdentificationService.getByEntityID(id);
            //JsonResponse response = new JsonResponse();
            //response.setItemId(temp.getInternalKey());
            ArachneDataset response = arachneSingleEntityDataService.getSingleEntityByArachneId(temp);
    		
    		return response;
    }
    
    /**
     * Handles http request for /doc/{category}/{id}
     * It uses the <Code>ItemService</Code> class to fetch the data and wraps it 
     * in a <Code>JsonResponse</Code> object.
     * @param itemId The id of the item to fetch
     * @return a JSON object containing the data
     */
    
    @RequestMapping(value="data/{category}/{id}", method=RequestMethod.GET)
    public @ResponseBody ArachneDataset handleGetDataCategoryIdRequest(@PathVariable("category") String category, @PathVariable("id") Long id) {
    		userRightsService.initializeUserData();
    		ArachneId temp = arachneEntityIdentificationService.getByEntityID(id);
            //JsonResponse response = new JsonResponse();
            //response.setItemId(temp.getInternalKey());
            ArachneDataset response = arachneSingleEntityDataService.getSingleEntityByArachneId(temp);
    		
    		return response;
    }
}