package de.open4me.ly.Addon.zugang;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.open4me.ly.Addon.interfaces.Controller;
import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.interfaces.Umsaetze;

public abstract class BaseZugang {

	protected final PropertyElement PROP_PASSWORD = new PropertyElement("Passwort für die Webseite", "pwd").setPwd();
	PropertyElement[] liste = {PROP_PASSWORD};
	
	private BigDecimal saldo;
	protected List<Umsaetze> umsatzliste = new ArrayList<Umsaetze>();
	public abstract String getName();
	private HashMap<String, String> info;
	private Controller controller;


	public void setController(Controller controller) {
		this.controller = controller;
	}
	
	public Controller getController() {
		return controller;
	}
	public abstract void run() throws Exception;

	public List<Umsaetze> getUmsatzliste() {
		return umsatzliste;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	protected void setSaldo(BigDecimal saldo) {
		this.saldo = saldo;
	}

	public abstract boolean supports(HashMap<String, String> info);

	
	public void setInfo(HashMap<String, String> info) {
		this.info = info;
	}

	protected HashMap<String, String> getInfo() {
		return info;
	}

	public List<PropertyElement> getPropertyNames() {
		List<PropertyElement> l = new ArrayList<PropertyElement>();
		Collections.addAll(l, liste);
		return l;
	}
	
}
