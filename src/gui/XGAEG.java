package gui;

import adress.InvalidXGAddressException;
import adress.XGAddress;
import adress.XGMemberNotFoundException;
import gui.XGPoint.PointRelation;
import module.XGModule;
import value.XGFixedValue;
import value.XGValue;
import xml.XMLNode;

public class XGAEG extends XGComponent
{
	private static final long serialVersionUID = 1L;
	private static final XGAddress ATTACK = new XGAddress("8//26"), DECAY = new XGAddress("8//27"), RELEASE = new XGAddress("8//28");

/**************************************************************************************/

	private final XGPointPanel panel;
	private final XGValue attack, decay, release;

	public XGAEG(XMLNode n, XGModule mod) throws XGMemberNotFoundException, InvalidXGAddressException
	{	super(n, mod);
		this.borderize();
		this.setLayout(null);

		this.attack = mod.getValues().get(ATTACK.complement(mod.getAddress()));
		this.decay = mod.getValues().get(DECAY.complement(mod.getAddress()));
		this.release = mod.getValues().get(RELEASE.complement(mod.getAddress()));
		int maxX = this.attack.getParameter().getMaxIndex() - this.attack.getParameter().getMinIndex();
		maxX += this.decay.getParameter().getMaxIndex() - this.decay.getParameter().getMinIndex();
		maxX += this.release.getParameter().getMaxIndex() - this.release.getParameter().getMinIndex();

		this.panel = new XGPointPanel(this, n);
		this.panel.setLimits(0, maxX, 0, 127);
		this.panel.setUnits("Time", "Gain");

		this.panel.add(new XGPoint(0, XGFixedValue.VALUE_0, XGFixedValue.VALUE_0, PointRelation.ABSOLUTE, PointRelation.ABSOLUTE));
		this.panel.add(new XGPoint(1, this.attack, XGFixedValue.VALUE_127, PointRelation.ADD_TO_PREVIOUS_COORDINATE, PointRelation.ABSOLUTE));
		this.panel.add(new XGPoint(2, this.decay, XGFixedValue.VALUE_64, PointRelation.ADD_TO_PREVIOUS_COORDINATE, PointRelation.ABSOLUTE));
		this.panel.add(new XGPoint(3, this.release, XGFixedValue.VALUE_0, PointRelation.ADD_TO_PREVIOUS_COORDINATE, PointRelation.ABSOLUTE));

		this.add(this.panel);
	}
}
