package de.open4me.ly.hibiscusAddon;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.interfaces.Umsaetze;
import de.open4me.ly.Addon.zugang.BaseZugang;
import de.open4me.ly.Addon.zugang.ZugangsFabrik;
import de.open4me.ly.webscraper.runner.Runner;
import de.willuhn.datasource.GenericIterator;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.messaging.ImportMessage;
import de.willuhn.jameica.hbci.messaging.SaldoMessage;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Umsatz;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJobKontoauszug;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * Implementierung des Kontoauszugsabruf fuer eine Beispiel-Bank.
 * Von der passenden Job-Klasse ableiten, damit der Job gefunden wird.
 */
public class ExampleSynchronizeJobKontoauszug extends SynchronizeJobKontoauszug implements JobInterfaceInterface
{
	static Map<String, String> storedValues = new HashMap<String, String>();
	private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

	@Resource
	private ExampleSynchronizeBackend backend = null;
	private boolean caching;
	private boolean storing;

	/**
	 * @see de.open4me.ly.hibiscusAddon.JobInterfaceInterface#execute()
	 */
	@Override
	public void execute() throws Exception
	{
		Runner r = new Runner() {

		};
		caching = Settings.getCachePin();
		if (!caching) {
			storedValues.clear();
		}
		storing = Settings.getStorePin();
		Konto konto = (Konto) this.getContext(CTX_ENTITY); // wurde von ExampleSynchronizeJobProviderKontoauszug dort abgelegt
		BaseZugang v = null;
		try {

			System.out.println("Cached: "  + Settings.getCachePin() + " Store: " + Settings.getStorePin());
			Logger.info("Rufe Umsätze/Saldo ab für " + backend.getName());

			HashMap<String, String> info = Utils.getKontoInformationen(konto);

			v = ZugangsFabrik.getZugang(info, ControllerHibiscus.getInstance());
			if (v == null) {
				throw new ApplicationException(i18n.tr("Konto dieses Konto keinem Anbieter zuordnen!"));
			}
			Logger.info("Genutzer Zugang: " + v.getName());
			Utils.addProperties(info, v, konto);
			checkProperties(konto, info, v);
			v.setInfo(info);
			v.run();
			
			List<Umsatz>  fetched = umsatz2umsatz(konto, v.getUmsatzliste());
			addUmsaetze(konto, fetched);

			if (v.getSaldo() != null) {
				konto.setSaldo(v.getSaldo().doubleValue());
			}
			konto.store();

			Application.getMessagingFactory().sendMessage(new SaldoMessage(konto));
		} catch (Exception e) {
			resetPwd(konto, v);
//			int i = 0;
//			for (ResultSets x : r.getResults()) {
//				if (x != null && x.page != null) {
//					System.out.println(i + ": " + x.page.toString());
//					if (x.page instanceof HtmlPage) {
//						HtmlPage hp = (HtmlPage) x.page;
//						System.out.println(hp.toString());
//						System.out.println(hp.asText());
//					}
//				}
//				i++;
//			}
			e.printStackTrace();
			throw new ApplicationException("Ein Fehler ist aufgetreten: " + e.toString());
		}
	}

	private void addUmsaetze(Konto konto, List<Umsatz> fetched) throws RemoteException, ApplicationException {
		// Dann muessen wir die jetzt noch mit den bereits in der Datenbank vorhandenen Umsaetzen
		// abgleichen, um nur die in der Datenbank zu speichern, die wir noch nicht haben
		Date oldest = null;

		for (Umsatz umsatz:fetched)
		{
			if (oldest == null || umsatz.getDatum().before(oldest))
				oldest = umsatz.getDatum();
		}

		// Wir holen uns die Umsaetze seit dem letzen Abruf von der Datenbank
		GenericIterator<Umsatz> existing = konto.getUmsaetze(oldest,null);

		// Liste der vorgemerkte Buchungen
		// Diese müssen, falls sie nicht mehr vorgemerkt sind, gelöscht werden
		List<Umsatz> vorgemerkte = entferneVorgemerkteBuchungen(existing);

		
		// Umsätze ggf. hinzufügen
		for (Umsatz umsatz:fetched) {
			if (existing.contains(umsatz) != null) {
				vorgemerkte.remove(umsatz);
				continue;
			}
			if (check(existing, umsatz)) {
				vorgemerkte.remove(umsatz);
				continue;
			}
			umsatz.store();
			Application.getMessagingFactory().sendMessage(new ImportMessage(umsatz));
		}
		
		// Alle alten vorgemerkte Umsätze löschen
		for (Umsatz x: vorgemerkte) {
			x.delete();
		}
	}

