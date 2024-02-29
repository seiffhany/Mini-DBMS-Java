package db.Utils;

import db.Page;
import db.SQLTerm;
import db.Table;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

public class OctNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 4044094362468306921L;

    int maxKeys;
    int level;
    Object xMin, xMax, yMin, yMax, zMin, zMax;

    Vector<OctPoint> points;
    OctNode[] children;
    boolean isLeaf;

    public OctNode(int maxKeys, int level, Object xMin, Object xMax, Object yMin, Object yMax, Object zMin, Object zMax) {
        this.maxKeys = maxKeys;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
        this.level = level;
        this.points = new Vector<>();

        // It has no children initially
        this.children = null;
        this.isLeaf = true;
    }

    public static String printMiddleString(String S, String T) {
        String tempS = S.toLowerCase();
        String tempT = T.toLowerCase();

        if (S.length() != T.length()) {
            int start;
            int max;
            if (S.length() > T.length()) {
                start = T.length();
                max = S.length();
                for (int i = start; i < max; i++) {
                    tempT += tempS.charAt(i);
                }
            } else {
                start = S.length();
                max = T.length();
                for (int i = start; i < max; i++) {
                    tempS += tempT.charAt(i);
                }
            }
        }
        int N = tempS.length();

        // Stores the base 26 digits after addition
        int[] a1 = new int[N + 1];
        for (int i = 0; i < N; i++) {
            a1[i + 1] = (int) tempS.charAt(i) - 97
                    + (int) tempT.charAt(i) - 97;
        }

        // Iterate from right to left
        // and add carry to next position
        for (int i = N; i >= 1; i--) {
            a1[i - 1] += (int) a1[i] / 26;
            a1[i] %= 26;
        }

        // Reduce the number to find the middle
        // string by dividing each position by 2
        for (int i = 0; i <= N; i++) {

            // If current value is odd,
            // carry 26 to the next index value
            if ((a1[i] & 1) != 0) {

                if (i + 1 <= N) {
                    a1[i + 1] += 26;
                }
            }

            a1[i] = (int) a1[i] / 2;
        }

        String output = "";
        for (int i = 1; i <= N; i++) {
            output += (char) (a1[i] + 97);
        }

        return output;
    }

    public void splitNode() {
        this.isLeaf = false;

        System.out.println("CHECKPOINT 10");
        Object xMidValue = getMidValue(xMin, xMax);
        System.out.println("CHECKPOINT 11");
        Object yMidValue = getMidValue(yMin, yMax);
        System.out.println("CHECKPOINT 12");
        Object zMidValue = getMidValue(zMin, zMax);
        System.out.println("CHECKPOINT 13");

        children = new OctNode[8];
        this.children[0] = new OctNode(this.maxKeys, this.level + 1, xMin, xMidValue, yMin, yMidValue, zMin, zMidValue);
        this.children[1] = new OctNode(this.maxKeys, this.level + 1, xMin, xMidValue, yMin, yMidValue, zMidValue, zMax);
        this.children[2] = new OctNode(this.maxKeys, this.level + 1, xMin, xMidValue, yMidValue, yMax, zMin, zMidValue);
        this.children[3] = new OctNode(this.maxKeys, this.level + 1, xMin, xMidValue, yMidValue, yMax, zMidValue, zMax);
        this.children[4] = new OctNode(this.maxKeys, this.level + 1, xMidValue, xMax, yMin, yMidValue, zMin, zMidValue);
        this.children[5] = new OctNode(this.maxKeys, this.level + 1, xMidValue, xMax, yMin, yMidValue, zMidValue, zMax);
        this.children[6] = new OctNode(this.maxKeys, this.level + 1, xMidValue, xMax, yMidValue, yMax, zMin, zMidValue);
        this.children[7] = new OctNode(this.maxKeys, this.level + 1, xMidValue, xMax, yMidValue, yMax, zMidValue, zMax);
    }

    public Object getMidValue(Object min, Object max) {
        if (min instanceof java.lang.Integer) {
            return ((Integer) min + (Integer) max) / 2;
        } else if (min instanceof java.lang.String) {
            System.out.println("INSTANCE OF STRING");
            return printMiddleString((String) min, (String) max);
        } else if (min instanceof java.lang.Double) {
            return ((Double) min + (Double) max) / 2;
        } else { // Date
            long minDate = ((Date) min).getTime();
            long maxDate = ((Date) max).getTime();
            return new Date((minDate + maxDate) / 2);
        }
    }

    public void insertIntoNode(Object x, Object y, Object z, Vector<String> references) {
        OctPoint newPoint = new OctPoint(x, y, z, references);
        System.out.println("CHECKPOINT 1");
        System.out.println("MAX KEYS: " + maxKeys);
        System.out.println("NEW POINT :: " + newPoint);

        if (isLeaf) {
            if (points.size() >= maxKeys) {
                // Node is full
                this.splitNode();
                for (OctPoint point : points) {
                    this.insertIntoNode(point.x, point.y, point.z, point.getReferences());
                }
                this.insertIntoNode(x, y, z, references);
                this.points = new Vector<>();

            } else { // Node is not full

                // Check if it already exists in this Node
                for (OctPoint point : points) {
                    if (point.equals(newPoint)) { // Point is already in the node
                        point.addReference(references);
                        return;
                    }
                }
                // New point in the node
                System.out.println("ADDING NEW POINT!!!!!");
                points.add(newPoint);
            }
        } else {
            // Search for the correct child from their ranges to insert
            boolean isInRange = false;
            for (OctNode node : children) {
                if (node.isInRange(newPoint)) {
                    //found correct node to insert in
                    isInRange = true;
                    node.insertIntoNode(x, y, z, references);
                }
            }
            if (isInRange) {
                System.out.println("IS IN RANGE");
            } else {
                System.out.println("NOT IN RANGE");
            }
        }
    }


    public boolean isInRange(OctPoint point) {
        return Page.compare(point.x, xMin) >= 0 && Page.compare(point.x, xMax) < 0 &&
                Page.compare(point.y, yMin) >= 0 && Page.compare(point.y, yMax) < 0 &&
                Page.compare(point.z, zMin) >= 0 && Page.compare(point.z, zMax) < 0;
    }

    @Override
    public String toString() {
        if (isLeaf) {
            StringBuilder output = new StringBuilder("\n======= Leaf Node " + this.level + " =======\n");
            output.append("Col X range: [" + xMin + ", " + xMax + "]\n");
            output.append("Col Y range: [" + yMin + ", " + yMax + "]\n");
            output.append("Col Z range: [" + zMin + ", " + zMax + "]\n");
            for (OctPoint point : this.points) {
                output.append("\n").append(point);
            }
            output.append("\n==========================\n");
            return output.toString();
        }
        StringBuilder output = new StringBuilder("\n======= Non Leaf Node " + this.level + " =======");
        for (OctNode node : this.children)
            output.append(node);
        output.append("\n==============================\n");
        return output.toString();
    }

    public ArrayList<Hashtable<String, Object>> searchForPoint(Object xValue, Object yValue, Object zValue, Table table) {

        OctPoint newPoint = new OctPoint(xValue, yValue, zValue, new Vector<>());

        if (isLeaf) {
            // search in our points
            for (OctPoint point : points) {
                if (point.equals(newPoint)) {
                    return point.getTuples(table);
                }
            }
        } else {
            for (OctNode node : children) {
                if (node.isInRange(newPoint)) {
                    System.out.println(node);
                    //found correct node to search in
                    return node.searchForPoint(xValue, yValue, zValue, table);
                }
            }
        }
        return new ArrayList<>();

    }

    public ArrayList<Hashtable<String, Object>> rangedSearchedForPoint(
            Object xValue,
            Object yValue,
            Object zValue,
            SQLTerm[] arrSQLTerms,
            Table table,
            ArrayList<Hashtable<String, Object>> acc
    ) {

        ArrayList<Hashtable<String, Object>> result = new ArrayList<>();

        if (isLeaf) {
            for (OctPoint point : points) {
                for (String ref : point.getReferences()) {
                    int pageNum = Integer.parseInt(ref.split(" page ")[1]);
                    Page currentPage = new Page(pageNum, table.tableName);
                    try {
                        Vector<Hashtable<String, Object>> currentTuples = currentPage.getFromDisk();
                        for (Hashtable<String, Object> tuple : currentTuples) {
                            String firstOperator = arrSQLTerms[0]._strOperator;
                            String firstColumn = arrSQLTerms[0]._strColumnName;
                            Object firstValue = arrSQLTerms[0]._objValue;
                            String secondOperator = arrSQLTerms[1]._strOperator;
                            String secondColumn = arrSQLTerms[1]._strColumnName;
                            Object secondValue = arrSQLTerms[1]._objValue;
                            String thirdOperator = arrSQLTerms[2]._strOperator;
                            String thirdColumn = arrSQLTerms[2]._strColumnName;
                            Object thirdValue = arrSQLTerms[2]._objValue;

                            if (updateTuples(firstOperator, tuple, firstColumn, firstValue) &&
                                    updateTuples(secondOperator, tuple, secondColumn, secondValue) &&
                                    updateTuples(thirdOperator, tuple, thirdColumn, thirdValue)) {
                                acc.add(tuple);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (this.isInRange(new OctPoint(xValue, yValue, zValue, new Vector<>()))) {
                for (OctNode node : children) {
                    acc.addAll(node.rangedSearchedForPoint(xValue, yValue, zValue, arrSQLTerms, table, acc));
                }
            }
        }

        return acc;
    }

    public boolean updateTuples(
            String operator,
            Hashtable<String, Object> tuple,
            String colName,
            Object colValue
    ) {
        switch (operator) {
            case "=":
                return Page.compare(tuple.get(colName), colValue) == 0;
            case ">":
                return Page.compare(tuple.get(colName), colValue) > 0;
            case ">=":
                return Page.compare(tuple.get(colName), colValue) >= 0;
            case "<":
                return Page.compare(tuple.get(colName), colValue) < 0;
            case "<=":
                return Page.compare(tuple.get(colName), colValue) <= 0;
            default:
                return Page.compare(tuple.get(colName), colValue) != 0;
        }
    }

    public void updateOneRecord(OctPoint myPoint, Object clusteringKeyValue, Table table) {
        if (isLeaf) {
            // search in our points
            for (OctPoint point : points) {
                if (point.equals(myPoint)) {
                    System.out.println("FOUND POINT EQUAL TO ");
                    // FOUND THE POINT
                    point.deleteReference(clusteringKeyValue, table);

                }
            }
        } else {
            for (OctNode node : children) {
                if (node.isInRange(myPoint)) {
                    System.out.println(node);
                    //found correct node to search in
                    node.updateOneRecord(myPoint, clusteringKeyValue, table);
                }
            }
        }

    }
}
