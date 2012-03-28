package de.uni_koeln.arachne.sqlutil;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koeln.arachne.mapping.UserAdministration;

public class GenericFieldSQLQueryBuilder extends AbstractSQLBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenericFieldsSQLQueryBuilder.class);
	
	protected transient SQLRightsConditionBuilder rightsConditionBuilder;
	
	private transient String field2;
	
	/**
	 * Constructs a condition to query a field.
	 * @param tableName The name of the table of the query.
	 * @param field1 The field for which the id is given.
	 * @param field1Id The field Id.
	 * @param field2 The field to query.
	 */
	public GenericFieldSQLQueryBuilder(String tableName, String field1, Long field1Id, String field2, UserAdministration user) {
		sql = "";
		conditions = new ArrayList<Condition>(1);
		table = tableName;
		this.field2 = SQLToolbox.getQualifiedFieldname(table, field2);
		rightsConditionBuilder = new SQLRightsConditionBuilder(table,user);
		// The key identification condition
		final Condition keyCondition = new Condition();
		keyCondition.setOperator("=");
		if (field1.equals(tableName)) {
			keyCondition.setPart1(SQLToolbox.getQualifiedFieldname(table, SQLToolbox.generatePrimaryKeyName(field1)));
		} else {
			keyCondition.setPart1(SQLToolbox.getQualifiedFieldname(table, SQLToolbox.generateForeignKeyName(field1)));
		}
		keyCondition.setPart2(field1Id.toString());
		conditions.add(keyCondition);
		// The field2 not null condition
		final Condition notNullCondition = new Condition();
		notNullCondition.setOperator("IS NOT");
		notNullCondition.setPart1(SQLToolbox.getQualifiedFieldname(table, field2));
		notNullCondition.setPart2("NULL");
		conditions.add(notNullCondition);
	}
	
	@Override
	protected String buildSQL() {
		sql += "SELECT " + field2 + " FROM `" + table + "` WHERE 1";
		sql += this.buildAndConditions();
		sql += rightsConditionBuilder.getUserRightsSQLSnipplett();  
		sql += ";";
		LOGGER.debug(sql);
		return sql;
	}
}
