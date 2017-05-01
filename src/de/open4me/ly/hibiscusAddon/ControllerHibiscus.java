package de.open4me.ly.hibiscusAddon;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;

import de.open4me.ly.Addon.interfaces.Controller;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;

public class ControllerHibiscus implements Controller {

	private static ControllerHibiscus instance;

	public static ControllerHibiscus getInstance() {
		if (instance == null) {
			instance = new ControllerHibiscus();
		}
		return instance;
	}
	private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

	@Override
	public String i18ntr(String key, String... replacements) {
		return i18n.tr(key, replacements);
	}

	@Override
	public void setProxyCfg(WebClient webClient, String url) throws URISyntaxException {
		boolean useSystem = Application.getConfig().getUseSystemProxy();
		ProxyConfig pc = null;
		if (useSystem) {
			List<Proxy> proxies = ProxySelector.getDefault().select(new URI(url));
			Logger.info("Using system proxy settings: " + proxies);
			for (Proxy p : proxies) {
				if (p.type() == Proxy.Type.HTTP && p.address() instanceof InetSocketAddress) {
					pc = new ProxyConfig();
					InetSocketAddress addr = (InetSocketAddress) p.address();
					pc.setProxyHost(addr.getHostString());
					pc.setProxyPort(addr.getPort());
					webClient.getOptions().setProxyConfig(pc);
					Logger.info("Setting Proxy to " + pc);
					return;
				}
			}
			Logger.error("No default Proxy found");
		} else {
			String host = Application.getConfig().getHttpsProxyHost();
			int port = Application.getConfig().getHttpsProxyPort();
			if (host != null && host.length() > 0 && port > 0) {
				pc = new ProxyConfig();
				pc.setProxyHost(host);
				pc.setProxyPort(port);
				webClient.getOptions().setProxyConfig(pc);
				Logger.info("Setting Proxy to " + pc);
				return;
			}
		}
		Logger.info("Keine gültige Proxy-Einstellunge gefunden. (" + useSystem + ")");
	}

	@Override
	public void log(Level info, String message) {
		if (info.equals(Level.SEVERE)) {
			Logger.error(message);
		} else if (info.equals(Level.WARNING)) {
			Logger.warn(message);
		} else if (info.equals(Level.INFO)) {
			Logger.info(message);
		} else if (info.equals(Level.CONFIG)) {
			Logger.debug(message);
		} else if (info.equals(Level.FINE)) {
			Logger.debug(message);
		} else if (info.equals(Level.FINER)) {
			Logger.trace(message);
		} else if (info.equals(Level.FINEST)) {
			Logger.trace(message);
		} else {
			Logger.error(message);
		}
	}

	@Override
	public void notifyUser(String string) {
		try {
			Application.getCallback().notifyUser("Aufgrund von Wartungsarbeiten ist bei der Barclays Bank z.Z. kein Abruf von Kontoinformationen möglich.");
		} catch (Exception e) {
			log(Level.SEVERE, e.getMessage());
		}
	}

}
