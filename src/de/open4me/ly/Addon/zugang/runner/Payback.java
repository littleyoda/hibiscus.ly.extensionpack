package de.open4me.ly.Addon.zugang.runner;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class Payback extends BaseZugangRunner {

	@Override
	public String getName() {
		return "Payback";
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
				u.setBuchung(df.parse(line.get(1)));
				u.setWertstellung(df.parse(line.get(1)));
				String buchungstext = line.get(2) + " " + line.get(3);
				u.setBuchungstext(buchungstext);

				String[] betragWaehrung = line.get(4).trim().split(" ");
				u.setBetrag(ToolKitUtils.betrag2BigDecimal(betragWaehrung[0], "de", "DE").divide(new BigDecimal(100)));
				umsatzliste.add(u);
				System.out.println(u);
			} catch (ParseException e) {
				System.out.println(line);
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw e;
			}
		}
		
		csv = new CsvListReader(new StringReader(r.getDownloads().get(1).toString()), CsvPreference.STANDARD_PREFERENCE);
		line = csv.read();
		setSaldo(ToolKitUtils.betrag2BigDecimal(line.get(1)).divide(new BigDecimal(100)));

		csv.close();
		
	}

	@Override
	public boolean supports(HashMap<String, String> info) {
		return "payback".equals(info.get("subid").toLowerCase());
	}

}
