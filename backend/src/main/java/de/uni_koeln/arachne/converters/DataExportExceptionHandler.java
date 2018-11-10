package de.uni_koeln.arachne.converters;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class DataExportExceptionHandler {

    @ExceptionHandler(DataExportException.class)
    public ResponseEntity<String> handlerMyException(DataExportException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        String msg = "data_export_" + ex.type;
        msg += (!ex.untranslatableContent.equals("")) ? "|" + ex.untranslatableContent : "";
        return new ResponseEntity(msg, headers, ex.getHttpStatus());
    }

}