	/**
	 * Setzt das Pwd zurück
	 * 
	 * @param konto
	 * @param v
	 * @throws RemoteException
	 */
	private void resetPwd(Konto konto, BaseZugang v) throws RemoteException {
		// Remove Pwd from Cache
		storedValues.remove(konto.getID());
		
		// Remove Pwd from Metadata
		if (v != null) {
			for (PropertyElement x :v.getPropertyNames()) {
				if (x.isPwd()) {
					konto.setMeta(x.getDescription(), "");
				}
			}
		}
	}
	
	/**
	 * Prüft die Properties und frägt den User ggf. nach dem Passwort
	 * @param konto
	 * @param info
	 * @param v
	 * @throws RemoteException
	 * @throws Exception
	 */
	private void checkProperties(Konto konto, HashMap<String, String> info, BaseZugang v) throws RemoteException, Exception {
		for (PropertyElement x :v.getPropertyNames()) {
			String value = info.get(x.getHashname());

			// Check for Password
			if (x.isPwd() && (value == null || value.isEmpty())) {
				// Check for cached PWD
				value = storedValues.get(konto.getID());
				info.put(x.getHashname(), value);
				// If still empty, ask the user
				if ((value == null || value.isEmpty())) {
					value = Application.getCallback().askPassword(v.getName());
					// Store the password
					if (storing) {
						konto.setMeta(x.getDescription(), value);
					}
					// Cache the password
					if (caching) {
						storedValues.put(konto.getID(), value);
					}
					info.put(x.getHashname(), value);
				}
			}
			
			// TODO Check for empty values
		}
	}

	/**
	* Prüft, ob der Umsatz bereits existiert.
    * Hierbei werden aber Leerzeichen im Verwendungszweck ignoriert und der Vergleich der Beträge, darf der Unterschied 0,001 Cent betragen
	*/
	private boolean check(GenericIterator<Umsatz> existing, Umsatz umsatz) throws RemoteException {
		existing.begin();
		while (existing.hasNext()) {
			Umsatz exist = existing.next();
			if (exist.getDatum().equals(umsatz.getDatum())) {
				boolean sameBetrag = Math.abs(exist.getBetrag() - umsatz.getBetrag()) < 0.0001;
				boolean sameText = getVerwendung(exist).equals(getVerwendung(umsatz)); 
				if (sameBetrag && sameText) {
					return true;
				}
			}
		}
		return false;
	}

	private List<Umsatz> entferneVorgemerkteBuchungen(GenericIterator<Umsatz> existing)
			throws RemoteException, ApplicationException {
		List<Umsatz> ungebuchteUmsaetze = new ArrayList<Umsatz>();
		existing.begin();
		while (existing.hasNext()) {
			Umsatz x = (Umsatz) existing.next();
			if ((x.getFlags() & Umsatz.FLAG_NOTBOOKED) > 0) {
				ungebuchteUmsaetze.add(x);
			}
		}
		return ungebuchteUmsaetze;
	}

	private String getVerwendung(Umsatz u) throws RemoteException {
		String v = ((u.getZweck() == null) ? "" :  u.getZweck()) + 
					((u.getZweck2() == null) ? "" :  u.getZweck2());
		for (String x : u.getWeitereVerwendungszwecke()) {
			v += x;
		}
		return v.replace(" ", "");
		
	}
	private List<Umsatz> umsatz2umsatz(Konto konto, List<Umsaetze> umsatzliste) throws RemoteException {
		List<Umsatz> fetched = new ArrayList<Umsatz>();
		for (Umsaetze f : umsatzliste) {
			// Umsatz-Objekt erstellen
			Umsatz newUmsatz = (Umsatz) Settings.getDBService().createObject(Umsatz.class,null);
			newUmsatz.setKonto(konto);
			newUmsatz.setBetrag(f.getBetrag().doubleValue());
			newUmsatz.setDatum(f.getBuchung());
			newUmsatz.setGegenkontoBLZ(f.getGegenBIC());
			//			newUmsatz.setGegenkontoName(...);
			newUmsatz.setGegenkontoNummer(f.getGegenIBAN()); 
			//			newUmsatz.setSaldo(); // Zwischensaldo
			newUmsatz.setValuta(f.getWertstellung());
			newUmsatz.setWeitereVerwendungszwecke(Utils.parse(f.getBuchungstext()));
			if (f.getSaldo() != null) {
				newUmsatz.setSaldo(f.getSaldo().doubleValue());
			}
			if (f.isVorgemerkt()) {
				newUmsatz.setFlags(Umsatz.FLAG_NOTBOOKED);
				newUmsatz.setSaldo(0.0d);
			}
			fetched.add(newUmsatz);
		}
		return fetched;
	}
}


