package de.open4me.ly.Addon.zugang.misc;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlImageInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;

import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.Addon.zugang.BaseZugang;
import de.open4me.ly.webscraper.runner.htmlunit.HUUtils;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class Airplus extends BaseZugang {


	  protected final static PropertyElement PROP_COMPANYNAME = new PropertyElement("AirPlus-Firmenname", "companyname").setEmptyAllowed();
	  protected final static PropertyElement PROP_USERNAME = new PropertyElement("AirPlus-Benutzername", "username");
	  protected final static PropertyElement PROP_AIRPLUSPASSWORD = new PropertyElement("AirPlus-Passwort", "pwd").setPwd();

	@Override
	public String getName() {
		return "Airplus";
	}


	@Override
	public boolean supports(HashMap<String, String> info) {
		
		return ("0".equals(info.get("blz")) 
				|| "0000000".equals(info.get("blz"))
				|| "50570018".equals(info.get("blz")))
				&& getName().toLowerCase().equals(info.get("subid").toLowerCase());
	}
	  @Override
	  public List<PropertyElement> getPropertyNames() {
		  List<PropertyElement> result = super.getPropertyNames();
		  result.remove(PROP_PASSWORD);
		  result.add(PROP_COMPANYNAME);
		  result.add(PROP_USERNAME);
		  result.add(PROP_AIRPLUSPASSWORD);
		  return result;
	  }

		private String[] pages = {"opArt2", "opArt1"};

		private DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

		@Override
		public void run() throws Exception {

			String firmenname = getInfo().get(PROP_COMPANYNAME.getHashname());
			String username = getInfo().get(PROP_USERNAME.getHashname());
			String password = getInfo().get(PROP_AIRPLUSPASSWORD.getHashname());
			doOneAccount(username, password, firmenname);
		}



		public void doOneAccount(String username, String password, String firmenname) throws Exception {

			final WebClient webClient = new WebClient();
			getController().setProxyCfg(webClient, "https://portal.airplus.com");
			webClient.setCssErrorHandler(new SilentCssErrorHandler());
			webClient.setRefreshHandler(new ThreadedRefreshHandler());

			// Login-Page und Login
			HtmlPage page = webClient.getPage("https://portal.airplus.com/airplus/?Language=de&Country=1U");
			//writePage(page, "Login");
			HtmlForm form = page.getForms().get(0);
			form.getInputByName("companyLoginname").setValueAttribute(firmenname);
			form.getInputByName("userLoginname").setValueAttribute(username);
			form.getInputByName("password").setValueAttribute(password);
			List<HtmlElement> submit = form.getElementsByAttribute("input", "type", "image");
			HtmlImageInput x = (HtmlImageInput) submit.get(0);
			page = (HtmlPage) x.click();
			//writePage(page, "NachLogin");
			// > Startseite > Credit Card Management > Online-Kartenkonto
			page = webClient.getPage("https://portal.airplus.com/transaction/transactionStart.do?TKN=1u561.16wa578&__w=1#selected");

			// Suche nach dem passenden Account
			String userid = getInfo().get("userid");
			HtmlAnchor link = null;
			for (HtmlAnchor ahref : HUUtils.getLinks(page)) {
				if (ahref.asText().replace(" ", "").contains(userid)) {
					link = ahref;
				}
			}
			if (link == null) {
				throw new IllegalStateException(getController().i18ntr("Keine Informationen für Kreditkarte '" + userid + "' gefunden!"));
			}
			// Neue Umsätze und Abgerechnete Umsätze fortlaufend abrufen
			for (int i = 0; i < 2; i++) {
				page = link.click();
				TextPage textpage = uebersichtsart(page, pages[i]);
				handle(textpage);
				if (i == 0) {
					setzeSaldo();
				}
			}
			// Logout
			webClient.getPage("https://portal.airplus.com/logout.do?TKN=1u561.6fuojg");
			webClient.close();
		}


		private void setzeSaldo() throws RemoteException {
			BigDecimal saldo = BigDecimal.ZERO;
			for (Umsaetze umsatz : getUmsatzliste()) {
				saldo = saldo.add(umsatz.getBetrag());
			}
			setSaldo(saldo);
		}

		private TextPage uebersichtsart(HtmlPage page, String elementID) throws Exception {
			HtmlRadioButtonInput rbutton = (HtmlRadioButtonInput) page.getElementById(elementID);
			page = rbutton.click();
			//writePage(page, "Value1");
			HtmlImageInput i = page.getElementByName("submit");
			page = (HtmlPage) i.click();
			//writePage(page,"Res1");
			page = ((HtmlAnchor) page.getElementById("export")).click();
			// Select the Export-Link
			//writePage(page,"Export");
			TextPage p = ((HtmlAnchor) page.getElementByName("export")).click();
			return p;
		}

		private void handle(TextPage p) throws Exception {
			ArrayList<HashMap<String, String>> liste = parseCSV(p.getContent(), "Rechnung");
			for (HashMap<String, String> e : liste) {
				
				Umsaetze newUmsatz = new Umsaetze();
				BigDecimal betrag = ToolKitUtils.betrag2BigDecimal((e.get("soll/haben").equals("H")? "": "-") + e.get("abgerechnet"), "de", "DE");
				newUmsatz.setBetrag(betrag);
				newUmsatz.setBuchung(df.parse(e.get("buch.datum")));
				String verwendungszweck = e.get("leistungserbringer") + " " + e.get("leistungsbeschreibung");
				newUmsatz.setBuchungstext(verwendungszweck);
				newUmsatz.setWertstellung(df.parse(e.get("kaufdatum")));
				getUmsatzliste().add(newUmsatz);


				//	Sonderfall Auslandseinsatzentgelt
				if (e.containsKey("auslandseinsatzentgelt wert")) {
					newUmsatz = new Umsaetze();
					betrag = ToolKitUtils.betrag2BigDecimal("-" + e.get("auslandseinsatzentgelt wert"), "de", "DE");
					newUmsatz.setBetrag(betrag);
					newUmsatz.setBuchung(df.parse(e.get("buch.datum")));
					newUmsatz.setBuchungstext(verwendungszweck + " " + "Auslandseinsatzentgelt");
					newUmsatz.setWertstellung(df.parse(e.get("kaufdatum")));
					getUmsatzliste().add(newUmsatz);
				}
			}
		}


		private ArrayList<HashMap<String, String>> parseCSV(String csv, String search) {
			ArrayList<HashMap<String, String>> liste = new ArrayList<HashMap<String, String>>();
			Scanner scanner = new Scanner(csv);
			String[] header = null;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (header == null) {
					if (!line.startsWith(search)) {
						continue;
					}
					header = line.replace(";;", ";_;").split(";");
					String pre = "";
					int nr = 1;
					for (int i = 0; i < header.length; i++) {
						//					System.out.print(header[i]);
						header[i] = header[i].toLowerCase();
						String orig = header[i];
						if (header[i].trim().equals("_") || header[i].trim().isEmpty()) {
							header[i] = pre + nr;
						} else {
							nr = 1;
						}
						pre = orig;
						//				System.out.println("  =>  " + header[i]);
					}
					continue;
				}
				HashMap<String, String> infos = new HashMap<String, String>();
				String[] data = line.split(";");
				for (int i = 0; i < data.length; i++) {
					infos.put(header[i], data[i]);
				}
				liste.add(infos);
			}
			scanner.close();
			return liste;

		}

	  
}
