/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.generation;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.impl.TemplateEditorUtil;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public class GenerateByPatternDialog extends DialogWrapper {

  private final Project myProject;
  private JPanel myPanel;
  private Splitter mySplitter;
  private final Tree myTree;
  private final Editor myEditor;

  private final MultiMap<String,PatternDescriptor> myMap;

  public GenerateByPatternDialog(Project project, PatternDescriptor[] descriptors) {
    super(project);
    myProject = project;
    setTitle("Generate by Pattern");
    setOKButtonText("Generate");

    myMap = new MultiMap<>();
    for (PatternDescriptor descriptor : descriptors) {
      myMap.putValue(descriptor.getParentId(), descriptor);
    }
    DefaultMutableTreeNode root = createNode(null);

    myTree = new SimpleTree() {

    };
    myTree.setRootVisible(false);
    myTree.setCellRenderer(new DefaultTreeCellRenderer() {
      @NotNull
      @Override
      public Component getTreeCellRendererComponent(@NotNull JTree tree,
                                                    Object value,
                                                    boolean sel,
                                                    boolean expanded,
                                                    boolean leaf,
                                                    int row,
                                                    boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,

                                                                           hasFocus);
        Object object = ((DefaultMutableTreeNode)value).getUserObject();
        if (object instanceof PatternDescriptor) {
          setText(((PatternDescriptor)object).getName());
          setIcon(((PatternDescriptor)object).getIcon());
        }
        return component;
      }
    });

    myTree.setModel(new DefaultTreeModel(root));
    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(@NotNull TreeSelectionEvent e) {
        update();
      }
    });
    myEditor = TemplateEditorUtil.createEditor(true, "");

    mySplitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree));
    JPanel details = new JPanel(new BorderLayout());
    details.add(myEditor.getComponent(), BorderLayout.CENTER);
    mySplitter.setSecondComponent(details);
    mySplitter.setHonorComponentsMinimumSize(true);
    mySplitter.setShowDividerControls(true);

    myTree.setSelectionRow(0);

    init();
  }

  private void update() {
    DefaultMutableTreeNode node = getSelectedNode();
    getOKAction().setEnabled(node != null && node.isLeaf());

    PatternDescriptor descriptor = getSelectedDescriptor();
    if (descriptor != null) {
      updateDetails(descriptor);
    }
  }

  private DefaultMutableTreeNode getSelectedNode() {
    TreePath path = myTree.getSelectionModel().getSelectionPath();
    return path == null ? null : (DefaultMutableTreeNode)path.getLastPathComponent();
  }

  PatternDescriptor getSelectedDescriptor() {
    DefaultMutableTreeNode selectedNode = getSelectedNode();
    if (selectedNode != null) {
      Object object = selectedNode.getUserObject();
      if (object instanceof PatternDescriptor) {
        return (PatternDescriptor)object;
      }
    }
    return null;
  }

  private void updateDetails(final PatternDescriptor descriptor) {
    WriteCommandAction.writeCommandAction(myProject).run(() -> {
      final Template template = descriptor.getTemplate();
      if (template instanceof TemplateImpl) {
        String text = template.getString();
        myEditor.getDocument().replaceString(0, myEditor.getDocument().getTextLength(), text);
        TemplateEditorUtil.setHighlighter(myEditor, ((TemplateImpl)template).getTemplateContext());
      }
      else {
        myEditor.getDocument().replaceString(0, myEditor.getDocument().getTextLength(), "");
      }
    });
  }

  private DefaultMutableTreeNode createNode(@Nullable PatternDescriptor descriptor) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(descriptor) {
      @NotNull
      @Override
      public String toString() {
        Object object = getUserObject();
        return object == null ? "" : ((PatternDescriptor)object).getName();
      }
    };
    String id = descriptor == null ? PatternDescriptor.ROOT : descriptor.getId();
    Collection<PatternDescriptor> collection = myMap.get(id);
    for (PatternDescriptor childDescriptor : collection) {
      root.add(createNode(childDescriptor));
    }
    return root;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "generate.patterns.dialog";
  }

  @Override
  protected void dispose() {
    super.dispose();
    EditorFactory.getInstance().releaseEditor(myEditor);
  }

  private void createUIComponents() {
    mySplitter = new Splitter(false, 0.3f);
  }
}
