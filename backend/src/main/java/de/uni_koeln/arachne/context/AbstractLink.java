package de.uni_koeln.arachne.context;

import java.util.Map;

/**
 * Base class for holding links between URIs and additional information.
 *
 */
public abstract class AbstractLink {
	// TODO add correct documentation
	/**
	 * Holds the linkType as defined in ???.
	 */
	protected String linkType;
	
	/**
	 * The map holding the additional information.
	 */
	protected Map<String, String> fields;
	
	/**
	 * Function to retrieve the URI of the left side of the link.
	 */
	abstract String getUri1();	
	
	/**
	 * Function to retrieve the URI of the right side of the link.
	 */
	abstract String getUri2();
	
	public String getLinkType() {
		return linkType;
	}
	
	public void setLinkType(final String linkType) {
		this.linkType = linkType;
	}
	
	public Map<String, String> getFields() {
		return fields;
	}
	
	public void setFields(final Map<String, String> fields) {
		this.fields = fields;
	}
	
	/**
	 * Looks up a field in the <code>fields</code> list and returns its value.
	 * @param fieldName The full qualified fieldName to look up.
	 * @return The value of the field or <code>null</code> if the field is not found.
	 */
	public String getFieldFromFields(final String fieldName) {
		return fields.get(fieldName);
	}

	@Override
	public String toString() {
		return "AbstractLink [linkType=" + linkType + ", fields=" + fields
				+ " URI1: " + getUri1() + " URI2: " + getUri2() + "]";
	}
	
	
	
}