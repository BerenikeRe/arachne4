package de.uni_koeln.arachne.controller;

import java.util.List;
import java.util.concurrent.TimeUnit; 

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.uni_koeln.arachne.response.StatusResponse;
import de.uni_koeln.arachne.service.DataImportService;
import de.uni_koeln.arachne.service.IUserRightsService;
import de.uni_koeln.arachne.service.UserRightsService;
import de.uni_koeln.arachne.util.StrUtils;
import de.uni_koeln.arachne.util.XmlConfigUtil;

/**
 * Handles http requests for <code>/admin<code>.
 * This includes requests for statuses (cache or dataimport) as well admin tasks (clearing the cache or starting a dataimport).
 */
@Controller
public class AdminController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
	
	@Autowired
	private transient IUserRightsService userRightsService;
	
	@Autowired
	private transient XmlConfigUtil xmlConfigUtil;
	
	@Autowired
	private transient DataImportService dataImportService;

	@Autowired
	private transient TaskExecutor defaultTaskExecutor;
	
	/**
	 * Handles HTTP GET requests to /admin/cache.   
	 * @param response The outgoing HTTP response.
	 * @return A <code>StatusResponse</code> containing the status of the XML configuration document cache or <code>null</code> on error-
	 */
	@RequestMapping(value="/admin/cache", method=RequestMethod.GET)
	public @ResponseBody StatusResponse getCacheStatus(final HttpServletResponse response) {
		
		LOGGER.debug("User GroupID: " + userRightsService.getCurrentUser().getGroupID());
		if (userRightsService.getCurrentUser().getGroupID() >= UserRightsService.MIN_ADMIN_ID) {
			final StringBuilder result = new StringBuilder("Cached documents:");
			final List<String> cachedDocuments = xmlConfigUtil.getXMLConfigDocumentList();
			if (cachedDocuments.isEmpty()) {
				result.append(" none");
			} else {
				for (final String document: cachedDocuments) {
					result.append(" " + document + ".xml");
				}
			}

			result.append(" - Cached include elements:");
			final List<String> cachedElements = xmlConfigUtil.getXMLIncludeElementList();
			if (cachedElements.isEmpty()) {
				result.append(" none");
			} else {
				for (final String element: cachedElements) {
					result.append(" " + element + "_inc.xml");
				}
			}

			return new StatusResponse(result.toString());
		}
		response.setStatus(403);
		return null;
	}
		
	/**
	 * Handles HTTP DELETE requests to /admin/cache.
	 * Deletes the cache.  
	 * @param response The outgoing HTTP response.
	 * @return A <code>StatusResponse</code> containing the status of the XML configuration document cache or <code>null<code> on error.
	 */
	@RequestMapping(value="/admin/cache", method=RequestMethod.DELETE)
	public @ResponseBody StatusResponse handleCache(final HttpServletResponse response) {
				
		LOGGER.debug("User GroupID: " + userRightsService.getCurrentUser().getGroupID());
		if (userRightsService.getCurrentUser().getGroupID() >= UserRightsService.MIN_ADMIN_ID) {
			xmlConfigUtil.clearCache();
			return new StatusResponse("Cache", "cleared");
		}
		response.setStatus(403);
		return null;
	}

	/**
	 * Handles HTTP GET requests to /admin/dataimport.
	 * Returns the current status of the Elasticsearch data import.
	 * @return A <code>StatusResponse</code> object.
	 */
	@RequestMapping(value="/admin/dataimport", method=RequestMethod.GET)
	public @ResponseBody StatusResponse getDataImportStatus() {
		
		if (dataImportService.isRunning()) {
			final long elapsedTime = dataImportService.getElapsedTime();
			final StatusResponse response = new StatusResponse("Dataimport", "running");
			response.setElapsedTime(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
					,TimeUnit.MILLISECONDS.toSeconds(elapsedTime) 
					- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))) + " minutes");
			long count = dataImportService.getCount();
			response.setCount(count);
			long indexedDocuments = dataImportService.getIndexedDocuments();
			response.setIndexedDocuments(indexedDocuments);
			if (elapsedTime > 0 && indexedDocuments > 0) {
				final long etr = (long)((count - indexedDocuments) / (float)(indexedDocuments/(float)elapsedTime));
				response.setEstimatedTimeRemaining(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(etr)
						,TimeUnit.MILLISECONDS.toSeconds(etr) 
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(etr))) + " minutes");
				response.setDocumentsPerSecond(indexedDocuments/((float)elapsedTime/1000));
			}
			return response;
		} else {
			return new StatusResponse("Dataimport", "idle");
		}
	}
	
	/**
	 * Handles HTTP POST requests to start or stop the Elasticsearch dataimport.
	 * For this it utilizes the <code>dataimportService</code> where the real work is done. 
	 * @param command The supported commands are "start" and "stop".
	 * @param response The outgoing HTTP response.
	 * @return A <code>StatusResponse</code> containing the current dataimport status or <code>null</code> on error.
	 */
	@RequestMapping(value="/admin/dataimport", method=RequestMethod.POST)
	public @ResponseBody StatusResponse handleDataImport(@RequestParam(value = "command", required = true) final String command
			, final HttpServletResponse response) {
		
		if (StrUtils.isEmptyOrNullOrZero(command)) {
			response.setStatus(400);
			return null;
		} else {
			if ("start".equals(command)) {
				if (dataImportService.isRunning()) {
					return new StatusResponse("Dataimport", "already running");
				} else {
					defaultTaskExecutor.execute(dataImportService);				
					return new StatusResponse("Dataimport", "started");
				}
			} else {
				if ("stop".equals(command)) {
					if (dataImportService.isRunning()) {
						dataImportService.stop();
						return new StatusResponse("Dataimport", "aborting");
					} else {
						return new StatusResponse("Dataimport", "not running");
					}
				}
			}
			return new StatusResponse("Dataimport", "unsupported command");
		}
	}
	
	// TODO find a better place for this function - maybe using java spring configuration
	@Scheduled(cron="0 0 0 * * *")
	private void scheduledDataimport() { // NOPMD 
		LOGGER.info("Starting scheduled dataimport...");
		defaultTaskExecutor.execute(dataImportService);
	}
}
