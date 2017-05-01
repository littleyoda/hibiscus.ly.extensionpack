package de.open4me.ly.Addon.zugang;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.open4me.ly.Addon.interfaces.Controller;
import de.open4me.ly.Addon.zugang.misc.Airplus;
import de.open4me.ly.Addon.zugang.misc.BahnBonus;
import de.open4me.ly.Addon.zugang.misc.Barclays;
import de.open4me.ly.Addon.zugang.runner.Handyticket;
import de.open4me.ly.Addon.zugang.runner.Payback;
import de.open4me.ly.Addon.zugang.runner.Shoop;
import de.open4me.ly.Addon.zugang.runner.Varengold;

public class ZugangsFabrik {
	
	static List<BaseZugang> getZugaenge() {
		return Arrays.asList(new Varengold(), new Handyticket(), new Payback(), new Airplus(), new BahnBonus(), new Barclays(), new Shoop());
	}

	public static BaseZugang getZugang(HashMap<String, String> info, Controller controller) {
		for (BaseZugang x : getZugaenge()) {
			if (x.supports(info)) {
				x.setController(controller);
				return x;
			}
		}
		return null;
	}

}
