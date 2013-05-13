package de.uni_koeln.arachne.response;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class to hold a single <code>String</code> value.
 */
@XmlRootElement
public class Field extends AbstractContent {
	/**
	 * The <code>String</code> value the class holds.
	 */
	protected String value;

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return getValue();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Field other = (Field) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}	
	
}
