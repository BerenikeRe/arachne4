package de.uni_koeln.arachne.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koeln.arachne.response.SearchResult;
import de.uni_koeln.arachne.response.StatusResponse;
import de.uni_koeln.arachne.service.GenericSQLService;
import de.uni_koeln.arachne.service.SearchService;


/**
 * Handles http requests (currently only get) for <code>/search<code>.
 */
@Controller
public class SearchController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);
	
	/**
	 * Maximum number of contexts fetched in one request from elasticsearch.
	 * Needed since URL parameters cannot be arbitrarily long and the context queries can easily get larger than
	 * 10000 characters.
	 */
	private static final int MAX_CONTEXT_QUERY_SIZE = 50;
	
	@Autowired
	private transient GenericSQLService genericSQLService; 
	
	@Autowired
	private transient SearchService searchService;
	
	private transient final List<String> defaultFacetList; 
	
	private transient final int defaultFacetLimit;
	
	private transient final int defaultLimit;
	
	@Autowired
	public SearchController(final @Value("#{config.esDefaultLimit}") int defaultLimit,
			final @Value("#{config.esDefaultFacetLimit}") int defaultFacetLimit,
			final @Value("#{config.esDefaultFacetList}") String defaultFacetListCS) {
		
		this.defaultLimit = defaultLimit;
		this.defaultFacetLimit = defaultFacetLimit;
		defaultFacetList = new ArrayList<String>(Arrays.asList(defaultFacetListCS.split(",")));
	}
	
	/**
	 * Handles the http request by querying the Elasticsearch index and returning the search result.
	 * The "title" field is boosted by 2 so that documents containing the search keyword in the title are higher ranked than
	 *  documents containing the keyword in other fields.
	 * <br>
	 * The return type of this method is <code>Object</code> so that it can return either a <code>SearchResult</code> or a <code>
	 * StatusMessage</code>.
	 * <br>
	 * Currently the search result can only be serialized to JSON as JAXB cannot handle Maps.
	 * @param searchParam The value of the search parameter. (mandatory)
	 * @param limit The maximum number of returned entities. (optional)
	 * @param offset The offset into the list of entities (used for paging). (optional)
	 * @return A response object containing the data or a status response (this is serialized to JSON; XML is not supported).
	 */
	@RequestMapping(value="/search", method=RequestMethod.GET, produces="application/json")
	public @ResponseBody Object handleSearchRequest(@RequestParam("q") final String searchParam,
													  @RequestParam(value = "limit", required = false) final Integer limit,
													  @RequestParam(value = "offset", required = false) final Integer offset,
													  @RequestParam(value = "fq", required = false) final String filterValues,
													  @RequestParam(value = "fl", required = false) final Integer facetLimit,
													  final HttpServletRequest request,
													  final HttpServletResponse response) {
		
		final int resultSize = limit == null ? defaultLimit : limit;
		final int resultOffset = offset == null ? 0 : offset;
		final int resultFacetLimit = facetLimit == null ? defaultFacetLimit : facetLimit;
		
		final List<String> facetList = new ArrayList<String>(defaultFacetList);
		final List<String> filterValueList = searchService.getFilterValueList(filterValues, facetList);
		
		final SearchRequestBuilder searchRequestBuilder = searchService.buildSearchRequest(searchParam, resultSize, resultOffset, filterValueList);
		searchService.addFacets(facetList, resultFacetLimit, searchRequestBuilder);
		
		final SearchResult searchResult = searchService.executeSearchRequest(searchRequestBuilder, resultSize, resultOffset, filterValues, facetList);
		
		if (searchResult == null) {
			return new StatusResponse("There was a problem executing the search. Please try again. If the problem persists please contact us.");
		} else {
			return searchResult;
		}
	}
	
	/**
	 * Handles the HTTP request by querying the elasticsearch index for contexts of a given entity and returning the result.
	 * <br>
	 * Since the queries can get quite large communication with elasticsearch may be split into multiple requests and
	 * the search result will be the sum of the responses. 
	 * <br> 
	 * Currently the search result can only be serialized to JSON as JAXB cannot handle Maps.
	 * @param entityId The id of the entity of interest. 
	 * @param limit The maximum number of returned entities. (optional)
	 * @param offset The offset into the list of entities (used for paging). (optional)
	 * @param filterValues The values of the solr filter query. (optional)
	 * @return A response object containing the data (this is serialized to JSON; XML is not supported).
	 */
	@RequestMapping(value="/contexts/{entityId}", method=RequestMethod.GET, produces="application/json")
	public @ResponseBody SearchResult handleContextRequest(@PathVariable("entityId") final Long entityId,
			@RequestParam(value = "limit", required = false) final Integer limit,
			@RequestParam(value = "offset", required = false) final Integer offset,
			@RequestParam(value = "fq", required = false) final String filterValues,
			@RequestParam(value = "fl", required = false) final Integer facetLimit,
			final HttpServletResponse response) {
		
		final int resultSize = limit == null ? 50 : limit;
		final int resultOffset = offset == null ? 0 : offset;
		final int resultFacetLimit = facetLimit == null ? defaultFacetLimit : facetLimit;
		
		SearchResult result = new SearchResult();
		final List<Long> contextIds = genericSQLService.getConnectedEntityIds(entityId);
		if (resultOffset >= contextIds.size()) {
			response.setStatus(400);
			return null;
		}
		// TODO filter entityId = 0
		System.out.println("Contexts: " + contextIds);
				
		final List<String> facetList = new ArrayList<String>(defaultFacetList);
		final List<String> filterValueList = searchService.getFilterValueList(filterValues, facetList);
		
		if (contextIds != null) { 
			if (contextIds.size() <= MAX_CONTEXT_QUERY_SIZE) {
				final String queryStr = getContextQueryString(0, contextIds.size() - 1, contextIds);
				LOGGER.debug("Context query: " + queryStr);
								
				final SearchRequestBuilder searchRequestBuilder = searchService.buildSearchRequest(queryStr, resultSize, resultOffset, filterValueList);
				searchService.addFacets(facetList, resultFacetLimit, searchRequestBuilder);
				result = searchService.executeSearchRequest(searchRequestBuilder, resultSize, resultOffset, filterValues, facetList);
			} else {
				final int requests = (contextIds.size() - 1) / MAX_CONTEXT_QUERY_SIZE;
							
				int start = resultOffset;
				int end = MAX_CONTEXT_QUERY_SIZE + resultOffset - 1;
				for (int i = 0; i <= requests; i++) {
					final String queryStr = getContextQueryString(start, end, contextIds);
					LOGGER.debug("Context multi query (" + i + " of " + requests + "): " + queryStr);
										
					final SearchRequestBuilder searchRequestBuilder = searchService.buildSearchRequest(queryStr, resultSize, 0, filterValueList);
					searchService.addFacets(facetList, resultFacetLimit, searchRequestBuilder);
					result.merge(searchService.executeSearchRequest(searchRequestBuilder, resultSize, resultOffset, filterValues, facetList));
					
					start = end + 1;
					end = end + MAX_CONTEXT_QUERY_SIZE;
					if (i == requests - 1) {
						end = contextIds.size() - 1;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Builds the elasticsearch context query string by specifying the connected Ids.
	 * @param start
	 * @param end
	 * @param contextIds
	 * @return
	 */
	private String getContextQueryString(final int start, final int end, final List<Long> contextIds) {
		
		final StringBuffer queryStr = new StringBuffer(16);
		queryStr.append("entityId:(");
		for (int i = start; i <= end; i++) {
			queryStr.append(contextIds.get(i));
			if (i < end) {
				queryStr.append(" OR ");
			} else {
				queryStr.append(')');
			}
		}
		return queryStr.toString();
	}
}
