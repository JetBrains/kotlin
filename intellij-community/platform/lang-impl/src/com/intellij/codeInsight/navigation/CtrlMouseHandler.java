// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ReadTask;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class CtrlMouseHandler {
  private static final Logger LOG = Logger.getInstance(CtrlMouseHandler.class);

  private final Project myProject;
  private final EditorColorsManager myEditorColorsManager;

  private HighlightersSet myHighlighter;
  @JdkConstants.InputEventMask private int myStoredModifiers;
  private TooltipProvider myTooltipProvider;
  private final DocumentationManager myDocumentationManager;
  @Nullable private Point myPrevMouseLocation;
  private LightweightHint myHint;

  public enum BrowseMode {None, Declaration, TypeDeclaration, Implementation}

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

      BrowseMode browseMode = getBrowseMode(modifiers);

      if (browseMode == BrowseMode.None) {
        disposeHighlighter();
        cancelPreviousTooltip();
      }
      else {
        TooltipProvider tooltipProvider = myTooltipProvider;
        if (tooltipProvider != null) {
          if (browseMode != tooltipProvider.getBrowseMode()) {
            disposeHighlighter();
          }
          myStoredModifiers = modifiers;
          cancelPreviousTooltip();
          myTooltipProvider = new TooltipProvider(tooltipProvider);
          myTooltipProvider.execute(browseMode);
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
      BrowseMode browseMode = getBrowseMode(myStoredModifiers);

      if (browseMode == BrowseMode.None || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
        disposeHighlighter();
        return;
      }

      Editor editor = e.getEditor();
      if (!(editor instanceof EditorEx) || editor.getProject() != null && editor.getProject() != myProject) return;
      Point point = new Point(mouseEvent.getPoint());
      if (editor.getInlayModel().getElementAt(point) != null) {
        disposeHighlighter();
        return;
      }
      myTooltipProvider = new TooltipProvider((EditorEx)editor, editor.xyToLogicalPosition(point));
      myTooltipProvider.execute(browseMode);
    }
  };

  public CtrlMouseHandler(final Project project,
                          StartupManager startupManager,
                          EditorColorsManager colorsManager,
                          @NotNull DocumentationManager documentationManager,
                          @NotNull final EditorFactory editorFactory) {
    myProject = project;
    myEditorColorsManager = colorsManager;
    startupManager.registerPostStartupActivity(new DumbAwareRunnable() {
      @Override
      public void run() {
        EditorEventMulticaster eventMulticaster = editorFactory.getEventMulticaster();
        eventMulticaster.addEditorMouseListener(myEditorMouseAdapter, project);
        eventMulticaster.addEditorMouseMotionListener(myEditorMouseMotionListener, project);
        eventMulticaster.addCaretListener(new CaretListener() {
          @Override
          public void caretPositionChanged(@NotNull CaretEvent e) {
            if (myHint != null) {
              myDocumentationManager.updateToolwindowContext();
            }
          }
        }, project);
      }
    });
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener(){
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        disposeHighlighter();
        cancelPreviousTooltip();
      }
    });
    myDocumentationManager = documentationManager;
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

  @NotNull
  private static BrowseMode getBrowseMode(@JdkConstants.InputEventMask int modifiers) {
    if (modifiers != 0) {
      final Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
      if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_DECLARATION)) return BrowseMode.Declaration;
      if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_TYPE_DECLARATION)) return BrowseMode.TypeDeclaration;
      if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_IMPLEMENTATION)) return BrowseMode.Implementation;
    }
    return BrowseMode.None;
  }

  @Nullable
  @TestOnly
  public static String getInfo(PsiElement element, PsiElement atPointer) {
    return generateInfo(element, atPointer, true).text;
  }

  @Nullable
  @TestOnly
  public static String getInfo(@NotNull Editor editor, BrowseMode browseMode) {
    Project project = editor.getProject();
    if (project == null) return null;
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;
    Info info = getInfoAt(project, editor, file, editor.getCaretModel().getOffset(), browseMode);
    return info == null ? null : info.getInfo().text;
  }

  @NotNull
  private static DocInfo generateInfo(PsiElement element, PsiElement atPointer, boolean fallbackToBasicInfo) {
    final DocumentationProvider documentationProvider = DocumentationManager.getProviderFromElement(element, atPointer);
    String result = documentationProvider.getQuickNavigateInfo(element, atPointer);
    if (result == null && fallbackToBasicInfo) {
      result = doGenerateInfo(element);
    }
    return result == null ? DocInfo.EMPTY : new DocInfo(result, documentationProvider);
  }

  @Nullable
  private static String doGenerateInfo(@NotNull PsiElement element) {
    if (element instanceof PsiFile) {
      final VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile != null) {
        return virtualFile.getPresentableUrl();
      }
    }

    String info = getQuickNavigateInfo(element);
    if (info != null) {
      return info;
    }

    if (element instanceof NavigationItem) {
      final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
      if (presentation != null) {
        return presentation.getPresentableText();
      }
    }

    return null;
  }

  @Nullable
  private static String getQuickNavigateInfo(PsiElement element) {
    final String name = ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE);
    if (StringUtil.isEmpty(name)) return null;
    final String typeName = ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE);
    final PsiFile file = element.getContainingFile();
    final StringBuilder sb = new StringBuilder();
    if (StringUtil.isNotEmpty(typeName)) sb.append(typeName).append(" ");
    sb.append("\"").append(name).append("\"");
    if (file != null && file.isPhysical()) {
      sb.append(" [").append(file.getName()).append("]");
    }
    return sb.toString();
  }

  public abstract static class Info {
    @NotNull final PsiElement myElementAtPointer;
    @NotNull private final List<TextRange> myRanges;

    public Info(@NotNull PsiElement elementAtPointer, @NotNull List<TextRange> ranges) {
      myElementAtPointer = elementAtPointer;
      myRanges = ranges;
    }

    public Info(@NotNull PsiElement elementAtPointer) {
      this(elementAtPointer, getReferenceRanges(elementAtPointer));
    }

    @NotNull
    private static List<TextRange> getReferenceRanges(@NotNull PsiElement elementAtPointer) {
      if (!elementAtPointer.isPhysical()) return Collections.emptyList();
      int textOffset = elementAtPointer.getTextOffset();
      final TextRange range = elementAtPointer.getTextRange();
      if (range == null) {
        throw new AssertionError("Null range for " + elementAtPointer + " of " + elementAtPointer.getClass());
      }
      if (textOffset < range.getStartOffset() || textOffset < 0) {
        LOG.error("Invalid text offset " + textOffset + " of element " + elementAtPointer + " of " + elementAtPointer.getClass());
        textOffset = range.getStartOffset();
      }
      return Collections.singletonList(new TextRange(textOffset, range.getEndOffset()));
    }

    boolean isSimilarTo(@NotNull Info that) {
      return Comparing.equal(myElementAtPointer, that.myElementAtPointer) && myRanges.equals(that.myRanges);
    }

    @NotNull
    public List<TextRange> getRanges() {
      return myRanges;
    }

    @NotNull
    public abstract DocInfo getInfo();

    public abstract boolean isValid(@NotNull Document document);

    public abstract boolean isNavigatable();

    boolean rangesAreCorrect(@NotNull Document document) {
      final TextRange docRange = new TextRange(0, document.getTextLength());
      for (TextRange range : getRanges()) {
        if (!docRange.contains(range)) return false;
      }

      return true;
    }
  }

  private static void showDumbModeNotification(@NotNull Project project) {
    DumbService.getInstance(project).showDumbModeNotification("Element information is not available during index update");
  }

  private static class InfoSingle extends Info {
    @NotNull private final PsiElement myTargetElement;

    InfoSingle(@NotNull PsiElement elementAtPointer, @NotNull PsiElement targetElement) {
      super(elementAtPointer);
      myTargetElement = targetElement;
    }

    InfoSingle(@NotNull PsiReference ref, @NotNull final PsiElement targetElement) {
      super(ref.getElement(), ReferenceRange.getAbsoluteRanges(ref));
      myTargetElement = targetElement;
    }

    @Override
    @NotNull
    public DocInfo getInfo() {
      return areElementsValid() ? generateInfo(myTargetElement, myElementAtPointer, isNavigatable()) : DocInfo.EMPTY;
    }

    private boolean areElementsValid() {
      return myTargetElement.isValid() && myElementAtPointer.isValid();
    }

    @Override
    public boolean isValid(@NotNull Document document) {
      return areElementsValid() && rangesAreCorrect(document);
    }

    @Override
    public boolean isNavigatable() {
      return myTargetElement != myElementAtPointer && myTargetElement != myElementAtPointer.getParent();
    }
  }

  private static class InfoMultiple extends Info {
    InfoMultiple(@NotNull final PsiElement elementAtPointer) {
      super(elementAtPointer);
    }

    InfoMultiple(@NotNull final PsiElement elementAtPointer, @NotNull PsiReference ref) {
      super(elementAtPointer, ReferenceRange.getAbsoluteRanges(ref));
    }

    @Override
    @NotNull
    public DocInfo getInfo() {
      return new DocInfo(CodeInsightBundle.message("multiple.implementations.tooltip"), null);
    }

    @Override
    public boolean isValid(@NotNull Document document) {
      return rangesAreCorrect(document);
    }

    @Override
    public boolean isNavigatable() {
      return true;
    }
  }

  @Nullable
  private Info getInfoAt(@NotNull final Editor editor, @NotNull PsiFile file, int offset, @NotNull BrowseMode browseMode) {
    return getInfoAt(myProject, editor, file, offset, browseMode);
  }

  @Nullable
  public static Info getInfoAt(@NotNull Project project, @NotNull final Editor editor, @NotNull PsiFile file, int offset,
                               @NotNull BrowseMode browseMode) {
    PsiElement targetElement = null;

    if (browseMode == BrowseMode.TypeDeclaration) {
      try {
        targetElement = GotoTypeDeclarationAction.findSymbolType(editor, offset);
      }
      catch (IndexNotReadyException e) {
        showDumbModeNotification(project);
      }
    }
    else if (browseMode == BrowseMode.Declaration) {
      final PsiReference ref = TargetElementUtil.findReference(editor, offset);
      final List<PsiElement> resolvedElements = ref == null ? Collections.emptyList() : resolve(ref);
      final PsiElement resolvedElement = resolvedElements.size() == 1 ? resolvedElements.get(0) : null;

      final PsiElement[] targetElements = GotoDeclarationAction.findTargetElementsNoVS(project, editor, offset, false);
      final PsiElement elementAtPointer = file.findElementAt(TargetElementUtil.adjustOffset(file, editor.getDocument(), offset));

      if (targetElements != null) {
        if (targetElements.length == 0) {
          return null;
        }
        else if (targetElements.length == 1) {
          if (targetElements[0] != resolvedElement && elementAtPointer != null && targetElements[0].isPhysical()) {
            return ref != null ? new InfoSingle(ref, targetElements[0]) : new InfoSingle(elementAtPointer, targetElements[0]);
          }
        }
        else {
          return elementAtPointer != null ? new InfoMultiple(elementAtPointer) : null;
        }
      }

      if (resolvedElements.size() == 1) {
        return new InfoSingle(ref, resolvedElements.get(0));
      }
      if (resolvedElements.size() > 1) {
        return elementAtPointer != null ? new InfoMultiple(elementAtPointer, ref) : null;
      }
    }
    else if (browseMode == BrowseMode.Implementation) {
      final PsiElement element = TargetElementUtil.getInstance().findTargetElement(editor, ImplementationSearcher.getFlags(), offset);
      PsiElement[] targetElements = new ImplementationSearcher() {
        @Override
        @NotNull
        protected PsiElement[] searchDefinitions(final PsiElement element, Editor editor) {
          final List<PsiElement> found = new ArrayList<>(2);
          DefinitionsScopedSearch.search(element, getSearchScope(element, editor)).forEach(psiElement -> {
            found.add(psiElement);
            return found.size() != 2;
          });
          return PsiUtilCore.toPsiElementArray(found);
        }
      }.searchImplementations(editor, element, offset);
      if (targetElements == null) {
        return null;
      }
      if (targetElements.length > 1) {
        PsiElement elementAtPointer = file.findElementAt(offset);
        if (elementAtPointer != null) {
          return new InfoMultiple(elementAtPointer);
        }
        return null;
      }
      if (targetElements.length == 1) {
        Navigatable descriptor = EditSourceUtil.getDescriptor(targetElements[0]);
        if (descriptor == null || !descriptor.canNavigate()) {
          return null;
        }
        targetElement = targetElements[0];
      }
    }

    if (targetElement != null && targetElement.isPhysical()) {
      PsiElement elementAtPointer = file.findElementAt(offset);
      if (elementAtPointer != null) {
        return new InfoSingle(elementAtPointer, targetElement);
      }
    }

    final PsiElement element = GotoDeclarationAction.findElementToShowUsagesOf(editor, offset);
    if (element instanceof PsiNameIdentifierOwner) {
      PsiElement identifier = ((PsiNameIdentifierOwner)element).getNameIdentifier();
      if (identifier != null && identifier.isValid()) {
        return new Info(identifier){
          @NotNull
          @Override
          public DocInfo getInfo() {
            String name = UsageViewUtil.getType(element) + " '"+ UsageViewUtil.getShortName(element)+"'";
            return new DocInfo("Show usages of "+name, null);
          }

          @Override
          public boolean isValid(@NotNull Document document) {
            return element.isValid();
          }

          @Override
          public boolean isNavigatable() {
            return true;
          }
        };
      }
    }
    return null;
  }

  @NotNull
  private static List<PsiElement> resolve(@NotNull PsiReference ref) {
    // IDEA-56727 try resolve first as in GotoDeclarationAction
    PsiElement resolvedElement = ref.resolve();

    if (resolvedElement == null && ref instanceof PsiPolyVariantReference) {
      List<PsiElement> result = new ArrayList<>();
      final ResolveResult[] psiElements = ((PsiPolyVariantReference)ref).multiResolve(false);
      for (ResolveResult resolveResult : psiElements) {
        if (resolveResult.getElement() != null) {
          result.add(resolveResult.getElement());
        }
      }
      return result;
    }
    return resolvedElement == null ? Collections.emptyList() : Collections.singletonList(resolvedElement);
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


  private class TooltipProvider {
    @NotNull private final EditorEx myHostEditor;
    private final int myHostOffset;
    private final boolean myInVirtualSpace;
    private BrowseMode myBrowseMode;
    private boolean myDisposed;
    private final ProgressIndicator myProgress = new ProgressIndicatorBase();
    private CompletableFuture myExecutionProgress;

    TooltipProvider(@NotNull EditorEx hostEditor, @NotNull LogicalPosition hostPos) {
      myHostEditor = hostEditor;
      myHostOffset = hostEditor.logicalPositionToOffset(hostPos);
      myInVirtualSpace = EditorUtil.inVirtualSpace(hostEditor, hostPos);
    }

    @SuppressWarnings("CopyConstructorMissesField")
    TooltipProvider(@NotNull TooltipProvider source) {
      myHostEditor = source.myHostEditor;
      myHostOffset = source.myHostOffset;
      myInVirtualSpace = source.myInVirtualSpace;
    }

    void dispose() {
      myDisposed = true;
      myProgress.cancel();
    }

    BrowseMode getBrowseMode() {
      return myBrowseMode;
    }

    void execute(@NotNull BrowseMode browseMode) {
      myBrowseMode = browseMode;

      if (PsiDocumentManager.getInstance(myProject).getPsiFile(myHostEditor.getDocument()) == null) return;

      if (myInVirtualSpace) {
        disposeHighlighter();
        return;
      }

      int selStart = myHostEditor.getSelectionModel().getSelectionStart();
      int selEnd = myHostEditor.getSelectionModel().getSelectionEnd();

      if (myHostOffset >= selStart && myHostOffset < selEnd) {
        disposeHighlighter();
        return;
      }

      PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(
        () -> myExecutionProgress = ProgressIndicatorUtils.scheduleWithWriteActionPriority(myProgress, new ReadTask() {
          @Nullable
          @Override
          public Continuation performInReadAction(@NotNull ProgressIndicator indicator) throws ProcessCanceledException {
            return doExecute();
          }

          @Override
          public void onCanceled(@NotNull ProgressIndicator indicator) {
            LOG.debug("Highlighting was cancelled");
          }
        }));
    }

    private ReadTask.Continuation createDisposalContinuation() {
      return new ReadTask.Continuation(() -> {
        if (!isTaskOutdated(myHostEditor)) disposeHighlighter();
      });
    }

    @Nullable
    private ReadTask.Continuation doExecute() {
      if (isTaskOutdated(myHostEditor)) return null;

      EditorEx editor = getPossiblyInjectedEditor();
      int offset = getOffset(editor);

      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (file == null) return createDisposalContinuation();

      final Info info;
      final DocInfo docInfo;
      try {
        info = getInfoAt(editor, file, offset, myBrowseMode);
        if (info == null) return createDisposalContinuation();
        docInfo = info.getInfo();
      }
      catch (IndexNotReadyException e) {
        showDumbModeNotification(myProject);
        return createDisposalContinuation();
      }

      LOG.debug("Obtained info about element under cursor");
      return new ReadTask.Continuation(() -> {
        if (isTaskOutdated(editor)) return;
        addHighlighterAndShowHint(info, docInfo, editor);
      });
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
             !ApplicationManager.getApplication().isUnitTestMode() && !editor.getComponent().isShowing();
    }

    private int getOffset(@NotNull Editor editor) {
      return editor instanceof EditorWindow ? ((EditorWindow)editor).getDocument().hostToInjected(myHostOffset) : myHostOffset;
    }

    private void addHighlighterAndShowHint(@NotNull Info info, @NotNull DocInfo docInfo, @NotNull EditorEx editor) {
      if (myDisposed || editor.isDisposed()) return;
      if (myHighlighter != null) {
        if (!info.isSimilarTo(myHighlighter.getStoredInfo())) {
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

      if (!info.isValid(editor.getDocument()) || !info.isNavigatable() && docInfo.text == null) {
        return;
      }

      boolean highlighterOnly = EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement() &&
                                DocumentationManager.getInstance(myProject).getDocInfoHint() != null;

      myHighlighter = installHighlighterSet(info, editor, highlighterOnly);

      if (highlighterOnly || docInfo.text == null) return;

      HyperlinkListener hyperlinkListener = docInfo.docProvider == null
                                   ? null
                                   : new QuickDocHyperlinkListener(docInfo.docProvider, info.myElementAtPointer);
      Ref<Consumer<String>> newTextConsumerRef = new Ref<>();
      JComponent component = HintUtil.createInformationLabel(docInfo.text, hyperlinkListener, null, newTextConsumerRef);
      component.setBorder(JBUI.Borders.empty(6, 6, 5, 6));

      final LightweightHint hint = new LightweightHint(wrapInScrollPaneIfNeeded(component, editor));

      myHint = hint;
      hint.addHintListener(__ -> myHint = null);

      showHint(hint, editor);

      Consumer<String> newTextConsumer = newTextConsumerRef.get();
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
                                    @NotNull Info info,
                                    @NotNull Consumer<? super String> textConsumer,
                                    @NotNull String oldText,
                                    @NotNull Editor editor) {
      if (!hint.isVisible()) return;
      Disposable hintDisposable = Disposer.newDisposable("CtrlMouseHandler.TooltipProvider.updateOnPsiChanges");
      hint.addHintListener(__ -> Disposer.dispose(hintDisposable));
      AtomicBoolean updating = new AtomicBoolean(false);
      myProject.getMessageBus().connect(hintDisposable).subscribe(PsiModificationTracker.TOPIC, () -> {
        if (updating.getAndSet(true)) return;
        PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(() -> {
          ProgressIndicatorBase progress = new ProgressIndicatorBase();
          if (Disposer.isDisposed(hintDisposable)) {
            progress.cancel();
          }
          else {
            Disposer.register(hintDisposable, () -> progress.cancel());
          }
          ProgressIndicatorUtils.scheduleWithWriteActionPriority(progress, new ReadTask() {
            @Nullable
            @Override
            public Continuation performInReadAction(@NotNull ProgressIndicator indicator) throws ProcessCanceledException {
              if (!info.isValid(editor.getDocument())) {
                updating.set(false);
                return null;
              }
              try {
                DocInfo newDocInfo = info.getInfo();
                return new Continuation(() -> {
                  updating.set(false);
                  if (newDocInfo.text != null && !oldText.equals(newDocInfo.text)) {
                    updateText(newDocInfo.text, textConsumer, hint, editor);
                  }
                });
              }
              catch (IndexNotReadyException e) {
                showDumbModeNotification(myProject);
                return createDisposalContinuation();
              }
            }

            @Override
            public void onCanceled(@NotNull ProgressIndicator indicator) {
              updating.set(false);
            }
          });
        });
      });
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
  private HighlightersSet installHighlighterSet(@NotNull Info info, @NotNull EditorEx editor, boolean highlighterOnly) {
    editor.getContentComponent().addKeyListener(myEditorKeyListener);
    editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
    if (info.isNavigatable()) {
      editor.setCustomCursor(CtrlMouseHandler.class, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    List<RangeHighlighter> highlighters = new ArrayList<>();

    if (!highlighterOnly || info.isNavigatable()) {
      TextAttributes attributes = info.isNavigatable()
                                  ? myEditorColorsManager.getGlobalScheme().getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR)
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
    CompletableFuture progress = provider.myExecutionProgress;
    if (progress == null) return false;
    return !progress.isDone();
  }

  private class HighlightersSet {
    @NotNull private final List<? extends RangeHighlighter> myHighlighters;
    @NotNull private final EditorEx myHighlighterView;
    @NotNull private final Info myStoredInfo;

    private HighlightersSet(@NotNull List<? extends RangeHighlighter> highlighters,
                            @NotNull EditorEx highlighterView,
                            @NotNull Info storedInfo) {
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
    Info getStoredInfo() {
      return myStoredInfo;
    }
  }

  public static class DocInfo {
    public static final DocInfo EMPTY = new DocInfo(null, null);

    @Nullable public final String text;
    @Nullable final DocumentationProvider docProvider;

    DocInfo(@Nullable String text, @Nullable DocumentationProvider provider) {
      this.text = text;
      docProvider = provider;
    }
  }

  private class QuickDocHyperlinkListener implements HyperlinkListener {
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
          myDocumentationManager.showJavaDocInfo(targetElement, myContext, null);
        }
      });
    }
  }
}
