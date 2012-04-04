package org.saiku.olap.util.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.olap4j.Position;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;

public class AxisNode {
    
    private static final Logger LOGGER = Logger.getLogger(AxisNode.class);
    
    private Axis axis;
    /**
     * depths of the levels of the hierarchies that this axis node is about
     */
    private int[] depthInHierarchyList;
    private int indexInChildren;
    private Position position;
    private List<AxisNode> children;
    private AxisNode parent;
	private List<Hierarchy> hierarchyList;

    public AxisNode(Axis axis, Position position, List<Hierarchy> hierarchyList) {
        this.axis = axis;
        this.position = position;
        this.hierarchyList = hierarchyList;
        int size = hierarchyList.size();
        depthInHierarchyList = new int[size];
        List<Member> memberList = position.getMembers();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("creating node with position : " + memberList);
        }
        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            int memberDepth = member.getDepth();
            depthInHierarchyList[i] = memberDepth;
			axis.addDisplayedLevelDepthByHierarchy(i, memberDepth);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("position is given following depthInHierarchyList : " + Arrays.toString(depthInHierarchyList));
        }
    }

	public void addChild(AxisNode child) {
        if (children == null) {
            children = new ArrayList<AxisNode>();
        }
        children.add(child);
        child.parent = this;
        child.indexInChildren = children.size() - 1;
    }

	/**
	 * if new axisNode must be added at the level under current axisNode,
	 * checks that the new axisNode position is under current position and adds it to the children
	 * @param axisNode
	 * @param level
	 */
    public void sort(AxisNode axisNode, int level) {
        if (level > 0) {
            for (AxisNode child : children) {
                child.sort(axisNode, level - 1);
            }
        } else {
            boolean isChildOfCurrent = true;
            if (position != null) {
                List<Member> memberList = position.getMembers();
                List<Member> newNodeMemberList = axisNode.position.getMembers();
                for (int i = 0; i < memberList.size() ; i++) {
                    Member member = memberList.get(i);
                    Member newNodeMember = newNodeMemberList.get(i);
                    if (!newNodeMember.equals(member) && !newNodeMember.getAncestorMembers().contains(member)) {
                        isChildOfCurrent = false;
                    }
                }
            }
            if (isChildOfCurrent) {
                addChild(axisNode);
            }
        }
    }

    /**
     * retrieves previous browser, usefull to know if cell must be fusioned
     * @return
     */
    public AxisNode getPreviousSibling() {
        if (indexInChildren == 0) {
            return null;
        }
        return parent.children.get(indexInChildren - 1);
    }

    /**
     * retrieves the number of columns under current axisNode, i.e. the number of layers and
     * the number of members of the position
     * @return
     */
    public int getDepth() {
        if (children == null) {
            if (position == null) {
                return 0;
            }
            List<Member> memberList = position.getMembers();
            return memberList.size();
        }
        int childMaxDepth = 0;
        for (AxisNode child : children) {
            int childDepth = child.getDepth();
            childMaxDepth = Math.max(childMaxDepth, childDepth);
        }
        return childMaxDepth + 1;
    }
    
    /**
     * is that node corresponding to depths specified
     * @param depths
     * @return
     */
	public boolean isDepths(int[] depths) {
        for (int i = 0; i < depthInHierarchyList.length; i++) {
            int depthInHierarchy = depthInHierarchyList[i];
            if (depths[i] != depthInHierarchy) {
                return false;
            }
        }
        return true;
	}
	
	/**
	 * for logging purpose, dump this axis node and children as an indented tree
	 * @param i
	 * @param returned
	 */
    public void recursiveToString(int i, StringBuilder returned) {
        if (position != null) {
            for (int j = 0; j < i; j++) {
                returned.append("--");
            }
            returned.append("position : " + position.getMembers() + "\n");
            for (int j = 0; j < i; j++) {
                returned.append("--");
            }
            returned.append(java.util.Arrays.toString(depthInHierarchyList) + "\n");
        }
        if (children != null) {
            for (AxisNode child : children) {
                child.recursiveToString(i + 1, returned);
            }
        }
    }
	
    @Override
    public String toString() {
        if (position == null) {
            return "";
        }
        StringBuffer returned = new StringBuffer("depth : ");
        returned.append(Arrays.toString(depthInHierarchyList));
        if (position != null) {
            returned.append(", position : " + position.getMembers().toString());
        }
        return returned.toString();
    }

	public Axis getAxis() {
		return axis;
	}

	public void setAxis(Axis axis) {
		this.axis = axis;
	}

	public int getIndexInChildren() {
		return indexInChildren;
	}

	public void setIndexInChildren(int indexInChildren) {
		this.indexInChildren = indexInChildren;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public List<AxisNode> getChildren() {
		return children;
	}

	public void setChildren(List<AxisNode> children) {
		this.children = children;
	}

	public AxisNode getParent() {
		return parent;
	}

	public void setParent(AxisNode parent) {
		this.parent = parent;
	}
	
	public List<Hierarchy> getHierarchyList() {
		return hierarchyList;
	}
	
	public void setHierarchyList(List<Hierarchy> hierarchyList) {
		this.hierarchyList = hierarchyList;
	}
	
}