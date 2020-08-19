package module;

import java.awt.event.ActionEvent;
import java.util.LinkedHashSet;
import java.util.Set;
import adress.XGAddress;
import adress.XGAddressableSet;
import adress.XGBulkDump;
import device.XGDevice;
import gui.XGTreeNode;
import gui.XGWindow;
import value.XGValue;
import xml.XMLNode;

public class XGModuleInstance extends XGModule
{	static final Set<String> ACTIONS = new LinkedHashSet<>();

	static
	{	ACTIONS.add(ACTION_EDIT);
		ACTIONS.add(ACTION_REQUEST);
		ACTIONS.add(ACTION_TRANSMIT);
//		ACTIONS.add(ACTION_LOADFILE);
//		ACTIONS.add(ACTION_SAVEFILE);
	}

/***************************************************************************************************************/

	private final Set<XGValue> info = new LinkedHashSet<>();
	private final XGAddressableSet<XGBulkDump> bulks;

	public XGModuleInstance(XGDevice dev, XGTreeNode par, String cat, XGAddress adr, XMLNode cfg)
	{	super(dev, par, cat, adr, cfg);
		this.bulks = XGBulkDump.init(this, cfg);
		this.registerValueListener(cfg);
	}

	void registerValueListener(XMLNode n)
	{	Set<String> set = new LinkedHashSet<>();
		set.add(n.getStringAttribute(ATTR_INFO1).toString());
		set.add(n.getStringAttribute(ATTR_INFO2).toString());
		set.add(n.getStringAttribute(ATTR_INFO3).toString());

		for(String s : set)
		{	XGAddress a = new XGAddress(s, this.address);
			XGValue v = this.device.getValues().get(a);
			if(v != null)
			{	this.info.add(v);
				v.addValueListener(this);
			}
		}
	}

	@Override public boolean isLeaf()
	{	return true;
	}

	@Override public Set<String> getContexts()
	{	return ACTIONS;
	}

	@Override public void actionPerformed(ActionEvent e)
	{	switch(e.getActionCommand())
		{	case ACTION_EDIT:		if(this.window == null) new XGWindow(this, XGWindow.getRootWindow(), false, this.getDevice() + "/" + this.category + " " + this.getTranslatedID());
									else this.window.toFront();
									break;
			case ACTION_REQUEST:	this.transmitAll(this.device.getMidi(), this.device.getValues()); break;
			case ACTION_TRANSMIT:	this.transmitAll(this.device.getValues(), this.device.getMidi()); break;
		}
	}

	@Override public String toString()
	{	String s = this.getTranslatedID() + ":\t";
		if(this.idTranslator == null) s = this.category + ":\t";
		else s = this.idTranslator.getByIndex(this.getID()) + ":\t";
		for(XGValue v : this.info) s += "\t" + v.getInfo();
		return s;
	}

	@Override public XGAddressableSet<XGBulkDump> getBulks()
	{	return this.bulks;
	}

	@Override public void contentChanged(XGValue v)
	{	this.repaintNode();
	}
}
