package de.uni_koeln.arachne.sqlutil;

import java.util.List;

/**
 * this class is an abstract Implementation, it helps to construct SQL Statements to be used in the Arachne Database.
 * @author Rasmus Krempel
 *
 */
public abstract class AbstractSQLBuilder {
	
	protected transient String sql = "";
	
	protected transient String table = "";
	
	protected transient boolean limit1 = false;
	
	/**
	 * With And connected Conditions
	 */
	protected transient List<Condition> conditions;
	
	/**
	 * returns the build SQL query as <code>String</code>.
	 * @return the SQL query String.
	 */
	public String getSQL(){		
		buildSQL();
		return sql;
	}
	
	protected abstract String buildSQL();
	
	/**
	 * This Function concats all the Conditions to an SQL Snipplett for the Where Statement
	 * @return An SQL Snipplett with all the Condition as SQl Snipplett
	 */
	protected String buildAndConditions(){
		StringBuffer result = new StringBuffer("");

		for (Condition cnd : conditions) {
			result.append(" AND");
			result.append(cnd.toString());
		}

		return result.toString();	
	}
	
	protected String appendLimitOne(){
		if (limit1) {
			return " Limit 1";
		} else {
			return "";
		}
	}
}
