package de.uni_koeln.arachne.service;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.uni_koeln.arachne.context.*;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.util.XmlConfigUtil;

/**
 * This class handles creation and retrieval of contexts and adds them to datasets.
 * Internally it uses <code>Contextualizers</code> to abstract the data access and allow to fetch contexts not only from
 * the Arachne database but from any other datasource (even external ones).  
 */
@Service("arachneContextService")
public class ContextService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ContextService.class);
	
	@Autowired
	private transient EntityIdentificationService entityIdentificationService; 
	
	/**
	 * Service to access the 'Verknuepfungen' table. The information stored in that table is used
	 * to determine which contexts the <code>addContext</code> method adds to a given dataset.
	 */	
	@Autowired
	private transient ConnectionService arachneConnectionService; 
	
	/**
	 * Service to access ids in 'cross tables'.
	 */
	@Autowired
	private transient GenericSQLService genericSQLService; 
	
	@Autowired
	private transient SingleEntityDataService singleEntityDataService;
	
	/**
	 * Utility class to work with the XML config files.
	 */
	@Autowired
	private transient XmlConfigUtil xmlConfigUtil; 
	
	/**
	 * This methods adds all contexts to the dataset that are found in the XML description.
	 * @param parent The dataset to add the contexts to.
	 */
	public void addMandatoryContexts(final Dataset parent) {
		final List<String> mandatoryContextTypes = xmlConfigUtil.getMandatoryContextNames(parent.getArachneId().getTableName());
		LOGGER.debug("Mandatory Contexts: " + mandatoryContextTypes);
		final Iterator<String> contextType = mandatoryContextTypes.iterator();
		while (contextType.hasNext()) {
			final Context context = new Context(contextType.next(), parent);
			context.getallContexts();
			parent.addContext(context);
		}
	}
	
	/**
	 * Method to append all context objects to the given dataset.
	 * Some context objects are universal like 'literatur' or 'ort'. They get included
	 * for every dataset type. Other context objects are looked up based on the 'Verknuepfungen'
	 * table.  
	 * 
	 * @param parent ArachneDataset that will gain the added context
	 */
	public void addContext(final Dataset parent) {
		if (parent.getArachneId().getTableName().equals("bauwerk")) {
			final List<String> connectionList = arachneConnectionService.getConnectionList(parent.getArachneId().getTableName());
			final Iterator<String> iterator = connectionList.iterator();
			while (iterator.hasNext()) {
				final Context context = new Context(iterator.next(), parent);
				context.getFirstContext();
				parent.addContext(context);
			}
			
			final Context litContext = new Context("Literatur", parent);
			litContext.getFirstContext();
			parent.addContext(litContext);
		}
	}
	
	/**
	 * This function retrieves the contexts according to the given criteria.
	 * It uses a context specific contextualizer to fetch the data.
	 * @param parent Instance of an <code>ArachneDataset</code> that will receive the context
	 * @param contextType String that describes the context-type
	 * @param offset Starting position for context listing
	 * @param limit Quantity of contexts 
	 * @return Returns a list of <code>Links</code> 
	 */ 
	public List<AbstractLink> getLinks(final Dataset parent, final String contextType, final Integer offset, final Integer limit) {
		final IContextualizer contextualizer = getContextualizerByContextType(contextType);
	    return contextualizer.retrieve(parent, offset, limit);
	}
	
	/**
	 * Method creating an appropriate contextualizer. The class name is constructed from the <code>contextType</code>.
	 * Then reflection is used to create the corresponding class instance.
	 * <br>
	 * If no specialized <code>Contextualizer</code> class is found an instance of <code>GenericSQLContextualizer</code> is returned.
	 * @param contextType Type of a context of interest  
	 * @return an appropriate contextualizer serving the specific context indicated by the given <code>contextType</code>
	 */
	@SuppressWarnings("rawtypes")
	private IContextualizer getContextualizerByContextType(final String contextType) {
		try {
			final String upperCaseContextType = contextType.substring(0, 1).toUpperCase() + contextType.substring(1).toLowerCase();
			final String className = "de.uni_koeln.arachne.context." + upperCaseContextType + "Contextualizer";
			LOGGER.debug("Initializing class: " + className + "...");
			final Class<?> aClass = Class.forName(className);
			final java.lang.reflect.Constructor classConstructor = aClass.getConstructor();
			final AbstractContextualizer contextualizer = (AbstractContextualizer)classConstructor.newInstance();
			// set services
			contextualizer.setEntityIdentificationService(entityIdentificationService);
			contextualizer.setGenericSQLService(genericSQLService);
			contextualizer.setSingleEntityDataService(singleEntityDataService);
			return contextualizer;
		} catch (ClassNotFoundException e) {
			LOGGER.debug("FAILURE - using SemanticConnectionsContextualizer instead");
			return new SemanticConnectionsContextualizer(contextType, genericSQLService);
		}
		catch (Exception e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage());
		}
		return null;
	}
}
