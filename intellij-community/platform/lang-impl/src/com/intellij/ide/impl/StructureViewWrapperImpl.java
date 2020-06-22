// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.impl;

import com.intellij.ide.ActivityTracker;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.structureView.*;
import com.intellij.ide.structureView.impl.StructureViewComposite;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.lang.LangBundle;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.PersistentFSConstants;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.content.*;
import com.intellij.util.BitUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.TimerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

/**
 * @author Eugene Belyaev
 */
public final class StructureViewWrapperImpl implements StructureViewWrapper, Disposable {
  public static final Topic<Runnable> STRUCTURE_CHANGED = new Topic<>("structure view changed", Runnable.class);
  private static final Logger LOG = Logger.getInstance(StructureViewWrapperImpl.class);
  private static final DataKey<StructureViewWrapper> WRAPPER_DATA_KEY = DataKey.create("WRAPPER_DATA_KEY");
  private static final int REFRESH_TIME = 100; // time to check if a context file selection is changed or not
  private static final int REBUILD_TIME = 100; // time to wait and merge requests to rebuild a tree model

  private final Project myProject;
  private final ToolWindow myToolWindow;

  private VirtualFile myFile;

  private StructureView myStructureView;
  private FileEditor myFileEditor;
  private ModuleStructureComponent myModuleStructureComponent;

  private JPanel[] myPanels = new JPanel[0];
  private final MergingUpdateQueue myUpdateQueue;

  private Runnable myPendingSelection;
  private boolean myFirstRun = true;
  private int myActivityCount;

