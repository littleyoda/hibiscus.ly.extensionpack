package de.open4me.ly.Addon.zugang.runner;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

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
		CsvListReader csv = new CsvListReader(new StringReader(r.getDownloads().get(0).toString()), CsvPreference.STANDARD_PREFERENCE);
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		List<String> line;
		line = csv.read(); // Skip first Line
		while ((line = csv.read()) != null) {
			if (line.size() == 1) {
				continue;
			}
			try {
				// 0 "Shoop.de-Transaktions ID", 
				// 1 Händler,
				// 2 Status, 
				// 3 "Cashback (Euro)", 
				// 4 "Ausgezahlt (Euro)", 
				// 5 "Insgesamt (Euro)",
				// 6 Erfassungsdatum, 
				// 7 Abgelehnt, 
				// 8 Erhalten, 
				// 9 "Ausgezahlt am", 
				// 10 "Shoop.de Auszahlungsnummer"
				Umsaetze u = new Umsaetze();
				u.setBuchung(df.parse(line.get(6)));
				String art = line.get(2).toLowerCase();
				if (art.equals("bezahlt") || art.equals("verfügbar")) {
					if (line.get(8) == null) {
						// Behandlung von Sonderfällen
						u.setWertstellung(df.parse(line.get(6)));
					} else {
						u.setWertstellung(df.parse(line.get(8)));
					}
				} else if (art.equals("erfasst")) {
					u.setVorgemerkt(true);
					u.setWertstellung(df.parse(line.get(6)));
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
				
				if (line.get(4) == null) {
					u.setBetrag(ToolKitUtils.betrag2BigDecimal(line.get(3), "de", "DE"));
				} else {
					u.setBetrag(ToolKitUtils.betrag2BigDecimal(line.get(4), "de", "DE"));
				}
				umsatzliste.add(u);
			} catch (ParseException| NullPointerException e) {
				getController().log(Level.SEVERE, "Kann folgende Zeile nicht auswerten: " + line);
				getController().log(e);
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
