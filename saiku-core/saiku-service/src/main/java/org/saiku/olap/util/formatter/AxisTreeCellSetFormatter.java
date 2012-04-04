package org.saiku.olap.util.formatter;


import java.text.DecimalFormat;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.CellSetAxis;
import org.olap4j.OlapException;
import org.olap4j.Position;
import org.olap4j.impl.CoordinateIterator;
import org.olap4j.metadata.Member;
import org.saiku.olap.dto.resultset.DataCell;
import org.saiku.olap.dto.resultset.Matrix;
import org.saiku.olap.dto.resultset.MemberCell;

public class AxisTreeCellSetFormatter implements ICellSetFormatter {

	private static final Logger LOGGER = Logger.getLogger(AxisTreeCellSetFormatter.class);

    public Matrix format(final CellSet cellSet) {
        // Compute how many rows are required to display the columns axis.
        final CellSetAxis columnsAxis;
        if (cellSet.getAxes().size() > 0) {
            columnsAxis = cellSet.getAxes().get(0);
        } else {
            columnsAxis = null;
        }
        final Axis colAxis = AxisFactory.computeAxisInfo(columnsAxis);

        // Compute how many columns are required to display the rows axis.
        final CellSetAxis rowsAxis;
        if (cellSet.getAxes().size() > 1) {
            rowsAxis = cellSet.getAxes().get(1);
        } else {
            rowsAxis = null;
        }
        final Axis rowAxis = AxisFactory.computeAxisInfo(rowsAxis);

        Matrix matrix = null;
        if (cellSet.getAxes().size() > 2) {
            final int[] dimensions = new int[cellSet.getAxes().size() - 2];
            for (int i = 2; i < cellSet.getAxes().size(); i++) {
                final CellSetAxis cellSetAxis = cellSet.getAxes().get(i);
                dimensions[i - 2] = cellSetAxis.getPositions().size();
            }
            for (final int[] pageCoords : CoordinateIterator.iterate(dimensions)) {
                matrix = formatPage(cellSet, pageCoords, columnsAxis, colAxis, rowsAxis, rowAxis);
            }
        } else {
            matrix = formatPage(cellSet, new int[] {}, columnsAxis, colAxis, rowsAxis, rowAxis);
        }

        return matrix;
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
    private Matrix formatPage(final CellSet cellSet, final int[] pageCoords, final CellSetAxis columnsAxis, final Axis colAxis,
            final CellSetAxis rowsAxis, final Axis rowAxis) {
        // Figure out the dimensions of the blank rectangle in the top left
        // corner.
        int xOffset = rowAxis.getDepth();
        int yOffset = colAxis.getDepth();

        int columnPositionListSize = colAxis.getAxisNodesNb();
        int rowPositionListSize = rowAxis.getAxisNodesNb();
        // Populate a string matrix
        int width = xOffset + (columnsAxis == null ? 1 : columnPositionListSize);
        int height = yOffset + (rowsAxis == null ? 1 : rowPositionListSize);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("found : [" + columnPositionListSize + "] positions on the column axis and : [" + rowPositionListSize
                    + "] positions on the row axis for : [" + xOffset + "] row headers and : [" + yOffset
                    + "] column headers, matrix generated will be : [" + width + "] width and : [" + height + "] height");
        }
        final Matrix matrix = new Matrix(width, height);

        populateCorner(colAxis, rowAxis, matrix, xOffset, yOffset);
        populateRowAxis(colAxis, rowAxis, matrix, yOffset, rowPositionListSize);

        populateColAxisWithValues(colAxis, rowAxis, cellSet, matrix, xOffset, yOffset);

        return matrix;

    }

