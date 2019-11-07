// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.structureView.newStructureView;

import com.intellij.ide.CopyPasteDelegator;
import com.intellij.ide.DataManager;
import com.intellij.ide.PsiCopyPasteManager;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.structureView.*;
import com.intellij.ide.structureView.customRegions.CustomRegionTreeElement;
import com.intellij.ide.structureView.impl.StructureViewFactoryImpl;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.ide.util.FileStructurePopup;
import com.intellij.ide.util.treeView.*;
import com.intellij.ide.util.treeView.smartTree.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.impl.IdeMouseEventDispatcher;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.*;
import com.intellij.ui.popup.HintUpdateSupply;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure;
import com.intellij.util.*;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeModelAdapter;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.CancellablePromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StructureViewComponent extends SimpleToolWindowPanel implements TreeActionsOwner, DataProvider, StructureView.Scrollable {
  private static final Logger LOG = Logger.getInstance(StructureViewComponent.class);

  private static final Key<TreeState> STRUCTURE_VIEW_STATE_KEY = Key.create("STRUCTURE_VIEW_STATE");
  private static final Key<Boolean> STRUCTURE_VIEW_STATE_RESTORED_KEY = Key.create("STRUCTURE_VIEW_STATE_RESTORED_KEY");
  private static final AtomicInteger ourSettingsModificationCount = new AtomicInteger();

  private FileEditor myFileEditor;
  private final TreeModelWrapper myTreeModelWrapper;

  private final Project myProject;
  private final StructureViewModel myTreeModel;

  private final Tree myTree;
  private final SmartTreeStructure myTreeStructure;

  private final StructureTreeModel<SmartTreeStructure> myStructureTreeModel;
  private final AsyncTreeModel myAsyncTreeModel;
  private final SingleAlarm myUpdateAlarm;

  private volatile AsyncPromise<TreePath> myCurrentFocusPromise;

  private boolean myAutoscrollFeedback;
  private volatile boolean myDisposed;
  private boolean myStoreStateDisabled;

  private final Alarm myAutoscrollAlarm = new Alarm(this);

  private final CopyPasteDelegator myCopyPasteDelegator;
  private final MyAutoScrollToSourceHandler myAutoScrollToSourceHandler;
  private final AutoScrollFromSourceHandler myAutoScrollFromSourceHandler;

  // read from different threads
  // written from EDT only
  @Nullable
  private volatile CancellablePromise<?> myLastAutoscrollPromise;


  public StructureViewComponent(@Nullable FileEditor editor,
                                @NotNull StructureViewModel structureViewModel,
                                @NotNull Project project,
                                boolean showRootNode) {
    super(true, true);

    myProject = project;
    myFileEditor = editor;
    myTreeModel = structureViewModel;
    myTreeModelWrapper = new TreeModelWrapper(myTreeModel, this);

    myTreeStructure = new SmartTreeStructure(project, myTreeModelWrapper) {
      @Override
      public void rebuildTree() {
        if (isDisposed()) return;
        super.rebuildTree();
      }

      @Override
      public boolean isToBuildChildrenInBackground(@NotNull final Object element) {
        return Registry.is("ide.structureView.StructureViewTreeStructure.BuildChildrenInBackground") ||
               getRootElement() == element;
      }

      @NotNull
      @Override
      protected TreeElementWrapper createTree() {
        return new MyNodeWrapper(myProject, myModel.getRoot(), myModel);
      }

      @Override
      public String toString() {
        return "structure view tree structure(model=" + myTreeModel + ")";
      }
    };

    myStructureTreeModel = new StructureTreeModel<>(myTreeStructure, this);
    myAsyncTreeModel = new AsyncTreeModel(myStructureTreeModel, this);
    myAsyncTreeModel.setRootImmediately(myStructureTreeModel.getRootImmediately());
    myTree = new MyTree(myAsyncTreeModel);

    Disposer.register(this, myTreeModelWrapper);

    registerAutoExpandListener(myTree, myTreeModel);

    myUpdateAlarm = new SingleAlarm(this::rebuild, 200, this);
    myTree.setRootVisible(showRootNode);
    myTree.getEmptyText().setText("Structure is empty");

    final ModelListener modelListener = () -> queueUpdate();
    myTreeModelWrapper.addModelListener(modelListener);

    Disposer.register(this, () -> {
      storeState();
      myTreeModelWrapper.removeModelListener(modelListener);
    });

    setContent(ScrollPaneFactory.createScrollPane(myTree));

    myAutoScrollToSourceHandler = new MyAutoScrollToSourceHandler();
    myAutoScrollFromSourceHandler = new MyAutoScrollFromSourceHandler(myProject, this);
    myCopyPasteDelegator = new CopyPasteDelegator(myProject, myTree);

    setToolbar(createToolbar());
    setupTree();
  }

  public static void registerAutoExpandListener(@NotNull JTree tree, @NotNull StructureViewModel structureViewModel) {
    tree.getModel().addTreeModelListener(new MyExpandListener(
      tree, ObjectUtils.tryCast(structureViewModel, StructureViewModel.ExpandInfoProvider.class)));
  }

  protected boolean showScrollToFromSourceActions() {
    return true;
  }

  @Override
  public FileEditor getFileEditor() {
    return myFileEditor;
  }

  private StructureViewFactoryImpl.State getSettings() {
    return ((StructureViewFactoryImpl)StructureViewFactory.getInstance(myProject)).getState();
  }

  public void showToolbar() {
    setToolbar(createToolbar());
  }

  @NotNull
  private JComponent createToolbar() {
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.STRUCTURE_VIEW_TOOLBAR, createActionGroup(), true).getComponent();
  }

  private void setupTree() {
    myTree.setCellRenderer(new NodeRenderer());
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    myTree.setShowsRootHandles(true);
    registerPsiListener(myProject, this, this::queueUpdate);
    myProject.getMessageBus().connect(this).subscribe(UISettingsListener.TOPIC, o -> rebuild());

    if (showScrollToFromSourceActions()) {
      myAutoScrollToSourceHandler.install(myTree);
      myAutoScrollFromSourceHandler.install();
    }

    TreeUtil.installActions(getTree());

    new TreeSpeedSearch(getTree(), treePath -> {
      Object userObject = TreeUtil.getLastUserObject(treePath);
      return userObject != null ? FileStructurePopup.getSpeedSearchText(userObject) : null;
    });

    addTreeKeyListener();
    addTreeMouseListeners();
    restoreState();
  }

  public static void registerPsiListener(@NotNull Project project, @NotNull Disposable disposable, @NotNull Runnable onChange) {
    MyPsiTreeChangeListener psiListener = new MyPsiTreeChangeListener(
      PsiManager.getInstance(project).getModificationTracker(), onChange);
    PsiManager.getInstance(project).addPsiTreeChangeListener(psiListener, disposable);
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public JTree getTree() {
    return myTree;
  }

  public void queueUpdate() {
    myUpdateAlarm.cancelAndRequest();
  }

  public void rebuild() {
    myStructureTreeModel.getInvoker().invoke(() -> {
      UIUtil.putClientProperty(myTree, STRUCTURE_VIEW_STATE_RESTORED_KEY, null);
      myTreeStructure.rebuildTree();
      myStructureTreeModel.invalidate();
    });
  }

  @NotNull
  private static JBTreeTraverser<Object> traverser() {
    return JBTreeTraverser.from(o -> o instanceof Group ? ((Group)o).getChildren() : null);
  }

  private JBIterable<Object> getSelectedValues() {
    return getSelectedValues(getTree());
  }

  @NotNull
  public static JBIterable<Object> getSelectedValues(JTree tree) {
    return traverser()
      .withRoots(JBIterable.of(tree.getSelectionPaths())
                           .map(TreePath::getLastPathComponent)
                           .filterMap(StructureViewComponent::unwrapValue))
      .traverse();
  }

  private void addTreeMouseListeners() {
    EditSourceOnDoubleClickHandler.install(getTree());
    CustomizationUtil.installPopupHandler(getTree(), IdeActions.GROUP_STRUCTURE_VIEW_POPUP, ActionPlaces.STRUCTURE_VIEW_POPUP);
  }

  private void addTreeKeyListener() {
    getTree().addKeyListener(
      new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (KeyEvent.VK_ENTER == e.getKeyCode()) {
            DataContext dataContext = DataManager.getInstance().getDataContext(getTree());
            OpenSourceUtil.openSourcesFrom(dataContext, false);
          }
          else if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
            if (e.isConsumed()) return;
            PsiCopyPasteManager copyPasteManager = PsiCopyPasteManager.getInstance();
            boolean[] isCopied = new boolean[1];
            if (copyPasteManager.getElements(isCopied) != null && !isCopied[0]) {
              copyPasteManager.clear();
              e.consume();
            }
          }
        }
      });
  }

  @Override
  public void storeState() {
    if (isDisposed() || !myProject.isOpen() || myStoreStateDisabled) return;
    Object root = myTree.getModel().getRoot();
    if (root == null) return;
    TreeState state = TreeState.createOn(myTree, new TreePath(root));
    if (myFileEditor != null) {
      myFileEditor.putUserData(STRUCTURE_VIEW_STATE_KEY, state);
    }
    UIUtil.putClientProperty(myTree, STRUCTURE_VIEW_STATE_RESTORED_KEY, null);
  }

  @Override
  public void disableStoreState() {
    myStoreStateDisabled = true;
  }

  @Override
  public void restoreState() {
    TreeState state = myFileEditor == null ? null : myFileEditor.getUserData(STRUCTURE_VIEW_STATE_KEY);
    if (state == null) {
      if (!Boolean.TRUE.equals(UIUtil.getClientProperty(myTree, STRUCTURE_VIEW_STATE_RESTORED_KEY))) {
        TreeUtil.expand(getTree(), getMinimumExpandDepth(myTreeModel));
      }
    }
    else {
      UIUtil.putClientProperty(myTree, STRUCTURE_VIEW_STATE_RESTORED_KEY, true);
      state.applyTo(myTree);
      if (myFileEditor != null) {
        myFileEditor.putUserData(STRUCTURE_VIEW_STATE_KEY, null);
      }
    }
  }

  @NotNull
  protected ActionGroup createActionGroup() {
    DefaultActionGroup result = new DefaultActionGroup();
    Sorter[] sorters = myTreeModel.getSorters();
    for (final Sorter sorter : sorters) {
      if (sorter.isVisible()) {
        result.add(new TreeActionWrapper(sorter, this));
      }
    }
    if (sorters.length > 0) {
      result.addSeparator();
    }

    addGroupByActions(result);

    Filter[] filters = myTreeModel.getFilters();
    for (Filter filter : filters) {
      result.add(new TreeActionWrapper(filter, this));
    }
    if (myTreeModel instanceof ProvidingTreeModel) {
      final Collection<NodeProvider> providers = ((ProvidingTreeModel)myTreeModel).getNodeProviders();
      for (NodeProvider provider : providers) {
        result.add(new TreeActionWrapper(provider, this));
      }
    }

    if (showScrollToFromSourceActions()) {
      result.addSeparator();

      result.add(myAutoScrollToSourceHandler.createToggleAction());
      result.add(myAutoScrollFromSourceHandler.createToggleAction());
    }
    return result;
  }

  protected void addGroupByActions(@NotNull DefaultActionGroup result) {
    Grouper[] groupers = myTreeModel.getGroupers();
    for (Grouper grouper : groupers) {
      result.add(new TreeActionWrapper(grouper, this));
    }
  }

  public Promise<AbstractTreeNode> expandPathToElement(Object element) {
    return expandSelectFocusInner(element, false, false)
      .then(p -> TreeUtil.getLastUserObject(AbstractTreeNode.class, p));
  }

  @NotNull
  public Promise<TreePath> select(Object element, boolean requestFocus) {
    return expandSelectFocusInner(element, true, requestFocus);
  }

  @NotNull
  private Promise<TreePath> expandSelectFocusInner(Object element, boolean select, boolean requestFocus) {
    AsyncPromise<TreePath> result = myCurrentFocusPromise = new AsyncPromise<>();
    int[] stage = {1, 0}; // 1 - first pass, 2 - optimization applied, 3 - retry w/o optimization
    TreePath[] deepestPath = {null};
    TreeVisitor visitor = path -> {
      if (myCurrentFocusPromise != result) {
        result.setError("rejected");
        return TreeVisitor.Action.INTERRUPT;
      }
      Object last = path.getLastPathComponent();
      Object userObject = unwrapNavigatable(last);
      Object value = unwrapValue(last);
      if (Comparing.equal(value, element) ||
          userObject instanceof AbstractTreeNode && ((AbstractTreeNode)userObject).canRepresent(element)) {
        return TreeVisitor.Action.INTERRUPT;
      }
      if (value instanceof PsiElement && element instanceof PsiElement) {
        if (PsiTreeUtil.isAncestor((PsiElement)value, (PsiElement)element, true)) {
          int count = path.getPathCount();
          if (stage[1] == 0 || stage[1] < count) {
            stage[1] = count;
            deepestPath[0] = path;
          }
        }
        else if (stage[0] != 3) {
          stage[0] = 2;
          return TreeVisitor.Action.SKIP_CHILDREN;
        }
      }
      return TreeVisitor.Action.CONTINUE;
    };
    Function<TreePath, Promise<TreePath>> action = path -> {
      if (select) {
        TreeUtil.selectPath(myTree, path);
      }
      else {
        myTree.expandPath(path);
      }
      if (requestFocus) {
        IdeFocusManager.getInstance(myProject).requestFocus(myTree, false);
      }
      return Promises.resolvedPromise(path);
    };
    Function<TreePath, Promise<TreePath>> fallback = new Function<TreePath, Promise<TreePath>>() {
      @Override
      public Promise<TreePath> fun(TreePath path) {
        if (myCurrentFocusPromise != result) {
          result.setError("rejected");
          return Promises.rejectedPromise();
        }
        else if (path == null && stage[0] == 2) {
          // Some structure views merge unrelated psi elements into a structure node (MarkdownStructureViewModel).
          // So turn off the isAncestor() optimization and retry once.
          stage[0] = 3;
          return myAsyncTreeModel.accept(visitor).thenAsync(this);
        }
        else {
          TreePath adjusted = path == null ? deepestPath[0] : path;
          return adjusted == null ? Promises.rejectedPromise() : action.fun(adjusted);
        }
      }
    };
    myAsyncTreeModel.accept(visitor).thenAsync(fallback).processed(result);
    return myCurrentFocusPromise;
  }

  private void scrollToSelectedElement() {
    if (myAutoscrollFeedback) {
      myAutoscrollFeedback = false;
      return;
    }

    if (!getSettings().AUTOSCROLL_FROM_SOURCE) {
      return;
    }

    cancelScrollToSelectedElement();
    myAutoscrollAlarm.cancelAllRequests();
    myAutoscrollAlarm.addRequest(this::scrollToSelectedElementLater, 1000);
  }

  private void cancelScrollToSelectedElement() {
    final CancellablePromise<?> lastPromise = myLastAutoscrollPromise;
    if (lastPromise != null && !lastPromise.isCancelled()) {
      lastPromise.cancel();
    }
  }

  private void scrollToSelectedElementLater() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    cancelScrollToSelectedElement();
    if (isDisposed()) return;

    myLastAutoscrollPromise = ReadAction.nonBlocking(this::doFindSelectedElement)
      .withDocumentsCommitted(myProject)
      .expireWith(this)
      .finishOnUiThread(ModalityState.current(), this::doScrollToSelectedElement)
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  @Nullable
  private Object doFindSelectedElement() {
    try {
      return myTreeModel.getCurrentEditorElement();
    }
    catch (IndexNotReadyException ignore) {
    }
    return null;
  }

  private void doScrollToSelectedElement(@Nullable Object currentEditorElement) {
    if (currentEditorElement == null) return;
    if (UIUtil.isFocusAncestor(this)) return;
    select(currentEditorElement, false);
  }

  @Override
  public void dispose() {
    LOG.assertTrue(EventQueue.isDispatchThread(), Thread.currentThread().getName());
    myDisposed = true;
    myFileEditor = null;
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public void centerSelectedRow() {
    TreePath path = getTree().getSelectionPath();
    if (path == null) return;

    myAutoScrollToSourceHandler.setShouldAutoScroll(false);
    TreeUtil.showRowCentered(getTree(), getTree().getRowForPath(path), false);
    myAutoScrollToSourceHandler.setShouldAutoScroll(true);
  }

  @Override
  public void setActionActive(String name, boolean state) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    storeState();
    StructureViewFactoryEx.getInstanceEx(myProject).setActiveAction(name, state);
    ourSettingsModificationCount.incrementAndGet();

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      waitForRebuildAndExpand();
    }
    else {
      rebuild();
      TreeUtil.expand(getTree(), getMinimumExpandDepth(myTreeModel));
    }
  }

  @SuppressWarnings("TestOnlyProblems")
  private void waitForRebuildAndExpand() {
    wait(rebuildAndUpdate());
    UIUtil.dispatchAllInvocationEvents();
    wait(TreeUtil.promiseExpand(getTree(), getMinimumExpandDepth(myTreeModel)));
  }

  private static void wait(Promise<?> originPromise) {
    AtomicBoolean complete = new AtomicBoolean(false);
    Promise<?> promise = originPromise.onProcessed(ignore -> complete.set(true));
    while (!complete.get()) {
      //noinspection TestOnlyProblems
      UIUtil.dispatchAllInvocationEvents();
      try {
        promise.blockingGet(10, TimeUnit.MILLISECONDS);
      }
      catch (Exception ignore) {
      }
    }
  }

  @Override
  public boolean isActionActive(String name) {
    return !myProject.isDisposed() && StructureViewFactoryEx.getInstanceEx(myProject).isActionActive(name);
  }

  public static void clearStructureViewState(Project project) {
    for (FileEditor editor : FileEditorManager.getInstance(project).getAllEditors()) {
      editor.putUserData(STRUCTURE_VIEW_STATE_KEY, null);
    }
  }

  private final class MyAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
    private boolean myShouldAutoScroll = true;

    void setShouldAutoScroll(boolean shouldAutoScroll) {
      myShouldAutoScroll = shouldAutoScroll;
    }

    @Override
    protected boolean isAutoScrollMode() {
      return myShouldAutoScroll && !myProject.isDisposed()
             && getSettings().AUTOSCROLL_MODE;
    }

    @Override
    protected void setAutoScrollMode(boolean state) {
      getSettings().AUTOSCROLL_MODE = state;
    }

    @Override
    protected void scrollToSource(Component tree) {
      if (isDisposed()) return;
      myAutoscrollFeedback = true;

      Navigatable navigatable = CommonDataKeys.NAVIGATABLE.getData(DataManager.getInstance().getDataContext(getTree()));
      if (myFileEditor != null && navigatable != null && navigatable.canNavigateToSource()) {
        navigatable.navigate(false);
      }
    }
  }

  private class MyAutoScrollFromSourceHandler extends AutoScrollFromSourceHandler {
    private FileEditorPositionListener myFileEditorPositionListener;

    private MyAutoScrollFromSourceHandler(Project project, Disposable parentDisposable) {
      super(project, getTree(), parentDisposable);
    }

    @Override
    protected void selectElementFromEditor(@NotNull FileEditor editor) {
    }

    @Override
    public void install() {
      addEditorCaretListener();
    }

    @Override
    public void dispose() {
      super.dispose();
      if (myFileEditorPositionListener != null) {
        myTreeModel.removeEditorPositionListener(myFileEditorPositionListener);
      }
    }

    private void addEditorCaretListener() {
      myFileEditorPositionListener = () -> scrollToSelectedElement();
      myTreeModel.addEditorPositionListener(myFileEditorPositionListener);

      if (isAutoScrollEnabled()) {
        //otherwise on any tab switching selection will be staying at the top file node until we made a first caret move
        scrollToSelectedElement();
      }
    }

    @Override
    protected boolean isAutoScrollEnabled() {
      return getSettings().AUTOSCROLL_FROM_SOURCE;
    }

    @Override
    protected void setAutoScrollEnabled(boolean state) {
      getSettings().AUTOSCROLL_FROM_SOURCE = state;
      final FileEditor[] selectedEditors = FileEditorManager.getInstance(myProject).getSelectedEditors();
      if (selectedEditors.length > 0 && state) {
        scrollToSelectedElementLater();
      }
    }
  }

  @Override
  public Object getData(@NotNull String dataId) {
    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      PsiElement element = getSelectedValues().filter(PsiElement.class).single();
      return element != null && element.isValid() ? element : null;
    }
    if (LangDataKeys.PSI_ELEMENT_ARRAY.is(dataId)) {
      return PsiUtilCore.toPsiElementArray(getSelectedValues().filter(PsiElement.class).toList());
    }
    if (PlatformDataKeys.FILE_EDITOR.is(dataId)) {
      return myFileEditor;
    }
    if (PlatformDataKeys.CUT_PROVIDER.is(dataId)) {
      return myCopyPasteDelegator.getCutProvider();
    }
    if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
      return myCopyPasteDelegator.getCopyProvider();
    }
    if (PlatformDataKeys.PASTE_PROVIDER.is(dataId)) {
      return myCopyPasteDelegator.getPasteProvider();
    }
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      List<Object> list = JBIterable.of(getTree().getSelectionPaths())
                                    .map(TreePath::getLastPathComponent)
                                    .map(StructureViewComponent::unwrapNavigatable)
                                    .toList();
      Object[] selectedElements = list.isEmpty() ? null : ArrayUtil.toObjectArray(list);
      if (selectedElements == null || selectedElements.length == 0) return null;
      if (selectedElements[0] instanceof Navigatable) {
        return selectedElements[0];
      }
    }
    if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return getHelpID();
    }
    if (CommonDataKeys.PROJECT.is(dataId)) {
      return myProject;
    }
    return super.getData(dataId);
  }

  @Override
  @NotNull
  public StructureViewModel getTreeModel() {
    return myTreeModel;
  }

  @Override
  public boolean navigateToSelectedElement(boolean requestFocus) {
    select(myTreeModel.getCurrentEditorElement(), requestFocus);
    return true;
  }

  @TestOnly
  public AsyncPromise<Void> rebuildAndUpdate() {
    AsyncPromise<Void> result = new AsyncPromise<>();
    rebuild();
    TreeVisitor visitor = path -> {
      AbstractTreeNode node = TreeUtil.getLastUserObject(AbstractTreeNode.class, path);
      if (node != null) node.update();
      return TreeVisitor.Action.CONTINUE;
    };
    myAsyncTreeModel.accept(visitor).onProcessed(ignore -> result.setResult(null));
    return result;
  }

  public String getHelpID() {
    return HelpID.STRUCTURE_VIEW;
  }

  @Override
  public Dimension getCurrentSize() {
    return getTree().getSize();
  }

  @Override
  public void setReferenceSizeWhileInitializing(Dimension size) {
    //_setRefSize(size);
    //
    //if (size != null) {
    //  todo com.intellij.ui.tree.AsyncTreeModelTest.invokeWhenProcessingDone() //
    //  myAbstractTreeBuilder.getReady(this).doWhenDone(() -> _setRefSize(null));
    //}
  }

  //private void _setRefSize(Dimension size) {
  //  JTree tree = getTree();
  //  tree.setPreferredSize(size);
  //  tree.setMinimumSize(size);
  //  tree.setMaximumSize(size);
  //
  //  tree.revalidate();
  //  tree.repaint();
  //}

  private static int getMinimumExpandDepth(@NotNull StructureViewModel structureViewModel) {
    final StructureViewModel.ExpandInfoProvider provider =
      ObjectUtils.tryCast(structureViewModel, StructureViewModel.ExpandInfoProvider.class);

    return provider == null ? 2 : provider.getMinimumAutoExpandDepth();
  }

  private static class MyNodeWrapper extends TreeElementWrapper
    implements NodeDescriptorProvidingKey, ValidateableNode {

    private long childrenStamp = -1;
    private int modificationCountForChildren = ourSettingsModificationCount.get();

    MyNodeWrapper(Project project, @NotNull TreeElement value, @NotNull TreeModel treeModel) {
      super(project, value, treeModel);
    }

    @Override
    @NotNull
    public Object getKey() {
      StructureViewTreeElement element = (StructureViewTreeElement)getValue();
      if (element instanceof NodeDescriptorProvidingKey) return ((NodeDescriptorProvidingKey)element).getKey();
      Object value = element == null ? null : element.getValue();
      return value == null ? this : value;
    }

    @Override
    @NotNull
    public Collection<AbstractTreeNode> getChildren() {
      if (ourSettingsModificationCount.get() != modificationCountForChildren) {
        resetChildren();
        modificationCountForChildren = ourSettingsModificationCount.get();
      }

      Object o = unwrapElement(getValue());
      long currentStamp = -1;
      if (o instanceof PsiElement) {
        if (!((PsiElement)o).isValid()) return Collections.emptyList();

        PsiFile file = ((PsiElement)o).getContainingFile();
        if (file != null) {
          currentStamp = file.getModificationStamp();
        }
      }
      else if (o instanceof ModificationTracker) {
        currentStamp = ((ModificationTracker)o).getModificationCount();
      }
      if (childrenStamp != currentStamp) {
        resetChildren();
        childrenStamp = currentStamp;
      }
      try {
        return super.getChildren();
      }
      catch (IndexNotReadyException ignore) {
        return Collections.emptyList();
      }
    }

    @Override
    public boolean isAlwaysShowPlus() {
      StructureViewModel.ElementInfoProvider elementInfoProvider = getElementInfoProvider();
      return elementInfoProvider == null || elementInfoProvider.isAlwaysShowsPlus((StructureViewTreeElement)getValue());
    }

    @Override
    public boolean isAlwaysLeaf() {
      StructureViewModel.ElementInfoProvider elementInfoProvider = getElementInfoProvider();
      return elementInfoProvider != null && elementInfoProvider.isAlwaysLeaf((StructureViewTreeElement)getValue());
    }

    @Nullable
    private StructureViewModel.ElementInfoProvider getElementInfoProvider() {
      if (myTreeModel instanceof StructureViewModel.ElementInfoProvider) {
        return (StructureViewModel.ElementInfoProvider)myTreeModel;
      }
      if (myTreeModel instanceof TreeModelWrapper) {
        StructureViewModel model = ((TreeModelWrapper)myTreeModel).getModel();
        if (model instanceof StructureViewModel.ElementInfoProvider) {
          return (StructureViewModel.ElementInfoProvider)model;
        }
      }

      return null;
    }

    @NotNull
    @Override
    protected TreeElementWrapper createChildNode(@NotNull TreeElement child) {
      return new MyNodeWrapper(myProject, child, myTreeModel);
    }

    @NotNull
    @Override
    protected GroupWrapper createGroupWrapper(Project project, @NotNull Group group, @NotNull final TreeModel treeModel) {
      return new MyGroupWrapper(project, group, treeModel);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof MyNodeWrapper) {
        return Comparing.equal(unwrapElement(getValue()), unwrapElement(((MyNodeWrapper)o).getValue()));
      }
      if (o instanceof StructureViewTreeElement) {
        return Comparing.equal(unwrapElement(getValue()), ((StructureViewTreeElement)o).getValue());
      }
      return false;
    }

    @Override
    public boolean isValid() {
      TreeElement value = getValue();
      PsiTreeElementBase psi = value instanceof PsiTreeElementBase ? (PsiTreeElementBase)value : null;
      return psi == null || psi.isValid();
    }

    @Override
    public int hashCode() {
      final Object o = unwrapElement(getValue());

      return o != null ? o.hashCode() : 0;
    }
  }

  private static class MyGroupWrapper extends GroupWrapper {
    MyGroupWrapper(Project project, @NotNull Group group, @NotNull TreeModel treeModel) {
      super(project, group, treeModel);
    }

    @NotNull
    @Override
    protected TreeElementWrapper createChildNode(@NotNull TreeElement child) {
      return new MyNodeWrapper(getProject(), child, myTreeModel);
    }


    @NotNull
    @Override
    protected GroupWrapper createGroupWrapper(Project project, @NotNull Group group, @NotNull TreeModel treeModel) {
      return new MyGroupWrapper(project, group, treeModel);
    }

    @Override
    public boolean isAlwaysShowPlus() {
      return true;
    }
  }

  private static class MyTree extends DnDAwareTree implements PlaceProvider<String> {
    MyTree(javax.swing.tree.TreeModel model) {
      super(model);
      HintUpdateSupply.installDataContextHintUpdateSupply(this);
    }

    @Override
    public String getPlace() {
      return ActionPlaces.STRUCTURE_VIEW_TOOLBAR;
    }

    @Override
    public void processMouseEvent(MouseEvent event) {
      IdeMouseEventDispatcher.requestFocusInNonFocusedWindow(event);
      super.processMouseEvent(event);
    }
  }

  private static class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
    final PsiModificationTracker modTracker;
    long prevModCount;
    final Runnable onChange;

    private MyPsiTreeChangeListener(PsiModificationTracker modTracker, Runnable onChange) {
      this.modTracker = modTracker;
      this.onChange = onChange;
      prevModCount = modTracker.getModificationCount();
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
      PsiElement child = event.getOldChild();
      if (child instanceof PsiWhiteSpace) return; //optimization

      childrenChanged();
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
      PsiElement child = event.getNewChild();
      if (child instanceof PsiWhiteSpace) return; //optimization
      childrenChanged();
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
      PsiElement oldChild = event.getOldChild();
      PsiElement newChild = event.getNewChild();
      if (oldChild instanceof PsiWhiteSpace && newChild instanceof PsiWhiteSpace) return; //optimization
      childrenChanged();
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
      childrenChanged();
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
      childrenChanged();
    }

    private void childrenChanged() {
      long newModificationCount = modTracker.getModificationCount();
      if (newModificationCount == prevModCount) return;
      prevModCount = newModificationCount;
      onChange.run();
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
      childrenChanged();
    }
  }

  public static Object unwrapValue(Object o) {
    return unwrapElement(unwrapWrapper(o));
  }

  @Nullable
  public static Object unwrapNavigatable(Object o) {
    Object p = TreeUtil.getUserObject(o);
    return p instanceof FilteringTreeStructure.FilteringNode ? ((FilteringTreeStructure.FilteringNode)p).getDelegate() : p;
  }

  public static Object unwrapWrapper(Object o) {
    Object p = unwrapNavigatable(o);
    return p instanceof MyNodeWrapper ? ((MyNodeWrapper)p).getValue() :
           p instanceof MyGroupWrapper ? ((MyGroupWrapper)p).getValue() : p;
  }

  private static Object unwrapElement(Object o) {
    return o instanceof StructureViewTreeElement ? ((StructureViewTreeElement)o).getValue() : o;
  }

  // for FileStructurePopup only
  @NotNull
  public static TreeElementWrapper createWrapper(@NotNull Project project, @NotNull TreeElement value, TreeModel treeModel) {
    return new MyNodeWrapper(project, value, treeModel);
  }

  private static class MyExpandListener extends TreeModelAdapter {
    private static final RegistryValue autoExpandDepth = Registry.get("ide.tree.autoExpandMaxDepth");

    private final JTree tree;
    final StructureViewModel.ExpandInfoProvider provider;
    final boolean smartExpand;

    MyExpandListener(@NotNull JTree tree, @Nullable StructureViewModel.ExpandInfoProvider provider) {
      this.tree = tree;
      this.provider = provider;
      smartExpand = provider != null && provider.isSmartExpand();
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
      TreePath parentPath = e.getTreePath();
      if (Boolean.TRUE.equals(UIUtil.getClientProperty(tree, STRUCTURE_VIEW_STATE_RESTORED_KEY))) return;
      if (parentPath == null || parentPath.getPathCount() > autoExpandDepth.asInteger() - 1) return;
      Object[] children = e.getChildren();
      if (smartExpand && children.length == 1) {
        expandLater(parentPath, children[0]);
      }
      else {
        for (Object o : children) {
          NodeDescriptor descriptor = TreeUtil.getUserObject(NodeDescriptor.class, o);
          if (descriptor != null && isAutoExpandNode(descriptor)) {
            expandLater(parentPath, o);
          }
        }
      }
    }

    void expandLater(@NotNull TreePath parentPath, @NotNull Object o) {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (!tree.isVisible(parentPath) || !tree.isExpanded(parentPath)) return;
        tree.expandPath(parentPath.pathByAddingChild(o));
      });
    }

    boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
      if (provider != null) {
        Object value = unwrapWrapper(nodeDescriptor.getElement());
        if (value instanceof CustomRegionTreeElement) {
          return true;
        }
        else if (value instanceof StructureViewTreeElement) {
          return provider.isAutoExpand((StructureViewTreeElement)value);
        }
        else if (value instanceof GroupWrapper) {
          Group group = ObjectUtils.notNull(((GroupWrapper)value).getValue());
          for (TreeElement treeElement : group.getChildren()) {
            if (treeElement instanceof StructureViewTreeElement && !provider.isAutoExpand((StructureViewTreeElement)treeElement)) {
              return false;
            }
          }
        }
      }
      // expand root node & its immediate children
      NodeDescriptor parent = nodeDescriptor.getParentDescriptor();
      return parent == null || parent.getParentDescriptor() == null;
    }
  }
}
