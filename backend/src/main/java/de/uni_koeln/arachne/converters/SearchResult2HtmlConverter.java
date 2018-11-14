package de.uni_koeln.arachne.converters;
import de.uni_koeln.arachne.response.search.SearchHit;
import de.uni_koeln.arachne.response.search.SearchResult;
import de.uni_koeln.arachne.response.search.SearchResultFacet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public class SearchResult2HtmlConverter extends BaseHtmlConverter<SearchResult> {

    @Override
    protected boolean supports(Class<?> aClass) {
        return aClass == SearchResult.class;
    }

    protected void writeInternal(SearchResult searchResult, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        enqueIfHuge(searchResult, 100);
        httpOutputMessage.getHeaders().add(HttpHeaders.CONTENT_TYPE, "text/html");
        httpOutputMessage.getHeaders().add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"currentSearch.html\"");
        convert(new DataExportConversionObject(searchResult), httpOutputMessage.getBody());
    }

    @Override
    public void convert(DataExportConversionObject conversionObject, OutputStream outputStream) throws IOException {
        final SearchResult searchResult = conversionObject.getSearchResult();
        this.writer = new OutputStreamWriter(outputStream);
        final List<SearchHit> entities = searchResult.getEntities();
        final List<SearchResultFacet> facets = searchResult.getFacets();
        initializeExport(transl8("search_result_for") + " " + task.getConversionName());
        htmlHeader();
        htmlFrontmatter();
        htmlResults(entities, facets);
        htmlFooter();
        writer.close();
    }
}
