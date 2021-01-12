/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.textCompletion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextFieldWithCompletion extends LanguageTextField {
  private final boolean myForceAutoPopup;
  private final boolean myShowHint;

  public TextFieldWithCompletion(@NotNull Project project,
                                 @NotNull TextCompletionProvider provider,
                                 @NotNull String value,
                                 boolean oneLineMode,
                                 boolean forceAutoPopup,
                                 boolean showHint) {
    this(project, provider, value, oneLineMode, true, forceAutoPopup, showHint);
  }

  public TextFieldWithCompletion(@Nullable Project project,
                                 @NotNull TextCompletionProvider provider,
                                 @NotNull String value,
                                 boolean oneLineMode,
                                 boolean autoPopup,
                                 boolean forceAutoPopup,
                                 boolean showHint) {
    super(PlainTextLanguage.INSTANCE, project, value, new TextCompletionUtil.DocumentWithCompletionCreator(provider, autoPopup),
          oneLineMode);
    myForceAutoPopup = forceAutoPopup;
    myShowHint = showHint;
  }

  @Override
  protected EditorEx createEditor() {
    EditorEx editor = super.createEditor();
    EditorCustomization disableSpellChecking = SpellCheckingEditorCustomizationProvider.getInstance().getDisabledCustomization();
    if (disableSpellChecking != null) disableSpellChecking.customize(editor);
    editor.putUserData(AutoPopupController.ALWAYS_AUTO_POPUP, myForceAutoPopup);

    if (myShowHint) {
      TextCompletionUtil.installCompletionHint(editor);
    }

    return editor;
  }
}
