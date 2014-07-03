package de.uni_koeln.arachne.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_koeln.arachne.dao.GenericSQLDao;
import de.uni_koeln.arachne.service.Transl8Service;
import de.uni_koeln.arachne.util.EntityId;
import de.uni_koeln.arachne.util.XmlConfigUtil;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(locations={"classpath:test-context.xml"}) 
public class TestResponseFactory { // NOPMD
	@Mock private GenericSQLDao genericSQLDao;
	@Mock private Transl8Service ts;
	@InjectMocks private ResponseFactory responseFactory = new ResponseFactory();
	
	private List<Long> mockIdList = null;
		
	private Dataset dataset = null;	
	
	@Before
	public void setUp() {
		final XmlConfigUtil xmlConfigUtil = new XmlConfigUtil();
		xmlConfigUtil.setServletContext(new MockServletContext());
		
		responseFactory.setXmlConfigUtil(xmlConfigUtil);
		
		dataset = new Dataset();
		
		dataset.setArachneId(new EntityId("test", 0L, 0L, false));
				
		dataset.setFields("test.Title", "Title of the Test");
		dataset.setFields("test.Subtitle", "Subtitle of the Test");
		
		dataset.setFields("test.DataPrefix", "success");
		dataset.setFields("test.DataPostfix", "PostfixTest");
		
		dataset.setFields("test.DataSeparatorBefore", "first");
		dataset.setFields("test.DataSeparatorAfter", "second");
		
		dataset.setFields("test.DataLink1", "http://testserver.com/link1.html");
		dataset.setFields("test.DataLink2", "link2");
		dataset.setFields("test.DataNoLink1", "Start");
		dataset.setFields("test.DataNoLink2", "End");
		
		dataset.setFields("test.facetTest", "test facet value");
		
		mockIdList = new ArrayList<Long>();
		for (long i = 1; i < 6; i++) {
			mockIdList.add(i);
		}
		Mockito.when(genericSQLDao.getConnectedEntityIds(0)).thenReturn(mockIdList);
		Mockito.when(ts.transl8(Mockito.anyString())).thenReturn("type_test");
		Mockito.when(ts.transl8Facet(Mockito.anyString(), Mockito.anyString())).then(AdditionalAnswers.returnsSecondArg());
	}
	
	@After
	public void tearDown() {
		responseFactory = null;
	}
	
	@Test
	public void testCreateFormattedArachneEntity() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertNotNull(response);
	}
	
	@Test
	public void testCreateResponseForDeletedEntity() throws JsonParseException, JsonMappingException, IOException {
		final EntityId deletedEntityId = new EntityId("test", 0L, 0L, true);
		final String response = responseFactory.createResponseForDeletedEntityAsJson(deletedEntityId);
		final ObjectMapper objectMapper = new ObjectMapper();
		DeletedArachneEntity deletedEntity = objectMapper.readValue(response, DeletedArachneEntity.class);
		assertNotNull(deletedEntity);
		assertEquals("test", deletedEntity.getType());
		assertEquals(Long.valueOf(0), deletedEntity.getEntityId());
		assertEquals(Long.valueOf(0), deletedEntity.getInternalId());
		assertEquals("This entity has been deleted.", deletedEntity.getMessage());
	}
	
	@Test
	public void testType() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"type\":\"type_test\""));
	}
	
	@Test
	public void testTitle() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"title\":\"Title of the Test\""));
	}
	
	@Test
	public void testSubtitle() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"subtitle\":\"Subtitle of the Test\""));
	}
	
	@Test
	public void testDatasectionLabel() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"label\":\"Testdata\""));
	}
	
	@Test
	public void testFieldPrefixPostfix() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"label\":\"Testdata prefix/postfix\""));
		assertTrue(response.contains("\"content\":[{\"value\":\"PrefixTest=success<br/>PostfixTest=success\"}]"));
	}
	
	@Test
	public void testFieldSeparator() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"label\":\"Testdata separator\""));
		assertTrue(response.contains("\"content\":[{\"value\":\"first-second\"}]"));
	}
	
	@Test
	public void testLinkField() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"label\":\"Testdata linkField\""));
		assertTrue(response.contains("\"content\":[{\"value\":\"Start<br/>"
				+ "<a href=\\\"http://testserver.com/link1.html\\\">TestLink1</a><br/>"
				+ "<a href=\\\"http://testserver.com/link2.html\\\">TestLink2</a><br/>End\"}]"));
	}
	
	@Test
	public void testDynamicFacets() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"facet_kategorie\": [\"test\"]"));
		assertTrue(response.contains("\"facet_test\": [\"test facet value\"]"));
	}
	
	@Test
	public void testStaticFacets() {
		final String response = responseFactory.createFormattedArachneEntityAsJson(dataset);
		assertTrue(response.contains("\"facet_image\": [\"nein\"]"));
	}
	
	// TODO add test for context tag - the current context implementation makes it nearly impossible to test
}
