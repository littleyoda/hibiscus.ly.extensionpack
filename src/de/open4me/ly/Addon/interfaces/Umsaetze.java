package de.open4me.ly.Addon.interfaces;

import java.math.BigDecimal;
import java.util.Date;

public class Umsaetze {

	private Date wertstellung;
	private Date buchungDatum;
	private String buchungstext;
	private String gegenIBAN;
	private String gegenBIC;
	private boolean vorgemerkt = false;
	
	private BigDecimal betrag;
	@Override
	public String toString() {
		return "Umsaetze [wertstellung=" + wertstellung + ", buchung=" + buchungDatum + ", buchungstext=" + buchungstext
				+ ", betrag=" + betrag + "]";
	}
	public Date getWertstellung() {
		return wertstellung;
	}
	public void setWertstellung(Date wertstellung) {
		this.wertstellung = wertstellung;
	}
	public Date getBuchung() {
		return buchungDatum;
	}
	public void setBuchung(Date buchung) {
		this.buchungDatum = buchung;
	}
	public String getBuchungstext() {
		return buchungstext;
	}
	public void setBuchungstext(String buchungstext) {
		this.buchungstext = buchungstext;
	}
	public BigDecimal getBetrag() {
		return betrag;
	}
	public void setBetrag(BigDecimal betrag) {
		this.betrag = betrag;
	}
	public String getGegenIBAN() {
		return gegenIBAN;
	}
	public void setGegenIBAN(String gegenIBAN) {
		this.gegenIBAN = gegenIBAN;
	}
	public String getGegenBIC() {
		return gegenBIC;
	}
	public void setGegenBIC(String gegenBIC) {
		this.gegenBIC = gegenBIC;
	}
	public boolean isVorgemerkt() {
		return vorgemerkt;
	}
	public void setVorgemerkt(boolean vorgemerkt) {
		this.vorgemerkt = vorgemerkt;
	}
	
}
