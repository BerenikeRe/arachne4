package de.uni_koeln.arachne.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import de.uni_koeln.arachne.mapping.DatasetMapper;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.service.UserRightsService;
import de.uni_koeln.arachne.sqlutil.SingleEntityQueryBuilder;
import de.uni_koeln.arachne.sqlutil.SingleEntitySubTablesQueryBuilder;
import de.uni_koeln.arachne.sqlutil.TableConnectionDescription;
import de.uni_koeln.arachne.util.ArachneId;
/**
 * Querys the DataBase and retrives the Result as Key/Value Map
 */
@Repository("arachneDataMapDao")
public class DataMapDao extends SQLDao {
		
	private static final Logger LOGGER = LoggerFactory.getLogger(DataMapDao.class);
	
	@Autowired
	private UserRightsService userRightsService; // NOPMD
	
	/**
	 * Gets a map of values by Id
	 * @param arachneId instance of <code>ArachneId</code> 
	 * @return a Simple representation of a Map<String,String> or <code>null</code>.
	 */
	public Map<String, String> getById(final ArachneId arachneId) {			

		final SingleEntityQueryBuilder queryBuilder = new SingleEntityQueryBuilder(arachneId,userRightsService.getCurrentUser());

		final String sql = queryBuilder.getSQL();
		
		LOGGER.debug(sql);

		@SuppressWarnings("unchecked")
		final List<Map<String,String>> temp = (List<Map<String, String>>) this.executeSelectQuery(sql, new DatasetMapper());
		if (temp != null) {
			if (!temp.isEmpty()) {
				Map<String,String> map =  temp.get(0);
				return map;
			}
		}
		return null;
	}

	/**
	 * Gets a subdataset for a main dataset (Objekt -> Objektplastik) by using <code>ArachneSingleEntitySubTablesQueryBuilder</code> for query Building
	 * @param dataset Dataset for which the subdataset should be retrieved
	 * @param tdesc instance of <code>TableConnectionDescription</code> which represents the Connection between the Dataset and the Subdataset 
	 * @return <code>Map<String,String></code> that contains the Description of the Subdataset, caution! The Subdataset is NOT automatically appended to the Dataset.
	 */
	public Map<String, String> getBySubDataset(Dataset dataset,TableConnectionDescription tdesc ) {

		SingleEntitySubTablesQueryBuilder qB = new SingleEntitySubTablesQueryBuilder(dataset,tdesc);

		String sql = qB.getSQL();
		LOGGER.debug(sql);
		@SuppressWarnings("unchecked")
		List<Map<String,String>> temp = (List<Map<String, String>>) this.executeSelectQuery(sql, new DatasetMapper());

		//achneDataset out= new  ArachneDataset();

		Map<String,String> map;  
		if (temp.isEmpty()) {
			map = new HashMap<String,String>();
		} else {
			map = temp.get(0);
		}
		return map;	
	}
}