package device;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.tree.TreeNode;
import adress.InvalidXGAddressException;
import adress.XGAddressConstants;
import adress.XGAddressableSet;
import application.Configurable;
import application.JXG;
import file.XGSysexFile;
import file.XGSysexFileConstants;
import gui.XGContext;
import gui.XGDeviceDetector;
import gui.XGFileSelector;
import gui.XGFrame;
import gui.XGSpinner;
import gui.XGTemplate;
import gui.XGTree;
import gui.XGTreeNode;
import gui.XGWindow;
import gui.XGWindowSource;
import module.XGModule;
import msg.XGBulkDump;
import msg.XGBulkDumper;
import msg.XGMessageBulkRequest;
import msg.XGMessageParameterChange;
import msg.XGMessenger;
import msg.XGMessengerException;
import msg.XGRequest;
import msg.XGResponse;
import parm.XGParameter;
import parm.XGTable;
import tag.XGTagableSet;
import value.ChangeableContent;
import value.XGValueStore;
import xml.XMLNode;

public class XGDevice implements XGDeviceConstants, Configurable, XGTreeNode, XGContext, XGWindowSource, XGBulkDumper
{
	private static Set<XGDevice> DEVICES = new LinkedHashSet<>();
//	private static XGDevice DEF = new XGDevice();
	static
	{	ACTIONS.add(ACTION_CONFIGURE);
		ACTIONS.add(ACTION_REMOVE);
		ACTIONS.add(ACTION_LOADFILE);
		ACTIONS.add(ACTION_SAVEFILE);
		ACTIONS.add(ACTION_REQUEST);
		ACTIONS.add(ACTION_TRANSMIT);
		ACTIONS.add(ACTION_RESET);
		ACTIONS.add(ACTION_XGON);
	}

	public static Set<XGDevice> getDevices()
	{	return DEVICES;
	}

//	public static XGDevice getDefaultDevice()
//	{	return DEF;
//	}

	public static void init(XMLNode x)
	{	for(XMLNode n : x.getChildNodes(TAG_DEVICE))
		{
			new Thread(()->
			{
				XGDevice d = null;
				try
				{	d = new XGDevice(n);
					synchronized(DEVICES)
					{	if(DEVICES.add(d)) d.reloadTree();
					}
				}
				catch(InvalidXGAddressException e)
				{	LOG.severe(e.getMessage());
				}
			}).start();
		}
		LOG.info(DEVICES.size() + " devices initialized");
	}

