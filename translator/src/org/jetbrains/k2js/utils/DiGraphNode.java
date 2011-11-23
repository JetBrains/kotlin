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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A node in a directed graph.  In addition to an arbitrary
 * <code>Object</code> containing user data associated with the node,
 * each node maintains a <code>Set</code>s of nodes which are pointed
 * to by the current node (available from <code>getOutNodes</code>).
 * The in-degree of the node (that is, number of nodes that point to
 * the current node) may be queried.
 */
class DiGraphNode implements Cloneable, Serializable {

    /**
     * The data associated with this node.
     */
    protected Object data;

    /**
     * A <code>Set</code> of neighboring nodes pointed to by this
     * node.
     */
    protected Set outNodes = new HashSet();

    /**
     * The in-degree of the node.
     */
    protected int inDegree = 0;

    /**
     * A <code>Set</code> of neighboring nodes that point to this
     * node.
     */
    private Set inNodes = new HashSet();

    public DiGraphNode(Object data) {
        this.data = data;
    }

    /**
     * Returns the <code>Object</code> referenced by this node.
     */
    public Object getData() {
        return data;
    }

    /**
     * Returns an <code>Iterator</code> containing the nodes pointed
     * to by this node.
     */
    public Iterator getOutNodes() {
        return outNodes.iterator();
    }

    /**
     * Adds a directed edge to the graph.  The outNodes list of this
     * node is updated and the in-degree of the other node is incremented.
     *
     * @param node a <code>DiGraphNode</code>.
     * @return <code>true</code> if the node was not previously the
     *         target of an edge.
     */
    public boolean addEdge(DiGraphNode node) {
        if (outNodes.contains(node)) {
            return false;
        }

        outNodes.add(node);
        node.inNodes.add(this);
        node.incrementInDegree();
        return true;
    }

    /**
     * Returns <code>true</code> if an edge exists between this node
     * and the given node.
     *
     * @param node a <code>DiGraphNode</code>.
     * @return <code>true</code> if the node is the target of an edge.
     */
    public boolean hasEdge(DiGraphNode node) {
        return outNodes.contains(node);
    }

    /**
     * Removes a directed edge from the graph.  The outNodes list of this
     * node is updated and the in-degree of the other node is decremented.
     *
     * @return <code>true</code> if the node was previously the target
     *         of an edge.
     */
    public boolean removeEdge(DiGraphNode node) {
        if (!outNodes.contains(node)) {
            return false;
        }

        outNodes.remove(node);
        node.inNodes.remove(this);
        node.decrementInDegree();
        return true;
    }

    /**
     * Removes this node from the graph, updating neighboring nodes
     * appropriately.
     */
    public void dispose() {
        Object[] inNodesArray = inNodes.toArray();
        for (int i = 0; i < inNodesArray.length; i++) {
            DiGraphNode node = (DiGraphNode) inNodesArray[i];
            node.removeEdge(this);
        }

        Object[] outNodesArray = outNodes.toArray();
        for (int i = 0; i < outNodesArray.length; i++) {
            DiGraphNode node = (DiGraphNode) outNodesArray[i];
            removeEdge(node);
        }
    }

    /**
     * Returns the in-degree of this node.
     */
    public int getInDegree() {
        return inDegree;
    }

    /**
     * Increments the in-degree of this node.
     */
    private void incrementInDegree() {
        ++inDegree;
    }

    /**
     * Decrements the in-degree of this node.
     */
    private void decrementInDegree() {
        --inDegree;
    }
}