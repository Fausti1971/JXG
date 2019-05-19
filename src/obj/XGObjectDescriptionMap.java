package obj;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public interface XGObjectDescriptionMap extends XGObjectConstants
{	static enum AdressType{NONE, ADRESS, DUMPSEQ};
	static int MIN = 0, MAX = 1;
	static Logger log = Logger.getAnonymousLogger();
	static final File FILE = new File(XML_FILE);
	static Set<XGObjectType> objectDescriptionMap = new HashSet<>();

	static Set<XGObjectType> getObjectDescriptionMap()
	{	return objectDescriptionMap;}

	public static void initObjectDescriptionMap()
	{	if(!FILE.canRead())
		{	log.info("can't read file: " + FILE);
			return;
		}

		try
		{	SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setValidating(true);
			SAXParser sp = spf.newSAXParser();
			sp.parse(FILE, new XMLHandler());
		}
		catch(ParserConfigurationException|SAXException | IOException e)
		{	e.printStackTrace();
		}
		return;
	}

	static class XMLHandler extends DefaultHandler
	{	private boolean mapTagIsOpened = false;
		private XGOD od;
		private XGDD dd;
		private XGA adr;
		private String temp;

		XMLHandler()
		{	}

		@Override public void startDocument() throws SAXException
			{	super.startDocument();
				log.info("start parsing " + FILE);
			}

		@Override public void endDocument() throws SAXException
			{	super.endDocument();
				log.info("parsing finished " + FILE);
			}

		@Override public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
		{	if(qName.equals("map"))
			{	this.mapTagIsOpened = true;
				return;
			}
			if(this.mapTagIsOpened)
			{	switch(qName)
				{	case TAG_OBJECT:	this.od = new XGOD(); return;
					case TAG_ADRESS:	this.adr = new XGA(AdressType.ADRESS); return;
					case TAG_DUMPSEQ:	this.dd = new XGDD();
										this.adr = new XGA(AdressType.DUMPSEQ);
										this.dd.name = attributes.getValue("name");
										return;
				}
			}
		}

		@Override public void endElement(String namespaceURI, String localName, String qName)
		{	if(qName.equals("map") && this.mapTagIsOpened)
			{	this.mapTagIsOpened = false;
				return;
			}
			switch(qName)
			{ 	case TAG_OBJECT:	objectDescriptionMap.add(new XGObjectType(this.od.adr, this.od.name, this.od.mapName, this.od.descs));
									break;
				case TAG_ADRESS:	this.od.adr = this.adr.getXGAdresses()[MIN];
									this.adr.type = AdressType.NONE;
									break;
				case TAG_DUMPSEQ:	this.dd.min = this.adr.getXGAdresses()[MIN];
									this.dd.max = this.adr.getXGAdresses()[MAX];
									this.od.descs.add(new XGDumpDescription(this.dd.min, this.dd.max, this.dd.name));
									this.adr.type = AdressType.NONE;
									break;
				case TAG_HI:		this.adr.hi = this.temp; break;
				case TAG_MID:		this.adr.mid = this.temp; break;
				case TAG_LO:		this.adr.lo = this.temp; break;
				case TAG_NAME:		this.od.name = this.temp; break;
				case TAG_PARAMETERMAP:	this.od.mapName = this.temp; break;
			}
		}

		@Override public void characters(char[] ch, int start, int length)
		{	if(!this.mapTagIsOpened) return;
			this.temp = String.copyValueOf(ch, start, length).strip();
		}
	};

	class XGOD
	{	private XGAdress adr;
		private String name, mapName;
		private Set<XGDumpDescription> descs = new HashSet<>();
	}

	class XGA
	{	private AdressType type;
		private String hi = null, mid = null, lo = null;

		private XGA(AdressType t)
		{	this.type = t;}

		private XGAdress[] getXGAdresses()
		{	int h, m, l;
			XGAdress[] array = new XGAdress[2];
			switch(this.type)
			{
				default:
				case NONE:	return null;
				case ADRESS:
				{	try
					{	h = Integer.parseInt(this.hi);
						array[MIN] = new XGAdress(h);
						m = Integer.parseInt(this.mid);
						array[MIN] = new XGAdress(h, m);
						l = Integer.parseInt(this.lo);
						array[MIN] = new XGAdress(h, m, l);
					}
					catch(NumberFormatException | NullPointerException e)
					{	//e.printStackTrace();
						return array;
					}
				}
				case DUMPSEQ:
				{	int hx, mx, lx;
					StringTokenizer st = new StringTokenizer(this.hi, "-");
					if(st.countTokens() == 1) h = hx = Integer.parseInt(this.hi);
					else
					{	h = Integer.parseInt(st.nextToken());
						hx = Integer.parseInt(st.nextToken());
					}
					st = new StringTokenizer(this.mid, "-");
					if(st.countTokens() == 1) m = mx = Integer.parseInt(this.mid);
					else
					{	m = Integer.parseInt(st.nextToken());
						mx = Integer.parseInt(st.nextToken());
					}
					st = new StringTokenizer(this.lo, "-");
					if(st.countTokens() == 1) l = lx = Integer.parseInt(this.lo);
					else
					{	l = Integer.parseInt(st.nextToken());
						lx = Integer.parseInt(st.nextToken());
					}
					array[MIN] = new XGAdress(h, m, l);
					array[MAX] = new XGAdress(hx, mx, lx);
					return array;
				}
			}
		}
	}

	class XGDD
	{	XGAdress min, max;
		String name;
	}
}