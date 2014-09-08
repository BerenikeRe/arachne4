package de.uni_koeln.arachne.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koeln.arachne.dao.GenericSQLDao;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.util.EntityId;

/**
 * This contextualizer retrieves internal contexts (tables in the arachne database). 
 */
public class SemanticConnectionsContextualizer implements IContextualizer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SemanticConnectionsContextualizer.class);
	
	/**
	 * The type of <code>Context<code> the <code>Contextualizer</code> retrieves.
	 */
	private transient final String contextType;
	
	private transient final GenericSQLDao genericSQLDao;
	
	private transient long linkCount = 0l;
	
	public SemanticConnectionsContextualizer(final String contextType, final GenericSQLDao genericSQLDao) {
		this.contextType = contextType;
		this.genericSQLDao = genericSQLDao;
	}
	
	@Override
	public String getContextType() {
		return contextType;
	}
	
	@Override
	public List<AbstractLink> retrieve(final Dataset parent) {
		final List<AbstractLink> result = new ArrayList<AbstractLink>();
		
		final long queryTime = System.currentTimeMillis();
		final List<Map<String, String>> contextContents = genericSQLDao.getConnectedEntities(contextType
				, parent.getArachneId().getArachneEntityID());
		LOGGER.debug("Query time: " + (System.currentTimeMillis() - queryTime) + " ms");		
		
		if (contextContents != null) {
			final ListIterator<Map<String, String>> contextMap = contextContents.listIterator();
			while (contextMap.hasNext()) {
				final ArachneLink link = new ArachneLink();
				link.setEntity1(parent);
				link.setEntity2(createDatasetFromQueryResults(contextMap.next()));
				result.add(link);
				linkCount++;
			}
		}
		return result;
	}

	/**
	 * Creates a new dataset which is a context from the results of an SQL query.
	 * @param map The SQL query result.
	 * @return The newly created dataset.
	 */
	private Dataset createDatasetFromQueryResults(final Map<String, String> map) {

		final Dataset result = new Dataset();
		
		long foreignKey = 0L;
		long eId = 0L;

		final Map<String, String> resultMap = new HashMap<String, String>();
		for (final Map.Entry<String, String> entry: map.entrySet()) {
			final String key = entry.getKey();
			if (!(key.contains("PS_") && key.contains("ID")) && !(key.contains("Source")) && !(key.contains("Type"))) {
				// get ArachneEntityID from context query result  
				if ("semanticconnection.Target".equals(key)) {
					eId = Long.parseLong(entry.getValue()); 
					continue;
				} else if ("semanticconnection.ForeignKeyTarget".equals(key)) {
					foreignKey = Long.parseLong(entry.getValue());
					continue;
				}  else if ("semanticconnection.ForeignKeyTarget".equals(key)) {
					foreignKey = Long.parseLong(entry.getValue());
					continue;
				} 

				resultMap.put(key, entry.getValue());
			}
		}

		final EntityId entityId = new EntityId(contextType, foreignKey, eId, false, 0L);
		result.setArachneId(entityId);
		result.appendFields(resultMap);
		return result;
	}
}
