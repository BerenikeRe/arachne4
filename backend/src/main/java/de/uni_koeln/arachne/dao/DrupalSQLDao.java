package de.uni_koeln.arachne.dao;

import java.util.List;
import java.util.Map;

import de.uni_koeln.arachne.mapping.DatasetMapper;

public class DrupalSQLDao extends SQLDao {

	public Map<String,String> getNode(int nid) {
		List<Map<String, String>> result = jdbcTemplate.query("SELECT nid, vid, language, type FROM node WHERE nid = ? LIMIT 1",
				new Object[]{nid}, new DatasetMapper());
		if (result != null && !result.isEmpty())
			return result.get(0);
		else
			return null;
	}
	
	public Map<String,String> getRevision(int vid) {
		List<Map<String, String>> result = jdbcTemplate
				.query("SELECT body, title FROM node_revisions WHERE vid = ? LIMIT 1",
						new Object[]{vid}, new DatasetMapper());
		if (result != null && !result.isEmpty())
			return result.get(0);
		else
			return null;
	}
	
	public String getTeaser(int vid) {
		return jdbcTemplate.queryForObject("SELECT field_teaser_value FROM content_type_project WHERE vid = ?",
				new Object[]{vid}, String.class);
	}
	
	public List<Map<String,String>> getLinks(int vid) {
		return jdbcTemplate.query("SELECT field_links_url, field_links_title FROM content_field_links WHERE vid = ? AND field_links_url IS NOT NULL",
				new Object[]{vid}, new DatasetMapper());
	}
	
	public List<Map<String,String>> getImages(int vid) {
		return jdbcTemplate.query("SELECT filepath FROM content_field_images JOIN files ON field_images_fid = fid WHERE vid = ?",
			new Object[]{vid}, new DatasetMapper());
	}

	public List<Map<String,String>> getMenuEntries(String name) {
		return jdbcTemplate.query("SELECT mlid, plid, link_path, link_title, has_children FROM menu_links WHERE menu_name LIKE ? ORDER BY weight DESC",
				new Object[]{name}, new DatasetMapper());	
	}

	public List<Map<String,String>> getTeasers(String language) {
		return jdbcTemplate.query("SELECT nid FROM node WHERE language LIKE ? AND type LIKE 'project' AND promote = 1",
				new Object[]{language}, new DatasetMapper());	
	}

}
