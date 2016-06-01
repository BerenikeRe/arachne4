package de.uni_koeln.arachne.util.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Class to hold and validate search parameters. As far as it is possible values are stored as the primitive types and 
 * unboxed in the setter methods which take the object types as parameters.
 * 
 * @author Reimar Grabowski
 */
@Component
@Scope("request")
public class SearchParameters {
	public static final int MAX_RESULT_WINDOW = 10000;
	
	public static int MAX_LIMIT = 1000;

	/**
	 * The query string.
	 */
	private String query;

	/**
	 * The maximum number of returned entities.
	 */
	private int limit;

	/**
	 * An offset into the search result.
	 */
	private int offset = 0;

	/**
	 * The maximum number of distinct facet values returned. Zero means that all values will be returned.
	 */
	private int facetLimit = 0;

	/**
	 * The name of the field to sort on.
	 */
	private String sortField = "";

	/**
	 * A flag indicating if the sort order is descending
	 */
	private boolean orderDesc = false;

	/**
	 * The coordinates (upper left, lower right) of a geo bounding box to filter the results on.
	 */
	private Double[] boundingBox = {};

	/**
	 * The precision of the geohash used in the geo grid aggregation.
	 */
	private int geoHashPrecision;

	/**
	 * 
	 */
	private List<String> facetsToSort = new ArrayList<String>();
	
	private boolean harvestMode = false;
	
	private boolean valid = true;
	
	/**
	 * Constructor that sets a default limit.
	 * @param defaultLimit The limit to set. Must be greater than -1.
	 */
	public SearchParameters(final int defaultLimit, final int defaultFacetLimit) {
		if (defaultLimit > -1) { 
			limit = defaultLimit;
		}
		if (defaultFacetLimit > -1) {
			facetLimit = defaultFacetLimit;
		}
	}
	
	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @param query the query to set
	 */
	public SearchParameters setQuery(String query) {
		this.query = query;
		return this;
	}

	/**
	 * @return the limit
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * @param limit the limit to set
	 */
	public SearchParameters setLimit(Integer limit) {
		if (limit != null && limit > -1) {
			this.limit = limit <= MAX_LIMIT ? limit : MAX_LIMIT;
		}
		return this;
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param offset the offset to set
	 */
	public SearchParameters setOffset(Integer offset) {
		if (offset != null && offset > 0) {
			this.offset = offset;
		}
		return this;
	}

	/**
	 * @return the facetLimit
	 */
	public int getFacetLimit() {
		return facetLimit;
	}

	/**
	 * @param facetLimit the facetLimit to set
	 */
	public SearchParameters setFacetLimit(Integer facetLimit) {
		if (facetLimit != null && facetLimit > -1) {
			this.facetLimit = facetLimit;
		}
		return this;
	}

	/**
	 * @return the sortField
	 */
	public String getSortField() {
		return sortField;
	}

	/**
	 * @param sortField the sortField to set
	 */
	public SearchParameters setSortField(String sortField) {
		if (sortField != null) {
			this.sortField = sortField;
		}
		return this;
	}

	/**
	 * @return the orderDesc
	 */
	public boolean isOrderDesc() {
		return orderDesc;
	}

	/**
	 * @param orderDesc the orderDesc to set
	 */
	public SearchParameters setOrderDesc(Boolean orderDesc) {
		if (orderDesc != null) {
			this.orderDesc = orderDesc;
		}
		return this;
	}

	/**
	 * @return the boundingBox
	 */
	public Double[] getBoundingBox() {
		return boundingBox;
	}

	/**
	 * @param boundingBox the boundingBox to set
	 */
	public SearchParameters setBoundingBox(Double[] boundingBox) {
		if (boundingBox != null && boundingBox.length == 4) {
			this.boundingBox = boundingBox;
		}
		return this;
	}

	/**
	 * @return the geoHashPrecision
	 */
	public int getGeoHashPrecision() {
		return geoHashPrecision;
		
	}

	/**
	 * @param geoHashPrecision the geoHashPrecision to set
	 */
	public SearchParameters setGeoHashPrecision(Integer geoHashPrecision) {
		// limit geohash precision to 10 as it is plenty of resolution
		if (geoHashPrecision != null && geoHashPrecision > 0 && geoHashPrecision < 11) {
			this.geoHashPrecision = geoHashPrecision;
		}
		return this;
	}

	public List<String> getFacetsToSort() {
		return this.facetsToSort;
	}
	
	/**
	 * @param facetsToSort the facets that should be sorted alphabetically
	 * @return this
	 */
	public SearchParameters setFacetsToSort(final String[] facetsToSort) {
		if (facetsToSort != null && facetsToSort.length > 0) {
			this.facetsToSort = Arrays.asList(facetsToSort);
		}
		return this;
	}

	public boolean isHarvestMode() {
		return harvestMode;
	}

	/**
	 * @param harvestMode if the complete search result will be available via the ES scroll API
	 * @return this
	 */
	public SearchParameters setHarvestMode(Boolean harvestMode) {
		if (harvestMode != null) {
			this.harvestMode = harvestMode;
		}
		return this;
	}
	
	/**
	 * Validates the current search parameters.
	 * @return The validity status of the search parameters.
	 */
	public boolean isValid() {
		if ((this.limit + this.offset > MAX_RESULT_WINDOW) && (harvestMode == false)) {
			valid = false;
		}
		return valid;
	}
}
