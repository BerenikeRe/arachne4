package de.uni_koeln.arachne.mapping;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * ORM class for the connection table (<code>Verknuepfungen</code>).
 * POJO for hibernate mapping.
 */
@Entity
@Table(name="Verknuepfungen")
public class ArachneConnection {
	@Id
	@Column(name="PS_VerknuepfungenID")
	private Long id = null;
	
	@Column(name="Teil1")
	private String part1 = null;

	@Column(name="Teil2")
	private String part2 = null;
	
	@Column(name="Felder")
	private String fields = null;
	
	@Column(name="Type")
	private String type = null;

	public Long getId() {
		return id;
	}

	public String getPart1() {
		return part1;
	}

	public String getPart2() {
		return part2;
	}

	public String getFields() {
		return fields;
	}

	public String getType() {
		return type;
	}
}
