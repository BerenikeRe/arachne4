package de.uni_koeln.arachne.sqlutil;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import de.uni_koeln.arachne.mapping.UserAdministration;
import de.uni_koeln.arachne.service.IUserRightsService;
import de.uni_koeln.arachne.util.EntityId;

@RunWith(MockitoJUnitRunner.class) 
public class TestSQLFactory {
	
	@Mock private IUserRightsService mockUserRightsService;
	@InjectMocks private SQLFactory sqlFactory = new SQLFactory();
	
	@Before
	public void setUp() {
		final UserAdministration user = new UserAdministration();
		// set mock user name to 'INDEXING' to not trigger the SQL user rights snippet creation  
		user.setUsername(IUserRightsService.INDEXING);
		Mockito.when(mockUserRightsService.getCurrentUser()).thenReturn(user);
		// set custom SQL snippet
		Mockito.when(mockUserRightsService.getSQL(Mockito.anyString())).thenReturn("insertPermissionSQLhere");
	}
	
	@Test
	public void testGetSingleEntityQuery() {
		final EntityId entityId = new EntityId("test", Long.valueOf(27000), Long.valueOf(100),false);
			
		final String sqlQuery = sqlFactory.getSingleEntityQuery(entityId);
		
		assertTrue(sqlQuery.startsWith("SELECT * FROM `test` WHERE `test`.`PS_TestID` = 27000"));
		assertTrue(sqlQuery.contains("insertPermissionSQLhere"));
		assertTrue(sqlQuery.endsWith("LIMIT 1;"));
	}
	
	@Test
	public void testGetFieldByIdQuery() {
		final String sqlQuery = sqlFactory.getFieldByIdQuery("test", 1, "testfield");
		
		assertTrue(sqlQuery.startsWith("SELECT `test`.`testfield` FROM `test` WHERE `test`.`PS_TestID` = \"1\" AND "
				+ "`test`.`testfield` IS NOT NULL"));
		assertTrue(sqlQuery.contains("insertPermissionSQLhere"));
		assertTrue(sqlQuery.endsWith("LIMIT 1;"));
	}
	
	@Test
	public void testGetFieldQuery() {
		// primary key
		String sqlQuery = sqlFactory.getFieldQuery("test", "test", 1, "testfield", false);
		
		assertTrue(sqlQuery.startsWith("SELECT `test`.`testfield` FROM `test` WHERE `test`.`PS_TestID` = \"1\" AND "
				+ "`test`.`testfield` IS NOT NULL"));
		assertTrue(sqlQuery.contains("insertPermissionSQLhere"));
		assertTrue(sqlQuery.endsWith("LIMIT 1;"));
		
		// without authorization
		sqlQuery = sqlFactory.getFieldQuery("test", "test", 2, "testfield", true);
		
		assertTrue(sqlQuery.startsWith("SELECT `test`.`testfield` FROM `test` WHERE `test`.`PS_TestID` = \"2\" AND "
				+ "`test`.`testfield` IS NOT NULL"));
		assertFalse(sqlQuery.contains("insertPermissionSQLhere"));
		assertTrue(sqlQuery.endsWith("LIMIT 1;"));
		
		// foreign key
		sqlQuery = sqlFactory.getFieldQuery("test", "testkey", 3, "testfield", false);
		
		assertTrue(sqlQuery.startsWith("SELECT `test`.`testfield` FROM `test` WHERE `test`.`FS_TestkeyID` = \"3\" AND "
				+ "`test`.`testfield` IS NOT NULL"));
		assertTrue(sqlQuery.contains("insertPermissionSQLhere"));
		assertTrue(sqlQuery.endsWith("LIMIT 1;"));
	}
	
	@Test
	public void testgetConnectedEntitiesQuery() {
		String  sqlQuery = sqlFactory.getConnectedEntitiesQuery("test", 1);
		
		assertTrue(sqlQuery.startsWith("SELECT * FROM `SemanticConnection` LEFT JOIN `test` ON `test`.`PS_TestID` = "
				+ "`SemanticConnection`.`ForeignKeyTarget` WHERE Source = 1 AND TypeTarget = \"test\""));
		assertTrue(sqlQuery.endsWith("insertPermissionSQLhere;"));
	}
}
