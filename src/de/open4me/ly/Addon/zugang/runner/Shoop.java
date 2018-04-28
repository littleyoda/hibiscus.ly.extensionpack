package de.open4me.ly.Addon.zugang.runner;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class Shoop extends BaseZugangRunner {

	@Override
	public String getName() {
		return "Shoop";
	}

	@Override
	protected void auswerten() throws IOException, ParseException {
		CsvListReader csv = new CsvListReader(new StringReader(r.getDownloads().get(0).toString()), CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		List<String> line;
		line = csv.read(); // Skip first Line
		while ((line = csv.read()) != null) {
			try {
				// 0 Shoop.de-Transaktions ID	
				// 1 Händler	2 Bestell-Referenz	3 Status	
				// 4 Cashback (Euro)	5 bereits bezahlt (Euro)	
				// 6 Insgesamt (Euro)	7 Erfassungsdatum	8 abgelehnt	
				// 9 bestätigt	10 erhalten 	11 bezahlt am	
				// 12 Shoop.de Auszahlungsnummer	13 Nachbuchungsanfrage
				Umsaetze u = new Umsaetze();
				u.setBuchung(df.parse(line.get(7)));
				String art = line.get(3).toLowerCase();
				if (art.equals("bezahlt") || art.equals("verfügbar")) {
					if (line.get(10) == null) {
						// Behandlung von Sonderfällen
						u.setWertstellung(df.parse(line.get(7)));
					} else {
						u.setWertstellung(df.parse(line.get(10)));
					}
				} else if (art.equals("erfasst")) {
					u.setVorgemerkt(true);
					u.setWertstellung(df.parse(line.get(7)));
				} else if (art.equals("erinnerung")) {
					// Ignore
					continue;
				} else if (art.equals("abgelehnt")) {
					// Ignore
					continue;
				} else {
					throw new IllegalStateException("Unbekannter Type: " + art);
				}

				String buchungstext = line.get(1) + " " + line.get(0);
				u.setBuchungstext(buchungstext);
				
				u.setBetrag(ToolKitUtils.betrag2BigDecimal(line.get(4), "de", "DE"));
				umsatzliste.add(u);
			} catch (ParseException e) {
				System.out.println(line);
				e.printStackTrace();
				throw e;
			}
		}
		
		csv = new CsvListReader(new StringReader(r.getDownloads().get(1).toString()), CsvPreference.STANDARD_PREFERENCE);
		line = csv.read();
		setSaldo(ToolKitUtils.betrag2BigDecimal(line.get(1), "de", "DE"));

		csv.close();
		
	}

	@Override
	public boolean supports(HashMap<String, String> info) {
		return "shoop".equals(info.get("subid").toLowerCase());
	}

}
