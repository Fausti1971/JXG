package module;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.tree.TreeNode;
import adress.InvalidXGAddressException;
import adress.XGAddress;
import adress.XGAddressField;
import adress.XGAddressable;
import adress.XGAddressableSet;
import application.*;
import device.XGDevice;
import gui.XGComponent;
import gui.XGTreeNode;
import gui.XGWindow;
import gui.XGWindowSource;
import msg.XGBulkDumper;
import value.XGValue;

public class XGModule implements XGAddressable, XGModuleConstants, XGLoggable, XGBulkDumper
{
	static final Set<String> ACTIONS = new LinkedHashSet<>();

	static
	{	ACTIONS.add(ACTION_EDIT);
		ACTIONS.add(ACTION_REQUEST);
		ACTIONS.add(ACTION_TRANSMIT);
		ACTIONS.add(ACTION_LOADFILE);
		ACTIONS.add(ACTION_SAVEFILE);
		ACTIONS.add(ACTION_RESET);
	}

/***************************************************************************************************************/

	private final Set<XGValue> infoValues = new LinkedHashSet<>();
	private final XGAddress address;
	private final XGModuleType type;
	private XGWindow window;
	private boolean selected;

	public XGModule(XGModuleType mt, int id) throws InvalidXGAddressException
	{	this.type = mt;
		this.address = new XGAddress(mt.getAddress().getHi(), new XGAddressField(id), mt.getAddress().getLo());

		for(XGAddress adr : this.type.getInfoAddresses())
		{	try
			{	XGValue v = this.type.getDevice().getValues().get(adr.complement(this.address));
				this.infoValues.add(v);
//				v.addValueListener((XGValue val)->{this.repaintNode();});
			}
			catch(InvalidXGAddressException e)
			{	LOG.warning(e.getMessage());
			}
		}
	}

	public XGModuleType getType()
	{	return this.type;
	}

	public boolean isSingleton()
	{	return this.type.getAddress().getMid().isFix();
	}

	public XGAddressableSet<XGValue> getValues()
	{	return this.type.getDevice().getValues().getAllIncluded(this.address);
	}

	public String getTranslatedID()
	{	try
		{	return this.type.idTranslator.getByIndex(this.address.getMid().getValue()).getName();
		}
		catch(InvalidXGAddressException | NullPointerException e)
		{	return this.address.getMid().toString();
		}
	}

	public void resetValues()
	{	for(XGValue v : this.getValues()) v.setDefaultValue();
	}

	//@Override public void actionPerformed(ActionEvent e)
	//{	XGDevice dev = this.type.getDevice();
	//	switch(e.getActionCommand())
	//	{	case ACTION_EDIT:		this.editWindow(); break;
	//		case ACTION_REQUEST:	new Thread(() -> {this.transmitAll(dev.getMidi(), dev.getValues());}).start(); break;
	//		case ACTION_TRANSMIT:	new Thread(() -> {this.transmitAll(dev.getValues(), dev.getMidi());}).start(); break;
	//		case ACTION_RESET:		if(JOptionPane.showConfirmDialog(XGWindow.getRootWindow(), "Do you really want to reset " + this, "Reset Module?", JOptionPane.CANCEL_OPTION) == JOptionPane.OK_OPTION) this.resetValues(); break;
	//		default:				JOptionPane.showMessageDialog(XGWindow.getRootWindow(), "action not implemented: " + e.getActionCommand());
	//	}
	//}

	@Override public String toString()
	{	return this.type.getName() + " " + this.getTranslatedID();
	}

	@Override public XGAddressableSet<XGAddress> getBulks()
	{	XGAddressableSet<XGAddress> set = new XGAddressableSet<>();
		for(XGAddress bd : this.type.getBulkAdresses())
		{	try
			{	set.add(bd.getAddress().complement(this.address));
			}
			catch(InvalidXGAddressException e)
			{	LOG.warning(e.getMessage());
			}
		}
		return set;
	}

	@Override public XGAddress getAddress()
	{	return this.address;
	}
}
