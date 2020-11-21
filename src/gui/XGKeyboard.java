package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import adress.InvalidXGAddressException;
import adress.XGAddress;
import device.XGDevice;
import device.XGMidi;
import module.XGModule;
import value.XGValue;
import xml.XMLNode;

public class XGKeyboard extends XGFrame implements XGUI
{

	private static final long serialVersionUID = 1L;
	private static final XGAddress
		PARTMODEADDRESS = new XGAddress("8//7"),
		MIDICHANNELADDRESS = new XGAddress("8//4"),
		MINKEYADDRESS = new XGAddress("8//15"),
		MAXKEYADDRESS = new XGAddress("8//16");
	private static final int
		WHITEWIDTH = 20,
		BLACKWIDTH = WHITEWIDTH / 2,
		DEF_VELOCITY = 100;

	private static final boolean isBlackNote(int i)
	{	int rest = i % 12;
		return rest == 1 || rest == 3 || rest == 6 || rest == 8 || rest == 10;
	}


/******************************************************************************************************************************************/

	private final XGValue partmodeValue, minKeyValue, maxKeyValue, midiChannelValue;
	private final XGDevice device;
	private final Map<Integer, XGKey> keyMap = new HashMap<>();
	private final JPanel panel = new JPanel(null);
	private final JScrollPane scrollpane = new JScrollPane(this.panel);
	private final ShortMessage message = new ShortMessage();
//	private final JComponent column;

