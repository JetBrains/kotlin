// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.QuickDocUtil;
import com.intellij.codeInsight.hint.LineTooltipRenderer;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.codeInsight.hint.TooltipRenderer;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.*;
import com.intellij.reference.SoftReference;
import com.intellij.ui.HintHint;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.SideBorder;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.PopupPositionManager;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditorMouseHoverPopupManager implements EditorMouseMotionListener {
  private static final TooltipGroup EDITOR_INFO_GROUP = new TooltipGroup("EDITOR_INFO_GROUP", 0);

  private final Alarm myAlarm;
  private WeakReference<AbstractPopup> myPopupReference;
  private Context myContext;
  private ProgressIndicator myCurrentProgress;

  public EditorMouseHoverPopupManager(Application application, EditorFactory editorFactory) {
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, application);
    editorFactory.getEventMulticaster().addEditorMouseMotionListener(this);
  }

  @Override
  public void mouseMoved(@NotNull EditorMouseEvent e) {
    if (!Registry.is("editor.new.mouse.hover.popups")) return;

    myAlarm.cancelAllRequests();
    if (myCurrentProgress != null) {
      myCurrentProgress.cancel();
      myCurrentProgress = null;
    }

    Editor editor = e.getEditor();
    int targetOffset = getTargetOffset(e);
    if (targetOffset < 0) {
      closeHint();
      return;
    }
    Context context = createContext(editor, targetOffset);
    if (context == null) {
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

    ProgressIndicatorBase progress = new ProgressIndicatorBase();
    myCurrentProgress = progress;
    myAlarm.addRequest(() -> ProgressManager.getInstance().executeProcessUnderProgress(() -> {
      Info info = context.calcInfo(editor);
      ApplicationManager.getApplication().invokeLater(() -> {
        if (progress != myCurrentProgress) return;
        myCurrentProgress = null;
        if (info != null && !EditorMouseHoverPopupControl.arePopupsDisabled(editor) && editor.getContentComponent().isShowing()) {
          PopupBridge popupBridge = new PopupBridge();
          JComponent component = info.createComponent(editor, popupBridge);
          if (component == null) {
            closeHint();
          }
          else {
            if (relation == Context.Relation.SIMILAR && isHintShown()) {
              updateHint(component, popupBridge);
            }
            else {
              AbstractPopup hint = createHint(component, popupBridge);
              showHintInEditor(hint, editor, context);
              myPopupReference = new WeakReference<>(hint);
            }
            myContext = context;
          }
        }
      });
    }, progress), Registry.intValue("editor.new.mouse.hover.popups.delay"));
  }

  private void showHintInEditor(AbstractPopup hint, Editor editor, Context context) {
    closeHint();
    editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, context.getPopupPosition(editor));
    try {
      PopupPositionManager.positionPopupInBestPosition(hint, editor, null);
    }
    finally {
      editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null);
    }
    Window window = hint.getPopupWindow();
    if (window != null) window.setFocusableWindowState(true);
  }

  private static AbstractPopup createHint(JComponent component, PopupBridge popupBridge) {
    WrapperPanel wrapper = new WrapperPanel(component);
    AbstractPopup popup = (AbstractPopup)JBPopupFactory.getInstance()
      .createComponentPopupBuilder(wrapper, component)
      .setResizable(true)
      .createPopup();
    popupBridge.setPopup(popup);
    return popup;
  }

  private void updateHint(JComponent component, PopupBridge popupBridge) {
    AbstractPopup popup = SoftReference.dereference(myPopupReference);
    if (popup != null) {
      WrapperPanel wrapper = (WrapperPanel)popup.getComponent();
      wrapper.setContent(component);
      popup.pack(true, true);
      popupBridge.setPopup(popup);
    }
  }

  private static void blockFurtherMouseEvents() {
    IdeEventQueue eventQueue = IdeEventQueue.getInstance();
    AWTEvent currentEvent = eventQueue.getTrueCurrentEvent();
    if (currentEvent instanceof MouseEvent && currentEvent.getID() == MouseEvent.MOUSE_PRESSED) { // e.g. on link activation
      // this is to prevent mouse released (and dragged, dispatched due to some reason) event to be dispatched into editor
      // alternative solution would be to activate links on mouse release, not on press
      eventQueue.blockNextEvents((MouseEvent)currentEvent);
    }
  }

  private static int getTargetOffset(EditorMouseEvent event) {
    Editor editor = event.getEditor();
    if (editor instanceof EditorEx &&
        editor.getProject() != null &&
        event.getArea() == EditorMouseEventArea.EDITING_AREA &&
        event.getMouseEvent().getModifiers() == 0 &&
        !EditorMouseHoverPopupControl.arePopupsDisabled(editor) &&
        LookupManager.getActiveLookup(editor) == null) {
      Point point = event.getMouseEvent().getPoint();
      VisualPosition visualPosition = editor.xyToVisualPosition(point);
      LogicalPosition logicalPosition = editor.visualToLogicalPosition(visualPosition);
      int offset = editor.logicalPositionToOffset(logicalPosition);
      if (editor.offsetToLogicalPosition(offset).equals(logicalPosition) && // not virtual space
          ((EditorEx)editor).getFoldingModel().getFoldingPlaceholderAt(point) == null &&
          editor.getInlayModel().getElementAt(point) == null) {
        return offset;
      }
    }
    return -1;
  }

  private static Context createContext(Editor editor, int offset) {
    Project project = Objects.requireNonNull(editor.getProject());

    HighlightInfo info = null;
    if (!Registry.is("ide.disable.editor.tooltips")) {
      info = ((DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project))
        .findHighlightByOffset(editor.getDocument(), offset, false);
    }

    PsiElement elementForQuickDoc = null;
    if (EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement()) {
      PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        elementForQuickDoc = psiFile.findElementAt(offset);
        if (elementForQuickDoc instanceof PsiWhiteSpace || elementForQuickDoc instanceof PsiPlainText) {
          elementForQuickDoc = null;
        }
      }
    }

    return info == null && elementForQuickDoc == null ? null : new Context(offset, info, elementForQuickDoc);
  }

  private void closeHint() {
    JBPopup popup = SoftReference.dereference(myPopupReference);
    if (popup != null) {
      popup.cancel();
    }
    myPopupReference = null;
    myContext = null;
  }

  private boolean isHintShown() {
    JBPopup popup = SoftReference.dereference(myPopupReference);
    return popup != null && popup.isVisible();
  }

  private static class Context {
    private final int targetOffset;
    private final WeakReference<HighlightInfo> highlightInfo;
    private final WeakReference<PsiElement> elementForQuickDoc;

    private Context(int targetOffset, HighlightInfo highlightInfo, PsiElement elementForQuickDoc) {
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

    @NotNull
    private VisualPosition getPopupPosition(Editor editor) {
      HighlightInfo highlightInfo = getHighlightInfo();
      if (highlightInfo == null) {
        int offset = targetOffset;
        PsiElement elementForQuickDoc = getElementForQuickDoc();
        if (elementForQuickDoc != null) {
          offset = elementForQuickDoc.getTextRange().getStartOffset();
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
    private Info calcInfo(Editor editor) {
      HighlightInfo info = getHighlightInfo();
      if (info != null && (info.getDescription() == null || info.getToolTip() == null)) {
        info = null;
      }

      String quickDocMessage = null;
      if (elementForQuickDoc != null) {
        PsiElement element = getElementForQuickDoc();
        Ref<PsiElement> targetElementRef = new Ref<>();
        QuickDocUtil.runInReadActionWithWriteActionPriorityWithRetries(() -> {
          if (element.isValid()) {
            targetElementRef.set(DocumentationManager.getInstance(editor.getProject()).findTargetElement(editor, targetOffset,
                                                                                                         element.getContainingFile(),
                                                                                                         element));
          }
        }, 5000, 100);

        if (!targetElementRef.isNull()) {
          quickDocMessage = DocumentationManager.getInstance(editor.getProject()).generateDocumentation(targetElementRef.get(), element);
        }
      }
      return info == null && quickDocMessage == null ? null : new Info(info, quickDocMessage);
    }

    private enum Relation {
      SAME, // no need to update popup
      SIMILAR, // popup needs to be updated
      DIFFERENT // popup needs to be closed, and new one shown
    }
  }

  private static class Info {
    private final HighlightInfo highlightInfo;
    private final String quickDocMessage;


    private Info(HighlightInfo highlightInfo, String quickDocMessage) {
      assert highlightInfo != null || quickDocMessage != null;
      this.highlightInfo = highlightInfo;
      this.quickDocMessage = quickDocMessage;
    }

    private JComponent createComponent(Editor editor, PopupBridge popupBridge) {
      JComponent c1 = createHighlightInfoComponent(editor, highlightInfo, quickDocMessage == null, popupBridge);
      JComponent c2 = createQuickDocComponent(editor, quickDocMessage, c1 != null);
      if (c1 == null && c2 == null) return null;
      JPanel p = new JPanel(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                                    JBUI.emptyInsets(), 0, 0);
      if (c1 != null) p.add(c1, c);
      c.gridy = 1;
      c.weighty = 1;
      c.fill = GridBagConstraints.BOTH;
      if (c2 != null) p.add(c2, c);
      return p;
    }

    private static JComponent createHighlightInfoComponent(Editor editor,
                                                           HighlightInfo info,
                                                           boolean highlightActions,
                                                           PopupBridge popupBridge) {
      if (info == null) return null;
      TooltipAction action = TooltipActionProvider.calcTooltipAction(info, editor);
      ErrorStripTooltipRendererProvider provider = ((EditorMarkupModel)editor.getMarkupModel()).getErrorStripTooltipRendererProvider();
      TooltipRenderer tooltipRenderer = provider.calcTooltipRenderer(Objects.requireNonNull(info.getToolTip()), action, -1);
      if (!(tooltipRenderer instanceof LineTooltipRenderer)) return null;
      return createHighlightInfoComponent(editor, (LineTooltipRenderer)tooltipRenderer, highlightActions, popupBridge);
    }

    private static JComponent createHighlightInfoComponent(Editor editor,
                                                           LineTooltipRenderer renderer,
                                                           boolean highlightActions,
                                                           PopupBridge popupBridge) {
      Ref<WrapperPanel> wrapperPanelRef = new Ref<>();
      LightweightHint hint =
        renderer.createHint(editor, new Point(), false, EDITOR_INFO_GROUP, new HintHint().setAwtTooltip(true), highlightActions, expand -> {
          LineTooltipRenderer newRenderer = renderer.createRenderer(renderer.getText(), expand ? 1 : 0);
          JComponent newComponent = createHighlightInfoComponent(editor, newRenderer, highlightActions, popupBridge);
          AbstractPopup popup = popupBridge.getPopup();
          WrapperPanel wrapper = wrapperPanelRef.get();
          if (newComponent != null && popup != null && wrapper != null) {
            wrapper.setContent(newComponent);
            popup.pack(true, true);
          }
        });
      if (hint == null) return null;
      bindHintHiding(hint, popupBridge);
      WrapperPanel wrapper = new WrapperPanel(hint.getComponent());
      wrapperPanelRef.set(wrapper);
      return wrapper;
    }

    private static void bindHintHiding(LightweightHint hint, PopupBridge popupBridge) {
      AtomicBoolean inProcess = new AtomicBoolean();
      hint.addHintListener(e -> {
        if (inProcess.compareAndSet(false, true)) {
          try {
            AbstractPopup popup = popupBridge.getPopup();
            if (popup != null) {
              popup.cancel();
              blockFurtherMouseEvents();
            }
          }
          finally {
            inProcess.set(false);
          }
        }
      });
      popupBridge.performOnCancel(() -> {
        if (inProcess.compareAndSet(false, true)) {
          try {
            hint.hide();
          }
          finally {
            inProcess.set(false);
          }
        }
      });
    }

    @Nullable
    private static JComponent createQuickDocComponent(Editor editor, String quickDocMessage, boolean deEmphasize) {
      if (quickDocMessage == null) return null;
      DocumentationComponent component = new DocumentationComponent(DocumentationManager.getInstance(editor.getProject()), false);
      if (deEmphasize) {
        component.setBackground(UIUtil.getToolTipActionBackground());
        if (component.needsToolbar()) {
          component.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
        }
      }
      component.setData(null, quickDocMessage, null, null, null);
      EditorUtil.disposeWithEditor(editor, component);
      return component;
    }
  }

  private static class PopupBridge {
    private final List<Runnable> actionsOnCancel = new ArrayList<>();
    private AbstractPopup popup;

    private void setPopup(@NotNull AbstractPopup popup) {
      assert this.popup == null;
      this.popup = popup;
      popup.addListener(new JBPopupListener() {
        @Override
        public void onClosed(@NotNull LightweightWindowEvent event) {
          actionsOnCancel.forEach(a -> a.run());
        }
      });
    }

    @Nullable
    private AbstractPopup getPopup() {
      return popup;
    }

    private void performOnCancel(@NotNull Runnable runnable) {
      actionsOnCancel.add(runnable);
    }
  }

  private static class WrapperPanel extends JPanel {
    private WrapperPanel(JComponent content) {
      super(new BorderLayout());
      setBorder(null);
      setContent(content);
    }

    private void setContent(JComponent content) {
      removeAll();
      add(content, BorderLayout.CENTER);
    }
  }
}
