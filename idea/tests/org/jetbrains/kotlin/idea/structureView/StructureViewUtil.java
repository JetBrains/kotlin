/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.structureView;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;

import static com.intellij.util.ObjectUtils.tryCast;


/**
 * Extracted from PlatformTestUtil to print JTree with location string.
 */
public class StructureViewUtil {
    @Nullable
    protected static String toString(@Nullable Object node, @Nullable Queryable.PrintInfo printInfo) {
        if (node instanceof AbstractTreeNode) {
            return ((AbstractTreeNode) node).toTestString(printInfo);
        }

        FilteringTreeStructure.FilteringNode filteringNode = tryCast(node, FilteringTreeStructure.FilteringNode.class);
        if (filteringNode != null && filteringNode.getDelegate() instanceof AbstractTreeNode) {
            return ((AbstractTreeNode) filteringNode.getDelegate()).toTestString(printInfo);
        }

        if (node == null) {
            return "NULL";
        }

        return node.toString();
    }

    @NotNull
    public static String print(JTree tree, boolean withSelection) {
        return print(tree, withSelection, null, null);
    }

    @NotNull
    public static String print(
            JTree tree, boolean withSelection,
            @Nullable Queryable.PrintInfo printInfo,
            @Nullable Condition<String> nodePrintCondition) {
        StringBuilder buffer = new StringBuilder();
        Collection<String> strings = printAsList(tree, withSelection, printInfo, nodePrintCondition);
        for (String string : strings) {
            buffer.append(string).append("\n");
        }
        return buffer.toString();
    }

    public static Collection<String> printAsList(
            JTree tree, boolean withSelection,
            @Nullable Queryable.PrintInfo printInfo,
            @Nullable Condition<String> nodePrintCondition) {
        Collection<String> strings = new ArrayList<String>();
        Object root = tree.getModel().getRoot();
        printImpl(tree, root, strings, 0, withSelection, printInfo, nodePrintCondition);
        return strings;
    }

    private static void printImpl(JTree tree,
            Object root,
            Collection<String> strings,
            int level,
            boolean withSelection,
            @Nullable Queryable.PrintInfo printInfo,
            @Nullable Condition<String> nodePrintCondition) {
        DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)root;

        Object userObject = defaultMutableTreeNode.getUserObject();
        String nodeText;
        if (userObject != null) {
            nodeText = toString(userObject, printInfo);
        }
        else {
            nodeText = "null";
        }

        if (nodePrintCondition != null && !nodePrintCondition.value(nodeText)) return;

        StringBuilder buff = new StringBuilder();
        StringUtil.repeatSymbol(buff, ' ', level);

        boolean expanded = tree.isExpanded(new TreePath(defaultMutableTreeNode.getPath()));
        if (!defaultMutableTreeNode.isLeaf()) {
            buff.append(expanded ? "-" : "+");
        }

        boolean selected = tree.getSelectionModel().isPathSelected(new TreePath(defaultMutableTreeNode.getPath()));
        if (withSelection && selected) {
            buff.append("[");
        }

        buff.append(nodeText);

        if (withSelection && selected) {
            buff.append("]");
        }

        strings.add(buff.toString());

        int childCount = tree.getModel().getChildCount(root);
        if (expanded) {
            for (int i = 0; i < childCount; i++) {
                printImpl(tree, tree.getModel().getChild(root, i), strings, level + 1, withSelection, printInfo, nodePrintCondition);
            }
        }
    }
}
