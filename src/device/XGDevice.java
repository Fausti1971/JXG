package device;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeNode;
import adress.InvalidXGAdressException;
import adress.XGAdress;
import adress.XGAdressConstants;
import application.Configurable;
import application.JXG;
import file.XGSysexFile;
import gui.Displayable;
import gui.GuiConstants;
import gui.XGTreeNode;
import gui.XGWindow;
import msg.XGMessageDumpRequest;
import msg.XGRequest;
import msg.XGResponse;
import obj.XGObjectType;
import opcode.XGOpcode;
import parm.XGParameter;
import parm.XGTranslationMap;
import tag.XGTagableSet;
import tag.XGTagdressableSet;
import value.XGValue;
import value.XGValueStore;
import xml.XMLNode;

public class XGDevice implements XGDeviceConstants, GuiConstants, Configurable, Displayable, XGTreeNode
{	private static Logger log = Logger.getAnonymousLogger();
	private static Set<XGDevice> DEVICES = new HashSet<>();
	private static XGDevice DEF = new XGDevice();

	public static Set<XGDevice> getDevices()
	{	return DEVICES;
	}

	public static XGDevice getDefaultDevice()
	{	return DEF;
	}

	public static void init()
	{	Set<XMLNode> deadDev = new HashSet<>();
		for(XMLNode n : JXG.getJXG().getConfig().getChildren())
		{	if(n.getTag().equals(TAG_DEVICE))
			{	try
				{	XGDevice d = new XGDevice(n);
					DEVICES.add(d);
					d.reloadTree(XGWindow.getRootWindow().getTree());
				}
				catch(InvalidXGAdressException|MidiUnavailableException | TimeoutException e)
				{	int result = JOptionPane.showConfirmDialog(JXG.getJXG().getWindow(), e.getMessage() + "\nremove from configuration?", "device not responding...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if(result == JOptionPane.YES_OPTION) deadDev.add(n);
					log.info("device initialisation aborted: " + e.getMessage());
				}
			}
		}
		for(XMLNode n : deadDev) n.removeNode();
		log.info(DEVICES.size() + " devices initialized / " + deadDev.size() + " devices removed");
	}

/***************************************************************************************************************************/

	private XGWindow window;
	private final XMLNode template;
	private final XMLNode config;
	private final XGValueStore values;
	private final XGTagdressableSet<XGOpcode> opcodes;
	private final XGTagableSet<XGObjectType> types;
	private final XGTagableSet<XGTranslationMap> translations;
	private final XGTagableSet<XGParameter> parameters;
	private final String name;
	private final int info1, info2;
	private Path defDumpPath;
	private final Queue<XGSysexFile> files = new LinkedList<>();
	private final XGMidi midi;
	private int sysexID;

	private XGDevice()	//DefaultDevice (XG)
	{	this.config = null;
		this.midi = null;
		this.name = "XG";
		this.info1 = 1;
		this.info2 = 1;
		this.values = null;
		this.opcodes = XGOpcode.init(this);
		this.types = XGObjectType.init(this);
		this.translations = XGTranslationMap.init(this);
		this.parameters = XGParameter.init(this);
		this.template = null;
	}

	public XGDevice(XMLNode cfg) throws InvalidXGAdressException, MidiUnavailableException, TimeoutException
	{	if(cfg == null)
		{	this.config = new XMLNode(TAG_DEVICE, null);
			this.midi = new XGMidi(this);
			this.setWindow(new XGWindow(this, XGWindow.getRootWindow(), true, this, "new device"));
		}
		else
		{	this.config = cfg;
			this.sysexID = this.config.parseChildNodeIntegerContentOrNew(TAG_SYSEXID, DEF_SYSEXID);
			this.midi = new XGMidi(this);
		}
		this.name = this.detectDevice();
		if(this.name == null)
		{	throw new MidiUnavailableException("device (ID " + this.sysexID + ") not responding for " + this.midi.getInputName());
		}
		this.defDumpPath = Paths.get(this.config.getChildNodeOrNew(TAG_DEFAULTDUMPFOLDER).getTextContent());
		this.info1 = requestInfo1();
		this.info2 = requestInfo2();
		this.values = new XGValueStore(this);
		this.opcodes = XGOpcode.init(this);
		this.types = XGObjectType.init(this);
		this.translations = XGTranslationMap.init(this);
		this.parameters = XGParameter.init(this);

		XMLNode temp = null;
		try
		{	temp = XMLNode.parse(this.getResourceFile(XML_TEMPLATE));
		}
		catch(FileNotFoundException e)
		{	log.info(e.getMessage());
		}
		this.template = temp;
	}

	public File getResourceFile(String fName) throws FileNotFoundException
	{	Path extPath = this.getResourcePath();
		File extFile = extPath.resolve(fName).toFile();
		File intFile = RSCPATH.resolve(this.getName()).resolve(fName).toFile();
		if(!extFile.canRead() && intFile.canRead())
		{	try
			{	Files.createDirectories(extPath);
				Files.copy(intFile.toPath(), extFile.toPath());
				log.info("can't read file: " + extFile + "; default was copied from: " + intFile);
			}
			catch(IOException e)
			{	log.info(e.getMessage() + ", using default");
				return intFile;
			}
		}
		if(!extFile.canRead() && !intFile.canRead()) throw new FileNotFoundException("can't read files: " + extFile + " nor: " + intFile);
		return extFile;
	}

	public Path getResourcePath()
	{	return HOMEPATH.resolve(this.getName());
	}

	public XGSysexFile getLastDumpFile()
	{	return this.files.peek();
	}

	public XGValueStore getValues()
	{	return values;
	}

	public XGTagdressableSet<XGOpcode> getOpcodes()
	{	if(this.opcodes == null) return XGDevice.getDefaultDevice().getOpcodes();
		else return this.opcodes;
	}

	public XGTagableSet<XGObjectType> getTypes()
	{	if(this.types == null) return XGDevice.getDefaultDevice().getTypes();
		else return this.types;
	}

	public XGObjectType getType(XGAdress adr)
	{	for(XGObjectType t : this.getTypes()) if(t.include(adr)) return t;
		return new XGObjectType(this, adr);
	}

	public XGTagableSet<XGTranslationMap> getTranslations()
	{	return translations;
	}

	public XGTagableSet<XGParameter> getParameters()
	{	if(this.parameters == null) return XGDevice.getDefaultDevice().getParameters();
	else return this.parameters;
	}

	public Path getDefDumpPath()
	{	if(this.defDumpPath == null) return JXG.HOMEPATH;
		return this.defDumpPath;
	}

	public void setDefDumpPath(Path defDumpPath)
	{	this.defDumpPath = defDumpPath;
		this.config.getChildNodeOrNew(TAG_DEFAULTDUMPFOLDER).setTextContent(defDumpPath.toString());
	}

	public int getSysexID()
	{	return this.sysexID;
	}

	void setSysexID(int id)
	{	this.sysexID = id & 0xF;
		this.config.getChildNodeOrNew(TAG_SYSEXID).setTextContent(id);
	}

	public void nodeSelected()
	{	new XGWindow(this, XGWindow.getRootWindow(), true, this, this.name);
	}

	public void selectNode()
	{	System.out.println(this + " select");
	}

	public void unselectNode()
	{	System.out.println(this + " unselected");
	}

	public String detectDevice() throws InvalidXGAdressException, MidiUnavailableException, TimeoutException	//SystemInfo ignoriert parameterrequest?!;
	{	XGRequest m = new XGMessageDumpRequest(this.midi, XGAdressConstants.XGMODELNAMEADRESS);
		m.setDestination(this.midi);
		XGResponse r = m.request();
		XGValue v = r.getValues().get(XGAdressConstants.XGMODELNAMEADRESS);
		if(v == null) return null;
		else return v.toString().strip();
	}

	public int requestInfo1()
	{	return 0;
	}

	public int requestInfo2()
	{	return 0;
	}

	public String getName()
	{	return this.name;
	}

	public XGMidi getMidi()
	{	return this.midi;
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
	{	return(this.getName() + " (" + this.sysexID + ")");
	}

	public XMLNode getConfig()
	{	return this.config;
	}

	public int getChildCount()
	{	return this.getTypes().size();
	}

	public TreeNode getParent()
	{	return JXG.getJXG();
	}

	public boolean getAllowsChildren()
	{	return true;
	}

	public Enumeration<? extends TreeNode> children()
	{	return this.getTypes().enumeration();
	}

	public XGWindow getWindow()
	{	return null;
	}

	public XMLNode getTemplate()
	{	return null;
	}

	public void setWindow(XGWindow win)
	{	this.window = win;
	}

	public JComponent getGuiComponents()
	{	JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(getDefaultBorder("device"));

		root.add(this.midi.getGuiComponents());

		JSpinner sp = new JSpinner();
		sp.setAlignmentX(0.5f);
		sp.setAlignmentY(0.5f);
		sp.setBorder(getDefaultBorder("sysex ID"));
		sp.setModel(new SpinnerNumberModel(this.getSysexID(), 0, 15, 1));
		sp.addChangeListener(new ChangeListener()
		{	public void stateChanged(ChangeEvent e)
			{	JSpinner s = (JSpinner)e.getSource();
				setSysexID((int)s.getModel().getValue());
			}
		});
		root.add(sp);

		JButton btn = new JButton(this.getDefDumpPath().toString());
		btn.setAlignmentX(0.5f);
		btn.addActionListener(new ActionListener()
		{	public void actionPerformed(ActionEvent e)
			{	XGSysexFile f = new XGSysexFile(null, getDefDumpPath().toString());
				Path p = f.selectPath(f.toString());
				setDefDumpPath(p);
				btn.setText(p.toString());
				btn.getTopLevelAncestor().revalidate();
			}
		});
		root.add(btn);

		return root;
	}
}