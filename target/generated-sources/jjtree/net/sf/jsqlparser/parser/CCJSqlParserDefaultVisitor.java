/* Generated By:JavaCC: Do not edit this line. CCJSqlParserDefaultVisitor.java Version 7.0.12 */
package net.sf.jsqlparser.parser;

public class CCJSqlParserDefaultVisitor implements CCJSqlParserVisitor{
  public Object defaultVisit(SimpleNode node, Object data){
    node.childrenAccept(this, data);
    return data;
  }
  public Object visit(SimpleNode node, Object data){
    return defaultVisit(node, data);
  }
}
/* JavaCC - OriginalChecksum=51639d768ffeb0eed1f50c9c1086f8bc (do not edit this line) */
