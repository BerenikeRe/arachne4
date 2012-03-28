package de.uni_koeln.arachne.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.uni_koeln.arachne.mapping.ImageRowMapper;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.response.Image;
import de.uni_koeln.arachne.util.EntityId;

/**
 * This service class provides the means to retrieve images from the database.
 */
@Service("ArachneImageService")
public class ImageService {
	@Autowired
	GenericSQLService genericSQLService; // NOPMD
	
	/**
	 * This method retrieves the images for a given dataset from the database and adds them to the datasets list
	 * of images. 
	 * @param dataset The dataset to add images to.
	 */
	public void addImages(final Dataset dataset) {
		final EntityId arachneId = dataset.getArachneId();
		final ArrayList<String> fieldList = new ArrayList<String>(2);
		fieldList.add("DateinameMarbilder");
		@SuppressWarnings("unchecked")
		final List<Image> imageList = (List<Image>) genericSQLService.getStringFieldsEntityIdJoinedWithCustomRowmapper("marbilder"
				, arachneId.getTableName(), arachneId.getInternalKey(), fieldList, new ImageRowMapper());
		dataset.setImages(imageList);
	}
}
