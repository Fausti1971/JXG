package parm;

import java.io.*;
import java.util.HashMap;
import application.XGLoggable;
import device.XGDevice;
import tag.XGTagable;
import xml.XMLNode;
import xml.XMLNodeConstants;

public class XGParameterTable extends HashMap<Integer, XGParameter> implements XGTagable, XMLNodeConstants, XGLoggable, XGParameterConstants
{
	private static final long serialVersionUID = 1L;
	private static int count = 0;

	public static void init(XGDevice dev)
	{	try
		{
			XMLNode n = XMLNode.parse(dev.getResourceFile(XML_PARAMETER));
			for(XMLNode t : n.getChildNodes(TAG_PARAMETERTABLE))
			{	dev.getParameterTables().add(new XGParameterTable(dev, t));
			}
		}
		catch(IOException e)
		{	LOG.info(e.getMessage());
		}
	}

/*****************************************************************************************/

	private final String name;

	public XGParameterTable(XGDevice dev, XMLNode n)
	{	this.name = n.getStringAttribute(ATTR_NAME);
		for(XMLNode pn : n.getChildNodes(TAG_PARAMETER))
		{	XGParameter p = new XGParameter(dev, pn);
			int index = pn.getValueAttribute(ATTR_SELECTORVALUE, DEF_SELECTORVALUE);
			this.put(index, p);
		}
		LOG.info(this.getClass().getSimpleName() + " " + this.name + " initialized");
	}

	public XGParameterTable(XGDevice dev)//Dummy-Parameter-Table für immutable Opcodes
	{	this.name = dev + "_anonymousParameterTable_" + count++;
	}

	@Override public String getTag()
	{	return name;
	}
}
