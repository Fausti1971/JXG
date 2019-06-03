package gui;

import java.util.HashSet;
import java.util.Set;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import adress.InvalidXGAdressException;
import adress.XGAdress;
import obj.XGObjectType;
import value.XGValue;
import value.XGValueChangeListener;
import value.XGValueStorage;

public class XGObjectTableModel implements TableModel, XGValueChangeListener
{	private XGObjectType ot;
	private XGAdress adress;
	private Integer[] col;
	private Set<TableModelListener> tml = new HashSet<>();
	private Set<XGValueChangeListener> vcl = new HashSet<>();

	public XGObjectTableModel(XGAdress adr) throws InvalidXGAdressException
	{	this.adress = adr;
		this.ot = XGObjectType.getObjectType(adr);
		XGValueStorage.addListener(this);
		this.col = ot.getParameterMap().keySet().toArray(Integer[]::new);
	}

	public XGAdress getAdress()
	{	return this.adress;}

	public void setValueAt(Object aValue,int rowIndex,int columnIndex)
	{	for(TableModelListener l : tml) l.tableChanged(new TableModelEvent(this, rowIndex, rowIndex, columnIndex));}
	
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{	return true;}
	
	public Object getValueAt(int rowIndex, int columnIndex)
	{	try
		{	return XGValueStorage.getValue(new XGAdress(this.adress.getHi(), rowIndex, col[columnIndex]));
		}
		catch(InvalidXGAdressException e)
		{	e.printStackTrace();
		}
		return null;
	}
	
	public int getRowCount()
	{	return XGValueStorage.getObjectInstances(this.adress).size();}
	
	public String getColumnName(int columnIndex)
	{	return ot.getParameter(col[columnIndex]).getLongName();}
	
	public int getColumnCount()
	{	return col.length;}
	
	public Class<?> getColumnClass(int columnIndex)
	{	return XGValue.class;}

	public void valueChanged(XGValue v)
	{	try
		{	if(v.getAdress().getLo() < col.length)
			{	this.setValueAt(v, v.getAdress().getMid(), col[v.getAdress().getLo()]);
			}
			for(XGValueChangeListener a : this.vcl)
			{	//if(v.getAdress().equalsMaskedValidFields(a.getAdress())) a.valueChanged(v);
				System.out.println("value changed: " + v);
				a.valueChanged(v);
			}
			
		}
		catch(InvalidXGAdressException e)
		{	e.printStackTrace();}
//		System.out.println("tm value changed: " + v.getAdress());
	}

	public void addTableModelListener(TableModelListener l)
	{	this.tml.add(l);}

	public void removeTableModelListener(TableModelListener l)
	{	this.tml.remove(l);}
}
