package gui;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.*;
import javax.swing.*;
import application.XGMath;
import parm.XGParameter;
import value.XGValue;
import value.XGValueChangeListener;

public class XGSlider extends XGFrame implements XGValueChangeListener, MouseListener
{	/**
	 * 
	 */
	private static final long serialVersionUID=1L;

/*****************************************************************************************************************************/

	private final XGValue value;
	private final XGSliderBar bar;
	private final XGValueLabel label;

	public XGSlider(XGValue v)
	{	super("");
		this.value = v;
		if(v == null)
		{	this.setEnabled(false);
			this.setVisible(false);
			this.setToolTipText(null);
			this.bar = null;
			this.label = null;
			return;
		}
		if(this.value.getParameter() != null)
		{	this.setEnabled(true);
			this.setVisible(true);
			this.setName(this.value.getParameter().getShortName());
		}
		this.addMouseListener(this);
		this.value.getValueListeners().add(this);

		this.bar = new XGSliderBar(this);
		this.add(this.bar, "0,0,1,1");

		this.label = new XGValueLabel(this.value);
		this.add(this.label, "0,1,1,1");
	}

//	@Override public String getName(){	return this.value.getParameter().getShortName();}

	@Override public void contentChanged(XGValue v)
	{	this.bar.repaint();
		this.label.setText(v.toString());
	}


	private class XGSliderBar extends JComponent implements MouseMotionListener, MouseWheelListener, XGComponent
	{	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

/**********************************************************************************************/

		private final XGSlider slider;
		private int barWidth;

		private XGSliderBar(XGSlider s)
		{	this.slider = s;
			this.setCursor(new Cursor(Cursor.HAND_CURSOR));
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
			this.addMouseWheelListener(this);
		}

		@Override public boolean isEnabled(){	return this.slider.isEnabled();}

		@Override protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D)g.create();
			g2.addRenderingHints(AALIAS);
			XGParameter parameter = this.slider.value.getParameter();
			Insets ins = this.getInsets();
			int w = this.getWidth() - (ins.left + ins.right);
			int h = this.getHeight() - (ins.top + ins.bottom);
	// draw background
			g2.setColor(COL_BAR_BACK);
			g2.fillRoundRect(ins.left, ins.top, w, h, ROUND_RADIUS, ROUND_RADIUS);
	// draw foreground
			int originX = XGMath.linearIO(parameter.getOriginIndex(), parameter.getMinIndex(), parameter.getMaxIndex(), ins.left, w);
			this.barWidth = XGMath.linearIO(this.slider.value.getIndex(), parameter.getMinIndex(), parameter.getMaxIndex(), ins.left, w) - originX;
			g2.setColor(COL_BAR_FORE);
			g2.fillRoundRect(Math.min(originX, originX + this.barWidth), ins.top, Math.abs(this.barWidth), h, ROUND_RADIUS, ROUND_RADIUS);
			g2.dispose();
		}

		@Override public void mouseClicked(MouseEvent e)
		{	if(e.getButton() != MouseEvent.BUTTON1) return;
			if(this.getX() + this.barWidth < e.getX()) this.slider.value.addIndex(1, true);
			else this.slider.value.addIndex(-1, true);
			e.consume();
		}
	
		@Override public void mouseWheelMoved(MouseWheelEvent e){	this.slider.value.addIndex(XGUI.getWheelRotation(e), true);}
	
		@Override public void mouseDragged(MouseEvent e)
		{	int distance = e.getX() - XGUI.ENVIRONMENT.dragEvent.getX();
			this.slider.value.addIndex(distance, true);
			XGUI.ENVIRONMENT.dragEvent = e;
			e.consume();
		}

		@Override public void mouseMoved(MouseEvent e){}

		@Override public void mouseReleased(MouseEvent e){	XGUI.ENVIRONMENT.dragEvent = e;}

		@Override public void mouseEntered(MouseEvent e){}

		@Override public void mouseExited(MouseEvent e){}
	}
}
