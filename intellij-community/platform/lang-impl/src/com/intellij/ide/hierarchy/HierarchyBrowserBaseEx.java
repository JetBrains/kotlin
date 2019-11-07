// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.OccurenceNavigatorSupport;
import com.intellij.ide.PsiCopyPasteManager;
import com.intellij.ide.dnd.*;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.hierarchy.actions.BrowseHierarchyActionBase;
import com.intellij.ide.projectView.impl.ProjectViewTree;
import com.intellij.ide.util.scopeChooser.EditScopesDialog;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.TreeBuilderUtil;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.PsiElementNavigatable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.popup.HintUpdateSupply;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.usageView.UsageViewTypeLocation;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.SingleAlarm;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

public abstract class HierarchyBrowserBaseEx extends HierarchyBrowserBase implements OccurenceNavigator {
  private static final Logger LOG = Logger.getInstance(HierarchyBrowserBaseEx.class);

  public static final String SCOPE_PROJECT = IdeBundle.message("hierarchy.scope.project");
  public static final String SCOPE_ALL = IdeBundle.message("hierarchy.scope.all");
  public static final String SCOPE_TEST = IdeBundle.message("hierarchy.scope.test");
  public static final String SCOPE_CLASS = IdeBundle.message("hierarchy.scope.this.class");

  public static final String HELP_ID = "reference.toolWindows.hierarchy";

  /** @deprecated use {@link #getCurrentViewType()} */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  @SuppressWarnings("DeprecatedIsStillUsed")
  protected String myCurrentViewType;

  private static class Sheet implements Disposable {
    private AsyncTreeModel myAsyncTreeModel;
    private StructureTreeModel myStructureTreeModel;
    private final @NotNull String myType;
    private final JTree myTree;
    private String myScope;
    private final OccurenceNavigator myOccurenceNavigator;

    Sheet(@NotNull String type, @NotNull JTree tree, @NotNull String scope, @NotNull OccurenceNavigator occurenceNavigator) {
      myType = type;
      myTree = tree;
      myScope = scope;
      myOccurenceNavigator = occurenceNavigator;
    }

    @Override
    public void dispose() {
      myAsyncTreeModel = null;
      myStructureTreeModel = null;
    }
  }

