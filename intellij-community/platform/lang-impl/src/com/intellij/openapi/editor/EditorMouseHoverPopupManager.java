// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.LineTooltipRenderer;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.codeInsight.hint.TooltipRenderer;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ModalityStateListener;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.reference.SoftReference;
import com.intellij.ui.*;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service
public final class EditorMouseHoverPopupManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(EditorMouseHoverPopupManager.class);
  private static final Key<Boolean> DISABLE_BINDING = Key.create("EditorMouseHoverPopupManager.disable.binding");
  private static final TooltipGroup EDITOR_INFO_GROUP = new TooltipGroup("EDITOR_INFO_GROUP", 0);
  private static final int MAX_POPUP_WIDTH = 650;
  private static final int MAX_QUICK_DOC_CHARACTERS = 100_000;

  private final Alarm myAlarm;
  private final MouseMovementTracker myMouseMovementTracker = new MouseMovementTracker();
  private boolean myKeepPopupOnMouseMove;
  private WeakReference<Editor> myCurrentEditor;
  private WeakReference<AbstractPopup> myPopupReference;
  private Context myContext;
  private ProgressIndicator myCurrentProgress;
  private CancellablePromise<Context> myPreparationTask;
  private boolean mySkipNextMovement;

  public EditorMouseHoverPopupManager() {
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
    multicaster.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@NotNull CaretEvent event) {
        Editor editor = event.getEditor();
        if (editor == SoftReference.dereference(myCurrentEditor)) {
          DocumentationManager.getInstance(Objects.requireNonNull(editor.getProject())).setAllowContentUpdateFromContext(true);
        }
      }
    }, this);
    multicaster.addVisibleAreaListener(e -> {
      Rectangle oldRectangle = e.getOldRectangle();
      if (e.getEditor() == SoftReference.dereference(myCurrentEditor) &&
          oldRectangle != null && !oldRectangle.getLocation().equals(e.getNewRectangle().getLocation())) {
        cancelProcessingAndCloseHint();
      }
    }, this);

    EditorMouseHoverPopupControl.getInstance().addListener(() -> {
      Editor editor = SoftReference.dereference(myCurrentEditor);
      if (editor != null && EditorMouseHoverPopupControl.arePopupsDisabled(editor)) {
        closeHint();
      }
    });
    LaterInvocator.addModalityStateListener(new ModalityStateListener() {
      @Override
      public void beforeModalityStateChanged(boolean entering, @NotNull Object modalEntity) {
        cancelProcessingAndCloseHint();
      }
    }, this);
    IdeEventQueue.getInstance().addDispatcher(event -> {
      int eventID = event.getID();
      if (eventID == KeyEvent.KEY_PRESSED || eventID == KeyEvent.KEY_TYPED) {
        cancelCurrentProcessing();
      }
      return false;
    }, this);
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.TOPIC, new MyActionListener());
  }

  @Override
  public void dispose() {}

  private void handleMouseMoved(@NotNull EditorMouseEvent e) {
    long startTimestamp = System.currentTimeMillis();

    cancelCurrentProcessing();

    if (ignoreEvent(e)) return;

    Editor editor = e.getEditor();
    if (isPopupDisabled(editor)) {
      closeHint();
      return;
    }

    int targetOffset = getTargetOffset(e);
    if (targetOffset < 0) {
      closeHint();
      return;
    }
    myPreparationTask = ReadAction.nonBlocking(() -> createContext(editor, targetOffset, startTimestamp))
      .coalesceBy(this)
      .withDocumentsCommitted(Objects.requireNonNull(editor.getProject()))
      .expireWhen(() -> editor.isDisposed())
      .finishOnUiThread(ModalityState.any(), context -> {
        myPreparationTask = null;
        if (context == null || !editor.getContentComponent().isShowing()) {
          closeHint();
          return;
        }
        Context.Relation relation = isHintShown() ? context.compareTo(myContext) : Context.Relation.DIFFERENT;
        if (relation == Context.Relation.SAME) {
          return;
        }
        else if (relation == Context.Relation.DIFFERENT) {
          closeHint();
        }
        scheduleProcessing(editor, context, relation == Context.Relation.SIMILAR, false, false);
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  private void cancelCurrentProcessing() {
    if (myPreparationTask != null) {
      myPreparationTask.cancel();
      myPreparationTask = null;
    }
    myAlarm.cancelAllRequests();
    if (myCurrentProgress != null) {
      myCurrentProgress.cancel();
      myCurrentProgress = null;
    }
  }

  private void skipNextMovement() {
    mySkipNextMovement = true;
  }

  private void scheduleProcessing(@NotNull Editor editor,
                                  @NotNull Context context,
                                  boolean updateExistingPopup,
                                  boolean forceShowing,
                                  boolean requestFocus) {
    ProgressIndicatorBase progress = new ProgressIndicatorBase();
    myCurrentProgress = progress;
    myAlarm.addRequest(() -> {
      ProgressManager.getInstance().executeProcessUnderProgress(() -> {
        Info info = context.calcInfo(editor);
        ApplicationManager.getApplication().invokeLater(() -> {
          if (progress != myCurrentProgress) {
            return;
          }

          myCurrentProgress = null;
          if (info == null ||
              !editor.getContentComponent().isShowing() ||
              (!forceShowing && isPopupDisabled(editor))) {
            return;
          }

          PopupBridge popupBridge = new PopupBridge();
          JComponent component = info.createComponent(editor, popupBridge, requestFocus);
          if (component == null) {
            closeHint();
          }
          else {
            if (updateExistingPopup && isHintShown()) {
              updateHint(component, popupBridge);
            }
            else {
              AbstractPopup hint = createHint(component, popupBridge, requestFocus);
              showHintInEditor(hint, editor, context);
              myPopupReference = new WeakReference<>(hint);
              myCurrentEditor = new WeakReference<>(editor);
            }
            myContext = context;
          }
        });
      }, progress);
    }, context.getShowingDelay());
  }

  private boolean ignoreEvent(EditorMouseEvent e) {
    if (mySkipNextMovement) {
      mySkipNextMovement = false;
      return true;
    }
    Rectangle currentHintBounds = getCurrentHintBounds(e.getEditor());
    return myMouseMovementTracker.isMovingTowards(e.getMouseEvent(), currentHintBounds) ||
           currentHintBounds != null && myKeepPopupOnMouseMove;
  }

  private static boolean isPopupDisabled(Editor editor) {
    return isAnotherAppInFocus() ||
           EditorMouseHoverPopupControl.arePopupsDisabled(editor) ||
           LookupManager.getActiveLookup(editor) != null ||
           isAnotherPopupFocused() ||
           isContextMenuShown();
  }

  private static boolean isAnotherAppInFocus() {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null;
  }

  // e.g. if documentation popup (opened via keyboard shortcut) is already shown
  private static boolean isAnotherPopupFocused() {
    JBPopup popup = PopupUtil.getPopupContainerFor(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
    return popup != null && !popup.isDisposed();
  }

  private static boolean isContextMenuShown() {
    return MenuSelectionManager.defaultManager().getSelectedPath().length > 0;
  }

  private Rectangle getCurrentHintBounds(Editor editor) {
    JBPopup popup = getCurrentHint();
    if (popup == null) return null;
    Dimension size = popup.getSize();
    if (size == null) return null;
    Rectangle result = new Rectangle(popup.getLocationOnScreen(), size);
    int borderTolerance = editor.getLineHeight() / 3;
    result.grow(borderTolerance, borderTolerance);
    return result;
  }

  private void showHintInEditor(AbstractPopup hint, Editor editor, Context context) {
    closeHint();
    myMouseMovementTracker.reset();
    myKeepPopupOnMouseMove = false;
    editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, context.getPopupPosition(editor));
    try {
      hint.showInBestPositionFor(editor);
    }
    finally {
      editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null);
    }
    Window window = hint.getPopupWindow();
    if (window != null) {
      window.setFocusableWindowState(true);
      IdeEventQueue.getInstance().addDispatcher(e -> {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getSource() == window) {
          myKeepPopupOnMouseMove = true;
        }
        else if (e.getID() == WindowEvent.WINDOW_OPENED && !isParentWindow(window, e.getSource())) {
          closeHint();
        }
        return false;
      }, hint);
    }
  }

  private static boolean isParentWindow(@NotNull Window parent, Object potentialChild) {
    return parent == potentialChild ||
           (potentialChild instanceof Component) && isParentWindow(parent, ((Component)potentialChild).getParent());
  }

  private static AbstractPopup createHint(JComponent component, PopupBridge popupBridge, boolean requestFocus) {
    WrapperPanel wrapper = new WrapperPanel(component);
    AbstractPopup popup = (AbstractPopup)JBPopupFactory.getInstance()
      .createComponentPopupBuilder(wrapper, component)
      .setResizable(true)
      .setFocusable(requestFocus)
      .setRequestFocus(requestFocus)
      .createPopup();
    popupBridge.setPopup(popup);
    return popup;
  }

  private void updateHint(JComponent component, PopupBridge popupBridge) {
    AbstractPopup popup = getCurrentHint();
    if (popup != null) {
      WrapperPanel wrapper = (WrapperPanel)popup.getComponent();
      wrapper.setContent(component);
      validatePopupSize(popup);
      popupBridge.setPopup(popup);
    }
  }

  private static void validatePopupSize(@NotNull AbstractPopup popup) {
    JComponent component = popup.getComponent();
    if (component != null) popup.setSize(component.getPreferredSize());
  }

  private static int getTargetOffset(EditorMouseEvent event) {
    Editor editor = event.getEditor();
    if (editor instanceof EditorEx &&
        editor.getProject() != null &&
        event.getArea() == EditorMouseEventArea.EDITING_AREA &&
        event.getMouseEvent().getModifiers() == 0 &&
        event.isOverText() &&
        event.getCollapsedFoldRegion() == null) {
      return event.getOffset();
    }
    return -1;
  }

  private static Context createContext(Editor editor, int offset, long startTimestamp) {
    Project project = Objects.requireNonNull(editor.getProject());

    HighlightInfo info = null;
    if (!Registry.is("ide.disable.editor.tooltips")) {
      DaemonCodeAnalyzerImpl daemonCodeAnalyzer = (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
      boolean highestPriorityOnly = !Registry.is("ide.tooltip.showAllSeverities");
      info = daemonCodeAnalyzer
        .findHighlightsByOffset(editor.getDocument(), offset, false, highestPriorityOnly, HighlightSeverity.INFORMATION);
    }

    PsiElement elementForQuickDoc = null;
    if (EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement()) {
      PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        elementForQuickDoc = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, offset);
        if (elementForQuickDoc == null) elementForQuickDoc = psiFile.findElementAt(offset);
        if (elementForQuickDoc instanceof PsiWhiteSpace || elementForQuickDoc instanceof PsiPlainText) {
          elementForQuickDoc = null;
        }
      }
    }

    return info == null && elementForQuickDoc == null ? null : new Context(startTimestamp, offset, info, elementForQuickDoc);
  }

  private void cancelProcessingAndCloseHint() {
    cancelCurrentProcessing();
    closeHint();
  }

  private void closeHint() {
    AbstractPopup hint = getCurrentHint();
    if (hint != null) {
      hint.cancel();
    }
    myPopupReference = null;
    myCurrentEditor = null;
    myContext = null;
  }

  private boolean isHintShown() {
    return getCurrentHint() != null;
  }

  private AbstractPopup getCurrentHint() {
    if (myPopupReference == null) return null;
    AbstractPopup hint = myPopupReference.get();
    if (hint == null || !hint.isVisible()) {
      if (hint != null) {
        // hint's window might've been hidden by AWT without notifying us
        // dispose to remove the popup from IDE hierarchy and avoid leaking components
        hint.cancel();
      }
      myPopupReference = null;
      myCurrentEditor = null;
      myContext = null;
      return null;
    }
    return hint;
  }

  public void showInfoTooltip(@NotNull Editor editor,
                              @NotNull HighlightInfo info,
                              int offset,
                              boolean requestFocus,
                              boolean showImmediately) {
    if (editor.getProject() == null) return;
    cancelProcessingAndCloseHint();
    Context context = new Context(System.currentTimeMillis(), offset, info, null) {
      @Override
      long getShowingDelay() {
        return showImmediately ? 0 : super.getShowingDelay();
      }
    };
    scheduleProcessing(editor, context, false, true, requestFocus);
  }

  private static class Context {
    private final long startTimestamp;
    private final int targetOffset;
    private final WeakReference<HighlightInfo> highlightInfo;
    private final WeakReference<PsiElement> elementForQuickDoc;

    private Context(long startTimestamp, int targetOffset, HighlightInfo highlightInfo, PsiElement elementForQuickDoc) {
      this.startTimestamp = startTimestamp;
      this.targetOffset = targetOffset;
      this.highlightInfo = highlightInfo == null ? null : new WeakReference<>(highlightInfo);
      this.elementForQuickDoc = elementForQuickDoc == null ? null : new WeakReference<>(elementForQuickDoc);
    }

    private PsiElement getElementForQuickDoc() {
      return SoftReference.dereference(elementForQuickDoc);
    }

    private HighlightInfo getHighlightInfo() {
      return SoftReference.dereference(highlightInfo);
    }

    private Relation compareTo(Context other) {
      if (other == null) return Relation.DIFFERENT;
      HighlightInfo highlightInfo = getHighlightInfo();
      if (!Objects.equals(highlightInfo, other.getHighlightInfo())) return Relation.DIFFERENT;
      return Objects.equals(getElementForQuickDoc(), other.getElementForQuickDoc())
             ? Relation.SAME
             : highlightInfo == null ? Relation.DIFFERENT : Relation.SIMILAR;
    }

    long getShowingDelay() {
      return Math.max(0, EditorSettingsExternalizable.getInstance().getTooltipsDelay() - (System.currentTimeMillis() - startTimestamp));
    }

    private static int getElementStartHostOffset(@NotNull PsiElement element) {
      int offset = element.getTextRange().getStartOffset();
      Project project = element.getProject();
      PsiFile containingFile = element.getContainingFile();
      if (containingFile != null && InjectedLanguageManager.getInstance(project).isInjectedFragment(containingFile)) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        if (document instanceof DocumentWindow) {
          return ((DocumentWindow)document).injectedToHost(offset);
        }
      }
      return offset;
    }

    @NotNull
    private VisualPosition getPopupPosition(Editor editor) {
      HighlightInfo highlightInfo = getHighlightInfo();
      if (highlightInfo == null) {
        int offset = targetOffset;
        PsiElement elementForQuickDoc = getElementForQuickDoc();
        if (elementForQuickDoc != null && elementForQuickDoc.isValid()) {
          offset = getElementStartHostOffset(elementForQuickDoc);
        }
        return editor.offsetToVisualPosition(offset);
      }
      else {
        VisualPosition targetPosition = editor.offsetToVisualPosition(targetOffset);
        VisualPosition endPosition = editor.offsetToVisualPosition(highlightInfo.getEndOffset());
        if (endPosition.line <= targetPosition.line) return targetPosition;
        Point targetPoint = editor.visualPositionToXY(targetPosition);
        Point endPoint = editor.visualPositionToXY(endPosition);
        Point resultPoint = new Point(targetPoint.x, endPoint.x > targetPoint.x ? endPoint.y : editor.visualLineToY(endPosition.line - 1));
        return editor.xyToVisualPosition(resultPoint);
      }
    }

    @Nullable
    private Info calcInfo(@NotNull Editor editor) {
      HighlightInfo info = getHighlightInfo();
      HighlightInfo infoToUse = null;
      TooltipAction tooltipAction = null;
      if (info != null && info.getDescription() != null && info.getToolTip() != null) {
        infoToUse = info;
        try {
          tooltipAction = ReadAction.nonBlocking(() -> TooltipActionProvider.calcTooltipAction(info, editor)).executeSynchronously();
        }
        catch (IndexNotReadyException ignored) {
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception e) {
          LOG.warn(e);
        }
      }

      String quickDocMessage = null;
      PsiElement targetElement = null;
      PsiElement element = getElementForQuickDoc();
      if (element != null) {
        try {
          Project project = Objects.requireNonNull(editor.getProject());
          DocumentationManager documentationManager =
            ReadAction.compute(() -> project.isDisposed() ? null : DocumentationManager.getInstance(project));
          if (documentationManager != null) {
            targetElement = ReadAction.nonBlocking(() -> {
              if (element.isValid()) {
                PsiFile containingFile = element.getContainingFile();
                Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, null, containingFile);
                int offset = injectedEditor instanceof EditorWindow
                             ? ((EditorWindow)injectedEditor).getDocument().hostToInjected(targetOffset)
                             : targetOffset;
                return documentationManager.findTargetElement(injectedEditor, offset, containingFile, element);
              }
              return null;
            }).executeSynchronously();
            if (targetElement != null) {
              quickDocMessage = documentationManager.generateDocumentation(targetElement, element, true);
              if (quickDocMessage != null && quickDocMessage.length() > MAX_QUICK_DOC_CHARACTERS) {
                quickDocMessage = quickDocMessage.substring(0, MAX_QUICK_DOC_CHARACTERS);
              }
            }
          }
        }
        catch (IndexNotReadyException ignored) {
        }
        catch (ProcessCanceledException ignored) {
        }
        catch (Exception e) {
          LOG.warn(e);
        }
      }
      return infoToUse == null && quickDocMessage == null ? null : new Info(infoToUse, tooltipAction, quickDocMessage, targetElement);
    }

    private enum Relation {
      SAME, // no need to update popup
      SIMILAR, // popup needs to be updated
      DIFFERENT // popup needs to be closed, and new one shown
    }
  }

  private static class Info {
    private final HighlightInfo highlightInfo;
    private final TooltipAction tooltipAction;

    private final String quickDocMessage;
    private final WeakReference<PsiElement> quickDocElement;


    private Info(HighlightInfo highlightInfo, TooltipAction tooltipAction, String quickDocMessage, PsiElement quickDocElement) {
      assert highlightInfo != null || quickDocMessage != null;
      this.highlightInfo = highlightInfo;
      this.tooltipAction = tooltipAction;
      this.quickDocMessage = quickDocMessage;
      this.quickDocElement = new WeakReference<>(quickDocElement);
    }

    private JComponent createComponent(Editor editor, PopupBridge popupBridge, boolean requestFocus) {
      boolean quickDocShownInPopup = quickDocMessage != null &&
                                     ToolWindowManager.getInstance(Objects.requireNonNull(editor.getProject()))
                                       .getToolWindow(ToolWindowId.DOCUMENTATION) == null;
      JComponent c1 = createHighlightInfoComponent(editor, !quickDocShownInPopup, popupBridge, requestFocus);
      DocumentationComponent c2 = createQuickDocComponent(editor, c1 != null, popupBridge);
      assert quickDocShownInPopup == (c2 != null);
      if (c1 == null && c2 == null) return null;
      JPanel p = new JPanel(new CombinedPopupLayout(c1, c2));
      p.setBorder(null);
      if (c1 != null) p.add(c1);
      if (c2 != null) p.add(c2);
      return p;
    }

    private JComponent createHighlightInfoComponent(Editor editor,
                                                    boolean highlightActions,
                                                    PopupBridge popupBridge,
                                                    boolean requestFocus) {
      if (highlightInfo == null) return null;
      ErrorStripTooltipRendererProvider provider = ((EditorMarkupModel)editor.getMarkupModel()).getErrorStripTooltipRendererProvider();
      TooltipRenderer tooltipRenderer = provider.calcTooltipRenderer(Objects.requireNonNull(highlightInfo.getToolTip()), tooltipAction, -1);
      if (!(tooltipRenderer instanceof LineTooltipRenderer)) return null;
      return createHighlightInfoComponent(editor, (LineTooltipRenderer)tooltipRenderer, highlightActions, popupBridge, requestFocus);
    }

    private static JComponent createHighlightInfoComponent(Editor editor,
                                                           LineTooltipRenderer renderer,
                                                           boolean highlightActions,
                                                           PopupBridge popupBridge,
                                                           boolean requestFocus) {
      Ref<WrapperPanel> wrapperPanelRef = new Ref<>();
      Ref<LightweightHint> mockHintRef = new Ref<>();
      HintHint hintHint = new HintHint().setAwtTooltip(true).setRequestFocus(requestFocus);
      LightweightHint hint =
        renderer.createHint(editor, new Point(), false, EDITOR_INFO_GROUP, hintHint, highlightActions, false, expand -> {
          LineTooltipRenderer newRenderer = renderer.createRenderer(renderer.getText(), expand ? 1 : 0);
          JComponent newComponent = createHighlightInfoComponent(editor, newRenderer, highlightActions, popupBridge, requestFocus);
          AbstractPopup popup = popupBridge.getPopup();
          WrapperPanel wrapper = wrapperPanelRef.get();
          if (newComponent != null && popup != null && wrapper != null) {
            LightweightHint mockHint = mockHintRef.get();
            if (mockHint != null) closeHintIgnoreBinding(mockHint);
            wrapper.setContent(newComponent);
            validatePopupSize(popup);
          }
        });
      if (hint == null) return null;
      mockHintRef.set(hint);
      bindHintHiding(hint, popupBridge);
      JComponent component = hint.getComponent();
      LOG.assertTrue(component instanceof WidthBasedLayout, "Unexpected type of tooltip component: " + component.getClass());
      WrapperPanel wrapper = new WrapperPanel(component);
      wrapperPanelRef.set(wrapper);
      // emulating LightweightHint+IdeTooltipManager+BalloonImpl - they use the same background
      wrapper.setBackground(hintHint.getTextBackground());
      wrapper.setOpaque(true);
      return wrapper;
    }

    private static void bindHintHiding(LightweightHint hint, PopupBridge popupBridge) {
      AtomicBoolean inProcess = new AtomicBoolean();
      hint.addHintListener(e -> {
        if (hint.getUserData(DISABLE_BINDING) == null && inProcess.compareAndSet(false, true)) {
          try {
            AbstractPopup popup = popupBridge.getPopup();
            if (popup != null) {
              popup.cancel();
            }
          }
          finally {
            inProcess.set(false);
          }
        }
      });
      popupBridge.performOnCancel(() -> {
        if (hint.getUserData(DISABLE_BINDING) == null && inProcess.compareAndSet(false, true)) {
          try {
            hint.hide();
          }
          finally {
            inProcess.set(false);
          }
        }
      });
    }

    private static void closeHintIgnoreBinding(LightweightHint hint) {
      hint.putUserData(DISABLE_BINDING, Boolean.TRUE);
      hint.hide();
    }

    @Nullable
    private DocumentationComponent createQuickDocComponent(Editor editor,
                                                           boolean deEmphasize,
                                                           PopupBridge popupBridge) {
      if (quickDocMessage == null) return null;
      PsiElement element = quickDocElement.get();
      Project project = Objects.requireNonNull(editor.getProject());
      DocumentationManager documentationManager = DocumentationManager.getInstance(project);
      ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION);
      if (toolWindow != null) {
        if (element != null) {
          documentationManager.showJavaDocInfo(editor, element, extractOriginalElement(element), null, quickDocMessage, true, false);
          documentationManager.setAllowContentUpdateFromContext(false);
        }
        return null;
      }
      class MyDocComponent extends DocumentationComponent {
        private MyDocComponent() {
          super(documentationManager, false);
          if (deEmphasize) {
            setBackground(UIUtil.getToolTipActionBackground());
          }
        }

        @Override
        protected void showHint() {
          AbstractPopup popup = popupBridge.getPopup();
          if (popup != null) {
            validatePopupSize(popup);
          }
        }
      }
      DocumentationComponent component = new MyDocComponent();
      if (deEmphasize) {
        component.setBorder(IdeBorderFactory.createBorder(UIUtil.getTooltipSeparatorColor(), SideBorder.TOP));
      }
      component.setData(element, quickDocMessage, null, null, null);
      component.setToolwindowCallback(() -> {
        PsiElement docElement = component.getElement();
        if (docElement != null) {
          documentationManager.createToolWindow(docElement, extractOriginalElement(docElement));
          ToolWindow createdToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION);
          if (createdToolWindow != null) {
            createdToolWindow.setAutoHide(false);
          }
        }
        AbstractPopup popup = popupBridge.getPopup();
        if (popup != null) {
          popup.cancel();
        }
      });
      popupBridge.performWhenAvailable(component::setHint);
      EditorUtil.disposeWithEditor(editor, component);
      popupBridge.performOnCancel(() -> Disposer.dispose(component));
      return component;
    }
  }

  private static PsiElement extractOriginalElement(PsiElement element) {
    if (element == null) {
      return null;
    }
    SmartPsiElementPointer<?> originalElementPointer = element.getUserData(DocumentationManager.ORIGINAL_ELEMENT_KEY);
    return originalElementPointer == null ? null : originalElementPointer.getElement();
  }

  @NotNull
  public static EditorMouseHoverPopupManager getInstance() {
    return ServiceManager.getService(EditorMouseHoverPopupManager.class);
  }

  @Nullable
  public DocumentationComponent getDocumentationComponent() {
    AbstractPopup hint = getCurrentHint();
    return hint == null ? null : UIUtil.findComponentOfType(hint.getComponent(), DocumentationComponent.class);
  }

  private static class PopupBridge {
    private AbstractPopup popup;
    private List<Consumer<AbstractPopup>> consumers = new ArrayList<>();

    private void setPopup(@NotNull AbstractPopup popup) {
      assert this.popup == null;
      this.popup = popup;
      consumers.forEach(c -> c.accept(popup));
      consumers = null;
    }

    @Nullable
    private AbstractPopup getPopup() {
      return popup;
    }

    private void performWhenAvailable(@NotNull Consumer<AbstractPopup> consumer) {
      if (popup == null) {
        consumers.add(consumer);
      }
      else {
        consumer.accept(popup);
      }
    }

    private void performOnCancel(@NotNull Runnable runnable) {
      performWhenAvailable(popup -> popup.addListener(new JBPopupListener() {
        @Override
        public void onClosed(@NotNull LightweightWindowEvent event) {
          runnable.run();
        }
      }));
    }
  }

  private static class WrapperPanel extends JPanel implements WidthBasedLayout {
    private WrapperPanel(JComponent content) {
      super(new BorderLayout());
      setBorder(null);
      setContent(content);
    }

    private void setContent(JComponent content) {
      removeAll();
      add(content, BorderLayout.CENTER);
    }

    private JComponent getComponent() {
      return (JComponent)getComponent(0);
    }

    @Override
    public int getPreferredWidth() {
      return WidthBasedLayout.getPreferredWidth(getComponent());
    }

    @Override
    public int getPreferredHeight(int width) {
      return WidthBasedLayout.getPreferredHeight(getComponent(), width);
    }
  }

  private static class CombinedPopupLayout implements LayoutManager {
    private final JComponent highlightInfoComponent;
    private final DocumentationComponent quickDocComponent;

    private CombinedPopupLayout(JComponent highlightInfoComponent, DocumentationComponent quickDocComponent) {
      this.highlightInfoComponent = highlightInfoComponent;
      this.quickDocComponent = quickDocComponent;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      int w1 = WidthBasedLayout.getPreferredWidth(highlightInfoComponent);
      int w2 = WidthBasedLayout.getPreferredWidth(quickDocComponent);
      int preferredWidth = Math.min(JBUI.scale(MAX_POPUP_WIDTH), Math.max(w1, w2));
      int h1 = WidthBasedLayout.getPreferredHeight(highlightInfoComponent, preferredWidth);
      int h2 = WidthBasedLayout.getPreferredHeight(quickDocComponent, preferredWidth);
      return new Dimension(preferredWidth, h1 + h2);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      Dimension d1 = highlightInfoComponent == null ? new Dimension() : highlightInfoComponent.getMinimumSize();
      Dimension d2 = quickDocComponent == null ? new Dimension() : quickDocComponent.getMinimumSize();
      return new Dimension(Math.max(d1.width, d2.width), d1.height + d2.height);
    }

    @Override
    public void layoutContainer(Container parent) {
      int width = parent.getWidth();
      int height = parent.getHeight();
      if (highlightInfoComponent == null) {
        if (quickDocComponent != null) quickDocComponent.setBounds(0, 0, width, height);
      }
      else if (quickDocComponent == null) {
        highlightInfoComponent.setBounds(0, 0, width, height);
      }
      else {
        int h1 = Math.min(height, highlightInfoComponent.getPreferredSize().height);
        highlightInfoComponent.setBounds(0, 0, width, h1);
        quickDocComponent.setBounds(0, h1, width, height - h1);
      }
    }
  }

  static final class MyEditorMouseMotionEventListener implements EditorMouseMotionListener {
    @Override
    public void mouseMoved(@NotNull EditorMouseEvent e) {
      getInstance().handleMouseMoved(e);
    }
  }

  static final class MyEditorMouseEventListener implements EditorMouseListener {
    @Override
    public void mouseEntered(@NotNull EditorMouseEvent event) {
      // we receive MOUSE_MOVED event after MOUSE_ENTERED even if mouse wasn't physically moved,
      // e.g. if a popup overlapping editor has been closed
      getInstance().skipNextMovement();
    }

    @Override
    public void mouseExited(@NotNull EditorMouseEvent event) {
      getInstance().cancelCurrentProcessing();
    }

    @Override
    public void mousePressed(@NotNull EditorMouseEvent event) {
      getInstance().cancelProcessingAndCloseHint();
    }
  }

  private static class MyActionListener implements AnActionListener {
    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
      if (action instanceof HintManagerImpl.ActionToIgnore) {
        return;
      }
      AbstractPopup currentHint = getInstance().getCurrentHint();
      if (currentHint != null) {
        Component contextComponent = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        JBPopup contextPopup = PopupUtil.getPopupContainerFor(contextComponent);
        if (contextPopup == currentHint) {
          return;
        }
      }
      getInstance().cancelProcessingAndCloseHint();
    }

    @Override
    public void beforeEditorTyping(char c, @NotNull DataContext dataContext) {
      getInstance().cancelProcessingAndCloseHint();
    }
  }
}
