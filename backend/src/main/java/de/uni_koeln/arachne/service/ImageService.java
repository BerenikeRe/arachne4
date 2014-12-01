package de.uni_koeln.arachne.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.uni_koeln.arachne.dao.GenericSQLDao;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.response.Image;
import de.uni_koeln.arachne.util.EntityId;
import de.uni_koeln.arachne.util.image.ImageComparator;
import de.uni_koeln.arachne.util.image.ImageUtils;

/**
 * This service class provides the means to retrieve images from the database.
 */
@Service("ArachneImageService")
public class ImageService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);
	
	@Autowired
	private transient GenericSQLDao genericSQLDao; 
	
	private transient final List<String> excludeList;
	
	@Autowired
	public ImageService(final @Value("#{config.imageExcludeList}") String imageExcludeListCS) {
		excludeList = new ArrayList<String>(Arrays.asList(imageExcludeListCS.split(",")));
	}
	
	/**
	 * This method retrieves the image ids for a given dataset from the database and adds them to the datasets list
	 * of images. It also finds the preview thumbnail from this list and adds it to the dataset.    
	 * @param dataset The dataset to add images to.
	 */
	public void addImages(final Dataset dataset) {
		final EntityId arachneId = dataset.getArachneId();
		if (excludeList.contains(arachneId.getTableName())) {
			LOGGER.debug("excluding " + arachneId.getTableName());
			return;
		} else {
			if ("marbilder".equals(arachneId.getTableName())) {
				final Image image = new Image();
				image.setImageId(arachneId.getArachneEntityID());
				String fileName = dataset.getField("marbilder.DateinameMarbilder");
				image.setImageSubtitle(fileName.substring(0, fileName.lastIndexOf('.')));
				final List<Image> imageList = new ArrayList<Image>();
				imageList.add(image);
				dataset.setImages(imageList);
				dataset.setThumbnailId(arachneId.getArachneEntityID());
			} else {
				@SuppressWarnings("unchecked")
				final List<Image> imageList = (List<Image>) genericSQLDao.getImageList(arachneId.getTableName()
						, arachneId.getInternalKey());
				// sort image List
				if (imageList != null && imageList.size() > 1) {
					Collections.sort(imageList, new ImageComparator());
				}

				dataset.setImages(imageList);
				// get thumbnail from imageList
				if (imageList != null && !imageList.isEmpty()) {
					dataset.setThumbnailId(ImageUtils.findThumbnailId(imageList));
				}
			}
		}
	}

}
