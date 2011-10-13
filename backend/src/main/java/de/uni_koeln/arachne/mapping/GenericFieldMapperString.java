package de.uni_koeln.arachne.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class GenericFieldMapperString implements RowMapper<String> {
	@Override
	public String mapRow(ResultSet resultSet, int i) throws SQLException {
		return resultSet.getString(1);
	}
}
