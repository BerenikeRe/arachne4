package de.uni_koeln.arachne.response.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.uni_koeln.arachne.response.Place;

/**
 * Simple class to hold the data of one hit of the search result.
 */
@JsonInclude(Include.NON_NULL)
public class SearchHit {
	private transient final long entityId;
	private transient final String type;
	private transient final String title;
	private transient final String subtitle;
	private transient final Long thumbnailId;
	private transient final List<Place> places;
		
	public SearchHit(final long entityId, final String type, final String title, final String subtitle, final Long thumbnailId
			, List<Place> places) {
		this.entityId = entityId;
		this.type = type;
		this.title = title;
		this.subtitle = subtitle;
		this.thumbnailId = thumbnailId;
		this.places = places;
	}
	
	public long getEntityId() {
		return this.entityId;
	}
	
	public String getType() {
		return this.type;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getSubtitle() {
		return this.subtitle;
	}
	
	public Long getThumbnailId() {
		return this.thumbnailId;
	}
	
	public List<Place> getPlaces() {
		return places;
	}
}
