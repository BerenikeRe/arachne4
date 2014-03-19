package de.uni_koeln.arachne.sqlutil;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectedEntityIdsSQLQueryBuilder extends AbstractSQLBuilder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedEntitiesSQLQueryBuilder.class);
	
	transient protected SQLRightsConditionBuilder rightsConditionBuilder;
	
	/**
	 * Constructs a condition to query a field.
	 * @param entityId The entityId of the object of interest.
	 */
	// TODO IMPORTANT - check if rights management must be enabled
	public ConnectedEntityIdsSQLQueryBuilder(final Long entityId) {
		super();
		conditions = new ArrayList<Condition>(1);
		//rightsConditionBuilder = new SQLRightsConditionBuilder(table, user);
		// The key identification condition
		final Condition keyCondition = new Condition();
		keyCondition.setOperator("=");
		keyCondition.setPart1("Source");
		keyCondition.setPart2(entityId.toString());
		conditions.add(keyCondition);
	}
	
	@Override
	protected void buildSQL() {
		final StringBuilder result = new StringBuilder(sql);
		result.append("SELECT `Target` FROM `SemanticConnection` WHERE NOT `Target` = 0");
		result.append(this.buildAndConditions());
		//sql += rightsConditionBuilder.getUserRightsSQLSnipplett();
		result.append(';');
		sql = result.toString();
		LOGGER.info(sql);
	}
}
