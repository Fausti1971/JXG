package module;

import java.io.File;import java.io.IOException;import java.util.LinkedHashSet;
import java.util.Set;
import adress.*;
import bulk.XGBulk;import bulk.XGBulkDumper;import bulk.XGBulkType;import config.XGConfigurable;
import application.JXG;import application.XGLoggable;
import table.XGTable;import static table.XGTable.TABLES;import static table.XGVirtualTable.DEF_TABLE;import tag.XGTagable;import tag.XGTagableAddressableSet;import xml.XGProperty;import xml.XMLNode;

/**
 * Moduletypen, keine Instanzen
 * @author thomas
 *
 */
public class XGModuleType implements XGAddressable, XGModuleConstants, XGLoggable, XGBulkDumper, XGConfigurable, XGTagable
{
	public static final XGTagableAddressableSet<XGModuleType> TYPES = new XGTagableAddressableSet<>();//Prototypen (inkl. XGAddress bulks); initialisiert auch XGOpcodes
	static final Set<String> ACTIONS = new LinkedHashSet<>();

/**
* instanziiert Moduletypen, Bulktypen und Valuetypen (=XGOpcodes)
*/
	public static void init()
	{	XMLNode xml;
		XGDrumsetModuleType.init();
		try
		{	File f = JXG.getDeviceXMLResourceFile(XML_STRUCTURE);
			xml = XMLNode.parse(f);
		}
		catch(IOException e)
		{	LOG.severe(e.getMessage());
			return;
		}
		for(XMLNode n : xml.getChildNodes(TAG_MODULE))
		{	XGAddress adr = new XGAddress(n.getStringAttribute(ATTR_ADDRESS));
			if(adr.getHi().getMin() >= 48)//falls Drumset
			{	for(int h : adr.getHi())//erzeuge für jedes Drumset ein ModuleType
				{	try
					{	TYPES.add(new XGDrumsetModuleType(n, new XGAddress(new XGAddressField(h), adr.getMid(), adr.getLo())));
					}
					catch(InvalidXGAddressException e)
					{	LOG.severe(e.getMessage());
					}
				}
				continue;
			}
			TYPES.add(new XGModuleType(n));
		}
		LOG.info(TYPES.size() + " Module-Types initialized");
	}

	static
	{	ACTIONS.add(ACTION_EDIT);
		ACTIONS.add(ACTION_REQUEST);
		ACTIONS.add(ACTION_TRANSMIT);
		ACTIONS.add(ACTION_LOADFILE);
		ACTIONS.add(ACTION_SAVEFILE);
	}

/********************************************************************************************************************/

	private final Set<String> infoTags = new LinkedHashSet<>();
	private final XGTagableAddressableSet<XGBulkType> bulkTypes = new XGTagableAddressableSet<>();
	private final XGAddressableSet<XGModule> modules = new XGAddressableSet<>();
	protected final StringBuffer name;
	protected String tag;
	protected final XGAddress address;
	protected XGTable idTranslator;
	private final XMLNode config;

/**
* instanziiert Moduletypen, Bulktypen und Valuetypen
*/
	public XGModuleType(XMLNode cfg, XGAddress adr, String name)
	{	this.config = cfg;
		this.address = adr;
		this.name = new StringBuffer(name);
		this.tag = cfg.getStringAttribute(ATTR_ID);
		this.idTranslator = TABLES.getOrDefault(cfg.getStringAttribute(ATTR_TABLE), DEF_TABLE);

		for(XMLNode x : cfg.getChildNodes(TAG_BULK))
		{	this.bulkTypes.add(new XGBulkType(this, x));
		}

		for(XMLNode n : cfg.getChildNodes(TAG_INFO))
		{	String opc = n.getStringAttribute(ATTR_REF);
			if(opc != null) this.infoTags.add(opc);
		}
	}

	public XGModuleType(XMLNode cfg)
	{	this(cfg, new XGAddress(cfg.getStringAttribute(ATTR_ADDRESS)), cfg.getStringAttributeOrDefault(ATTR_NAME, DEF_MODULENAME));
	}

	public XGAddressableSet<XGModule> getModules(){ return this.modules;}

	public Set<String> getInfoTags(){ return this.infoTags;}

	@Override public XMLNode getConfig(){ return this.config;}

	@Override public void propertyChanged(XGProperty n){}

	public String getName(){ return this.name.toString();}

	public void resetValues(){	for(XGModule m : this.getModules()) m.resetValues();}

	@Override public String toString(){ return this.name.toString();}

	@Override public XGAddress getAddress(){ return this.address;}

	public XGAddressableSet<XGBulk> getBulks()
	{	XGAddressableSet<XGBulk> set = new XGAddressableSet<>();
		for(XGModule m : this.modules) set.addAll(m.getBulks());
		return set;
	}

	public String getTag(){ return this.tag;}

	public Set<XGBulkType> getBulkTypes(){	return this.bulkTypes;}
}