package de.uni_koeln.arachne.dao.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.uni_koeln.arachne.mapping.hibernate.Catalog;
import de.uni_koeln.arachne.mapping.hibernate.CatalogEntry;
import de.uni_koeln.arachne.service.UserRightsService;

@Repository("CatalogEntryDao")
public class CatalogEntryDao {
	
	@Autowired
    private transient SessionFactory sessionFactory;
	
	@Autowired
	private transient UserRightsService userRightsService;
	
	@Transactional(readOnly=true)
	public CatalogEntry getByCatalogEntryId(final long catalogEntryId) {
		return getByCatalogEntryId(catalogEntryId, false);
	}
	
	@Transactional(readOnly=true)
	public CatalogEntry getByCatalogEntryId(final long catalogEntryId, final boolean full) {
		final Session session = sessionFactory.getCurrentSession();
		CatalogEntry result = session.get(CatalogEntry.class, catalogEntryId);
		if (!full) {
			for (final CatalogEntry child: result.getChildren()) {
				child.removeChildren();
			}
		}
		return result;
	}
	
	/**
	 * Gets a list containing public catalog identifiers and corresponding catalog paths that are connected to an 
	 * entity. The list is in ascending order.
	 * @param entityId The entity identifier of interest.
	 * @return A list of <code>Object[2]</code>. Id first, then path.
	 */
	@Transactional(readOnly=true)
	public List<Object[]> getPublicCatalogIdsAndPathsByEntityId(final long entityId) {
		final List<Object[]> result = new ArrayList<Object[]>();
		for (final CatalogEntry catalogEntry : getByEntityId(entityId)) {
			final Catalog catalog = catalogEntry.getCatalog(); 
			if (catalog.isPublic()) {
				result.add(new Object[] {catalog.getId(), catalogEntry.getPath()}); // NOPMD
			}
		}
		
		return result;
	}
	
	/**
	 * Gets a list of private catalog identifiers that are connected to an entity. The list is in ascending order.
	 * @param entityId The entity identifier of interest.
	 * @return A list of catalog ids. 
	 */
	@Transactional(readOnly=true)
	public List<Long> getPrivateCatalogIdsByEntityId(final long entityId) {
		final List<Long> result = new ArrayList<Long>();
		if (userRightsService.isSignedInUser()) {
			for (final CatalogEntry catalogEntry : getByEntityId(entityId)) {
				final Catalog catalog = catalogEntry.getCatalog(); 
				if (!catalog.isPublic() && catalog.isCatalogOfUserWithId(userRightsService.getCurrentUser().getId())) {
					result.add(catalog.getId());
				}
			}
		}
		return result;
	}
	
	@Transactional(readOnly=true)
	private List<CatalogEntry> getByEntityId(final long entityId) {
		final Session session = sessionFactory.getCurrentSession();
		final Criteria criteria = session.createCriteria(CatalogEntry.class);
		criteria.add(Restrictions.eq("arachneEntityId", entityId));
		@SuppressWarnings("unchecked")
		List<CatalogEntry> result = criteria.list();
		return result;
	}
	
	@Transactional
	public void deleteOrphanedCatalogEntries(final Catalog catalog) {
		final List<Long> catalogEntryIds = new ArrayList<Long>();
		final String querystring = "DELETE catalog_entry FROM catalog_entry LEFT JOIN catalog ON catalog_entry.catalog_id = "
				+ "catalog.id WHERE catalog.id = :catalogId";
		Query query;
		
		final Session session = sessionFactory.getCurrentSession();
		if (catalog.getCatalogEntries() != null) {
			for (final CatalogEntry referenced : catalog.getCatalogEntries()){
				catalogEntryIds.add(referenced.getId());
			}
			query = session.createSQLQuery(querystring + " AND catalog_entry.id NOT IN (:ids)")
					.setLong("catalogId", catalog.getId())
					.setParameterList("ids", catalogEntryIds);
			
		} else {
			query = session.createSQLQuery(querystring)
					.setLong("catalogId", catalog.getId());
		}
		query.executeUpdate();
	}
	
	@Transactional
	public CatalogEntry updateCatalogEntry(final CatalogEntry catalogEntry) throws HibernateException {
		final Session session = sessionFactory.getCurrentSession();
		session.update(catalogEntry);
		return catalogEntry;
	}
	
	@Transactional
	public CatalogEntry saveCatalogEntry(final CatalogEntry catalogEntry) throws HibernateException {
		final Session session = sessionFactory.getCurrentSession();
		session.save(catalogEntry);
		return catalogEntry;
	}
	
	@Transactional
	public void deleteCatalogEntry(final CatalogEntry catalogEntry) throws HibernateException {
		final Session session = sessionFactory.getCurrentSession();
		session.delete(catalogEntry);
	}

}
