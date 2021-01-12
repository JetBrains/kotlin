/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Provides default implementation for {@link EditorTextFieldProvider} service and applies available
 * {@link EditorCustomization customizations} if necessary.
 *
 * @author Denis Zhdanov
 */
public class EditorTextFieldProviderImpl implements EditorTextFieldProvider {
  @NotNull
  @Override
  public EditorTextField getEditorField(@NotNull Language language, @NotNull Project project,
                                        @NotNull final Iterable<? extends EditorCustomization> features) {
    return new MyEditorTextField(language, project, features);
  }

  private static class MyEditorTextField extends LanguageTextField {

    @NotNull private final Iterable<? extends EditorCustomization> myCustomizations;

    MyEditorTextField(@NotNull Language language, @NotNull Project project, @NotNull Iterable<? extends EditorCustomization> customizations) {
      super(language, project, "", false);
      myCustomizations = customizations;
    }

    @Override
    protected EditorEx createEditor() {
      final EditorEx ex = super.createEditor();
      ex.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      ex.setHorizontalScrollbarVisible(true);
      applyDefaultSettings(ex);
      applyCustomizations(ex);
      return ex;
    }

    private static void applyDefaultSettings(EditorEx ex) {
      EditorSettings settings = ex.getSettings();
      settings.setAdditionalColumnsCount(3);
      settings.setVirtualSpace(false);
    }

    private void applyCustomizations(@NotNull EditorEx editor) {
      for (EditorCustomization customization : myCustomizations) {
        customization.customize(editor);
      }
      updateBorder(editor);
    }
  }
}
