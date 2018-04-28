package de.open4me.ly.Addon.zugang.runner;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.supercsv.exception.SuperCsvException;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.webscraper.utils.ToolKitUtils;

public class Privatbank1891 extends BaseZugangRunner {

	private DateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

	@Override
	public String getName() {
		return "Privatbank1891";
	}

	@Override
	protected void auswerten() throws IOException, ParseException {

		try {
			CsvMapReader c = new CsvMapReader(new StringReader(r.getDownloads().get(1).toString()), new CsvPreference.Builder('"', ';', "\r\n").build());
			final String[] header = c.getHeader(true);
			Map<String, String> line;
			while ((line = c.read(header)) != null) { 
				try {
					//Buchung	Wertstellung	Beschreibung	Betrag 
					Umsaetze u = new Umsaetze();

					u.setBuchung(df.parse(line.get("Buchungstag")));
					u.setWertstellung(df.parse(line.get("Wertstellung")));
					u.setSaldo(ToolKitUtils.betrag2BigDecimal(line.get("Kontostand"), "de", "DE"));
					String buchungstext = line.get("Buchungstext");
					for (int i = 1; i < 13; i++) {
						String s = line.get("VWZ" + i);
						if (s != null && !s.isEmpty()) {
							buchungstext +=  " ";
							buchungstext += s;
						}

					}
					u.setBuchungstext(buchungstext.trim());
					u.setBetrag(ToolKitUtils.betrag2BigDecimal(line.get("Betrag"), "de", "DE"));
					umsatzliste.add(u);
				} catch (ParseException e) {
					System.out.println(line);
					e.printStackTrace();
					throw e;
				}
			}
			c.close();
		} catch (SuperCsvException e) {
			e.printStackTrace();
			throw e;
		}
		

		CsvListReader csv = new CsvListReader(new StringReader(r.getDownloads().get(0).toString()), CsvPreference.STANDARD_PREFERENCE);
		List<String> line = csv.read();
		setSaldo(ToolKitUtils.betrag2BigDecimal(line.get(4).replace("EUR", "").trim(), "de", "DE"));

		csv.close();

	}

	@Override
	public boolean supports(HashMap<String, String> info) {
		return "70013100".equals(info.get("blz"))
				|| "BVWBDE2WXXX".equals(info.get("bic"));
	}

}
