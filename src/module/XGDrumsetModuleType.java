package module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import adress.InvalidXGAddressException;
import adress.XGAddress;
import adress.XGAddressableSet;
import adress.XGMemberNotFoundException;
import device.XGDevice;
import value.*;
import xml.XMLNode;

public class XGDrumsetModuleType extends XGModuleType
{

/****************************************************************************************************************************/

	public XGDrumsetModuleType(XMLNode n, XGAddress adr)
	{	super(n, adr, n.getStringAttributeOrDefault(ATTR_NAME, "Drumset"));
		int i = 48;
		try
		{	i = this.address.getHi().getValue();
			int nr = i - 47;
			this.id.append(" ").append(nr);
			this.name.append(" ").append(nr);
		}
		catch(InvalidXGAddressException e)
		{	LOG.severe(e.getMessage());
		}
	}
}
