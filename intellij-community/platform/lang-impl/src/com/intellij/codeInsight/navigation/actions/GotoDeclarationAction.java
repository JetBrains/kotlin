// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.diagnostic.PluginException;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.actions.ShowUsagesAction;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.LanguageNamesValidation;
import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class GotoDeclarationAction extends BaseCodeInsightAction implements CodeInsightActionHandler, DumbAware {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.navigation.actions.GotoDeclarationAction");

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return this;
  }

  @Override
  protected boolean isValidForLookup() {
    return true;
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull Editor editor, @NotNull PsiFile file) {

    DumbService.getInstance(project).setAlternativeResolveEnabled(true);
    try {
      int offset = editor.getCaretModel().getOffset();
      PsiElement[] elements = underModalProgress(project, "Resolving Reference...", () -> findAllTargetElements(project, editor, offset));
      FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");

      if (elements.length != 1) {
        if (elements.length == 0 && suggestCandidates(TargetElementUtil.findReference(editor, offset)).isEmpty()) {
          PsiElement element = findElementToShowUsagesOf(editor, editor.getCaretModel().getOffset());

          if (element != null) {
            startFindUsages(editor, project, element);
            return;
          }

          //disable 'no declaration found' notification for keywords
          if (isKeywordUnderCaret(project, file, offset)) return;
        }
        chooseAmbiguousTarget(editor, offset, elements, file);
        return;
      }

      PsiElement element = elements[0];
      if (element == findElementToShowUsagesOf(editor, editor.getCaretModel().getOffset())) {
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
      DumbService.getInstance(project).showDumbModeNotification("Navigation is not available here during index update");
    }
    finally {
      DumbService.getInstance(project).setAlternativeResolveEnabled(false);
    }
  }

  public static void startFindUsages(@NotNull Editor editor, @NotNull Project project, @NotNull PsiElement element) {
    if (DumbService.getInstance(project).isDumb()) {
      AnAction action = ActionManager.getInstance().getAction(ShowUsagesAction.ID);
      String name = action.getTemplatePresentation().getText();
      DumbService.getInstance(project).showDumbModeNotification(ActionUtil.getUnavailableMessage(name, false));
    }
    else {
      RelativePoint popupPosition = JBPopupFactory.getInstance().guessBestPopupLocation(editor);
      new ShowUsagesAction().startFindUsages(element, popupPosition, editor, ShowUsagesAction.getUsagesPageSize());
    }
  }

  static <T> T underModalProgress(@NotNull Project project,
                                  @NotNull @Nls(capitalization = Nls.Capitalization.Title) String progressTitle,
                                  @NotNull Computable<T> computable) throws ProcessCanceledException {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      DumbService.getInstance(project).setAlternativeResolveEnabled(true);
      try {
        ThrowableComputable<T, RuntimeException> inRead = () -> ApplicationManager.getApplication().runReadAction(computable);
        return ProgressManager.getInstance().computePrioritized(inRead);
      }
      finally {
        DumbService.getInstance(project).setAlternativeResolveEnabled(false);
      }
    }, progressTitle, true, project);
  }

  public static PsiElement findElementToShowUsagesOf(@NotNull Editor editor, int offset) {
    return TargetElementUtil.getInstance().findTargetElement(editor, TargetElementUtil.ELEMENT_NAME_ACCEPTED, offset);
  }

  private static void chooseAmbiguousTarget(final Editor editor, int offset, PsiElement[] elements, PsiFile currentFile) {
    if (!editor.getComponent().isShowing()) return;
    PsiElementProcessor<PsiElement> navigateProcessor = element -> {
      gotoTargetElement(element, editor, currentFile);
      return true;
    };
    boolean found =
      chooseAmbiguousTarget(editor, offset, navigateProcessor, CodeInsightBundle.message("declaration.navigation.title"), elements);
    if (!found) {
      HintManager.getInstance().showErrorHint(editor, "Cannot find declaration to go to");
    }
  }

  private static boolean navigateInCurrentEditor(@NotNull PsiElement element, @NotNull PsiFile currentFile, @NotNull Editor currentEditor) {
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

  // returns true if processor is run or is going to be run after showing popup
  public static boolean chooseAmbiguousTarget(@NotNull Editor editor,
                                              int offset,
                                              @NotNull PsiElementProcessor<? super PsiElement> processor,
                                              @NotNull String titlePattern,
                                              @Nullable PsiElement[] elements) {
    if (TargetElementUtil.inVirtualSpace(editor, offset)) {
      return false;
    }

    final PsiReference reference = TargetElementUtil.findReference(editor, offset);

    if (elements == null || elements.length == 0) {
      elements = reference == null ? PsiElement.EMPTY_ARRAY
                                   : PsiUtilCore.toPsiElementArray(
                                     underModalProgress(reference.getElement().getProject(), "Resolving Reference...",
                                                        () -> suggestCandidates(reference)));
    }

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
        LOG.assertTrue(range.getStartOffset() >= 0 && range.getEndOffset() <= elementText.length(), Arrays.toString(elements) + ";" + reference);
        final String refText = range.substring(elementText);
        title = MessageFormat.format(titlePattern, refText);
      }

      NavigationUtil.getPsiElementPopup(elements, new DefaultPsiElementCellRenderer(), title, processor).showInBestPositionFor(editor);
      return true;
    }
    return false;
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

  @NotNull
  @VisibleForTesting
  public static PsiElement[] findAllTargetElements(Project project, Editor editor, int offset) {
    if (TargetElementUtil.inVirtualSpace(editor, offset)) {
      return PsiElement.EMPTY_ARRAY;
    }

    final PsiElement[] targets = findTargetElementsNoVS(project, editor, offset, true);
    return targets != null ? targets : PsiElement.EMPTY_ARRAY;
  }

  @Nullable
  static PsiElement[] findTargetElementsFromProviders(@NotNull Project project, @NotNull Editor editor, int offset) {
    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return null;

    PsiElement elementAt = file.findElementAt(TargetElementUtil.adjustOffset(file, document, offset));
    for (GotoDeclarationHandler handler : GotoDeclarationHandler.EP_NAME.getExtensionList()) {
      PsiElement[] result = handler.getGotoDeclarationTargets(elementAt, offset, editor);
      if (result != null && result.length > 0) {
        return assertNotNullElements(result, handler.getClass()) ? result : null;
      }
    }

    return PsiElement.EMPTY_ARRAY;
  }

  @Nullable
  public static PsiElement[] findTargetElementsNoVS(Project project, Editor editor, int offset, boolean lookupAccepted) {
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
    if (editor != null && inputEvent instanceof MouseEvent &&
        editor.getInlayModel().getElementAt(new RelativePoint((MouseEvent)inputEvent).getPoint(editor.getContentComponent())) != null) {
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

  static boolean isKeywordUnderCaret(@NotNull Project project, @NotNull PsiFile file, int offset) {
    final PsiElement elementAtCaret = file.findElementAt(offset);
    if (elementAtCaret == null) return false;
    final NamesValidator namesValidator = LanguageNamesValidation.INSTANCE.forLanguage(elementAtCaret.getLanguage());
    return namesValidator != null && namesValidator.isKeyword(elementAtCaret.getText(), project);
  }

  private static boolean assertNotNullElements(@NotNull PsiElement[] result, Class<?> clazz) {
    for (PsiElement element : result) {
      if (element == null) {
        PluginException.logPluginError(LOG,
          "Null target element is returned by 'getGotoDeclarationTargets' in " + clazz.getName(), null, clazz
        );
        return false;
      }
    }
    return true;
  }
}
