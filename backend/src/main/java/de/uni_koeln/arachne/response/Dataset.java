package de.uni_koeln.arachne.response;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import de.uni_koeln.arachne.context.Context;
import de.uni_koeln.arachne.context.ArachneLink;
import de.uni_koeln.arachne.context.AbstractLink;
import de.uni_koeln.arachne.util.EntityId;
import de.uni_koeln.arachne.util.StrUtils;
/**
 * This class provides a low level Interface to Arachne Datasets.
 * Its Maps the core Infos to the Fields and stores the other Data in the Sections Map
 * It Also contains a List for The Images and annother Map for the Contexts
 *
 */
public class Dataset {
	// TODO change implementation to something more portable
	/**
	 * workaround for implementing getUri;
	 */
	private transient static final String BASEURI = "http://localhost:8080/arachnedataservice/entity/";	
	
	/**
	 * Identification of the Dataset.
	 */
	protected EntityId arachneId;
	
	/**
	 * The image to show as preview.
	 */
	protected Long thumbnailId;
	
	/**
	 * The Informations of the Dataset that is not in the core Dataset Definitions.
	 */
	protected transient Map<String,String> fields;
	
	/**
	 * The context map contains the contexts of the entity.
	 */
	protected List<Context> context;
	
	/**
	 * The Images that are asociated with the dataset.
	 */
	//protected List<ArachneImage> images;
	protected List<Image> images;
	
	/**
	 * Parameterless constructor.
	 */
	public Dataset() {
		fields = new Hashtable<String,String>();
		context = new ArrayList<Context>();
	}	
	
	/**
	 * Returns the unique Uri of the dataset.
	 * @return The unique Uri idenifying the dataset
	 */
	public String getUri() {
		if (arachneId.getArachneEntityID() == null) {
			return "Invalid Uri! Ask later!";
		} else {
			return BASEURI + arachneId.getArachneEntityID();
		}
	}
	
	//get methods
	public EntityId getArachneId() {
		return arachneId;
	}
	
	public List<Context> getContext() {
		return context;
	}
	
	public List<Image> getImages() {
		return images;
	}
	
	public Long getThumbnailId() {
		return thumbnailId;
	}
	
	/**
	 * This method returns the number of contexts of a given type. 
	 * <br>
	 * Side effect: If not all contexts are retrieved they will be retrieved now.
	 * @param contextType The type of the context of interest
	 * @return The number of context entities in this context
	 */
	public int getContextSize(final String contextType) {
		for (Context context: this.context) {
			if (context.getContextType().equals(contextType)) {
				return context.getContextSize();				
			}
		}
		return 0;
	}
	
	/**
	 * Looks up a field in the </code>fields<code> list	or in the contexts and returns its value. The 
	 * </code>fields<code> list is the preferred search location and only if a field is not found there the contexts are 
	 * searched.
	 * <br>
	 * "dataset" is a special contextualizer name that is used to reference data which is in every dataset (basically the <code>ArachneEntityId</code> object).
	 * This function returns these values, too, as it is faster than doing the look up again via the contextualizer mechanism.
	 * @param fieldName The full qualified fieldName to look up.
	 * @return The value of the field or <code>null<code/> if the field is not found.
	 */
	public String getField(final String fieldName) {
		String result = null;
		if (fieldName.startsWith("Dataset")) {
			// the magic number is the "dataset." char count
			final String unqualifiedFieldName = fieldName.substring(8);
			if ("Id".equals(unqualifiedFieldName)) {
				result = String.valueOf(arachneId.getArachneEntityID());
			} else {
				if ("internalId".equals(unqualifiedFieldName)) {
					result = String.valueOf(arachneId.getInternalKey());
				} else {
					if ("TableName".equals(unqualifiedFieldName)) {
						result = arachneId.getTableName();
					}
				}
			}
		} else {
			result = getFieldFromFields(fieldName);
			if (StrUtils.isEmptyOrNull(result)) {
				result = getFieldFromContext(fieldName);
			}
		}
		return result;
	}
	
