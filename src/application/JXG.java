package application;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;
import adress.InvalidXGAdressException;
import device.TimeoutException;
import device.XGDevice;
import gui.GuiConfigurable;
import gui.XGFrame;
import gui.XGTreeNode;
import gui.XGTreeNodeComponent;
import gui.XGWindow;
import gui.XGWindowSourceTreeNode;
import xml.XMLNode;

public class JXG implements GuiConfigurable, XGWindowSourceTreeNode
{	static
	{	System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tl:%1$tM:%1$tS %4$s %2$s: %5$s %n");
		//	%1 = date+time (tb = mon, td = tag, tY = jahr, tl = std, tM = min, tS = sec) %2 = class+method, %3 = null, %4 = level, %5 = msg
	}

	private static final Logger log = Logger.getAnonymousLogger();
	public static final XMLNode config = initConfig(); 
	private final static JXG jxg = new JXG();

	private static XMLNode  initConfig()
	{	XMLNode x = new XMLNode(APPNAME, null);
		HOMEPATH.toFile().mkdirs();
		File f = CONFIGFILEPATH.toFile();
		if(f.exists()) x = XMLNode.parse(f);
		return x;
	}

	public static JXG getJXG()
	{	return jxg;
	}

	public static void main(String[] args)
	{	
//		Runtime.getRuntime().addShutdownHook
//		(	new Thread()
//			{	@Override public void run()
//				{	log.info("application exited");
//				}
//			}
//		);
		XGWindow.getRootWindow().setVisible(true);
		XGDevice.init();
//		quit();
	}

	public static void quit()
	{	log.info("exiting application");
		try
		{	jxg.getConfig().save(CONFIGFILEPATH.toFile());
		}
		catch(IOException|XMLStreamException e)
		{	e.printStackTrace();
		}
		System.exit(0);
	}

/***************************************************************************************************************/

	private XGTreeNodeComponent nodeComponent;
	private XGWindow window;

	private JXG()
	{	//System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tl:%1$tM:%1$tS %4$s %2$s: %5$s %n");
		this.nodeComponent = new XGTreeNodeComponent(APPNAME);
		log.info("JXG config initialized");
	}

	public TreeNode getParent()
	{	return null;
	}

	public boolean getAllowsChildren()
	{	return true;
	}

	public Enumeration<? extends TreeNode> children()
	{	return Collections.enumeration(XGDevice.getDevices());
	}

	public XGWindow getWindow()
	{	return this.window;
	}

	public void setWindow(XGWindow win)
	{	this.window = win;
	}

	public XMLNode getTemplate()
	{	return null;
	}

	@Override public String toString()
	{	return APPNAME;
	}

	public void nodeClicked()
	{	new XGWindow(this, XGWindow.getRootWindow(), true, "settings");
	}

	public XGTreeNodeComponent getGuiComponent()
	{	return this.nodeComponent;
	}

	public XMLNode getConfig()
	{	return JXG.config;
	}

	public Component getWindowContent()
	{	return this.getConfigurationGuiComponents();
	}

	public Component getConfigurationGuiComponents()
	{	XGFrame root = new XGFrame("settigs");
//		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

		JButton btn = new JButton("add device...");
		btn.addActionListener(new AbstractAction()
		{	private static final long serialVersionUID=2717877286233170533L;
			public void actionPerformed(ActionEvent e)
			{	try
				{	XGDevice dev = new XGDevice(null);
					if(dev != null)
					{	if(XGDevice.getDevices().add(dev))
						{	config.addChild(dev.getConfig());
							((XGTreeNode)getGuiComponent()).reloadTree();
						}
					}
				}
				catch(InvalidXGAdressException|MidiUnavailableException | TimeoutException e1)
				{	log.info(e1.getMessage());
				}
			}
		});
		root.add(btn);

		btn = new JButton("refresh");
		btn.addActionListener(new AbstractAction()
		{	private static final long serialVersionUID=-7638850235957790794L;

			public void actionPerformed(ActionEvent e)
			{	XGDevice.init();
			}
		});
		root.add(btn);

		return root;
	}


	public JTree getTree()
	{	return (JTree)XGWindow.getRootWindow().getRootComponent();
	}
}