	private static void removeDevice(XGDevice dev)
	{	if(JOptionPane.showConfirmDialog(XGWindow.getRootWindow(), "Do you really want to remove " + dev, "remove device...", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
		if(DEVICES.remove(dev))
		{	dev.exit();
			dev.getConfig().removeNode();
			dev.reloadTree();
			LOG.info(dev + " removed");
		}
		else LOG.info(dev + " not found");
	}

/***************************************************************************************************************************/

	private final ChangeableContent<Integer> sysex = new ChangeableContent<Integer>()
	{	@Override public Integer getContent()
		{	return getSysexID();
		}
		@Override public boolean setContent(Integer i)
		{	int old = getSysexID();
			setSysexID(i);
			return old != i;
		}
	};
	private final StringBuffer defaultFileName, name;
	private XGTree tree;
	private Color color;
	private boolean isSelected = false;

	private XMLNode config;
	private final XGTagableSet<XGTable> tables = new XGTagableSet<>();
	private final XGTagableSet<XGParameter> parameters = new XGTagableSet<>();
	private final XGAddressableSet<XGTemplate> templates = new XGAddressableSet<>();
	private final Set<XGModule> modules = new LinkedHashSet<>();

	private int info1, info2;
	private final XMLNode files;
	private XGSysexFile defaultFile;
	private XGMidi midi;
	private final XGValueStore values;
	private int sysexID;
	private XGWindow childWindow;

	public XGDevice(XMLNode cfg) throws InvalidXGAddressException
	{	this.config = cfg;
		if(this.config == null)
		{	this.config = new XMLNode(TAG_DEVICE);
			this.name = this.config.getStringBufferAttributeOrNew(ATTR_NAME, DEF_DEVNAME);
			this.midi = new XGMidi(this);
			this.files = this.config.getChildNodeOrNew(TAG_FILES);
			this.defaultFileName = this.files.getStringBufferAttributeOrNew(ATTR_DEFAULTDUMPFILE, HOMEPATH.resolve("default.syx").toString());
			this.configure();
		}
		else
		{	this.name = this.config.getStringBufferAttributeOrNew(ATTR_NAME, DEF_DEVNAME);
			this.midi = new XGMidi(this);
			this.files = this.config.getChildNodeOrNew(TAG_FILES);
			this.defaultFileName = this.files.getStringBufferAttributeOrNew(ATTR_DEFAULTDUMPFILE, HOMEPATH.resolve("default.syx").toString());
		}
		this.sysex.setContent(this.config.getIntegerAttribute(ATTR_SYSEXID, DEF_SYSEXID));
		this.setColor(new Color(this.config.getIntegerAttribute(ATTR_COLOR, DEF_DEVCOLOR)));
		this.values = new XGValueStore(this);

		XGTemplate.init(this);
		XGTable.init(this);
		XGParameter.init(this);
		XGModule.init(this);//initialisiert und instanziert auch XGBulkDump, XGOpcode und XGValue

		try
		{	this.defaultFile = new XGSysexFile(this, this.defaultFileName.toString());
			this.defaultFile.parse();
			this.transmitAll(this.defaultFile, this.values);
		}
		catch(IOException e)
		{	LOG.severe(e.getMessage());
		}
		LOG.info("device initialized: " + this);
	}

	public void exit()
	{	this.midi.close();
	}

	public File getResourceFile(String fName) throws FileNotFoundException
	{	Path extPath = this.getResourcePath();
		File extFile = extPath.resolve(fName).toFile();
		if(extFile.canRead()) return extFile;
		throw new FileNotFoundException("can't read file " + extFile);//TODO: interner Pfad wird außerhalb von eclipse nicht aufgelöst!
	}

	public Path getResourcePath()
	{	return HOMEPATH.resolve(this.name.toString());
	}

	public Color getColor()
	{	return color;
	}

	public void setColor(Color color)
	{	this.color = color;
	}

	public XGTagableSet<XGParameter> getParameters()
	{	return this.parameters;
	}

	public XGValueStore getValues()
	{	return this.values;
	}

	public XGAddressableSet<XGTemplate> getTemplates()
	{	return this.templates;
	}

	public XGTagableSet<XGTable> getTables()
	{	return this.tables;
	}

	public Set<XGModule> getModules()
	{	return this.modules;
	}

	public int getSysexID()
	{	return this.sysexID;
	}

	public void setSysexID(int id)
	{	this.sysexID = id & 0xF;
		this.config.setIntegerAttribute(ATTR_SYSEXID, this.sysexID);
	}

	public void requestInfo()	//SystemInfo ignoriert parameterrequest?!;
	{	XGRequest m;
		try
		{	m = new XGMessageBulkRequest(this.midi, this.midi, XGAddressConstants.XGMODELNAMEADRESS);// TODO: Krücke, für dest existiert hier noch kein XGMessenger, der allerdings für getDevice() erforderlich ist
			try
			{	m.request();
				if(m.isResponsed())
				{	XGResponse r = m.getResponse();
					String s = r.getString(r.getBaseOffset(), r.getBaseOffset() + r.getBulkSize());
					this.name.replace(0, this.name.length(), s.trim());
				}
				else JOptionPane.showMessageDialog(this.getChildWindow(), "no response for " + m);
			}
			catch(InvalidXGAddressException | XGMessengerException e)
			{	LOG.severe(e.getMessage());
			}
		}
		catch(InvalidXGAddressException | InvalidMidiDataException e)
		{	LOG.severe(e.getMessage());
		}
	}

	private void resetXG()
	{	try
		{	new XGMessageParameterChange(this.values, this.midi, new byte[]{0,0,0,0,0,0,0x7E,0,0}, true).transmit();
		}
		catch(InvalidXGAddressException|InvalidMidiDataException | XGMessengerException e1)
		{	LOG.severe(e1.getMessage());
		}
	}

	private void resetAll()
	{	try
		{	new XGMessageParameterChange(this.values, this.midi, new byte[]{0,0,0,0,0,0,0x7F,0,0}, true).transmit();
		}
		catch(InvalidXGAddressException|InvalidMidiDataException | XGMessengerException e1)
		{	LOG.severe(e1.getMessage());
		}
	}

	private void load()
	{	XMLNode last = this.files.getLastChildOrNew(TAG_ITEM);
		XGFileSelector fs = new XGFileSelector(last.getTextContent(), "open sysex file...", "open", XGSysexFileConstants.SYX_FILEFILTER);
		switch(fs.select(this.childWindow))
		{	case JFileChooser.APPROVE_OPTION:
			{	XGSysexFile f;
				try
				{	f = new XGSysexFile(this, last.getTextContent().toString());
					f.parse();
					this.transmitAll(f, this.values);
					f.close();
				}
				catch(IOException e)
				{	LOG.severe(e.getMessage());
				}
				break;
			}
			case JFileChooser.CANCEL_OPTION:
			{	last.removeNode();
				LOG.severe("fileselection aborted");
				break;
			}
		}
	}

	private void save()
	{	XMLNode last = this.files.getLastChildOrNew(TAG_ITEM);
		XGFileSelector fs = new XGFileSelector(last.getTextContent(), "save sysex file...", "save", XGSysexFileConstants.SYX_FILEFILTER);
		switch(fs.select(this.childWindow))
		{	case JFileChooser.APPROVE_OPTION:
			{	XGSysexFile f;
				try
				{	f = new XGSysexFile(this, last.getTextContent().toString());
					this.transmitAll(this.values, f);
					f.save();
					f.close();
				}
				catch(IOException e)
				{	LOG.severe(e.getMessage());
				}
				break;
			}
			case JFileChooser.CANCEL_OPTION:
			{	last.removeNode();
				LOG.severe("fileselection aborted");
				break;
			}
		}
	}

	@Override public XGAddressableSet<XGBulkDump> getBulks()
	{	XGAddressableSet<XGBulkDump> set = new XGAddressableSet<>();
		for(XGModule m : this.modules) set.addAll(m.getBulks());
		return set;
	}

	public XGMessenger getMidi()
	{	return this.midi;
	}

	public void configure()
	{	new XGWindow(this, XGWindow.getRootWindow(), true, this.toString());
	}

	@Override public int hashCode()
	{	return HASH * this.midi.hashCode() + HASH * this.sysexID;
	}

	@Override public boolean equals(Object o)
	{	if(this == o) return true;
		if(!(o instanceof XGDevice)) return false;
		return this.hashCode() == o.hashCode();
	}

	@Override public String toString()
	{	return this.name.toString() + "." + this.sysexID;
	}

	@Override public XMLNode getConfig()
	{	return this.config;
	}

	@Override public TreeNode getParent()
	{	return JXG.getApp();
	}

	@Override public boolean getAllowsChildren()
	{	return true;
	}

	@Override public Enumeration<? extends TreeNode> children()
	{	return Collections.enumeration(this.modules);
	}

	@Override public boolean isSelected()
	{	return this.isSelected;
	}

	@Override public void setSelected(boolean s)
	{	this.isSelected = s;
	}

	@Override public Set<String> getContexts()
	{	return ACTIONS;
	}

	@Override public void actionPerformed(ActionEvent e)
	{	switch(e.getActionCommand())
		{	case ACTION_CONFIGURE:	this.configure(); break;
			case ACTION_REMOVE:		removeDevice(this); break;
			case ACTION_LOADFILE:	this.load(); break;
			case ACTION_SAVEFILE:	this.save(); break;
			case ACTION_TRANSMIT:	new Thread(() -> {	this.transmitAll(this.values, this.midi);}).start(); break;
			case ACTION_REQUEST:	new Thread(() -> {	this.transmitAll(this.midi, this.values);}).start(); break;
			case ACTION_RESET:		this.resetAll(); break;
			case ACTION_XGON:		this.resetXG(); break;
			default:				break;
		}
	}

	@Override public XGWindow getChildWindow()
	{	return this.childWindow;
	}

	@Override public void setChildWindow(XGWindow win)
	{	this.childWindow = win;
	}

	@Override public void windowOpened(WindowEvent e)
	{	XGWindowSource.super.windowOpened(e);
		this.setSelected(true);
	}

	@Override public void windowClosed(WindowEvent e)
	{	XGWindowSource.super.windowClosed(e);
		this.setSelected(false);
	}

	@Override public JComponent getChildWindowContent()
	{	return this.getConfigComponent();
	}

	public JComponent getConfigComponent()
	{	GridBagConstraints gbc = new GridBagConstraints();
		XGFrame root = new XGFrame("device");
		root.setLayout(new GridBagLayout());

		JComponent c = this.midi.getConfigComponent();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		root.add(c, gbc);

		c = new XGSpinner("sysexID", this.sysex, 0, 15, 1);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		root.add(c, gbc);

		c = new XGDeviceDetector("device name", this.name, this);
		gbc.gridx = 0;
		gbc.gridy = 2;
		root.add(c, gbc);

		c = new XGFileSelector(this.defaultFileName, "default dump file", "select", XGSysexFileConstants.SYX_FILEFILTER).small();
		gbc.gridx = 0;
		gbc.gridy = 3;
		root.add(c, gbc);

		return root;
	}

	@Override public void setTreeComponent(XGTree t)
	{	this.tree = t;
	}

	@Override public XGTree getTreeComponent()
	{	return this.tree;
	}

	@Override public void nodeFocussed(boolean b)
	{
	}
}