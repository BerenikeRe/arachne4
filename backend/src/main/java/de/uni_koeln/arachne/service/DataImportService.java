package de.uni_koeln.arachne.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import de.uni_koeln.arachne.response.BaseArachneEntity;
import de.uni_koeln.arachne.response.ResponseFactory;
import de.uni_koeln.arachne.util.EntityId;

/**
 * This class implements the dataimport into elastic search. It is realized as a <code>@Service</code> so it can make use of autowiring and
 * be autowired itself (for communication). At the same time it implements <code>Runnable</code> so that the dataimport can run asynchronously
 * via a <code>TaskExecutor</code>.  
 */
@Service("DataImportService")
public class DataImportService implements Runnable { // NOPMD - Threading is used via Springs TaskExecutor so it is save 
	private static final Logger LOGGER = LoggerFactory.getLogger(DataImportService.class);
	
	@Autowired
	private transient IUserRightsService userRightsService;
	
	@Autowired
	private transient EntityIdentificationService entityIdentificationService;
	
	@Autowired
	private transient EntityService entityService;
	
	@Autowired
	private transient ResponseFactory responseFactory;
	
	private transient JdbcTemplate jdbcTemplate;
	
	protected transient DataSource dataSource;
	
	/**
	 * Through this function the datasource is injected
	 * @param dataSource An SQL Datasource
	 */
	@Autowired
	public void setDataSource(final DataSource dataSource) {
		this.dataSource = dataSource;		
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	private transient final AtomicLong elapsedTime;
	private transient final AtomicBoolean running;
	private transient final AtomicLong indexedDocuments;
	
	private transient final String esName;
	private transient final int esBulkSize;
	
	private transient final ObjectMapper mapper;
	private transient final Node node;
	private transient final Client client;
	
	private transient boolean terminate = false;
	
	@Autowired
	public DataImportService(final @Value("#{config.esName}") String esName, final @Value("#{config.esBulkSize}") int esBulkSize) {
		elapsedTime = new AtomicLong(0);
		running = new AtomicBoolean(false);
		indexedDocuments = new AtomicLong(0);
		this.esName = esName;
		this.esBulkSize = esBulkSize;
		LOGGER.info("Setting up elastic search client...");
		mapper = new ObjectMapper();
		node = NodeBuilder.nodeBuilder(). client(true).clusterName(esName).node();
		client = node.client();
	}

	/**
	 * The dataimport implementation. This method retrieves a list of EntityIds from the DB and iterates over this list constructing the
	 * associated documents and indexing them via elastic search.
	 */
	public void run() { // NOPMD - Threading is used via Springs TaskExecutor so it is save 
		// enable request scope hack- needed so the UserRightsService can be used
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
		terminate = false;
		running.set(true);
		indexedDocuments.set(0);		
		elapsedTime.set(0);
		final long startTime = System.currentTimeMillis();
		
		userRightsService.setUserSolr();
		LOGGER.info("Getting list of ArachneEntityIds.");
		final List<Long> entityIds = jdbcTemplate.query("select `ArachneEntityID` from `arachneentityidentification`", new RowMapper<Long>() {
			public Long mapRow(final ResultSet resultSet, final int index) throws SQLException {
				return resultSet.getLong(1);
			}
		});
		elapsedTime.set(System.currentTimeMillis() - startTime);		
		try {
			LOGGER.info("Dataimport started.");
			long deltaT = 0;
			long documentCount = 0;
			long bulkDocumentCount = 0;
			BulkRequestBuilder bulkRequest = client.prepareBulk();
			for (long currentEntityId: entityIds) {
				if (terminate) {
					bulkDocumentCount = 0;
					running.set(false);
					break;
				}
				
				final EntityId entityId = entityIdentificationService.getId(currentEntityId);
				BaseArachneEntity entity;
				if (entityId.isDeleted()) {
					entity = responseFactory.createResponseForDeletedEntity(entityId);
				} else {
					entity = entityService.getFormattedEntityById(entityId);
				}
				
				if (entity == null) {
					LOGGER.error("Entity " + entityId + " is null! This should never happen. Check the database immediately.");
				} else {
					bulkRequest.add(client.prepareIndex(esName,entity.getType(),String.valueOf(entityId.getArachneEntityID()))
							.setSource(mapper.writeValueAsBytes(entity)));
					bulkDocumentCount++;
				}
				
				// uodate elapsed time every second
				final long now = System.currentTimeMillis();
				if (now - deltaT > 1000) {
					deltaT = now;
					elapsedTime.set(now - startTime);
				}
								
				if (bulkDocumentCount >= esBulkSize) {
					documentCount = documentCount + bulkDocumentCount;
					bulkRequest.execute().actionGet();
					bulkRequest = client.prepareBulk();
					bulkDocumentCount = 0;
					indexedDocuments.set(documentCount);
				}
			}
			if (bulkDocumentCount > 0) {
				bulkRequest.execute().actionGet();
				documentCount = documentCount + bulkDocumentCount;
				indexedDocuments.set(documentCount);
			}
			if (running.get()) {
				LOGGER.info("Import of " + documentCount + " documents finished in " + ((System.currentTimeMillis() - startTime)/1000f/60f/60f) + " hours.");
			} else {
				LOGGER.info("Dataimport aborted.");
			}
		}
		catch (Exception e) {
			LOGGER.error("Dataimport failed with: " + e.toString());
		}
		// disable request scope hack
		((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).requestCompleted();
		RequestContextHolder.resetRequestAttributes();
		running.set(false);
	}
	
	/**
	 * Closes the elastic search node.
	 */
	@PreDestroy
	public void destroy() { 
		node.close();
	}
	
	/**
	 * Method to signal that the task shall stop.
	 */
	public void stop() {
		terminate = true;
	}
	
	public long getElapsedTime() {
		return elapsedTime.get();
	}
	
	public boolean isRunning() {
		return running.get();
	}
	
	public long getIndexedDocuments() {
		return indexedDocuments.get();
	}
}