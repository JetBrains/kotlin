/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 * Norris Boyd
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */

package com.google.gwt.dev.js.rhino;

/**
 * This class allows the creation of nodes, and follows the Factory pattern.
 *
 * @see Node
 * @author Mike McCabe
 * @author Norris Boyd
 */
public class IRFactory {

    public IRFactory(TokenStream ts) {
        this.ts = ts;
    }

    /**
     * Script (for associating file/url names with toplevel scripts.)
     */
    public Object createScript(Object body, String sourceName,
                               int baseLineno, int endLineno, Object source)
    {
        Node result = new Node(TokenStream.SCRIPT);
        Node children = ((Node) body).getFirstChild();
        if (children != null)
            result.addChildrenToBack(children);
        
        result.putProp(Node.SOURCENAME_PROP, sourceName);
        result.putIntProp(Node.BASE_LINENO_PROP, baseLineno);
        result.putIntProp(Node.END_LINENO_PROP, endLineno);
        if (source != null)
            result.putProp(Node.SOURCE_PROP, source);
        return result;
    }

    /**
     * Leaf
     */
    public Object createLeaf(int nodeType) {
            return new Node(nodeType);
    }

    public Object createLeaf(int nodeType, int nodeOp) {
        return new Node(nodeType, nodeOp);
    }

    public int getLeafType(Object leaf) {
        Node n = (Node) leaf;
        return n.getType();
    }

    /**
     * Statement leaf nodes.
     */

    public Object createSwitch(int lineno) {
        return new Node(TokenStream.SWITCH, lineno);
    }

    public Object createVariables(int lineno) {
        return new Node(TokenStream.VAR, lineno);
    }

    public Object createExprStatement(Object expr, int lineno) {
        return new Node(TokenStream.EXPRSTMT, (Node) expr, lineno);
    }

    /**
     * Name
     */
    public Object createName(String name) {
        return Node.newString(TokenStream.NAME, name);
    }

    /**
     * String (for literals)
     */
    public Object createString(String string) {
        return Node.newString(string);
    }

    /**
     * Number (for literals)
     */
    public Object createNumber(double number) {
        return Node.newNumber(number);
    }

    /**
     * Catch clause of try/catch/finally
     * @param varName the name of the variable to bind to the exception
     * @param catchCond the condition under which to catch the exception.
     *                  May be null if no condition is given.
     * @param stmts the statements in the catch clause
     * @param lineno the starting line number of the catch clause
     */
    public Object createCatch(String varName, Object catchCond, Object stmts,
                              int lineno)
    {
        if (catchCond == null) {
            catchCond = new Node(TokenStream.PRIMARY, TokenStream.TRUE);
        }
        return new Node(TokenStream.CATCH, (Node)createName(varName),
                               (Node)catchCond, (Node)stmts, lineno);
    }

    /**
     * Throw
     */
    public Object createThrow(Object expr, int lineno) {
        return new Node(TokenStream.THROW, (Node)expr, lineno);
    }

    /**
     * Return
     */
    public Object createReturn(Object expr, int lineno) {
        return expr == null
            ? new Node(TokenStream.RETURN, lineno)
            : new Node(TokenStream.RETURN, (Node)expr, lineno);
    }

    /**
     * Label
     */
    public Object createLabel(String label, int lineno) {
        Node result = new Node(TokenStream.LABEL, lineno);
        Node name = Node.newString(TokenStream.NAME, label);
        result.addChildToBack(name);
        return result;
    }

    /**
     * Break (possibly labeled)
     */
    public Object createBreak(String label, int lineno) {
        Node result = new Node(TokenStream.BREAK, lineno);
        if (label == null) {
            return result;
        } else {
            Node name = Node.newString(TokenStream.NAME, label);
            result.addChildToBack(name);
            return result;
        }
    }

    /**
     * Continue (possibly labeled)
     */
    public Object createContinue(String label, int lineno) {
        Node result = new Node(TokenStream.CONTINUE, lineno);
        if (label == null) {
            return result;
        } else {
            Node name = Node.newString(TokenStream.NAME, label);
            result.addChildToBack(name);
            return result;
        }
    }

    /**
     * debugger
     */
    public Object createDebugger(int lineno) {
        Node result = new Node(TokenStream.DEBUGGER, lineno);
        return result;
    }

    /**
     * Statement block
     * Creates the empty statement block
     * Must make subsequent calls to add statements to the node
     */
    public Object createBlock(int lineno) {
        return new Node(TokenStream.BLOCK, lineno);
    }

    public Object createFunction(String name, Object args, Object statements,
                                 String sourceName, int baseLineno,
                                 int endLineno, Object source,
                                 boolean isExpr)
    {
        Node f = new Node(TokenStream.FUNCTION, 
                          Node.newString(TokenStream.NAME, 
                                         name == null ? "" : name),
                          (Node)args, (Node)statements, baseLineno);

        f.putProp(Node.SOURCENAME_PROP, sourceName);
        f.putIntProp(Node.BASE_LINENO_PROP, baseLineno);
        f.putIntProp(Node.END_LINENO_PROP, endLineno);
        if (source != null)
            f.putProp(Node.SOURCE_PROP, source);

        return f;
    }

    /**
     * Add a child to the back of the given node.  This function
     * breaks the Factory abstraction, but it removes a requirement
     * from implementors of Node.
     */
    public void addChildToBack(Object parent, Object child) {
        ((Node)parent).addChildToBack((Node)child);
    }

    /**
     * While
     */
    public Object createWhile(Object cond, Object body, int lineno) {
        return new Node(TokenStream.WHILE, (Node)cond, (Node)body, lineno);
    }

