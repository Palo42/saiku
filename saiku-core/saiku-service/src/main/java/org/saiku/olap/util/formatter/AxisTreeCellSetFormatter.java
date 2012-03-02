package org.saiku.olap.util.formatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;
import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.CellSetAxis;
import org.olap4j.CellSetAxisMetaData;
import org.olap4j.OlapException;
import org.olap4j.Position;
import org.olap4j.impl.CoordinateIterator;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;
import org.saiku.olap.dto.resultset.DataCell;
import org.saiku.olap.dto.resultset.Matrix;
import org.saiku.olap.dto.resultset.MemberCell;

public class AxisTreeCellSetFormatter implements ICellSetFormatter {

    /**
     * @param formattedValue
     * @return values
     */
    public static String getValueString(final String formattedValue) {
        final String[] values = formattedValue.split("\\|"); //$NON-NLS-1$
        if (values.length > 1) {
            return values[1];
        }
        return values[0];
    }

    public Matrix format(final CellSet cellSet) {
        // Compute how many rows are required to display the columns axis.
        final CellSetAxis columnsAxis;
        if (cellSet.getAxes().size() > 0) {
            columnsAxis = cellSet.getAxes().get(0);
        } else {
            columnsAxis = null;
        }
        final AxisNode rootColAxisNode = computeAxisInfo(columnsAxis);

        // Compute how many columns are required to display the rows axis.
        final CellSetAxis rowsAxis;
        if (cellSet.getAxes().size() > 1) {
            rowsAxis = cellSet.getAxes().get(1);
        } else {
            rowsAxis = null;
        }
        final AxisNode rootRowAxisNode = computeAxisInfo(rowsAxis);

        Matrix matrix = null;
        if (cellSet.getAxes().size() > 2) {
            final int[] dimensions = new int[cellSet.getAxes().size() - 2];
            for (int i = 2; i < cellSet.getAxes().size(); i++) {
                final CellSetAxis cellSetAxis = cellSet.getAxes().get(i);
                dimensions[i - 2] = cellSetAxis.getPositions().size();
            }
            for (final int[] pageCoords : CoordinateIterator.iterate(dimensions)) {
                matrix = formatPage(cellSet, pageCoords, columnsAxis, rootColAxisNode, rowsAxis, rootRowAxisNode);
            }
        } else {
            matrix = formatPage(cellSet, new int[] {}, columnsAxis, rootColAxisNode, rowsAxis, rootRowAxisNode);
        }

        return matrix;
    }

    /**
     * Computes a description of an axis.
     * the ordering by depth in hierarchy is not sufficient, each new node position members must be compared
     * to all the tree nodes position members, and the children nodes may need reordering
     * 
     * @param axis
     *            Axis
     * @return Description of axis
     */
    public static AxisNode computeAxisInfo(CellSetAxis axis) {
        if (axis == null) {
            return new AxisNode();
        }
        AxisNode rootAxisNode = new AxisNode();
        CellSetAxisMetaData axisMetaData = axis.getAxisMetaData();
        List<Hierarchy> hierarchyList = axisMetaData.getHierarchies();
        int hierarchyNb = hierarchyList.size();
        rootAxisNode.depthInHierarchyList = new int[hierarchyNb];
        for (int i = 0; i < hierarchyNb; i++) {
            rootAxisNode.depthInHierarchyList[i] = -1;
        }
        List<Position> axisPositionList = axis.getPositions();
        if (!axisPositionList.isEmpty()) {
            AxisNode currentAxisNode = new AxisNode();
            rootAxisNode.addChild(currentAxisNode);
            currentAxisNode.root = rootAxisNode;
            Position firstPosition = axisPositionList.get(0);
            setHierarchyAndDepth(currentAxisNode, hierarchyList, firstPosition);
            for (int i = 1; i < axisPositionList.size(); i++) {
                Position position = axisPositionList.get(i);
                AxisNode newNode = new AxisNode();
                newNode.root = rootAxisNode;
                setHierarchyAndDepth(newNode, hierarchyList, position);
                while (!newNode.isUnder(currentAxisNode)) {
                    currentAxisNode = currentAxisNode.parent;
                }
                currentAxisNode.addChild(newNode);
                currentAxisNode = newNode;
                
            }
        }
        return rootAxisNode;
    }

