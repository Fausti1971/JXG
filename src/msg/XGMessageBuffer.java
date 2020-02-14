package msg;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import adress.XGAddressableSet;
import device.TimeoutException;
import device.XGDevice;
import gui.XGWindow;
import gui.XGWindowSource;

public class XGMessageBuffer extends XGAddressableSet<XGMessage> implements XGMessenger, XGWindowSource
{	private final XGMessenger source;
	private XGWindow window;
	private final JList<XGMessage> list = new JList<>(new DefaultListModel<XGMessage>());
	private JLabel status = new JLabel();

	public XGMessageBuffer(XGMessenger src)
	{	this.source = src;
	}

	@Override public XGDevice getDevice()
	{	return this.source.getDevice();
	}

	@Override public String getMessengerName()
	{	return this.source.getMessengerName() + " buffer";
	}

	@Override public void transmit(XGMessage m) throws MidiUnavailableException
	{	this.add(m);
		((DefaultListModel<XGMessage>)this.list.getModel()).addElement(m);
		if(this.window == null) this.setChildWindow(new XGWindow(this, XGWindow.getRootWindow(), false, this.getMessengerName()));
		this.status.setText(this.size() + " messages buffered");

		if(this.window.isVisible())
		{	this.window.setPreferredSize(new Dimension(400, (int)this.list.getSize().getHeight()));
			this.window.pack();
		}
		else
		{	this.window.pack();
			this.window.setVisible(true);
		}
	}

	@Override public XGResponse request(XGRequest msg) throws TimeoutException, MidiUnavailableException
	{	return null;
	}

	private void rm(XGMessage m)
	{	super.remove(m);
		((DefaultListModel<XGMessage>)this.list.getModel()).removeElement(m);
		this.status.setText(this.size() + " messages buffered");
		this.window.setPreferredSize(new Dimension(400, (int)this.list.getSize().getHeight()));
		this.window.pack();
		if(this.size() == 0) this.window.dispose();
	}

	@Override public void windowOpened(WindowEvent e)
	{
	}

	@Override public void windowClosing(WindowEvent e)
	{	((DefaultListModel<XGMessage>)this.list.getModel()).clear();
		this.clear();
	}

	@Override public void windowClosed(WindowEvent e)
	{	this.window = null;
	}

	@Override public void windowIconified(WindowEvent e)
	{
	}

	@Override public void windowDeiconified(WindowEvent e)
	{
	}

	@Override public void windowActivated(WindowEvent e)
	{
	}

	@Override public void windowDeactivated(WindowEvent e)
	{
	}

	@Override public XGWindow getChildWindow()
	{	return this.window;
	}

	@Override public void setChildWindow(XGWindow win)
	{	this.window = win;
	}

	@Override public JComponent getChildWindowContent()
	{	JComponent root = new JPanel();
		root.setLayout(new BorderLayout());
		root.add(new JScrollPane(this.list), BorderLayout.CENTER);

		JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
		tb.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		tb.setFloatable(false);

		JButton b = new JButton("delete selected");
		b.addActionListener(new AbstractAction()
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e)
			{	for(XGMessage m : list.getSelectedValuesList()) rm(m);
			}
		});
		tb.add(b);

		b = new JButton("selected to " + this.source.getDevice().getValues().getMessengerName());
		b.addActionListener(new AbstractAction()
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e)
			{	for(XGMessage m : list.getSelectedValuesList())
				{	m.setDestination(source.getDevice().getValues());
					try
					{	source.getDevice().getValues().transmit(m);
						rm(m);
					}
					catch(MidiUnavailableException e1)
					{	e1.printStackTrace();
					}
				}
			}
		});
		tb.add(b);
		tb.validate();

		root.add(tb, BorderLayout.NORTH);

		this.status.setText(this.size() + " messages buffered");
		this.status.setHorizontalAlignment(SwingConstants.CENTER);
		this.status.setVerticalAlignment(SwingConstants.CENTER);
		root.add(this.status, BorderLayout.SOUTH);

		return root;
	}
}