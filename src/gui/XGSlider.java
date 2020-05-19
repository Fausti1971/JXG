package gui;

import static application.XGLoggable.log;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JComponent;
import adress.InvalidXGAddressException;
import adress.XGAddress;
import application.JXG;
import application.Rest;
import device.XGDevice;
import module.XGModule;
import msg.XGMessageParameterChange;
import parm.XGParameter;
import parm.XGParameterConstants;
import value.XGValue;
import value.XGValueChangeListener;
import xml.XMLNode;

public class XGSlider extends XGFrame implements KeyListener, XGParameterConstants, XGValueChangeListener
{	/**
	 * 
	 */
	private static final long serialVersionUID=1L;

	private final static int PREF_W = 128, PREF_H = 44;

/*****************************************************************************************************************************/

	private final XGAddress address;
	private final XGValue value;
	private final XGSliderBar bar;
	private final XGValueLabel label;

	public XGSlider(XMLNode n, XGModule mod)
	{	super(n, mod);
		this.address = new XGAddress(n.getStringAttribute(ATTR_VALUE), mod.getAddress());
		XGValue v = mod.getDevice().getValues().getFirstIncluding(this.address);
		this.value = v;
		if(this.isEnabled())
		{	this.setToolTipText(null);
			this.setFocusable(true);
		}
//		this.setName(this.value.getParameter().getShortName());
		this.setSizes(PREF_W,  PREF_H);
		this.borderize();
		v.addListener(this);
		this.addMouseListener(this);
		this.addFocusListener(this);

		this.bar = new XGSliderBar(this.value);
		this.addGB(this.bar, 0, 0, 1, 1, GridBagConstraints.BOTH, 0.5, 0.5, GridBagConstraints.NORTH, new Insets(0,0,2,0), 0, 0);

		this.label = new XGValueLabel(this.value);
		this.addGB(this.label, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0.5, 0, GridBagConstraints.SOUTH, new Insets(1,1,0,1), 0, 0);

		log.info("slider initialized: " + this.value.getParameter());
	}

	@Override public void paint(Graphics g)
	{	if(this.isEnabled()) super.paint(g);
	}

	@Override public String getName()
	{	return this.value.getParameter().getShortName();
	}

	@Override public String getToolTipText()
		{	return this.value.getParameter().getLongName();
		}

	@Override public boolean isEnabled()
	{	return super.isEnabled() && this.value != null && this.value.getParameter() != null;
	}

	@Override public void contentChanged(XGValue v)
	{	super.repaint();
	}

	@Override public void keyTyped(KeyEvent e)
	{
	}

	@Override public void keyPressed(KeyEvent e)
	{
	}

	@Override public void keyReleased(KeyEvent e)
	{
	}

	@Override public JComponent getJComponent()
	{	return this;
	}

	@Override public boolean isManagingFocus()
	{	return true;
	}
	
	@Override public boolean isFocusTraversable()
	{	return true;
	}


	private class XGSliderBar extends JComponent implements XGValueChangeListener, MouseMotionListener, MouseWheelListener, MouseListener
	{	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

/**********************************************************************************************/

		private final XGValue value;
		private XGParameter parameter;
		private int barWidth, originWidth;
		private Graphics2D g2;
//		private Cursor lastCursor;

		private XGSliderBar(XGValue v)
		{	//super();
			//this.setBorder(null);	buggy: getX() liefert IMMER inset.left; getY() IMMER inset.top; (5, 15)! Bug?; deshalb beim malen diese koordinaten ignorieren...
			this.value = v;
			this.setCursor(new Cursor(Cursor.HAND_CURSOR));
			this.value.addListener(this);
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
			this.addMouseWheelListener(this);
		}

		@Override public boolean isEnabled()
		{	return super.isEnabled() && this.value != null && this.value.getParameter() != null;
		}

		@Override protected void paintComponent(Graphics g)
		{	if(!(g instanceof Graphics2D) || !this.isEnabled()) return;
			this.g2 = (Graphics2D)g.create();
			this.g2.addRenderingHints(AALIAS);
			this.parameter = this.value.getParameter();
	// draw background
			this.g2.setColor(COL_BAR_BACK);
			this.g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), ROUND_RADIUS, ROUND_RADIUS);
	// draw foreground
			this.originWidth = Rest.linearIO(this.parameter.getOrigin(), this.parameter.getMinValue(), this.parameter.getMaxValue(), 0, this.getWidth());
			this.barWidth = Rest.linearIO(this.value.getContent(), this.parameter.getMinValue(), this.parameter.getMaxValue(), 0, this.getWidth()) - this.originWidth;
			this.g2.setColor(COL_BAR_FORE);
			this.g2.fillRoundRect(0 + Math.min(this.originWidth, this.originWidth + this.barWidth), 0, Math.abs(this.barWidth), this.getHeight(), ROUND_RADIUS, ROUND_RADIUS);
			this.g2.dispose();
		}

		@Override public void mouseClicked(MouseEvent e)
		{	XGDevice dev = this.value.getSource().getDevice();
			boolean changed = false;
			if(e.getButton() != MouseEvent.BUTTON1) return;
			if(this.getX() + this.barWidth < e.getX()) changed = this.value.addContent(1);
			else changed = this.value.addContent(-1);
			if(changed)
			{	try
				{	new XGMessageParameterChange(dev, dev.getMidi(), this.value).transmit();
				}
				catch(InvalidXGAddressException | InvalidMidiDataException e1)
				{	e1.printStackTrace();
				}
			}
			e.consume();
		}
	
		@Override public void mouseWheelMoved(MouseWheelEvent e)
		{	boolean changed = this.value.addContent(e.getWheelRotation());
			if(changed)
			{	XGDevice dev = this.value.getSource().getDevice();
				try
				{	new XGMessageParameterChange(dev, dev.getMidi(), this.value).transmit();
				}
				catch(InvalidXGAddressException | InvalidMidiDataException e1)
				{	e1.printStackTrace();
				}
			}
			e.consume();
		}
	
		@Override public void mouseDragged(MouseEvent e)
		{	int distance = e.getX() - JXG.dragEvent.getX();
			boolean changed = this.value.addContent(distance);
			if(changed)
			{	XGDevice dev = this.value.getSource().getDevice();
				try
				{	new XGMessageParameterChange(dev, dev.getMidi(), this.value).transmit();
				}
				catch(InvalidXGAddressException | InvalidMidiDataException e1)
				{	e1.printStackTrace();
				}
			}
			JXG.dragEvent = e;
			e.consume();
		}

		@Override public void mouseMoved(MouseEvent e)
		{
		}

		@Override public void contentChanged(XGValue v)
		{	this.repaint();
		}

		@Override public void mousePressed(MouseEvent e)
		{	JXG.dragEvent = e;
			e.consume();
		}
	
		@Override public void mouseReleased(MouseEvent e)
		{	JXG.dragEvent = e;
		}


		@Override public void mouseEntered(MouseEvent e)
		{
		}

		@Override public void mouseExited(MouseEvent e)
		{	
		}
	}
}
