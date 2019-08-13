// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors;

import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptorWithPath;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.FontUtil;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.*;
import java.awt.event.ActionListener;
import java.util.*;

import static com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptorWithPath.NAME_SEPARATOR;

/**
 * @author Rustam Vishnyakov
 */
public class ColorOptionsTree extends Tree {
  private final String myCategoryName;
  private final DefaultTreeModel myTreeModel;

  private static final Comparator<EditorSchemeAttributeDescriptor> ATTR_COMPARATOR =
    (o1, o2) -> StringUtil.naturalCompare(o1.toString(), o2.toString());

  public ColorOptionsTree(@NotNull String categoryName) {
    super(createTreeModel());
    myTreeModel = (DefaultTreeModel)getModel();
    setRootVisible(false);
    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myCategoryName = categoryName;
    new TreeSpeedSearch(this, TreeSpeedSearch.NODE_DESCRIPTOR_TOSTRING, true);
  }

  public void fillOptions(@NotNull ColorAndFontOptions options) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    for (EditorSchemeAttributeDescriptor description : getOrderedDescriptors(options)) {
      if (!description.getGroup().equals(myCategoryName)) continue;
      List<String> path = extractPath(description);
      if (path != null && path.size() > 1) {
        MyTreeNode groupNode = ensureGroup(root, path, 0);
        groupNode.add(new MyTreeNode(description, path.get(path.size() - 1)));
      }
      else {
        root.add(new MyTreeNode(description));
      }
    }
    myTreeModel.setRoot(root);
  }

  public void setEmptyText(@NotNull String text, @Nullable ActionListener linkListener) {
    StatusText statusText = getEmptyText();
    if (linkListener != null) {
      statusText.clear();
      statusText.appendText(text + ' ');
      statusText.appendText("More...", SimpleTextAttributes.LINK_ATTRIBUTES, linkListener);
    }
    else {
      statusText.setText(text);
    }
  }

  private static TreeModel createTreeModel()  {
    return new DefaultTreeModel(new DefaultMutableTreeTableNode());
  }

  private Collection<EditorSchemeAttributeDescriptor> getOrderedDescriptors(@NotNull ColorAndFontOptions options) {
    ArrayList<EditorSchemeAttributeDescriptor> list = new ArrayList<>();
    for (EditorSchemeAttributeDescriptor description : options.getCurrentDescriptions()) {
      if (!description.getGroup().equals(myCategoryName)) continue;
      list.add(description);
    }
    Collections.sort(list, ATTR_COMPARATOR);
    return list;
  }

  @Nullable
  public EditorSchemeAttributeDescriptor getSelectedDescriptor() {
    Object selectedValue = getSelectedValue();
    return selectedValue instanceof EditorSchemeAttributeDescriptor ? (EditorSchemeAttributeDescriptor)selectedValue : null;
  }

  @Nullable
  public Object getSelectedValue() {
    Object selectedNode = getLastSelectedPathComponent();
    if (selectedNode instanceof DefaultMutableTreeNode) {
      return ((DefaultMutableTreeNode)selectedNode).getUserObject();
    }
    return null;
  }

  public void selectOptionByType(@NotNull final String attributeType) {
    selectPath(findOption(myTreeModel.getRoot(), new DescriptorMatcher() {
      @Override
      public boolean matches(@NotNull Object data) {
        if (data instanceof EditorSchemeAttributeDescriptor) {
          return attributeType.equals(((EditorSchemeAttributeDescriptor)data).getType());
        }
        return false;
      }
    }));
  }

  public void selectOptionByName(@NotNull String name) {
    String optionName = name.replace(FontUtil.rightArrow(UIUtil.getLabelFont()), NAME_SEPARATOR);
    selectPath(findOption(myTreeModel.getRoot(), new DescriptorMatcher() {
      @Override
      public boolean matches(@NotNull Object data) {
        return !optionName.isEmpty() &&  StringUtil.containsIgnoreCase(data.toString(), optionName);
      }
    }));
  }

  @Nullable
  private TreePath findOption(@NotNull Object nodeObject, @NotNull DescriptorMatcher matcher) {
    for (int i = 0; i < myTreeModel.getChildCount(nodeObject); i ++) {
      Object childObject = myTreeModel.getChild(nodeObject, i);
      if (childObject instanceof MyTreeNode) {
        Object data = ((MyTreeNode)childObject).getUserObject();
        if (matcher.matches(data)) {
          return new TreePath(myTreeModel.getPathToRoot((MyTreeNode)childObject));
        }
      }
      TreePath pathInChild = findOption(childObject, matcher);
      if (pathInChild != null) return pathInChild;
    }
    return null;
  }

  private void selectPath(@Nullable TreePath path) {
    if (path != null) {
      setSelectionPath(path);
      scrollPathToVisible(path);
    }
  }

  @Nullable
  private static List<String> extractPath(@NotNull EditorSchemeAttributeDescriptor descriptor) {
    if (descriptor instanceof EditorSchemeAttributeDescriptorWithPath) {
      String name = descriptor.toString();
      List<String> path = new ArrayList<>();
      int separatorStart = name.indexOf(NAME_SEPARATOR);
      int nextChunkStart = 0;
      while(separatorStart > 0) {
        path.add(name.substring(nextChunkStart, separatorStart));
        nextChunkStart = separatorStart + NAME_SEPARATOR.length();
        separatorStart = name.indexOf(NAME_SEPARATOR, nextChunkStart);
      }
      if (nextChunkStart < name.length()) {
        path.add(name.substring(nextChunkStart));
      }
      return path;
    }
    return null;
  }

  private static class MyTreeNode extends DefaultMutableTreeNode {
    private final String myName;

    MyTreeNode(@NotNull EditorSchemeAttributeDescriptor descriptor, @NotNull String name) {
      super(descriptor);
      myName = name;
    }

    MyTreeNode(@NotNull EditorSchemeAttributeDescriptor descriptor) {
      super(descriptor);
      myName = descriptor.toString();
    }

    MyTreeNode(@NotNull String groupName) {
      super(groupName);
      myName = groupName;
    }

    @Override
    public String toString() {
      return myName;
    }

  }

  private interface DescriptorMatcher {
    boolean matches(@NotNull Object data);
  }

  private static MyTreeNode ensureGroup(@NotNull DefaultMutableTreeNode root, @NotNull List<String> path, int index) {
    String groupName = path.get(index ++);
    for (int i = 0; i < root.getChildCount(); i ++) {
      TreeNode child = root.getChildAt(i);
      if (child instanceof MyTreeNode && groupName.equals(child.toString())) {
        return index < path.size() - 1 ? ensureGroup((MyTreeNode)child, path, index) : (MyTreeNode)child;
      }
    }
    MyTreeNode groupNode = new MyTreeNode(groupName);
    root.add(groupNode);
    return index < path.size() - 1 ? ensureGroup(groupNode, path, index) : groupNode;
  }
}
