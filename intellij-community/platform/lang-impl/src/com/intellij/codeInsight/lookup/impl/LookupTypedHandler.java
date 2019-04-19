// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CodeCompletionFeatures;
import com.intellij.codeInsight.completion.CompletionPhase;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.editorActions.AutoHardWrapHandler;
import com.intellij.codeInsight.editorActions.TypedHandler;
import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.actions.ChooseItemAction;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LookupTypedHandler extends TypedActionHandlerBase {

  public LookupTypedHandler(@Nullable TypedActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  public void execute(@NotNull Editor originalEditor, char charTyped, @NotNull DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    PsiFile file = project == null ? null : PsiUtilBase.getPsiFileInEditor(originalEditor, project);

    if (file == null) {
      if (myOriginalHandler != null){
        myOriginalHandler.execute(originalEditor, charTyped, dataContext);
      }
      return;
    }

    if (!EditorModificationUtil.checkModificationAllowed(originalEditor)) {
      return;
    }

    CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
    if (oldPhase instanceof CompletionPhase.CommittingDocuments && ((CompletionPhase.CommittingDocuments)oldPhase).isRestartingCompletion()) {
      assert oldPhase.indicator != null;
      oldPhase.indicator.scheduleRestart();
    }

    Editor editor = TypedHandler.injectedEditorIfCharTypedIsSignificant(charTyped, originalEditor, file);
    if (editor != originalEditor) {
      file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    }

    if (originalEditor.isInsertMode() && beforeCharTyped(charTyped, project, originalEditor, editor, file)) {
      return;
    }

    if (myOriginalHandler != null) {
      myOriginalHandler.execute(originalEditor, charTyped, dataContext);
    }
  }

  private static boolean beforeCharTyped(final char charTyped,
                                Project project,
                                final Editor originalEditor,
                                final Editor editor,
                                PsiFile file) {
    final LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(originalEditor);
    if (lookup == null){
      return false;
    }

    if (charTyped == ' ' && ChooseItemAction.hasTemplatePrefix(lookup, TemplateSettings.SPACE_CHAR)) {
      return false;
    }

    final CharFilter.Result result = getLookupAction(charTyped, lookup);
    if (lookup.isLookupDisposed()) {
      return false;
    }

    if (result == CharFilter.Result.ADD_TO_PREFIX) {
      Document document = editor.getDocument();
      long modificationStamp = document.getModificationStamp();

      if (!lookup.performGuardedChange(() -> {
          lookup.fireBeforeAppendPrefix(charTyped);
          EditorModificationUtil.typeInStringAtCaretHonorMultipleCarets(originalEditor, String.valueOf(charTyped), true);
        })) {
        return true;
      }
      lookup.appendPrefix(charTyped);
      if (lookup.isStartCompletionWhenNothingMatches() && lookup.getItems().isEmpty()) {
        final CompletionProgressIndicator completion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
        if (completion != null) {
          completion.scheduleRestart();
        } else {
          AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);
        }
      }

      AutoHardWrapHandler.getInstance().wrapLineIfNecessary(originalEditor,
                                                            DataManager.getInstance().getDataContext(originalEditor.getContentComponent()),
                                                            modificationStamp);

      final CompletionProgressIndicator completion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
      if (completion != null) {
        completion.prefixUpdated();
      }
      return true;
    }

    if (result == CharFilter.Result.SELECT_ITEM_AND_FINISH_LOOKUP && lookup.isFocused()) {
      LookupElement item = lookup.getCurrentItem();
      if (item != null) {
        if (completeTillTypedCharOccurrence(charTyped, lookup, item)) {
          return true;
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_DOT_ETC);
        lookup.finishLookupInWritableFile(charTyped, item);
        return true;
      }
    }

    lookup.hide();
    TypedHandler.autoPopupCompletion(editor, charTyped, project, file);
    return false;
  }

  private static boolean completeTillTypedCharOccurrence(char charTyped, LookupImpl lookup, LookupElement item) {
    PrefixMatcher matcher = lookup.itemMatcher(item);
    final String oldPrefix = matcher.getPrefix() + lookup.getAdditionalPrefix();
    PrefixMatcher expanded = matcher.cloneWithPrefix(oldPrefix + charTyped);
    if (expanded.prefixMatches(item)) {
      for (String s : item.getAllLookupStrings()) {
        if (matcher.prefixMatches(s)) {
          int i = -1;
          while (true) {
            i = s.indexOf(charTyped, i + 1);
            if (i < 0)  break;
            final String newPrefix = s.substring(0, i + 1);
            if (expanded.prefixMatches(newPrefix)) {
              lookup.replacePrefix(oldPrefix, newPrefix);
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  static CharFilter.Result getLookupAction(final char charTyped, final LookupImpl lookup) {
    final CharFilter.Result filtersDecision = getFiltersDecision(charTyped, lookup);
    if (filtersDecision != null) {
      return filtersDecision;
    }
    return CharFilter.Result.HIDE_LOOKUP;
  }

  @Nullable
  private static CharFilter.Result getFiltersDecision(char charTyped, LookupImpl lookup) {
    lookup.checkValid();
    LookupElement item = lookup.getCurrentItem();
    int prefixLength = item == null ? lookup.getAdditionalPrefix().length(): lookup.itemPattern(item).length();

    for (final CharFilter extension : getFilters()) {
      final CharFilter.Result result = extension.acceptChar(charTyped, prefixLength, lookup);
      if (result != null) {
        return result;
      }
      if (lookup.isLookupDisposed()) {
        throw new AssertionError("Lookup disposed after " + extension);
      }
    }
    return null;
  }

  private static List<CharFilter> getFilters() {
    return CharFilter.EP_NAME.getExtensionList();
  }
}
