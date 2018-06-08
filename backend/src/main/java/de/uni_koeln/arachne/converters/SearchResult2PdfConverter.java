package de.uni_koeln.arachne.converters;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import de.uni_koeln.arachne.response.search.SearchHit;
import de.uni_koeln.arachne.response.search.SearchResult;
import de.uni_koeln.arachne.response.search.SearchResultFacet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.*;
import java.util.List;

public class SearchResult2PdfConverter extends BasePdfConverter<SearchResult> {

    @Override
    protected boolean supports(Class<?> aClass) { return aClass == SearchResult.class; }

    @Override
    protected void writeInternal(SearchResult searchResult, HttpOutputMessage httpOutputMessage) throws IOException {
        abortIfHuge(searchResult, 50);
        httpOutputMessage.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/pdf");
        //httpOutputMessage.getHeaders().add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"currentSearch.pdf\""); // @ todo reactivate
        //OutputStream outStream = httpOutputMessage.getBody();
        convert(new DataExportConversionObject(searchResult), httpOutputMessage.getBody());
    }

    @Override
    public void convert(DataExportConversionObject conversionObject, OutputStream outputStream) throws IOException {
        final SearchResult searchResult = conversionObject.getSearchResult();
        try {
            final List<SearchHit> entities = searchResult.getEntities();
            final List<SearchResultFacet> facets = searchResult.getFacets();
            SearchResult2HtmlConverter htmlConverter = getHtmlConverter();
            htmlConverter.initializeExport("Search Result");// @  TODO transl8
            htmlConverter.writer = new StringWriter();
            htmlConverter.htmlHeader();
            htmlConverter.htmlFrontmatter();
            htmlConverter.htmlResults(entities, facets);
            htmlConverter.htmlFooter();
            writePdf((StringWriter) htmlConverter.writer, outputStream);
            htmlConverter.writer.close();
        } catch (Exception e) {
            e.printStackTrace(); // @ Todo
        }
    }



}