    /**
     * DoWhile
     */
    public Object createDoWhile(Object body, Object cond, int lineno) {
        return new Node(TokenStream.DO, (Node)body, (Node)cond, lineno);
    }

    /**
     * For
     */
    public Object createFor(Object init, Object test, Object incr,
                            Object body, int lineno)
    {
        return new Node(TokenStream.FOR, (Node)init, (Node)test, (Node)incr,
                        (Node)body);
    }

    /**
     * For .. In
     *
     */
    public Object createForIn(Object lhs, Object obj, Object body, int lineno) {
        return new Node(TokenStream.FOR, (Node)lhs, (Node)obj, (Node)body);
    }

    /**
     * Try/Catch/Finally
     */
    public Object createTryCatchFinally(Object tryblock, Object catchblocks,
                                        Object finallyblock, int lineno)
    {
        if (finallyblock == null) {
            return new Node(TokenStream.TRY, (Node)tryblock, (Node)catchblocks);
        }
        return new Node(TokenStream.TRY, (Node)tryblock,
                        (Node)catchblocks, (Node)finallyblock);
    }

    /**
     * Throw, Return, Label, Break and Continue are defined in ASTFactory.
     */

    /**
     * With
     */
    public Object createWith(Object obj, Object body, int lineno) {
        return new Node(TokenStream.WITH, (Node)obj, (Node)body, lineno);
    }

    /**
     * Array Literal
     */
    public Object createArrayLiteral(Object obj) {
        return obj;
    }

    /**
     * Object Literals
     */
    public Object createObjectLiteral(Object obj) {
        return obj;
    }

    /**
     * Regular expressions
     */
    public Object createRegExp(String string, String flags) {
        return flags.length() == 0
               ? new Node(TokenStream.REGEXP,
                          Node.newString(string))
               : new Node(TokenStream.REGEXP,
                          Node.newString(string),
                          Node.newString(flags));
    }

    /**
     * If statement
     */
    public Object createIf(Object cond, Object ifTrue, Object ifFalse,
                           int lineno)
    {
        if (ifFalse == null)
            return new Node(TokenStream.IF, (Node)cond, (Node)ifTrue);
        return new Node(TokenStream.IF, (Node)cond, (Node)ifTrue, (Node)ifFalse);
    }

    public Object createTernary(Object cond, Object ifTrue, Object ifFalse) {
        return new Node(TokenStream.HOOK,
                        (Node)cond, (Node)ifTrue, (Node)ifFalse);
    }

    /**
     * Unary
     */
    public Object createUnary(int nodeType, Object child) {
        Node childNode = (Node) child;
        return new Node(nodeType, childNode);
    }

    public Object createUnary(int nodeType, int nodeOp, Object child) {
        return new Node(nodeType, (Node)child, nodeOp);
    }

    /**
     * Binary
     */
    public Object createBinary(int nodeType, Object left, Object right) {
        Node temp;
        switch (nodeType) {

          case TokenStream.DOT:
            nodeType = TokenStream.GETPROP;
            Node idNode = (Node) right;
            idNode.setType(TokenStream.STRING);
            String id = idNode.getString();
            if (id.equals("__proto__") || id.equals("__parent__")) {
                Node result = new Node(nodeType, (Node) left);
                result.putProp(Node.SPECIAL_PROP_PROP, id);
                return result;
            }
            break;

          case TokenStream.LB:
            // OPT: could optimize to GETPROP iff string can't be a number
            nodeType = TokenStream.GETELEM;
            break;
        }
        return new Node(nodeType, (Node)left, (Node)right);
    }

    public Object createBinary(int nodeType, int nodeOp, Object left,
                               Object right)
    {
        if (nodeType == TokenStream.ASSIGN) {
            return createAssignment(nodeOp, (Node) left, (Node) right,
                                    null, false);
        }
        return new Node(nodeType, (Node) left, (Node) right, nodeOp);
    }

    public Object createAssignment(int nodeOp, Node left, Node right,
                                   Class convert, boolean postfix)
    {
        int nodeType = left.getType();
        switch (nodeType) {
            case TokenStream.NAME:
            case TokenStream.GETPROP:
            case TokenStream.GETELEM:
                break;
            default:
                // TODO: This should be a ReferenceError--but that's a runtime 
                //  exception. Should we compile an exception into the code?
                reportError("msg.bad.lhs.assign");
        }
        
        return new Node(TokenStream.ASSIGN, left, right, nodeOp);
    }

    private Node createConvert(Class toType, Node expr) {
        if (toType == null)
            return expr;
        Node result = new Node(TokenStream.CONVERT, expr);
        result.putProp(Node.TYPE_PROP, Number.class);
        return result;
    }

    public static boolean hasSideEffects(Node exprTree) {
        switch (exprTree.getType()) {
            case TokenStream.INC:
            case TokenStream.DEC:
            case TokenStream.SETPROP:
            case TokenStream.SETELEM:
            case TokenStream.SETNAME:
            case TokenStream.CALL:
            case TokenStream.NEW:
                return true;
            default:
                Node child = exprTree.getFirstChild();
                while (child != null) {
                    if (hasSideEffects(child))
                        return true;
                    else
                        child = child.getNext();
                }
                break;
        }
        return false;
    }

    private void reportError(String msgResource) {

        String message = Context.getMessage0(msgResource);
        Context.reportError(
            message, ts.getSourceName(), ts.getLineno(),
            ts.getLine(), ts.getOffset());
    }

    // Only needed to get file/line information. Could create an interface
    // that TokenStream implements if we want to make the connection less
    // direct.
    private TokenStream ts;
}
