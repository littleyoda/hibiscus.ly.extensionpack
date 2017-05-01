package de.open4me.ly.Addon.zugang.misc;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.Addon.zugang.BaseZugang;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class BahnBonus extends BaseZugang {


	protected final static PropertyElement PROP_BAHNPWD = new PropertyElement("Passwort", "pwd").setPwd();

	  private DateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);


	@Override
	public String getName() {
		return "Bahn.Bonus";
	}


	@Override
	public boolean supports(HashMap<String, String> info) {
		return ("0".equals(info.get("blz")) 
				|| "0000000".equals(info.get("blz")))
				&& 
				(
						getName().toLowerCase().equals(info.get("subid").toLowerCase())
						|| "bahn".equals(info.get("subid").toLowerCase())
				);
	}

	
	@Override
	public void run() throws Exception {
		String username = getInfo().get("userid");
		String password = getInfo().get("pwd");;
		doOneAccount(username, password);
	}
	
	
	@SuppressWarnings("deprecation")
	public void doOneAccount(String username, String password) throws Exception {

		final WebClient webClient = new WebClient();
		getController().setProxyCfg(webClient, "https://fahrkarten.bahn.de/");
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.setRefreshHandler(new ThreadedRefreshHandler());

		// Login-Page und Login
		HtmlPage page = webClient.getPage("https://fahrkarten.bahn.de/privatkunde/start/start.post?lang=de&scope=login");
		((HtmlInput) page.getElementById("username")).setValueAttribute(username);
		((HtmlInput) page.getElementById("password")).setValueAttribute(password);
		HtmlButton button = (HtmlButton) page.getElementById("button.weiter");

		page = button.click();
		webClient.waitForBackgroundJavaScript(3000);
		if (page.asText().contains("Ihr Zugang zu unserem Buchungssystem vorübergehend gesperrt")) {
			throw new IllegalStateException("Der Zugang ist gesperrt. Bitte E-Mail von bahn.de bachten!");
		}
		page = ((HtmlInput) page.getElementById("mbahnbonuspunkte.button.bahnbonus")).click();

		Date now = new Date();
		((HtmlInput) page.getElementById("auswahl.von")).setValueAttribute(now.getDate() + "." +  (now.getMonth() + 1) + "." + (now.getYear() + 1900 - 3));
		button = (HtmlButton) page.getElementById("button.aktualisieren");
		if (button == null) {
			throw new IllegalStateException(getController().i18ntr("Button zur Aktualisierung nicht gefunden!"));
		}
		page = button.click();


		@SuppressWarnings("unchecked")
		List<HtmlTable> tables = (List<HtmlTable>) (List<?>) page.getByXPath( "//table[contains(@class, 'bcpunktedetails')]");
		if (tables.size() != 1) {
			throw new IllegalStateException("Table nicht gefunden! Size: " + tables.size());
		}
		HtmlTable table = (HtmlTable) tables.get(0);
		for (int i = 1; i < table.getRows().size(); i++) {
			HtmlTableRow row = table.getRow(i);
			if (row.getCells().size() == 1 || row.getCell(0).asText().isEmpty()) {
				continue;
			}
			String text = row.getCell(3).asText() + " " + row.getCell(4).asText() + " " + row.getCell(7).asText();
			if (!row.getCell(6).asText().isEmpty()) {
				text += " Statuspunkte: " + row.getCell(6).asText(); 
			}
			store(row.getCell(2).asText(), text, row.getCell(5).asText());
		}

		extractPunkteStand(page);
		
		webClient.close();
	}

	private void extractPunkteStand(HtmlPage page) throws IllegalStateException, ParseException {
		@SuppressWarnings("unchecked")
		List<HtmlDivision> punkte = (List<HtmlDivision>) (List<?>) page.getByXPath( "//div[contains(@class, 'bcpunkteinfo')]");
		if (punkte.size() != 1) {
			throw new IllegalStateException("Punkteübersicht nicht gefunden! Size: " + punkte.size());
		}
		String out = "";
		
		out = punkteVariante1(punkte);
		getController().log(Level.INFO, "Variante1: " + out);
		if (out == null) {
			out = punkteVariante2(page);
			getController().log(Level.INFO, "Variante2: " + out);
		}
		setSaldo(ToolKitUtils.betrag2BigDecimal(out, "de", "DE"));
	}

	private String punkteVariante1(List<HtmlDivision> punkte) throws IllegalStateException {
		// No better way :-(
		HtmlDivision div;
		try {
			div = (HtmlDivision) punkte.get(0).getChildNodes().get(0).getChildNodes().get(1).getChildNodes().get(1);
		} catch (NullPointerException e) {
			return null;
		}
		return div.asText().trim();
	}

	@SuppressWarnings("unchecked")
	private String punkteVariante2(HtmlPage page) throws IllegalStateException {
		// No better way :-(
		
		HtmlDivision div;
		try {
			List<HtmlElement> divs = (List<HtmlElement>) (List<?>) page.getByXPath(".//div[./div/label[contains(text(), 'Ihr aktueller Prämienpunktestand')]]/div[2]");
			if (divs.size() == 0) {
				throw new IllegalStateException("Punktestand nicht gefunden!");
			}
			div = (HtmlDivision) divs.get(0);
		} catch (NullPointerException e) {
			return null;
		}
		return div.asText().trim();
	}



	private void store(String date, String text, String punkte) throws ParseException {
		Umsaetze newUmsatz = new Umsaetze();
		BigDecimal betrag = ToolKitUtils.betrag2BigDecimal(punkte, "de", "DE");
		newUmsatz.setBetrag(betrag);
		newUmsatz.setBuchung(df.parse(date));
		newUmsatz.setBuchungstext(text);
		newUmsatz.setWertstellung(df.parse(date));
		getUmsatzliste().add(newUmsatz);
	}


	@Override
	public List<PropertyElement> getPropertyNames() {
		List<PropertyElement> l = super.getPropertyNames();
		l.remove(PROP_PASSWORD);
		l.add(PROP_BAHNPWD);
		return l;
	}

	
}
