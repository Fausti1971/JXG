package parm;
public interface XGOpcodeConstants
{	public enum ValueType{Number, Text, Bitmap};

	public static final int
	MP_ELRES = 0x00,
	MP_BANK_MSB = 0x01,
	MP_BANK_LSB = 0x02,
	MP_PRG = 0x03,
	MP_CH = 0x04,
	MP_TUNE = 0x09,
	MP_PARTMODE = 0x7,
	MP_POLYMODE = 0x5,
	MP_KEYON = 0x6,
	MP_TRANSPOSE = 0x8,
	MP_VOL= 0xB,
	MP_VELSENSEDEPTH = 0xC,
	MP_VELSENSEOFFSET = 0xD,
	MP_VELLIMITLO = 0x6D,
	MP_VELLIMITHI = 0x6E,
	MP_PAN = 0xE,
	MP_NOTELIMITLO = 0xF,
	MP_NOTELIMITHI = 0x10,
	MP_DRYLVL = 0x11,
	MP_CHO = 0x12,
	MP_REV = 0x13,
	MP_VAR = 0x14,
	MP_VIBRATE = 0x15,
	MP_VIBDEPTH = 0x16,
	MP_VIBDELAY = 0x17,
	MP_CUTOFF = 0x18,
	MP_RESONANCE = 0x19,
	MP_ATACK = 0x1A,
	MP_DECAY = 0x1B,
	MP_RELEASE = 0x1C,
	MP_MW_PITCH = 0x1D,
	MP_MW_FILTER = 0x1E,
	MP_MW_AMPL = 0x1F,
	MP_MW_LFOPITCHDEPTH = 0x20,
	MP_MW_LFOFILTERDEPTH = 0x21,
	MP_MW_LFOAMPLDEPTH = 0x22,
	MP_PB_PITCH = 0x23,
	MP_PB_FILTER = 0x24,
	MP_PB_AMPL = 0x25,
	MP_PB_LFOPITCHDEPTH = 0x26,
	MP_PB_LFOFILTERDEPTH = 0x27,
	MP_PB_LFOAMPLDEPTH = 0x28,
	AD_PRG = 0x00,

		DR_COARSE = 0x00,
		DR_FINE = 0x01,
		DR_VOL = 0x02,
		DR_GRP = 0x03,
		DR_PAN = 0x04,
		DR_REV = 0x05,
		DR_CHO = 0x06,
		DR_VAR = 0x07,
		DR_ASSIGN = 0x08,
		DR_RCVOFF = 0x09,
		DR_RCVON = 0x0A,
		DR_CUTPOFF = 0x0B,
		DR_RESO = 0x0C,
		DR_ATTACK = 0x0D,
		DR_DECAY = 0x0E,
		DR_RELEASE = 0x0F;
	/*


	0x4D	catPitch
	0x4E	catFilter
	0x4F	catAmpl
	0x50	catLfoPitchDepth
	0x51	catLfoFilterDepth
	0x52	catLfoAmplDepth
	0x53	patPitch
	0x54	patFilter
	0x55	patAmpl
	0x56	patLfoPitchDepth
	0x57	patLfoFilterDepth
	0x58	patLfoAmplDepth
	0x59	ac1Pitch
	0x5A	ac1Filter
	0x5B	ac1Ampl
	0x5C	ac1AmplCtrl
	0x5D	ac1LfoPitchDepth
	0x5E	ac1LfoFilterDepth
	0x5F	ac1LfoAmplDepth
	0x60	ac2Pitch
	0x61	ac2Filter
	0x62	ac2Ampl
	0x63	ac2AmplCtrl
	0x64	ac2LfoPitchDepth
	0x65	ac2LfoFilterDepth
	0x66	ac2LfoAmplDepth
	0x30	rcvPB
	0x31	rcvCAT
	0x32	rcvPrg
	0x33	rcvCC
	0x34	rcvPAT
	0x35	rcvNote
	0x36	rcvRPN
	0x37	rcvNRPN
	0x38	rcvMW
	0x39	rcvVol
	0x3A	rcvPan
	0x3B	rcvExp
	0x3C	rcvHold1
	0x3D	rcvPortamento
	0x3E	rcvSostenuto
	0x3F	rcvSoftPedal
	0x40	rcvBank
	0x41	scaleC
	0x42	scaleC#
	0x43	scaleD
	0x44	scaleD#
	0x45	scaleE
	0x46	scaleF
	0x47	scaleF#
	0x48	scaleG
	0x49	scaleG#
	0x4A	scaleA
	0x4B	scaleA#
	0x4C	scaleB
	0x67	portam
	0x68	portamTime
	0x69	pegInitLvl
	0x6A	pegatckTime
	0x6B	pegRelLvl
	0x6C	pegRelTime
	*/

}
