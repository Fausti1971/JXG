package device;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import adress.InvalidXGAddressException;
import application.Configurable;
import application.XGLoggable;
import gui.XGList;
import gui.XGSpinner;
import msg.XGMessage;
import msg.XGMessenger;
import msg.XGMessengerException;
import msg.XGRequest;
import msg.XGResponse;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification;
import value.ChangeableContent;
import xml.XMLNode;

public class XGMidi implements XGMidiConstants, XGLoggable, XGMessenger, CoreMidiNotification, Configurable, Receiver, AutoCloseable
{	private static Set<Info> INPUTS = new LinkedHashSet<>();
	private static Set<Info> OUTPUTS = new LinkedHashSet<>();

	static
	{	synchronized(INPUTS)
		{	initInputs();
		}
		synchronized(OUTPUTS)
		{	initOutputs();
		}
	}

	private static void initInputs()
	{	INPUTS.clear();
		MidiDevice.Info[] infos = CoreMidiDeviceProvider.getMidiDeviceInfo();
		MidiDevice tmpDev = null;
		for (MidiDevice.Info i : infos)	// i == i.getName() == dev.getDeviceInfo()
		{	try
			{	tmpDev = MidiSystem.getMidiDevice(i);
				if(tmpDev.getMaxTransmitters() == 0) continue;
				INPUTS.add(i);
			}
			catch (MidiUnavailableException e)
			{	LOG.info(e.getMessage());
			}
		}
	}
	
	private static void initOutputs()
	{	OUTPUTS.clear();
		MidiDevice.Info[] infos = CoreMidiDeviceProvider.getMidiDeviceInfo();
		MidiDevice tmpDev = null;
		for (MidiDevice.Info i : infos)
		{	try
			{	tmpDev = MidiSystem.getMidiDevice(i);
				if(tmpDev.getMaxReceivers() == 0) continue;
				OUTPUTS.add(i);
			}
			catch (MidiUnavailableException e)
			{	LOG.info(e.getMessage());
			}
		}
	}

/******************************************************************************************************************/

	public final ChangeableContent<Info> input = new ChangeableContent<Info>()
		{	@Override public Info getContent()
			{	if(getInput() != null) return getInput().getDeviceInfo();
				else return null;
			}
			@Override public boolean setContent(Info s)
			{	setInput(s);
				return true;
			}
		};
	public final ChangeableContent<Info> output = new ChangeableContent<Info>()
		{	@Override public Info getContent()
			{	if(getOutput() != null) return getOutput().getDeviceInfo();
				else return null;
			}
			@Override public boolean setContent(Info s)
			{	setOutput(s);
				return true;
			}
		};
	public final ChangeableContent<Integer> timeout = new ChangeableContent<Integer>()
		{	@Override public Integer getContent()
			{	return timeoutValue;
			}
			@Override public boolean setContent(Integer s)
			{	int old = getContent();
				setTimeout(s);
				return old != getContent();
			}
		};

	private final XGDevice device;
	private final XMLNode config;
	private Receiver transmitter;
	private MidiDevice midiOutput = null;
	private MidiDevice midiInput = null;
	private XGRequest request = null;
	private int timeoutValue;
//	private final XGMessageBuffer buffer;

	public XGMidi(XGDevice dev)
	{	this.device = dev;
		this.config = this.device.getConfig().getChildNodeOrNew(TAG_MIDI);
		this.setInput(this.config.getStringAttribute(ATTR_MIDIINPUT));
		this.setOutput(this.config.getStringAttribute(ATTR_MIDIOUTPUT));
		this.timeout.setContent(this.config.getIntegerAttribute(ATTR_MIDITIMEOUT, DEF_TIMEOUT));
//		this.buffer = new XGMessageBuffer(this);

		try
		{	CoreMidiDeviceProvider.addNotificationListener(this);
		}
		catch(CoreMidiException e)
		{	LOG.warning(e.getMessage());
		}
	}

	private void setOutput(String s)
	{	for(Info i : OUTPUTS) if(i.getName().equals(s)) this.setOutput(i);
	}

	private void setOutput(Info i)
	{	try
		{	this.setOutput(MidiSystem.getMidiDevice(i));
		}
		catch(MidiUnavailableException e)
		{	LOG.warning(e.getMessage());
		}
	}

	private void setOutput(MidiDevice dev)
	{	if(this.transmitter != null) this.transmitter.close();
		if(this.midiOutput != null && this.midiOutput.isOpen()) this.midiOutput.close();
		this.midiOutput = dev;
		if(dev != null)
		{	try
			{	dev.open();
				this.transmitter = dev.getReceiver();
			}
			catch(MidiUnavailableException e)
			{	e.printStackTrace();
				try
				{	this.transmitter = MidiSystem.getReceiver();
				}
				catch(MidiUnavailableException e1)
				{	LOG.warning(e1.getMessage());
				}
			}
		}
		LOG.info(this.getOutputName());
		this.config.setStringAttribute(ATTR_MIDIOUTPUT, this.getOutputName());
		return;
	}

	private void setInput(String s)
	{	for(Info i : INPUTS) if(i.getName().equals(s)) this.setInput(i);
	}

