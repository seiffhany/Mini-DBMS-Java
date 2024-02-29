package db.Utils;

import db.Page;
import db.Table;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

public class OctPoint implements Serializable {

    @Serial
    private static final long serialVersionUID = -7278350413200286758L;

    private final Vector<String> references; // MyTable - Page [number] -
    Object x;
    Object y;
    Object z;

    public OctPoint(Object x, Object y, Object z, Vector<String> references) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.references = references;
    }

    public void addReference(Vector<String> references) {
        this.references.addAll(references);
    }

    public Vector<String> getReferences() {
        return references;
    }
//    public void updatePosition(Object x, Object y, Object z) {
//        this.x = x;
//        this.y = y;
//        this.z = z;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OctPoint)) return false;
        OctPoint octPoint = (OctPoint) o;
        return x.equals(octPoint.x) && y.equals(octPoint.y) && z.equals(octPoint.z);
    }

    @Override
    public String toString() {
        return "\nColumn X Value: " + this.x +
                "\nColumn Y Value: " + this.y +
                "\nColumn Z Value: " + this.z +
                "\nReferences: " + this.references + "\n";
    }

    public ArrayList<Hashtable<String, Object>> getTuples(Table table) {
        ArrayList<Hashtable<String, Object>> results = new ArrayList<>();
        // value + "/" + this.tableName + " - page " + current.getPageID() + ".ser"
        for (String ref : references) {

            String clusteringKeyValue = ref.split("/")[0];
            Object clusteringValue = table.convertStringToObject(table.getClusteringKey(), clusteringKeyValue);
            String path = ref.split("/")[1];
            int pageNum = Integer.parseInt(path.split(" page ")[1]);

            Page currentPage = new Page(pageNum, table.tableName);
            try {
                Vector<Hashtable<String, Object>> tuples = currentPage.getFromDisk();
                String value = table.getClusteringKey();
                for (Hashtable<String, Object> tuple : tuples) {
                    if (Page.compare(clusteringValue, tuple.get(value)) == 0) {
                        results.add(tuple);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    public void deleteReference(Object valueToDelete, Table table) {
                    System.out.println("DELETING ID: " + valueToDelete);
        for (int i = 0; i < references.size(); i++) {
            String ref = references.get(i);
            String clusteringKeyValue = ref.split("/")[0];
            Object clusteringValue = table.convertStringToObject(table.getClusteringKey(), clusteringKeyValue);
            if (Page.compare(clusteringValue, valueToDelete) == 0) {
                references.remove(ref);
                break;
            }
        }
    }
}

