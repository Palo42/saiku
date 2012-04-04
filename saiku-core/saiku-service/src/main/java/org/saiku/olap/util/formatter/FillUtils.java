package org.saiku.olap.util.formatter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.olap4j.Position;
import org.olap4j.metadata.Member;
import org.saiku.olap.dto.resultset.AbstractBaseCell;
import org.saiku.olap.dto.resultset.Matrix;
import org.saiku.olap.dto.resultset.MemberCell;

public class FillUtils {
    
    private static final Logger LOGGER = Logger.getLogger(FillUtils.class);

    public static void fillCell(Matrix matrix, Member member, int x, int y) {
        MemberCell memberCell = new MemberCell();
        String caption = member.getCaption();
        memberCell.setRawValue(caption);
        memberCell.setFormattedValue(caption);
        List<String> memberPath = new ArrayList<String>();
        memberPath.add(member.getUniqueName());
        memberCell.setMemberPath(memberPath);
        matrix.set(x, y, memberCell);
    }

    public static void fillTotal(Matrix matrix, int x, int y) {
        String caption = "total";
        MemberCell totalMemberCell = new MemberCell();
        totalMemberCell.setRawValue(caption);
        totalMemberCell.setFormattedValue(caption);
        matrix.set(x, y, totalMemberCell);
    }

    public static void fillWithNullOrEmptyIfTotalDumped(Matrix matrix, int x, int y, boolean totalDumped) {
        AbstractBaseCell cell = matrix.get(x, y);
        if (cell == null) {
            if (totalDumped) {
                FillUtils.fillWithEmpty(matrix, x, y);
            } else {
                FillUtils.fillWithNull(matrix, x, y);
            }
        }
    }

    /**
     * isFusionCell means the previous header case has the same value, so value must not be repeated
     * @param previousSibling
     * @param i
     * @param member
     * @return
     */
    public static boolean isFusionCell(AxisNode previousSibling, int i, Member member) {
        boolean fusionCell = false;
        if (previousSibling != null) {
            Position position = previousSibling.getPosition();
			List<Member> previousSiblingMemberList = position.getMembers();
            Member previousMember = previousSiblingMemberList.get(i);
            if (previousMember.equals(member)) {
                fusionCell = true;
            }
        }
        return fusionCell;
    }

    public static void fillHeader(Matrix matrix, Member member, int i, AxisNode previousSibling, int x, int y) {
        if (isFusionCell(previousSibling, i, member)) {
            FillUtils.fillWithNull(matrix, x, y);
        } else {
            fillCell(matrix, member, x, y);
        }
    }

    public static void fillWithEmpty(Matrix matrix, int x, int y) {
        MemberCell nullMemberCell = new MemberCell();
        nullMemberCell.setRawValue("");
        nullMemberCell.setFormattedValue("");
        matrix.set(x, y, nullMemberCell);
    }

    public static void fillWithNull(Matrix matrix, int x, int y) {
        MemberCell nullMemberCell = new MemberCell();
        nullMemberCell.setRawValue(null);
        nullMemberCell.setFormattedValue(null);
        matrix.set(x, y, nullMemberCell);
    }

    public static void fillIfIsNull(Matrix matrix, int xIncremented, int y) {
        AbstractBaseCell cell = matrix.get(xIncremented, y);
        if (cell == null) {
            fillWithNull(matrix, xIncremented, y);
        }
    }
    
    public static String getValueString(final String formattedValue) {
        final String[] values = formattedValue.split("\\|"); //$NON-NLS-1$
        if (values.length > 1) {
            return values[1];
        }
        return values[0];
    }

}
