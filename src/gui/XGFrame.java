package gui;

import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import module.XGModule;
import xml.XMLNode;

public class XGFrame extends JPanel implements XGComponent
{	/**
	 * 
	 */
	private static final long serialVersionUID=-2090844398565572567L;

/********************************************************************************************************************/

//	private int gridx, gridy, gridwidth, gridheight, fill, anchor, ipadx, ipady;
//	private double weightx, weighty;
//	private Insets insets;
	private final XMLNode config;

	public XGFrame(String text)
	{	super(new GridBagLayout());
		this.config = new XMLNode(text, null);
		this.setName(text);
		this.borderize();
	}

	public XGFrame(XMLNode n, XGModule mod)
	{	super(new GridBagLayout());
		this.config = n;
		this.setName(n.getStringAttribute(ATTR_NAME, mod.getName()));
//		this.borderize();
	}

	@Override public JComponent getJComponent()
	{	return this;
	}

	@Override public XMLNode getConfig()
	{	return this.config;
	}
}
