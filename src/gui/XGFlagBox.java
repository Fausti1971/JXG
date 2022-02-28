package gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.*;
import value.XGValue;

public class XGFlagBox extends JPanel implements XGComponent
{
	private static final long serialVersionUID = 1L;

/*****************************************************************************************************************/

	private final JButton button = new JButton();
	private final Set<XGValue> values = new LinkedHashSet<>();

	public XGFlagBox(String name, XGValue... vals)
	{
		this.setLayout(new GridBagLayout());
		if(vals == null)
		{	this.setEnabled(false);
			this.setVisible(false);
			return;
		}
		this.setName(name);
		this.setFont(MEDIUM_FONT);
		this.borderize();

		for(XGValue v : vals)
		{	if(v != null) this.values.add(v);
		}
		this.button.addActionListener((ActionEvent)->{new XGPopup(this, this.values);});
		this.button.setFont(MEDIUM_FONT);
		this.button.setText("select...");
		this.addFocusListener(this);

		GridBagConstraints gbc = new GridBagConstraints(0, 0, 0, 0, 0.5, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
		this.add(this.button, gbc);
	}
}