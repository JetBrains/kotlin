// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ToggleOptionAction.Option;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.pom.Navigatable;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.RestoreSelectionListener;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.function.Predicate;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.application.ModalityState.stateForComponent;
import static com.intellij.ui.AppUIUtil.invokeLaterIfProjectAlive;
import static com.intellij.ui.ColorUtil.toHtmlColor;
import static com.intellij.ui.ScrollPaneFactory.createScrollPane;
import static com.intellij.ui.scale.JBUIScale.scale;
import static com.intellij.util.OpenSourceUtil.navigate;
import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;

abstract class ProblemsViewPanel extends OnePixelSplitter implements Disposable, DataProvider {
  private static final Logger LOG = Logger.getInstance(ProblemsViewPanel.class);
  private final Project myProject;
  private final ProblemsViewState myState;
  private final ProblemsTreeModel myTreeModel = new ProblemsTreeModel(this);
  private final ProblemsViewPreview myPreview = new ProblemsViewPreview(this);
  private final JPanel myPanel;
  private final ActionToolbar myToolbar;
  private final Insets myToolbarInsets = JBUI.insetsRight(1);
  private final JTree myTree;

  private final Option myAutoscrollToSource = new Option() {
    @Override
    public boolean isSelected() {
      return myState.getAutoscrollToSource();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setAutoscrollToSource(selected);
      updateAutoscroll(getSelectedDescriptor());
    }
  };
  private final Option myShowPreview = new Option() {
    @Override
    public boolean isEnabled() {
      OpenFileDescriptor descriptor = getSelectedDescriptor();
      return descriptor != null && null != ProblemsView.getDocument(getProject(), descriptor.getFile());
    }

    @Override
    public boolean isAlwaysVisible() {
      return true;
    }

    @Override
    public boolean isSelected() {
      return myState.getShowPreview();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setShowPreview(selected);
      updatePreview(getSelectedDescriptor());
    }
  };
  private final Option myShowErrors = new Option() {
    @Override
    public boolean isSelected() {
      return myState.getShowErrors();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setShowErrors(selected);
      myTreeModel.setFilter(createFilter());
    }
  };
  private final Option myShowWarnings = new Option() {
    @Override
    public boolean isSelected() {
      return myState.getShowWarnings();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setShowWarnings(selected);
      myTreeModel.setFilter(createFilter());
    }
  };
  private final Option myShowInformation = new Option() {
    @Override
    public boolean isSelected() {
      return myState.getShowInformation();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setShowInformation(selected);
      myTreeModel.setFilter(createFilter());
    }
  };
  private final Option mySortFoldersFirst = new Option() {
    @Override
    public boolean isSelected() {
      return myState.getSortFoldersFirst();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setSortFoldersFirst(selected);
      myTreeModel.setComparator(createComparator());
    }
  };
  private final Option mySortBySeverity = new Option() {
    @Override
    public boolean isSelected() {
      return myState.getSortBySeverity();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setSortBySeverity(selected);
      myTreeModel.setComparator(createComparator());
    }
  };
  private final Option mySortByName = new Option() {
    @Override
    public boolean isSelected() {
      return myState.getSortByName();
    }

    @Override
    public void setSelected(boolean selected) {
      myState.setSortByName(selected);
      myTreeModel.setComparator(createComparator());
    }
  };

