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
 */
public class IRFactory {
    public IRFactory(TokenStream ts) {
        this.ts = ts;
    }

    /**
     * Script (for associating file/url names with toplevel scripts.)
     */
    public Node createScript(Node body)
    {
        Node result = new Node(TokenStream.SCRIPT);
        Node children = body.getFirstChild();
        if (children != null)
            result.addChildrenToBack(children);

        return result;
    }

    /**
     * Leaf
     */
    public Node createLeaf(int nodeType, CodePosition location) {
        return new Node(nodeType, location);
    }

    public Node createLeaf(int nodeType, int nodeOp, CodePosition location) {
        return new Node(nodeType, nodeOp, location);
    }

    public int getLeafType(Node leaf) {
        return leaf.getType();
    }

    /**
     * Statement leaf nodes.
     */

    public Node createSwitch(CodePosition location) {
        return new Node(TokenStream.SWITCH, location);
    }

    public Node createVariables(CodePosition location) {
        return new Node(TokenStream.VAR, location);
    }

    public Node createExprStatement(Object expr, CodePosition location) {
        return new Node(TokenStream.EXPRSTMT, (Node) expr, location);
    }

    /**
     * Name
     */
    public Node createName(String name, CodePosition location) {
        return Node.newString(TokenStream.NAME, name, location);
    }

    /**
     * String (for literals)
     */
    public Node createString(String string, CodePosition location) {
        return Node.newString(string, location);
    }

    /**
     * Number (for literals)
     */
    public Node createIntNumber(double number, CodePosition location) {
        return Node.newIntNumber(number, location);
    }

    public Node createNumber(double number, CodePosition location) {
        return Node.newNumber(number, location);
    }

    /**
     * Catch clause of try/catch/finally
     * @param varName the name of the variable to bind to the exception
     * @param catchCond the condition under which to catch the exception.
     *                  May be null if no condition is given.
     * @param stmts the statements in the catch clause
     * @param lineno the starting line number of the catch clause
     */
    public Node createCatch(Node varName, Node catchCond, Node stmts, CodePosition location) {
        if (catchCond == null) {
            catchCond = new Node(TokenStream.PRIMARY, TokenStream.TRUE, location);
        }
        return new Node(TokenStream.CATCH, varName, catchCond, stmts, location);
    }

    /**
     * Throw
     */
    public Node createThrow(Node expr, CodePosition location) {
        return new Node(TokenStream.THROW, expr, location);
    }

    /**
     * Return
     */
    public Node createReturn(Node expr, CodePosition location) {
        return expr == null
            ? new Node(TokenStream.RETURN, location)
            : new Node(TokenStream.RETURN, expr, location);
    }

    /**
     * Label
     */
    public Node createLabel(Node label, CodePosition location) {
        Node result = new Node(TokenStream.LABEL, location);
        result.addChildToBack(label);
        return result;
    }

    /**
     * Break (possibly labeled)
     */
    public Node createBreak(Node label, CodePosition location) {
        Node result = new Node(TokenStream.BREAK, location);
        if (label == null) {
            return result;
        } else {
            result.addChildToBack(label);
            return result;
        }
    }

    /**
     * Continue (possibly labeled)
     */
    public Node createContinue(Node label, CodePosition location) {
        Node result = new Node(TokenStream.CONTINUE, location);
        if (label == null) {
            return result;
        } else {
            result.addChildToBack(label);
            return result;
        }
    }

    /**
     * debugger
     */
    public Node createDebugger(CodePosition location) {
        Node result = new Node(TokenStream.DEBUGGER, location);
        return result;
    }

    /**
     * Statement block
     * Creates the empty statement block
     * Must make subsequent calls to add statements to the node
     */
    public Node createBlock(CodePosition location) {
        return new Node(TokenStream.BLOCK, location);
    }

    public Node createFunction(Node name, Node args, Node statements, CodePosition location) {
        if (name == null) {
            name = createName("", location);
        }
        return new Node(TokenStream.FUNCTION, name, args, statements, location);
    }

