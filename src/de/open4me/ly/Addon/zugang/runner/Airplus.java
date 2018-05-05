package de.open4me.ly.Addon.zugang.runner;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class Airplus extends BaseZugangRunner {

	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

	protected final static PropertyElement PROP_COMPANYNAME = new PropertyElement("AirPlus-Firmenname", "companyname").setEmptyAllowed();
	protected final static PropertyElement PROP_USERNAME = new PropertyElement("AirPlus-Benutzername", "username");
	protected final static PropertyElement PROP_AIRPLUSPASSWORD = new PropertyElement("AirPlus-Passwort", "pwd").setPwd();

	@Override
	public void setInfo(HashMap<String, String> info) {
		super.setInfo(info);
		String userid = getInfo().get("userid").replace(" ", "");
		if (userid.length() != 16) {
			
		}
		userid = "" +  userid.substring(0, 6) + " xxxxxx " + userid.substring(12);
		getInfo().put("userid", userid);
	}
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

	@Override
	protected void auswerten() throws IOException, ParseException {
			handle(r.getDownloads().get(0).toString(), true);
			handle(r.getDownloads().get(1).toString(), false);
	}

	private void handle(String text, boolean calcSaldo) throws ParseException {
		BigDecimal saldo = BigDecimal.ZERO;
		ArrayList<HashMap<String, String>> liste = parseCSV(text, "Kartennummer(n);Rechnung;");
		for (HashMap<String, String> e : liste) {

			Umsaetze newUmsatz = new Umsaetze();
			BigDecimal betrag = ToolKitUtils.betrag2BigDecimal((e.get("soll/haben").equals("H")? "": "-") + e.get("abgerechnet"), "de", "DE");
			saldo.add(betrag);
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
				saldo.add(betrag);
				newUmsatz.setBetrag(betrag);
				newUmsatz.setBuchung(df.parse(e.get("buch.datum")));
				newUmsatz.setBuchungstext(verwendungszweck + " " + "Auslandseinsatzentgelt");
				newUmsatz.setWertstellung(df.parse(e.get("kaufdatum")));
				getUmsatzliste().add(newUmsatz);
			}
		}
		if (calcSaldo) {
			setSaldo(saldo);
		}
	}


	private ArrayList<HashMap<String, String>> parseCSV(String csv, String search) {
		ArrayList<HashMap<String, String>> liste = new ArrayList<HashMap<String, String>>();
		Scanner scanner = new Scanner(csv);
		String[] header = null;
		Set<String> headernames = new HashSet<String>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.isEmpty()) {
				continue;
			}
			if (header == null) {
				if (!line.startsWith(search)) {
					continue;
				}
				header = line.replace(";;", ";_;").split(";");
				String pre = "";
				// Create Unique Header Names
				int nr = 1;
				for (int i = 0; i < header.length; i++) {
					String name = header[i].toLowerCase(); 
					if (name.trim().equals("_") || name.trim().isEmpty() || headernames.contains(name)) {
						name = pre + nr;
					} else {
						nr = 1;
					}
					if (name.startsWith("\"")) {
						name = name.substring(1, name.length() - 1);
					}
					header[i] = name;
					headernames.add(name);
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


