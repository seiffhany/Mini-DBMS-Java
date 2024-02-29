package db;

import db.Exceptions.DBAppException;
import db.Utils.Octree;

import java.io.*;
import java.util.*;

public class Page {
    public static int pageLimit = getPageLimit(); // N

    private final int pageID;
    private final String path;
    Hashtable<String, Object> minMax;
    private String tableName;
    private Page next;

    public Page(int pageID, String tableName) {
        this.pageID = pageID;
        this.tableName = tableName;
        this.path = DBApp.tablesDirPath + tableName + "/" + tableName + " - page " + this.pageID + ".ser";
    }

    // Get Page Limit From DBApp.config file
    private static int getPageLimit() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(DBApp.DBAppConfigPath));
            // First line will contain the page size
            String firstLine = reader.readLine();
            return Integer.parseInt(firstLine.split("=")[1]);
        } catch (Exception ignored) {
        }
        return 2; // CHANGED TO 2 FOR NOW -- UNTIL DBApp.config IS PUSHED
    }

    public static int compare(Object value, Object fromRecord) {
        if (value instanceof String && fromRecord instanceof String)
            return (((String) value).toLowerCase()).compareTo((((String) fromRecord).toLowerCase()));
        if (value instanceof Integer && fromRecord instanceof Integer)
            return ((Integer) value).compareTo((Integer) fromRecord);
        if (value instanceof Double && fromRecord instanceof Double)
            return ((Double) value).compareTo((Double) fromRecord);
        if (value instanceof Date && fromRecord instanceof Date)
            return ((Date) value).compareTo((Date) fromRecord);

        return 0;
    }

    public static String printSpaces(Object value) {
        String output = "";
        int num = 10 - value.toString().length();
        for (int i = 0; i < num; i++) output += " ";
        return output;
    }

    // SETTERS AND GETTERS //
    public int getPageID() {
        return this.pageID;
    }

    public Page getNext() {
        return this.next;
    }

    public void setNext(Page page) {
        this.next = page;
    }

    // SAVING TO AND READING FROM DISK //
    public void saveToDisk(Vector<Hashtable<String, Object>> tuple) {
        try {
            DBApp.createFile(this.path);
            FileOutputStream fos = new FileOutputStream(this.path);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(tuple);
            oos.close();
            fos.close();
        } catch (Exception ignored) {
        }
    }

    public Vector<Hashtable<String, Object>> getFromDisk() throws Exception {
        try {
            FileInputStream fis = new FileInputStream(this.path);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Vector<Hashtable<String, Object>> obj = (Vector<Hashtable<String, Object>>) ois.readObject();
            ois.close();
            fis.close();
            return obj;
        } catch (Exception e) {
//            throw new PageDoesNotExistException("Page " + this.pageID + " does not exist. Insert any data.");
            throw new Exceptions.PageDoesNotExistException("");
        }
    }

    // INSERTING RECORDS //
    public Hashtable<String, Object> insert(Hashtable<String, Object> newTuple, boolean isDeleting) {
        Vector<Hashtable<String, Object>> page = new Vector<>();
        Table table = DBApp.getTable(this.tableName);
        String clusteringColumn = table.getClusteringKey();

        // IN CASE OF FIRST INSERTION, GETTING WILL THROW AN EXCEPTION
        try {
            page = getFromDisk();
        } catch (Exception ignored) {
        }

        // CHECKING IF PAGE HAS ROOM FOR INSERTION

        // SORTS IF THERE IS A CLUSTERING COLUMN
        if (clusteringColumn != null && page.size() > 0) {
            int midPosition;
            int lowPosition = 0;
            int highPosition = page.size() - 1;
            Object currentClusteringValue;
            Object newClusteringValue = newTuple.get(clusteringColumn);

            while (highPosition > lowPosition + 1) {
                midPosition = (lowPosition + highPosition) / 2;
                currentClusteringValue = page.get(midPosition).get(clusteringColumn);

                if (compare(newClusteringValue, currentClusteringValue) > 0)
                    lowPosition = midPosition + 1;
                else highPosition = midPosition;
            }

            Object lowValue = page.get(lowPosition).get(clusteringColumn);
            Object highValue = page.get(highPosition).get(clusteringColumn);

            if (compare(newClusteringValue, lowValue) < 0)  // LOWER THAN MIN
                page.add(lowPosition, newTuple);
            else if (compare(newClusteringValue, highValue) > 0)  // HIGHER THEN MAX
                page.add(highPosition + 1, newTuple);
            else page.add(lowPosition + 1, newTuple); // BETWEEN MIN AND MAX

        } //In case of first insertion or no sorting needed
        else page.add(newTuple);

        saveToDisk(page);

        try {
            Vector<Hashtable<String, Object>> currentRecords = this.getFromDisk();
            if (currentRecords.size() > Page.pageLimit && !isDeleting) {
                Hashtable<String, Object> overflow = currentRecords.remove(currentRecords.size() - 1);
                saveToDisk(currentRecords);
                return overflow; // overflow
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // no overflow
    }

    // REMOVING RECORDS //
    public void removeRecords(Hashtable<String, Object> conditions, Octree octree, Table table) {
        String clustKey = table.getClusteringKey();
        Vector<Hashtable<String, Object>> allPageRecords;
        try {
            allPageRecords = getFromDisk();
            ArrayList<Hashtable<String, Object>> recordsToRemove = new ArrayList<>();
            boolean shouldRemove;

            // LOOP THROUGH (RECORDS IN PAGE) - CHECK IF RECORD SATISFIES CONDITIONS FOR DELETION
            for (Hashtable<String, Object> record : allPageRecords) {
                shouldRemove = true; // RESETS TO TRUE FOR EACH NEW RECORD

                // LOOP THROUGH (COLUMNS IN RECORD)
                for (String column : Collections.list(record.keys())) {
                    // CHECK THAT THE CURRENT COLUMN IS INCLUDED IN CONDITION
                    // (ex: "id" == 12 [then "name" will return null since it's not in condition])
                    if (conditions.get(column) != null)
                        shouldRemove &= (record.get(column)).equals(conditions.get(column));
                    // IF COLUMN INCLUDED, 'AND' WITH CURRENT REMOVE STATE
                }

                if (shouldRemove) {
                    recordsToRemove.add(record);

                    // remove from octree as well
                    if (octree != null) {
                        octree.deleteRecord(record, clustKey, table);
                    }

                }

            } // END OF LOOP

            allPageRecords.removeAll(recordsToRemove);
            if (allPageRecords.size() == 0) this.deleteSelf();
            else saveToDisk(allPageRecords);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<Hashtable<String, Object>> linearSearch(
            SQLTerm sqlTerm
    ) {
        ArrayList<Hashtable<String, Object>> resultSet = new ArrayList<>();
        Page page = this;
        try {
            Vector<Hashtable<String, Object>> records = page.getFromDisk();
            String columnName = sqlTerm._strColumnName;
            Object columnValue = sqlTerm._objValue;
            String operator = sqlTerm._strOperator;
            if (operator.equals("=")) {
                for (Hashtable<String, Object> record : records) {
                    if (record.get(columnName).equals(columnValue)) {
                        resultSet.add(record);
                    }
                }
            } else if (operator.equals(">")) {
                for (Hashtable<String, Object> record : records) {
                    if ((record.get(columnName) instanceof Integer) && ((Integer) record.get(columnName)).compareTo((Integer) columnValue) > 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof String) && ((String) record.get(columnName)).compareTo((String) columnValue) > 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Double) && ((Double) record.get(columnName)).compareTo((Double) columnValue) > 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Date) && ((Date) record.get(columnName)).compareTo((Date) columnValue) > 0) {
                        resultSet.add(record);
                    }
                }
            } else if (operator.equals("<")) {
                for (Hashtable<String, Object> record : records) {
                    if ((record.get(columnName) instanceof Integer) && ((Integer) record.get(columnName)).compareTo((Integer) columnValue) < 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof String) && ((String) record.get(columnName)).compareTo((String) columnValue) < 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Double) && ((Double) record.get(columnName)).compareTo((Double) columnValue) < 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Date) && ((Date) record.get(columnName)).compareTo((Date) columnValue) < 0) {
                        resultSet.add(record);
                    }
                }
            } else if (operator.equals(">=")) {
                for (Hashtable<String, Object> record : records) {
                    if ((record.get(columnName) instanceof Integer) && ((Integer) record.get(columnName)).compareTo((Integer) columnValue) >= 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof String) && ((String) record.get(columnName)).compareTo((String) columnValue) >= 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Double) && ((Double) record.get(columnName)).compareTo((Double) columnValue) >= 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Date) && ((Date) record.get(columnName)).compareTo((Date) columnValue) >= 0) {
                        resultSet.add(record);
                    }
                }
            } else if (operator.equals("<=")) {
                for (Hashtable<String, Object> record : records) {
                    if ((record.get(columnName) instanceof Integer) && ((Integer) record.get(columnName)).compareTo((Integer) columnValue) <= 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof String) && ((String) record.get(columnName)).compareTo((String) columnValue) <= 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Double) && ((Double) record.get(columnName)).compareTo((Double) columnValue) <= 0) {
                        resultSet.add(record);
                    } else if ((record.get(columnName) instanceof Date) && ((Date) record.get(columnName)).compareTo((Date) columnValue) <= 0) {
                        resultSet.add(record);
                    }
                }
            } else if (operator.equals("!=")) {
                for (Hashtable<String, Object> record : records) {
                    if (!record.get(columnName).equals(columnValue)) {
                        resultSet.add(record);
                    }
                }
            }

        } catch (Exception e) {
        }
        return resultSet;
    }

    ///// NOT USED FOR NOW /////
    public Vector<Hashtable<String, Object>> shiftPages(int numOfRemovals) {
        Page current = this;

        try {
            Vector<Hashtable<String, Object>> currentTuples = current.getFromDisk();
            Vector<Hashtable<String, Object>> removedTuples = new Vector<>();
            if (current.getNext() != null) { // NOT LAST PAGE
                for (int i = 0; i < numOfRemovals; i++)
                    removedTuples.add(currentTuples.remove(0));
                current.saveToDisk(currentTuples);

                Vector<Hashtable<String, Object>> newInsertions = current.getNext().shiftPages(numOfRemovals);
                if (newInsertions != null) {
                    current.batchInsert(newInsertions);
                } else {
                    current.setNext(null);
                }
            } else { // NOW AT LAST PAGE [BASE CASE OF THE RECURSION]
                int newNum = Math.min(numOfRemovals, currentTuples.size());
                for (int i = 0; i < newNum; i++)
                    removedTuples.add(currentTuples.remove(0));
                current.saveToDisk(currentTuples);
            }
//            else saveToDisk(currentTuples);

            return removedTuples;
        } catch (Exception ignored) {
        }
        return null;
    }

    public void deleteSelf() {
        File currentFile = new File(this.path);
        if (currentFile.delete()) System.out.println("Page " + this.pageID + " Has Been Deleted!");
        else System.out.println("File Has Not Been Deleted");
    }

    public void batchInsert(Vector<Hashtable<String, Object>> newTuples) {
        for (Hashtable<String, Object> tuple : newTuples)
            this.insert(tuple, true);
    }

    // HELPER METHODS //
    // CHECK IF A RECORD WITH A SPECIFIED COLUMN VALUE EXISTS
    public Object[] doesExistInPage(String columnName, Object value) {
        try {
            Vector<Hashtable<String, Object>> tuples = getFromDisk();

            for (Hashtable<String, Object> tuple : tuples) {
                if (tuple.get(columnName).equals(value)) return new Object[]{true, this.pageID};
            }

        } catch (Exception ignored) {
        }

        return new Object[]{false, this.pageID};
    }

    public boolean shouldInsert(Object value) {
        if (this.next == null) return true;
        try {
            Vector<Hashtable<String, Object>> records = this.getFromDisk();
            for (Hashtable<String, Object> record : records) {
                for (String colName : Collections.list(record.keys()))
                    if (compare(value, record.get(colName)) < 0)
                        return true;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return true;
        }

        return true;
    }

    public boolean updateOneRecord(String strClusteringKeyName,
                                   Object clusteringKeyValue,
                                   Hashtable<String, Object> htblColNameValue,Table table) throws DBAppException {
        try {
            Vector<Hashtable<String, Object>> records = this.getFromDisk();
            // VALIDATE RECORD FIRST
            for (String col : htblColNameValue.keySet())
                if (records.get(0).get(col) == null) throw new DBAppException();
            for (Hashtable<String, Object> record : records) {
                if (record.get(strClusteringKeyName).equals(clusteringKeyValue)) {
                    System.out.println("FOUND YA!");
                    // RECORD TO UPDATE IN OCTREEE


                    updateRecordInOctree(table, clusteringKeyValue.toString(), htblColNameValue);
                    // PUT ALL ;)
                    record.putAll(htblColNameValue);
                    this.saveToDisk(records);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new DBAppException();
        }
        return false;
    }

    public void updateRecordInOctree(Table table,String strClusteringKeyValue,Hashtable<String, Object> htblColNameValue) {
        // UPDATE TUPLE IF INDEX
        Octree tableTree = null;
        ArrayList<String> indexCols = table.getIndex();
        String clustKey = table.getClusteringKey();

        if (indexCols == null) return;

        tableTree = table.getIndexFromDisk(indexCols.get(3));

        Object clustValue = table.convertStringToObject(clustKey,strClusteringKeyValue);
        // create hastable for tuple
        Hashtable<String, Object> tupleToUpdate = new Hashtable<>();
        tupleToUpdate.put(clustKey, clustValue);
        tupleToUpdate.putAll(htblColNameValue);


        tableTree.updateOneRecord(clustKey, clustValue, tupleToUpdate, table);

        // now its reference is deleted from the Octree
        // now we add the new one

        // value + "/" + this.tableName + " - page " + current.getPageID() + ".ser"

        // INSERT INTO OCTREE
        Object value;
        boolean didInsert = false;
        Page current = table.getHeadPage();
        while (current != null && tupleToUpdate != null) {
            if (current.shouldInsert(clustValue)) {
                if (!didInsert && tableTree != null) {
                    tableTree.insertIntoOctree(tupleToUpdate, clustValue + "/" + this.tableName + " - page " + current.getPageID() + ".ser");
                    didInsert = true;
                    break;
                }
                //                 tupleToUpdate = current.insert(tupleToUpdate, false);
            }
            current = current.getNext();
        }
    }

    public String toString() {
        String output = "";

        try {
            Vector<Hashtable<String, Object>> records = getFromDisk();

            for (Hashtable<String, Object> record : records) {
                for (String colName : Collections.list(record.keys()))
                    output += colName + printSpaces(colName) + "|";
                break;
            }
            output += "\n";

            for (Hashtable<String, Object> record : records) {
                for (String colName : Collections.list(record.keys())) {
                    Object value = record.get(colName);
                    output += value + printSpaces(value) + "|";
                }
                output += "\n";
            }
        } catch (Exception ignored) {
        }

        return output + "\n";
    }
}