    /**
     * While
     */
    public Node createWhile(Node cond, Node body, CodePosition location) {
        return new Node(TokenStream.WHILE, cond, body, location);
    }

    /**
     * DoWhile
     */
    public Node createDoWhile(Node body, Node cond, CodePosition location) {
        return new Node(TokenStream.DO, body, cond, location);
    }

    /**
     * For
     */
    public Node createFor(Node init, Node test, Node incr, Node body, CodePosition location) {
        return new Node(TokenStream.FOR, init, test, incr, body, location);
    }

    /**
     * For .. In
     *
     */
    public Node createForIn(Node lhs, Node obj, Node body, CodePosition location) {
        return new Node(TokenStream.FOR, lhs, obj, body, location);
    }

    /**
     * Try/Catch/Finally
     */
    public Node createTryCatchFinally(Node tryblock, Node catchblocks, Node finallyblock, CodePosition location) {
        if (finallyblock == null) {
            return new Node(TokenStream.TRY, tryblock, catchblocks, location);
        }
        return new Node(TokenStream.TRY, tryblock, catchblocks, finallyblock, location);
    }

    /**
     * Throw, Return, Label, Break and Continue are defined in ASTFactory.
     */

    /**
     * With
     */
    public Node createWith(Node obj, Node body, CodePosition location) {
        return new Node(TokenStream.WITH, obj, body, location);
    }

    /**
     * Array Literal
     */
    public Node createArrayLiteral(Node obj) {
        return obj;
    }

    /**
     * Object Literals
     */
    public Node createObjectLiteral(Node obj) {
        return obj;
    }

    /**
     * Regular expressions
     */
    public Node createRegExp(String string, String flags, CodePosition location) {
        return flags.length() == 0
               ? new Node(TokenStream.REGEXP,
                          Node.newString(string, location), location)
               : new Node(TokenStream.REGEXP,
                          Node.newString(string, location),
                          Node.newString(flags, location),
                          location);
    }

    /**
     * If statement
     */
    public Node createIf(Node cond, Node ifTrue, Node ifFalse, CodePosition location) {
        if (ifFalse == null)
            return new Node(TokenStream.IF, cond, ifTrue, location);
        return new Node(TokenStream.IF, cond, ifTrue, ifFalse, location);
    }

    public Node createTernary(Node cond, Node ifTrue, Node ifFalse, CodePosition location) {
        return new Node(TokenStream.HOOK, cond, ifTrue, ifFalse, location);
    }

    /**
     * Unary
     */
    public Node createUnary(int nodeType, Node child, CodePosition location) {
        return new Node(nodeType, child, location);
    }

    public Node createUnary(int nodeType, int nodeOp, Node child, CodePosition location) {
        return new Node(nodeType, child, nodeOp, location);
    }

    /**
     * Binary
     */
    public Node createBinary(int nodeType, Node left, Node right, CodePosition location) {
        switch (nodeType) {

          case TokenStream.DOT:
            nodeType = TokenStream.GETPROP;
              right.setType(TokenStream.STRING);
            break;

          case TokenStream.LB:
            // OPT: could optimize to GETPROP iff string can't be a number
            nodeType = TokenStream.GETELEM;
            break;
        }
        return new Node(nodeType, left, right, location);
    }

    public Node createBinary(int nodeType, int nodeOp, Node left, Node right, CodePosition location) {
        if (nodeType == TokenStream.ASSIGN) {
            return createAssignment(nodeOp, left, right, location);
        }
        return new Node(nodeType, left, right, nodeOp, location);
    }

    public Node createAssignment(int nodeOp, Node left, Node right, CodePosition location) {
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
        
        return new Node(TokenStream.ASSIGN, left, right, nodeOp, location);
    }

    private void reportError(String msgResource) {

        String message = Context.getMessage0(msgResource);
        ts.reportSyntaxError(message, null);
    }

    // Only needed to get file/line information. Could create an interface
    // that TokenStream implements if we want to make the connection less
    // direct.
    private TokenStream ts;
}
