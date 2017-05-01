package de.open4me.ly.hibiscusAddon;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import de.open4me.ly.Addon.interfaces.PropertyElement;
import de.open4me.ly.Addon.zugang.BaseZugang;
import de.open4me.ly.Addon.zugang.ZugangsFabrik;
import de.willuhn.annotation.Lifecycle;
import de.willuhn.annotation.Lifecycle.Type;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJob;
import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.plugin.Version;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;
import de.willuhn.util.ProgressMonitor;

/**
 * Implementierung eines Sync-Backends.
 */
@Lifecycle(Type.CONTEXT)
public class ExampleSynchronizeBackend extends AbstractSynchronizeBackend<SynchronizeJobProviderInterface>
{
	private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();


	/**
	 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend#createJobGroup(de.willuhn.jameica.hbci.rmi.Konto)
	 */
	@Override
	protected JobGroup createJobGroup(Konto k)
	{
		return new ExampleJobGroup(k);
	}

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend#getJobProviderInterface()
	 */
	@Override
	protected Class<SynchronizeJobProviderInterface> getJobProviderInterface()
	{
		return SynchronizeJobProviderInterface.class;
	}

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend#getPropertyNames(de.willuhn.jameica.hbci.rmi.Konto)
	 */
	@Override
	public List<String> getPropertyNames(Konto konto)
	{

		try
		{
			if (konto == null || konto.hasFlag(Konto.FLAG_DISABLED) || konto.hasFlag(Konto.FLAG_OFFLINE)) {
				return null;
			}
			
			// Find matching Konto
			HashMap<String, String> info = Utils.getKontoInformationen(konto);
			BaseZugang bz = ZugangsFabrik.getZugang(info, ControllerHibiscus.getInstance());
			if (bz == null) {
				return null;
			}
			boolean supported = supportPwdType();
			
			// Create PropertyName-List
			List<String> list = new ArrayList<String>();
			List<PropertyElement> propList = bz.getPropertyNames();
			list.addAll(propList.stream().map( 
					p->  {
						if (p.isPwd() && supported) {
							return p.getDescription() + "(pwd)";	
						} 
						return p.getDescription();	
					}).collect(Collectors.toList()));
			return list;
		}
		catch (RemoteException re)
		{
			Logger.error("unable to determine property-names",re);
			return null;
		}
	}

	/**
	 * Das Password-Feature benötigt mindestens Version 2.6.20
	 * @return
	 */
	private boolean supportPwdType() {
		Version sollVersion = new Version("2.6.19");
		for (Manifest x:Application.getPluginLoader().getManifests()) {
			if (x.getName().equals("hibiscus")) {
				Version v = x.getVersion();
				return sollVersion.compareTo(v) < 0;
			}
		}
		return false;
	}

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.SynchronizeBackend#supports(java.lang.Class, de.willuhn.jameica.hbci.rmi.Konto)
	 */
	@Override
	public boolean supports(Class<? extends SynchronizeJob> type, Konto konto)
	{
		boolean b = super.supports(type,konto);
		if (!b)
			return false;

		try
		{
			if ((konto.getBackendClass() != null && konto.getBackendClass().equals(getClass().getName()))) {
				// weitere Tests
				return true;
			}


		}
		catch (RemoteException re)
		{
			Logger.error("unable to check support for this account",re);
		}
		return false;
	}
	

	/**
	 * @see de.willuhn.jameica.hbci.synchronize.SynchronizeBackend#getName()
	 */
	@Override
	public String getName()
	{
		return "Diverse Addons";
	}

	/**
	 * Hier findet die eigentliche Ausfuehrung des Jobs statt.
	 */
	protected class ExampleJobGroup extends JobGroup
	{
		/**
		 * ct.
		 * @param k
		 */
		protected ExampleJobGroup(Konto k)
		{
			super(k);
		}

		/**
		 * @see de.willuhn.jameica.hbci.synchronize.AbstractSynchronizeBackend.JobGroup#sync()
		 */
		@Override
		protected void sync() throws Exception
		{
			////////////////////////////////////////////////////////////////////
			// lokale Variablen
			ProgressMonitor monitor = worker.getMonitor();
			String kn               = this.getKonto().getLongName();

			int step = 100 / worker.getSynchronization().size();
			////////////////////////////////////////////////////////////////////

			try
			{
				this.checkInterrupted();

				monitor.log(" ");
				monitor.log(i18n.tr("Synchronisiere Konto: {0}",kn));

				Logger.info("processing jobs");
				for (SynchronizeJob job:this.jobs)
				{
					this.checkInterrupted();

					JobInterfaceInterface j = (JobInterfaceInterface) job;
					j.execute();

					monitor.addPercentComplete(step);
				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}

	}

}


