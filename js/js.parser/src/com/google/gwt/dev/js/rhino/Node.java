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
 *
 * @author Norris Boyd
 * @author Mike McCabe
 */

public class Node implements Cloneable {

    private static class NumberNode extends Node {

        NumberNode(double number) {
            super(TokenStream.NUMBER);
            this.number = number;
        }

        @Override
        public double getDouble() {
          return this.number;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof NumberNode
                && getDouble() == ((NumberNode) o).getDouble();
        }

        private double number;
    }

    private static class StringNode extends Node {

        StringNode(int type, String str) {
            super(type);
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

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StringNode)) { return false; }
            return o instanceof StringNode
                && this.str.equals(((StringNode) o).str);
        }

        private String str;
    }

    public Node(int nodeType) {
        type = nodeType;
    }

    public Node(int nodeType, Node child) {
        type = nodeType;
        first = last = child;
        child.next = null;
    }

    public Node(int nodeType, Node left, Node right) {
        type = nodeType;
        first = left;
        last = right;
        left.next = right;
        right.next = null;
    }

    public Node(int nodeType, Node left, Node mid, Node right) {
        type = nodeType;
        first = left;
        last = right;
        left.next = mid;
        mid.next = right;
        right.next = null;
    }

    public Node(int nodeType, Node left, Node mid, Node mid2, Node right) {
        type = nodeType;
        first = left;
        last = right;
        left.next = mid;
        mid.next = mid2;
        mid2.next = right;
        right.next = null;
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

    public Node(int nodeType, int value) {
        type = nodeType;
        intDatum = value;
    }

    public Node(int nodeType, Node child, int value) {
        this(nodeType, child);
        intDatum = value;
    }

    public Node(int nodeType, Node left, Node right, int value) {
        this(nodeType, left, right);
        intDatum = value;
    }

    public Node(int nodeType, Node left, Node mid, Node right, int value) {
        this(nodeType, left, mid, right);
        intDatum = value;
    }

    public static Node newNumber(double number) {
        return new NumberNode(number);
    }

    public static Node newString(String str) {
        return new StringNode(TokenStream.STRING, str);
    }

    public static Node newString(int type, String str) {
        return new StringNode(type, str);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getIntDatum() {
        return this.intDatum;
    }

    public boolean hasChildren() {
        return first != null;
    }

    public Node getFirstChild() {
        return first;
    }

    public Node getLastChild() {
        return last;
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


    public Node getChildBefore(Node child) {
        if (child == first)
            return null;
        Node n = first;
        while (n.next != child) {
            n = n.next;
            if (n == null)
                throw new RuntimeException("node is not a child");
        }
        return n;
    }

    public Node getLastSibling() {
        Node n = this;
        while (n.next != null) {
            n = n.next;
        }
        return n;
    }

    public void addChildToFront(Node child) {
        child.next = first;
        first = child;
        if (last == null) {
            last = child;
        }
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

    public void addChildrenToFront(Node children) {
        Node lastSib = children.getLastSibling();
        lastSib.next = first;
        first = children;
        if (last == null) {
            last = lastSib;
        }
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

    /**
     * Add 'child' before 'node'.
     */
    public void addChildBefore(Node newChild, Node node) {
        if (newChild.next != null)
            throw new RuntimeException(
                      "newChild had siblings in addChildBefore");
        if (first == node) {
            newChild.next = first;
            first = newChild;
            return;
        }
        Node prev = getChildBefore(node);
        addChildAfter(newChild, prev);
    }

    /**
     * Add 'child' after 'node'.
     */
    public void addChildAfter(Node newChild, Node node) {
        if (newChild.next != null)
            throw new RuntimeException(
                      "newChild had siblings in addChildAfter");
        newChild.next = node.next;
        node.next = newChild;
        if (last == node)
            last = newChild;
    }

    public void removeChild(Node child) {
        Node prev = getChildBefore(child);
        if (prev == null)
            first = first.next;
        else
            prev.next = child.next;
        if (child == last) last = prev;
        child.next = null;
    }

    public void replaceChild(Node child, Node newChild) {
        newChild.next = child.next;
        if (child == first) {
            first = newChild;
        } else {
            Node prev = getChildBefore(child);
            prev.next = newChild;
        }
        if (child == last)
            last = newChild;
        child.next = null;
    }

    public void replaceChildAfter(Node prevChild, Node newChild) {
        Node child = prevChild.next;
        newChild.next = child.next;
        prevChild.next = newChild;
        if (child == last)
            last = newChild;
        child.next = null;
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

    public Object getProp(int propType) {
        if (props == null)
            return null;
        return props.getObject(propType);
    }

    public int getIntProp(int propType, int defaultValue) {
        if (props == null)
            return defaultValue;
        return props.getInt(propType, defaultValue);
    }

    public int getExistingIntProp(int propType) {
        return props.getExistingInt(propType);
    }

    public void putProp(int propType, Object prop) {
        if (prop == null) {
            removeProp(propType);
        }
        else {
            if (props == null) {
                props = new UintMap(2);
            }
            props.put(propType, prop);
        }
    }

    public void putIntProp(int propType, int prop) {
        if (props == null)
            props = new UintMap(2);
        props.put(propType, prop);
    }

    public void removeProp(int propType) {
        if (props != null) {
            props.remove(propType);
        }
    }

    public int getOperation() {
        switch (type) {
            case TokenStream.EQOP:
            case TokenStream.RELOP:
            case TokenStream.UNARYOP:
            case TokenStream.PRIMARY:
                return intDatum;
        }
        Context.codeBug();
        return 0;
    }

    public int getLineno() {
        if (hasLineno()) {
            return intDatum;
        }
        return -1;
    }

    private boolean hasLineno() {
        switch (type) {
            case TokenStream.EXPRSTMT:
            case TokenStream.BLOCK:
            case TokenStream.VAR:
            case TokenStream.WHILE:
            case TokenStream.DO:
            case TokenStream.SWITCH:
            case TokenStream.CATCH:
            case TokenStream.THROW:
            case TokenStream.RETURN:
            case TokenStream.BREAK:
            case TokenStream.CONTINUE:
            case TokenStream.WITH:
                return true;
        }
        return false;
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

    /**
     * Not usefully implemented.
     */
    @Override
    public final int hashCode() {
        assert false : "hashCode not designed";
        return 42; // any arbitrary constant will do
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Node)) { return false; }
        return hasLineno()
            || this.getIntDatum() == ((Node) o).getIntDatum();
    }

    public Node cloneNode() {
        Node result;
        try {
            result = (Node) super.clone();
            result.next = null;
            result.first = null;
            result.last = null;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    public Node cloneTree() {
        Node result = cloneNode();
        for (Node n2 = getFirstChild(); n2 != null; n2 = n2.getNext()) {
            Node n2clone = n2.cloneTree();
            if (result.last != null) {
                result.last.next = n2clone;
            }
            if (result.first == null) {
                result.first = n2clone;
            }
            result.last = n2clone;
        }
        return result;
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
            if (intDatum != -1) {
                sb.append(' ');
                sb.append(intDatum);
            }
            if (props == null)
                return sb.toString();

            int[] keys = props.getKeys();
            for (int i = 0; i != keys.length; ++i) {
                int key = keys[i];
                sb.append(" [");
                sb.append(propToString(key));
                sb.append(": ");
                switch (key) {
                    case FIXUPS_PROP : // can't add this as it recurses
                        sb.append("fixups property");
                        break;
                    case SOURCE_PROP : // can't add this as it has unprintables
                        sb.append("source property");
                        break;
                    case TARGETBLOCK_PROP : // can't add this as it recurses
                        sb.append("target block property");
                        break;
                    case LASTUSE_PROP :     // can't add this as it is dull
                        sb.append("last use property");
                        break;
                    default :
                        Object obj = props.getObject(key);
                        if (obj != null) {
                            sb.append(obj.toString());
                        } else {
                            sb.append(props.getExistingInt(key));
                        }
                        break;
                }
                sb.append(']');
            }
            return sb.toString();
        }
        return null;
    }

    public String toStringTree() {
        return toStringTreeHelper(0);
    }


    private String toStringTreeHelper(int level) {
        if (Context.printTrees) {
            StringBuffer s = new StringBuffer();
            for (int i=0; i < level; i++) {
                s.append("    ");
            }
            s.append(toString());
            s.append('\n');
            for (Node cursor = getFirstChild(); cursor != null;
                 cursor = cursor.getNext())
            {
                Node n = cursor;
                s.append(n.toStringTreeHelper(level+1));
            }
            return s.toString();
        }
        return "";
    }

    /**
     * Checks if the subtree under this node is the same as another subtree.
     * Returns null if it's equal, or a message describing the differences.
     */
    public String checkTreeEquals(Node node2) {
        boolean eq = false;

        if (type == node2.getType() &&
            getChildCount() == node2.getChildCount() &&
            getClass() == node2.getClass()) {

            eq = this.equals(node2);
        }

        if (!eq) {
            return "Node tree inequality:\nTree1:\n" + toStringTreeHelper(1) +
                "\n\nTree2:\n" + node2.toStringTreeHelper(1);
        }

        String res = null;
        Node n, n2;
        for (n = first, n2 = node2.first;
             res == null && n != null;
             n = n.next, n2 = n2.next) {
            res = n.checkTreeEquals(n2);
            if (res != null) {
                return res;
            }
        }
        return res;
    }

    public void setIsSyntheticBlock(boolean val) {
        isSyntheticBlock = val;
    }

    public boolean isSyntheticBlock() {
        return isSyntheticBlock;
    }

    int type;              // type of the node; TokenStream.NAME for example
    Node next;             // next sibling
    private Node first;    // first element of a linked list of children
    private Node last;     // last element of a linked list of children
    private int intDatum = -1;    // encapsulated int data; depends on type
    private UintMap props;
    private boolean isSyntheticBlock = false;
}
