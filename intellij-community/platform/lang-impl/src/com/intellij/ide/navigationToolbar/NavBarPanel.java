// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.ide.CopyPasteDelegator;
import com.intellij.ide.CopyPasteSupport;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeView;
import com.intellij.ide.dnd.DnDDragStartBean;
import com.intellij.ide.dnd.DnDSupport;
import com.intellij.ide.dnd.TransferableWrapper;
import com.intellij.ide.navigationToolbar.ui.NavBarUI;
import com.intellij.ide.navigationToolbar.ui.NavBarUIManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.DeleteHandler;
import com.intellij.internal.statistic.service.fus.collectors.UIEventId;
import com.intellij.internal.statistic.service.fus.collectors.UIEventLogger;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupOwner;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.PanelUI;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * @author Konstantin Bulenkov
 * @author Anna Kozlova
 */
public class NavBarPanel extends JPanel implements DataProvider, PopupOwner, Disposable, Queryable {

  private final NavBarModel myModel;

  private final NavBarPresentation myPresentation;
  private final Project myProject;

  private final ArrayList<NavBarItem> myList = new ArrayList<>();

  private final ModuleDeleteProvider myDeleteModuleProvider = new ModuleDeleteProvider();
  private final IdeView myIdeView;
  private FocusListener myNavBarItemFocusListener;

  private LightweightHint myHint = null;
  private NavBarPopup myNodePopup = null;
  private JComponent myHintContainer;
  private Component myContextComponent;

  private final NavBarUpdateQueue myUpdateQueue;

  private NavBarItem myContextObject;
  private boolean myDisposed = false;
  private RelativePoint myLocationCache;

  public NavBarPanel(@NotNull Project project, boolean docked) {
    super(new FlowLayout(FlowLayout.LEFT, 0, 0));
    myProject = project;
    myModel = createModel();
    myIdeView = new NavBarIdeView(this);
    myPresentation = new NavBarPresentation(myProject);
    myUpdateQueue = new NavBarUpdateQueue(this);

    installPopupHandler(this, -1);
    setOpaque(false);
    if (!docked && UIUtil.isUnderDarcula()) {
      setBorder(new LineBorder(Gray._120, 1));
    }
    myUpdateQueue.queueModelUpdateFromFocus();
    myUpdateQueue.queueRebuildUi();
    if (!docked) {
      final ActionCallback typeAheadDone = new ActionCallback();
      IdeFocusManager.getInstance(project).typeAheadUntil(typeAheadDone, "NavBarPanel");
      myUpdateQueue.queueTypeAheadDone(typeAheadDone);
    }

    Disposer.register(project, this);
    AccessibleContextUtil.setName(this, "Navigation Bar");
  }

  /**
   * Navigation bar entry point to determine if the keyboard/focus behavior should be
   * compatible with screen readers. This additional level of indirection makes it
   * easier to figure out the various locations in the various navigation bar components
   * that enable screen reader friendly behavior.
   */
  protected boolean allowNavItemsFocus() {
    return ScreenReader.isActive();
  }

  public boolean isFocused() {
    if (allowNavItemsFocus()) {
      return UIUtil.isFocusAncestor(this);
    } else {
      return hasFocus();
    }
  }

  public void addNavBarItemFocusListener(@Nullable FocusListener l) {
    if (l == null) {
      return;
    }
    myNavBarItemFocusListener = AWTEventMulticaster.add(myNavBarItemFocusListener, l);
  }

  public void removeNavBarItemFocusListener(@Nullable FocusListener l) {
    if (l == null) {
      return;
    }
    myNavBarItemFocusListener = AWTEventMulticaster.remove(myNavBarItemFocusListener, l);
  }

  protected void fireNavBarItemFocusGained(final FocusEvent e) {
    FocusListener listener = myNavBarItemFocusListener;
    if (listener != null) {
      listener.focusGained(e);
    }
  }

  protected void fireNavBarItemFocusLost(final FocusEvent e) {
    FocusListener listener = myNavBarItemFocusListener;
    if (listener != null) {
      listener.focusLost(e);
    }
  }

  protected NavBarModel createModel() {
    return new NavBarModel(myProject);
  }

  @Nullable
  public NavBarPopup getNodePopup() {
    return myNodePopup;
  }

  public boolean isNodePopupActive() {
    return myNodePopup != null && myNodePopup.isVisible();
  }

