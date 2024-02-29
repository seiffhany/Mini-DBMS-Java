package db;

import db.Exceptions.DBAppException;
import db.Utils.Octree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DBApp {
    public static final String metadataPath = "src/main/resources/metadata.csv";
    public static final String DBAppConfigPath = "src/main/resources/DBApp.config";
    public static final String tablesDirPath = "src/main/resources/data/";
    public static final String indicesDirPath = "src/main/resources/data/";

    public DBApp() {
        createFile(metadataPath);
        addMetadataHeaders();
        createFile(DBAppConfigPath);
        createFile(tablesDirPath);
        loadDBAppConfig();
    }

    // GET TYPE OF COLUMN USING ITS NAME AND THE TABLE IT CORRESPONDS TO
    public static String getColumnType(String tableName, String columnName) {
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(metadataPath));
            String line = reader.readLine();

            while (line != null) {
                // refers to table name in the csv file
                String tblName = line.split(",")[0];
                // refers to column name in the csv file
                String colName = line.split(",")[1];

                if (tblName.equals(tableName) && colName.equals(columnName)) {
                    return line.split(",")[2]; // returns column type (ex: java.lang.String)
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // CHECK IF A COLUMN IS A PRIMARY/CLUSTERING KEY


    // CHECK IF OBJECT IS EQUAL TO EXPECTED TYPE
    private static boolean checkMatchingTypes(Object obj, String type) {
        if (obj instanceof String) {
            return type.equals("java.lang.String");
        }
        if (obj instanceof Integer) {
            return type.equals("java.lang.Integer");
        }
        if (obj instanceof Double) {
            return type.equals("java.lang.Double");
        }
        if (obj instanceof Date) {
            return type.equals("java.util.Date");
        }
        return false;
    }

    // GET TABLE BY NAME - FASTER THAN 'new Table(tableName)'
    static Table getTable(String tableName) {
        return new Table(tableName);
    }

    // CREATE A FILE WITH ANY GIVEN DIRECTORY
    public static void createFile(String path) {
        String[] parsedString = path.split("/");
        ArrayList<String> directories = new ArrayList<>(Arrays.asList(parsedString));

        String md = "";

        for (String directory : directories) {
            md += directory;
            if (directory.split("\\.").length == 2) break;

            File f = new File(md);
            if (f.mkdir()) System.out.println(md + " | Directory created");
            md += "/";
        }

        try {
            File myObj = new File(md);
            if (myObj.createNewFile()) System.out.println(myObj.getName() + " | File created");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void loadDBAppConfig() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(DBAppConfigPath));
            if (reader.readLine() != null) {
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            FileWriter writer = new FileWriter(DBAppConfigPath);
            writer.write("MaximumRowsCountInTablePage=2\n");
            writer.write("MaximumEntriesinOctreeNode=4\n");
            writer.close();
        } catch (Exception ignored) {
        }
    }

    public static boolean checkIndexOnColumns(String tableName, ArrayList<String> columns) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(DBApp.metadataPath));
            String line = br.readLine();
            while (line != null) {
                if ((columns.contains(line.split(",")[1]) && line.split(",")[0].equals(tableName))
                        && (line.split(",")[5].equals("null"))
                ) {
                    return false;
                }
                line = br.readLine();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void printValues(ArrayList<Hashtable<String, Object>> rows) {
        for (Hashtable<String, Object> row : rows) {
            row.forEach((key, value) -> {
                System.out.println(key + ": " + value);
            });
            System.out.println("=====================================");
        }
    }

    // MAIN METHOD FOR TESTING //
    public static void main(String[] args) {
        /////////////// CREATING A TABLE ///////////////
        String strTableName = "MyTable";
        String strClusteringKeyColumn = "id";
        Hashtable<String, String> htblColNameType = new Hashtable<>();
        Hashtable<String, String> htblColNameMin = new Hashtable<>();
        Hashtable<String, String> htblColNameMax = new Hashtable<>();

        htblColNameType.put("id", "java.lang.Integer");
        htblColNameMin.put("id", "0");
        htblColNameMax.put("id", "10");

        htblColNameType.put("name", "java.lang.String");
        htblColNameMin.put("name", "A");
        htblColNameMax.put("name", "ZZZZZZZZZZZ");

        htblColNameType.put("age", "java.lang.Integer");
        htblColNameMin.put("age", "0");
        htblColNameMax.put("age", "100");

//        htblColNameType.put("grade", "java.lang.Double");
//        htblColNameMin.put("grade", "0");
//        htblColNameMax.put("grade", "100");

        ///////// RECORD TO INSERT INTO TABLE /////////
        Hashtable<String, Object> insertions = new Hashtable<>();
        insertions.put("id", 42);
        insertions.put("name", "Bodia");
        insertions.put("age", 20);
//        insertions.put("grade", 100.0);

        //// SPECIFY CONDITIONS FOR RECORD DELETION ////
        Hashtable<String, Object> conditions = new Hashtable<>();
//        conditions.put("name", "Bodia");
        conditions.put("id", 15);

        //// UPDATE RECORD ////
        Hashtable<String, Object> updateCon = new Hashtable<>();
        updateCon.put("name", "Bodiaa");
        updateCon.put("age", "67");

        SQLTerm[] arrSQLTerms = new SQLTerm[3];
        arrSQLTerms[0] = new SQLTerm();
        arrSQLTerms[0]._strTableName = strTableName;
        arrSQLTerms[0]._strColumnName = "id";
        arrSQLTerms[0]._strOperator = "=";
        arrSQLTerms[0]._objValue = 30;
//
        arrSQLTerms[1] = new SQLTerm();
        arrSQLTerms[1]._strTableName = strTableName;
        arrSQLTerms[1]._strColumnName = "name";
        arrSQLTerms[1]._strOperator = "=";
        arrSQLTerms[1]._objValue = "Bodia";
//
        arrSQLTerms[2] = new SQLTerm();
        arrSQLTerms[2]._strTableName = strTableName;
        arrSQLTerms[2]._strColumnName = "age";
        arrSQLTerms[2]._strOperator = "=";
        arrSQLTerms[2]._objValue = 20;

        DBApp db = new DBApp();
        try {
//            db.createTable(strTableName, strClusteringKeyColumn, htblColNameType, htblColNameMin, htblColNameMax);
//            db.createIndex(strTableName, new String[]{"name", "id", "age"});
//            ArrayList<Hashtable<String, Object>> rows = db.selectFromTable(arrSQLTerms, new String[]{"AND", "AND"});
//            printValues(rows);
//            db.insertIntoTable(strTableName, insertions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try { // PRINT ALL RECORDS IN LAST PAGE OF THE TABLE
//            String[] names = new String[] {"Seif", "Bodia", "Shadyyyyyyyyyy", "Bodia", "Bodia", "Seif"};
//            int counter = 0;
//            for (int i = 25; i < 55; i+=5) {
//                insertions.put("id", i);
//                insertions.put("name", names[counter++]);
//                db.insertIntoTable(strTableName, insertions);
//            }
////
//            insertions.put("id", 45); // 5 10 19 17 16 -> 15 1 3
//            insertions.put("name","Balabizo");
//            insertions.put("age", 20);
//            db.insertIntoTable(strTableName, insertions);
//
////            db.insertIntoTable(strTableName, insertions);
////           db.deleteFromTable(strTableName, conditions);
            Table table = getTable(strTableName);
//            System.out.println(table);
           db.updateTable(strTableName, "40", updateCon);
           System.out.println(table);

            Octree tableTree = table.getIndexFromDisk("nameidageIndex");
            System.out.println(tableTree);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // MILESTONE 1 //
    public void createTable(
            String strTableName,
            String strClusteringKeyColumn,
            Hashtable<String, String> htblColNameType,
            Hashtable<String, String> htblColNameMin,
            Hashtable<String, String> htblColNameMax) throws DBAppException {
        if (!isCreated(strTableName)) // CHECK THAT TABLE DOESN'T ALREADY EXIST
        {
            // ADD TABLE METADATA TO 'metadata.csv'
            try {
                File file = new File(metadataPath);
                FileWriter fw = new FileWriter(file, true);
                PrintWriter pw = new PrintWriter(fw);
                String entry = "";

                Enumeration<String> e = htblColNameType.keys();

                while (e.hasMoreElements()) {
                    entry = strTableName + ",";

                    String colName = e.nextElement();
                    String colType = htblColNameType.get(colName);
                    String colMin = htblColNameMin.get(colName);
                    String colMax = htblColNameMax.get(colName);
                    String clusteringKey = (colName.equals(strClusteringKeyColumn)) ? "True" : "False";

                    entry += colName + "," + colType + "," + clusteringKey
                            + ",null,null," + colMin + "," + colMax;

                    pw.println(entry);
                }
                pw.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        } else System.out.println("Table already exists");
    }

    public void insertIntoTable(
            String strTableName,
            Hashtable<String, Object> htblColNameValue) throws Exception {
        // WE FIRST VALIDATE INSERTED DATA HAS VALID TYPE AND IS NOT DUPLICATING PRIMARY KEY
        // LOOP THROUGH (COLUMNS IN INSERTED RECORD)
        for (String colName : Collections.list(htblColNameValue.keys())) {
            Object value = htblColNameValue.get(colName);
            String colType = getColumnType(strTableName, colName); // EXPECTED TYPE (from csv)

            if (colType == null) throw new DBAppException();

            // CHECK IF CURRENT VALUE HAS THE EXPECTED DATA TYPE
//            System.out.println("VALUE: " + value + " - TYPE: " + colType);
            if (!checkMatchingTypes(value, colType)) {
                System.out.println("Invalid Data Types");
                throw new DBAppException();
            }

            // CHECK IF CURRENT COLUMN IS PRIMARY KEY: IF TRUE THEN MAKE SURE VALUE DOESN'T ALREADY EXIST
            Table table = getTable(strTableName);
            if (Objects.equals(table.getClusteringKey(), colName)) {
                Object[] existState = doesExist(strTableName, colName, value);
                boolean valueExists = (boolean) existState[0];
                if (valueExists) {
                    System.out.println("\nCannot insert duplicate data in a clustering key column (" + colName + ")");
                    System.out.println("(" + colName + " : " + value + ") already exists in page " + existState[1]);
                    throw new DBAppException();
                }
            }
        } // END OF LOOP
        getTable(strTableName).insertRecord(htblColNameValue);
    }

    public void deleteFromTable(
            String strTableName,
            Hashtable<String, Object> htblColNameValue) throws DBAppException {

        getTable(strTableName).deleteRecords(htblColNameValue);
    }

    public void updateTable(String strTableName,
                            String strClusteringKeyValue,
                            Hashtable<String, Object> htblColNameValue)
            throws DBAppException {
        Table table = getTable(strTableName);
        table.updateRecord(table.getClusteringKey(), strClusteringKeyValue, htblColNameValue);
    }

    public void createIndex(
            String strTableName,
            String[] strarrColName
    ) throws DBAppException {
        // transform input String[] to ArrayList<String>
        ArrayList<String> starrColNameArrList = new ArrayList<>(List.of(strarrColName));

        // create index name
        StringBuilder indexName = new StringBuilder();
        for (String s : strarrColName) {
            indexName.append(s);
        }
        indexName.append("Index");

        // check index already created (we check all the possible name combinations)
        ArrayList<String> allPossibleIndexNames = createAllPossibleIndexNames(strarrColName);
        try {
            BufferedReader br = new BufferedReader(new FileReader(DBApp.metadataPath));
            String line = br.readLine();
            while (line != null) {
                if (line.split(",")[0].equals(strTableName) && allPossibleIndexNames.contains(line.split(",")[4])) {
                    System.out.println("Index " + indexName + " already created");
                    return;
                }
                line = br.readLine();
            }
            br.close();
        } catch (Exception ignored) {
        }

        // add index to the metadata.csv file
        try {
            BufferedReader br = new BufferedReader(new FileReader(DBApp.metadataPath));
            String line = br.readLine();
            StringBuilder inputBuffer = new StringBuilder();
            ArrayList<String> linesToReplace = new ArrayList<>();

            while (line != null) {
                if (line.split(",")[0].equals(strTableName) && starrColNameArrList.contains(line.split(",")[1])) {
                    linesToReplace.add(line);
                }
                inputBuffer.append(line);
                inputBuffer.append("\n");
                line = br.readLine();
            }
            br.close();
            String inputStr = inputBuffer.toString();
            for (String row : linesToReplace) {
                String[] sr = row.split(",");
                inputStr = inputStr.replace(
                        row,
                        sr[0] + "," + sr[1] + "," + sr[2] + "," + sr[3] + ","
                                + indexName + ",Octree," + sr[6] + "," + sr[7]
                );
            }

            FileOutputStream fs = new FileOutputStream(DBApp.metadataPath);
            fs.write(inputStr.getBytes(StandardCharsets.UTF_8));
            fs.close();
            // Actual Code For The Index Creation & Stuff
            Octree index = new Octree(indexName.toString(), strTableName, strarrColName[0], strarrColName[1], strarrColName[2]);
            // load the index with the data if data exists

            // save index to disk
            Table table = getTable(strTableName);
            table.addAllToIndex(index);
            table.saveIndexToDisk(indexName.toString(), index);

            System.out.println("Index " + indexName + " was created successfully");

        } catch (Exception ignored) {
        }
    }

    // ArrayList<Hashtable<String, Object>>
    public Iterator<Hashtable<String, Object>> selectFromTable(
            SQLTerm[] arrSQLTerms,
            String[] strarrOperators
    ) throws DBAppException {
        // the result set array list contains all the output tuples
        ArrayList<Hashtable<String, Object>> resultSet = new ArrayList<>();

        // the select statement will always be from a single table so for any of
        // the SQLTerms's we load the table name

        String tableName = arrSQLTerms[0]._strTableName;
        ArrayList<String> columns = new ArrayList<>();

        //  if we are gonna search using the index the arrSQLTerms array has to have size 3
        // and each of its columns need to be a part of the index
        // otherwise we search linearly

        for (int i = 0; i < arrSQLTerms.length; i++) {
            columns.add(arrSQLTerms[i]._strColumnName);
        }

        boolean useIndex = false;
        if ((arrSQLTerms.length == 3) && checkIndexOnColumns(tableName, columns)) {
            boolean allAnds = true;
            for (String op : strarrOperators) {
                if (!op.equals("AND")) {
                    allAnds = false;
                    break;
                }
            }
            if (allAnds) {
                useIndex = true;

            }

        }

        if (useIndex) {
            Table table = getTable(tableName);
            Octree tableTree = null;
            ArrayList<String> indexCols = table.getIndex();
            if (indexCols != null) {
                tableTree = table.getIndexFromDisk(indexCols.get(3));
            }

            // search using the index
            resultSet = indexSearch(arrSQLTerms, strarrOperators, tableTree);

            System.out.println("Searching Using The Index");
        } else {
            // search linearly
            resultSet = linearSearch(arrSQLTerms, strarrOperators);
            System.out.println("Searching Linearly");
        }

        System.out.println("RESULT SET IN DBAPP: " + resultSet);
        return resultSet.iterator();
    }

    // HELPER METHODS //
    public ArrayList<Hashtable<String, Object>> linearSearch(
            SQLTerm[] arrSQLTerms,
            String[] starrOperators
    ) {
        String tableName = arrSQLTerms[0]._strTableName;
        Table table = getTable(tableName);

        return table.linearSearch(arrSQLTerms, starrOperators);
    }

    public ArrayList<Hashtable<String, Object>> indexSearch(
            SQLTerm[] arrSQLTerms,
            String[] starrOperators,
            Octree octree
    ) {
        String tableName = arrSQLTerms[0]._strTableName;
        Table table = getTable(tableName);

        return table.indexSearch(arrSQLTerms, starrOperators, octree);
    }

    private boolean isCreated(String strTableName) {
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(metadataPath));
            String line = reader.readLine();

            while (line != null) {
                if (line.split(",")[0].equals(strTableName)) return true;
                line = reader.readLine();
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void addMetadataHeaders() {
        // ADD TABLE METADATA HEADERS TO 'metadata.csv'
        try {
            BufferedReader br = new BufferedReader(new FileReader(metadataPath));
            if (br.readLine() != null) return;

            File file = new File(metadataPath);
            FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);
            String entry = "";

            entry = "TableName, ColumnName, ColumnType, ClusteringKey, IndexName, IndexType, min, max";
            pw.println(entry);
            pw.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // CHECK IF A COLUMN VALUE ALREADY EXISTS
    private Object[] doesExist(String tableName, String columnName, Object value) {
        return getTable(tableName).doesExistInTable(columnName, value);
    }

    // Create All Possible Index Names
    public ArrayList<String> createAllPossibleIndexNames(String[] starrColName) {
        ArrayList<String> allPossibleNames = new ArrayList<>();
        allPossibleNames.add(starrColName[0] + starrColName[1] + starrColName[2] + "Index");
        allPossibleNames.add(starrColName[0] + starrColName[2] + starrColName[1] + "Index");
        allPossibleNames.add(starrColName[1] + starrColName[0] + starrColName[2] + "Index");
        allPossibleNames.add(starrColName[1] + starrColName[2] + starrColName[0] + "Index");
        allPossibleNames.add(starrColName[2] + starrColName[0] + starrColName[1] + "Index");
        allPossibleNames.add(starrColName[2] + starrColName[1] + starrColName[0] + "Index");
        return allPossibleNames;
    }

    public void init() {
    }
}
