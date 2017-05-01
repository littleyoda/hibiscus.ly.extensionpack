package de.open4me.ly.Addon.interfaces;

import java.net.URISyntaxException;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.WebClient;

public interface Controller {

		public String i18ntr(String key, String... replacements);

		public void setProxyCfg(WebClient webClient, String string) throws URISyntaxException;

		public void log(Level info, String string);

		public void notifyUser(String string);


}
