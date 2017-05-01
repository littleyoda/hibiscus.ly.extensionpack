package de.open4me.ly.hibiscusAddon.actions;

import de.open4me.ly.hibiscusAddon.dialogs.EinrichtungsAssistenten;
import de.willuhn.jameica.gui.Action;

public class EinrichtungsassistentenAction  implements Action
{

	public void handleAction(Object context) 
	{
		EinrichtungsAssistenten a = new EinrichtungsAssistenten(EinrichtungsAssistenten.POSITION_CENTER);
		try {
			a.open();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