	/**
	 * Looks up a field in the </code>fields<code> list and returns its value.
	 * @param fieldName The full qualified fieldName to look up.
	 * @return The value of the field or <code>null<code/> if the field is not found.
	 */
	public String getFieldFromFields(final String fieldName) {
		return fields.get(fieldName);
	}
	
	/**
	 * Looks up a field in the contexts and returns its value.
	 * @param fieldName The full qualified fieldName to look up.
	 * @return The value of the field or <code>null<code/> if the field is not found.
	 */
	public String getFieldFromContext(final String fieldName) {
		String result = null;
		for (Context context: this.context) {
			final ArachneLink link = (ArachneLink)context.getFirstContext();
			if (link != null) {
				// we know that Entity1 is 'this'
				result = link.getEntity2().getFieldFromFields(fieldName);
				if (!StrUtils.isEmptyOrNull(result)) {
					return result;
				}
			}
		}
		return null;
	}
	
	/**
	 * Looks up a field in the contexts and returns its value.
	 * @param fieldName The full qualified fieldName to look up.
	 * @return The value of the field or <code>null<code/> if the field is not found.
	 */
	public String getFieldFromContext(final String fieldName, final int index) {
		String result = null;
		for (Context context: this.context) {
			final ArachneLink link = (ArachneLink)context.getContext(index);
			if (link != null) {
				// we know that Entity1 is 'this'
				result = link.getEntity2().getFieldFromFields(fieldName);
				if (!StrUtils.isEmptyOrNull(result)) {
					return result;
				}
			}
		}
		return null;
	}
	
	/**
	 * Looks up a field in all contexts and returns their values as list.
	 * Currently only internal links are supported.
	 * @param fieldName The full qualified fieldName to look up.
	 * @return The value of the fields or <code>null<code/> if the field is not found.
	 */
	public List<String> getFieldsFromContexts(final String fieldName) {
		final List<String> result = new ArrayList<String>();
		for (Context context: this.context) {
			final List<AbstractLink> links = context.getallContexts();
			if (!links.isEmpty()) {
				for (AbstractLink link: links) {
					String tmpResult = null;
					// TODO add support for external links
					// we know that Entity1 is 'this'
					if (link instanceof ArachneLink) {
						final ArachneLink internalLink = (ArachneLink)link;
						tmpResult = internalLink.getEntity2().getFieldFromFields(fieldName);
					}
					if (!StrUtils.isEmptyOrNull(tmpResult)) {
						result.add(tmpResult);
					}
				}
			}
		}
		if (result.isEmpty()) {
			return null;
		} else {
			return result;
		}
	}
	
	// set methods
	public void setContext(final List<Context> context) {
		this.context = context;
	}
	
	public void setImages(final List<Image> images) {
		this.images = images;
	}
	
	public void setThumbnailId(final Long thumbnailId) {
		this.thumbnailId = thumbnailId;
	}
		
	public void addContext(final Context context) {
		this.context.add(context);
	}

	
	/**
	 * This Function sets a Single Section in the Sections Map
	 * @param fieldsLabel The Label of the Section Information
	 * @param fieldsValues The Value that this Section has
	 * @return returns false if the section value is overwritten true if the Section is new to the Object
	 */
	
	public boolean setFields(final String fieldsLabel, final String fieldsValues) {
		if (this.fields.containsKey(fieldsLabel)) {
			this.fields.put(fieldsLabel, fieldsValues);
			return false;
		} else {
			this.fields.put(fieldsLabel, fieldsValues);
			return true;
		}
	}
	
	public void appendFields(final Map<String, String> sections) {
		this.fields.putAll(sections);
	}

	public void setArachneId(final EntityId arachneId) {
		this.arachneId = arachneId;
	}
	
	@Override
	public String toString() {
		return fields + ", " + context;
	}
}
