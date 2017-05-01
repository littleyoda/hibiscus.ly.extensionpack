package de.open4me.ly.Addon.zugang.misc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.Addon.zugang.BaseZugang;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class Barclays extends BaseZugang {

	protected final static PropertyElement PROP_BARCLAYSPASSWORD = new PropertyElement("Passwort", "pwd").setPwd();

	@Override
	public String getName() {
		return "Barclays";
	}

	@Override
	public boolean supports(HashMap<String, String> info) {

		return ("0".equals(info.get("blz")) 
				|| "0000000".equals(info.get("blz")))
				&& 
				(
				  getName().toLowerCase().equals(info.get("subid").toLowerCase()) 
				  || "barclaystagesgeld".equals(info.get("subid").toLowerCase().replace(" ", ""))
				  || "barclaysfestgeld".equals(info.get("subid").toLowerCase().replace(" ", ""))
				  || "barclay".equals(info.get("subid").toLowerCase().replace(" ", ""))
				);
	}


	@Override
	public void run() throws Exception {
		String username = getInfo().get("userid");
		String password = getInfo().get(PROP_BARCLAYSPASSWORD.getHashname());
		doOneAccount(username, password);
	}

	private DateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

	
	public void doOneAccount(String username, String password) throws Exception {

		final WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
		getController().setProxyCfg(webClient, "https://service.barclays.de/");
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setRedirectEnabled(true);
		// Login-Page und Login
		HtmlPage page = webClient.getPage("https://service.barclays.de/");
		if (page.getUrl().toString().equals("http://www.barclays.de/wartung.html")) {
			getController().notifyUser("Aufgrund von Wartungsarbeiten ist bei der Barclays Bank z.Z. kein Abruf von Kontoinformationen möglich.");
			return;
		}
		HtmlForm form = page.getForms().get(0);
		((HtmlInput) page.getHtmlElementById("b_usr")).setValueAttribute(username);
		((HtmlInput) page.getHtmlElementById("b_pwd")).setValueAttribute(password);
		final HtmlButton button = form.getButtonByName("post");
		page = button.click();
		
		// Kontostand extrahierne und Umsatzlink suchen
		@SuppressWarnings("unchecked")
		List<HtmlTable> kontentabellen = (List<HtmlTable>) (List<?>)  page.getByXPath( "//table[@id='konten']");
		if (kontentabellen.size() != 1) {
			throw new IllegalStateException(getController().i18ntr("Konnte die Kontenübersicht nicht finden. (Username/Pwd falsch?)"));
		}
		HtmlAnchor ahref = null;
		HtmlTable kontentabelle = kontentabellen.get(0);
		for (int i = 0; i < kontentabelle.getRowCount(); i++) {
			if (kontentabelle.getCellAt(i, 0).asText().equals(getInfo().get("nummer"))) {
				List<?> x = kontentabelle.getRow(i).getByXPath( "//a[@title='Umsätze anzeigen']");
				if (x.size() != 1) {
					throw new IllegalStateException(getController().i18ntr("Konnte den Kontostand nicht ermitteln (" + x.size() + ")! Zugangsdaten falsch?"));
				}
				ahref = (HtmlAnchor) x.get(0);
				setSaldo(ToolKitUtils.betrag2BigDecimal(kontentabelle.getCellAt(i,  5).asText().replace(" ", "").trim(), "de", "DE"));
			}
		}
		if (ahref == null) {
			throw new IllegalStateException(getController().i18ntr("Link für die Umsätze nicht gefunden!"));
		}
		page = ahref.click();

		// Datumsbereich der Umsätze ändern
		HtmlSelect select = (HtmlSelect) page.getElementById("duration_field");
		select.setSelectedAttribute("360", true);
		@SuppressWarnings("unchecked")
		List<HtmlButton> submitButton = (List<HtmlButton>) (List<?>) page.getByXPath( "//button[@value='weiter']");
		if (submitButton.size() != 1) {
			throw new IllegalStateException(getController().i18ntr("Konnte den Datumsbereich  nicht ändern!"));
		}
		page = submitButton.get(0).click();
		
		// Alle Unterseiten mit Umsätzen durchgehen
		int pagenr = 1;
		try {
			while (true) {
				@SuppressWarnings("unchecked")
				List<HtmlTable> tabellen = (List<HtmlTable>) (List<?>) page.getByXPath( "//table[@id='umsaetze']");
				if (tabellen.size() != 1) {
					throw new IllegalStateException(getController().i18ntr("Konnte die Umsätze aus Tabelle nicht extrahieren."));
				}
				HtmlTable tab = tabellen.get(0);
				for (int zeileIdx = 1; zeileIdx < tab.getRowCount(); zeileIdx++) {
					HtmlTableRow zeile = tab.getRow(zeileIdx);
					if (zeile.getCells().size() != 4) {
						continue;
					}
					String betrag = zeile.getCell(3).asText();
					String wertstellung = zeile.getCell(1).asText();
					String datum = zeile.getCell( 0).asText();
					String verwendungszweck = zeile.getCell(2).asText();
					Umsaetze newUmsatz = new Umsaetze();
					if (datum.isEmpty()) {
						// Sonderfall Vorgemerkte Buchungen
						newUmsatz.setBuchung(df.parse(wertstellung));
						newUmsatz.setWertstellung(df.parse(wertstellung));
						newUmsatz.setVorgemerkt(true);
					} else {
						newUmsatz.setBuchung(df.parse(datum));
						newUmsatz.setWertstellung(df.parse(wertstellung));
					}
					newUmsatz.setBetrag(ToolKitUtils.betrag2BigDecimal(betrag, "de", "DE"));
					newUmsatz.setBuchungstext(verwendungszweck);
					getUmsatzliste().add(newUmsatz);
				}
				pagenr++;
				page = (page.getAnchorByText("" + pagenr)).click();
			} 
		} catch (ElementNotFoundException e) {
			System.out.println("Page " + pagenr + " nicht gefunden!");

		}
		webClient.close();
	}
	

	@Override
	public List<PropertyElement> getPropertyNames() {
		List<PropertyElement> l = super.getPropertyNames();
		l.remove(PROP_PASSWORD);
		l.add(PROP_BARCLAYSPASSWORD);
		return l;
	}


}
