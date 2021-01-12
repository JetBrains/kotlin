// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.textCompletion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LanguageTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public final class TextCompletionUtil {
  public static final Key<TextCompletionProvider> COMPLETING_TEXT_FIELD_KEY = Key.create("COMPLETING_TEXT_FIELD_KEY");
  public static final Key<Boolean> AUTO_POPUP_KEY = Key.create("AUTOPOPUP_TEXT_FIELD_KEY");

  public static void installProvider(@NotNull PsiFile psiFile, @NotNull TextCompletionProvider provider, boolean autoPopup) {
    psiFile.putUserData(COMPLETING_TEXT_FIELD_KEY, provider);
    psiFile.putUserData(AUTO_POPUP_KEY, autoPopup);
  }

  @Nullable
  public static TextCompletionProvider getProvider(@NotNull PsiFile file) {
    TextCompletionProvider provider = file.getUserData(COMPLETING_TEXT_FIELD_KEY);

    if (provider == null || (DumbService.isDumb(file.getProject()) && !DumbService.isDumbAware(provider))) {
      return null;
    }
    return provider;
  }

  public static void installCompletionHint(@NotNull EditorEx editor) {
    String completionShortcutText =
      KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));
    if (!StringUtil.isEmpty(completionShortcutText)) {

      final Ref<Boolean> toShowHintRef = new Ref<>(true);
      editor.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent e) {
          toShowHintRef.set(false);
        }
      });

      editor.addFocusListener(new FocusChangeListener() {
        @Override
        public void focusGained(@NotNull final Editor editor) {
          if (Boolean.TRUE.equals(editor.getUserData(AutoPopupController.AUTO_POPUP_ON_FOCUS_GAINED))) {
            AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);
            return;
          }
          if (toShowHintRef.get() && editor.getDocument().getText().isEmpty() && !hasValidationInfo(editor)) {
            ApplicationManager.getApplication().invokeLater(
              () -> HintManager.getInstance().showInformationHint(editor, LangBundle.message("hint.text.code.completion.available",
                                                                                             completionShortcutText)));
          }
        }

        private boolean hasValidationInfo(@NotNull Editor editor) {
          for (Component parent : UIUtil.uiParents(editor.getComponent(), false)) {
            if (parent instanceof JComponent) {
              ComponentValidator validator = ComponentValidator.getInstance((JComponent)parent).orElse(null);
              if (validator != null && validator.getValidationInfo() != null) {
                return true;
              }
            }
          }
          return false;
        }

        @Override
        public void focusLost(@NotNull Editor editor) {
          // Do nothing
        }
      });
    }
  }

  public static class DocumentWithCompletionCreator extends LanguageTextField.SimpleDocumentCreator {
    @NotNull private final TextCompletionProvider myProvider;
    private final boolean myAutoPopup;

    public DocumentWithCompletionCreator(@NotNull TextCompletionProvider provider, boolean autoPopup) {
      myProvider = provider;
      myAutoPopup = autoPopup;
    }

    @Override
    public void customizePsiFile(@NotNull PsiFile file) {
      installProvider(file, myProvider, myAutoPopup);
    }
  }
}
