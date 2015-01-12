/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
