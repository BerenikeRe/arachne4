package de.uni_koeln.arachne.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import de.uni_koeln.arachne.response.Dataset;

public class OrtgazetteerContextualizer extends AbstractContextualizer implements IContextualizer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrtgazetteerContextualizer.class);

	@Override
	public String getContextType() {
		return "gazetteer";
	}

	@Override
	public List<AbstractLink> retrieve(final Dataset parent, final Integer offset, final Integer limit) {
		
		final List<AbstractLink> result = new ArrayList<AbstractLink>();
		
		final ExternalLink link = new ExternalLink();
		link.setEntity(parent);
		
		final RestTemplate restTemplate = new RestTemplate();
		final String gazId = parent.getFieldFromContext("ort.Gazetteerid");
		LOGGER.debug("gazId: {}", gazId);
		try {
			final long queryTime = System.currentTimeMillis();
			final String doc = restTemplate.getForObject("http://gazetteer.dainst.org/doc/{gazId}.json", String.class, gazId);
			LOGGER.debug("Query time: " + (System.currentTimeMillis() - queryTime) + " ms");
			final JSONObject jsonObject = new JSONObject(doc);
			
			final Map<String,String> fields = new HashMap<String,String>();
			fields.put("ortgazetteer.prefName", jsonObject.getJSONObject("prefName").getString("title"));
			final JSONArray coords = jsonObject.getJSONObject("prefLocation").getJSONArray("coordinates");
			fields.put("ortgazetteer.lon", coords.getString(0));
			fields.put("ortgazetteer.lat", coords.getString(1));
			link.setFields(fields);
			
			result.add(link);
		} catch (JSONException e) {
			LOGGER.error("Error while parsing JSON!", e);
		} catch (HttpClientErrorException e) {
			LOGGER.warn("Unable to get gazetteer data for id: {}", gazId);
		}
		
		return result;
	}

}
