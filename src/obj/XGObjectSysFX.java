 package obj;

import java.util.Map;
import parm.XGParameterMap;
import parm.XGParameter;

public class XGObjectSysFX extends XGObject
{	private final static Map<Integer, XGParameter> PARAMETERS = XGParameterMap.getParameterMap("fx1_parameters");

	/************** Instance ************************************************************************/

	public XGObjectSysFX(XGAdress adr)
	{	super(adr);}

	@Override public XGParameter getParameter(int offs)
	{	return PARAMETERS.get(offs);
	}
}