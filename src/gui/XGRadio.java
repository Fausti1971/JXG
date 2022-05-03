package gui;

import java.awt.*;
import javax.swing.*;
import parm.XGParameter;
import parm.XGParameterChangeListener;
import table.XGTable;
import table.XGTableEntry;
import value.XGValue;
import value.XGValueChangeListener;

public class XGRadio extends XGFrame implements XGValueChangeListener, XGParameterChangeListener
{
	private static final long serialVersionUID = 1L;

/*********************************************************************************************************/

	private final XGValue value;
	private final XGOrientation orientation;

	public XGRadio(XGValue val)
	{	this(val, XGOrientation.vertical);
	}

	public XGRadio(XGValue val, XGOrientation orientation)
	{	super("");
		this.orientation = orientation;
		this.value = val;
		if(val == null)
		{	this.setVisible(false);
			this.setEnabled(false);
			return;
		}
		this.setAlignmentY(XGFrame.TOP_ALIGNMENT);
		this.setAlignmentX(XGFrame.LEFT_ALIGNMENT);
		this.value.getValueListeners().add(this);
		this.parameterChanged(this.value.getParameter());
	}

	@Override public void parameterChanged(XGParameter p)
	{	if(p != null)
		{	this.setEnabled(true);
			this.setName(p.getShortName());
			this.setToolTipText(p.getName());
			XGTable t = this.value.getParameter().getTranslationTable();
			int x = 0, y = 0;
			switch(this.orientation)
			{	case horizontal:
					for(XGTableEntry e : t) this.add(new XGRadioButton(this.value, e), new int[]{x++,0,1,2}); break;
				case vertical:
					for(XGTableEntry e : t)
					{	this.add(new XGRadioButton(this.value, e), new int[]{0,y,1,2});
						y+=2;
					}
					break;
			}
		}
		else
		{	this.setEnabled(false);
			this.setName("n/a");
		}
	}

	@Override public void contentChanged(XGValue v){	this.repaint();}

/****************************************************************************************************/

	private class XGRadioButton extends JRadioButton
	{
		private static final long serialVersionUID = 1L;

/******************************************************************/

		private final XGTableEntry entry;
		private final XGValue value;

		XGRadioButton(XGValue v, XGTableEntry e)
		{	super(e.getName());
			this.setRolloverEnabled(true);
			this.setOpaque(false);
			this.setToolTipText(e.toString());
			this.entry = e;
			this.value = v;
			this.addActionListener((ActionEvent)->this.value.setEntry(entry, false, true));
		}

		@Override public boolean isSelected(){	return this.entry.getValue() == this.value.getValue();}

		@Override public void paint(Graphics g)
		{	this.setSelected(this.isSelected());
			super.paint(g);
		}

		@Override public String toString(){	return this.entry.toString();}
	}
}
