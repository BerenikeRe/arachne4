package de.uni_koeln.arachne.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.uni_koeln.arachne.util.EntityId;

/**
 * Response class for deleted entities. This class is either serialized to JSON or XML.
 */
@XmlRootElement(name="entity")
public class DeletedArachneEntity extends BaseArachneEntity {
	private String message = "This entity has been deleted."; //NOPMD
			
	// little hack to remove the datasetGroup from the response. 
	@SuppressWarnings("unused")
	private transient String datasetGroup; // NOPMD
	
	@JsonIgnore
	@Override
	public String getDatasetGroup() {
		return "none";
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public void setMessage(final String message) {
		this.message = message;
	}
	
	public DeletedArachneEntity() {
		this.entityId = -1L;
		this.type = "unknown";
		this.internalId = 1L;
	}
	
	public DeletedArachneEntity(final EntityId entityId) {
		this.entityId = entityId.getArachneEntityID();
		this.type = entityId.getTableName();
		this.internalId = entityId.getInternalKey();
	}
}
