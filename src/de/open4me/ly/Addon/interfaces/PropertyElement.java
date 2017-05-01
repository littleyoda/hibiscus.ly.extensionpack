package de.open4me.ly.Addon.interfaces;

public class PropertyElement {


	private String description;
	private String hashname;
	private boolean isEmptyallowed = false;
	private boolean isPwd = false;

	public PropertyElement(String description, String hashname) {
		this.description = description;
		this.hashname = hashname;
		this.isPwd = false;
	}

	public String getDescription() {
		return description;
	}

	public String getHashname() {
		return hashname;
	}
	
	public PropertyElement setEmptyAllowed() {
		isEmptyallowed = true;
		return this;
	}

	public boolean isEmptyAllowed() {
		return isEmptyallowed;
	}

	public PropertyElement setPwd() {
		isPwd = true;
		return this;
	}
	
	public boolean isPwd() {
		return isPwd;
	}
}