  ProblemsViewPanel(@NotNull Project project, @NotNull ProblemsViewState state) {
    super(false, .5f, .1f, .9f);
    myProject = project;
    myState = state;

    myTreeModel.setComparator(createComparator());
    myTreeModel.setFilter(createFilter());
    myTree = new Tree(new AsyncTreeModel(myTreeModel, this));
    myTree.setRootVisible(false);
    myTree.getSelectionModel().setSelectionMode(SINGLE_TREE_SELECTION);
    myTree.addTreeSelectionListener(new RestoreSelectionListener());
    myTree.addTreeSelectionListener(event -> {
      OpenFileDescriptor descriptor = getSelectedDescriptor();
      updateAutoscroll(descriptor);
      updatePreview(descriptor);
    });
    new TreeSpeedSearch(myTree);
    EditSourceOnDoubleClickHandler.install(myTree);
    EditSourceOnEnterKeyHandler.install(myTree);
    PopupHandler.installPopupHandler(myTree, "ProblemsView.ToolWindow.TreePopup", ActionPlaces.POPUP);

    ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction("ProblemsView.ToolWindow.Toolbar");
    myToolbar = ActionManager.getInstance().createActionToolbar(getClass().getName(), group, false);
    myToolbar.getComponent().setVisible(state.getShowToolbar());
    UIUtil.addBorder(myToolbar.getComponent(), new CustomLineBorder(myToolbarInsets));

    myPanel = new JPanel(new BorderLayout());
    myPanel.add(BorderLayout.CENTER, createScrollPane(myTree, true));
    myPanel.add(BorderLayout.WEST, myToolbar.getComponent());
    setFirstComponent(myPanel);

    putClientProperty(UIUtil.NOT_IN_HIERARCHY_COMPONENTS, (Iterable<ProblemsViewPreview>)()
      -> JBIterable.of(myPreview).filter(component -> null == component.getParent()).iterator());
  }

  @Override
  public void dispose() {
    myPreview.preview(null, false);
  }

