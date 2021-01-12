// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.MouseShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.concurrency.CancellablePromise;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_DECLARATION;

@ApiStatus.Internal
public final class CtrlMouseHandler {
  static final Logger LOG = Logger.getInstance(CtrlMouseHandler.class);

  private final Project myProject;

  private HighlightersSet myHighlighter;
  @JdkConstants.InputEventMask private int myStoredModifiers;
  private TooltipProvider myTooltipProvider;
  @Nullable private Point myPrevMouseLocation;
  private LightweightHint myHint;

  private final KeyListener myEditorKeyListener = new KeyAdapter() {
    @Override
    public void keyPressed(final KeyEvent e) {
      handleKey(e);
    }

    @Override
    public void keyReleased(final KeyEvent e) {
      handleKey(e);
    }

    private void handleKey(final KeyEvent e) {
      int modifiers = e.getModifiers();
      if (modifiers == myStoredModifiers) {
        return;
      }
      CtrlMouseAction action = getCtrlMouseAction(modifiers);
      if (action == null) {
        disposeHighlighter();
        cancelPreviousTooltip();
      }
      else {
        TooltipProvider tooltipProvider = myTooltipProvider;
        if (tooltipProvider != null) {
          if (action != tooltipProvider.getAction()) {
            disposeHighlighter();
          }
          myStoredModifiers = modifiers;
          cancelPreviousTooltip();
          myTooltipProvider = new TooltipProvider(tooltipProvider, action);
          myTooltipProvider.execute();
        }
      }
    }
  };

  private final VisibleAreaListener myVisibleAreaListener = __ -> {
    disposeHighlighter();
    cancelPreviousTooltip();
  };

  private final EditorMouseListener myEditorMouseAdapter = new EditorMouseListener() {
    @Override
    public void mouseReleased(@NotNull EditorMouseEvent e) {
      disposeHighlighter();
      cancelPreviousTooltip();
    }
  };

  private final EditorMouseMotionListener myEditorMouseMotionListener = new EditorMouseMotionListener() {
    @Override
    public void mouseMoved(@NotNull final EditorMouseEvent e) {
      if (e.isConsumed() || !myProject.isInitialized() || myProject.isDisposed()) {
        return;
      }
      MouseEvent mouseEvent = e.getMouseEvent();

      Point prevLocation = myPrevMouseLocation;
      myPrevMouseLocation = mouseEvent.getLocationOnScreen();
      if (isMouseOverTooltip(mouseEvent.getLocationOnScreen())
          || ScreenUtil.isMovementTowards(prevLocation, mouseEvent.getLocationOnScreen(), getHintBounds())) {
        return;
      }
      cancelPreviousTooltip();

      myStoredModifiers = mouseEvent.getModifiers();
      CtrlMouseAction ctrlMouseAction = getCtrlMouseAction(myStoredModifiers);
      if (ctrlMouseAction == null || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
        disposeHighlighter();
        return;
      }

      Editor editor = e.getEditor();
      if (!(editor instanceof EditorEx) || editor.getProject() != null && editor.getProject() != myProject) return;
      if (!e.isOverText()) {
        disposeHighlighter();
        return;
      }
      myTooltipProvider = new TooltipProvider((EditorEx)editor, e.getLogicalPosition(), ctrlMouseAction);
      myTooltipProvider.execute();
    }
  };