  private final Map<String, Sheet> myType2Sheet = new THashMap<>();
  private final RefreshAction myRefreshAction = new RefreshAction();
  private final SingleAlarm myCursorAlarm = new SingleAlarm(() -> setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)), 100, this);
  private SmartPsiElementPointer mySmartPsiElementPointer;
  private final CardLayout myCardLayout;
  private final JPanel myTreePanel;
  private boolean myCachedIsValidBase;

  public HierarchyBrowserBaseEx(@NotNull Project project, @NotNull PsiElement element) {
    super(project);

    setHierarchyBase(element);

    myCardLayout = new CardLayout();
    myTreePanel = new JPanel(myCardLayout);

    Map<String, JTree> type2treeMap = new THashMap<>();
    createTrees(type2treeMap);

    HierarchyBrowserManager.State state = HierarchyBrowserManager.getSettings(project);

    for (Map.Entry<String, JTree> entry : type2treeMap.entrySet()) {
      JTree tree = entry.getValue();
      String type = entry.getKey();
      String scope = state.SCOPE != null ? state.SCOPE : SCOPE_ALL;

      OccurenceNavigatorSupport occurenceNavigatorSupport = new OccurenceNavigatorSupport(tree) {
        @Override
        @Nullable
        protected Navigatable createDescriptorForNode(@NotNull DefaultMutableTreeNode node) {
          HierarchyNodeDescriptor descriptor = getDescriptor(node);
          if (descriptor != null) {
            PsiElement psiElement = getOpenFileElementFromDescriptor(descriptor);
            if (psiElement != null && psiElement.isValid()) {
              return new PsiElementNavigatable(psiElement);
            }
          }
          return null;
        }

        @NotNull
        @Override
        public String getNextOccurenceActionName() {
          return getNextOccurenceActionNameImpl();
        }

        @NotNull
        @Override
        public String getPreviousOccurenceActionName() {
          return getPrevOccurenceActionNameImpl();
        }
      };


      myType2Sheet.put(type, new Sheet(type, tree, scope, occurenceNavigatorSupport));
      myTreePanel.add(ScrollPaneFactory.createScrollPane(tree), type);
    }

    final JPanel legendPanel = createLegendPanel();
    final JPanel contentPanel;
    if (legendPanel != null) {
      contentPanel = new JPanel(new BorderLayout());
      contentPanel.add(myTreePanel, BorderLayout.CENTER);
      contentPanel.add(legendPanel, BorderLayout.SOUTH);
    }
    else {
      contentPanel = myTreePanel;
    }

    buildUi(createToolbar(getActionPlace(), HELP_ID).getComponent(), contentPanel);
  }

  @Nullable
  protected PsiElement getOpenFileElementFromDescriptor(@NotNull HierarchyNodeDescriptor descriptor) {
    return getElementFromDescriptor(descriptor);
  }

  @Override
  @Nullable
  protected abstract PsiElement getElementFromDescriptor(@NotNull HierarchyNodeDescriptor descriptor);

  @NotNull
  protected abstract String getPrevOccurenceActionNameImpl();

  @NotNull
  protected abstract String getNextOccurenceActionNameImpl();

  protected abstract void createTrees(@NotNull Map<String, JTree> trees);

  @Nullable
  protected abstract JPanel createLegendPanel();

  protected abstract boolean isApplicableElement(@NotNull PsiElement element);

  protected boolean isApplicableElementForBaseOn(@NotNull PsiElement element) {
    return isApplicableElement(element);
  }

  @Nullable
  protected abstract HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String type, @NotNull PsiElement psiElement);

  @Nullable
  protected abstract Comparator<NodeDescriptor> getComparator();

  @NotNull
  protected abstract String getActionPlace();

  @NotNull
  protected abstract String getBrowserDataKey();

  @Nullable
  protected Color getFileColorForNode(Object node) {
    if (node instanceof HierarchyNodeDescriptor) {
      PsiFile containingFile = ((HierarchyNodeDescriptor) node).getContainingFile();
      return ProjectViewTree.getColorForElement(containingFile);
    }
    return null;
  }

  @NotNull
  protected final JTree createTree(boolean dndAware) {
    final Tree tree;

    DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(""));
    if (dndAware) {
      tree = new DnDAwareTree(treeModel) {
        @Override
        public void addNotify() {
          super.addNotify();
          myRefreshAction.registerShortcutOn(this);
        }

        @Override
        public void removeNotify() {
          super.removeNotify();
          myRefreshAction.unregisterCustomShortcutSet(this);
        }

        @Override
        public boolean isFileColorsEnabled() {
          return ProjectViewTree.isFileColorsEnabledFor(this);
        }

        @Override
        public Color getFileColorFor(Object object) {
          return getFileColorForNode(object);
        }
      };

      if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
        DnDManager.getInstance().registerSource(new DnDSource() {
          @Override
          public boolean canStartDragging(final DnDAction action, final Point dragOrigin) {
            return getSelectedElements().length > 0;
          }

          @Override
          public DnDDragStartBean startDragging(final DnDAction action, final Point dragOrigin) {
            return new DnDDragStartBean(new TransferableWrapper() {
              @Override
              public TreeNode[] getTreeNodes() {
                return tree.getSelectedNodes(TreeNode.class, null);
              }

              @Override
              public PsiElement[] getPsiElements() {
                return getSelectedElements();
              }

              @Override
              public List<File> asFileList() {
                return PsiCopyPasteManager.asFileList(getPsiElements());
              }
            });
          }

          @Override
          public void dragDropEnd() {
          }

          @Override
          public void dropActionChanged(final int gestureModifiers) {
          }
        }, tree);
      }
    }
    else {
      tree = new Tree(treeModel)  {
        @Override
        public void addNotify() {
          super.addNotify();
          myRefreshAction.registerShortcutOn(this);
        }

        @Override
        public void removeNotify() {
          super.removeNotify();
          myRefreshAction.unregisterCustomShortcutSet(this);
        }

        @Override
        public boolean isFileColorsEnabled() {
          return ProjectViewTree.isFileColorsEnabledFor(this);
        }

        @Override
        public Color getFileColorFor(Object object) {
          return getFileColorForNode(object);
        }
      };
    }
    HintUpdateSupply.installDataContextHintUpdateSupply(tree);
    configureTree(tree);
    EditSourceOnDoubleClickHandler.install(tree);
    EditSourceOnEnterKeyHandler.install(tree);
    return tree;
  }

  protected void setHierarchyBase(@NotNull PsiElement element) {
    mySmartPsiElementPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element);
  }

  protected PsiElement getHierarchyBase() {
    return mySmartPsiElementPointer.getElement();
  }

  private void restoreCursor() {
    myCursorAlarm.cancelAllRequests();
    setCursor(Cursor.getDefaultCursor());
  }

  private void setWaitCursor() {
    myCursorAlarm.request();
  }

  public void changeView(@NotNull final String typeName) {
    changeView(typeName, true);
  }

  public void changeView(@NotNull final String typeName, boolean requestFocus) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myCurrentViewType = typeName;

    final PsiElement element = mySmartPsiElementPointer.getElement();
    if (element == null || !isApplicableElement(element)) {
      return;
    }

    if (myContent != null) {
      final String displayName = getContentDisplayName(typeName, element);
      if (displayName != null) {
        myContent.setDisplayName(displayName);
      }
    }

    myCardLayout.show(myTreePanel, typeName);

    Sheet sheet = myType2Sheet.get(typeName);
    if (sheet.myStructureTreeModel == null) {
      try {
        setWaitCursor();
        final JTree tree = sheet.myTree;

        final HierarchyTreeStructure structure = createHierarchyTreeStructure(typeName, element);
        if (structure == null) {
          return;
        }
        Comparator<NodeDescriptor> comparator = getComparator();
        StructureTreeModel myModel = comparator == null ? new StructureTreeModel<>(structure, sheet)
                                                        : new StructureTreeModel<>(structure, comparator, sheet);
        AsyncTreeModel atm = new AsyncTreeModel(myModel, sheet);
        tree.setModel(atm);

        sheet.myStructureTreeModel = myModel;
        sheet.myAsyncTreeModel = atm;
        selectLater(tree, structure.getBaseDescriptor());
        expandLater(tree, structure.getBaseDescriptor());
      }
      finally {
        restoreCursor();
      }
    }

    if (requestFocus) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(getCurrentTree(), true));
    }
  }

  private static boolean isAncestor(@NotNull Project project,
                                    @NotNull HierarchyNodeDescriptor ancestor,
                                    @NotNull HierarchyNodeDescriptor child) {
    PsiElement ancestorElement = ancestor.getPsiElement();
    while (child != null) {
      PsiElement childElement = child.getPsiElement();
      if (PsiManager.getInstance(project).areElementsEquivalent(ancestorElement, childElement)) return true;
      child = (HierarchyNodeDescriptor)child.getParentDescriptor();
    }
    return false;
  }

  private void selectLater(@NotNull JTree tree, @NotNull HierarchyNodeDescriptor descriptor) {
    TreeUtil.promiseSelect(tree, visitor(descriptor));
  }
  private void selectLater(@NotNull JTree tree, @NotNull List<? extends HierarchyNodeDescriptor> descriptors) {
    TreeUtil.promiseSelect(tree, descriptors.stream().map(descriptor -> visitor(descriptor)));
  }
  private void expandLater(@NotNull JTree tree, @NotNull HierarchyNodeDescriptor descriptor) {
    TreeUtil.promiseExpand(tree, visitor(descriptor));
  }

  @NotNull
  private TreeVisitor visitor(@NotNull HierarchyNodeDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) return path -> TreeVisitor.Action.INTERRUPT;
    PsiManager psiManager = element.getManager();
    return path -> {
      Object component = path.getLastPathComponent();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)component;
      Object object = node.getUserObject();
      HierarchyNodeDescriptor current = (HierarchyNodeDescriptor)object;
      PsiElement currentPsiElement = current.getPsiElement();
      if (psiManager.areElementsEquivalent(currentPsiElement, element)) return TreeVisitor.Action.INTERRUPT;
      return isAncestor(myProject, current, descriptor) ? TreeVisitor.Action.CONTINUE : TreeVisitor.Action.SKIP_CHILDREN;
    };
  }

  @Nullable
  protected String getContentDisplayName(@NotNull String typeName, @NotNull PsiElement element) {
    if (element instanceof PsiNamedElement) {
      return MessageFormat.format(typeName, ((PsiNamedElement)element).getName());
    }
    return null;
  }

  @Override
  protected void appendActions(@NotNull DefaultActionGroup actionGroup, @Nullable String helpID) {
    prependActions(actionGroup);
    actionGroup.add(myRefreshAction);
    super.appendActions(actionGroup, helpID);
  }

  protected void prependActions(@NotNull DefaultActionGroup actionGroup) {
  }

  @Override
  public boolean hasNextOccurence() {
    return getOccurrenceNavigator().hasNextOccurence();
  }

  @NotNull
  private OccurenceNavigator getOccurrenceNavigator() {
    String currentViewType = getCurrentViewType();
    if (currentViewType != null) {
      OccurenceNavigator navigator = myType2Sheet.get(currentViewType).myOccurenceNavigator;
      if (navigator != null) {
        return navigator;
      }
    }
    return EMPTY;
  }

  @Override
  public boolean hasPreviousOccurence() {
    return getOccurrenceNavigator().hasPreviousOccurence();
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return getOccurrenceNavigator().goNextOccurence();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return getOccurrenceNavigator().goPreviousOccurence();
  }

  @NotNull
  @Override
  public String getNextOccurenceActionName() {
    return getOccurrenceNavigator().getNextOccurenceActionName();
  }

  @NotNull
  @Override
  public String getPreviousOccurenceActionName() {
    return getOccurrenceNavigator().getPreviousOccurenceActionName();
  }

  @NotNull
  public StructureTreeModel getTreeModel(@NotNull String viewType) {
    return myType2Sheet.get(viewType).myStructureTreeModel;
  }

  @Override
  StructureTreeModel getCurrentBuilder() {
    String viewType = getCurrentViewType();
    if (viewType == null) {
      return null;
    }
    Sheet sheet = myType2Sheet.get(viewType);
    return sheet == null ? null : sheet.myStructureTreeModel;
  }

  final boolean isValidBase() {
    if (myProject.isDisposed()) return false;
    if (PsiDocumentManager.getInstance(myProject).getUncommittedDocuments().length > 0) {
      return myCachedIsValidBase;
    }

    final PsiElement element = mySmartPsiElementPointer.getElement();
    myCachedIsValidBase = element != null && isApplicableElement(element) && element.isValid();
    return myCachedIsValidBase;
  }

  @Override
  protected JTree getCurrentTree() {
    String currentViewType = getCurrentViewType();
    return currentViewType == null ? null : myType2Sheet.get(currentViewType).myTree;
  }

  protected final String getCurrentViewType() {
    return myCurrentViewType;
  }

  @Override
  public Object getData(@NotNull final String dataId) {
    if (getBrowserDataKey().equals(dataId)) {
      return this;
    }
    if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return HELP_ID;
    }
    return super.getData(dataId);
  }

  @Override
  public void dispose() {
    disposeAllSheets();
    super.dispose();
  }

  private void disposeAllSheets() {
    for (final Sheet sheet : myType2Sheet.values()) {
      disposeSheet(sheet);
    }
  }

  private void disposeSheet(@NotNull Sheet sheet) {
    Disposer.dispose(sheet);
    myType2Sheet.put(sheet.myType, new Sheet(sheet.myType, sheet.myTree, sheet.myScope, sheet.myOccurenceNavigator));
  }

  protected void doRefresh(boolean currentBuilderOnly) {
    if (currentBuilderOnly) LOG.assertTrue(getCurrentViewType() != null);

    if (!isValidBase()) return;

    if (getCurrentBuilder() == null) return; // seems like we are in the middle of refresh already

    final String currentViewType = getCurrentViewType();
    List<Object> pathsToExpand = new ArrayList<>();
    List<Object> selectionPaths = new ArrayList<>();
    if (currentViewType != null) {
      Sheet sheet = myType2Sheet.get(currentViewType);
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)sheet.myAsyncTreeModel.getRoot();
      TreeBuilderUtil.storePaths(sheet.myTree, root, pathsToExpand, selectionPaths, true);
    }

    final PsiElement element = mySmartPsiElementPointer.getElement();
    if (element == null || !isApplicableElement(element)) {
      return;
    }
    if (currentBuilderOnly) {
      Sheet sheet = myType2Sheet.get(currentViewType);
      disposeSheet(sheet);
    }
    else {
      disposeAllSheets();
    }
    setHierarchyBase(element);
    validate();
    ApplicationManager.getApplication().invokeLater(() -> {
      changeView(currentViewType);
      for (Object p : pathsToExpand) {
        HierarchyNodeDescriptor descriptor = (HierarchyNodeDescriptor)p;
        expandLater(getCurrentTree(), descriptor);
      }

      selectLater(getCurrentTree(), (List)selectionPaths);
    }, __-> isDisposed());
  }

  protected String getCurrentScopeType() {
    String currentViewType = getCurrentViewType();
    return currentViewType == null ? null : myType2Sheet.get(currentViewType).myScope;
  }

  protected class AlphaSortAction extends ToggleAction {
    public AlphaSortAction() {
      super(IdeBundle.message("action.sort.alphabetically"), IdeBundle.message("action.sort.alphabetically"), AllIcons.ObjectBrowser.Sorted);
    }

    @Override
    public final boolean isSelected(@NotNull final AnActionEvent event) {
      return HierarchyBrowserManager.getSettings(myProject).SORT_ALPHABETICALLY;
    }

    @Override
    public final void setSelected(@NotNull final AnActionEvent event, final boolean flag) {
      HierarchyBrowserManager.getSettings(myProject).SORT_ALPHABETICALLY = flag;
      final Comparator<NodeDescriptor> comparator = getComparator();
      myType2Sheet.values().stream().map(s->s.myStructureTreeModel).filter(m-> m != null).forEach(m->m.setComparator(comparator));
    }

    @Override
    public final void update(@NotNull final AnActionEvent event) {
      super.update(event);
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(isValidBase());
    }
  }

  protected static class BaseOnThisElementAction extends AnAction {
    private final String myBrowserDataKey;
    private final LanguageExtension<HierarchyProvider> myProviderLanguageExtension;

    protected BaseOnThisElementAction(@NotNull String text,
                                      @NotNull String browserDataKey,
                                      @NotNull LanguageExtension<HierarchyProvider> providerLanguageExtension) {
      super(text);
      myBrowserDataKey = browserDataKey;
      myProviderLanguageExtension = providerLanguageExtension;
    }

    @Override
    public final void actionPerformed(@NotNull final AnActionEvent event) {
      final DataContext dataContext = event.getDataContext();
      final HierarchyBrowserBaseEx browser = (HierarchyBrowserBaseEx)dataContext.getData(myBrowserDataKey);
      if (browser == null) return;

      final PsiElement selectedElement = browser.getSelectedElement();
      if (selectedElement == null || !browser.isApplicableElementForBaseOn(selectedElement)) return;

      final String currentViewType = browser.getCurrentViewType();
      Disposer.dispose(browser);
      final HierarchyProvider provider = BrowseHierarchyActionBase.findProvider(
        myProviderLanguageExtension, selectedElement, selectedElement.getContainingFile(), event.getDataContext());
      if (provider != null) {
        HierarchyBrowserBaseEx newBrowser = (HierarchyBrowserBaseEx)BrowseHierarchyActionBase.createAndAddToPanel(
          selectedElement.getProject(), provider, selectedElement);
        ApplicationManager.getApplication().invokeLater(() -> newBrowser.changeView(correctViewType(browser, currentViewType)), __ -> newBrowser.isDisposed());
      }
    }

    protected String correctViewType(@NotNull HierarchyBrowserBaseEx browser, String viewType) {
      return viewType;
    }

    @Override
    public final void update(@NotNull final AnActionEvent event) {
      final Presentation presentation = event.getPresentation();

      final DataContext dataContext = event.getDataContext();
      final HierarchyBrowserBaseEx browser = (HierarchyBrowserBaseEx)dataContext.getData(myBrowserDataKey);
      if (browser == null) {
        presentation.setEnabledAndVisible(false);
        return;
      }

      presentation.setVisible(true);

      final PsiElement selectedElement = browser.getSelectedElement();
      if (selectedElement == null || !browser.isApplicableElementForBaseOn(selectedElement)) {
        presentation.setEnabledAndVisible(false);
      }
      else {
        String typeName = ElementDescriptionUtil.getElementDescription(selectedElement, UsageViewTypeLocation.INSTANCE);
        if (StringUtil.isNotEmpty(typeName)) {
          presentation.setText(IdeBundle.message("action.base.on.this.0", StringUtil.capitalize(typeName)));
        }
        presentation.setEnabled(isEnabled(browser, selectedElement));
      }
    }

    protected boolean isEnabled(@NotNull HierarchyBrowserBaseEx browser, @NotNull PsiElement element) {
      return !element.equals(browser.mySmartPsiElementPointer.getElement()) && element.isValid();
    }
  }

  private class RefreshAction extends com.intellij.ide.actions.RefreshAction {
    RefreshAction() {
      super(IdeBundle.message("action.refresh"), IdeBundle.message("action.refresh"), AllIcons.Actions.Refresh);
    }

    @Override
    public final void actionPerformed(@NotNull final AnActionEvent e) {
      doRefresh(false);
    }

    @Override
    public final void update(@NotNull final AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(isValidBase());
    }
  }

  @NotNull
  private Collection<String> getValidScopeNames() {
    List<String> result = new ArrayList<>();
    result.add(SCOPE_PROJECT);
    result.add(SCOPE_TEST);
    result.add(SCOPE_ALL);
    result.add(SCOPE_CLASS);

    final NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(myProject);
    for (NamedScopesHolder holder : holders) {
      NamedScope[] scopes = holder.getEditableScopes(); //predefined scopes already included
      for (NamedScope scope : scopes) {
        result.add(scope.getName());
      }
    }
    return result;
  }

  public class ChangeScopeAction extends ComboBoxAction {
    @Override
    public final void update(@NotNull final AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      final Project project = e.getProject();
      if (project == null) return;
      presentation.setEnabled(isEnabled());
      presentation.setText(getCurrentScopeType());
    }

    protected boolean isEnabled(){
      return true;
    }

    @Override
    @NotNull
    protected final DefaultActionGroup createPopupActionGroup(final JComponent button) {
      final DefaultActionGroup group = new DefaultActionGroup();

      for(String name: getValidScopeNames()) {
        group.add(new MenuAction(name));
      }

      group.add(new ConfigureScopesAction());

      return group;
    }

    private void selectScope(@NotNull String scopeType) {
      myType2Sheet.get(getCurrentViewType()).myScope =  scopeType;
      HierarchyBrowserManager.getSettings(myProject).SCOPE = scopeType;

      // invokeLater is called to update state of button before long tree building operation
      // scope is kept per type so other builders don't need to be refreshed
      ApplicationManager.getApplication().invokeLater(() -> doRefresh(true), __ -> isDisposed());
    }

    @NotNull
    @Override
    public final JComponent createCustomComponent(@NotNull final Presentation presentation, @NotNull String place) {
      final JPanel panel = new JPanel(new GridBagLayout());
      panel.add(new JLabel(IdeBundle.message("label.scope")),
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, JBUI.insetsLeft(5), 0, 0));
      panel.add(super.createCustomComponent(presentation, place),
                new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));
      return panel;
    }

    private final class MenuAction extends AnAction {
      private final String myScopeType;

      MenuAction(@NotNull String scopeType) {
        super(scopeType);
        myScopeType = scopeType;
      }

      @Override
      public final void actionPerformed(@NotNull final AnActionEvent e) {
        selectScope(myScopeType);
      }
    }

    private final class ConfigureScopesAction extends AnAction {
      private ConfigureScopesAction() {
        super("Configure...");
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        EditScopesDialog.showDialog(myProject, null);
        if (!getValidScopeNames().contains(getCurrentScopeType())) {
          selectScope(SCOPE_ALL);
        }
      }
    }
  }
}