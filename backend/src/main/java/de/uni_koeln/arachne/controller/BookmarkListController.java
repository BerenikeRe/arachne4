package de.uni_koeln.arachne.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.uni_koeln.arachne.dao.BookmarkDao;
import de.uni_koeln.arachne.dao.BookmarkListDao;
import de.uni_koeln.arachne.mapping.Bookmark;
import de.uni_koeln.arachne.mapping.BookmarkList;
import de.uni_koeln.arachne.mapping.UserAdministration;
import de.uni_koeln.arachne.service.IUserRightsService;

/**
 * Handles http requests (currently only get) for <code>/bookmark<code> and <code>/bookmarklist</code>.
 */
@Controller
public class BookmarkListController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
	
	@Autowired
	private transient IUserRightsService rightsService;
	
	@Autowired
	private transient BookmarkDao bookmarkDao;
	
	@Autowired
	private transient BookmarkListDao bookmarkListDao;
	
	/**
	 * Handles http GET request for <code>/bookmark/{bookmarkId}</code>.
	 * Returns a bookmark entity which is serialized into JSON or XML depending
	 * on the requested format.
	 * If the given id does not refer to a bookmark entity, a 404 error code is returned.
	 * if the bookmark is not owned by the current user or no user is signed in, 
	 * a 403 error code is returned. 
	 */
	@RequestMapping(value="/bookmark/{bookmarkId}", method=RequestMethod.GET)
	public @ResponseBody Bookmark handleGetBookmarkRequest(
			@PathVariable("bookmarkId") final Long bookmarkId,
			final HttpServletResponse response) {
		
		Bookmark result = null;
		final UserAdministration user = rightsService.getCurrentUser();
		LOGGER.debug("Request for bookmark: " + bookmarkId);
		if ("Anonymous".equals(user.getUsername())) {
			response.setStatus(403);
		} else {
			result = bookmarkDao.getByBookmarkId(bookmarkId);
			if (result == null) {
				response.setStatus(404);
			} else if (result.getBookmarkList().getUid() != user.getId()) {
				result = null;
				response.setStatus(403);
			}
		}
		return result;
	}
	
	/**
	 * Handles http GET request for <code>/bookmarklist</code>.
	 * Returns all bookmarkLists belonging to the user, that is signed in, serialized 
	 * into JSON or XML depending on the requested format.
	 * If the current user does not own any bookmarks a 404 error code is returned.
	 * If no user is signed in, a 403 error code is returned.
	 */
	@RequestMapping(value="/bookmarklist", method=RequestMethod.GET)
	public @ResponseBody List<BookmarkList> handleGetBookmarksRequest(
			final HttpServletResponse response) {
		
		List<BookmarkList> result = null;
		final UserAdministration user = rightsService.getCurrentUser();
		LOGGER.debug("Request for bookmarks of user: " + user.getUsername());
		if ("Anonymous".equals(user.getUsername())) {
			response.setStatus(403);
		} else {
			result = bookmarkListDao.getByUid(user.getId());
			if (result == null || result.isEmpty()) {
				response.setStatus(404);
			}
		}
		return result;
	}
	
	/**
	 * Handles http GET request for <code>/bookmarklist/{bookmarkListId}</code>.
	 * Returns a bookmarkList entity which is serialized into JSON or XML depending
	 * on the requested format.
	 * If the given id does not refer to a bookmarkList, a 404 error code is returned.
	 * if the bookmarkList is not owned by the current user or no user is signed in, 
	 * a 403 error code is returned. 
	 */
	@RequestMapping(value="/bookmarklist/{bookmarkListId}", method=RequestMethod.GET)
	public @ResponseBody BookmarkList handleGetBookmarkListRequest(
			@PathVariable("bookmarkListId") final Long bookmarkListId,
			final HttpServletResponse response) {
		
		BookmarkList result = null;
		final UserAdministration user = rightsService.getCurrentUser();
		LOGGER.debug("Request for bookmarkList " + bookmarkListId + " of user: " + user.getUsername());
		if ("Anonymous".equals(user.getUsername())) {
			response.setStatus(403);
		} else {
			result = bookmarkListDao.getByBookmarkListId(bookmarkListId);
			if (result == null) {
				response.setStatus(404);
			} else if (result.getUid() != user.getId()) {
				result = null;
				response.setStatus(403);
			}
		}
		return result;
	}

}
