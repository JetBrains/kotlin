/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.execution.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author gregsh
 */
public interface LanguageConsoleView extends ConsoleView, Disposable {
  @NotNull
  Project getProject();

  @NotNull
  String getTitle();

  void setTitle(String title);

  @NotNull
  PsiFile getFile();

  @NotNull
  VirtualFile getVirtualFile();

  @NotNull
  EditorEx getCurrentEditor();

  @NotNull
  EditorEx getConsoleEditor();

  @NotNull
  Document getEditorDocument();

  @NotNull
  EditorEx getHistoryViewer();

  @NotNull
  Language getLanguage();

  void setLanguage(@NotNull Language language);

  @Nullable
  String getPrompt();

  @Nullable
  ConsoleViewContentType getPromptAttributes();

  void setPrompt(@Nullable String prompt);

  void setPromptAttributes(@NotNull ConsoleViewContentType textAttributes);

  void setInputText(@NotNull String inputText);

  boolean isEditable();

  void setEditable(boolean editable);

  boolean isConsoleEditorEnabled();

  void setConsoleEditorEnabled(boolean enabled);
}
