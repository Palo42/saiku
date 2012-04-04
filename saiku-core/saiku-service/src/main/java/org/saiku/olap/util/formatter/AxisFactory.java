package org.saiku.olap.util.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.olap4j.CellSetAxis;
import org.olap4j.CellSetAxisMetaData;
import org.olap4j.Position;
import org.olap4j.metadata.Hierarchy;

public class AxisFactory {

    private static final Logger LOGGER = Logger.getLogger(AxisFactory.class);

    /**
     * Computes a description of an axis.
     * In order to do that, it will browse all the positions of the axis.
     * That way we know all levels of all hierarchies which are displayed in the form : 
     * displayedLevelDepthByHierarchy : [[0, 1], [0, 1]]
     * 
     * Then we are going to select the levels we want to keep,
     * get the corresponding nodes and put them in the tree, tree layer by tree layer.
     * example :
     * mondrian returned the following positions : 
     * [all].[all] 
     * [all].[1]
     * [all].[2] 
     * [a].[all] 
     * [a].[1] 
     * [a].[2] 
     * [b].[all] 
     * [b].[1] 
     * [b].[2] 
     * we remove the [all].[1] and [all].[2] 
     * and build the following tree : 
     * [all].[all]
     *  -- [a].[all]
     *  ---- [a].[1] 
     *  ---- [a].[2] 
     *  -- [b].[all] 
     *  ---- [b].[1] 
     *  ---- [b].[2]
     * 
     * At each layer, we retrieve the axisNodes and add them to the final tree.
     * First layer is [all].[all] (depths : 0, 0),
     * second layer is [a].[all] and [b].[all] (depths : 1, 0),
     * third layer is [a].[1], [a].[2], [b].[1] and [b].[2] (depths : 1, 1)
     * 
     * That means we browse displayedLevelDepthByHierarchy and we will retrieve
     * the axisNodes of the levels we want to keep :
     * - for last level, keeps everything
     * - for other levels, only keeps the first level of all the other hierarchies ([all])
     * 
     * @param cellSetAxis
     *            Axis
     * @return Description of axis
     */
    public static Axis computeAxisInfo(CellSetAxis cellSetAxis) {
        if (cellSetAxis == null) {
            return new Axis();
        }
        CellSetAxisMetaData axisMetaData = cellSetAxis.getAxisMetaData();
        List<Hierarchy> hierarchyList = axisMetaData.getHierarchies();
        int hierarchyNb = hierarchyList.size();
        Axis axis = new Axis(hierarchyList);
        List<Position> axisPositionList = cellSetAxis.getPositions();

        if (!axisPositionList.isEmpty()) {
            List<AxisNode> axisNodeList = browsePositionList(axis, hierarchyList, axisPositionList);
            int[] depths = new int[hierarchyNb];
            MutableInt level = new MutableInt(-1);
            for (int i = 0; i < hierarchyNb; i++) {
                // get list of level depths displayed in that hierarchy
                TreeSet<Integer> hierarchyDepthSet = axis.getDisplayedLevelDepthByHierarchy(i);
                for (Iterator<Integer> iter = hierarchyDepthSet.iterator(); iter.hasNext();) {
                    Integer hierarchyDepth = iter.next();
                    depths[i] = hierarchyDepth;
                    if (iter.hasNext()) {
                        // build depths with first levels of the other displayed hierarchies
                        for (int j = i + 1; j < hierarchyNb; j++) {
                            TreeSet<Integer> nextHierarchyDepthSet = axis.getDisplayedLevelDepthByHierarchy(j);
                            Integer first = nextHierarchyDepthSet.first();
                            depths[j] = first;
                        }
                        addNodesOfThatDepthToAxisAndIncrementLevel(axis, axisNodeList, depths, level);
                    }
                }
            }
            addNodesOfThatDepthToAxisAndIncrementLevel(axis, axisNodeList, depths, level);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("result of positions sort inside a tree is : [" + axis.recursiveToString() + "]");
        }
        return axis;
    }

    /**
     * browse the list of positions and create the AxisNode instances
     * @param axis
     * @param hierarchyList
     * @param axisPositionList
     * @return
     */
    private static List<AxisNode> browsePositionList(Axis axis, List<Hierarchy> hierarchyList, List<Position> axisPositionList) {
        List<AxisNode> axisNodeList = new ArrayList<AxisNode>();
        for (Position position : axisPositionList) {
            AxisNode axisNode = new AxisNode(axis, position, hierarchyList);
            axisNodeList.add(axisNode);
        }
        return axisNodeList;
    }

    /**
     * add the nodes retrieved to the axis and increment level which gives the depth of the next nodes to add
     * since the nodes are added layer by layer
     * @param axis
     * @param axisNodeList
     * @param depths
     * @param level
     */
    private static void addNodesOfThatDepthToAxisAndIncrementLevel(Axis axis, List<AxisNode> axisNodeList, int[] depths, MutableInt level) {
        level.increment();
        List<AxisNode> axisNodeListOfDepth = getAxisNodeListOfDepth(axisNodeList, depths);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("found : [" + axisNodeListOfDepth.size() + "] axis nodes for depths : " + Arrays.toString(depths) + ", level : ["
                    + level.intValue() + "] to be added to axis");
        }
        for (AxisNode axisNode : axisNodeListOfDepth) {
            axis.sort(axisNode, level.intValue());
        }
    }

    /**
     * retrieve the nodes of depths specified in the list
     * @param axisNodeList
     * @param depths
     * @return
     */
    private static List<AxisNode> getAxisNodeListOfDepth(List<AxisNode> axisNodeList, int[] depths) {
        List<AxisNode> returned = new ArrayList<AxisNode>();
        for (AxisNode axisNode : axisNodeList) {
            if (axisNode.isDepths(depths)) {
                returned.add(axisNode);
            }
        }
        return returned;
    }

}
