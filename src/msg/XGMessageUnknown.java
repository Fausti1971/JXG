package msg;

import javax.sound.midi.InvalidMidiDataException;

public class XGMessageUnknown extends XGSuperMessage
{
//	protected XGMessageUnknown(XGMessenger src, XGMessenger dest, SysexMessage msg) throws InvalidMidiDataException
//	{	super(src, dest, msg);
//		LOG.warning("unknown message received: " + this.toHexString());
//	}

	protected XGMessageUnknown(XGMessenger src, XGMessenger dest, byte[] msg, boolean init) throws InvalidMidiDataException
	{	super(src, dest, msg, init);
		LOG.warning("unknown message received: " + this.toHexString());
	}

	@Override public int getHi()
	{	return 0;
	}

	@Override public int getMid()
	{	return 0;
	}

	@Override public int getLo()
	{	return 0;
	}

	@Override public void setHi(int hi)
	{
	}

	@Override public void setMid(int mid)
	{
	}

	@Override public void setLo(int lo)
	{
	}

	@Override public void setMessageID()
	{
	}
}
