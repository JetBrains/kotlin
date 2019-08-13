/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.psi.templateLanguages;

import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public interface MultipleLangCommentProvider {
  ExtensionPointName<MultipleLangCommentProvider> EP_NAME = ExtensionPointName.create("com.intellij.multiLangCommenter");

  @Nullable
  Commenter getLineCommenter(PsiFile file, Editor editor,
                             Language lineStartLanguage, Language lineEndLanguage);

  boolean canProcess(PsiFile file, FileViewProvider viewProvider);
}
