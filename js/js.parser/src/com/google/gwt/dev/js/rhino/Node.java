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
 * Roger Lawrence
 * Mike McCabe
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
// Modified by Google

package com.google.gwt.dev.js.rhino;

/**
 * This class implements the root of the intermediate representation.
 */

public class Node implements Cloneable {

    private static class NumberNode extends Node {
        NumberNode(int type, double number, CodePosition position) {
            super(type, position);
            this.number = number;
        }

        @Override
        public double getDouble() {
          return this.number;
        }

        private double number;
    }

    private static class StringNode extends Node {

        StringNode(int type, String str, CodePosition position) {
            super(type, position);
            if (null == str) {
                throw new IllegalArgumentException("StringNode: str is null");
            }
            this.str = str;
        }

        /** returns the string content.
          * @return non null.
          */
        @Override
        public String getString() {
            return this.str;
        }

        /** sets the string content.
          * @param str the new value.  Non null.
          */
        @Override
        public void setString(String str) {
            if (null == str) {
                throw new IllegalArgumentException("StringNode: str is null");
            }
            this.str = str;
        }

        private String str;
    }

    public Node(int nodeType) {
        type = nodeType;
    }

    public Node(int nodeType, Node child, CodePosition position) {
        type = nodeType;
        first = last = child;
        child.next = null;
        this.position = position;
    }

    public Node(int nodeType, Node child, int operation, CodePosition position) {
        type = nodeType;
        first = last = child;
        child.next = null;
        this.operation = operation;
        this.position = position;
    }

    public Node(int nodeType, Node left, Node right, CodePosition position) {
        this(nodeType, left, right, -1, position);
    }

    public Node(int nodeType, Node left, Node right, int operation, CodePosition position) {
        type = nodeType;
        first = left;
        last = right;
        left.next = right;
        right.next = null;
        this.operation = operation;
        this.position = position;
    }

    public Node(int nodeType, Node left, Node mid, Node right, CodePosition position) {
        type = nodeType;
        first = left;
        last = right;
        left.next = mid;
        mid.next = right;
        right.next = null;
        this.position = position;
    }

    public Node(int nodeType, Node left, Node mid, Node mid2, Node right, CodePosition position) {
        type = nodeType;
        first = left;
        last = right;
        left.next = mid;
        mid.next = mid2;
        mid2.next = right;
        right.next = null;
        this.position = position;
    }

    public Node(int nodeType, Node[] children) {
        this.type = nodeType;
        if (children.length != 0) {
            this.first = children[0];
            this.last = children[children.length - 1];

            for (int i = 1; i < children.length; i++) {
                if (null != children[i - 1].next) {
                    // fail early on loops.  implies same node in array twice
                    throw new IllegalArgumentException("duplicate child");
                }
                children[i - 1].next = children[i];
            }
            if (null != this.last.next) {
                // fail early on loops.  implies same node in array twice
                throw new IllegalArgumentException("duplicate child");
            }
        }
    }

    public Node(int nodeType, CodePosition position) {
        type = nodeType;
        this.position = position;
    }

    public Node(int nodeType, int operation, CodePosition position) {
        type = nodeType;
        this.operation = operation;
        this.position = position;
    }

    public static Node newIntNumber(double number, CodePosition position) {
        return new NumberNode(TokenStream.NUMBER_INT, number, position);
    }

    public static Node newNumber(double number, CodePosition position) {
        return new NumberNode(TokenStream.NUMBER, number, position);
    }

    public static Node newString(String str, CodePosition position) {
        return new StringNode(TokenStream.STRING, str, position);
    }

