package de.uni_koeln.arachne.controller;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koeln.arachne.mapping.DatasetGroup;
import de.uni_koeln.arachne.response.SearchResult;
import de.uni_koeln.arachne.service.GenericSQLService;
import de.uni_koeln.arachne.service.UserRightsService;
import de.uni_koeln.arachne.util.StrUtils;
import de.uni_koeln.arachne.util.XmlConfigUtil;

/**
 * Handles http requests (currently only get) for <code>/search<code>.
 */
@Controller
public class SearchController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);
	
	@Autowired
	private GenericSQLService genericSQLService; // NOPMD
	
	@Autowired
	private UserRightsService userRightsService; // NOPMD
	
	@Autowired
	private transient XmlConfigUtil xmlConfigUtil;
	
	private transient final SolrServer server;
	
	@Autowired
	public SearchController(final @Value("#{config.solrProtocol}") String solrPotocol, final @Value("#{config.solrIp}") String solrIp,
			final @Value("#{config.solrPort}") int solrPort, final @Value("#{config.solrName}") String solrName) {
		
		SolrServer server = null;
		try {
			final String solrUrl = solrPotocol+"://"+solrIp+':'+solrPort+'/'+solrName;
			LOGGER.info("SolrUrl: " + solrUrl);
			if (StrUtils.isValidIPAddress(solrIp)) {
				server = new CommonsHttpSolrServer(solrUrl);
			} else {
				throw new MalformedURLException("solrIp " + solrIp + " is not a valid IP address.");
			}
		} catch (MalformedURLException e) {
			LOGGER.error("Setting up SolrServer: " + e.getMessage());
		}
		this.server = server;
	}
	
	/**
	 * Handles the http request by querying the Solr index and returning the search result.
	 * <br>
	 * Currently the search result can only be serialized to JSON as JAXB cannot handle Maps.
	 * @param searchParam The value of the search parameter. (mandatory)
	 * @param limit The maximum number of returned entities. (optional)
	 * @param offset The offset into the list of entities (used for paging). (optional)
	 * @param filterValues The values of the solr filter query. (optional)
	 * @param facetLimit The maximum number of facet results. (optional)
	 * @return A response object containing the data (this is serialized to XML or JSON depending on content negotiation).
	 */
	@RequestMapping(value="/search", method=RequestMethod.GET)
	public @ResponseBody SearchResult handleSearchRequest(@RequestParam("q") final String searchParam,
														  @RequestParam(value = "limit", required = false) final String limit,
														  @RequestParam(value = "offset", required = false) final String offset,
														  @RequestParam(value = "fq", required = false) final String filterValues,
														  @RequestParam(value = "fl", required = false) final String facetLimit) {
		
		final SearchResult result = new SearchResult();
		try {
			final SolrQuery query = new SolrQuery("*:*");
			final StringBuffer fullSearchParam = new StringBuffer(64);
			fullSearchParam.append('(');
			fullSearchParam.append(searchParam);
			appendAccessControl(fullSearchParam);
			LOGGER.debug("fullSearchParam: " + fullSearchParam);
			query.setQuery(fullSearchParam.toString());
		    // default value for limit
		    query.setRows(50);
		    query.setFacetMinCount(1);
		    // default facets to include
		    query.addFacetField("facet_kategorie");
		    query.addFacetField("facet_ort");
		    query.addFacetField("facet_datierung-epoche");
		    // add category specific facets
		    if (filterValues != null && filterValues.contains("facet_kategorie")) {
		    	final String category = getCategoryFromFilterValues(filterValues);
		    	final List<String> facets = xmlConfigUtil.getFacetsFromXMLFile(category);
		    	if (!StrUtils.isEmptyOrNull(facets)) {
		    		for (String facet: facets) {
		    			LOGGER.debug("adding: " + "facet_" + facet);
		    			query.addFacetField("facet_" + facet);
		    		}
		    	}
		    }
		    
		    query.setFacet(true);
		    		    		    
		    setSearchParameters(limit, offset, filterValues, facetLimit, result, query);
		    
		    final QueryResponse response = server.query(query);
		    result.setEntities(response.getResults());
		    result.setSize(response.getResults().getNumFound());
		    final Map<String, Map<String, Long>> facets = new LinkedHashMap<String, Map<String, Long>>();
		    
		    final List<FacetField> facetFields = response.getFacetFields();
		    for (FacetField facetField: facetFields) {
		    	final List<FacetField.Count> facetItems = facetField.getValues();
		    	final Map<String, Long> facetValueMap = new LinkedHashMap<String, Long>();
		    	if (facetItems != null) {
		    		for (FacetField.Count fcount: facetItems) {
		    			facetValueMap.put(fcount.getName(), fcount.getCount());
		    		}
		    		if (!facetValueMap.isEmpty()) {
		    			facets.put(facetField.getName(), facetValueMap);
		    		}
		    	}
		    }
		    
		    if (!facets.isEmpty()) {
		    	result.setFacets(facets);
		    }
		    
		} catch (SolrServerException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		}
			    
	    return result;
	}

	/**
	 * Parses the filter query parameter string and extracts the value for the key "facet_kategorie".
	 * @param filterValues
	 * @return The category name or <code>null</code> if none is found.
	 */
	private String getCategoryFromFilterValues(final String filterValues) {
		//String string = filterValues;
		if (filterValues.startsWith("facet_")) {
			final int beginIndex = filterValues.indexOf("facet_kategorie:") + 16;
			int endIndex = filterValues.indexOf(",facet", beginIndex);
			if (endIndex < 0) {
				endIndex = filterValues.length();
			}
			return filterValues.substring(beginIndex, endIndex);
		} else {
			return null;
		}
	}

	/**
	 * Handles the http request by querying the Solr index for contexts of a given entity and returning the result.
	 * <br>
	 * Since the queries can get quite large the HTTP POST method is used instead of GET to submit the query to Solr.
	 * Nonetheless the query may fail with <code>Bad request</code>. This indicates that the maximum number of boolean
	 * clauses is reached (although Solr should throw a <code>maxBoolean</code> exception it does not). The only way to 
	 * solve this problem is to increase the number of allowed boolean clauses in <code>solrconfig.xml</code>.
	 * <br> 
	 * Currently the search result can only be serialized to JSON as JAXB cannot handle Maps.
	 * @param entityId The id of the entity of interest. 
	 * @param limit The maximum number of returned entities. (optional)
	 * @param offset The offset into the list of entities (used for paging). (optional)
	 * @param filterValues The values of the solr filter query. (optional)
	 * @param facetLimit The maximum number of facet results. (optional)
	 * @return A response object containing the data (this is serialized to XML or JSON depending on content negotiation).
	 */
	@RequestMapping(value="/context/{entityId}", method=RequestMethod.GET)
	public @ResponseBody SearchResult handleContextRequest(@PathVariable("entityId") final Long entityId,
			@RequestParam(value = "limit", required = false) final String limit,
			@RequestParam(value = "offset", required = false) final String offset,
			@RequestParam(value = "fq", required = false) final String filterValues,
			@RequestParam(value = "fl", required = false) final String facetLimit) {
		
		final SearchResult result = new SearchResult();
		final List<Long> contextIds = genericSQLService.getConnectedEntityIds(entityId);
		
		if (contextIds == null) { 
			return new SearchResult();
		}
		
		try {
			final SolrQuery query = new SolrQuery("*:*");
			final StringBuffer queryStr = new StringBuffer(64);
			queryStr.append("(id:(");
			for (int i = 0; i < contextIds.size() ; i++) {
				queryStr.append(contextIds.get(i));
				if (i < contextIds.size() - 1) {
					queryStr.append(" OR ");
				} else {
					queryStr.append(')');
				}
			}
			appendAccessControl(queryStr);
			query.setQuery(queryStr.toString());
			// default value for limit
			query.setRows(50);
			query.setFacetMinCount(1);
			// default facets to include
			query.addFacetField("facet_kategorie");
			query.addFacetField("facet_ort");
			query.addFacetField("facet_datierung-epoche");
			// TODO add category specific facets based on info from where?
			query.setFacet(true);
			
			setSearchParameters(limit, offset, filterValues, facetLimit, result, query);

			final QueryResponse response = server.query(query, METHOD.POST);
			result.setEntities(response.getResults());
			result.setSize(response.getResults().getNumFound());
			final Map<String, Map<String, Long>> facets = new LinkedHashMap<String, Map<String, Long>>();
			
			final List<FacetField> facetFields = response.getFacetFields();
			for (FacetField facetField: facetFields) {
				final List<FacetField.Count> facetItems = facetField.getValues();
				final Map<String, Long> facetValueMap = new LinkedHashMap<String, Long>();
				if (facetItems != null) {
					for (FacetField.Count fcount: facetItems) {
						facetValueMap.put(fcount.getName(), fcount.getCount());
					}
					if (!facetValueMap.isEmpty()) {
						facets.put(facetField.getName(), facetValueMap);
					}
				}
			}

			if (!facets.isEmpty()) {
				result.setFacets(facets);
			}

		} catch (SolrServerException e) {
			LOGGER.error(e.getMessage());
		}
		
		return result;
	}

	/**
	 * Method to append access control based on the dataset groups of the current user.
	 * The <code>StringBuffer</code> given to this method must start with a <code>(</code>. 
	 * @param queryStr The query string to append the access control to.
	 */
	private void appendAccessControl(final StringBuffer queryStr) {
		queryStr.append(" AND (");
		boolean first = true;
		for (DatasetGroup datasetGroup: userRightsService.getCurrentUser().getDatasetGroups()) {
			if (first) {
				queryStr.append("datasetGroup:");
				first = false;
			} else {
				queryStr.append(" OR datasetGroup:");
			}
			queryStr.append(datasetGroup.getName());
		}			
		queryStr.append("))");
	}
	
	/**
	 * Helper function to set the search parameters for the Solr query. The first four parameters are the same as used
	 *  in handle search request.
	 *
	 * @param result The query result.
	 * @param query The query to set the parameters on.
	 */
	private void setSearchParameters(final String limit, final String offset,
			final String filterValues, final String facetLimit,
			final SearchResult result, final SolrQuery query) {
		
		if (!StrUtils.isEmptyOrNull(offset)) {
			final int intOffset = Integer.valueOf(offset);
			query.setStart(intOffset);
			result.setOffset(intOffset);
		}
		
		if (!StrUtils.isEmptyOrNull(limit)) {
			final int intLimit = Integer.valueOf(limit);
			query.setRows(intLimit);
			result.setLimit(intLimit);
		}
		
		if (!StrUtils.isEmptyOrNull(facetLimit)) {
			final int intFacetLimit = Integer.valueOf(facetLimit);
			query.setFacetLimit(intFacetLimit);
		}
		
		if (!StrUtils.isEmptyOrNull(filterValues)) {
			final List<String> filterValueList = filterQueryStringToStringList(filterValues); 
			if (!StrUtils.isEmptyOrNull(filterValueList)) {
				for (String filterValue: filterValueList) {
					query.addFilterQuery(filterValue);
				}
			}
		}
	}
	
	/**
	 * Converts the input string of query filter parameters to a string list of parameters.
	 * The string is split at every occurrence of ",facet_".
	 * @param filterString The filter query string to convert.
	 * @return a string list containing the separated parameters or <code>null</code> if the conversion fails.
	 */
	private List<String> filterQueryStringToStringList(final String filterString) {
		String string = filterString;
		if (string.startsWith("facet_")) {
			final List<String> result = new ArrayList<String>();
			int index = string.indexOf(",facet_");
			while (index != -1) {
				final String subString = string.substring(0, index);
				string = string.substring(index + 1);
				index = string.indexOf(",facet_");
				result.add(subString);
			}
			result.add(string);
			return result;
		} else {
			return null;
		}
	}
}
