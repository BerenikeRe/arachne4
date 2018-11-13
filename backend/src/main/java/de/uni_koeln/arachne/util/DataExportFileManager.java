package de.uni_koeln.arachne.util;

import de.uni_koeln.arachne.converters.DataExportException;
import de.uni_koeln.arachne.converters.DataExportTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DataExportFileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("DataExportLogger");

    @Value("${dataExportPath:'/tmp'}")
    private String dataExportPath;

    @PostConstruct
    public void init() {
        LOGGER.info("Tmp file path for data exports: " + dataExportPath);
    }

    public InputStream getFile(DataExportTask task) {

        final File file = new File(getFileName(task));

        byte[] fileContent = null;

        if(!file.exists()){
            throw new DataExportException("not_found", file.toString(), HttpStatus.NOT_FOUND);
        }

        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DataExportException("io_error", file.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public void deleteFile(DataExportTask task) {
        final Path path = Paths.get(getFileName(task));

        try {
            Files.delete(path);
        } catch (NoSuchFileException x) {
            throw new DataExportException("not_found", path.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException x) {
            throw new DataExportException("io_error", path.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public long getFileSize(DataExportTask task) {
        final String fileName = getFileName(task);
        try {
            return Files.size(Paths.get(fileName));
        } catch (IOException e) {
            throw new DataExportException("io_error", fileName, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String getFileName(DataExportTask task) {
        final String extension = task.getMediaType().getSubtype().toString();
        return dataExportPath + "/export-" + task.uuid.toString() + "." + extension;
    }

    public void writeToFile(DataExportTask task) throws Exception {
        try {
            final File file = new File(getFileName(task));
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            file.createNewFile();
            task.perform(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new DataExportException("io_error", getFileName(task), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