	private void setInput(Info i)
	{	try
		{	this.setInput(MidiSystem.getMidiDevice(i));
		}
		catch(MidiUnavailableException e)
		{	LOG.warning(e.getMessage());
		}
	}

	private void setInput(MidiDevice dev)
	{	if(this.midiInput != null && this.midiInput.isOpen()) this.midiInput.close();
		this.midiInput = dev;
		if(dev != null)
		{	try
			{	dev.getTransmitter().setReceiver(this);
				dev.open();
			}
			catch(MidiUnavailableException e)
			{	e.printStackTrace();
				try
				{	MidiSystem.getTransmitter().setReceiver(this);
				}
				catch(MidiUnavailableException e1)
				{	LOG.warning(e1.getMessage());
				}
			}
		}
		LOG.info(this.getInputName());
		this.config.setStringAttribute(ATTR_MIDIINPUT, this.getInputName());
		return;
	}

	private MidiDevice getInput()
	{	return this.midiInput;
	}

	private MidiDevice getOutput()
	{	return this.midiOutput;
	}

	private String getInputName()
	{	if(getInput() == null) return "no input device";
		else return this.midiInput.getDeviceInfo().getName();
	}

	private String getOutputName()
	{	if(getOutput() == null) return "no output device";
		else return this.midiOutput.getDeviceInfo().getName();
	}

	@Override public void close()
	{//	if(this.queue.isAlive())this.queue.interrupt();
		if(this.midiInput != null && this.midiInput.isOpen()) this.midiInput.close();
		LOG.info("MidiInput closed: " + this.getInputName());
		if(this.midiOutput != null && this.midiOutput.isOpen()) this.midiOutput.close();
		LOG.info("MidiOutput closed: " + this.getOutputName());
	}

	@Override public void midiSystemUpdated() throws CoreMidiException
	{	initInputs();
		initOutputs();
		this.setInput(this.config.getStringAttribute(ATTR_MIDIINPUT).toString());
		this.setOutput(this.config.getStringAttribute(ATTR_MIDIOUTPUT).toString());
		LOG.info("CoreMidiSystem updated, " + this.midiInput.getDeviceInfo() + "=" + this.midiInput.isOpen() + ", " + this.midiOutput.getDeviceInfo() + "=" + this.midiOutput.isOpen());
	}

	public void transmit(MidiMessage mm)
	{	this.transmitter.send(mm, -1L);
	}

	@Override public void submit(XGMessage m) throws XGMessengerException
	{	if(this.transmitter == null) throw new XGMessengerException(this + ": no transmitter initialized!");
		if(m == null)throw new XGMessengerException(this + ": message was null");
		m.setTimeStamp();
		this.transmitter.send((MidiMessage)m, -1L);
	}

	@Override public void send(MidiMessage mmsg, long timeStamp)	//send-methode des receivers (this); also eigentlich meine receive-methode
	{	try
		{	XGMessage m = XGMessage.newMessage(this, this.device.getValues(), mmsg);
			if(this.request != null && this.request.setResponsed((XGResponse)m))
			{	synchronized(this.request)
				{	this.request.notify();
				}
				return;
			}
		}
		catch(InvalidMidiDataException|InvalidXGAddressException e)
		{	LOG.info(e.getMessage());
		}
	}

	@Override public void request(XGRequest msg) throws XGMessengerException
	{	this.submit(msg);
		{	try
			{	this.request = msg;
				synchronized(this.request)
				{	this.request.wait(this.timeout.getContent());
				}
			}
			catch (InterruptedException e)
			{	LOG.info(e.getMessage());
			}
		}
		this.request = null;
	}

	@Override public int hashCode()
	{	if(this.midiInput == null || this.midiOutput == null) return HASH;
		return HASH * this.midiInput.hashCode() + HASH * this.midiOutput.hashCode();
	}

	@Override public boolean equals(Object o)
	{	if(this == o) return true;
		if(!(o instanceof XGMidi)) return false;
		return this.hashCode() == o.hashCode();
	}

	@Override public XGDevice getDevice()
	{	return this.device;
	}

	public int getTimeout()
	{	return this.timeout.getContent();
	}

	public void setTimeout(int t)
	{	this.timeoutValue = t;
		this.config.setIntegerAttribute(ATTR_MIDITIMEOUT, t);
		LOG.info("timeout set to " + t);
	}

	@Override public String toString()
	{	return this.getMessengerName();
	}

	@Override public String getMessengerName()
	{	return this.getInputName();
	}

	@Override public XMLNode getConfig()
	{	return this.config;
	}

	public JComponent getConfigComponent()
	{	GridBagConstraints gbc = new GridBagConstraints();
		JPanel root = new JPanel();
		root.setLayout(new GridBagLayout());

		JComponent c = new XGList<Info>("input", INPUTS, this.input);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridy = 0;
		root.add(new JScrollPane(c), gbc);

		c = new XGList<Info>("output", OUTPUTS, this.output);
		root.add(new JScrollPane(c), gbc);

		c = new XGSpinner("timeout", this.timeout, 30, 1000, 10);
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
//		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		root.add(c, gbc);

		return root;
	}
}