  public LightweightHint getHint() {
    return myHint;
  }

  public NavBarPresentation getPresentation() {
    return myPresentation;
  }

  public void setContextComponent(@Nullable Component contextComponent) {
    myContextComponent = contextComponent;
  }

  public NavBarItem getContextObject() {
    return myContextObject;
  }

  public List<NavBarItem> getItems() {
    return Collections.unmodifiableList(myList);
  }

  public void addItem(NavBarItem item) {
    myList.add(item);
  }

  public void clearItems() {
    final NavBarItem[] toDispose = myList.toArray(new NavBarItem[0]);
    myList.clear();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      for (NavBarItem item : toDispose) {
        Disposer.dispose(item);
      }
    });

    getNavBarUI().clearItems();
  }

  @Override
  public void setUI(PanelUI ui) {
    getNavBarUI().clearItems();
    super.setUI(ui);
  }

  public NavBarUpdateQueue getUpdateQueue() {
    return myUpdateQueue;
  }

  public void escape() {
    myModel.setSelectedIndex(-1);
    hideHint();
    ToolWindowManager.getInstance(myProject).activateEditorComponent();
  }

  public void enter() {
    navigateInsideBar(myModel.getSelectedValue());
  }

  public void moveHome() {
    shiftFocus(-myModel.getSelectedIndex());
  }

  public void navigate() {
    if (myModel.getSelectedIndex() != -1) {
      doubleClick(myModel.getSelectedIndex());
    }
  }

  public void moveDown() {
    final int index = myModel.getSelectedIndex();
    if (index != -1) {
      if (myModel.size() - 1 == index) {
        shiftFocus(-1);
        ctrlClick(index - 1);
      }
      else {
        ctrlClick(index);
      }
    }
  }

  public void moveEnd() {
    shiftFocus(myModel.size() - 1 - myModel.getSelectedIndex());
  }

  public Project getProject() {
    return myProject;
  }

  public NavBarModel getModel() {
    return myModel;
  }

  @Override
  public void dispose() {
    cancelPopup();
    getNavBarUI().clearItems();
    myDisposed = true;
    NavBarListener.unsubscribeFrom(this);
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  boolean isSelectedInPopup(Object object) {
    return isNodePopupActive() && myNodePopup.getList().getSelectedValuesList().contains(object);
  }

  static Object expandDirsWithJustOneSubdir(Object target) {
    if (target instanceof PsiElement && !((PsiElement)target).isValid()) return target;
    if (target instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)target;
      for (VirtualFile file = directory.getVirtualFile(), next; ; file = next) {
        VirtualFile[] children = file.getChildren();
        VirtualFile child = children.length == 1 ? children[0] : null;
        //noinspection AssignmentToForLoopParameter
        next = child != null && child.isDirectory() && !child.is(VFileProperty.SYMLINK) ? child : null;
        if (next == null) return ObjectUtils.notNull(directory.getManager().findDirectory(file), directory);
      }
    }
    return target;
  }

  void updateItems() {
    for (NavBarItem item : myList) {
      item.update();
    }
    if (UISettings.getInstance().getShowNavigationBar()) {
      NavBarRootPaneExtension.NavBarWrapperPanel wrapperPanel = ComponentUtil
        .getParentOfType((Class<? extends NavBarRootPaneExtension.NavBarWrapperPanel>)NavBarRootPaneExtension.NavBarWrapperPanel.class,
                         (Component)this);

      if (wrapperPanel != null) {
        wrapperPanel.revalidate();
        wrapperPanel.repaint();
      }
    }
  }

  public void rebuildAndSelectTail(final boolean requestFocus) {
    myUpdateQueue.queueModelUpdateFromFocus();
    myUpdateQueue.queueRebuildUi();
    myUpdateQueue.queueSelect(() -> {
      if (!myList.isEmpty()) {
        myModel.setSelectedIndex(myList.size() - 1);
        requestSelectedItemFocus();
      }
    });

    myUpdateQueue.flush();
  }

  public void requestSelectedItemFocus() {
    int index = myModel.getSelectedIndex();
    if (index >= 0 && index < myModel.size() && allowNavItemsFocus()) {
      IdeFocusManager.getInstance(myProject).requestFocus(getItem(index), true);
    } else {
      IdeFocusManager.getInstance(myProject).requestFocus(this, true);
    }
  }

  public void moveLeft() {
    shiftFocus(-1);
  }

  public void moveRight() {
    shiftFocus(1);
  }

  void shiftFocus(int direction) {
    final int selectedIndex = myModel.getSelectedIndex();
    final int index = myModel.getIndexByModel(selectedIndex + direction);
    myModel.setSelectedIndex(index);
    if (allowNavItemsFocus()) {
      requestSelectedItemFocus();
    }
  }

  void scrollSelectionToVisible() {
    final int selectedIndex = myModel.getSelectedIndex();
    if (selectedIndex == -1 || selectedIndex >= myList.size()) return;
    scrollRectToVisible(myList.get(selectedIndex).getBounds());
  }

  @Nullable
  private NavBarItem getItem(int index) {
    if (index != -1 && index < myList.size()) {
      return myList.get(index);
    }
    return null;
  }

  public boolean isInFloatingMode() {
    return myHint != null && myHint.isVisible();
  }


  @Override
  public Dimension getPreferredSize() {
    if (myDisposed || !myList.isEmpty()) {
      return super.getPreferredSize();
    }
    else {
      final NavBarItem item = new NavBarItem(this, null, 0, null);
      final Dimension size = item.getPreferredSize();
      ApplicationManager.getApplication().executeOnPooledThread(() -> Disposer.dispose(item));
      return size;
    }
  }

  boolean isRebuildUiNeeded() {
    myModel.revalidate();
    if (myList.size() == myModel.size()) {
      int index = 0;
      for (NavBarItem eachLabel : myList) {
        Object eachElement = myModel.get(index);
        if (eachLabel.getObject() == null || !eachLabel.getObject().equals(eachElement)) {
          return true;
        }

        if (!StringUtil.equals(eachLabel.getText(), getPresentation().getPresentableText(eachElement))) {
          return true;
        }

        SimpleTextAttributes modelAttributes1 = myPresentation.getTextAttributes(eachElement, true);
        SimpleTextAttributes modelAttributes2 = myPresentation.getTextAttributes(eachElement, false);
        SimpleTextAttributes labelAttributes = eachLabel.getAttributes();

        if (!modelAttributes1.toTextAttributes().equals(labelAttributes.toTextAttributes())
            && !modelAttributes2.toTextAttributes().equals(labelAttributes.toTextAttributes())) {
          return true;
        }
        index++;
      }
      return false;
    }
    else {
      return true;
    }
  }


  @Nullable
  Window getWindow() {
    return !isShowing() ? null : (Window)UIUtil.findUltimateParent(this);
  }

  void installPopupHandler(@NotNull JComponent component, int index) {
    ActionManager actionManager = ActionManager.getInstance();
    PopupHandler.installPopupHandler(component, new ActionGroup() {
      @NotNull
      @Override
      public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) return EMPTY_ARRAY;
        String popupGroup = null;
        for (NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
          popupGroup = modelExtension.getPopupMenuGroup(NavBarPanel.this);
          if (popupGroup != null) break;
        }
        if (popupGroup == null) popupGroup = IdeActions.GROUP_NAVBAR_POPUP;
        return ((ActionGroup)actionManager.getAction(popupGroup)).getChildren(e);
      }
    }, ActionPlaces.NAVIGATION_BAR_POPUP, actionManager, new PopupMenuListenerAdapter() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        if (index != -1) {
          myModel.setSelectedIndex(index);
        }
      }
    });
  }

  public void installActions(int index, NavBarItem component) {
    //suppress it for a while
    //installDnD(index, component);
    installPopupHandler(component, index);
    ListenerUtil.addMouseListener(component, new MouseAdapter() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        if (SystemInfo.isWindows) {
          click(e);
        }
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        if (!SystemInfo.isWindows) {
          click(e);
        }
      }

      private void click(final MouseEvent e) {
        if (e.isConsumed()) return;

        if (e.isPopupTrigger()) return;
        if (e.getClickCount() == 1) {
          ctrlClick(index);
          e.consume();
        }
        else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
          requestSelectedItemFocus();
          doubleClick(index);
          e.consume();
        }
      }
    });

    ListenerUtil.addKeyListener(component, new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getModifiers() == 0 && e.getKeyCode() == KeyEvent.VK_SPACE) {
          ctrlClick(index);
          myModel.setSelectedIndex(index);
          e.consume();
        }
      }
    });
  }

  private void installDnD(final int index, NavBarItem component) {
    DnDSupport.createBuilder(component)
      .setBeanProvider(dnDActionInfo -> new DnDDragStartBean(new TransferableWrapper() {
        @Override
        public List<File> asFileList() {
          Object o = myModel.get(index);
          if (o instanceof PsiElement) {
            VirtualFile vf = o instanceof PsiDirectory ? ((PsiDirectory)o).getVirtualFile()
                                                       : ((PsiElement)o).getContainingFile().getVirtualFile();
            if (vf != null) {
              return Collections.singletonList(new File(vf.getPath()).getAbsoluteFile());
            }
          }
          return Collections.emptyList();
        }

        @Override
        public TreeNode[] getTreeNodes() {
          return null;
        }

        @Override
        public PsiElement[] getPsiElements() {
          return null;
        }
      }))
      .setDisposableParent(component)
      .install();
  }

  private void doubleClick(final int index) {
    doubleClick(myModel.getElement(index));
  }

  private void doubleClick(final Object object) {
    if (object instanceof Navigatable) {
      Navigatable navigatable = (Navigatable)object;
      if (navigatable.canNavigate()) {
        navigatable.navigate(true);
      }
    }
    else if (object instanceof Module) {
      ProjectView projectView = ProjectView.getInstance(myProject);
      AbstractProjectViewPane projectViewPane = projectView.getProjectViewPaneById(projectView.getCurrentViewId());
      if (projectViewPane != null) {
        projectViewPane.selectModule((Module)object, true);
      }
    }
    else if (object instanceof Project) {
      return;
    }
    hideHint(true);
  }

  private void ctrlClick(final int index) {
    if (isNodePopupActive()) {
      cancelPopup();
      if (myModel.getSelectedIndex() == index) {
        return;
      }
    }

    final Object object = myModel.getElement(index);
    final List<Object> objects = myModel.getChildren(object);

    if (!objects.isEmpty()) {
      final Object[] siblings = new Object[objects.size()];
      //final Icon[] icons = new Icon[objects.size()];
      for (int i = 0; i < objects.size(); i++) {
        siblings[i] = objects.get(i);
        //icons[i] = NavBarPresentation.getIcon(siblings[i], false);
      }
      final NavBarItem item = getItem(index);

      final int selectedIndex = index < myModel.size() - 1 ? objects.indexOf(myModel.getElement(index + 1)) : 0;
      myNodePopup = new NavBarPopup(this, siblings, selectedIndex);
     // if (item != null && item.isShowing()) {
        myNodePopup.show(item);
        item.update();
     // }
    }
  }

  protected void navigateInsideBar(final Object object) {
    UIEventLogger.logUIEvent(UIEventId.NavBarNavigate);

    Object obj = expandDirsWithJustOneSubdir(object);
    myContextObject = null;

    myUpdateQueue.cancelAllUpdates();
    if (myNodePopup != null && myNodePopup.isVisible()) {
      myUpdateQueue.queueModelUpdateForObject(obj);
    }
    myUpdateQueue.queueRebuildUi();

    myUpdateQueue.queueAfterAll(() -> {
      int index = myModel.indexOf(obj);
      if (index >= 0) {
        myModel.setSelectedIndex(index);
      }

      if (myModel.hasChildren(obj)) {
        restorePopup();
      }
      else {
        doubleClick(obj);
      }
    }, NavBarUpdateQueue.ID.NAVIGATE_INSIDE);
  }

  void restorePopup() {
    cancelPopup();
    ctrlClick(myModel.getSelectedIndex());
  }

  void cancelPopup() {
    cancelPopup(false);
  }


  void cancelPopup(boolean ok) {
    if (myNodePopup != null) {
      myNodePopup.hide(ok);
      myNodePopup = null;
      if (allowNavItemsFocus()) {
        requestSelectedItemFocus();
      }
    }
  }

  void hideHint() {
    hideHint(false);
  }

  void hideHint(boolean ok) {
    cancelPopup(ok);
    if (myHint != null) {
      myHint.hide(ok);
      myHint = null;
    }
  }

  @Override
  @Nullable
  public Object getData(@NotNull String dataId) {
    for (NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
      Object data = modelExtension.getData(dataId, this::getDataInner);
      if (data != null) return data;
    }
    return getDataInner(dataId);
  }

  @Nullable
  private Object getDataInner(String dataId) {
    return getDataImpl(dataId, this, () -> getSelection());
  }

  @NotNull
  JBIterable<?> getSelection() {
    Object value = myModel.getSelectedValue();
    if (value != null) return JBIterable.of(value);
    int size = myModel.size();
    return JBIterable.of(size > 0 ? myModel.getElement(size - 1) : null);
  }

  Object getDataImpl(String dataId, @NotNull JComponent source, @NotNull Getter<? extends JBIterable<?>> selection) {
    if (CommonDataKeys.PROJECT.is(dataId)) {
      return !myProject.isDisposed() ? myProject : null;
    }
    if (LangDataKeys.MODULE.is(dataId)) {
      Module module = selection.get().filter(Module.class).first();
      if (module != null && !module.isDisposed()) return module;
      PsiElement element = selection.get().filter(PsiElement.class).first();
      if (element != null) {
        return ModuleUtilCore.findModuleForPsiElement(element);
      }
      return null;
    }
    if (LangDataKeys.MODULE_CONTEXT.is(dataId)) {
      PsiDirectory directory = selection.get().filter(PsiDirectory.class).first();
      if (directory != null) {
        VirtualFile dir = directory.getVirtualFile();
        if (ProjectRootsUtil.isModuleContentRoot(dir, myProject)) {
          return ModuleUtilCore.findModuleForPsiElement(directory);
        }
      }
      return null;
    }
    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      PsiElement element = selection.get().filter(PsiElement.class).first();
      return element != null && element.isValid() ? element : null;
    }
    if (LangDataKeys.PSI_ELEMENT_ARRAY.is(dataId)) {
      List<PsiElement> result = selection.get().filter(PsiElement.class)
        .filter(e -> e != null && e.isValid()).toList();
      return result.isEmpty() ? null : result.toArray(PsiElement.EMPTY_ARRAY);
    }

    if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
      Set<VirtualFile> files = selection.get().filter(PsiElement.class)
        .filter(e -> e != null && e.isValid())
        .filterMap(e -> PsiUtilCore.getVirtualFile(e)).toSet();
      return !files.isEmpty() ? VfsUtilCore.toVirtualFileArray(files) : null;
    }

    if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) {
      List<Navigatable> elements = selection.get().filter(Navigatable.class).toList();
      return elements.isEmpty() ? null : elements.toArray(new Navigatable[0]);
    }

    if (PlatformDataKeys.CONTEXT_COMPONENT.is(dataId)) {
      return this;
    }
    if (PlatformDataKeys.CUT_PROVIDER.is(dataId)) {
      return getCopyPasteDelegator(source).getCutProvider();
    }
    if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
      return getCopyPasteDelegator(source).getCopyProvider();
    }
    if (PlatformDataKeys.PASTE_PROVIDER.is(dataId)) {
      return getCopyPasteDelegator(source).getPasteProvider();
    }
    if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
      return selection.get().filter(Module.class).isNotEmpty() ? myDeleteModuleProvider : new DeleteHandler.DefaultDeleteProvider();
    }

    if (LangDataKeys.IDE_VIEW.is(dataId)) {
      return myIdeView;
    }

    return null;
  }

  @NotNull
  private CopyPasteSupport getCopyPasteDelegator(@NotNull JComponent source) {
    String key = "NavBarPanel.copyPasteDelegator";
    Object result = source.getClientProperty(key);
    if (!(result instanceof CopyPasteSupport)) {
      source.putClientProperty(key, result = new CopyPasteDelegator(myProject, source));
    }
    return (CopyPasteSupport)result;
  }

  @Override
  public Point getBestPopupPosition() {
    int index = myModel.getSelectedIndex();
    final int modelSize = myModel.size();
    if (index == -1) {
      index = modelSize - 1;
    }
    if (index > -1 && index < modelSize) {
      final NavBarItem item = getItem(index);
      if (item != null) {
        return new Point(item.getX(), item.getY() + item.getHeight());
      }
    }
    return null;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    NavBarListener.subscribeTo(this);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
      Disposer.dispose(this);
    }
  }

  public void updateState(final boolean show) {
    if (show) {
      myUpdateQueue.queueModelUpdateFromFocus();
      myUpdateQueue.queueRebuildUi();
    }
  }

  // ------ popup NavBar ----------
  public void showHint(@Nullable final Editor editor, final DataContext dataContext) {
    myModel.updateModel(dataContext);
    if (myModel.isEmpty()) return;
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(this);
    panel.setOpaque(true);
    panel.setBackground(UIUtil.getListBackground());

    myHint = new LightweightHint(panel) {
      @Override
      public void hide() {
        super.hide();
        cancelPopup();
        Disposer.dispose(NavBarPanel.this);
      }
    };
    myHint.setForceShowAsPopup(true);
    myHint.setFocusRequestor(this);
    final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    myUpdateQueue.rebuildUi();
    if (editor == null) {
      myContextComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
      getHintContainerShowPoint().doWhenDone((Consumer<RelativePoint>)relativePoint -> {
        final Component owner = focusManager.getFocusOwner();
        final Component cmp = relativePoint.getComponent();
        if (cmp instanceof JComponent && cmp.isShowing()) {
          myHint.show((JComponent)cmp, relativePoint.getPoint().x, relativePoint.getPoint().y,
                      owner instanceof JComponent ? (JComponent)owner : null,
                      new HintHint(relativePoint.getComponent(), relativePoint.getPoint()));
        }
      });
    }
    else {
      myHintContainer = editor.getContentComponent();
      getHintContainerShowPoint().doWhenDone((Consumer<RelativePoint>)rp -> {
        Point p = rp.getPointOn(myHintContainer).getPoint();
        final HintHint hintInfo = new HintHint(editor, p);
        HintManagerImpl.getInstanceImpl().showEditorHint(myHint, editor, p, HintManager.HIDE_BY_ESCAPE, 0, true, hintInfo);
      });
    }

    rebuildAndSelectTail(true);
  }

  AsyncResult<RelativePoint> getHintContainerShowPoint() {
    AsyncResult<RelativePoint> result = new AsyncResult<>();
    if (myLocationCache == null) {
      if (myHintContainer != null) {
        final Point p = AbstractPopup.getCenterOf(myHintContainer, this);
        p.y -= myHintContainer.getVisibleRect().height / 4;
        myLocationCache = RelativePoint.fromScreen(p);
      }
      else {
        DataManager dataManager = DataManager.getInstance();
        if (myContextComponent != null) {
          DataContext ctx = dataManager.getDataContext(myContextComponent);
          myLocationCache = JBPopupFactory.getInstance().guessBestPopupLocation(ctx);
        }
        else {
          dataManager.getDataContextFromFocus().doWhenDone((Consumer<DataContext>)dataContext -> {
            myContextComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
            DataContext ctx = dataManager.getDataContext(myContextComponent);
            myLocationCache = JBPopupFactory.getInstance().guessBestPopupLocation(ctx);
          });
        }
      }
    }
    final Component c = myLocationCache.getComponent();
    if (!(c instanceof JComponent && c.isShowing())) {
      //Yes. It happens sometimes.
      // 1. Empty frame. call nav bar, select some package and open it in Project View
      // 2. Call nav bar, then Esc
      // 3. Hide all tool windows (Ctrl+Shift+F12), so we've got empty frame again
      // 4. Call nav bar. NPE. ta da
      final JComponent ideFrame = WindowManager.getInstance().getIdeFrame(getProject()).getComponent();
      final JRootPane rootPane = UIUtil.getRootPane(ideFrame);
      myLocationCache = JBPopupFactory.getInstance().guessBestPopupLocation(rootPane);
    }
    result.setDone(myLocationCache);
    return result;
  }

  @Override
  public void putInfo(@NotNull Map<String, String> info) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < myList.size(); i++) {
      NavBarItem each = myList.get(i);
      if (each.isSelected()) {
        result.append("[").append(each.getText()).append("]");
      }
      else {
        result.append(each.getText());
      }
      if (i < myList.size() - 1) {
        result.append(">");
      }
    }
    info.put("navBar", result.toString());

    if (isNodePopupActive()) {
      StringBuilder popupText = new StringBuilder();
      JBList list = myNodePopup.getList();
      for (int i = 0; i < list.getModel().getSize(); i++) {
        Object eachElement = list.getModel().getElementAt(i);
        String text = new NavBarItem(this, eachElement, myNodePopup).getText();
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex != -1 && eachElement.equals(list.getSelectedValue())) {
          popupText.append("[").append(text).append("]");
        }
        else {
          popupText.append(text);
        }
        if (i < list.getModel().getSize() - 1) {
          popupText.append(">");
        }
      }
      info.put("navBarPopup", popupText.toString());
    }
  }

  @NotNull
  public NavBarUI getNavBarUI() {
    return NavBarUIManager.getUI();
  }
}
