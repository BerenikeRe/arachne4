package de.uni_koeln.arachne.service;

import de.uni_koeln.arachne.mapping.DatasetGroup;
import de.uni_koeln.arachne.mapping.UserAdministration;

public interface IUserRightsService {

	public static final String INDEXING = "Indexing";
	public static final String ANONYMOUS_USER_NAME = "Anonymous";
	public static final int MIN_ADMIN_ID = 800;
	
	/**
	 * Get the current arachne user
	 * @return UserAdministration the user object or the "anonymous" user if no user is logged in
	 */
	public abstract UserAdministration getCurrentUser();

	/**
	 * Set the 'dataimport user'.
	 */
	public abstract void setDataimporter();
	
	/**
	 * Is the current user the 'dataimport user'.
	 * @return <code>true</code> if the current user is Solr.
	 */
	public abstract boolean isDataimporter();
	
	/**
	 * Is the current user signed in.
	 * @return <code>true</code> if the current user is signed in.
	 */
	public abstract boolean isSignedInUser();
	
	/**
	 * Is the given <code>Datasetgroup</code> in the users <code>Set</code> of <code>DatasetGroups</code>.
	 * @param datasetGroup A <code>DatasetGroup</code> to check against the user groups.
	 * @return <code>true</code> if the given <code>DatasetGroup</code> is in the users <code>Set</code>.
	 */
	public boolean userHasDatasetGroup(final DatasetGroup datasetGroup);
	
	/**
	 * Method to reset the current user (e.g. for logout)
	 */
	public abstract void reset();

	/**
	 * Returns the users permissions as an SQL-Snipplet that is ready to be append to a SQL <code>WHERE</code> statement.
	 * @return A SQL snipplet as String. 
	 */
	public abstract String getSQL(final String tableName);
}