	protected XGKeyboard(XMLNode n, XGModule mod) throws InvalidXGAddressException
	{	super(n);
		this.device = mod.getType().getDevice();
		this.partmodeValue = this.device.getValues().get(PARTMODEADDRESS.complement(mod.getAddress()));
		this.midiChannelValue = this.device.getValues().get(MIDICHANNELADDRESS.complement(mod.getAddress()));
		this.minKeyValue = this.device.getValues().get(MINKEYADDRESS.complement(mod.getAddress()));
		this.maxKeyValue = this.device.getValues().get(MAXKEYADDRESS.complement(mod.getAddress()));

		this.partmodeValue.addValueListener((XGValue)->{this.partmodeChanged();});
		this.minKeyValue.addValueListener((XGValue)->{this.minKeyChanged();});
		this.maxKeyValue.addValueListener((XGValue)->{this.maxKeyChanged();});

		int h = this.getContentArea().height;
		for(int i = 0, p = 0; i < 128; i++)
		{	XGKey k = new XGKey(this, i);
			this.panel.add(k);
			if(k.isBlack())
			{	k.setBounds(p - BLACKWIDTH / 2, 0, BLACKWIDTH, (int)(h * 0.6));
			}
			else
			{	k.setBounds(p, 0, WHITEWIDTH, h);
				p += WHITEWIDTH;
			}
			this.keyMap.put(k.number, k);
			this.panel.setSize(p, h);
		}
		for(Component c : this.panel.getComponents())
		{	if(((XGKey)c).isBlack()) this.panel.setComponentZOrder(c, this.panel.getComponentZOrder(c) - 1);//bringe schwarze Tasten hierarchisch über die folgende Weiße
		}
		
		this.panel.setPreferredSize(this.panel.getSize());

//		this.column = new JLabel("Test");

		this.add(this.scrollpane);
		this.scrollpane.setBorder(null);
		this.scrollpane.setViewportBorder(null);
		this.scrollpane.setBounds(this.getContentArea());
//		this.scrollpane.setColumnHeaderView(this.column);
		this.scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//		this.scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	}

	private boolean sendMessage(int status, XGKey num)
	{	try
		{	this.message.setMessage(status | this.midiChannelValue.getValue(), num.number, DEF_VELOCITY);
			((XGMidi)this.device.getMidi()).transmit(this.message);
			return true;
		}
		catch(InvalidMidiDataException e)
		{	LOG.info(e.getMessage());
			return false;
		}
	}

	private XGKey getKey(int num)
	{	return this.keyMap.get(num);
	}

	private String getKeyText(XGKey key)
	{	return this.device.getTables().get("tones").getByIndex(key.getNumber()).getName();
	}

	private void maxKeyChanged()
	{	this.repaint();
	}

	private void minKeyChanged()
	{	this.repaint();
	}

	private void partmodeChanged()
	{	//TODO: show oder hide note- or drumnames, dependently by partmodeValue;
	}

/************************************************************************************************************************************************/

	private class XGKey extends JLabel implements MouseListener
	{
		private static final long serialVersionUID = 1L;

	/************************************************************************/

		private final int number;
		private final XGKeyboard keyboard;
		private final Color normalColor, pressedColor;
		private final boolean isBlack;

		public XGKey(XGKeyboard brd, int num)
		{	this.keyboard = brd;
			this.number = num;
			this.isBlack = XGKeyboard.isBlackNote(this.number);
			if(this.isBlack)
			{	this.normalColor = Color.black;
				this.pressedColor = COL_BAR_FORE;
				this.setForeground(Color.white);
			}
			else
			{	this.normalColor = Color.white;
				this.pressedColor = COL_BAR_FORE;
				this.setForeground(Color.black);
				this.setBorder(new LineBorder(Color.lightGray, 1));
			}
			this.setBackground(this.normalColor);
			this.setOpaque(true);
			this.setVerticalAlignment(SwingConstants.TOP);
			this.setHorizontalAlignment(SwingConstants.CENTER);
			this.setFont(SMALL_FONT);
			this.setName(brd.getKeyText(this));
			this.setText(this.getHTML("" + (this.getOctave() - 2)));
			this.setToolTipText(brd.getKeyText(this));
//			int h = brd.panel.getHeight() - (brd.panel.getInsets().top + brd.panel.getInsets().bottom);
			this.setCursor(new Cursor(Cursor.HAND_CURSOR));
			this.addMouseListener(this);
		}

		private String getHTML(String text)
		{	String tmp = new String("<html>");
			for(char c : text.toCharArray())
			{	tmp += c + "<br>";
			}
			tmp += "</html>";
			return tmp;
		}

		private int getNumber()
		{	return this.number;
		}

		private int getOctave()
		{	return this.number / 12;
		}

		private boolean isBlack()
		{	return this.isBlack;
		}

		@Override public boolean isEnabled()
		{	return this.number >= this.keyboard.minKeyValue.getValue() && this.number <= this.keyboard.maxKeyValue.getValue();
		}

		@Override public void mouseClicked(MouseEvent e)
		{	if(e.getClickCount() == 2 & this.keyboard.partmodeValue.getValue() > 0) System.out.println("doublecklick should open a drumedit window " + this.keyboard.getKeyText(this));
		
			//TODO: if doubleclick && partmode > 0 open drumEditWindow
			//TODO: if rightclick ask for lowLimit or highLimit and setLimit
		}

		@Override public void mousePressed(MouseEvent e)
		{	XGUI.VARIABLES.mousePressed = true;
			if(!this.isEnabled() || e.getButton() != MouseEvent.BUTTON1) return;
			if(this.keyboard.sendMessage(ShortMessage.NOTE_ON, this))
			{	this.setBackground(this.pressedColor);
				this.keyboard.repaint();
			}
		}

		@Override public void mouseReleased(MouseEvent e)
		{	XGUI.VARIABLES.mousePressed = false;
			if(!this.isEnabled() || e.getButton() != MouseEvent.BUTTON1) return;
			if(this.keyboard.sendMessage(ShortMessage.NOTE_OFF, this))
			{	this.setBackground(this.normalColor);
				this.keyboard.repaint();
			}
		}

		@Override public void mouseEntered(MouseEvent e)
		{	if(!XGUI.VARIABLES.mousePressed);
		}
	
		@Override public void mouseExited(MouseEvent e)
		{	if(!XGUI.VARIABLES.mousePressed);
		}
	}
}
