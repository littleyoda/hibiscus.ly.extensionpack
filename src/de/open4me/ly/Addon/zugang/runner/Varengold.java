package de.open4me.ly.Addon.zugang.runner;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class Varengold extends BaseZugangRunner {


	@Override
	public String getName() {
		return "Varengold";
	}

	
	private String deregex =  "IBAN: ([DE]{2}([0-9a-zA-Z]{20}))";
	private String bicregex = "BIC: ([a-zA-Z]{6}[0-9a-zA-Z]{2}([0-9a-zA-Z]{3})?)";


	@Override
	protected void auswerten() throws IOException, ParseException {
		Pattern setPat = Pattern.compile(deregex);
		Pattern bicPat = Pattern.compile(bicregex);

		CsvListReader csv = new CsvListReader(new StringReader(r.getDownloads().get(0).toString()), CsvPreference.STANDARD_PREFERENCE);
		List<String> line = csv.read();
		String[] saldoText = line.get(4).trim().split(" ");
		setSaldo(ToolKitUtils.betrag2BigDecimal(saldoText[0], "en", "US"));


		csv = new CsvListReader(new StringReader(r.getDownloads().get(1).toString()), CsvPreference.STANDARD_PREFERENCE);
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		while ((line = csv.read()) != null) {
			try {
				//Buchung	Wertstellung	Beschreibung	Betrag 
				Umsaetze u = new Umsaetze();
				u.setBuchung(df.parse(line.get(1)));
				String buchungstext = line.get(3);
				
				// Extract IBAN
				Matcher m = setPat.matcher(buchungstext);
				if (m.find()) {
					buchungstext = buchungstext.replace(m.group(0), "");
					u.setGegenIBAN(m.group(1));
				}
				
				// Extract BIC
				m = bicPat.matcher(buchungstext);
				if (m.find()) {
					buchungstext = buchungstext.replace(m.group(0), "");
					u.setGegenBIC(m.group(1));
				}

				u.setWertstellung(df.parse(line.get(2)));
				u.setBuchungstext(buchungstext);

				String[] betragWaehrung = line.get(5).trim().split(" ");
				u.setBetrag(ToolKitUtils.betrag2BigDecimal(betragWaehrung[0]));
				umsatzliste.add(u);
			} catch (ParseException e) {
				getController().log(Level.SEVERE, "Kann folgende Zeile nicht auswerten: " + line);
				getController().log(e);
				throw e;
			}
		}

	}

	@Override
	public boolean supports(HashMap<String, String> info) {
		return "20030133".equals(info.get("blz"))
				|| "VGAGDEHHXXX".equals(info.get("bic"));
	}


}
