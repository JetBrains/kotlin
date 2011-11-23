package org.jetbrains.k2js.utils;

/*
 * Copyright 2000 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

import java.util.*;

/**
 * A set of <code>Object</code>s with pairwise orderings between them.
 * The <code>iterator</code> method provides the elements in
 * topologically sorted order.  Elements participating in a cycle
 * are not returned.
 * <p/>
 * Unlike the <code>SortedSet</code> and <code>SortedMap</code>
 * interfaces, which require their elements to implement the
 * <code>Comparable</code> interface, this class receives ordering
 * information via its <code>setOrdering</code> and
 * <code>unsetPreference</code> methods.  This difference is due to
 * the fact that the relevant ordering between elements is unlikely to
 * be inherent in the elements themselves; rather, it is set
 * dynamically accoring to application policy.  For example, in a
 * service provider registry situation, an application might allow the
 * user to set a preference order for service provider objects
 * supplied by a trusted vendor over those supplied by another.
 */
class PartiallyOrderedSet extends AbstractSet {

    // The topological sort (roughly) follows the algorithm described in
    // Horowitz and Sahni, _Fundamentals of Data Structures_ (1976),
    // p. 315.

    // Maps Objects to DigraphNodes that contain them
    private Map poNodes = new HashMap();

    // The set of Objects
    private Set nodes = poNodes.keySet();

    /**
     * Constructs a <code>PartiallyOrderedSet</code>.
     */
    public PartiallyOrderedSet() {
    }

    public int size() {
        return nodes.size();
    }

    public boolean contains(Object o) {
        return nodes.contains(o);
    }

    /**
     * Returns an iterator over the elements contained in this
     * collection, with an ordering that respects the orderings set
     * by the <code>setOrdering</code> method.
     */
    public Iterator iterator() {
        return new PartialOrderIterator(poNodes.values().iterator());
    }

    /**
     * Adds an <code>Object</code> to this
     * <code>PartiallyOrderedSet</code>.
     */
    public boolean add(Object o) {
        if (nodes.contains(o)) {
            return false;
        }

        DiGraphNode node = new DiGraphNode(o);
        poNodes.put(o, node);
        return true;
    }

    /**
     * Removes an <code>Object</code> from this
     * <code>PartiallyOrderedSet</code>.
     */
    public boolean remove(Object o) {
        DiGraphNode node = (DiGraphNode) poNodes.get(o);
        if (node == null) {
            return false;
        }

        poNodes.remove(o);
        node.dispose();
        return true;
    }

    public void clear() {
        poNodes.clear();
    }

    /**
     * Sets an ordering between two nodes.  When an iterator is
     * requested, the first node will appear earlier in the
     * sequence than the second node.  If a prior ordering existed
     * between the nodes in the opposite order, it is removed.
     *
     * @return <code>true</code> if no prior ordering existed
     *         between the nodes, <code>false</code>otherwise.
     */
    public boolean setOrdering(Object first, Object second) {
        DiGraphNode firstPONode =
                (DiGraphNode) poNodes.get(first);
        DiGraphNode secondPONode =
                (DiGraphNode) poNodes.get(second);

        secondPONode.removeEdge(firstPONode);
        return firstPONode.addEdge(secondPONode);
    }

    /**
     * Removes any ordering between two nodes.
     *
     * @return true if a prior prefence existed between the nodes.
     */
    public boolean unsetOrdering(Object first, Object second) {
        DiGraphNode firstPONode =
                (DiGraphNode) poNodes.get(first);
        DiGraphNode secondPONode =
                (DiGraphNode) poNodes.get(second);

        return firstPONode.removeEdge(secondPONode) ||
                secondPONode.removeEdge(firstPONode);
    }

    /**
     * Returns <code>true</code> if an ordering exists between two
     * nodes.
     */
    public boolean hasOrdering(Object preferred, Object other) {
        DiGraphNode preferredPONode =
                (DiGraphNode) poNodes.get(preferred);
        DiGraphNode otherPONode =
                (DiGraphNode) poNodes.get(other);

        return preferredPONode.hasEdge(otherPONode);
    }
}

class PartialOrderIterator implements Iterator {

    LinkedList zeroList = new LinkedList();
    Map inDegrees = new HashMap(); // DiGraphNode -> Integer

    public PartialOrderIterator(Iterator iter) {
        // Initialize scratch in-degree values, zero list
        while (iter.hasNext()) {
            DiGraphNode node = (DiGraphNode) iter.next();
            int inDegree = node.getInDegree();
            inDegrees.put(node, new Integer(inDegree));

            // Add nodes with zero in-degree to the zero list
            if (inDegree == 0) {
                zeroList.add(node);
            }
        }
    }

    public boolean hasNext() {
        return !zeroList.isEmpty();
    }

    public Object next() {
        DiGraphNode first = (DiGraphNode) zeroList.removeFirst();

        // For each out node of the output node, decrement its in-degree
        Iterator outNodes = first.getOutNodes();
        while (outNodes.hasNext()) {
            DiGraphNode node = (DiGraphNode) outNodes.next();
            int inDegree = ((Integer) inDegrees.get(node)).intValue() - 1;
            inDegrees.put(node, new Integer(inDegree));

            // If the in-degree has fallen to 0, place the node on the list
            if (inDegree == 0) {
                zeroList.add(node);
            }
        }

        return first.getData();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}