  public CtrlMouseHandler(@NotNull Project project) {
    myProject = project;
    StartupManager.getInstance(project).runAfterOpened(() -> {
      EditorEventMulticaster eventMulticaster = EditorFactory.getInstance().getEventMulticaster();
      eventMulticaster.addEditorMouseListener(myEditorMouseAdapter, project);
      eventMulticaster.addEditorMouseMotionListener(myEditorMouseMotionListener, project);
      eventMulticaster.addCaretListener(new CaretListener() {
        @Override
        public void caretPositionChanged(@NotNull CaretEvent e) {
          if (myHint != null) {
            DocumentationManager.getInstance(myProject).updateToolwindowContext();
          }
        }
      }, project);
    });
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener(){
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        disposeHighlighter();
        cancelPreviousTooltip();
      }
    });
  }

  private void cancelPreviousTooltip() {
    if (myTooltipProvider != null) {
      myTooltipProvider.dispose();
      myTooltipProvider = null;
    }
  }

  private boolean isMouseOverTooltip(@NotNull Point mouseLocationOnScreen) {
    Rectangle bounds = getHintBounds();
    return bounds != null && bounds.contains(mouseLocationOnScreen);
  }

  @Nullable
  private Rectangle getHintBounds() {
    LightweightHint hint = myHint;
    if (hint == null) {
      return null;
    }
    JComponent hintComponent = hint.getComponent();
    if (!hintComponent.isShowing()) {
      return null;
    }
    return new Rectangle(hintComponent.getLocationOnScreen(), hintComponent.getSize());
  }

  private static @Nullable CtrlMouseAction getCtrlMouseAction(@JdkConstants.InputEventMask int modifiers) {
    if (modifiers == 0) {
      return null;
    }
    KeymapManager keymapManager = KeymapManager.getInstance();
    if (keymapManager == null) {
      return null;
    }
    MouseShortcut shortcut = new MouseShortcut(MouseEvent.BUTTON1, modifiers, 1);
    List<String> actionIds = keymapManager.getActiveKeymap().getActionIds(shortcut);
    return ContainerUtil.getOnlyItem(ContainerUtil.mapNotNull(actionIds, CtrlMouseHandler::getCtrlMouseAction));
  }

  private static @Nullable CtrlMouseAction getCtrlMouseAction(@NotNull String actionId) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    return action instanceof CtrlMouseAction ? (CtrlMouseAction)action : null;
  }

  private static boolean areSimilar(@NotNull CtrlMouseInfo info1, @NotNull CtrlMouseInfo info2) {
    return info1.getRanges().equals(info2.getRanges());
  }

  private static boolean isValidAndRangesAreCorrect(@NotNull CtrlMouseInfo info, @NotNull Document document) {
    if (!info.isValid()) {
      return false;
    }
    List<TextRange> ranges = info.getRanges();
    final TextRange docRange = new TextRange(0, document.getTextLength());
    for (TextRange range : ranges) {
      if (!docRange.contains(range)) return false;
    }
    return true;
  }

  private static void showDumbModeNotification(@NotNull Project project) {
    DumbService.getInstance(project).showDumbModeNotification(
      CodeInsightBundle.message("notification.element.information.is.not.available.during.index.update"));
  }

  private void disposeHighlighter() {
    HighlightersSet highlighter = myHighlighter;
    if (highlighter != null) {
      myHighlighter = null;
      highlighter.uninstall();
      HintManager.getInstance().hideAllHints();
    }
  }

  private void updateText(@NotNull String updatedText,
                          @NotNull Consumer<? super String> newTextConsumer,
                          @NotNull LightweightHint hint,
                          @NotNull Editor editor) {
    UIUtil.invokeLaterIfNeeded(() -> {
      // There is a possible case that quick doc control width is changed, e.g. it contained text
      // like 'public final class String implements java.io.Serializable, java.lang.Comparable<java.lang.String>' and
      // new text replaces fully-qualified class names by hyperlinks with short name.
      // That's why we might need to update the control size. We assume that the hint component is located at the
      // layered pane, so, the algorithm is to find an ancestor layered pane and apply new size for the target component.
      JComponent component = hint.getComponent();
      Dimension oldSize = component.getPreferredSize();
      newTextConsumer.consume(updatedText);


      Dimension newSize = component.getPreferredSize();
      if (newSize.width == oldSize.width) {
        return;
      }
      component.setPreferredSize(new Dimension(newSize.width, newSize.height));

      // We're assuming here that there are two possible hint representation modes: popup and layered pane.
      if (hint.isRealPopup()) {

        TooltipProvider tooltipProvider = myTooltipProvider;
        if (tooltipProvider != null) {
          // There is a possible case that 'raw' control was rather wide but the 'rich' one is narrower. That's why we try to
          // re-show the hint here. Benefits: there is a possible case that we'll be able to show nice layered pane-based balloon;
          // the popup will be re-positioned according to the new width.
          hint.hide();
          tooltipProvider.showHint(new LightweightHint(component), editor);
        }
        else {
          component.setPreferredSize(new Dimension(newSize.width, oldSize.height));
          hint.pack();
        }
        return;
      }

      Container topLevelLayeredPaneChild = null;
      boolean adjustBounds = false;
      for (Container current = component.getParent(); current != null; current = current.getParent()) {
        if (current instanceof JLayeredPane) {
          adjustBounds = true;
          break;
        }
        else {
          topLevelLayeredPaneChild = current;
        }
      }

      if (adjustBounds && topLevelLayeredPaneChild != null) {
        Rectangle bounds = topLevelLayeredPaneChild.getBounds();
        topLevelLayeredPaneChild.setBounds(bounds.x, bounds.y, bounds.width + newSize.width - oldSize.width, bounds.height);
      }
    });
  }

  private final class TooltipProvider {

    private final @NotNull EditorEx myHostEditor;
    private final int myHostOffset;
    private final @NotNull CtrlMouseAction myAction;

    private boolean myDisposed;
    private CancellablePromise<?> myExecutionProgress;

    TooltipProvider(@NotNull EditorEx hostEditor, @NotNull LogicalPosition hostPos, @NotNull CtrlMouseAction action) {
      myHostEditor = hostEditor;
      myHostOffset = hostEditor.logicalPositionToOffset(hostPos);
      myAction = action;
    }

    TooltipProvider(@NotNull TooltipProvider source, @NotNull CtrlMouseAction action) {
      myHostEditor = source.myHostEditor;
      myHostOffset = source.myHostOffset;
      myAction = action;
    }

    void dispose() {
      myDisposed = true;
      if (myExecutionProgress != null) {
        myExecutionProgress.cancel();
      }
    }

    @NotNull CtrlMouseAction getAction() {
      return myAction;
    }

    void execute() {
      if (PsiDocumentManager.getInstance(myProject).getPsiFile(myHostEditor.getDocument()) == null) return;

      int selStart = myHostEditor.getSelectionModel().getSelectionStart();
      int selEnd = myHostEditor.getSelectionModel().getSelectionEnd();

      if (myHostOffset >= selStart && myHostOffset < selEnd) {
        disposeHighlighter();
        return;
      }

      myExecutionProgress = ReadAction
        .nonBlocking(() -> doExecute())
        .withDocumentsCommitted(myProject)
        .expireWhen(() -> isTaskOutdated(myHostEditor))
        .finishOnUiThread(ModalityState.defaultModalityState(), Runnable::run)
        .submit(AppExecutorUtil.getAppExecutorService());
    }

    private Runnable createDisposalContinuation() {
      return CtrlMouseHandler.this::disposeHighlighter;
    }

    @NotNull
    private Runnable doExecute() {
      EditorEx editor = getPossiblyInjectedEditor();
      int offset = getOffset(editor);

      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (file == null) return createDisposalContinuation();

      final CtrlMouseInfo info;
      final CtrlMouseDocInfo docInfo;
      try {
        info = myAction.getCtrlMouseInfo(editor, file, offset);
        if (info == null) return createDisposalContinuation();
        docInfo = info.getDocInfo();
      }
      catch (IndexNotReadyException e) {
        showDumbModeNotification(myProject);
        return createDisposalContinuation();
      }

      LOG.debug("Obtained info about element under cursor");
      return () -> addHighlighterAndShowHint(info, docInfo, editor);
    }

    @NotNull
    private EditorEx getPossiblyInjectedEditor() {
      final Document document = myHostEditor.getDocument();
      if (PsiDocumentManager.getInstance(myProject).isCommitted(document)) {
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        return (EditorEx)InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(myHostEditor, psiFile, myHostOffset);
      }
      return myHostEditor;
    }

    private boolean isTaskOutdated(@NotNull Editor editor) {
      return myDisposed || myProject.isDisposed() || editor.isDisposed() ||
             !ApplicationManager.getApplication().isUnitTestMode() && !EditorActivityManager.getInstance().isVisible(editor);
    }

    private int getOffset(@NotNull Editor editor) {
      return editor instanceof EditorWindow ? ((EditorWindow)editor).getDocument().hostToInjected(myHostOffset) : myHostOffset;
    }

    private void addHighlighterAndShowHint(@NotNull CtrlMouseInfo info, @NotNull CtrlMouseDocInfo docInfo, @NotNull EditorEx editor) {
      if (myDisposed || editor.isDisposed()) return;
      if (myHighlighter != null) {
        if (!areSimilar(info, myHighlighter.getStoredInfo())) {
          disposeHighlighter();
        }
        else {
          // highlighter already set
          if (info.isNavigatable()) {
            editor.setCustomCursor(CtrlMouseHandler.class, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
          return;
        }
      }

      if (!isValidAndRangesAreCorrect(info, editor.getDocument()) || !info.isNavigatable() && docInfo.text == null) {
        return;
      }

      boolean highlighterOnly = EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement() &&
                                DocumentationManager.getInstance(myProject).getDocInfoHint() != null;

      myHighlighter = installHighlighterSet(info, editor, highlighterOnly);

      if (highlighterOnly || docInfo.text == null) return;

      HyperlinkListener hyperlinkListener = docInfo.docProvider == null || docInfo.context == null
                                   ? null
                                   : new QuickDocHyperlinkListener(docInfo.docProvider, docInfo.context);
      Ref<Consumer<? super String>> newTextConsumerRef = new Ref<>();
      JComponent component = HintUtil.createInformationLabel(docInfo.text, hyperlinkListener, null, newTextConsumerRef);
      component.setBorder(JBUI.Borders.empty(6, 6, 5, 6));

      final LightweightHint hint = new LightweightHint(wrapInScrollPaneIfNeeded(component, editor));

      myHint = hint;
      hint.addHintListener(__ -> myHint = null);

      showHint(hint, editor);

      Consumer<? super String> newTextConsumer = newTextConsumerRef.get();
      if (newTextConsumer != null) {
        updateOnPsiChanges(hint, info, newTextConsumer, docInfo.text, editor);
      }
    }

    @NotNull
    private JComponent wrapInScrollPaneIfNeeded(@NotNull JComponent component, @NotNull Editor editor) {
      if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
        Dimension preferredSize = component.getPreferredSize();
        Dimension maxSize = getMaxPopupSize(editor);
        if (preferredSize.width > maxSize.width || preferredSize.height > maxSize.height) {
          // We expect documentation providers to exercise good judgement in limiting the displayed information,
          // but in any case, we don't want the hint to cover the whole screen, so we also implement certain limiting here.
          JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(component, true);
          scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width, maxSize.width),
                                                    Math.min(preferredSize.height, maxSize.height)));
          return scrollPane;
        }
      }
      return component;
    }

    @NotNull
    private Dimension getMaxPopupSize(@NotNull Editor editor) {
      Rectangle rectangle = ScreenUtil.getScreenRectangle(editor.getContentComponent());
      return new Dimension((int)(0.9 * Math.max(640, rectangle.width)), (int)(0.33 * Math.max(480, rectangle.height)));
    }

    private void updateOnPsiChanges(@NotNull LightweightHint hint,
                                    @NotNull CtrlMouseInfo info,
                                    @NotNull Consumer<? super String> textConsumer,
                                    @NotNull String oldText,
                                    @NotNull Editor editor) {
      if (!hint.isVisible()) return;
      Disposable hintDisposable = Disposer.newDisposable("CtrlMouseHandler.TooltipProvider.updateOnPsiChanges");
      hint.addHintListener(__ -> Disposer.dispose(hintDisposable));
      myProject.getMessageBus().connect(hintDisposable).subscribe(PsiModificationTracker.TOPIC, () -> ReadAction
        .nonBlocking(() -> {
          try {
            CtrlMouseDocInfo newDocInfo = info.getDocInfo();
            return (Runnable)() -> {
              if (newDocInfo.text != null && !oldText.equals(newDocInfo.text)) {
                updateText(newDocInfo.text, textConsumer, hint, editor);
              }
            };
          }
          catch (IndexNotReadyException e) {
            showDumbModeNotification(myProject);
            return createDisposalContinuation();
          }
        })
        .finishOnUiThread(ModalityState.defaultModalityState(), Runnable::run)
        .withDocumentsCommitted(myProject)
        .expireWith(hintDisposable)
        .expireWhen(() -> !isValidAndRangesAreCorrect(info, editor.getDocument()))
        .coalesceBy(CtrlMouseHandler.class, hint)
        .submit(AppExecutorUtil.getAppExecutorService()));
    }

    public void showHint(@NotNull LightweightHint hint, @NotNull Editor editor) {
      if (ApplicationManager.getApplication().isUnitTestMode() || editor.isDisposed()) return;
      final HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
      short constraint = HintManager.ABOVE;
      LogicalPosition position = editor.offsetToLogicalPosition(getOffset(editor));
      Point p = HintManagerImpl.getHintPosition(hint, editor, position, constraint);
      if (p.y - hint.getComponent().getPreferredSize().height < 0) {
        constraint = HintManager.UNDER;
        p = HintManagerImpl.getHintPosition(hint, editor, position, constraint);
      }
      hintManager.showEditorHint(hint, editor, p,
                                 HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING,
                                 0, false, HintManagerImpl.createHintHint(editor, p, hint, constraint).setContentActive(false));
    }
  }

  @NotNull
  private HighlightersSet installHighlighterSet(@NotNull CtrlMouseInfo info, @NotNull EditorEx editor, boolean highlighterOnly) {
    editor.getContentComponent().addKeyListener(myEditorKeyListener);
    editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
    if (info.isNavigatable()) {
      editor.setCustomCursor(CtrlMouseHandler.class, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    List<RangeHighlighter> highlighters = new ArrayList<>();

    if (!highlighterOnly || info.isNavigatable()) {
      TextAttributes attributes = info.isNavigatable()
                                  ? EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR)
                                  : new TextAttributes(null, HintUtil.getInformationColor(), null, null, Font.PLAIN);
      for (TextRange range : info.getRanges()) {
        TextAttributes attr = NavigationUtil.patchAttributesColor(attributes, range, editor);
        final RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(range.getStartOffset(), range.getEndOffset(),
                                                                                         HighlighterLayer.HYPERLINK,
                                                                                         attr,
                                                                                         HighlighterTargetArea.EXACT_RANGE);
        highlighters.add(highlighter);
      }
    }

    return new HighlightersSet(highlighters, editor, info);
  }

  @TestOnly
  public boolean isCalculationInProgress() {
    TooltipProvider provider = myTooltipProvider;
    if (provider == null) return false;
    Future<?> progress = provider.myExecutionProgress;
    if (progress == null) return false;
    return !progress.isDone();
  }

  private final class HighlightersSet {
    @NotNull private final List<? extends RangeHighlighter> myHighlighters;
    @NotNull private final EditorEx myHighlighterView;
    @NotNull private final CtrlMouseInfo myStoredInfo;

    private HighlightersSet(@NotNull List<? extends RangeHighlighter> highlighters,
                            @NotNull EditorEx highlighterView,
                            @NotNull CtrlMouseInfo storedInfo) {
      myHighlighters = highlighters;
      myHighlighterView = highlighterView;
      myStoredInfo = storedInfo;
    }

    public void uninstall() {
      for (RangeHighlighter highlighter : myHighlighters) {
        highlighter.dispose();
      }

      myHighlighterView.setCustomCursor(CtrlMouseHandler.class, null);
      myHighlighterView.getContentComponent().removeKeyListener(myEditorKeyListener);
      myHighlighterView.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
    }

    @NotNull
    CtrlMouseInfo getStoredInfo() {
      return myStoredInfo;
    }
  }

  private final class QuickDocHyperlinkListener implements HyperlinkListener {
    @NotNull private final DocumentationProvider myProvider;
    @NotNull private final PsiElement myContext;

    QuickDocHyperlinkListener(@NotNull DocumentationProvider provider, @NotNull PsiElement context) {
      myProvider = provider;
      myContext = context;
    }

    @Override
    public void hyperlinkUpdate(@NotNull HyperlinkEvent e) {
      if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
        return;
      }

      String description = e.getDescription();
      if (StringUtil.isEmpty(description) || !description.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
        return;
      }

      String elementName = e.getDescription().substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());

      DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> {
        PsiElement targetElement = myProvider.getDocumentationElementForLink(PsiManager.getInstance(myProject), elementName, myContext);
        if (targetElement != null) {
          LightweightHint hint = myHint;
          if (hint != null) {
            hint.hide(true);
          }
          DocumentationManager.getInstance(myProject).showJavaDocInfo(targetElement, myContext, null);
        }
      });
    }
  }

  @TestOnly
  public static @Nullable String getInfo(PsiElement element, PsiElement atPointer) {
    return SingleTargetElementInfo.generateInfo(element, atPointer, true).text;
  }

  @TestOnly
  public static @Nullable String getGoToDeclarationOrUsagesText(@NotNull Editor editor) {
    Project project = editor.getProject();
    if (project == null) return null;
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;
    CtrlMouseInfo ctrlMouseInfo = getCtrlMouseInfo(ACTION_GOTO_DECLARATION, editor, file, editor.getCaretModel().getOffset());
    return ctrlMouseInfo == null ? null : ctrlMouseInfo.getDocInfo().text;
  }

  @ApiStatus.Internal
  public static @Nullable CtrlMouseInfo getCtrlMouseInfo(@NotNull String actionId,
                                                         @NotNull Editor editor,
                                                         @NotNull PsiFile file,
                                                         int offset) {
    CtrlMouseAction action = getCtrlMouseAction(actionId);
    if (action == null) {
      return null;
    }
    return action.getCtrlMouseInfo(editor, file, offset);
  }
}
