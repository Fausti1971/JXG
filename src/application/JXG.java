package application;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;
import adress.InvalidXGAddressException;
import device.XGDevice;
import gui.XGContext;
import gui.XGTree;
import gui.XGTreeNode;
import gui.XGWindow;
import xml.XMLNode;

public class JXG implements Configurable, XGTreeNode, XGContext, XGLoggable
{
	static
	{	System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tl:%1$tM:%1$tS %4$s %2$s: %5$s %n");
		//	%1 = date+time (tb = mon, td = tag, tY = jahr, tl = std, tM = min, tS = sec) %2 = class+method, %3 = null, %4 = level, %5 = msg
	}

	private final static Set<String> ACTIONS = new LinkedHashSet<>();

	static final String
		ACTION_CONFIGURE = "configure...",
		ACTION_ADDNEWDEVICE = "add new device...",
		ACTION_REFRESHDEVICELIST = "reinit devices",
		ACTION_INFO = "info...";

	static
	{	JXG.ACTIONS.add(ACTION_CONFIGURE);
		JXG.ACTIONS.add(ACTION_ADDNEWDEVICE);
		JXG.ACTIONS.add(ACTION_REFRESHDEVICELIST);
		JXG.ACTIONS.add(ACTION_INFO);
	}

	private static JXG APP;

	public static JXG getApp()
	{	if(APP == null) APP = new JXG();
		return APP;
	}

	public static void main(String[] args)
	{	APP = new JXG();
//		Runtime.getRuntime().addShutdownHook
//		(	new Thread()
//			{	@Override public void run()
//				{	log.info("application exited");
//				}
//			}
//		);
		XGWindow.getRootWindow();
		new Thread(() -> {	XGDevice.init(APP.getConfig());}).start();
//		quit();
	}

/***************************************************************************************************************/

	private XGTree tree;
	private boolean isSelected = false;
	private final XMLNode config;
//	private final Path rscPath = Paths.get("rsc"), xsdPath;

	public JXG()
	{	
//		this.xsdPath = rscPath.resolve("xsd");
		CONFIGPATH.toFile().mkdirs();//TODO: der Ordner soll nur angelegt werden, wenn CONFIGPATH = HOMEPATH

		File f = CONFIGFILEPATH.toFile();
		if(f.exists()) this.config = XMLNode.parse(f);
		else this.config = new XMLNode(APPNAME);

		LOG.info("JXG config initialized");
	}

//	public Path getRscPath()
//	{	return this.rscPath;
//	}

//	public Path getXsdPath()
//	{	return this.xsdPath;
//	}

	public void quit()
	{	LOG.info("exiting application");
		try
		{	APP.getConfig().save(CONFIGFILEPATH.toFile());
			for(XGDevice d : XGDevice.getDevices()) d.exit();
		}
		catch(IOException|XMLStreamException e)
		{	e.printStackTrace();
		}
		System.exit(0);
	}

	public void addDevice()
	{	XGDevice dev;
		try
		{	dev = new XGDevice(null);
			if(XGDevice.getDevices().add(dev))
			{	this.config.addChildNode(dev.getConfig());
				dev.reloadTree();
			};
		}
		catch(InvalidXGAddressException e)
		{	LOG.warning(e.getMessage());
		}
	}

	@Override public String toString()
	{	return APPNAME;
	}

	@Override public XMLNode getConfig()
	{	return this.config;
	}

	@Override public boolean isSelected()
	{	return this.isSelected;
	}

	@Override public void setSelected(boolean s)
	{	this.isSelected = s;
	}

	@Override public Set<String> getContexts()
	{	return JXG.ACTIONS;
	}

	@Override public void actionPerformed(ActionEvent e)
	{	if(e == null || e.getActionCommand() == null) return;
		switch(e.getActionCommand())
		{	case ACTION_CONFIGURE:		JOptionPane.showMessageDialog(XGWindow.getRootWindow(), "action not implemented: " + e.getActionCommand()); break;
			case ACTION_ADDNEWDEVICE:	this.addDevice(); break;
			case ACTION_REFRESHDEVICELIST:	XGDevice.init(this.config); break;
			case ACTION_INFO:
			default:
				JOptionPane.showMessageDialog(XGWindow.getRootWindow(), "action not implemented: " + e.getActionCommand());
		}
	}

	@Override public void nodeFocussed(boolean b)
	{
	}

	@Override public String getNodeText()
	{	return APPNAME + " (" + XGDevice.getDevices().size() + ")";
	}

	@Override public TreeNode getParent()
	{	return null;
	}

	@Override public XGTree getTreeComponent()
	{	return this.tree;
	}

	@Override public void setTreeComponent(XGTree t)
	{	this.tree = t;
	}

	@Override public Collection<? extends TreeNode> getChildNodes()
	{	return XGDevice.getDevices();
	}
}