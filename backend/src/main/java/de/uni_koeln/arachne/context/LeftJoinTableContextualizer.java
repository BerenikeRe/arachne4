package de.uni_koeln.arachne.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.uni_koeln.arachne.response.ArachneDataset;
import de.uni_koeln.arachne.service.ArachneEntityIdentificationService;
import de.uni_koeln.arachne.service.GenericSQLService;
import de.uni_koeln.arachne.util.ArachneId;

/**
 * This is the baseclass for all contextualizers that get their contexts from
 * 'leftjoin tables', most likely only the <code>Ort-</code> and <code>DatierungContextualizers</code>.
 */
public abstract class LeftJoinTableContextualizer implements IContextualizer {

	private ArachneEntityIdentificationService arachneEntityIdentificationService;
	private GenericSQLService genericSQLService;
	
	protected String tableName;
	protected String joinTableName;
	
	
	/**
	 * Constructor setting the needed services.
	 */
	public LeftJoinTableContextualizer(ArachneEntityIdentificationService arachneEntityIdentificationService
			, GenericSQLService genericSQLService) {
		this.arachneEntityIdentificationService = arachneEntityIdentificationService;
		this.genericSQLService = genericSQLService;
	}
	
	@Override
	public String getContextType() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Long linkCount = 0l;
	
	@Override
	public List<Link> retrieve(ArachneDataset parent, Integer offset, Integer limit) {
		List<Link> result = new ArrayList<Link>();
		String parentTableName = parent.getArachneId().getTableName();
		List<Map<String, String>> contextContents = genericSQLService.getEntitiesById(joinTableName
				, parentTableName, parent.getArachneId().getInternalKey());
		
		if (contextContents != null) {
			ListIterator<Map<String, String>> contextMap = contextContents.listIterator(offset);
			while (contextMap.hasNext() && linkCount < limit) {
				Map<String, String> map = contextMap.next();
				ArachneLink link = new ArachneLink();
				ArachneDataset dataset = new ArachneDataset();
				String id = map.get(joinTableName + ".PS_" + Character.toUpperCase(tableName.charAt(0)) + tableName.substring(1) + "ID");
				// TODO check if ID not found exception is needed
				ArachneId arachneId = arachneEntityIdentificationService.getId(tableName, Long.parseLong(id));
				dataset.setArachneId(arachneId);
				// rename ortsbezug_leftjoin_ort to ort
				// this is how the contextualizer can set his own names
				Map<String, String> resultMap = new HashMap<String, String>();
				for (Map.Entry<String, String> entry: map.entrySet()) {
					String key = entry.getKey();
					if (!(key.contains("PS_") && key.contains("ID"))) {
						String newKey = tableName + "." + key.split("\\.")[1];
						resultMap.put(newKey, entry.getValue());
					}
				}
				dataset.appendFields(resultMap);
				link.setEntity1(parent);
				link.setEntity2(dataset);
				result.add(link);
			}
		}
		return result;
	}
}