    public static Node newString(int type, String str, CodePosition position) {
        return new StringNode(type, str, position);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Node getFirstChild() {
        return first;
    }

    public Node getNext() {
        return next;
    }

    public int getChildCount() {
        int c = 0;
        for (Node n = first; n != null; n = n.next)
            c++;

        return c;
    }

    public Node getLastSibling() {
        Node n = this;
        while (n.next != null) {
            n = n.next;
        }
        return n;
    }

    public void addChildToBack(Node child) {
        child.next = null;
        if (last == null) {
            first = last = child;
            return;
        }
        last.next = child;
        last = child;
    }

    public void addChildrenToBack(Node children) {
        if (last != null) {
            last.next = children;
        }
        last = children.getLastSibling();
        if (first == null) {
            first = children;
        }
    }

    public static final int
        TARGET_PROP       =  1,
        BREAK_PROP        =  2,
        CONTINUE_PROP     =  3,
        ENUM_PROP         =  4,
        FUNCTION_PROP     =  5,
        TEMP_PROP         =  6,
        LOCAL_PROP        =  7,
        CODEOFFSET_PROP   =  8,
        FIXUPS_PROP       =  9,
        VARS_PROP         = 10,
        USES_PROP         = 11,
        REGEXP_PROP       = 12,
        CASES_PROP        = 13,
        DEFAULT_PROP      = 14,
        CASEARRAY_PROP    = 15,
        SOURCENAME_PROP   = 16,
        SOURCE_PROP       = 17,
        TYPE_PROP         = 18,
        SPECIAL_PROP_PROP = 19,
        LABEL_PROP        = 20,
        FINALLY_PROP      = 21,
        LOCALCOUNT_PROP   = 22,
    /*
        the following properties are defined and manipulated by the
        optimizer -
        TARGETBLOCK_PROP - the block referenced by a branch node
        VARIABLE_PROP - the variable referenced by a BIND or NAME node
        LASTUSE_PROP - that variable node is the last reference before
                        a new def or the end of the block
        ISNUMBER_PROP - this node generates code on Number children and
                        delivers a Number result (as opposed to Objects)
        DIRECTCALL_PROP - this call node should emit code to test the function
                          object against the known class and call diret if it
                          matches.
    */

        TARGETBLOCK_PROP  = 23,
        VARIABLE_PROP     = 24,
        LASTUSE_PROP      = 25,
        ISNUMBER_PROP     = 26,
        DIRECTCALL_PROP   = 27,

        BASE_LINENO_PROP  = 28,
        END_LINENO_PROP   = 29,
        SPECIALCALL_PROP  = 30,
        DEBUGSOURCE_PROP  = 31;

    public static final int    // this value of the ISNUMBER_PROP specifies
        BOTH = 0,               // which of the children are Number types
        LEFT = 1,
        RIGHT = 2;

    private static final String propToString(int propType) {
        switch (propType) {
            case TARGET_PROP:        return "target";
            case BREAK_PROP:         return "break";
            case CONTINUE_PROP:      return "continue";
            case ENUM_PROP:          return "enum";
            case FUNCTION_PROP:      return "function";
            case TEMP_PROP:          return "temp";
            case LOCAL_PROP:         return "local";
            case CODEOFFSET_PROP:    return "codeoffset";
            case FIXUPS_PROP:        return "fixups";
            case VARS_PROP:          return "vars";
            case USES_PROP:          return "uses";
            case REGEXP_PROP:        return "regexp";
            case CASES_PROP:         return "cases";
            case DEFAULT_PROP:       return "default";
            case CASEARRAY_PROP:     return "casearray";
            case SOURCENAME_PROP:    return "sourcename";
            case SOURCE_PROP:        return "source";
            case TYPE_PROP:          return "type";
            case SPECIAL_PROP_PROP:  return "special_prop";
            case LABEL_PROP:         return "label";
            case FINALLY_PROP:       return "finally";
            case LOCALCOUNT_PROP:    return "localcount";

            case TARGETBLOCK_PROP:   return "targetblock";
            case VARIABLE_PROP:      return "variable";
            case LASTUSE_PROP:       return "lastuse";
            case ISNUMBER_PROP:      return "isnumber";
            case DIRECTCALL_PROP:    return "directcall";

            case BASE_LINENO_PROP:   return "base_lineno";
            case END_LINENO_PROP:    return "end_lineno";
            case SPECIALCALL_PROP:   return "specialcall";
            case DEBUGSOURCE_PROP:   return "debugsource";

            default: Context.codeBug();

        }
        return null;
    }

    public int getOperation() {
        return operation;
    }

    public CodePosition getPosition() {
        return position;
    }

    /** Can only be called when <tt>getType() == TokenStream.NUMBER</tt> */
    public double getDouble() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(this + " is not a number node");
    }

    /** Can only be called when node has String context. */
    public String getString() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(this + " is not a string node");
    }

    /** Can only be called when node has String context. */
    public void setString(String s) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(this + " is not a string node");
    }

    @Override
    public String toString() {
        if (Context.printTrees) {
            StringBuffer sb = new StringBuffer(TokenStream.tokenToName(type));
            if (this instanceof StringNode) {
                sb.append(' ');
                sb.append(getString());
            } else {
                switch (type) {
                    case TokenStream.TARGET:
                        sb.append(' ');
                        sb.append(hashCode());
                        break;
                    case TokenStream.NUMBER_INT:
                        sb.append(' ');
                        sb.append((int) getDouble());
                        break;
                    case TokenStream.NUMBER:
                        sb.append(' ');
                        sb.append(getDouble());
                        break;
                    case TokenStream.FUNCTION:
                        sb.append(' ');
                        sb.append(first.getString());
                        break;
                }
            }
            if (operation != -1) {
                sb.append(' ');
                sb.append(operation);
            }
            return sb.toString();
        }
        return null;
    }

    int type;              // type of the node; TokenStream.NAME for example
    Node next;             // next sibling
    private Node first;    // first element of a linked list of children
    private Node last;     // last element of a linked list of children
    private CodePosition position;
    private int operation;
}
