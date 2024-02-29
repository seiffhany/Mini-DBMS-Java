package db;

import db.Exceptions.DBAppException;
import db.Utils.Octree;

import java.io.*;
import java.sql.Date;
import java.util.*;

public class Table {
    public String tableName;
    private Page headPage;

    public Table(String tableName) {
        this.tableName = tableName;
        this.headPage = new Page(1, tableName);
        init();
    }

    // CREATES THE PAGE CHAIN FOR THE TABLE STARTING FROM HEAD
    public void init() {
        File tableDirectory = new File(DBApp.tablesDirPath + tableName + "/");

        int pagesFound = 0;
        int currentPageNumber = 1;
        int tableSize = 0;
        if (tableDirectory.listFiles() != null)
            tableSize = tableDirectory.listFiles().length;

        String md = tableDirectory + "/" + this.tableName + " - page ";

        while (pagesFound < tableSize - 1) {
            String path = md + currentPageNumber + ".ser";
            File file = new File(path);
            if (file.exists()) {
                pagesFound++;
                try {
                    FileInputStream fis = new FileInputStream(md + currentPageNumber + ".ser");
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    if (pagesFound == 1) this.headPage = new Page(currentPageNumber, tableName);
                    else if (pagesFound == 2) this.headPage.setNext(new Page(currentPageNumber, tableName));
                    else getLastPage().setNext(new Page(currentPageNumber, tableName));
                    ois.close();
                    fis.close();
                } catch (Exception e) {
                    System.out.println("FILE " + currentPageNumber + " NOT FOUND?");
                }
            }

            currentPageNumber++;
        }
    }

    // SETTERS AND GETTERS //
    public Page getHeadPage() {
        return headPage;
    }

    public Page getLastPage() {
        Page current = this.headPage;
        while (current.getNext() != null) {
            current = current.getNext();
        }
        return current;
    }

    public void addAllToIndex(Octree octree) {
        try {
            Page current = this.headPage;
            while (current != null) {
                Vector<Hashtable<String, Object>> tuples = current.getFromDisk();
                String pk = getClusteringKey();
                for (Hashtable<String, Object> tuple : tuples) {
                    Object value = tuple.get(pk);
                    octree.insertIntoOctree(tuple, value + "/" + this.tableName + " - page " + current.getPageID());
                }
                current = current.getNext();
            }
        } catch (Exception ignored) {
        }
    }

    public String getClusteringKey() {
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(DBApp.metadataPath));
            String line = reader.readLine();

