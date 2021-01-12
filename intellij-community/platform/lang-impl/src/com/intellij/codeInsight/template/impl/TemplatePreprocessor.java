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

package com.intellij.codeInsight.template.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiFile;

/**
 * When an template is started, allows to prepare the editor before actual template expanding.
 * 
 * For example, for some XML-based languages it make sense to check whether text of template contains 
 * unescaped character and insert CDATA-element to the editor before template expanding.
 *
 * @see TemplateOptionalProcessor
 * @see com.intellij.codeInsight.template.TemplateSubstitutor 
 */
public interface TemplatePreprocessor {
  ExtensionPointName<TemplatePreprocessor> EP_NAME = ExtensionPointName.create("com.intellij.liveTemplatePreprocessor");

  void preprocessTemplate(final Editor editor, final PsiFile file, int caretOffset, final String textToInsert, final String templateText);
}
