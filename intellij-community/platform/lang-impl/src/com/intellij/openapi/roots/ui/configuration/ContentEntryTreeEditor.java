// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.fileChooser.actions.NewFolderAction;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl;
import com.intellij.openapi.fileChooser.impl.FileTreeBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.ui.configuration.actions.IconWithTextAction;
import com.intellij.openapi.roots.ui.configuration.actions.ToggleExcludedStateAction;
import com.intellij.openapi.roots.ui.configuration.actions.ToggleSourcesStateAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class ContentEntryTreeEditor {
  private final Project myProject;
  private final List<ModuleSourceRootEditHandler<?>> myEditHandlers;
  protected final Tree myTree;
  private FileSystemTreeImpl myFileSystemTree;
  private final JPanel myTreePanel;
  private final TreeNode EMPTY_TREE_ROOT = new DefaultMutableTreeNode(ProjectBundle.message("module.paths.empty.node"));
  protected final DefaultActionGroup myEditingActionsGroup;
  private ContentEntryEditor myContentEntryEditor;
  private final MyContentEntryEditorListener myContentEntryEditorListener = new MyContentEntryEditorListener();
  private final FileChooserDescriptor myDescriptor;
  private final JTextField myExcludePatternsField;

  public ContentEntryTreeEditor(Project project, List<ModuleSourceRootEditHandler<?>> editHandlers) {
    myProject = project;
    myEditHandlers = editHandlers;
    myTree = new Tree();
    myTree.setRootVisible(true);
    myTree.setShowsRootHandles(true);

    myEditingActionsGroup = new DefaultActionGroup();

    TreeUtil.installActions(myTree);
    new TreeSpeedSearch(myTree);

    JPanel excludePatternsPanel = new JPanel(new GridBagLayout());
    excludePatternsPanel.setBorder(JBUI.Borders.empty(5));
    GridBag gridBag = new GridBag().setDefaultWeightX(1, 1.0).setDefaultPaddingX(JBUIScale.scale(5));
    excludePatternsPanel.add(new JLabel(ProjectBundle.message("module.paths.exclude.patterns")), gridBag.nextLine().next());
    myExcludePatternsField = new JTextField();
    myExcludePatternsField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        if (myContentEntryEditor != null) {
          ContentEntry entry = myContentEntryEditor.getContentEntry();
          if (entry != null) {
            List<String> patterns = StringUtil.split(myExcludePatternsField.getText().trim(), ";");
            if (!patterns.equals(entry.getExcludePatterns())) {
              entry.setExcludePatterns(patterns);
            }
          }
        }
      }
    });
    excludePatternsPanel.add(myExcludePatternsField, gridBag.next().fillCellHorizontally());
    JBLabel excludePatternsLegendLabel = new JBLabel(XmlStringUtil.wrapInHtml("Use <b>;</b> to separate name patterns, <b>*</b> for any number of symbols, <b>?</b> for one."));
    excludePatternsLegendLabel.setForeground(JBColor.GRAY);
    excludePatternsPanel.add(excludePatternsLegendLabel, gridBag.nextLine().next().next().fillCellHorizontally());
    myTreePanel = new MyPanel(new BorderLayout());
    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree, true);
    myTreePanel.add(scrollPane, BorderLayout.CENTER);
    myTreePanel.add(excludePatternsPanel, BorderLayout.SOUTH);

    myTreePanel.setVisible(false);
    myDescriptor = FileChooserDescriptorFactory.createMultipleFoldersDescriptor();
    myDescriptor.setShowFileSystemRoots(false);
  }

  protected void createEditingActions() {
    for (final ModuleSourceRootEditHandler<?> editor : myEditHandlers) {
      ToggleSourcesStateAction action = new ToggleSourcesStateAction(myTree, this, editor);
      CustomShortcutSet shortcutSet = editor.getMarkRootShortcutSet();
      if (shortcutSet != null) {
        action.registerCustomShortcutSet(shortcutSet, myTree);
      }
      myEditingActionsGroup.add(action);
    }

    setupExcludedAction();
  }

  protected List<ModuleSourceRootEditHandler<?>> getEditHandlers() {
    return myEditHandlers;
  }

  protected TreeCellRenderer getContentEntryCellRenderer() {
    return new ContentEntryTreeCellRenderer(this, myEditHandlers);
  }

  /**
   * @param contentEntryEditor : null means to clear the editor
   */
  public void setContentEntryEditor(final ContentEntryEditor contentEntryEditor) {
    if (myContentEntryEditor != null && myContentEntryEditor.equals(contentEntryEditor)) {
      return;
    }
    if (myFileSystemTree != null) {
      Disposer.dispose(myFileSystemTree);
      myFileSystemTree = null;
    }
    if (myContentEntryEditor != null) {
      myContentEntryEditor.removeContentEntryEditorListener(myContentEntryEditorListener);
      myContentEntryEditor = null;
    }
    if (contentEntryEditor == null) {
      ((DefaultTreeModel)myTree.getModel()).setRoot(EMPTY_TREE_ROOT);
      myTreePanel.setVisible(false);
      if (myFileSystemTree != null) {
        Disposer.dispose(myFileSystemTree);
      }
      return;
    }
    myTreePanel.setVisible(true);
    myContentEntryEditor = contentEntryEditor;
    myContentEntryEditor.addContentEntryEditorListener(myContentEntryEditorListener);

    final ContentEntry entry = contentEntryEditor.getContentEntry();
    assert entry != null : contentEntryEditor;
    final VirtualFile file = entry.getFile();
    if (file != null) {
      myDescriptor.setRoots(file);
    }
    else {
      String path = VfsUtilCore.urlToPath(entry.getUrl());
      myDescriptor.setTitle(FileUtil.toSystemDependentName(path));
    }
    myExcludePatternsField.setText(StringUtil.join(entry.getExcludePatterns(), ";"));

    final Runnable init = () -> {
      myFileSystemTree.updateTree();
      myFileSystemTree.select(file, null);
    };

    myFileSystemTree = new FileSystemTreeImpl(myProject, myDescriptor, myTree, getContentEntryCellRenderer(), init, null) {
      @Override
      protected AbstractTreeBuilder createTreeBuilder(JTree tree, DefaultTreeModel treeModel, AbstractTreeStructure treeStructure,
                                                      Comparator<NodeDescriptor> comparator, FileChooserDescriptor descriptor,
                                                      final Runnable onInitialized) {
        return new MyFileTreeBuilder(tree, treeModel, treeStructure, comparator, descriptor, onInitialized);
      }
    };
    myFileSystemTree.showHiddens(true);
    Disposer.register(myProject, myFileSystemTree);

    final NewFolderAction newFolderAction = new MyNewFolderAction();
    final DefaultActionGroup mousePopupGroup = new DefaultActionGroup();
    mousePopupGroup.add(myEditingActionsGroup);
    mousePopupGroup.addSeparator();
    mousePopupGroup.add(newFolderAction);
    myFileSystemTree.registerMouseListener(mousePopupGroup);
  }

  public ContentEntryEditor getContentEntryEditor() {
    return myContentEntryEditor;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  public JComponent createComponent() {
    createEditingActions();
    return myTreePanel;
  }

  public void select(VirtualFile file) {
    if (myFileSystemTree != null) {
      myFileSystemTree.select(file, null);
    }
  }

  public void requestFocus() {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myTree, true));
  }

  public void update() {
    if (myFileSystemTree != null) {
      myFileSystemTree.updateTree();
      final DefaultTreeModel model = (DefaultTreeModel)myTree.getModel();
      final int visibleRowCount = TreeUtil.getVisibleRowCount(myTree);
      for (int row = 0; row < visibleRowCount; row++) {
        final TreePath pathForRow = myTree.getPathForRow(row);
        if (pathForRow != null) {
          final TreeNode node = (TreeNode)pathForRow.getLastPathComponent();
          if (node != null) {
            model.nodeChanged(node);
          }
        }
      }
    }
  }

  private class MyContentEntryEditorListener extends ContentEntryEditorListenerAdapter {
    @Override
    public void sourceFolderAdded(@NotNull ContentEntryEditor editor, SourceFolder folder) {
      update();
    }

    @Override
    public void sourceFolderRemoved(@NotNull ContentEntryEditor editor, VirtualFile file) {
      update();
    }

    @Override
    public void folderExcluded(@NotNull ContentEntryEditor editor, VirtualFile file) {
      update();
    }

    @Override
    public void folderIncluded(@NotNull ContentEntryEditor editor, String fileUrl) {
      update();
    }

    @Override
    public void sourceRootPropertiesChanged(@NotNull ContentEntryEditor editor, @NotNull SourceFolder folder) {
      update();
    }
  }

  private static class MyNewFolderAction extends NewFolderAction implements CustomComponentAction {
    private MyNewFolderAction() {
      super(ActionsBundle.message("action.FileChooser.NewFolder.text"),
            ActionsBundle.message("action.FileChooser.NewFolder.description"),
            AllIcons.Actions.NewFolder);
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
      return IconWithTextAction.createCustomComponentImpl(this, presentation, place);
    }
  }

  private static class MyFileTreeBuilder extends FileTreeBuilder {
    MyFileTreeBuilder(JTree tree,
                      DefaultTreeModel treeModel,
                      AbstractTreeStructure treeStructure,
                      Comparator<? super NodeDescriptor> comparator,
                      FileChooserDescriptor descriptor,
                      @Nullable Runnable onInitialized) {
      super(tree, treeModel, treeStructure, comparator, descriptor, onInitialized);
    }

    @Override
    protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
      return false; // need this in order to not show plus for empty directories
    }
  }

  private class MyPanel extends JPanel implements DataProvider {
    private MyPanel(final LayoutManager layout) {
      super(layout);
    }

    @Override
    @Nullable
    public Object getData(@NotNull @NonNls final String dataId) {
      if (FileSystemTree.DATA_KEY.is(dataId)) {
        return myFileSystemTree;
      }
      return null;
    }
  }

  public DefaultActionGroup getEditingActionsGroup() {
    return myEditingActionsGroup;
  }

  protected void setupExcludedAction() {
    ToggleExcludedStateAction toggleExcludedAction = new ToggleExcludedStateAction(myTree, this);
    myEditingActionsGroup.add(toggleExcludedAction);
    toggleExcludedAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_MASK)), myTree);
  }

}
