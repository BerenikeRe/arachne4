package de.uni_koeln.arachne.converters;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import de.uni_koeln.arachne.mapping.jdbc.Catalog;
import de.uni_koeln.arachne.response.search.SearchHit;
import de.uni_koeln.arachne.response.search.SearchResultFacet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.List;

public class Catalog2PdfConverter extends BasePdfConverter<Catalog> {

    @Override
    protected boolean supports(Class<?> aClass) {
        return aClass == Catalog.class;
    }

    @Override
    protected void writeInternal(Catalog catalog, HttpOutputMessage httpOutputMessage) throws IOException {

        abortIfHuge(catalog, 50);

        httpOutputMessage.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/pdf");
        //httpOutputMessage.getHeaders().add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"currentSearch.pdf\"");

        OutputStream outStream = httpOutputMessage.getBody();

        SearchResult2HtmlConverter htmlConverter = getHtmlConverter();
        htmlConverter.initializeExport(catalog);
        htmlConverter.writer = new StringWriter();
        htmlConverter.htmlHeader();
        htmlConverter.htmlFrontmatter();
        htmlConverter.htmlCatalog(catalog);
        htmlConverter.htmlFooter();
        writePdf((StringWriter) htmlConverter.writer, outStream);
        htmlConverter.writer.close();

    }



}