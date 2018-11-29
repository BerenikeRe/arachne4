package de.uni_koeln.arachne.util;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.uni_koeln.arachne.mapping.hibernate.ArachneEntity;

@XmlRootElement
public class EntityId {

	/**
	 * This is the outer Arachne entity ID.
	 */
	protected transient Long arachneEntityID;

	/**
	 * This is the internal table name of Arachne.
	 */
	protected transient String tableName;

	/**
	 * This is the internal key which is used inside the Arachne.
	 */
	protected transient Long internalKey;

	/**
	 * Flag indicating if the dataset exists or was deleted.
	 */
	protected transient boolean deleted;

	/**
	 * A measure for the number of entities this entity is connected to.
	 */
	protected transient Long degree;
	
	/**
	 * This Constructor gets Tablename and Internal key as Identification
	 * @param tableName String Tablename
	 * @param internalKey Long This is the 
	 * @param arachneEntityID Long Arachne Identification Number
	 * @param deleted is Deleted?
	 * @param degree The degree (a value for the 'quality' of an entity based on connected images and entities as well 
	 * as the number of fields).
	 */
	public EntityId(final String tableName, final Long internalKey, final Long arachneEntityID, final boolean deleted,
			final Long degree) {
		this.arachneEntityID = arachneEntityID;
		this.tableName = tableName;
		this.internalKey = internalKey;
		this.deleted = deleted;
		this.degree = degree;
	}
	
	/**
	 * This Constructor gets an ArachneEntity retrieved from hibernate and uses its information to construct
	 * an EntityId
	 * @param entity ArachneEntity Retrieved Hibernate-record from arachneentityidentification
	 */
	public EntityId(final ArachneEntity entity) {
		this.arachneEntityID = entity.getEntityId();
		this.tableName = entity.getTableName();
		this.internalKey = entity.getForeignKey();
		this.deleted = entity.isDeleted();
		this.degree = entity.getDegree();
	}
	
	/**
	 * Parameterless default constructor.
	 */
	public EntityId() {
		// just to make JAXB happy
	}

	@XmlElement
	public Long getInternalKey() {
		return internalKey;
	}
	
	@XmlElement
	public String getTableName() {
		return tableName;
	}

	@XmlElement
	public Long getArachneEntityID() {
		return arachneEntityID;
	}

	@XmlElement
	public boolean isDeleted() {
		return deleted;
	}

	@XmlElement
	@JsonIgnore
	public Long getDegree() {
		return degree;
	}
	
	@Override
	public String toString() {
		return "EntityId [arachneEntityID=" + arachneEntityID + ", tableName="
				+ tableName + ", internalKey=" + internalKey + ", deleted="
				+ deleted + "degree=" + degree + "]";
	}

	
}