    /**
     * not implemented, corner is empty
     * @param colAxis
     * @param rowAxis
     * @param matrix
     * @param xOffset
     * @param yOffset
     */
    protected void populateCorner(Axis colAxis, Axis rowAxis, Matrix matrix, int xOffset, int yOffset) {
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

    /**
     * browse the row axis in order to fill the headers of the rows
     * @param colAxis
     * @param rowAxis
     * @param matrix
     * @param yOffset
     * @param rowPositionListSize
     */
    protected void populateRowAxis(Axis colAxis, Axis rowAxis, Matrix matrix, int yOffset, int rowPositionListSize) {
        MutableInt y = new MutableInt(yOffset);
        List<AxisNode> rootRowAxisNodeChildren = rowAxis.getRootAxisNodeList();
        if (rootRowAxisNodeChildren != null) {
            for (AxisNode rowAxisNode : rootRowAxisNodeChildren) {
                populateRowAxisNode(rowAxisNode, matrix, y);
            }
        }
    }

    /**
     * on each row axis node, fill the row headers, then browse the children and fill the total line headers
     * y is incremented at each execution of that method
     * @param rowAxisNode
     * @param matrix
     * @param y
     */
    protected void populateRowAxisNode(AxisNode rowAxisNode, Matrix matrix, MutableInt y) {
        Position position = rowAxisNode.getPosition();
        if (position != null) {
            fillRowHeaders(rowAxisNode, matrix, y.intValue(), position);
        }
        List<AxisNode> rowAxisNodeChildren = rowAxisNode.getChildren();
        if (rowAxisNodeChildren != null) {
            for (AxisNode child : rowAxisNodeChildren) {
                populateRowAxisNode(child, matrix, y);                
            }
            fillRowTotalLine(rowAxisNode, matrix, y.intValue(), position);
        }
        y.increment();
    }

    /**
     * fill the row headers for that row axis node
     * browse the hierarchies and dump row title
     * @param rowAxisNode
     * @param matrix
     * @param y
     * @param position
     */
    protected void fillRowHeaders(AxisNode rowAxisNode, Matrix matrix, int y, Position position) {
        List<Member> memberList = position.getMembers();
        AxisNode previousSibling = rowAxisNode.getPreviousSibling();
        int x = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            Axis axis = rowAxisNode.getAxis();
            TreeSet<Integer> hierarchyDepthSet = axis.getDisplayedLevelDepthByHierarchy(i);
            for (Integer hierarchyDepth : hierarchyDepthSet) {
                if (member.getDepth() == hierarchyDepth.intValue()) {
                    FillUtils.fillHeader(matrix, member, i, previousSibling, x, y);
                } else {
                    FillUtils.fillIfIsNull(matrix, x, y);
                }
                x++;
            }
        }
    }

