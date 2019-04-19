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

package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.daemon.impl.quickfix.RenameFileFix;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.actions.CreateFromTemplateActionBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Maxim.Mossienko
 */
public class FileReferenceQuickFixProvider {
  private FileReferenceQuickFixProvider() {}

  @NotNull
  public static List<? extends LocalQuickFix> registerQuickFix(@NotNull FileReference reference) {
    final FileReferenceSet fileReferenceSet = reference.getFileReferenceSet();
    int index = reference.getIndex();

    if (index < 0) return Collections.emptyList();
    final String newFileName = reference.getFileNameToCreate();

    // check if we could create file
    if (newFileName.isEmpty() ||
        newFileName.indexOf('\\') != -1 ||
        newFileName.indexOf('*') != -1 ||
        newFileName.indexOf('?') != -1 ||
        SystemInfo.isWindows && newFileName.indexOf(':') != -1) {
      return Collections.emptyList();
    }

    PsiFileSystemItem context = null;
    PsiElement element = reference.getElement();
    PsiFile containingFile = element.getContainingFile();

    if(index > 0) {
      context = fileReferenceSet.getReference(index - 1).resolve();
    }
    else { // index == 0
      final Collection<PsiFileSystemItem> defaultContexts = fileReferenceSet.getDefaultContexts();
      if (defaultContexts.isEmpty()) {
        return Collections.emptyList();
      }

      Module module = containingFile == null ? null : ModuleUtilCore.findModuleForPsiElement(containingFile);

      for (PsiFileSystemItem defaultContext : defaultContexts) {
        if (defaultContext != null) {
          final VirtualFile virtualFile = defaultContext.getVirtualFile();
          if (virtualFile != null && defaultContext.isDirectory() && virtualFile.isInLocalFileSystem()) {
            if (context == null) {
              context = defaultContext;
            }
            if (module != null && module == getModuleForContext(defaultContext)) {
              // fixes IDEA-64156
              // todo: fix it on PsiFileReferenceHelper level in 10.X
              context = defaultContext;
              break;
            }
          }
        }
      }
      if (context == null && ApplicationManager.getApplication().isUnitTestMode()) {
        context = defaultContexts.iterator().next();
      }
    }
    if (context == null) return Collections.emptyList();

    final VirtualFile virtualFile = context.getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return Collections.emptyList();

    final PsiDirectory directory = context.getManager().findDirectory(virtualFile);
    if (directory == null) return Collections.emptyList();

    if (fileReferenceSet.isCaseSensitive()) {
      final PsiElement psiElement = containingFile == null ? null : reference.innerSingleResolve(false, containingFile);

      if (psiElement != null) {
        final String existingElementName = ((PsiNamedElement)psiElement).getName();

        final RenameFileReferenceIntentionAction renameRefAction = new RenameFileReferenceIntentionAction(existingElementName, reference);
        final RenameFileFix renameFileFix = new RenameFileFix(newFileName);
        return Arrays.asList(renameRefAction, renameFileFix);
      }
    }

    final boolean isDirectory;

    if (!reference.isLast()) {
      // directory
      try {
        directory.checkCreateSubdirectory(newFileName);
      } catch(IncorrectOperationException ex) {
        return Collections.emptyList();
      }
      isDirectory = true;
    } else {
      FileType ft = FileTypeManager.getInstance().getFileTypeByFileName(newFileName);
      if (ft instanceof UnknownFileType) return Collections.emptyList();

      try {
        directory.checkCreateFile(newFileName);
      } catch(IncorrectOperationException ex) {
        return Collections.emptyList();
      }

      isDirectory = false;
    }

    final CreateFileFix action = new MyCreateFileFix(isDirectory, newFileName, directory, reference);
    return Collections.singletonList(action);
  }


  @Nullable
  private static Module getModuleForContext(@NotNull PsiFileSystemItem context) {
    VirtualFile file = context.getVirtualFile();
    return file != null ? ModuleUtilCore.findModuleForFile(file, context.getProject()) : null;
  }

  private static class MyCreateFileFix extends CreateFileFix {
    private final boolean isDirectory;
    private final String myNewFileTemplateName;

    MyCreateFileFix(boolean isDirectory, String newFileName, PsiDirectory directory, FileReference reference) {
      super(isDirectory, newFileName, directory);
      this.isDirectory = isDirectory;
      myNewFileTemplateName = this.isDirectory ? null : reference.getNewFileTemplateName();
    }

    @Override
    protected String getFileText() {
      if (!isDirectory && myNewFileTemplateName != null) {
        Project project = getStartElement().getProject();
        FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
        FileTemplate template = findTemplate(fileTemplateManager);

        if (template != null) {
          try {
            return template.getText(fileTemplateManager.getDefaultProperties());
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
      return super.getFileText();
    }

    private FileTemplate findTemplate(FileTemplateManager fileTemplateManager) {
      FileTemplate template = fileTemplateManager.getTemplate(myNewFileTemplateName);
      if (template == null) template = fileTemplateManager.findInternalTemplate(myNewFileTemplateName);
      if (template == null) {
        for (FileTemplate fileTemplate : fileTemplateManager.getAllJ2eeTemplates()) {
          final String fileTemplateWithExtension = fileTemplate.getName() + '.' + fileTemplate.getExtension();
          if (fileTemplateWithExtension.equals(myNewFileTemplateName)) {
            return fileTemplate;
          }
        }
      }
      return template;
    }

    @Override
    protected void openFile(@NotNull Project project, PsiDirectory directory, PsiFile newFile, String text) {
      super.openFile(project, directory, newFile, text);
      if (!isDirectory && myNewFileTemplateName != null) {
        FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
        FileTemplate template = findTemplate(fileTemplateManager);

        if (template != null && template.isLiveTemplateEnabled()) {
          CreateFromTemplateActionBase.startLiveTemplate(newFile);
        }
      }
    }
  }
}
