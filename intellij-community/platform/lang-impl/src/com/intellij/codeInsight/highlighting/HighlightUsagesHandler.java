// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.daemon.impl.IdentifierUtil;
import com.intellij.codeInsight.daemon.impl.VisibleHighlightingPassFactory;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.EditorSearchSession;
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.pom.PsiDeclaredTarget;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageTargetUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HighlightUsagesHandler extends HighlightHandlerBase {
  public static void invoke(@NotNull final Project project, @NotNull final Editor editor, @Nullable PsiFile file) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (file == null && !selectionModel.hasSelection()) {
      selectionModel.selectWordAtCaret(false);
    }
    if (file == null || selectionModel.hasSelection()) {
      doRangeHighlighting(editor, project);
      return;
    }

    final HighlightUsagesHandlerBase handler = createCustomHandler(editor, file);
    if (handler != null) {
      final String featureId = handler.getFeatureId();

      if (featureId != null) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed(featureId);
      }

      handler.highlightUsages();
      return;
    }

    DumbService.getInstance(project).withAlternativeResolveEnabled(() -> {
      UsageTarget[] usageTargets = getUsageTargets(editor, file);
      if (usageTargets == null) {
        handleNoUsageTargets(file, editor, selectionModel, project);
        return;
      }

      boolean clearHighlights = isClearHighlights(editor);
      for (UsageTarget target : usageTargets) {
        target.highlightUsages(file, editor, clearHighlights);
      }
    });
  }

  @Nullable
  private static UsageTarget[] getUsageTargets(@NotNull Editor editor, @NotNull PsiFile file) {
    UsageTarget[] usageTargets = UsageTargetUtil.findUsageTargets(editor, file);

    if (usageTargets == null) {
      PsiElement targetElement = getTargetElement(editor, file);
      if (targetElement != null && targetElement != file) {
        if (!(targetElement instanceof NavigationItem)) {
          targetElement = targetElement.getNavigationElement();
        }
        if (targetElement instanceof NavigationItem) {
          usageTargets = new UsageTarget[]{new PsiElement2UsageTargetAdapter(targetElement)};
        }
      }
    }

    if (usageTargets == null) {
      PsiReference ref = TargetElementUtil.findReference(editor);

      if (ref instanceof PsiPolyVariantReference) {
        ResolveResult[] results = ((PsiPolyVariantReference)ref).multiResolve(false);

        if (results.length > 0) {
          usageTargets = ContainerUtil.mapNotNull(results, result -> {
            PsiElement element = result.getElement();
            return element == null ? null : new PsiElement2UsageTargetAdapter(element);
          }, UsageTarget.EMPTY_ARRAY);
        }
      }
    }
    return usageTargets;
  }

  private static void handleNoUsageTargets(@NotNull PsiFile file,
                                           @NotNull Editor editor,
                                           @NotNull SelectionModel selectionModel,
                                           @NotNull Project project) {
    if (file.findElementAt(editor.getCaretModel().getOffset()) instanceof PsiWhiteSpace) return;
    selectionModel.selectWordAtCaret(false);
    String selection = selectionModel.getSelectedText();
    if (selection != null) {
      for (int i = 0; i < selection.length(); i++) {
        if (!Character.isJavaIdentifierPart(selection.charAt(i))) {
          selectionModel.removeSelection();
        }
      }
    }

    doRangeHighlighting(editor, project);
    selectionModel.removeSelection();
  }

  @Nullable
  public static <T extends PsiElement> HighlightUsagesHandlerBase<T> createCustomHandler(@NotNull Editor editor, @NotNull PsiFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return createCustomHandler(editor, file, visibleRange);
  }

  /**
   * @see HighlightUsagesHandlerFactory#createHighlightUsagesHandler(Editor, PsiFile, ProperTextRange)
   */
  @Nullable
  public static <T extends PsiElement> HighlightUsagesHandlerBase<T> createCustomHandler(@NotNull Editor editor, @NotNull PsiFile file,
                                                                                         @NotNull ProperTextRange visibleRange) {
    for (HighlightUsagesHandlerFactory factory : HighlightUsagesHandlerFactory.EP_NAME.getExtensionList()) {
      final HighlightUsagesHandlerBase handler = factory.createHighlightUsagesHandler(editor, file, visibleRange);
      if (handler != null) {
        //noinspection unchecked
        return handler;
      }
    }
    return null;
  }

  @Nullable
  private static PsiElement getTargetElement(@NotNull Editor editor, @NotNull PsiFile file) {
    PsiElement target = TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().getReferenceSearchFlags());

    if (target == null) {
      int offset = TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset());
      PsiElement element = file.findElementAt(offset);
      if (element == null) return null;
    }

    return target;
  }

  private static void doRangeHighlighting(@NotNull Editor editor, @NotNull Project project) {
    if (!editor.getSelectionModel().hasSelection()) return;

    final String text = editor.getSelectionModel().getSelectedText();
    if (text == null) return;

    if (editor instanceof EditorWindow) {
      // highlight selection in the whole editor, not injected fragment only
      editor = ((EditorWindow)editor).getDelegate();
    }

    EditorSearchSession oldSearch = EditorSearchSession.get(editor);
    if (oldSearch != null) {
      if (oldSearch.hasMatches()) {
        String oldText = oldSearch.getTextInField();
        if (!oldSearch.getFindModel().isRegularExpressions()) {
          oldText = StringUtil.escapeToRegexp(oldText);
          oldSearch.getFindModel().setRegularExpressions(true);
        }

        String newText = oldText + '|' + StringUtil.escapeToRegexp(text);
        oldSearch.setTextInField(newText);
        return;
      }
    }

    EditorSearchSession.start(editor, project).getFindModel().setRegularExpressions(false);
  }

  public static class DoHighlightRunnable implements Runnable {
    private final List<? extends PsiReference> myRefs;
    @NotNull
    private final Project myProject;
    private final PsiElement myTarget;
    private final Editor myEditor;
    private final PsiFile myFile;
    private final boolean myClearHighlights;

    public DoHighlightRunnable(@NotNull List<? extends PsiReference> refs, @NotNull Project project, @NotNull PsiElement target, @NotNull Editor editor,
                               @NotNull PsiFile file, boolean clearHighlights) {
      myRefs = refs;
      myProject = project;
      myTarget = target;
      myEditor = editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor;
      myFile = file;
      myClearHighlights = clearHighlights;
    }

    @Override
    public void run() {
      highlightReferences(myProject, myTarget, myRefs, myEditor, myFile, myClearHighlights);
      setStatusText(myProject, getElementName(myTarget), myRefs.size(), myClearHighlights);
    }
  }

  public static void highlightReferences(@NotNull Project project,
                                         @NotNull PsiElement element,
                                         @NotNull List<? extends PsiReference> refs,
                                         @NotNull Editor editor,
                                         PsiFile file,
                                         boolean clearHighlights) {

    HighlightManager highlightManager = HighlightManager.getInstance(project);
    EditorColorsManager manager = EditorColorsManager.getInstance();
    TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    TextAttributes writeAttributes = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);

    setupFindModel(project);

    ReadWriteAccessDetector detector = ReadWriteAccessDetector.findDetector(element);

    if (detector != null) {
      List<PsiReference> readRefs = new ArrayList<>();
      List<PsiReference> writeRefs = new ArrayList<>();

      for (PsiReference ref : refs) {
        if (detector.getReferenceAccess(element, ref) == ReadWriteAccessDetector.Access.Read) {
          readRefs.add(ref);
        }
        else {
          writeRefs.add(ref);
        }
      }
      doHighlightRefs(highlightManager, editor, readRefs, attributes, clearHighlights);
      doHighlightRefs(highlightManager, editor, writeRefs, writeAttributes, clearHighlights);
    }
    else {
      doHighlightRefs(highlightManager, editor, refs, attributes, clearHighlights);
    }

    TextRange range = getNameIdentifierRange(file, element);
    if (range != null) {
      TextAttributes nameAttributes = attributes;
      if (detector != null && detector.isDeclarationWriteAccess(element)) {
        nameAttributes = writeAttributes;
      }
      highlightRanges(highlightManager, editor, nameAttributes, clearHighlights, Collections.singletonList(range));
    }
  }

  @Nullable
  public static TextRange getNameIdentifierRange(@NotNull PsiFile file, @NotNull PsiElement element) {
    final InjectedLanguageManager injectedManager = InjectedLanguageManager.getInstance(element.getProject());
    if (element instanceof PomTargetPsiElement) {
      final PomTarget target = ((PomTargetPsiElement)element).getTarget();
      if (target instanceof PsiDeclaredTarget) {
        final PsiDeclaredTarget declaredTarget = (PsiDeclaredTarget)target;
        final TextRange range = declaredTarget.getNameIdentifierRange();
        if (range != null) {
          if (range.getStartOffset() < 0 || range.getLength() <= 0) {
            return null;
          }
          final PsiElement navElement = declaredTarget.getNavigationElement();
          if (PsiUtilBase.isUnderPsiRoot(file, navElement)) {
            return injectedManager.injectedToHost(navElement, range.shiftRight(navElement.getTextRange().getStartOffset()));
          }
        }
      }
    }

    if (!PsiUtilBase.isUnderPsiRoot(file, element)) {
      return null;
    }

    PsiElement identifier = IdentifierUtil.getNameIdentifier(element);
    if (identifier != null && PsiUtilBase.isUnderPsiRoot(file, identifier)) {
      TextRange range = identifier instanceof ExternallyAnnotated
                        ? ((ExternallyAnnotated)identifier).getAnnotationRegion() // the way to skip the id highlighting
                        : identifier.getTextRange();
      return range == null ? null : injectedManager.injectedToHost(identifier, range);
    }
    return null;
  }

  public static void doHighlightElements(@NotNull Editor editor, @NotNull PsiElement[] elements, @NotNull TextAttributes attributes, boolean clearHighlights) {
    HighlightManager highlightManager = HighlightManager.getInstance(editor.getProject());
    List<TextRange> textRanges = new ArrayList<>(elements.length);
    for (PsiElement element : elements) {
      TextRange range = element.getTextRange();
      // injection occurs
      range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
      textRanges.add(range);
    }
    highlightRanges(highlightManager, editor, attributes, clearHighlights, textRanges);
  }

  public static void highlightRanges(@NotNull HighlightManager highlightManager, @NotNull Editor editor, @NotNull TextAttributes attributes,
                                     boolean clearHighlights,
                                     @NotNull List<? extends TextRange> textRanges) {
    if (clearHighlights) {
      clearHighlights(editor, highlightManager, textRanges, attributes);
      return;
    }
    ArrayList<RangeHighlighter> highlighters = new ArrayList<>();
    for (TextRange range : textRanges) {
      highlightManager.addRangeHighlight(editor, range.getStartOffset(), range.getEndOffset(), attributes, false, highlighters);
    }
    for (RangeHighlighter highlighter : highlighters) {
      String tooltip = getLineTextErrorStripeTooltip(editor.getDocument(), highlighter.getStartOffset(), true);
      highlighter.setErrorStripeTooltip(tooltip);
    }
  }

  public static boolean isClearHighlights(@NotNull Editor editor) {
    if (editor instanceof EditorWindow) editor = ((EditorWindow)editor).getDelegate();

    RangeHighlighter[] highlighters = ((HighlightManagerImpl)HighlightManager.getInstance(editor.getProject())).getHighlighters(editor);
    int caretOffset = editor.getCaretModel().getOffset();
    for (RangeHighlighter highlighter : highlighters) {
      if (TextRange.create(highlighter).grown(1).contains(caretOffset)) {
        return true;
      }
    }
    return false;
  }

  private static void clearHighlights(@NotNull Editor editor,
                                      @NotNull HighlightManager highlightManager,
                                      @NotNull List<? extends TextRange> rangesToHighlight,
                                      @NotNull TextAttributes attributes) {
    if (editor instanceof EditorWindow) editor = ((EditorWindow)editor).getDelegate();
    RangeHighlighter[] highlighters = ((HighlightManagerImpl)highlightManager).getHighlighters(editor);
    Arrays.sort(highlighters, Comparator.comparingInt(RangeMarker::getStartOffset));
    Collections.sort(rangesToHighlight, Comparator.comparingInt(TextRange::getStartOffset));
    int i = 0;
    int j = 0;
    while (i < highlighters.length && j < rangesToHighlight.size()) {
      RangeHighlighter highlighter = highlighters[i];
      TextRange highlighterRange = TextRange.create(highlighter);
      TextRange refRange = rangesToHighlight.get(j);
      if (refRange.equals(highlighterRange) && attributes.equals(highlighter.getTextAttributes()) &&
          highlighter.getLayer() == HighlighterLayer.SELECTION - 1) {
        highlightManager.removeSegmentHighlighter(editor, highlighter);
        i++;
      }
      else if (refRange.getStartOffset() > highlighterRange.getEndOffset()) {
        i++;
      }
      else if (refRange.getEndOffset() < highlighterRange.getStartOffset()) {
        j++;
      }
      else {
        i++;
        j++;
      }
    }
  }

  private static void doHighlightRefs(@NotNull HighlightManager highlightManager, @NotNull Editor editor, @NotNull List<? extends PsiReference> refs,
                                      @NotNull TextAttributes attributes, boolean clearHighlights) {
    List<TextRange> textRanges = new ArrayList<>(refs.size());
    for (PsiReference ref : refs) {
      collectRangesToHighlight(ref, textRanges);
    }
    highlightRanges(highlightManager, editor, attributes, clearHighlights, textRanges);
  }

  @NotNull
  public static List<TextRange> collectRangesToHighlight(@NotNull PsiReference ref, @NotNull List<TextRange> result) {
    for (TextRange relativeRange : ReferenceRange.getRanges(ref)) {
      PsiElement element = ref.getElement();
      TextRange range = safeCut(element.getTextRange(), relativeRange);
      if (range.isEmpty()) continue;
      // injection occurs
      result.add(InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range));
    }
    return result;
  }

  @NotNull
  private static TextRange safeCut(@NotNull TextRange range, @NotNull TextRange relative) {
    int start = Math.min(range.getEndOffset(), range.getStartOffset() + relative.getStartOffset());
    int end = Math.min(range.getEndOffset(), range.getStartOffset() + relative.getEndOffset());
    return new TextRange(start, end);
  }

  private static void setStatusText(@NotNull Project project, @Nullable String elementName, int refCount, boolean clearHighlights) {
    String message;
    if (clearHighlights) {
      message = "";
    }
    else if (refCount > 0) {
      message = CodeInsightBundle.message(elementName != null ?
                                          "status.bar.highlighted.usages.message" :
                                          "status.bar.highlighted.usages.no.target.message", refCount, elementName, getShortcutText());
    }
    else {
      message = CodeInsightBundle.message(elementName != null ?
                                          "status.bar.highlighted.usages.not.found.message" :
                                          "status.bar.highlighted.usages.not.found.no.target.message", elementName);
    }
    WindowManager.getInstance().getStatusBar(project).setInfo(message);
  }

  @NotNull
  private static String getElementName(@NotNull PsiElement element) {
    return ElementDescriptionUtil.getElementDescription(element, HighlightUsagesDescriptionLocation.INSTANCE);
  }

  @NotNull
  public static String getShortcutText() {
    final Shortcut[] shortcuts = ActionManager.getInstance()
      .getAction(IdeActions.ACTION_HIGHLIGHT_USAGES_IN_FILE)
      .getShortcutSet()
      .getShortcuts();
    if (shortcuts.length == 0) {
      return "<no key assigned>";
    }
    return KeymapUtil.getShortcutText(shortcuts[0]);
  }
}
