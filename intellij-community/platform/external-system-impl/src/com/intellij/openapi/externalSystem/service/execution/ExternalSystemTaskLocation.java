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
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.execution.PsiLocation;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemTaskLocation extends PsiLocation<PsiElement> {

  @NotNull private final ExternalTaskExecutionInfo myTaskInfo;

  public ExternalSystemTaskLocation(@NotNull Project project, @NotNull PsiElement psiElement, @NotNull ExternalTaskExecutionInfo taskInfo) {
    super(project, psiElement);
    myTaskInfo = taskInfo;
  }

  @NotNull
  public ExternalTaskExecutionInfo getTaskInfo() {
    return myTaskInfo;
  }

  public static ExternalSystemTaskLocation create(@NotNull Project project,
                                                  @NotNull ProjectSystemId systemId,
                                                  @Nullable String projectPath,
                                                  @NotNull ExternalTaskExecutionInfo taskInfo) {
    if (projectPath != null) {
      final VirtualFile file = VfsUtil.findFileByIoFile(new File(projectPath), false);
      if (file != null) {
        final PsiDirectory psiFile = PsiManager.getInstance(project).findDirectory(file);
        if (psiFile != null) {
          return new ExternalSystemTaskLocation(project, psiFile, taskInfo);
        }
      }
    }

    String name = systemId.getReadableName() + projectPath + StringUtil.join(taskInfo.getSettings().getTaskNames(), " ");
    // We create a dummy text file instead of re-using external system file in order to avoid clashing with other configuration producers.
    // For example gradle files are enhanced groovy scripts but we don't want to run them via regular IJ groovy script runners.
    // Gradle tooling api should be used for running gradle tasks instead. IJ execution sub-system operates on Location objects
    // which encapsulate PsiElement and groovy runners are automatically applied if that PsiElement IS-A GroovyFile.
    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(name, PlainTextFileType.INSTANCE, "");
    return new ExternalSystemTaskLocation(project, psiFile, taskInfo);
  }
}
