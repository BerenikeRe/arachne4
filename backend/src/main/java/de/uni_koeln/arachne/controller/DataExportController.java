package de.uni_koeln.arachne.controller;

import de.uni_koeln.arachne.converters.DataExportException;
import de.uni_koeln.arachne.converters.DataExportStack;
import de.uni_koeln.arachne.converters.DataExportTask;
import de.uni_koeln.arachne.service.UserRightsService;
import de.uni_koeln.arachne.util.DataExportFileManager;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static de.uni_koeln.arachne.util.security.SecurityUtils.ADMIN;

/**
 * @author Paf
 */

@Controller
@RequestMapping("/export")
public class DataExportController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogController.class);

    @Autowired
    private transient UserRightsService userRightsService;

    @Autowired
    private transient DataExportStack dataExportStack;

    @Autowired
    private transient DataExportFileManager dataExportFileManager;

    @RequestMapping(value = "/file/{exportId}", method = RequestMethod.GET)
    public void handleGetExportFile(
            @PathVariable("exportId") final String exportId,
            @RequestHeader(value = "Accept-Language", defaultValue = "de") String headerLanguage,
            HttpServletResponse response
    ) {

        System.out.println("get file named " + exportId);

        final DataExportTask task = dataExportStack.getFinishedTaskById(exportId);

        if (task.getOwner().getId() != userRightsService.getCurrentUser().getId()) {
            throw new DataExportException("wrong_user", HttpStatus.FORBIDDEN, "DE"); // @ TODO right language
        }

        final InputStream fileStream = dataExportFileManager.getFile(task);
        final HttpHeaders headers = new HttpHeaders();
        response.setHeader("Content-Type", task.getMediaType().toString()+ "; charset=utf-8");
        response.setHeader("Content-Length", Long.toString(dataExportFileManager.getFileSize(task)));
        //response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));

        response.setStatus(HttpStatus.OK.value());
        try {
            IOUtils.copy(fileStream, response.getOutputStream());
            response.flushBuffer();
            dataExportStack.removeFinishedTask(task);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DataExportException("io_error", HttpStatus.INTERNAL_SERVER_ERROR, "DE"); // @ TODO right language
        }
    }


    @RequestMapping(value = "/status", method = RequestMethod.GET)
    ResponseEntity<String> handleGetExportStatus(
            @RequestHeader(value = "Accept-Language", defaultValue = "de") String headerLanguage
    ) {

        if (!userRightsService.userHasRole(ADMIN)) {
            throw new DataExportException("no_admin", HttpStatus.FORBIDDEN, headerLanguage);
        }

        return ResponseEntity.status(HttpStatus.OK).body(dataExportStack.getStatus().toString());
    }


    @RequestMapping(value = "/types", method = RequestMethod.GET)
    ResponseEntity<String> handleGetMediaTypes(
            @RequestHeader(value = "Accept-Language", defaultValue = "de") String headerLanguage
    ) {

        final JSONArray mediaTypes = new JSONArray();
        mediaTypes.put("csv");
        mediaTypes.put("html");
        mediaTypes.put("pdf");

        final JSONObject response = new JSONObject();
        response.put("catalog", mediaTypes);
        response.put("searchresults", mediaTypes);

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

}