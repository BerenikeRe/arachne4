package de.uni_koeln.arachne.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.ServletContextResource;

import de.uni_koeln.arachne.response.AbstractContent;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.response.Field;
import de.uni_koeln.arachne.response.FieldList;
import de.uni_koeln.arachne.response.Section;

/**
 * This class provides functions to find a XML config file by type, extract information based on the XML element from
 * the dataset and grants access to the servlet context.
 * If some class wants to work with the XML config files it should use this class via autowiring or as base class.
 */
@Component("xmlConfigUtil")
public class XmlConfigUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlConfigUtil.class);
	
	/**
	 * Servlet context to load the XML config files. 
	 */
	@Autowired
	private transient ServletContext servletContext;
	
	/**
	 * This function checks if a config file for the given type exists and returns its filename.
	 * @param type Type of the config to look for.
	 * @return The filename of the XML config file for the given type or <code>null</code>.
	 */
	public String getFilenameFromType(final String type) {
		String filename = "/WEB-INF/xml/"+ type + ".xml";
		final ServletContextResource file = new ServletContextResource(servletContext, filename);
		if (!file.exists()) {
			filename = "unknown";
		}
		
		LOGGER.debug("config file: " + filename);
		return filename;
	}
	
	/**
	 * This function handles sections in the xml config files. It extracts the content from the dataset following the definitions in the xml files
	 * and returns it as a <code>String</code>.
	 * <br>
	 * The validity of the xml file is not checked!!!
	 * @param section The xml section Element to parse.
	 * @param dataset The dataset that contains the SQL query results.
	 * @return A concatenated string containing the sections content.
	 */
	public String getStringFromSections(final Dataset dataset, final Namespace nameSpace, final Element section) {
		final StringBuffer result = new StringBuffer("");
		final List<Element> children = section.getChildren();
		String separator = "<br/>";
		if (section.getAttributeValue("separator") != null) {
			separator = section.getAttributeValue("separator");
		}

		for (Element e:children) {
			if (e.getName().equals("field")) {
				getFieldString(dataset, nameSpace, result, separator, e); 
			} else {
				final String datasetResult = getStringFromSections(dataset, nameSpace, e);
				if (!(result.length() < 1) && !datasetResult.isEmpty()) {
					result.append(separator);
				}
				result.append(datasetResult);
			}
		}
		return result.toString();
	}

	private void getFieldString(final Dataset dataset, final Namespace nameSpace, final StringBuffer result, final String separator
			, final Element element) {
		
		final String initialValue = dataset.getField(element.getAttributeValue("datasource"));
		StringBuffer datasetResult = null;
		if (initialValue != null) {
			datasetResult = new StringBuffer(initialValue);
		} 

		final String postfix = element.getAttributeValue("postfix");
		final String prefix = element.getAttributeValue("prefix");

		if (datasetResult == null) {
			datasetResult = getIfEmpty(dataset, nameSpace, element);
		}

		if (!StrUtils.isEmptyOrNull(datasetResult)) {
			if (prefix != null) {
				datasetResult.insert(0, prefix);
			}
			if (postfix != null) { 
				datasetResult.append(postfix);
			}
			if (!(result.length() < 1) && !(datasetResult.length() < 1)) {
				result.append(separator);
			}
			result.append(datasetResult);
		}
	}

	private StringBuffer getIfEmpty(final Dataset dataset, final Namespace nameSpace, final Element element) {
		
		String key;
		StringBuffer result = null;
		final Element ifEmptyElement = element.getChild("ifEmpty", nameSpace);
		if (ifEmptyElement != null) {
			// TODO discuss if multiple fields inside an ifEmpty tag make sense
			key = ifEmptyElement.getChild("field", nameSpace).getAttributeValue("datasource");
			if (key != null && !key.isEmpty()) {
				final String ifEmptyValue = dataset.getField(key);
				if (ifEmptyValue != null) {
					result = new StringBuffer(ifEmptyValue); 
				}
			}
		}
		return result;
	}
	
	/**
	 * This function handles sections in the xml config files. It extracts the content from the dataset following the 
	 * definitions in the xml files and returns it as <code>Content</code>.
	 * <br>
	 * The validity of the xml file is not checked!!!
	 * @param section The xml section <code>Element</code> to parse.
	 * @param dataset The dataset that contains the SQL query results.
	 * @return A <code>Content</code> object containing the sections content.
	 */
	public AbstractContent getContentFromSections(final Element section, final Dataset dataset, final int groupId) {
		final Section result = new Section();
		//TODO Get translated label string for value of labelKey-attribute in the section element  
		result.setLabel(section.getAttributeValue("labelKey"));
		
		final List<Element> children = section.getChildren();
		
		final String defaultSeparator = "<br/>";
		String separator = section.getAttributeValue("separator"); 
		if (section.getAttributeValue("separator") == null) {
			separator = defaultSeparator;
		}
		
		final String minGroupIdStr = section.getAttributeValue("minGroupId");
		if (!StrUtils.isEmptyOrNull(minGroupIdStr)) {
			final int minGroupId = Integer.parseInt(minGroupIdStr);
			LOGGER.debug(section.getAttributeValue("labelKey") + " minGroupId: " + minGroupId + " - user groupId: " + groupId);
			if (groupId < minGroupId) {
				return null;
			}
		}
						
		for (Element e:children) {
			if (e.getName().equals("field")) {
				getContentFromField(dataset, result, separator, e);
			} else {
				if (e.getName().equals("context")) {
					final Section nextSection = (Section)getContentFromContext(e, dataset, groupId);
					if (nextSection != null && !((Section)nextSection).getContent().isEmpty()) { 
						result.add(nextSection);
					}
				} else {
					final Section nextSection = (Section)getContentFromSections(e, dataset, groupId);
					if (nextSection != null && !((Section)nextSection).getContent().isEmpty()) { 
						result.add(nextSection);
					}
				}
			}
		}
		return result;
	}

	private void getContentFromField(final Dataset dataset,	final Section result, final String separator
			, final Element element) {
		
		final Field field = new Field();
		StringBuffer value = null;
		final String initialValue = dataset.getField(element.getAttributeValue("datasource"));
		if (initialValue != null) {
			value = new StringBuffer(initialValue);
		}
		final String postfix = element.getAttributeValue("postfix");
		final String prefix = element.getAttributeValue("prefix");
		if (value != null) {
			if (prefix != null) {
				value.insert(0, prefix);
			}
			if (postfix != null) {
				value.append(postfix); 
			}

			// TODO find better solution as the previous content may be a section
			// If there are more than one field in this section add the value (incl. separator) to the previous field
			if (result.getContent().isEmpty()) {
				field.setValue(value.toString());
				result.add(field);
			} else {
				final int contentSize = result.getContent().size();
				final Field previousContent = (Field)result.getContent().get(contentSize-1);
				previousContent.setValue(previousContent.getValue() + separator + value);
			}
		}
	}
	
	/**
	 * This function handles context elements in the xml config files. It extracts the content from the dataset 
	 * following the definitions in the xml files and returns it as <code>Content</code>.
	 * <br>
	 * The validity of the xml file is not checked!!!
	 * @param context The xml context <code>Element</code> to parse.
	 * @param dataset The dataset that contains the SQL query results.
	 * @return A <code>Content</code> object containing the context sections content.
	 */
	public Section getContentFromContext(final Element context, final Dataset dataset, final int groupId) {
		final Section result = new Section();
		final String contextType = context.getAttributeValue("type");
		//TODO Get translated label string for value of labelKey-attribute in the section element  
		result.setLabel(context.getAttributeValue("labelKey"));
		
		String parentSeparator = null;
		if (context.getParentElement().getName().equals("section")) {
			parentSeparator = context.getParentElement().getAttributeValue("separator");
		}
		if (parentSeparator == null) {
			parentSeparator = "<br/>";
		}
		
		final List<Element> children = context.getChildren();
		final String defaultSeparator = "<br/>";
		String separator = context.getAttributeValue("separator"); 
		if (context.getAttributeValue("separator") == null) {
			separator = defaultSeparator;
		}
				
		final FieldList fieldList = new FieldList();
		for (int i = 0; i < dataset.getContextSize(contextType); i++) {
			getFields(dataset, contextType, children, separator, fieldList, i);
		}
		
		if (fieldList.size() > 1) {
			result.add(fieldList);
		} else {
			if (fieldList.size() == 1 ) {
				final Field field = new Field();
				field.setValue(fieldList.get(0));
				result.add(field);
			}
		}
		
		if (result.getContent().isEmpty()) {
			return null;
		}
		
		return result;
	}
	
	public Section getContentFromContext(final Element context, final Dataset dataset) {
		return getContentFromContext(context, dataset, -1);
	}

	private void getFields(final Dataset dataset, final String contextType,	final List<Element> children
			, final String separator, final FieldList fieldList, final int index) {
		
		for (Element e: children) {
			if (e.getName().equals("field")) {
				getField(dataset, contextType, separator, fieldList, index, e);
			}
		}
	}

	private void getField(final Dataset dataset, final String contextType, final String separator
			, final FieldList fieldList, final int index, final Element element) {
		
		final String initialValue = dataset.getFieldFromContext(contextType + element.getAttributeValue("datasource"), index);
		StringBuffer value = null;
		if (initialValue != null) {
			value = new StringBuffer(initialValue);
		}
		final String postfix = element.getAttributeValue("postfix");
		final String prefix = element.getAttributeValue("prefix");
		if (value != null) {
			if (prefix != null) {
				value.insert(0, prefix);
			}
			if (postfix != null) {
				value.append(postfix); 
			}
			String currentListValue = null;
			if (!fieldList.getValue().isEmpty() && index < fieldList.size()) {
				currentListValue = fieldList.get(index);
			}
			if (currentListValue == null) {
				fieldList.add(value.toString());
			} else {
				fieldList.modify(index, currentListValue + separator + value);
			}
		}
	}
	
	public List<String> getFacetsFromXMLFile(final String category) {
		final String filename = getFilenameFromType(category);
		if ("unknown".equals(filename)) {
			return null;
		}
		
		final List<String> facetList = new ArrayList<String>();
		
		final ServletContextResource xmlDocument = new ServletContextResource(getServletContext(), filename);
	    try {
	    	final SAXBuilder saxBuilder = new SAXBuilder();
	    	final Document doc = saxBuilder.build(xmlDocument.getFile());
	    	//TODO Make Nicer XML Parsing is very quick and Dirty solution for my Problems 
	    	final Namespace nameSpace = Namespace.getNamespace("http://arachne.uni-koeln.de/schemas/category");
	    	
			// Get facets
 			final Element facets = doc.getRootElement().getChild("facets", nameSpace);
 			for (Element e: facets.getChildren()) {
 				facetList.add(e.getAttributeValue("name")); 				
 			}
 			return facetList;
		} catch (JDOMException e) {
			LOGGER.error(e.getMessage());
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}
	
	public ServletContext getServletContext() {
		return servletContext;
	}
}