  public StructureViewWrapperImpl(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    myProject = project;
    myToolWindow = toolWindow;
    JComponent component = toolWindow.getComponent();

    //noinspection TestOnlyProblems
    if (ProjectManagerImpl.isLight(project)) {
      LOG.error("StructureViewWrapperImpl must be not created for light project.");
    }

    myUpdateQueue = new MergingUpdateQueue("StructureView", REBUILD_TIME, false, component, this, component)
      .usePassThroughInUnitTestMode();
    myUpdateQueue.setRestartTimerOnAdd(true);

    // to check on the next turn
    Timer timer = TimerUtil.createNamedTimer("StructureView", REFRESH_TIME, event -> {
      if (!component.isShowing()) return;

      int count = ActivityTracker.getInstance().getCount();
      if (count == myActivityCount) return;

      ModalityState state = ModalityState.stateForComponent(component);
      if (ModalityState.current().dominates(state)) return;

      boolean successful = loggedRun("check if update needed", this::checkUpdate);
      if (successful) myActivityCount = count; // to check on the next turn
    });

    LOG.debug("timer to check if update needed: add");
    timer.start();
    Disposer.register(this, new Disposable() {
      @Override
      public void dispose() {
        LOG.debug("timer to check if update needed: remove");
        timer.stop();
      }
    });

    component.addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        if (BitUtil.isSet(e.getChangeFlags(), HierarchyEvent.DISPLAYABILITY_CHANGED)) {
          boolean visible = myToolWindow.isVisible();
          LOG.debug("displayability changed: " + visible);
          if (visible) {
            loggedRun("update file", StructureViewWrapperImpl.this::checkUpdate);
            scheduleRebuild();
          }
          else if (!myProject.isDisposed()) {
            myFile = null;
            loggedRun("clear a structure on hide", StructureViewWrapperImpl.this::rebuild);
          }
        }
      }
    });
    if (component.isShowing()) {
      loggedRun("initial structure rebuild", this::checkUpdate);
      scheduleRebuild();
    }
    myToolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
      @Override
      public void selectionChanged(@NotNull ContentManagerEvent event) {
        if (myStructureView instanceof StructureViewComposite) {
          StructureViewComposite.StructureViewDescriptor[] views = ((StructureViewComposite)myStructureView).getStructureViews();
          for (StructureViewComposite.StructureViewDescriptor view : views) {
            if (view.title.equals(event.getContent().getTabName())) {
              updateHeaderActions(view.structureView);
              break;
            }
          }
        }
      }
    });
    Disposer.register(myToolWindow.getContentManager(), this);

    PsiStructureViewFactory.EP_NAME.addChangeListener(this::clearCaches, this);

    StructureViewBuilder.EP_NAME.addChangeListener(this::clearCaches, this);
    getApplication().getMessageBus().connect(this).subscribe(STRUCTURE_CHANGED, this::clearCaches);
  }

  private void clearCaches() {
    StructureViewComponent.clearStructureViewState(myProject);
    if (myStructureView != null) {
      myStructureView.disableStoreState();
    }
    rebuild();
  }

  private void checkUpdate() {
    if (myProject.isDisposed()) return;

    final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    final boolean insideToolwindow = SwingUtilities.isDescendingFrom(myToolWindow.getComponent(), owner);
    if (insideToolwindow) LOG.debug("inside structure view");
    if (!myFirstRun && (insideToolwindow || JBPopupFactory.getInstance().isPopupActive())) {
      return;
    }

    final DataContext dataContext = DataManager.getInstance().getDataContext(owner);
    if (WRAPPER_DATA_KEY.getData(dataContext) == this) return;
    if (CommonDataKeys.PROJECT.getData(dataContext) != myProject) return;

    VirtualFile[] files = insideToolwindow ? null : CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
    if (files != null && files.length == 1) {
      setFile(files[0]);
    }
    else if (files != null && files.length > 1) {
      setFile(null);
    }
    else if (myFirstRun) {
      FileEditorManagerImpl editorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(myProject);
      List<Pair<VirtualFile, EditorWindow>> history = editorManager.getSelectionHistory();
      if (!history.isEmpty()) {
        setFile(history.get(0).getFirst());
      }
    }

    myFirstRun = false;
  }

  private void setFile(@Nullable VirtualFile file) {
    boolean forceRebuild = !Comparing.equal(file, myFile);
    if (!forceRebuild && myStructureView != null) {
      StructureViewModel model = myStructureView.getTreeModel();
      StructureViewTreeElement treeElement = model.getRoot();
      Object value = treeElement.getValue();
      if (value == null ||
          value instanceof PsiElement && !((PsiElement)value).isValid() ||
          myStructureView instanceof StructureViewComposite && ((StructureViewComposite)myStructureView).isOutdated()) {
        forceRebuild = true;
      }
      else if (file != null) {
        forceRebuild = FileEditorManager.getInstance(myProject).getSelectedEditor(file) != myFileEditor;
      }
    }
    if (forceRebuild) {
      myFile = file;
      LOG.debug("show structure for file: ", file);
      scheduleRebuild();
    }
  }


  // -------------------------------------------------------------------------
  // StructureView interface implementation
  // -------------------------------------------------------------------------

  @Override
  public void dispose() {
    //we don't really need it
    //rebuild();
  }

  @Override
  public boolean selectCurrentElement(final FileEditor fileEditor, final VirtualFile file, final boolean requestFocus) {
    //todo [kirillk]
    // this is dirty hack since some bright minds decided to used different TreeUi every time, so selection may be followed
    // by rebuild on completely different instance of TreeUi

    Runnable runnable = () -> {
      if (!Comparing.equal(myFileEditor, fileEditor)) {
        myFile = file;
        LOG.debug("replace file on selection: ", file);
        loggedRun("rebuild a structure immediately: ", this::rebuild);
      }
      if (myStructureView != null) {
        myStructureView.navigateToSelectedElement(requestFocus);
      }
    };

    if (isStructureViewShowing()) {
      if (myUpdateQueue.isEmpty()) {
        runnable.run();
      } else {
        myPendingSelection = runnable;
      }
    } else {
      myPendingSelection = runnable;
    }

    return true;
  }

  private void scheduleRebuild() {
    if (!myToolWindow.isVisible()) return;
    LOG.debug("request to rebuild a structure");
    myUpdateQueue.queue(new Update("rebuild") {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;
        if (!getApplication().isDispatchThread()) {
          LOG.error("EDT-based MergingUpdateQueue on background thread");
        }
        loggedRun("rebuild a structure: ", StructureViewWrapperImpl.this::rebuild);
      }
    });
  }

  public void rebuild() {
    if (myProject.isDisposed()) return;

    Dimension referenceSize = null;

    Container container = myToolWindow.getComponent();
    boolean wasFocused = UIUtil.isFocusAncestor(container);

    if (myStructureView != null) {
      if (myStructureView instanceof StructureView.Scrollable) {
        referenceSize = ((StructureView.Scrollable)myStructureView).getCurrentSize();
      }

      myStructureView.storeState();
      Disposer.dispose(myStructureView);
      myStructureView = null;
      myFileEditor = null;
    }

    if (myModuleStructureComponent != null) {
      Disposer.dispose(myModuleStructureComponent);
      myModuleStructureComponent = null;
    }

    final ContentManager contentManager = myToolWindow.getContentManager();
    contentManager.removeAllContents(true);
    if (!isStructureViewShowing()) {
      return;
    }

    VirtualFile file = myFile;
    if (file == null) {
      final VirtualFile[] selectedFiles = FileEditorManager.getInstance(myProject).getSelectedFiles();
      if (selectedFiles.length > 0) {
        file = selectedFiles[0];
      }
    }

    String[] names = {""};
    if (file != null && file.isValid()) {
      if (file.isDirectory()) {
        if (ProjectRootsUtil.isModuleContentRoot(file, myProject)) {
          Module module = ModuleUtilCore.findModuleForFile(file, myProject);
          if (module != null && !ModuleType.isInternal(module)) {
            myModuleStructureComponent = new ModuleStructureComponent(module);
            createSinglePanel(myModuleStructureComponent.getComponent());
            Disposer.register(this, myModuleStructureComponent);
          }
        }
      }
      else {
        FileEditor editor = FileEditorManager.getInstance(myProject).getSelectedEditor(file);
        StructureViewBuilder structureViewBuilder =
          editor != null && editor.isValid() ? editor.getStructureViewBuilder() :
          createStructureViewBuilder(file);
        if (structureViewBuilder != null) {
          myStructureView = structureViewBuilder.createStructureView(editor, myProject);
          myFileEditor = editor;
          Disposer.register(this, myStructureView);

          if (myStructureView instanceof StructureView.Scrollable) {
            ((StructureView.Scrollable)myStructureView).setReferenceSizeWhileInitializing(referenceSize);
          }

          if (myStructureView instanceof StructureViewComposite) {
            final StructureViewComposite composite = (StructureViewComposite)myStructureView;
            final StructureViewComposite.StructureViewDescriptor[] views = composite.getStructureViews();
            myPanels = new JPanel[views.length];
            names = new String[views.length];
            for (int i = 0; i < myPanels.length; i++) {
              myPanels[i] = createContentPanel(views[i].structureView.getComponent());
              names[i] = views[i].title;
            }
          }
          else {
            createSinglePanel(myStructureView.getComponent());
          }

          myStructureView.restoreState();
          myStructureView.centerSelectedRow();
        }
      }
    }

    updateHeaderActions(myStructureView);

    if (myModuleStructureComponent == null && myStructureView == null) {
      JBPanelWithEmptyText panel = new JBPanelWithEmptyText() {
        @Override
        public Color getBackground() {
          return UIUtil.getTreeBackground();
        }
      };
      panel.getEmptyText().setText(LangBundle.message("panel.empty.text.no.structure"));
      createSinglePanel(panel);
    }

    for (int i = 0; i < myPanels.length; i++) {
      final Content content = ContentFactory.SERVICE.getInstance().createContent(myPanels[i], names[i], false);
      contentManager.addContent(content);
      if (i == 0 && myStructureView != null) {
        Disposer.register(content, myStructureView);
      }
    }

    if (myPendingSelection != null) {
      Runnable selection = myPendingSelection;
      myPendingSelection = null;
      selection.run();
    }

    if (wasFocused) {
      FocusTraversalPolicy policy = container.getFocusTraversalPolicy();
      Component component = policy == null ? null : policy.getDefaultComponent(container);
      if (component != null) IdeFocusManager.getInstance(myProject).requestFocusInProject(component, myProject);
    }
  }

  private void updateHeaderActions(@Nullable StructureView structureView) {
    List<AnAction> titleActions = Collections.emptyList();
    if (structureView instanceof StructureViewComponent) {
      JTree tree = ((StructureViewComponent)structureView).getTree();
      CommonActionsManager commonActionManager = CommonActionsManager.getInstance();
      titleActions = Arrays.asList(
        commonActionManager.createExpandAllHeaderAction(tree),
        commonActionManager.createCollapseAllHeaderAction(tree));
    }
    myToolWindow.setTitleActions(titleActions);
  }

  private void createSinglePanel(final JComponent component) {
    myPanels = new JPanel[1];
    myPanels[0] = createContentPanel(component);
  }

  private ContentPanel createContentPanel(JComponent component) {
    final ContentPanel panel = new ContentPanel();
    panel.setBackground(UIUtil.getTreeBackground());
    panel.add(component, BorderLayout.CENTER);
    return panel;
  }

  @Nullable
  private StructureViewBuilder createStructureViewBuilder(@NotNull VirtualFile file) {
    if (file.getLength() > PersistentFSConstants.getMaxIntellisenseFileSize()) return null;

    FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(myProject, file);
    FileEditorProvider provider = providers.length == 0 ? null : providers[0];
    if (provider == null) return null;
    if (provider instanceof TextEditorProvider) {
      return StructureViewBuilder.PROVIDER.getStructureViewBuilder(file.getFileType(), file, myProject);
    }

    FileEditor editor = provider.createEditor(myProject, file);
    try {
      return editor.getStructureViewBuilder();
    }
    finally {
      Disposer.dispose(editor);
    }
  }


  protected boolean isStructureViewShowing() {
    ToolWindowManager windowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = windowManager.getToolWindow(ToolWindowId.STRUCTURE_VIEW);
    // it means that window is registered
    return toolWindow != null && toolWindow.isVisible();
  }

  private class ContentPanel extends JPanel implements DataProvider {
    ContentPanel() {
      super(new BorderLayout());
    }

    @Override
    public Object getData(@NotNull @NonNls String dataId) {
      if (WRAPPER_DATA_KEY.is(dataId)) return StructureViewWrapperImpl.this;
      return null;
    }
  }

  private static boolean loggedRun(@NotNull String message, @NotNull Runnable task) {
    try {
      if (LOG.isTraceEnabled()) LOG.trace(message + ": started");
      task.run();
      return true;
    }
    catch (ProcessCanceledException exception) {
      LOG.debug(message, ": canceled");
      return false;
    }
    catch (Throwable throwable) {
      LOG.warn(message, throwable);
      return false;
    }
    finally {
      if (LOG.isTraceEnabled()) LOG.trace(message + ": finished");
    }
  }
}
