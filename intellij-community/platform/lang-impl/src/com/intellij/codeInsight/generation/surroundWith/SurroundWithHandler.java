// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.generation.surroundWith;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.SurroundWithLogger;
import com.intellij.codeInsight.template.impl.SurroundWithTemplateHandler;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageSurrounders;
import com.intellij.lang.folding.CustomFoldingSurroundDescriptor;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.util.DocumentUtil;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SurroundWithHandler implements CodeInsightActionHandler {
  private static final String CHOOSER_TITLE = CodeInsightBundle.message("surround.with.chooser.title");
  public static final TextRange CARET_IS_OK = new TextRange(0, 0);

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
    invoke(project, editor, file, null);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  public static void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, Surrounder surrounder) {
    if (!EditorModificationUtil.checkModificationAllowed(editor)) return;
    if (file instanceof PsiCompiledElement) {
      HintManager.getInstance().showErrorHint(editor, "Can't modify decompiled code");
      return;
    }

    List<AnAction> applicable = buildSurroundActions(project, editor, file, surrounder);
    if (applicable != null) {
      showPopup(editor, applicable);
    }
    else if (!ApplicationManager.getApplication().isUnitTestMode()) {
      HintManager.getInstance().showErrorHint(editor, "Couldn't find Surround With variants applicable to the current context");
    }
  }

  @Nullable
  public static List<AnAction> buildSurroundActions(final Project project, final Editor editor, PsiFile file, @Nullable Surrounder surrounder){
    SelectionModel selectionModel = editor.getSelectionModel();
    boolean hasSelection = selectionModel.hasSelection();
    if (!hasSelection) {
      selectLogicalLineContentsAtCaret(editor);
    }
    int startOffset = selectionModel.getSelectionStart();
    int endOffset = selectionModel.getSelectionEnd();

    PsiElement element1 = file.findElementAt(startOffset);
    PsiElement element2 = file.findElementAt(endOffset - 1);

    if (element1 == null || element2 == null) return null;

    TextRange textRange = new TextRange(startOffset, endOffset);
    for(SurroundWithRangeAdjuster adjuster: SurroundWithRangeAdjuster.EP_NAME.getExtensionList()) {
      textRange = adjuster.adjustSurroundWithRange(file, textRange, hasSelection);
      if (textRange == null) return null;
    }
    startOffset = textRange.getStartOffset();
    endOffset = textRange.getEndOffset();
    element1 = file.findElementAt(startOffset);

    final Language baseLanguage = file.getViewProvider().getBaseLanguage();
    assert element1 != null;
    final Language l = element1.getParent().getLanguage();

    List<SurroundDescriptor> surroundDescriptors = new ArrayList<>(LanguageSurrounders.INSTANCE.allForLanguage(l));
    if (l != baseLanguage) surroundDescriptors.addAll(LanguageSurrounders.INSTANCE.allForLanguage(baseLanguage));
    surroundDescriptors.add(CustomFoldingSurroundDescriptor.INSTANCE);

    int exclusiveCount = 0;
    List<SurroundDescriptor> exclusiveSurroundDescriptors = new ArrayList<>();
    for (SurroundDescriptor sd : surroundDescriptors) {
      if (sd.isExclusive()) {
        exclusiveCount++;
        exclusiveSurroundDescriptors.add(sd);
      }
    }

    if (exclusiveCount > 0) {
      surroundDescriptors = exclusiveSurroundDescriptors;
    }

    if (surrounder != null) {
      invokeSurrounderInTests(project, editor, file, surrounder, startOffset, endOffset, surroundDescriptors);
      return null;
    }

    Map<Surrounder, PsiElement[]> surrounders = new LinkedHashMap<>();
    for (SurroundDescriptor descriptor : surroundDescriptors) {
      final PsiElement[] elements = descriptor.getElementsToSurround(file, startOffset, endOffset);
      if (elements.length > 0) {
        for (PsiElement element : elements) {
          assert element != null : "descriptor " + descriptor + " returned null element";
          assert element.isValid() : descriptor;
        }
        for (Surrounder s: descriptor.getSurrounders()) {
          surrounders.put(s, elements);
        }
      }
    }
    return doBuildSurroundActions(project, editor, file, surrounders);
  }

  public static void selectLogicalLineContentsAtCaret(Editor editor) {
    int caretOffset = editor.getCaretModel().getOffset();
    Document document = editor.getDocument();
    CharSequence text = document.getImmutableCharSequence();
    editor.getSelectionModel().setSelection(CharArrayUtil.shiftForward(text, DocumentUtil.getLineStartOffset(caretOffset, document), " \t"),
                                            CharArrayUtil.shiftBackward(text, DocumentUtil.getLineEndOffset(caretOffset, document) - 1, " \t") + 1);
  }

  private static void invokeSurrounderInTests(Project project,
                                              Editor editor,
                                              PsiFile file,
                                              Surrounder surrounder,
                                              int startOffset,
                                              int endOffset, List<? extends SurroundDescriptor> surroundDescriptors) {
    assert ApplicationManager.getApplication().isUnitTestMode();
    for (SurroundDescriptor descriptor : surroundDescriptors) {
      final PsiElement[] elements = descriptor.getElementsToSurround(file, startOffset, endOffset);
      if (elements.length > 0) {
        for (Surrounder descriptorSurrounder : descriptor.getSurrounders()) {
          if (surrounder.getClass().equals(descriptorSurrounder.getClass())) {
            WriteCommandAction.runWriteCommandAction(project, () -> doSurround(project, editor, surrounder, elements));
            return;
          }
        }
      }
    }
  }

  private static void showPopup(Editor editor, List<AnAction> applicable) {
    DataContext context = DataManager.getInstance().getDataContext(editor.getContentComponent());
    JBPopupFactory.ActionSelectionAid mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS;
    DefaultActionGroup group = new DefaultActionGroup(applicable.toArray(AnAction.EMPTY_ARRAY));
    JBPopupFactory.getInstance().createActionGroupPopup(CHOOSER_TITLE, group, context, mnemonics, true).showInBestPositionFor(editor);
  }

  static void doSurround(final Project project, final Editor editor, final Surrounder surrounder, final PsiElement[] elements) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    int col = editor.getCaretModel().getLogicalPosition().column;
    int line = editor.getCaretModel().getLogicalPosition().line;
    if (!editor.getCaretModel().supportsMultipleCarets()) {
      LogicalPosition pos = new LogicalPosition(0, 0);
      editor.getCaretModel().moveToLogicalPosition(pos);
    }
    TextRange range = surrounder.surroundElements(project, editor, elements);
    if (range != CARET_IS_OK) {
      if (TemplateManager.getInstance(project).getActiveTemplate(editor) == null &&
          InplaceRefactoring.getActiveInplaceRenamer(editor) == null) {
        LogicalPosition pos1 = new LogicalPosition(line, col);
        editor.getCaretModel().moveToLogicalPosition(pos1);
      }
      if (range != null) {
        int offset = range.getStartOffset();
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
      }
    }
  }

  @Nullable
  private static List<AnAction> doBuildSurroundActions(Project project,
                                                     Editor editor,
                                                     PsiFile file,
                                                     Map<Surrounder, PsiElement[]> surrounders) {
    List<AnAction> applicable = new ArrayList<>();

    Set<Character> usedMnemonicsSet = new HashSet<>();

    int index = 0;
    for (Map.Entry<Surrounder, PsiElement[]> entry : surrounders.entrySet()) {
      Surrounder surrounder = entry.getKey();
      PsiElement[] elements = entry.getValue();
      if (surrounder.isApplicable(elements)) {
        char mnemonic;
        if (index < 9) {
          mnemonic = (char)('0' + index + 1);
        }
        else if (index == 9) {
          mnemonic = '0';
        }
        else {
          mnemonic = (char)('A' + index - 10);
        }
        index++;
        usedMnemonicsSet.add(Character.toUpperCase(mnemonic));
        applicable.add(new InvokeSurrounderAction(surrounder, project, editor, elements, mnemonic));
      }
    }

    List<AnAction> templateGroup = SurroundWithTemplateHandler.createActionGroup(editor, file, usedMnemonicsSet);
    if (!templateGroup.isEmpty()) {
      applicable.add(new Separator("Live templates"));
      applicable.addAll(templateGroup);
      applicable.add(Separator.getInstance());
      applicable.add(new ConfigureTemplatesAction());
    }
    return applicable.isEmpty() ? null : applicable;
  }

  private static class InvokeSurrounderAction extends AnAction {
    private final Surrounder mySurrounder;
    private final Project myProject;
    private final Editor myEditor;
    private final PsiElement[] myElements;

    InvokeSurrounderAction(Surrounder surrounder, Project project, Editor editor, PsiElement[] elements, char mnemonic) {
      super(UIUtil.MNEMONIC + String.valueOf(mnemonic) + ". " + surrounder.getTemplateDescription());
      mySurrounder = surrounder;
      myProject = project;
      myEditor = editor;
      myElements = elements;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (!FileDocumentManager.getInstance().requestWriting(myEditor.getDocument(), myProject)) {
        return;
      }

      Language language = Language.ANY;
      if (myElements != null && myElements.length != 0) {
        language = myElements[0].getLanguage();
      }
      WriteCommandAction.runWriteCommandAction(myProject, () -> doSurround(myProject, myEditor, mySurrounder, myElements));
      SurroundWithLogger.logSurrounder(mySurrounder, language, myProject);
    }
  }

  private static class ConfigureTemplatesAction extends AnAction {
    private ConfigureTemplatesAction() {
      super("Configure Live Templates...");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      ShowSettingsUtil.getInstance().showSettingsDialog(e.getData(CommonDataKeys.PROJECT), "Live Templates");
    }
  }
}
