package de.uni_koeln.arachne.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import de.uni_koeln.arachne.dao.GenericSQLDao;

@Service
public class GenericSQLService {
	@Autowired
	protected GenericSQLDao genericSQLDao; // NOPMD
	
	public List<Map<String, String>> getConnectedEntities(final String contextType, final Long entityId) {
		return genericSQLDao.getConnectedEntities(contextType, entityId);
	}
	
	public List<Long> getConnectedEntityIds(final Long entityId) {
		return genericSQLDao.getConnectedEntityIds(entityId);
	}
	
	public List<? extends SQLResponseObject> getStringFieldsEntityIdJoinedWithCustomRowmapper(final String tableName, final String field1
			, final Long field1Id, final List<String> fields, final RowMapper<? extends SQLResponseObject> rowMapper) {
		return genericSQLDao.getStringFieldsEntityIdJoinedWithCustomRowMapper(tableName, field1, field1Id, fields, rowMapper);
	}
}
