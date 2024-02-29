package db.Utils;

import db.DBApp;
import db.SQLTerm;
import db.Table;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

public class Octree implements Serializable {

    @Serial
    private static final long serialVersionUID = 6755001414983805633L;

    public String name;
    public String tableName;
    public Vector<String> cols;
    public OctNode root;

    public Octree(String name, String tableName, String colX, String colY, String colZ) {
        this.name = name;
        this.tableName = tableName;
        this.cols = new Vector<String>();
        this.cols.add(0, colX);
        this.cols.add(1, colY);
        this.cols.add(2, colZ);

//        populateOctree();
    }

//    public void populateOctree() {
//        // TODO
//    }

    private static int getMaxKeys() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(DBApp.DBAppConfigPath));
            // First line will contain the page size
            reader.readLine();
            String secondLine = reader.readLine();
            return Integer.parseInt(secondLine.split("=")[1]);
        } catch (Exception ignored) {
        }
        return 2; // CHANGED TO 2 FOR NOW -- UNTIL DBApp.config IS PUSHED
    }

    public void insertIntoOctree(Hashtable<String, Object> htblColNameValue, String reference) {
        // extract colX, colY, colZ
//        System.out.println("INSERTING -> " + htblColNameValue);
        System.out.println("INSERTING INTO OCTREE");
        System.out.println("BEFORE: " + this);
        Object colXValue = htblColNameValue.get(cols.get(0));
        Object colYValue = htblColNameValue.get(cols.get(1));
        Object colZValue = htblColNameValue.get(cols.get(2));

        Vector<String> newReference = new Vector<String>();
        newReference.add(reference);

        // then pass them to node.insertIntoNode
        if (root == null) {
            System.out.println("ROOT IS NULL IN INSERTION");
            Hashtable<String, Object> xMinMax = getMinMax(cols.get(0));
            Hashtable<String, Object> yMinMax = getMinMax(cols.get(1));
            Hashtable<String, Object> zMinMax = getMinMax(cols.get(2));
            root = new OctNode(Octree.getMaxKeys(), 0,
                    xMinMax.get("min"),
                    xMinMax.get("max"),
                    yMinMax.get("min"),
                    yMinMax.get("max"),
                    zMinMax.get("min"),
                    zMinMax.get("max")
            );
        }

        root.insertIntoNode(colXValue, colYValue, colZValue, newReference);
        System.out.println("AFTER: " + this);
    }

    public ArrayList<Hashtable<String, Object>> rangedSearchedForPoint(Object xValue,
                                                                       Object yValue,
                                                                       Object zValue,
                                                                       SQLTerm[] arrSQLTerms,
                                                                       Table table,
                                                                       ArrayList<Hashtable<String, Object>> acc
    ) {
        return root.rangedSearchedForPoint(xValue, yValue, zValue, arrSQLTerms, table, acc);
    }

    public Hashtable<String, Object> getMinMax(String col) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(DBApp.metadataPath));
            String line = br.readLine();
            while (line != null) {
                String colName = line.split(",")[1];
                if (colName.equals(col)) {
                    String min = line.split(",")[6];
                    String max = line.split(",")[7];
                    Object minValue = convertStringToObject(col, min);
                    Object maxValue = convertStringToObject(col, max);
                    Hashtable<String, Object> minMax = new Hashtable<String, Object>();
                    minMax.put("min", minValue);
                    minMax.put("max", maxValue);
                    return minMax;
                }
                line = br.readLine();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public Object convertStringToObject(String strColName, String strValue) {
        String strColType = DBApp.getColumnType(this.tableName, strColName);
        return switch (strColType) {
            case "java.lang.Integer" -> Integer.parseInt(strValue);
            case "java.lang.String" -> strValue;
            case "java.lang.Double" -> Double.parseDouble(strValue);
            case "java.util.Date" -> Date.valueOf(strValue);
            default -> null;
        };
    }

    @Override
    public String toString() {
        if (root != null) {
            return root.toString();
        }
        return "Root is null";
    }

    public ArrayList<Hashtable<String, Object>> searchForPoint(Object xValue, Object yValue, Object zValue, Table table) {
        if (root == null) {
            return new ArrayList<Hashtable<String, Object>>();
        }
        return root.searchForPoint(xValue, yValue, zValue, table);

    }

    public void updateOneRecord(String strClusteringKeyName, Object clusteringKeyValue, Hashtable<String, Object> tuple, Table table) {
        System.out.println("UPDATING RECORD IN OCTREE " + tuple);
        if (root == null) return;

        OctPoint newPoint = new OctPoint(tuple.get(cols.get(0)), tuple.get(cols.get(1)), tuple.get(cols.get(2)), new Vector<>());

        root.updateOneRecord(newPoint, clusteringKeyValue, table);


    }

    public void deleteRecord(Hashtable<String, Object> tuple, String clustKey, Table table) {
        // DELETEEE
        if (root == null) return;

        OctPoint newPoint = new OctPoint(tuple.get(cols.get(0)), tuple.get(cols.get(1)), tuple.get(cols.get(2)), new Vector<>());

        root.updateOneRecord(newPoint, tuple.get(clustKey), table);
    }
}
