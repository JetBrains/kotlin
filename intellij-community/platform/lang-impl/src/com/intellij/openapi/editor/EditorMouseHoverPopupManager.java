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
import com.intellij.openapi.util.ActionCallback;
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
import java.util.Objects;

public class EditorMouseHoverPopupManager implements EditorMouseMotionListener {
  private static final TooltipGroup EDITOR_INFO_GROUP = new TooltipGroup("EDITOR_INFO_GROUP", 0);

  private final Alarm myAlarm;
  private WeakReference<JBPopup> myPopupReference;
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
        if (info != null && editor.getContentComponent().isShowing()) {
          ActionCallback hideCallback = new ActionCallback();
          JComponent component = info.createComponent(editor, hideCallback::setDone);
          if (component == null) {
            closeHint();
          }
          else {
            if (relation == Context.Relation.SIMILAR && isHintShown()) {
              updateHint(component, hideCallback);
            }
            else {
              AbstractPopup hint = createHint(component, hideCallback);
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

  private static AbstractPopup createHint(JComponent component, ActionCallback hideCallback) {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setBorder(null);
    wrapper.add(component, BorderLayout.CENTER);
    AbstractPopup popup = (AbstractPopup)JBPopupFactory.getInstance()
      .createComponentPopupBuilder(wrapper, component)
      .setResizable(true)
      .createPopup();
    cancelPopupWhenRequested(hideCallback, popup);
    return popup;
  }

  private void updateHint(JComponent component, ActionCallback hideCallback) {
    JBPopup popup = SoftReference.dereference(myPopupReference);
    if (popup != null) {
      JPanel wrapper = (JPanel)popup.getContent();
      wrapper.removeAll();
      wrapper.add(component, BorderLayout.CENTER);
      popup.pack(true, true);
      cancelPopupWhenRequested(hideCallback, popup);
    }
  }

  private static void cancelPopupWhenRequested(ActionCallback hideCallback, JBPopup popup) {
    hideCallback.doWhenDone(() -> {
      popup.cancel();
      IdeEventQueue eventQueue = IdeEventQueue.getInstance();
      AWTEvent currentEvent = eventQueue.getTrueCurrentEvent();
      if (currentEvent instanceof MouseEvent && currentEvent.getID() == MouseEvent.MOUSE_PRESSED) { // e.g. on link activation
        // this is to prevent mouse released (and dragged, dispatched due to some reason) event to be dispatched into editor
        // alternative solution would be to activate links on mouse release, not on press
        eventQueue.blockNextEvents((MouseEvent)currentEvent);
      }
    });
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

    private JComponent createComponent(Editor editor, Runnable hide) {
      JComponent c1 = createHighlightInfoComponent(editor, highlightInfo, quickDocMessage == null, hide);
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

    private static JComponent createHighlightInfoComponent(Editor editor, HighlightInfo info, boolean highlightActions, Runnable hide) {
      if (info == null) return null;
      TooltipAction action = TooltipActionProvider.calcTooltipAction(info, editor);
      ErrorStripTooltipRendererProvider provider = ((EditorMarkupModel)editor.getMarkupModel()).getErrorStripTooltipRendererProvider();
      TooltipRenderer tooltipRenderer = provider.calcTooltipRenderer(Objects.requireNonNull(info.getToolTip()), action, -1);
      if (!(tooltipRenderer instanceof LineTooltipRenderer)) return null;
      LightweightHint hint = ((LineTooltipRenderer)tooltipRenderer).createHint(editor, new Point(), false, EDITOR_INFO_GROUP,
                                                                               new HintHint().setAwtTooltip(true), highlightActions);
      if (hint == null) return null;
      hint.addHintListener(e -> hide.run());
      return hint.getComponent();
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
}
