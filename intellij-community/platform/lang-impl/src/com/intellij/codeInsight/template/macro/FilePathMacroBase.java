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
import com.intellij.codeInsight.template.*;
import com.intellij.ide.actions.CopyReferenceUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Nikolay Matveev
 */
public abstract class FilePathMacroBase extends Macro {

  @Override
  public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
    Project project = context.getProject();
    Editor editor = context.getEditor();
    if (editor != null) {
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file != null) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
          return calculateResult(virtualFile, project);
        }
      }
    }
    return null;
  }

  @Nullable
  protected TextResult calculateResult(@NotNull VirtualFile virtualFile, @NotNull Project project) {
    return new TextResult(virtualFile.getName());
  }

  public static class FileNameWithoutExtensionMacro extends FilePathMacroBase {

    @Override
    public String getName() {
      return "fileNameWithoutExtension";
    }

    @Override
    public String getPresentableName() {
      return CodeInsightBundle.message("macro.file.name.without.extension");
    }

    @Override
    protected TextResult calculateResult(@NotNull VirtualFile virtualFile, @NotNull Project project) {
      return new TextResult(virtualFile.getNameWithoutExtension());
    }
  }

  public static class FileNameMacro extends FilePathMacroBase {
    @Override
    public String getName() {
      return "fileName";
    }

    @Override
    public String getPresentableName() {
      return CodeInsightBundle.message("macro.file.name");
    }

    @Override
    protected TextResult calculateResult(@NotNull VirtualFile virtualFile, @NotNull Project project) {
      return new TextResult(virtualFile.getName());
    }
  }

  public static class FilePathMacro extends FilePathMacroBase {
    @Override
    public String getName() {
      return "filePath";
    }

    @Override
    public String getPresentableName() {
      return "filePath()";
    }

    @Override
    protected TextResult calculateResult(@NotNull VirtualFile virtualFile, @NotNull Project project) {
      return new TextResult(FileUtil.toSystemDependentName(virtualFile.getPath()));
    }
  }

  public static class FileRelativePathMacro extends FilePathMacroBase {
    @Override
    public String getName() {
      return "fileRelativePath";
    }

    @Override
    public String getPresentableName() {
      return "fileRelativePath()";
    }

    @Override
    protected TextResult calculateResult(@NotNull VirtualFile virtualFile, @NotNull Project project) {
      return new TextResult(FileUtil.toSystemDependentName(CopyReferenceUtil.getVirtualFileFqn(virtualFile, project)));
    }
  }
}
