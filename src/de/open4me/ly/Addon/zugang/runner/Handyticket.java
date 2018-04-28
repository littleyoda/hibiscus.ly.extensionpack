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

public class Handyticket extends BaseZugangRunner {

	@Override
	public String getName() {
		return "Handyticket";
	}

	@Override
	protected void auswerten() throws IOException, ParseException {
		CsvListReader csv = new CsvListReader(new StringReader(r.getDownloads().get(0).toString()), CsvPreference.STANDARD_PREFERENCE);
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		List<String> line;
		while ((line = csv.read()) != null) {
			try {
				//Buchung	Wertstellung	Beschreibung	Betrag 
				Umsaetze u = new Umsaetze();
				u.setBuchung(df.parse(line.get(2)));
				u.setWertstellung(df.parse(line.get(2)));
				String buchungstext = line.get(1);
				u.setBuchungstext(buchungstext);

				String[] betragWaehrung = line.get(5).trim().split(" ");
				u.setBetrag(ToolKitUtils.betrag2BigDecimal(betragWaehrung[0], "de", "DE"));
				umsatzliste.add(u);
			} catch (ParseException e) {
				System.out.println(line);
				e.printStackTrace();
				throw e;
			}
		}
		csv.close();
		
	}

	@Override
	public boolean supports(HashMap<String, String> info) {
		return "handyticket".equals(info.get("subid").toLowerCase());
	}

}
