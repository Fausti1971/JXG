package device;

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
import adress.InvalidXGAddressException;
import application.Configurable;
import application.XGLoggable;
import gui.XGComponent;
import gui.XGFrame;
import gui.XGList;
import gui.XGSpinner;
import msg.XGMessage;
import msg.XGMessageBuffer;
import msg.XGMessenger;
import msg.XGRequest;
import msg.XGResponse;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification;
import value.ChangeableContent;
import xml.XMLNode;

public class XGMidi implements XGMidiConstants, XGLoggable, XGMessenger, CoreMidiNotification, Configurable, Receiver, AutoCloseable
{
	public static Set<Info> getInputs()
	{	Set<Info> inputs = new LinkedHashSet<>();
		MidiDevice.Info[] infos = CoreMidiDeviceProvider.getMidiDeviceInfo();
		MidiDevice tmpDev = null;
		for (MidiDevice.Info i : infos)	// i == i.getName() == dev.getDeviceInfo()
		{	try
			{	tmpDev = MidiSystem.getMidiDevice(i);
				if(tmpDev.getMaxTransmitters() == 0) continue;
				inputs.add(i);
			}
			catch (MidiUnavailableException e)
			{	log.info(e.getMessage());
			}
		}
		return inputs;
	}
	
	public static Set<Info> getOutputs()
	{	Set<Info> outputs = new LinkedHashSet<>();
		MidiDevice.Info[] infos = CoreMidiDeviceProvider.getMidiDeviceInfo();
		MidiDevice tmpDev = null;
		for (MidiDevice.Info i : infos)
		{	try
			{	tmpDev = MidiSystem.getMidiDevice(i);
				if(tmpDev.getMaxReceivers() == 0) continue;
				outputs.add(i);
			}
			catch (MidiUnavailableException e)
			{	log.info(e.getMessage());
			}
		}
		return outputs;
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
	private Thread requestThread;
	private final XGMessageBuffer buffer;

	public XGMidi(XGDevice dev)
	{	this.device = dev;
		this.config = this.device.getConfig().getChildNodeOrNew(TAG_MIDI);
		this.setInput(this.config.getStringAttribute(ATTR_MIDIINPUT));
		this.setOutput(this.config.getStringAttribute(ATTR_MIDIOUTPUT));
		this.timeout.setContent(this.config.getIntegerAttribute(ATTR_MIDITIMEOUT, DEF_TIMEOUT));
		this.buffer = new XGMessageBuffer(this);

		try
		{	CoreMidiDeviceProvider.addNotificationListener(this);
		}
		catch(CoreMidiException e)
		{	e.printStackTrace();
		}
	}

	private void setOutput(String s)
	{	for(Info i : getOutputs()) if(i.getName().equals(s)) this.setOutput(i);
	}

	private void setOutput(Info i)
	{	try
		{	this.setOutput(MidiSystem.getMidiDevice(i));
		}
		catch(MidiUnavailableException e)
		{	e.printStackTrace();
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
				{	e1.printStackTrace();
				}
			}
		}
		log.info(this.getOutputName());
		this.config.setStringAttribute(ATTR_MIDIOUTPUT, this.getOutputName());
//		this.notifyConfigurationListeners();
		return;
	}

	private void setInput(String s)
	{	for(Info i : getInputs()) if(i.getName().equals(s)) this.setInput(i);
	}

	private void setInput(Info i)
	{	try
		{	this.setInput(MidiSystem.getMidiDevice(i));
		}
		catch(MidiUnavailableException e)
		{	e.printStackTrace();
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
				{	e1.printStackTrace();
				}
			}
		}
		log.info(this.getInputName());
		this.config.setStringAttribute(ATTR_MIDIINPUT, this.getInputName());
//		this.notifyConfigurationListeners();
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

	@Override public void send(MidiMessage mmsg, long timeStamp)	//send-methode des receivers (this); also eigentlich meine receive-methode
	{	synchronized(this)
		{	try
			{	XGMessage m = XGMessage.newMessage(this, this.buffer, mmsg);
				if(this.request != null && this.request.setResponsed((XGResponse)m))
				{	this.requestThread.interrupt();
//					return;
				}
				m.getDestination().submit((XGResponse)m);
			}
			catch(InvalidMidiDataException|InvalidXGAddressException e)
			{	log.info(e.getMessage());
			}
		}
	}

	@Override public void close()
	{//	if(this.queue.isAlive())this.queue.interrupt();
		if(this.midiInput != null && this.midiInput.isOpen()) this.midiInput.close();
		log.info("MidiInput closed: " + this.getInputName());
		if(this.midiOutput != null && this.midiOutput.isOpen()) this.midiOutput.close();
		log.info("MidiOutput closed: " + this.getOutputName());
	}

	@Override public void midiSystemUpdated() throws CoreMidiException
	{	this.setInput(this.config.getStringAttribute(ATTR_MIDIINPUT));
		this.setOutput(this.config.getStringAttribute(ATTR_MIDIOUTPUT));
		log.info("CoreMidiSystem updated, " + this.midiInput.getDeviceInfo() + "=" + this.midiInput.isOpen() + ", " + this.midiOutput.getDeviceInfo() + "=" + this.midiOutput.isOpen());
		//	this.notifyConfigurationListeners();
	}

	private boolean transmit(XGMessage m)
	{	if(this.transmitter == null)
		{	log.info("no transmitter initialized!");
			return false;
		}
		if(m == null)
		{	log.info("message was null");
			return false;
		}
		m.setTimeStamp();
		this.transmitter.send((MidiMessage)m, -1L);
		return true;
	}

	@Override public void submit(XGResponse msg)
	{	this.transmit(msg);
	}

	@Override public XGResponse request(XGRequest msg) throws TimeoutException
	{	synchronized(this.buffer)
		{	if(this.transmit(msg))
			{	try
				{	this.request = msg;
					this.requestThread = Thread.currentThread();
					Thread.sleep(this.timeoutValue);
					throw new TimeoutException("timeout: no response within " + (System.currentTimeMillis() - msg.getTimeStamp()) + " ms");
				}
				catch(InterruptedException e)//wird bei validiertem empfang via send() interrupted, sofern ein request im Lauf ist...
				{	log.info("response within " + (System.currentTimeMillis() - msg.getTimeStamp()) + " ms");
				}
			}
			this.request = null;
			return msg.getResponse();
		}
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
		log.info("timeout set to " + t);
	}

	@Override public String getMessengerName()
	{	return this.getInputName();
	}

	@Override public XMLNode getConfig()
	{	return this.config;
	}

	public XGComponent getConfigComponent()
	{	XGFrame root = new XGFrame("midi");

		JComponent c = new XGList<Info>("input", XGMidi.getInputs(), this.input);
		root.addGB(c, 0, 0, 4, 4);

		c = new XGList<Info>("output", XGMidi.getOutputs(), this.output);
		root.addGB(c, 1, 0, 4, 4);

		c = new XGSpinner("timeout", this.timeout, 30, 1000, 10);
		root.addGB(c, 0, 1);

		return root;
	}
}