package de.open4me.ly.Addon.zugang.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.open4me.ly.Addon.zugang.BaseZugang;
import de.open4me.ly.hibiscusAddon.dialogs.DebugDialogWithTextarea;
import de.open4me.ly.webscraper.runner.Runner;
import de.open4me.ly.webscraper.runner.Runner.ResultSets;
import de.willuhn.util.ApplicationException;

public abstract class BaseZugangRunner extends BaseZugang {

	protected Runner r;

	public BaseZugangRunner() {
		r = new Runner() {
			
		};
	}
	
	protected String getRunnerScript() {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			InputStream is = this.getClass().getResourceAsStream(getClass().getSimpleName() + ".rnn");
			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();	
				}
			}
		}
		return sb.toString();
	}
	
	protected abstract void auswerten() throws Exception;
	
	@Override
	public void run() throws Exception {
		setSaldo(null);
		umsatzliste.clear();
		r.setInfo(getInfo());
		r.setCode(getRunnerScript());
		if (!r.run()) {
			ArrayList<ResultSets> results = r.getResults();
			List<String> s = new ArrayList<String>();
			for (int i = results.size() - 1; i > 0; i--) {
				ResultSets ret = results.get(i);
				s.add(ret.command);
				if (ret.htmlcode == null || ret.htmlcode.isEmpty()) {
					continue;
				}
				s.add(ret.htmlcode);
				DebugDialogWithTextarea dialog = new DebugDialogWithTextarea(0, s);
				dialog.open();
				break;
			}
			Exception e = results.get(results.size() - 1).e;
			throw new ApplicationException("Ein Fehler ist aufgetreten: " + e.getMessage());
		}
		auswerten();
	}

	
	

}
