package de.uni_koeln.arachne.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.uni_koeln.arachne.dao.ArachneEntityDao;
import de.uni_koeln.arachne.mapping.ArachneEntity;
import de.uni_koeln.arachne.util.ArachneId;


@Service("arachneEntityIdentificationService")
public class EntityIdentificationService {
	
	@Autowired
	private ArachneEntityDao arachneEntityDao;
	
	/**
	 * Gets all identifiers of a dataset by Arachne entity ID. This is the external reference ID for the dataset in Arachne.
	 * @param arachneEntityId
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	public ArachneId getId(Long arachneEntityId) {
		return getByEntityId(arachneEntityId);
	}
	
	/**
	 * Gets all identifiers of a dataset by Arachne entity ID. This is the external reference ID for the dataset in Arachne.
	 * @param arachneEntityId
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	public ArachneId getId(String table, Long id) {
		return getByTablenameAndInternalKey(table, id);
	}
	
	/**
	 * Gets all identifiers of a dataset by Arachne entity ID. This is the external reference ID for the dataset in Arachne.
	 * For convenience the public method <code>getArachneId</code> is overloaded.
	 * @param ArachneEntityID ArachneEntityID
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	private ArachneId getByEntityId(Long arachneEntityId) {
		return constructArachneID(arachneEntityDao.getByEntityID(arachneEntityId));
	}
	
	/**
	 * Gets all identifiers of a Dataset by tablename and primary key.
	 * For convenience the public method <code>getArachneId</code> is overloaded.
	 * @param table The internal table name in the Arachne database.
	 * @param id internal table key of the dataset
	 * @return an <code>ArachneId</code> object that contains all the identification information.
	 */
	private ArachneId getByTablenameAndInternalKey(String table, Long id){
		return constructArachneID(arachneEntityDao.getByTablenameAndInternalKey(table, id));
	}
	
	/**
	 * Constructs an <code>ArachneId</code> instance.
	 * @param arachneEntity The <code>ArachneEntity</code> for which the instance should be created.
	 * @return The new instance or <code>null</code>.
	 */
	private ArachneId constructArachneID(ArachneEntity arachneEntity){
		if (arachneEntity == null) {
			return null;	
		} else {
			return new ArachneId(arachneEntity.getTableName(), arachneEntity.getForeignKey(), arachneEntity.getId()
					, arachneEntity.isDeleted());
		}
		
	}
}
