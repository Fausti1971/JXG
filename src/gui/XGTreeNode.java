package gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * qualifiziert das implementierende Objekt als in einem Baum darstellbar.
 * @author thomas
 *
 */

public interface XGTreeNode extends TreeNode, XGDisplayable, XGAction
{
	static final Logger log = Logger.getAnonymousLogger();

/*****************************************************************************************************************/

	void setSelected(boolean s);
	boolean isSelected();

	@Override default JComponent getGuiComponent()	//für RootNode überschreiben!
	{	return ((XGTreeNode)this.getParent()).getGuiComponent();
	}

	default void repaintNode()
	{	JTree t = (JTree)this.getGuiComponent();
		TreePath p = this.getTreePath();
		Rectangle r = t.getPathBounds(p);
		if(r != null) t.repaint(r);	//falls sichtbar
	}

	default void reloadTree()
	{	((DefaultTreeModel)((JTree)this.getGuiComponent()).getModel()).reload(this.getParent());
	}

	default public TreePath getTreePath()
	{	List<TreeNode> array = new ArrayList<>();
		TreeNode n = this;
		while(n != null)
		{	array.add(0, n);
			n = n.getParent();
		}
		return new TreePath(array.toArray());
	}

	default String getNodeText()
	{	return this.toString();
	}

	@Override default int getChildCount()
	{	return Collections.list(this.children()).size();
	}

	@Override default public int getIndex(TreeNode node)
	{	return Collections.list(this.children()).indexOf(node);
	}

	@Override default public TreeNode getChildAt(int childIndex)
	{	return Collections.list(this.children()).get(childIndex);
	}

	@Override default public boolean isLeaf()
	{	return this.getChildCount() == 0;
	}
}
