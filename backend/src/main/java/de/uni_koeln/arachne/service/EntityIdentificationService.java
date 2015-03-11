package de.uni_koeln.arachne.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.uni_koeln.arachne.dao.ArachneEntityDao;
import de.uni_koeln.arachne.mapping.ArachneEntity;
import de.uni_koeln.arachne.util.EntityId;


@Service("arachneEntityIdentificationService")
public class EntityIdentificationService {
	
	@Autowired
	private transient ArachneEntityDao arachneEntityDao; 
	
	/**
	 * Gets all identifiers of a dataset by Arachne entity ID. This is the external reference ID for the dataset in Arachne.
	 * @param arachneEntityId
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	public EntityId getId(final Long arachneEntityId) {
		return getByEntityId(arachneEntityId);
	}
		
	/**
	 * Gets a range of <code>ArachneEntityIds</code>.
	 * @param start First id in the range.
	 * @param end Last id in the range. 
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	public EntityId getId(final String tableName, final Long internalKey) {
		return getByTablenameAndInternalKey(tableName, internalKey);
	}
	
	/**
	 * Gets all identifiers of a dataset by table name and internal key. This is the external reference ID for the dataset in Arachne.
	 * @param tableName The name of the SQL table.
	 * @param internalKey The internalkey of the entity. 
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	public List<ArachneEntity> getByLimitedEntityIdRange(final long startId, final int limit) {
		return arachneEntityDao.getByLimitedEntityIdRange(startId, limit);
	}
	
	/**
	 * Gets all identifiers of a dataset by Arachne entity ID. This is the external reference ID for the dataset in Arachne.
	 * For convenience the public method <code>getId</code> is overloaded.
	 * @param entityId The Arachne entity ID.
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	private EntityId getByEntityId(final Long entityId) {
		return new EntityId(arachneEntityDao.getByEntityID(entityId));
	}
	
	/**
	 * Gets all identifiers of a Dataset by tablename and primary key.
	 * For convenience the public method <code>getId</code> is overloaded.
	 * @param tableName The internal table name in the Arachne database.
	 * @param internalKey internal table key of the dataset
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	private EntityId getByTablenameAndInternalKey(final String tableName, final Long internalKey){
		return new EntityId(arachneEntityDao.getByTablenameAndInternalKey(tableName, internalKey));
	}
}
