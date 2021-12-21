package de.uni_koeln.arachne.response.search;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Multimap;
import de.uni_koeln.arachne.util.search.SearchParameters;
import org.elasticsearch.rest.RestStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Class representing a search result. This class is the return type of a search request.
 */
@XmlRootElement
@JsonInclude(Include.NON_EMPTY)
public class SearchResult {

	private long size;
	
	private int limit;
	
	@JsonInclude(Include.NON_DEFAULT)
	private int offset;
	
	private String scrollId;
	
	private List<SearchResultFacet> facets;
	
	private List<SearchHit> entities;
	
	private RestStatus status;

	private Multimap<String,String> filters;

	private SearchParameters searchParameters;

	public SearchResult() {
		status = RestStatus.OK;
	}
	
	public void addSearchHit(final SearchHit searchHit) {
		if (searchHit == null) {
			return;
		}
		if (entities == null) {
			entities = new ArrayList<SearchHit>();
		}
		entities.add(searchHit);
	}
	
	// getter/setter

	public long getSize() {
		return size;
	}
	
	public void setSize(final long size) {
		this.size = size;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public void setLimit(final int limit) {
		this.limit = limit;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public void setOffset(final int offset) {
		this.offset = offset;
	}
	
	public String getScrollId() {
		return scrollId;
	}
	
	public void setScrollId(final String scrollId) {
		this.scrollId = scrollId;
	}
	
	public List<SearchResultFacet> getFacets() {
		return facets;
	}
	
	public void setFacets(final List<SearchResultFacet> facets) {
		this.facets = facets;
	}
	
	public List<SearchHit> getEntities() {
		return entities;
	}
	
	public void setEntities(final List<SearchHit> entities) {
		this.entities = entities;
	}

	@JsonIgnore
	public int facetSize() {
		if (facets != null && !facets.isEmpty()) {
			return facets.size();
		}
		return 0;
	}
	
	/**
	 * Getter for the <code>RestStatus</code> of the search request.
	 * @return The status.
	 */
	public RestStatus getStatus() {
		return status;
	}

	/**
	 * Setter for the <code>RestStatus</code> of the search request.
	 * @param status The status to set.
	 */
	public void setStatus(RestStatus status) {
		this.status = status;
	}

    public void setFilters(Multimap<String, String> filters) {
		this.filters = filters;
    }

	public void setSearchParameters(SearchParameters searchParameters) {
		this.searchParameters = searchParameters;
	}

	public Multimap<String, String> getFilters() {
		return filters;
	}

	public SearchParameters getSearchParameters() {
		return searchParameters;
	}
}
