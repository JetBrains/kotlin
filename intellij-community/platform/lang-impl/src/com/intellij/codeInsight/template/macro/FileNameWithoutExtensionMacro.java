/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nikolay Matveev
 */
public class FileNameWithoutExtensionMacro extends FileNameMacro {

  @Override
  public String getName() {
    return "fileNameWithoutExtension";
  }

  @Override
  public String getPresentableName() {
    return CodeInsightBundle.message("macro.file.name.without.extension");
  }

  @Override
  protected TextResult calculateResult(@NotNull VirtualFile virtualFile) {
    return new TextResult(virtualFile.getNameWithoutExtension());
  }
}
