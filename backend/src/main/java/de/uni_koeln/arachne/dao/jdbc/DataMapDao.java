package de.uni_koeln.arachne.dao.jdbc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import de.uni_koeln.arachne.mapping.jdbc.DatasetMapper;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.service.UserRightsService;
import de.uni_koeln.arachne.util.EntityId;
import de.uni_koeln.arachne.util.sql.SQLToolbox;
import de.uni_koeln.arachne.util.sql.SimpleTableEntityQueryBuilder;
import de.uni_koeln.arachne.util.sql.SingleEntitySubTablesQueryBuilder;
import de.uni_koeln.arachne.util.sql.TableConnectionDescription;
/**
 * Querys the DataBase and retrives the Result as Key/Value Map
 */
@Repository("arachneDataMapDao")
public class DataMapDao extends SQLDao {
		
	private static final Logger LOGGER = LoggerFactory.getLogger(DataMapDao.class);
	
	@Autowired
	private transient UserRightsService userRightsService;
	
	/**
	 * Gets a map of values by Id
	 * @param arachneId instance of <code>ArachneId</code> 
	 * @return a Simple representation of a Map<String,String> or <code>null</code>.
	 */
	public Map<String, String> getById(final EntityId arachneId) {			

		final String sql = getSingleEntityQuery(arachneId);

		LOGGER.debug(sql);

		final List<Map<String,String>> temp = (List<Map<String, String>>) this.query(sql, new DatasetMapper());
		if (temp != null && !temp.isEmpty()) {
			return filterEmptyStringValues(temp.get(0));
		}
		return null;
	}
	
	/**
	 * Gets a map of values by PrimaryKey and TableName
	 * @param primaryKey Primary key within given table
	 * @param tableName Tablename
	 * @return a Simple representation of a Map<String,String> or <code>null</code>.
	 */
	public Map<String, String> getByPrimaryKeyAndTable(final Integer primaryKey, final String tableName) {			

		final SimpleTableEntityQueryBuilder queryBuilder = new SimpleTableEntityQueryBuilder(tableName, primaryKey);
		final String sql = queryBuilder.getSQL();
		
		LOGGER.debug(sql);

		final List<Map<String,String>> temp = (List<Map<String, String>>) this.query(sql, new DatasetMapper());
		if (temp != null && !temp.isEmpty()) {
			return temp.get(0);
		}
		return null;
	}
	

	/**
	 * Gets a subdataset for a main dataset (Objekt -> Objektplastik) by using <code>ArachneSingleEntitySubTablesQueryBuilder</code> for query Building
	 * @param dataset Dataset for which the subdataset should be retrieved
	 * @param tableConnectionDescription instance of <code>TableConnectionDescription</code> which represents the Connection between the Dataset and the Subdataset 
	 * @return <code>Map<String,String></code> that contains the Description of the Subdataset, caution! The Subdataset is NOT automatically appended to the Dataset.
	 */
	public Map<String, String> getBySubDataset(final Dataset dataset, final TableConnectionDescription tableConnectionDescription ) {
		
		final SingleEntitySubTablesQueryBuilder queryBuilder = new SingleEntitySubTablesQueryBuilder(dataset
				,tableConnectionDescription);

		final String sql = queryBuilder.getSQL();
		LOGGER.debug(sql);
		final List<Map<String,String>> temp = (List<Map<String, String>>) this.query(sql, new DatasetMapper());

		if (temp == null || temp.isEmpty()) {
			return new HashMap<String,String>();
		} else {
			return filterEmptyStringValues(temp.get(0));
		}	
	}
	
	private String getSingleEntityQuery(final EntityId entityId) {
		final String tableName = entityId.getTableName();
		final StringBuilder result = new StringBuilder(256)
			.append("SELECT * FROM `")
			.append(tableName)
			
			.append("` WHERE ")
			.append(SQLToolbox.getQualifiedFieldname(tableName, SQLToolbox.generatePrimaryKeyName(tableName)))
			.append(" = ")
			.append(entityId.getInternalKey())
			.append(userRightsService.getSQL(tableName))
			.append(" LIMIT 1;");
		return result.toString();
	}
	
    /**
     * Filters out empty values. In the database there are some columns with one whitespace instead of NULL.
     * Those are removed from the query result.
     */
	private Map<String, String> filterEmptyStringValues(Map<String, String> resultMap) {
	    
	    for (Iterator<Map.Entry<String, String>> it = resultMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            if(entry.getValue().trim().equals("")) {
                it.remove();
            }
        }
	    
	    return resultMap;
	}
}