    public static void setHierarchyAndDepth(AxisNode axisNode, List<Hierarchy> hierarchyList, Position position) {
        axisNode.position = position;
        int size = hierarchyList.size();
        axisNode.depthInHierarchyList = new int[size];
        List<Member> memberList = position.getMembers();
        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            int memberDepth = member.getDepth();
            axisNode.depthInHierarchyList[i] = memberDepth;
        }
    }

    /**
     * Formats a two-dimensional page.
     * 
     * @param cellSet
     *            Cell set
     * @param pw
     *            Print writer
     * @param pageCoords
     *            Coordinates of page [page, chapter, section, ...]
     * @param columnsAxis
     *            Columns axis
     * @param columnsAxisInfo
     *            Description of columns axis
     * @param rowsAxis
     *            Rows axis
     * @param rowsAxisInfo
     *            Description of rows axis
     */
    private Matrix formatPage(final CellSet cellSet, final int[] pageCoords, final CellSetAxis columnsAxis, final AxisNode rootColAxisNode,
            final CellSetAxis rowsAxis, final AxisNode rootRowAxisNode) {
        // Figure out the dimensions of the blank rectangle in the top left
        // corner.
        int xOffset = rootRowAxisNode.getDepth();
        int yOffset = rootColAxisNode.getDepth();

        List<Position> columnPositionList = columnsAxis.getPositions();
        int columnPositionListSize = columnPositionList.size();
        List<Position> rowPositionList = rowsAxis.getPositions();
        int rowPositionListSize = rowPositionList.size();
        // Populate a string matrix
        final Matrix matrix = new Matrix(xOffset + (columnsAxis == null ? 1 : columnPositionListSize), yOffset
                + (rowsAxis == null ? 1 : rowPositionListSize));

        populateCorner(rootColAxisNode, rootRowAxisNode, matrix, xOffset, yOffset);
        populateRowAxis(rootColAxisNode, rootRowAxisNode, matrix, yOffset, rowPositionListSize);

        populateColAxisWithValues(rootColAxisNode, rootRowAxisNode, cellSet, matrix, xOffset, yOffset);

        return matrix;

    }

    public static void populateCorner(AxisNode rootColAxisNode, AxisNode rootRowAxisNode, Matrix matrix, int xOffset, int yOffset) {
        for (int x = 0; x < xOffset; x++) {
            for (int y = 0; y < yOffset; y++) {
                MemberCell memberCell = new MemberCell(false, x > 0);
                memberCell.setRawValue("");
                memberCell.setFormattedValue("");
                memberCell.setProperty("__headertype", "row_header_header");
                memberCell.setProperty("levelindex", "1");
                matrix.set(x, y, memberCell);
            }
        }
    }

    public static void populateRowAxis(AxisNode rootColAxisNode, AxisNode rootRowAxisNode, Matrix matrix, int yOffset, int rowPositionListSize) {
        MutableInt y = new MutableInt(yOffset);
        List<AxisNode> rootRowAxisNodeChildren = rootRowAxisNode.children;
        if (rootRowAxisNodeChildren != null) {
            for (AxisNode rowAxisNode : rootRowAxisNodeChildren) {
                populateRowAxisNode(rowAxisNode, matrix, 0, y);
            }
        }
    }

    public static void populateRowAxisNode(AxisNode rowAxisNode, Matrix matrix, int x, MutableInt y) {
        Position position = rowAxisNode.position;
        if (position != null) {
            List<Member> memberList = position.getMembers();
            AxisNode previousSibling = rowAxisNode.getPreviousSibling();
            boolean fusionCell = true;
            int xIncrement = 0;
            for (int i = 0; i < memberList.size(); i++) {
                //should not need this as tree ordering should be correct
                Member member = memberList.get(i);
                Integer depth = member.getDepth();
                List<Integer> allLevelDisplayedInHierarchy = new ArrayList<Integer>();
                rowAxisNode.root.getAllLevelDisplayedInHierarchy(i, allLevelDisplayedInHierarchy);
                fusionCell = fillHeader(matrix, memberList, i, previousSibling, fusionCell, x + xIncrement, y.intValue());
                int indexOf = allLevelDisplayedInHierarchy.indexOf(depth);
                int inc = allLevelDisplayedInHierarchy.size() - indexOf;
                for (int j = x + xIncrement + 1; j < x + xIncrement + inc; j++) {
                    fillWithNull(matrix, j, y.intValue());
                }
                xIncrement += inc; 
            }
        }
        List<AxisNode> rowAxisNodeChildren = rowAxisNode.children;
        if (rowAxisNodeChildren != null) {
            AxisNode firstChild = rowAxisNodeChildren.get(0);
            populateRowAxisNode(firstChild, matrix, x + 1, y);
            for (int i = 1; i < rowAxisNodeChildren.size(); i++) {
                AxisNode child = rowAxisNodeChildren.get(i);
                for (int j = 0; j <= x; j++) {
                    fillWithNull(matrix, j, y.intValue());
                }
                populateRowAxisNode(child, matrix, x + 1, y);
            }
            // fill before total node with null
            for (int j = 0; j <= x; j++) {
                fillWithNull(matrix, j, y.intValue());
            }
            // dump total node
            fillTotal(matrix, x + 1, y.intValue());

            // fill after total node with null
            int depth = rowAxisNode.getDepth();
            for (int j = x + 2; j < x + 2 + depth; j++) {
                fillWithNull(matrix, j, y.intValue());
            }
        }
        y.increment();
    }

    public static void fillTotal(Matrix matrix, int x, int y) {
        String caption = "total";
        MemberCell totalMemberCell = new MemberCell();
        totalMemberCell.setRawValue(caption);
        totalMemberCell.setFormattedValue(caption);
        matrix.set(x, y, totalMemberCell);
    }

    protected static void fillCell(Matrix matrix, Member member, int x, int y) {
        MemberCell memberCell = new MemberCell();
        String caption = member.getCaption();
        memberCell.setRawValue(caption);
        memberCell.setFormattedValue(caption);
        List<String> memberPath = new ArrayList<String>();
        memberPath.add(member.getUniqueName());
        memberCell.setMemberPath(memberPath);
        matrix.set(x, y, memberCell);
    }

    protected void populateColAxisWithValues(AxisNode rootColAxisNode, AxisNode rootRowAxisNode, CellSet cellSet, Matrix matrix, int xOffset,
            int yOffset) {
        MutableInt x = new MutableInt(xOffset);
        List<AxisNode> rootColAxisNodeChildren = rootColAxisNode.children;
        if (rootColAxisNodeChildren != null) {
            for (AxisNode colAxisNode : rootColAxisNodeChildren) {
                populateColAxisNodeWithValues(colAxisNode, rootRowAxisNode, cellSet, matrix, yOffset, x, 0);
            }
        }
    }

    public static void populateColAxisNodeWithValues(AxisNode colAxisNode, AxisNode rootRowAxisNode, CellSet cellSet, Matrix matrix, int yOffset,
            MutableInt x, int y) {
        Position position = colAxisNode.position;
        if (position != null) {
            List<Member> memberList = position.getMembers();
            AxisNode previousSibling = colAxisNode.getPreviousSibling();
            boolean fusionCell = true;
            int yIncrement = 0;
            for (int i = 0; i < memberList.size(); i++) {
              //should not need this as tree ordering should be correct
                Member member = memberList.get(i);
                Integer depth = member.getDepth();
                List<Integer> allLevelDisplayedInHierarchy = new ArrayList<Integer>();
                colAxisNode.root.getAllLevelDisplayedInHierarchy(i, allLevelDisplayedInHierarchy);
                fusionCell = fillHeader(matrix, memberList, i, previousSibling, fusionCell, x.intValue(), y + yIncrement);
                int indexOf = allLevelDisplayedInHierarchy.indexOf(depth);
                int inc = allLevelDisplayedInHierarchy.size() - indexOf;
                for (int j = y + yIncrement + 1; j < y + yIncrement + inc; j++) {
                    fillWithNull(matrix, x.intValue(), j);
                }
                yIncrement += inc;
            }
        }
        List<AxisNode> colAxisNodeChildren = colAxisNode.children;
        if (colAxisNodeChildren != null) {
            AxisNode firstChild = colAxisNodeChildren.get(0);
            populateColAxisNodeWithValues(firstChild, rootRowAxisNode, cellSet, matrix, yOffset, x, y + 1);
            for (int i = 1; i < colAxisNodeChildren.size(); i++) {
                AxisNode child = colAxisNodeChildren.get(i);
                for (int j = 0; j <= y; j++) {
                    fillWithNull(matrix, x.intValue(), j);
                }
                populateColAxisNodeWithValues(child, rootRowAxisNode, cellSet, matrix, yOffset, x, y + 1);
            }
            for (int j = 0; j <= y; j++) {
                fillWithNull(matrix, x.intValue(), j);
            }
            // dump total node
            fillTotal(matrix, x.intValue(), y + 1);
            int depth = colAxisNode.getDepth();
            for (int j = y + 2; j < y + 2 + depth; j++) {
                fillWithNull(matrix, x.intValue(), j);
            }
        }
        MutableInt yCell = new MutableInt(yOffset);
        populateColCells(cellSet, colAxisNode, rootRowAxisNode, position, matrix, x, yCell);
        x.increment();
    }

    private static boolean fillHeader(Matrix matrix, List<Member> memberList, int i, AxisNode previousSibling, boolean fusionCell, int x, int y) {
        Member member = memberList.get(i);
        // once fusionCell has been set to false, any other line must not be
        // fusioned
        if (fusionCell) {
            fusionCell = isFusionCell(previousSibling, i, member);
        }
        if (fusionCell) {
            fillWithNull(matrix, x, y);
        } else {
            fillCell(matrix, member, x, y);
        }
        return fusionCell;
    }

    public static boolean isFusionCell(AxisNode previousSibling, int i, Member member) {
        boolean fusionCell = false;
        if (previousSibling != null) {
            List<Member> previousSiblingMemberList = previousSibling.position.getMembers();
            Member previousMember = previousSiblingMemberList.get(i);
            if (previousMember.equals(member)) {
                fusionCell = true;
            }
        }
        return fusionCell;
    }

    public static void populateColCells(CellSet cellSet, AxisNode colAxisNode, AxisNode rowAxisNode, Position colPosition, Matrix matrix,
            MutableInt x, MutableInt y) {
        List<AxisNode> rowAxisNodeChildren = rowAxisNode.children;
        if (rowAxisNodeChildren != null) {
            for (AxisNode rowAxisNodeChild : rowAxisNodeChildren) {
                populateColCells(cellSet, colAxisNode, rowAxisNodeChild, colPosition, matrix, x, y);
            }
        }
        Position rowPosition = rowAxisNode.position;
        // not root node
        if (rowPosition != null) {
            Cell cell = cellSet.getCell(colPosition, rowPosition);
            List<Integer> coordinateList = cell.getCoordinateList();
            DataCell cellInfo = new DataCell(true, false, coordinateList);

            if (cell.getValue() != null) {
                try {
                    cellInfo.setRawNumber(cell.getDoubleValue());
                } catch (OlapException e1) {
                }
            }
            String cellValue = cell.getFormattedValue(); // First try to get a
            // formatted value

            if (cellValue == null || cellValue.equals("null")) { //$NON-NLS-1$
                cellValue = ""; //$NON-NLS-1$
            }
            if (cellValue.length() < 1) {
                final Object value = cell.getValue();
                if (value == null || value.equals("null")) //$NON-NLS-1$
                    cellValue = ""; //$NON-NLS-1$
                else {
                    try {
                        DecimalFormat myFormatter = new DecimalFormat("#,###.###"); //$NON-NLS-1$
                        String output = myFormatter.format(cell.getValue());
                        cellValue = output;
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                // the raw value
            }
            cellInfo.setFormattedValue(getValueString(cellValue));
            matrix.set(x.intValue(), y.intValue(), cellInfo);
            y.increment();
        }
    }

    public static void fillWithNull(Matrix matrix, int x, int y) {
        MemberCell nullMemberCell = new MemberCell();
        nullMemberCell.setRawValue(null);
        nullMemberCell.setFormattedValue(null);
        matrix.set(x, y, nullMemberCell);
    }

    private static class AxisNode {
        public AxisNode root;
        public int[] depthInHierarchyList;
        public int indexInChildren;
        public Position position;
        public List<AxisNode> children;
        public AxisNode parent;

        public void addChild(AxisNode child) {
            if (children == null) {
                children = new ArrayList<AxisNode>();
            }
            children.add(child);
            child.parent = this;
            child.indexInChildren = children.size() - 1;
        }
        
        public List<Integer> getAllLevelDisplayedInHierarchy(int hierarchyIdx, List<Integer> allLevelDisplayedInHierarchy) {
            Integer levelDisplayed = depthInHierarchyList[hierarchyIdx];
            if (levelDisplayed != -1 && !allLevelDisplayedInHierarchy.contains(levelDisplayed)) {
                 allLevelDisplayedInHierarchy.add(levelDisplayed);
            }
            if (children != null) {
                for (AxisNode child : children) {
                    allLevelDisplayedInHierarchy = child.getAllLevelDisplayedInHierarchy(hierarchyIdx, allLevelDisplayedInHierarchy);
                }
            }
            return allLevelDisplayedInHierarchy;
        }

        public boolean isUnder(AxisNode compared) {
            for (int i = 0; i < depthInHierarchyList.length; i++) {
                int depth = depthInHierarchyList[i];
                int comparedDepth = compared.depthInHierarchyList[i];
                if (comparedDepth < depth) {
                    return true;
                } else if (comparedDepth > depth) {
                    return false;
                }
            }
            return false;
        }

        public AxisNode getPreviousSibling() {
            if (indexInChildren == 0) {
                return null;
            }
            return parent.children.get(indexInChildren - 1);
        }

        public int getDepth() {
            if (children == null) {
                if (position == null) {
                    return 0;
                }
                List<Member> memberList = position.getMembers();
                return memberList.size() - 1;
            }
            int childMaxDepth = 0;
            for (AxisNode child : children) {
                int childDepth = child.getDepth();
                childMaxDepth = Math.max(childMaxDepth, childDepth);
            }
            return childMaxDepth + 1;
        }

        @Override
        public String toString() {
            if (position == null) {
                return "";
            }
            StringBuffer returned = new StringBuffer("depth");
            returned.append(java.util.Arrays.toString(depthInHierarchyList));
            if (position != null) {
                returned.append(position.getMembers().toString());
            }
            return returned.toString();
        }
    }

}
