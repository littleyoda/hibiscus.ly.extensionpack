package de.open4me.ly.hibiscusAddon;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.zugang.BaseZugang;
import de.willuhn.jameica.hbci.rmi.Konto;

public class Utils {
	
	  // Zerlegt einen String intelligent in max. 27 Zeichen lange Stücke
	  public static String[] parse(String line)
	  {
	    if (line == null || line.length() == 0)
	      return new String[0];
	    List<String> out = new ArrayList<String>();
	    String rest = line.trim();
	    int lastpos = 0;
	    while (rest.length() > 0) {
	    	if (rest.length() < 28) {
	    		out.add(rest);
	    		rest = "";
	    		continue;
	    	}
	    	int pos = rest.indexOf(' ', lastpos + 1);
	    	boolean zulang = (pos > 28) || pos == -1;
	    	// 1. Fall: Durchgehender Text mit mehr als 27 Zeichen ohne Space
	    	if (lastpos == 0 && zulang) {
	    		out.add(rest.substring(0, 27));
	    		rest = rest.substring(27).trim();
	    		continue;
	    	} 
	    	// 2. Fall Wenn der String immer noch passt, weitersuchen
	    	if (!zulang) {
	    		lastpos = pos;
	    		continue;
	    	}
	    	// Bis zum Space aus dem vorherigen Schritt den String herausschneiden
	    	out.add(rest.substring(0, lastpos));
	    	rest = rest.substring(lastpos + 1).trim();
	    	lastpos = 0;
	    }
	    return out.toArray(new String[0]);
	  }

	  public static HashMap<String, String> getKontoInformationen(Konto konto) throws RemoteException {
			HashMap<String, String> kontoInfo = new HashMap<String, String>();
			kontoInfo.put("blz", konto.getBLZ());
			kontoInfo.put("bic", konto.getBic());
			kontoInfo.put("iban", konto.getIban());
			kontoInfo.put("userid", konto.getKundennummer());
			kontoInfo.put("subid", konto.getUnterkonto());
			kontoInfo.put("nummer", konto.getKontonummer());
			return kontoInfo;
		  
	  }

	public static void addProperties(HashMap<String, String> info, BaseZugang v, Konto konto) throws RemoteException {
		for (PropertyElement x : v.getPropertyNames()) {
			info.put(x.getHashname(), konto.getMeta(x.getDescription(), null));
		}
	}
}
