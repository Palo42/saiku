package org.saiku.olap.util.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.olap4j.metadata.Hierarchy;

public class Axis {

    private static final Logger LOGGER = Logger.getLogger(Axis.class);

    private List<AxisNode> rootAxisNodeList;
    /**
     * for each hierarchy, list of the depth of each level displayed
     */
    private TreeSet<Integer>[] displayedLevelDepthByHierarchy;

    private List<Hierarchy> hierarchyList;
    private int axisNodesNb = 0;

    public Axis() {
    }

    public Axis(List<Hierarchy> hierarchyList) {
        this.hierarchyList = hierarchyList;
        rootAxisNodeList = new ArrayList<AxisNode>();
        int hierarchyNb = hierarchyList.size();
        displayedLevelDepthByHierarchy = new TreeSet[hierarchyNb];
        for (int i = 0; i < hierarchyNb; i++) {
            displayedLevelDepthByHierarchy[i] = new TreeSet<Integer>();
        }
    }

    public int getDepth() {
        if (rootAxisNodeList == null || rootAxisNodeList.isEmpty()) {
            return 0;
        }
        AxisNode rootAxisNode = rootAxisNodeList.get(0);
        int depth = rootAxisNode.getDepth();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("this axis is : [" + depth + "] levels deep");
        }
        return depth;
    }

    public void sort(AxisNode axisNode, int level) {
        axisNodesNb++;
        if (level == 0) {
            rootAxisNodeList.add(axisNode);
        } else {
            for (AxisNode rootAxisNode : rootAxisNodeList) {
                rootAxisNode.sort(axisNode, level - 1);
            }
        }
    }
    
    public String recursiveToString() {
        StringBuilder returned = new StringBuilder("\n");
        for (AxisNode rootAxisNode : rootAxisNodeList) {
            rootAxisNode.recursiveToString(0, returned);
        }
        return returned.toString();
    }
    
    public int getAxisNodesNb() {
        return axisNodesNb;
    }

    public TreeSet<Integer> getDisplayedLevelDepthByHierarchy(int i) {
        return displayedLevelDepthByHierarchy[i];
    }

    public void addDisplayedLevelDepthByHierarchy(int i, int memberDepth) {
        displayedLevelDepthByHierarchy[i].add(memberDepth);
    }

    public List<AxisNode> getRootAxisNodeList() {
        return rootAxisNodeList;
    }

    public void setRootAxisNodeList(List<AxisNode> rootAxisNodeList) {
        this.rootAxisNodeList = rootAxisNodeList;
    }

    public List<Hierarchy> getHierarchyList() {
        return hierarchyList;
    }

    public void setHierarchyList(List<Hierarchy> hierarchyList) {
        this.hierarchyList = hierarchyList;
    }

}