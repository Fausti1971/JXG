package msg;

import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import adress.XGAddress;
import adress.XGAddressable;

abstract class XGSuperMessage extends SysexMessage implements XGMessage, XGAddressable
{	protected static final Logger log = Logger.getAnonymousLogger();

/****************************************************************************************************************************************/

	private final XGMessenger source;
	private XGMessenger destination;
	private long transmissionTimeStamp;
	private byte[] data;

	protected XGSuperMessage(XGMessenger src, byte[] array)	// für manuell erzeugte
	{	this.source = src;
		this.data = array;
		setSOX();
		setSysexID(src.getDevice().getSysexID());
		setVendorID();
		setModelID();
		this.setTimeStamp(System.currentTimeMillis());
	}

	protected XGSuperMessage(XGMessenger src, SysexMessage msg) throws InvalidMidiDataException	//für Midi und File
	{	this.source = src;
		this.setTimeStamp(System.currentTimeMillis());
		this.data = msg.getMessage();
		this.validate();
	}

	@Override public byte[] getByteArray()
	{	return this.data;
	}

	@Override public XGMessenger getSource()
	{	return this.source;
	}

	@Override public XGMessenger getDestination()
	{	return this.destination;
	}

	@Override public void setDestination(XGMessenger dest)
	{	this.destination = dest;
	}
	
	@Override public long getTimeStamp()
	{	return this.transmissionTimeStamp;
	}

	@Override public void setTimeStamp(long time)
	{	this.transmissionTimeStamp = time;
	}

	@Override public void validate() throws InvalidMidiDataException
	{	if(this.getVendorID() != VENDOR || this.getModelID() != MODEL) throw new InvalidMidiDataException("no xg data");
	}

	@Override public SysexMessage asSysexMessage() throws InvalidMidiDataException
	{	return new SysexMessage(this.data, this.data.length);
	}

	@Override public XGAddress getAdress()
	{	return new XGAddress(getHi(), getMid(), getLo());
	}

	@Override public String toString()
	{	return this.getClass().getSimpleName() + "/" + this.source.getDevice().getType(this.getAdress()) + "/" + this.getAdress();
	}

	protected abstract int getHi();
	protected abstract int getMid();
	protected abstract int getLo();
	protected abstract void setHi(int hi);
	protected abstract void setMid(int mid);
	protected abstract void setLo(int lo);
	protected abstract void setMessageID();
}