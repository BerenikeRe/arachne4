package de.uni_koeln.arachne.context;

import de.uni_koeln.arachne.response.Dataset;

/**
 * This class is used to retrieve images from contexts. These may be added to a dataset additionally or only if the record
 * itself doesn´t contain any images at all. 
 * @author Patrick Gunia
 *
 */

public class ContextImage extends Context {

	/** Describes the usage of images found in the context of the current record */
	private String usage = null;
	
	public ContextImage(final String contextType, final String usage, final Dataset parent) {
		super(contextType, parent);
		this.usage = usage;
	}

	public String getUsage() {
		return usage;
	}
	
	
}