  @Override
  public @Nullable Object getData(@NotNull String dataId) {
    if (CommonDataKeys.PROJECT.is(dataId)) return getProject();
    OpenFileDescriptor descriptor = getSelectedDescriptor();
    if (descriptor != null) {
      if (CommonDataKeys.NAVIGATABLE.is(dataId)) return descriptor;
      if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) return descriptor.getFile();
      if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) return new Navigatable[]{descriptor};
      if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) return new VirtualFile[]{descriptor.getFile()};
    }
    return null;
  }

  abstract @NotNull String getDisplayName();

  final void updateToolWindowContent() {
    invokeLaterIfProjectAlive(getProject(), () -> {
      ToolWindow window = ProblemsView.getToolWindow(getProject());
      if (window == null) return;
      ContentManager manager = window.getContentManagerIfCreated();
      if (manager == null) return;
      Content content = manager.getContent(this);
      if (content == null) return;

      Root root = myTreeModel.getRoot();
      int count = root == null ? 0 : root.getProblemsCount();
      content.setDisplayName(getContentDisplayName(count));
      Icon icon = getToolWindowIcon(count);
      if (icon != null) window.setIcon(icon);
    });
  }

  @Nullable Icon getToolWindowIcon(int count) {
    return null;
  }

  @NotNull String getContentDisplayName(int count) {
    String name = getDisplayName();
    if (count <= 0) return name;
    return "<html><body>" + name + " <font color='" + toHtmlColor(UIUtil.getInactiveTextColor()) + "'>" + count + "</font></body></html>";
  }

  @Override
  protected void loadProportion() {
    if (myState != null) setProportion(myState.getProportion());
  }

  @Override
  protected void saveProportion() {
    if (myState != null) myState.setProportion(getProportion());
  }

  final @NotNull Project getProject() {
    return myProject;
  }

  final @NotNull ProblemsTreeModel getTreeModel() {
    return myTreeModel;
  }

  final @NotNull JTree getTree() {
    return myTree;
  }

  void orientationChangedTo(boolean vertical) {
    setOrientation(vertical);
    myPanel.remove(myToolbar.getComponent());
    myToolbar.setOrientation(vertical ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL);
    myToolbarInsets.right = !vertical ? scale(1) : 0;
    myToolbarInsets.bottom = vertical ? scale(1) : 0;
    myPanel.add(vertical ? BorderLayout.NORTH : BorderLayout.WEST, myToolbar.getComponent());
    updatePreview(getSelectedDescriptor());
  }

  void selectionChangedTo(boolean selected) {
    if (selected) {
      myTreeModel.setComparator(createComparator());
      myTreeModel.setFilter(createFilter());
      updatePreview(getSelectedDescriptor());

      ToolWindow window = ProblemsView.getToolWindow(getProject());
      if (window instanceof ToolWindowEx) {
        ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction("ProblemsView.ToolWindow.SecondaryActions");
        ((ToolWindowEx)window).setAdditionalGearActions(group);
      }
    }
  }

  private @Nullable OpenFileDescriptor getSelectedDescriptor() {
    Object object = TreeUtil.getLastUserObject(getTree().getSelectionPath());
    if (object instanceof FileNode) return getDescriptor((FileNode)object);
    if (object instanceof ProblemNode) return getDescriptor((ProblemNode)object);
    return null;
  }

  private @Nullable OpenFileDescriptor getDescriptor(@NotNull FileNode node) {
    return getDescriptor(node.getFile(), -1);
  }

  private @Nullable OpenFileDescriptor getDescriptor(@NotNull ProblemNode node) {
    return getDescriptor(node.getFile(), node.getProblem().getOffset());
  }

  private @Nullable OpenFileDescriptor getDescriptor(@NotNull VirtualFile file, int offset) {
    Document document = ProblemsView.getDocument(getProject(), file);
    if (document == null) return null;
    if (offset < 0) return new OpenFileDescriptor(getProject(), file);
    int length = document.getTextLength();
    if (offset <= length) return new OpenFileDescriptor(getProject(), file, offset);
    LOG.warn("offset is bigger then document length: " + file);
    return new OpenFileDescriptor(getProject(), file, length);
  }

  private void updateAutoscroll(@Nullable OpenFileDescriptor descriptor) {
    if (descriptor != null && UIUtil.isFocusAncestor(this) && isNotNullAndSelected(getAutoscrollToSource())) {
      invokeLater(() -> navigate(false, descriptor));
    }
  }

  private void updatePreview(@Nullable OpenFileDescriptor descriptor) {
    Document document = descriptor == null ? null : ProblemsView.getDocument(getProject(), descriptor.getFile());
    Editor editor = myPreview.preview(document, document != null && isNotNullAndSelected(getShowPreview()));
    if (editor != null && descriptor != null) {
      invokeLater(() -> {
        if (editor.getComponent().isShowing()) {
          descriptor.navigateIn(editor);
        }
      });
    }
  }

  private void invokeLater(@NotNull Runnable runnable) {
    getApplication().invokeLater(runnable, stateForComponent(this));
  }

  void select(@NotNull Node node) {
    TreeUtil.promiseSelect(getTree(), createVisitor(node));
  }

  @NotNull TreeVisitor createVisitor(@NotNull Node node) {
    return new TreeVisitor.ByTreePath<>(node.getPath(), o -> o);
  }

  @NotNull Comparator<Node> createComparator() {
    return new NodeComparator(
      isNullableOrSelected(getSortFoldersFirst()),
      isNullableOrSelected(getSortBySeverity()),
      isNotNullAndSelected(getSortByName()));
  }

  @NotNull Predicate<Node> createFilter() {
    return new NodeFilter(
      isNullableOrSelected(getShowErrors()),
      isNotNullAndSelected(getShowWarnings()),
      isNotNullAndSelected(getShowInformation()));
  }

  @Nullable Option getAutoscrollToSource() {
    return isNotNullAndSelected(getShowPreview()) ? null : myAutoscrollToSource;
  }

  @Nullable Option getShowPreview() {
    return myShowPreview;
  }

  @Nullable Option getShowErrors() {
    return myShowErrors;
  }

  @Nullable Option getShowWarnings() {
    return myShowWarnings;
  }

  @Nullable Option getShowInformation() {
    return myShowInformation;
  }

  @Nullable Option getSortFoldersFirst() {
    return mySortFoldersFirst;
  }

  @Nullable Option getSortBySeverity() {
    return mySortBySeverity;
  }

  @Nullable Option getSortByName() {
    return mySortByName;
  }

  private static boolean isNotNullAndSelected(@Nullable Option option) {
    return option != null && option.isSelected();
  }

  private static boolean isNullableOrSelected(@Nullable Option option) {
    return option == null || option.isSelected();
  }
}