    /**
     * fill the headers for a total row
     * @param rowAxisNode
     * @param matrix
     * @param y
     * @param position
     */
    protected void fillRowTotalLine(AxisNode rowAxisNode, Matrix matrix, int y, Position position) {
        int x = 0;
        List<Member> memberList = position.getMembers();
        boolean totalDumped = false;
        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            int depth = member.getDepth();
            Axis axis = rowAxisNode.getAxis();
            TreeSet<Integer> hierarchyDepthSet = axis.getDisplayedLevelDepthByHierarchy(i);
            //if not a total on that hierarchy, dumps empty
            if (depth == hierarchyDepthSet.last().intValue()) {
                for (int j = 0; j < hierarchyDepthSet.size(); j++) {
                    FillUtils.fillWithNullOrEmptyIfTotalDumped(matrix, x, y, totalDumped);
                    x++;
                }
            } else {
                for (Integer hierarchyDepth : hierarchyDepthSet) {
                    //this is a total on that hierarchy then dumps total at x+1 case
                    if (depth == hierarchyDepth.intValue()) {
                        FillUtils.fillWithNullOrEmptyIfTotalDumped(matrix, x, y, totalDumped);
                        if (!totalDumped) {
                            FillUtils.fillTotal(matrix, x + 1, y);
                            totalDumped = true;
                        }
                    } else {
                        FillUtils.fillWithNullOrEmptyIfTotalDumped(matrix, x, y, totalDumped);
                    }
                    x++;
                }
            }
        }
    }

    /**
     * browse the column axis in order to fill the column headers and the value in the matrix
     * @param colAxis
     * @param rowAxis
     * @param cellSet
     * @param matrix
     * @param xOffset
     * @param yOffset
     */
    protected void populateColAxisWithValues(Axis colAxis, Axis rowAxis, CellSet cellSet, Matrix matrix, int xOffset,
            int yOffset) {
        MutableInt x = new MutableInt(xOffset);
        List<AxisNode> rootColAxisNodeChildren = colAxis.getRootAxisNodeList();
        if (rootColAxisNodeChildren != null) {
            for (AxisNode colAxisNode : rootColAxisNodeChildren) {
                populateColAxisNodeWithValues(colAxisNode, rowAxis, cellSet, matrix, yOffset, x);
            }
        }
    }

    /**
     * dump the column headers and the values of that axis node
     * fill the column headers, browse the children axis nodes, then dump the total line columns headers and finally dump the cell values
     * @param colAxisNode
     * @param rowAxis
     * @param cellSet
     * @param matrix
     * @param yOffset
     * @param x
     */
    protected void populateColAxisNodeWithValues(AxisNode colAxisNode, Axis rowAxis, CellSet cellSet, Matrix matrix, int yOffset,
            MutableInt x) {
        Position position = colAxisNode.getPosition();
        if (position != null) {
            fillColHeaders(matrix, colAxisNode, x.intValue(), position);
        }
        List<AxisNode> colAxisNodeChildren = colAxisNode.getChildren();
        if (colAxisNodeChildren != null) {
            for (AxisNode child : colAxisNodeChildren) {
                populateColAxisNodeWithValues(child, rowAxis, cellSet, matrix, yOffset, x);                
            }
            fillColTotalLine(matrix, colAxisNode, x.intValue(), position);
        }
        MutableInt yCell = new MutableInt(yOffset);
        List<AxisNode> rootAxisNodeList = rowAxis.getRootAxisNodeList();
        for (AxisNode rowRootAxisNode : rootAxisNodeList) {
            populateColCells(cellSet, colAxisNode, rowRootAxisNode, position, matrix, x, yCell);
        }
        x.increment();
    }
    
    /**
     * fill the column headers for that axis node
     * @param matrix
     * @param colAxisNode
     * @param x
     * @param position
     */
    protected void fillColHeaders(Matrix matrix, AxisNode colAxisNode, int x, Position position) {
        List<Member> memberList = position.getMembers();
        AxisNode previousSibling = colAxisNode.getPreviousSibling();
        int y = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            Axis axis = colAxisNode.getAxis();
            TreeSet<Integer> hierarchyDepthSet = axis.getDisplayedLevelDepthByHierarchy(i);
            for (Integer hierarchyDepth : hierarchyDepthSet) {
                if (member.getDepth() == hierarchyDepth.intValue()) {
                    FillUtils.fillHeader(matrix, member, i, previousSibling, x, y);
                } else {
                    FillUtils.fillIfIsNull(matrix, x, y);
                }
                y++;
            }
        }
    }

    /**
     * fill the column headers of that total line
     * @param matrix
     * @param colAxisNode
     * @param x
     * @param position
     */
    protected void fillColTotalLine(Matrix matrix, AxisNode colAxisNode, int x, Position position) {
        int y = 0;
        List<Member> memberList = position.getMembers();
        boolean totalDumped = false;
        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            int depth = member.getDepth();
            Axis axis = colAxisNode.getAxis();
            TreeSet<Integer> hierarchyDepthSet = axis.getDisplayedLevelDepthByHierarchy(i);
            if (depth == hierarchyDepthSet.last().intValue()) {
                for (int j = 0; j < hierarchyDepthSet.size(); j++) {
                    FillUtils.fillWithNullOrEmptyIfTotalDumped(matrix, x, y, totalDumped);
                    y++;
                }
            } else {
                for (Integer hierarchyDepth : hierarchyDepthSet) {
                    if (depth == hierarchyDepth.intValue()) {
                        FillUtils.fillWithNullOrEmptyIfTotalDumped(matrix, x, y, totalDumped);
                        if (!totalDumped) {
                            FillUtils.fillTotal(matrix, x, y + 1);
                            totalDumped = true;
                        }
                    } else {
                        FillUtils.fillWithNullOrEmptyIfTotalDumped(matrix, x, y, totalDumped);
                    }
                    y++;
                }
            }
        }
    }

    /**
     * dump the cell values of that column axis node
     * @param cellSet
     * @param colAxisNode
     * @param rowAxisNode
     * @param colPosition
     * @param matrix
     * @param x
     * @param y
     */
    protected void populateColCells(CellSet cellSet, AxisNode colAxisNode, AxisNode rowAxisNode, Position colPosition, Matrix matrix,
            MutableInt x, MutableInt y) {
        List<AxisNode> rowAxisNodeChildren = rowAxisNode.getChildren();
        if (rowAxisNodeChildren != null) {
            for (AxisNode rowAxisNodeChild : rowAxisNodeChildren) {
                populateColCells(cellSet, colAxisNode, rowAxisNodeChild, colPosition, matrix, x, y);
            }
        }
        Position rowPosition = rowAxisNode.getPosition();
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
            cellInfo.setFormattedValue(FillUtils.getValueString(cellValue));
            matrix.set(x.intValue(), y.intValue(), cellInfo);
            y.increment();
        }
    }

}