            while (line != null) {
                // refers to table name in the csv file
                String tblName = line.split(",")[0];
                // refers to column name in the csv file
                String colName = line.split(",")[1];

                if (tblName.equals(this.tableName)) {
                    if (line.split(",")[3].toLowerCase().equals("true"))
                        return colName;
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // INSERTIONS INTO TABLE //
    public void insertRecord(Hashtable<String, Object> tuple) throws Exception {
        Object value;
        String clusteringKeyColumn = getClusteringKey();
        ArrayList<String> indexCols = this.getIndex();
        Octree tableTree = null;
        if (indexCols != null) {
            tableTree = getIndexFromDisk(indexCols.get(3));
        }

        boolean didInsert = false;
        Page current = this.headPage;
        while (current != null && tuple != null) {
            value = tuple.get(clusteringKeyColumn);
            if (current.shouldInsert(value)) {
                if (!didInsert && tableTree != null) {
                    tableTree.insertIntoOctree(tuple, value + "/" + this.tableName + " - page " + current.getPageID() + ".ser");
                    didInsert = true;
                }
                tuple = current.insert(tuple, false);
            }
            current = current.getNext();
        }

        if (tableTree != null) {
            saveIndexToDisk(tableTree.name, tableTree);
        }

        if (tuple != null) {
            Page lastPage = getLastPage();
            Page newPage = new Page(lastPage.getPageID() + 1, tableName); // CREATE A PAGE WITH NEW ID
            lastPage.setNext(newPage); // LINK THE CURRENT LAST PAGE TO THE NOW NEW LAST PAGE
            lastPage = newPage; // MOVE LAST PAGE TO THE NEW LAST PAGE
            lastPage.insert(tuple, false); // NOW INSERT RECORD INTO NEWLY CREATED PAGE
        }


    }

    public ArrayList<String> getIndex() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(DBApp.metadataPath));
            String line = br.readLine();
            ArrayList<String> columns = new ArrayList<>();
            String indexName = "";
            while (line != null) {
                String[] data = line.split(",");
                if (data[0].equals(this.tableName) && !data[5].equals("null")) {
                    columns.add(data[1]);
                    indexName = data[4];
                }
                line = br.readLine();
            }
            if (columns.size() == 0) return null;
            columns.add(indexName);
            return columns;
        } catch (Exception e) {
            return null;
        }
    }

    // DELETIONS FROM TABLE //
    public void deleteRecords(Hashtable<String, Object> conditions) {
        // get octree
        Octree tableTree = null;
        ArrayList<String> indexCols = getIndex();
        if (indexCols != null) {
            tableTree = getIndexFromDisk(indexCols.get(3));
        }

        Page currentPage = this.headPage; // START AT HEAD PAGE

        while (currentPage != null) {
            currentPage.removeRecords(conditions, tableTree, this);

            ////// SHIFTING SECTION IN CASE WE NEED IT //////
//             while (numOfRemovals > 0) {
//                 if (currentPage.getNext() != null)  { // IF NOT THE LAST PAGE - SHIFT ALL BENEATH
//                     Vector<Hashtable<String, Object>> newInsertions =
//                             currentPage.getNext().shiftPages(numOfRemovals);
//                     if (newInsertions != null) {
//                         currentPage.batchInsert(newInsertions);
//                     }
//                 }
//
//                 try {
//                     if (currentPage.getFromDisk().size() == 0){
//                         currentPage.deleteFile();
//                         break;
//                     }
//                 } catch (Exception ignored) {}
//
//                 numOfRemovals = currentPage.removeRecords(conditions);
//             }

            currentPage = currentPage.getNext();
        }
    }

    public ArrayList<Hashtable<String, Object>> indexSearch(
            SQLTerm[] arrSQLTerms,
            String[] strarrOperators,
            Octree octree
    ) {

        ArrayList<Hashtable<String, Object>> resultSet = new ArrayList<>();

        boolean allEquality = true;
        for (SQLTerm term : arrSQLTerms) {
            if (!term._strOperator.equals("=")) {
                allEquality = false;
                break;
            }
        }

        String colX = octree.cols.get(0);
        String colY = octree.cols.get(1);
        String colZ = octree.cols.get(2);
        Object xValue = null;
        Object yValue = null;
        Object zValue = null;

        if (allEquality) {
            System.out.println("ALL EQUALITYYY");
            // name >= Bodia AND age < 20 AND id = 30

            for (SQLTerm term : arrSQLTerms) {
                if (term._strColumnName.equals(colX)) {
                    xValue = term._objValue;
                }
                if (term._strColumnName.equals(colY)) {
                    yValue = term._objValue;
                }
                if (term._strColumnName.equals(colZ)) {
                    zValue = term._objValue;
                }
            }
            System.out.println("X: " + xValue);
            System.out.println("Y: " + yValue);
            System.out.println("Z: " + zValue);

            resultSet = octree.searchForPoint(xValue, yValue, zValue, this);
            System.out.println("RESULT SET IN TABLE: " + resultSet);
        } else {
            resultSet = octree.rangedSearchedForPoint(xValue, yValue, zValue, arrSQLTerms, this, new ArrayList<>());
        }


        return resultSet;
    }

    public ArrayList<Hashtable<String, Object>> linearSearch(
            SQLTerm[] arrSQLTerms,
            String[] starrOperators
    ) {
        ArrayList<ArrayList<Hashtable<String, Object>>> resultSets = new ArrayList<>();
        for (int i = 0; i < arrSQLTerms.length; i++) {
            ArrayList<Hashtable<String, Object>> resultSet = new ArrayList<>();
            Page currentPage = this.headPage;
            while (currentPage != null) {
                resultSet.addAll(currentPage.linearSearch(arrSQLTerms[i]));
                currentPage = currentPage.getNext();
            }
            resultSets.add(resultSet);
        }
        if (starrOperators.length == 0) {
            return resultSets.get(0);
        }
        for (String operator : starrOperators) {
            ArrayList<Hashtable<String, Object>> smth = new ArrayList<>();
            if (operator.equals("OR")) {
                smth = orOperation(resultSets.get(0), resultSets.get(1));

            } else if (operator.equals("AND")) {
                smth = andOperation(resultSets.get(0), resultSets.get(1));
            } else if (operator.equals("XOR")) {
                smth = xorOperation(resultSets.get(0), resultSets.get(1));
            }
            resultSets.remove(0);
            resultSets.remove(0);
            resultSets.add(0, smth);
        }
        return resultSets.get(0);
    }

    private ArrayList<Hashtable<String, Object>> xorOperation
            (ArrayList<Hashtable<String, Object>> set1, ArrayList<Hashtable<String, Object>> set2) {
        ArrayList<Hashtable<String, Object>> totalSet = orOperation(set1, set2);
        ArrayList<Hashtable<String, Object>> xorSet = new ArrayList<>();
        for (Hashtable<String, Object> tuple : totalSet) {
            if ((set1.contains(tuple) && !set2.contains(tuple)) || (!set1.contains(tuple) && set2.contains(tuple))) {
                xorSet.add(tuple);
            }
        }
        return xorSet;
    }

    private ArrayList<Hashtable<String, Object>> andOperation
            (ArrayList<Hashtable<String, Object>> set1, ArrayList<Hashtable<String, Object>> set2) {
        ArrayList<Hashtable<String, Object>> andSet = new ArrayList<>();
        for (Hashtable<String, Object> tuple : set1) {
            if (set2.contains(tuple)) {
                andSet.add(tuple);
            }
        }
        return andSet;
    }

    private ArrayList<Hashtable<String, Object>> orOperation
            (ArrayList<Hashtable<String, Object>> set1, ArrayList<Hashtable<String, Object>> set2) {
        Set<Hashtable<String, Object>> set = new HashSet<>();

        set.addAll(set1);
        set.addAll(set2);

        return new ArrayList<>(set);
    }


    // HELPER METHODS //
    // CHECKS IF CURRENT COLUMN VALUE EXISTS IN THE ENTIRE TABLE
    public Object[] doesExistInTable(String colName, Object value) {
        Page currentPage = this.headPage; // START AT HEADER
        while (currentPage != null) {
            if ((boolean) currentPage.doesExistInPage(colName, value)[0])
                return currentPage.doesExistInPage(colName, value);
            currentPage = currentPage.getNext();
        }
        return new Object[]{false, null};
    }

    public void updateRecord(String strClusteringKeyName, String
            strClusteringKeyValue, Hashtable<String, Object> htblColNameValue) throws DBAppException {
        Page currentPage = this.headPage;

        Object clusteringKeyValue = convertStringToObject(strClusteringKeyName, strClusteringKeyValue);
        System.out.println(clusteringKeyValue.getClass().toString() + " : " + clusteringKeyValue);

        try {
            while (!currentPage.updateOneRecord(strClusteringKeyName, clusteringKeyValue, htblColNameValue, this)) {
                currentPage = currentPage.getNext();
                if (currentPage == null) {
                    System.out.println("MSH LA2EEK!");
                }
            }
        } catch (Exception e) {
            throw new DBAppException();
        }




    }

    public Object convertStringToObject(String strColName, String strValue) {
        String strColType = DBApp.getColumnType(this.tableName, strColName);
        assert strColType != null;
        return switch (strColType) {
            case "java.lang.Integer" -> Integer.parseInt(strValue);
            case "java.lang.String" -> strValue;
            case "java.lang.Double" -> Double.parseDouble(strValue);
            case "java.util.Date" -> Date.valueOf(strValue);
            default -> null;
        };
    }

    public String toString() {
        String output = "";
        Page current = this.headPage;

        try {
            if (current.getFromDisk() == null) {
                output = "Table (" + tableName + ") has no pages!";
            }
            while (current != null) {
                output += "\nPage " + current.getPageID() + "\n" + current.toString();
                current = current.getNext();
                output += "==============================\n";
            }
        } catch (Exception e) {
            output = "Table (" + tableName + ") has no pages!";
        }


        return output;
    }

    public void saveIndexToDisk(String indexName, Octree octree) {
        System.out.println("SAVING INDEX TO DISK!");
        try {
            String path = DBApp.indicesDirPath + this.tableName + "/Indices/";
            DBApp.createFile(path);
            FileOutputStream fos = new FileOutputStream(path + indexName + ".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            System.out.println("INPUT TYPE :::: " + (octree.getClass().getName()));
            oos.writeObject(octree);
            oos.flush();
            fos.flush();
            oos.close();
            fos.close();
        } catch (Exception ignored) {
        }
    }

    public Octree getIndexFromDisk(String indexName) {
        try {
            String path = DBApp.indicesDirPath + "/" + this.tableName + "/Indices/";
            FileInputStream fis = new FileInputStream(path + indexName + ".ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            Octree obj = (Octree) ois.readObject();
            ois.close();
            fis.close();
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
