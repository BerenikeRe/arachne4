package de.uni_koeln.arachne.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.uni_koeln.arachne.dao.ImageRightsDao;
import de.uni_koeln.arachne.mapping.ImageRightsGroup;
import de.uni_koeln.arachne.mapping.UserAdministration;
import de.uni_koeln.arachne.response.Dataset;
import de.uni_koeln.arachne.service.EntityIdentificationService;
import de.uni_koeln.arachne.service.ImageResolutionType;
import de.uni_koeln.arachne.service.ImageRightsGroupService;
import de.uni_koeln.arachne.service.SingleEntityDataService;
import de.uni_koeln.arachne.service.UserRightsService;
import de.uni_koeln.arachne.util.EntityId;

/**
 * Handles http requests for images, currently only get
 * @author Sven Ole Clemens
 * @author Sebastian Cuy
 */
@Controller
public class ImageController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageController.class);
	
	@Autowired
	private UserRightsService userRightsService; // NOPMD
	
	@Autowired
	private EntityIdentificationService arachneEntityIdentificationService; // NOPMD
	
	@Autowired
	private ImageRightsDao imageRightsDao; // NOPMD

	@Autowired
	private ImageRightsGroupService imageRightsGroupService; // NOPMD
	
	@Autowired
	private SingleEntityDataService arachneSingleEntityDataService; // NOPMD
	
	private final transient String imageServerUrl;
	private final transient String imagePath;
	private final transient int imageServerReadTimeout;
	
	
	@Autowired
	/**
	 * Autowired constructor to initialize the image server parameters set in application.properties.
	 * @param imageServerUrl
	 * @param imagePath
	 * @param imageServerReadTimeout
	 */
	public ImageController(final @Value("#{config.imageServerUrl}") String imageServerUrl,
			final @Value("#{config.imagePath}") String imagePath,
			final @Value("#{config.imageServerReadTimeout}") int imageServerReadTimeout) {
			
		this.imageServerUrl = imageServerUrl;
		this.imagePath = imagePath;
		this.imageServerReadTimeout = imageServerReadTimeout;
		LOGGER.info("ImageServerUrl: " + imageServerUrl);
	}
	
	/**
	 * This method handles requests from the <code>mooviewer</code> to the image server. If meta data is requested plain text is
	 * returned wrapped in a <code>ResponseEntity&ltString&gt</code> else a JPEG image is returned via the <code>HttpServletResponse</code>.
	 * @param entityId the unique image ID. (mandatory)
	 * @param request The incoming HTTP request.
	 * @param response The outgoing HTTP response.
	 * @return Either the meta data or the image returned by the image server.
	 */
	@RequestMapping(value = "/image/iipviewer", method = RequestMethod.GET)
	public ResponseEntity<String> getFromImageServer(@RequestParam(value = "FIF", required = true) final long entityId,
			final HttpServletRequest request, final HttpServletResponse response) {
		
		LOGGER.debug("Received Request: " + request.getQueryString());
				
		HttpURLConnection connection = null;
		
		final ImageProperties imageProperties = getImageProperties(entityId, ImageResolutionType.HIGH);
		
		if (imageProperties.httpResponseCode == 200) {
			String imageName = imageProperties.name;

			// TODO replace when the correct images are accessible by the image server
			imageName = "ptif_test.tif";

			final String remainingQueryString = request.getQueryString().split("&", 2)[1];
			final String fullQueryString = "?FIF=" + imagePath + imageName + "&" + remainingQueryString;

			LOGGER.debug("Sent Request: " + fullQueryString);

			try {
				final URL serverAdress = new URL(imageServerUrl + fullQueryString);
				connection = (HttpURLConnection)serverAdress.openConnection();			
				connection.setRequestMethod("GET");
				connection.setReadTimeout(imageServerReadTimeout);
				connection.connect();

				if (connection.getResponseCode() == 200) {
					final HttpHeaders responseHeaders = new HttpHeaders();

					// check if viewer calls for metadata - if the request query string contains "obj=IIP" it does
					if (request.getQueryString().contains("obj=IIP")) {
						final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

						final StringBuilder stringBuilder = new StringBuilder();
						String line = null;

						while ((line = bufferedReader.readLine()) != null) {
							stringBuilder.append(line  + '\n');
						}

						responseHeaders.setContentType(MediaType.TEXT_PLAIN);
						return new ResponseEntity<String>(stringBuilder.toString(), responseHeaders, HttpStatus.OK);
					} else {
						response.setContentType("image/jpeg");
						final OutputStream outputStream = response.getOutputStream();
						ImageIO.write(ImageIO.read(connection.getInputStream()), "jpg", outputStream);
						// no return object needed as the result is directly written to the HTTPResponse
						return null;
					}
				}

			} catch (MalformedURLException e) {
				LOGGER.error(e.getMessage());
			} catch (ProtocolException e) {
				LOGGER.error(e.getMessage());
			} catch (SocketTimeoutException e) {
				LOGGER.error(e.getMessage());
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
			} finally {
				connection.disconnect();
				connection = null;
			}
		} else {
			response.setStatus(imageProperties.httpResponseCode);
		}
		
		return null;
	}
	
	/**
	 * Handles the request for /image/{entityId}. 
	 * @param entityId The unique ID of the image.
	 * @param response The outgoing HTTP response.
	 */
	@RequestMapping(value = "/image/{entityId}", method = RequestMethod.GET)
	public void getImage(	@PathVariable("entityId") final long entityId, final HttpServletResponse response) {
		getImageFromServer(entityId, ImageResolutionType.HIGH, response);
	}
	
	/**
	 * Handles the request for /image/thumbnail/{entityId}.
	 * @param entityId The unique ID of the image.
	 * @param response The outgoing HTTP response.
	 */
	@RequestMapping(value = "/image/thumbnail/{entityId}", method = RequestMethod.GET)
	public void getThumbnail(	@PathVariable("entityId") final long entityId, final HttpServletResponse response) {
		getImageFromServer(entityId, ImageResolutionType.THUMBNAIL, response);
	}
	
	/**
	 * Handles the request for /image/preview/{entityId}.
	 * @param entityId The unique ID of the image.
	 * @param response The outgoing HTTP response.
	 */
	@RequestMapping(value = "/image/preview/{entityId}", method = RequestMethod.GET)
	public void getPreview(@PathVariable("entityId") final long entityId, final HttpServletResponse response) {
		getImageFromServer(entityId, ImageResolutionType.PREVIEW, response);
	}
	
	/**
	 * This private method retrieves images from the image server and writes them as JPEG directly to the HTTP response. 
	 * @param entityId The unique ID of the image.
	 * @param requestedResolution The requested resolution. Only the constants <code>ImageController.THUMBNAIL</code>, 
	 * <code>ImageController.PREVIEW</code> and <code>ImageController.HIGH</code> are currently in use but any integer value is allowed.
	 * @param response The outgoing HTTP response.
	 */
	private void getImageFromServer(final long entityId, final ImageResolutionType requestedResolution, final HttpServletResponse response) {
		
		HttpURLConnection connection = null;
		
		final ImageProperties imageProperties = getImageProperties(entityId, requestedResolution);
		
		if (imageProperties.httpResponseCode == 200) {
			String imageName = imageProperties.name;
			final String watermark = imageProperties.watermark;
			final ImageResolutionType resolution = imageProperties.resolution;
			
			// TODO replace when the correct images are accessible by the image server
			imageName = "ptif_test.tif";

			try {
				// TODO use watermarks when they are fully implemented on the server side
				final URL serverAdress = new URL(imageServerUrl + "?FIF=" + imagePath + imageName + "&SDS=0,90&CNT=1.0&WID="
						+ resolution.getWidth() + "&QLT=99&CVT=jpeg");
				connection = (HttpURLConnection)serverAdress.openConnection();			
				connection.setRequestMethod("GET");
				connection.setReadTimeout(imageServerReadTimeout);
				connection.connect();

				if (connection.getResponseCode() == 200) {
					response.setContentType("image/jpeg");
					final OutputStream outputStream = response.getOutputStream();
					ImageIO.write(ImageIO.read(connection.getInputStream()), "jpg", outputStream);
					response.setStatus(200);
				}
			} catch (MalformedURLException e) {
				LOGGER.error(e.getMessage());
			} catch (ProtocolException e) {
				LOGGER.error(e.getMessage());
			} catch (SocketTimeoutException e) {
				LOGGER.error(e.getMessage());
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
			} finally {
				connection.disconnect();
				connection = null;
			}
		} else {
			response.setStatus(imageProperties.httpResponseCode);
		}
	}

	/**
	 * Method to retrieve the name of the image, the allowed maximum resolution and the watermark to use. Maximum resolution and watermark
	 * depend on the rights of the currently logged in user.
	 * @param entityId The unique image ID.
	 * @return A HTTP response code indicating success or failure.
	 */
	private ImageProperties getImageProperties(final long entityId, final ImageResolutionType requestedResolution) {
		String imageName = null;
		String watermark = null;
		ImageResolutionType resolution = requestedResolution;
		
		if (entityId>0) {
			final EntityId arachneId = arachneEntityIdentificationService.getId(entityId);
			
			if(!arachneId.getTableName().equals("marbilder")) {
				LOGGER.error("EntityId {} does not refer to an image.", entityId);
				return new ImageProperties(imageName, resolution, watermark, 404);
			}
			
			final Dataset imageEntity = arachneSingleEntityDataService.getSingleEntityByArachneId(arachneId
					, userRightsService.getCurrentUser());
			// TODO get correct image name not the old one
			imageName = imageEntity.getField("marbilder.Pfad");
			LOGGER.debug("Image: " + entityId + ": " + imageName);
			
			// TODO implement watermarking
			// Check image rights
			final ImageRightsGroup imageRightsGroup = imageRightsDao.findByName(imageEntity.getField("marbilder.BildrechteGruppe"));
			final UserAdministration currentUser = userRightsService.getCurrentUser();
			watermark = imageRightsGroupService.getWatermarkFilename(imageEntity, currentUser, imageRightsGroup);
			if(!imageRightsGroupService.checkResolutionRight(imageEntity, currentUser, resolution, imageRightsGroup)) {
				resolution = imageRightsGroupService.getMaxResolution(imageEntity, currentUser, imageRightsGroup);
				
				// Forbidden
				if (resolution == null) {
					return new ImageProperties(imageName, resolution, watermark, 403);
				}
			}
		}
		return new ImageProperties(imageName, resolution, watermark, 200);
	}	
	
	/**
	 * Inner class to return multiple values from the getImageProperties method.
	 */
	private class ImageProperties {
		public final transient String name;
		
		public final transient ImageResolutionType resolution;
		
		public final transient String watermark;
		
		public final transient int httpResponseCode;
		
		public ImageProperties(final String imageName, final ImageResolutionType resolution, final String watermark, final int httpResponseCode) {
			this.name = imageName;
			this.resolution = resolution;
			this.watermark = watermark;
			this.httpResponseCode = httpResponseCode;
		}
	}
}