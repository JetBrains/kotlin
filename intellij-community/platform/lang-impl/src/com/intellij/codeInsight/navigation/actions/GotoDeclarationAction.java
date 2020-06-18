// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.navigation.*;
import com.intellij.codeInsight.navigation.action.GotoDeclarationUtil;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.actions.ShowUsagesAction;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl;
import com.intellij.internal.statistic.eventLog.EventPair;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorActivityManager;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.*;

import static com.intellij.codeInsight.navigation.actions.UiKt.notifyNowhereToGo;

public class GotoDeclarationAction extends BaseCodeInsightAction implements CodeInsightActionHandler, DumbAware, CtrlMouseAction {

  private static final Logger LOG = Logger.getInstance(GotoDeclarationAction.class);
  private static List<EventPair<?>> ourCurrentEventData; // accessed from EDT only

  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    try {
      ourCurrentEventData = ActionsCollectorImpl.actionEventData(e);
      super.actionPerformed(e);
    }
    finally {
      ourCurrentEventData = null;
    }
  }

  static void recordGTD() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    GTDUCollector.record(ourCurrentEventData, GTDUCollector.GTDUChoice.GTD);
  }

  static void recordSU() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    GTDUCollector.record(ourCurrentEventData, GTDUCollector.GTDUChoice.SU);
  }

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return Registry.is("ide.symbol.gtd") ? GotoDeclarationOrUsageHandler2.INSTANCE : this;
  }

  @Override
  protected boolean isValidForLookup() {
    return true;
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    DumbService.getInstance(project).runWithAlternativeResolveEnabled(() -> {
      try {
        int offset = editor.getCaretModel().getOffset();
        Pair<PsiElement[], PsiElement> pair = ActionUtil
          .underModalProgress(project, CodeInsightBundle.message("progress.title.resolving.reference"),
                              () -> doSelectCandidate(project, editor, offset));
        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");

        PsiElement[] elements = pair.first;
        PsiElement usage = pair.second;

        if (elements.length != 1) {
          if (elements.length == 0) {
            if (usage != null) {
              startFindUsages(editor, project, usage);
              return;
            }
          }

          chooseAmbiguousTarget(project, editor, offset, elements, file);
          return;
        }

        PsiElement element = elements[0];
        if (element == usage) {
          startFindUsages(editor, project, element);
          return;
        }

        PsiElement navElement = element.getNavigationElement();
        navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(element, navElement);
        if (navElement != null) {
          gotoTargetElement(navElement, editor, file);
        }
      }
      catch (IndexNotReadyException e) {
        DumbService.getInstance(project).showDumbModeNotification(
          CodeInsightBundle.message("message.navigation.is.not.available.here.during.index.update"));
      }
    });
  }

  @NotNull
  private static Pair<PsiElement[], PsiElement> doSelectCandidate(@NotNull Project project, @NotNull Editor editor, int offset) {
    PsiElement[] elements = findAllTargetElements(project, editor, offset);
    if (elements.length == 0) {
      PsiElement usage = suggestCandidates(TargetElementUtil.findReference(editor, offset)).isEmpty()
                         ? findElementToShowUsagesOf(editor, offset)
                         : null;
      return new Pair<>(elements, usage);
    }
    else if (elements.length == 1) {
      return new Pair<>(elements, findElementToShowUsagesOf(editor, offset));
    }
    else {
      return new Pair<>(elements, null);
    }
  }

  public static void startFindUsages(@NotNull Editor editor, @NotNull Project project, @NotNull PsiElement element) {
    startFindUsages(editor, project, element, null);
  }

  public static void startFindUsages(@NotNull Editor editor,
                                     @NotNull Project project,
                                     @NotNull PsiElement element,
                                     @Nullable RelativePoint point) {
    if (DumbService.getInstance(project).isDumb()) {
      AnAction action = ActionManager.getInstance().getAction(ShowUsagesAction.ID);
      String name = action.getTemplatePresentation().getText();
      DumbService.getInstance(project).showDumbModeNotification(ActionUtil.getUnavailableMessage(name, false));
    }
    else {
      RelativePoint popupPosition = point != null ? point : JBPopupFactory.getInstance().guessBestPopupLocation(editor);
      ShowUsagesAction.startFindUsages(element, popupPosition, editor);
    }
  }

  public static PsiElement findElementToShowUsagesOf(@NotNull Editor editor, int offset) {
    return TargetElementUtil.getInstance().findTargetElement(editor, TargetElementUtil.ELEMENT_NAME_ACCEPTED, offset);
  }

  private static void chooseAmbiguousTarget(@NotNull final Project project,
                                            final Editor editor,
                                            int offset,
                                            PsiElement[] elements,
                                            PsiFile currentFile) {
    if (!EditorActivityManager.getInstance().isVisible(editor)) return;
    PsiElementProcessor<PsiElement> navigateProcessor = element -> {
      gotoTargetElement(element, editor, currentFile);
      return true;
    };
    boolean found =
      chooseAmbiguousTarget(project, editor, offset, navigateProcessor, CodeInsightBundle.message("declaration.navigation.title"),
                            elements);

    if (!found) {
      notifyNowhereToGo(project, editor, currentFile, offset);
    }
  }

  static boolean navigateInCurrentEditor(@NotNull PsiElement element, @NotNull PsiFile currentFile, @NotNull Editor currentEditor) {
    if (element.getContainingFile() == currentFile && !currentEditor.isDisposed()) {
      int offset = element.getTextOffset();
      PsiElement leaf = currentFile.findElementAt(offset);
      // check that element is really physically inside the file
      // there are fake elements with custom navigation (e.g. opening URL in browser) that override getContainingFile for various reasons
      if (leaf != null && PsiTreeUtil.isAncestor(element, leaf, false)) {
        Project project = element.getProject();
        CommandProcessor.getInstance().executeCommand(project, () -> {
          IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
          new OpenFileDescriptor(project, currentFile.getViewProvider().getVirtualFile(), offset).navigateIn(currentEditor);
        }, "", null);
        return true;
      }
    }
    return false;
  }

  static void gotoTargetElement(@NotNull PsiElement element, @NotNull Editor currentEditor, @NotNull PsiFile currentFile) {
    if (navigateInCurrentEditor(element, currentFile, currentEditor)) return;

    Navigatable navigatable = element instanceof Navigatable ? (Navigatable)element : EditSourceUtil.getDescriptor(element);
    if (navigatable != null && navigatable.canNavigate()) {
      navigatable.navigate(true);
    }
  }

  /**
   * @deprecated use chooseAmbiguousTarget(Project, Editor, int, PsiElementProcessor, String, PsiElement[])
   */
  @Deprecated
  @SuppressWarnings("unused") // for external usages only
  public static boolean chooseAmbiguousTarget(@NotNull Editor editor,
                                              int offset,
                                              @NotNull PsiElementProcessor<? super PsiElement> processor,
                                              @NotNull String titlePattern,
                                              PsiElement @Nullable [] elements) {
    Project project = editor.getProject();
    if (project == null) {
      return false;
    }
    return chooseAmbiguousTarget(project, editor, offset, processor, titlePattern, elements);
  }

  // returns true if processor is run or is going to be run after showing popup
  public static boolean chooseAmbiguousTarget(@NotNull final Project project,
                                              @NotNull Editor editor,
                                              int offset,
                                              @NotNull PsiElementProcessor<? super PsiElement> processor,
                                              @NotNull String titlePattern,
                                              PsiElement @Nullable [] elements) {
    if (TargetElementUtil.inVirtualSpace(editor, offset)) {
      return false;
    }

    final PsiElement[] finalElements = elements;
    Pair<PsiElement[], PsiReference> pair =
      ActionUtil.underModalProgress(project, CodeInsightBundle.message("progress.title.resolving.reference"),
                                    () -> doChooseAmbiguousTarget(editor, offset, finalElements));

    elements = pair.first;
    PsiReference reference = pair.second;

    if (elements.length == 1) {
      PsiElement element = elements[0];
      LOG.assertTrue(element != null);
      processor.execute(element);
      return true;
    }
    if (elements.length > 1) {
      String title;

      if (reference == null) {
        title = titlePattern;
      }
      else {
        final TextRange range = reference.getRangeInElement();
        final String elementText = reference.getElement().getText();
        LOG.assertTrue(range.getStartOffset() >= 0 && range.getEndOffset() <= elementText.length(),
                       Arrays.toString(elements) + ";" + reference);
        final String refText = range.substring(elementText);
        title = MessageFormat.format(titlePattern, refText);
      }

      NavigationUtil.getPsiElementPopup(elements, new DefaultPsiElementCellRenderer(), title, processor).showInBestPositionFor(editor);
      return true;
    }
    return false;
  }

  @NotNull
  private static Pair<PsiElement[], PsiReference> doChooseAmbiguousTarget(@NotNull Editor editor,
                                                                          int offset,
                                                                          PsiElement @Nullable [] elements) {
    final PsiReference reference = TargetElementUtil.findReference(editor, offset);

    if (elements == null || elements.length == 0) {
      elements = reference == null ? PsiElement.EMPTY_ARRAY
                                   : PsiUtilCore.toPsiElementArray(suggestCandidates(reference));
    }
    return new Pair<>(elements, reference);
  }

  @NotNull
  private static Collection<PsiElement> suggestCandidates(@Nullable PsiReference reference) {
    if (reference == null) {
      return Collections.emptyList();
    }
    return TargetElementUtil.getInstance().getTargetCandidates(reference);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Nullable
  @TestOnly
  public static PsiElement findTargetElement(Project project, Editor editor, int offset) {
    final PsiElement[] targets = findAllTargetElements(project, editor, offset);
    return targets.length == 1 ? targets[0] : null;
  }

  @VisibleForTesting
  public static @NotNull PsiElement @NotNull [] findAllTargetElements(Project project, Editor editor, int offset) {
    if (TargetElementUtil.inVirtualSpace(editor, offset)) {
      return PsiElement.EMPTY_ARRAY;
    }

    final PsiElement[] targets = findTargetElementsNoVS(project, editor, offset, true);
    return targets != null ? targets : PsiElement.EMPTY_ARRAY;
  }

  static @NotNull PsiElement @Nullable [] findTargetElementsFromProviders(@NotNull Project project, @NotNull Editor editor, int offset) {
    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return null;

    return GotoDeclarationUtil.findTargetElementsFromProviders(editor, offset, file);
  }

  public static @NotNull PsiElement @Nullable [] findTargetElementsNoVS(Project project,
                                                                        Editor editor,
                                                                        int offset,
                                                                        boolean lookupAccepted) {
    PsiElement[] fromProviders = findTargetElementsFromProviders(project, editor, offset);
    if (fromProviders == null || fromProviders.length > 0) {
      return fromProviders;
    }

    int flags = TargetElementUtil.getInstance().getAllAccepted() & ~TargetElementUtil.ELEMENT_NAME_ACCEPTED;
    if (!lookupAccepted) {
      flags &= ~TargetElementUtil.LOOKUP_ITEM_ACCEPTED;
    }
    PsiElement element = TargetElementUtil.getInstance().findTargetElement(editor, flags, offset);
    if (element != null) {
      return new PsiElement[]{element};
    }

    // if no references found in injected fragment, try outer document
    if (editor instanceof EditorWindow) {
      EditorWindow window = (EditorWindow)editor;
      return findTargetElementsNoVS(project, window.getDelegate(), window.getDocument().injectedToHost(offset), lookupAccepted);
    }

    return null;
  }

  @Override
  public void update(@NotNull final AnActionEvent event) {
    if (event.getProject() == null ||
        event.getData(EditorGutter.KEY) != null ||
        Boolean.TRUE.equals(event.getData(CommonDataKeys.EDITOR_VIRTUAL_SPACE))) {
      event.getPresentation().setEnabled(false);
      return;
    }

    InputEvent inputEvent = event.getInputEvent();
    Editor editor = event.getData(CommonDataKeys.EDITOR);
    if (editor != null && inputEvent instanceof MouseEvent && event.getPlace().equals(ActionPlaces.MOUSE_SHORTCUT) &&
        !EditorUtil.isPointOverText(editor, new RelativePoint((MouseEvent)inputEvent).getPoint(editor.getContentComponent()))) {
      event.getPresentation().setEnabled(false);
      return;
    }

    for (GotoDeclarationHandler handler : GotoDeclarationHandler.EP_NAME.getExtensionList()) {
      String text = handler.getActionText(event.getDataContext());
      if (text != null) {
        Presentation presentation = event.getPresentation();
        presentation.setText(text);
        break;
      }
    }

    super.update(event);
  }

  @Override
  public @Nullable CtrlMouseInfo getCtrlMouseInfo(@NotNull Editor editor, @NotNull PsiFile file, int offset) {
    if (Registry.is("ide.symbol.gtd")) {
      return GotoDeclarationOrUsageHandler2.getCtrlMouseInfo(editor, file, offset);
    }
    final PsiReference ref = TargetElementUtil.findReference(editor, offset);
    final List<PsiElement> resolvedElements = ref == null ? Collections.emptyList() : resolve(ref);
    final PsiElement resolvedElement = resolvedElements.size() == 1 ? resolvedElements.get(0) : null;

    final PsiElement[] targetElements = findTargetElementsNoVS(file.getProject(), editor, offset, false);
    final PsiElement elementAtPointer = file.findElementAt(TargetElementUtil.adjustOffset(file, editor.getDocument(), offset));

    if (targetElements != null) {
      if (targetElements.length == 0) {
        return null;
      }
      else if (targetElements.length == 1) {
        if (targetElements[0] != resolvedElement && elementAtPointer != null && targetElements[0].isPhysical()) {
          return ref != null ? new SingleTargetElementInfo(ref, targetElements[0])
                             : new SingleTargetElementInfo(elementAtPointer, targetElements[0]);
        }
      }
      else {
        return elementAtPointer != null ? new MultipleTargetElementsInfo(elementAtPointer) : null;
      }
    }

    if (resolvedElements.size() == 1) {
      return new SingleTargetElementInfo(ref, resolvedElements.get(0));
    }
    if (resolvedElements.size() > 1) {
      return elementAtPointer != null ? new MultipleTargetElementsInfo(ref) : null;
    }

    final PsiElement element = findElementToShowUsagesOf(editor, offset);
    if (element instanceof PsiNameIdentifierOwner) {
      PsiElement identifier = ((PsiNameIdentifierOwner)element).getNameIdentifier();
      if (identifier != null && identifier.isValid()) {
        return new BaseCtrlMouseInfo(identifier) {
          @Override
          public @NotNull CtrlMouseDocInfo getDocInfo() {
            String name = UsageViewUtil.getType(element) + " '" + UsageViewUtil.getShortName(element) + "'";
            return new CtrlMouseDocInfo("Show usages of " + name, null, null);
          }

          @Override
          public boolean isValid() {
            return element.isValid();
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
}
