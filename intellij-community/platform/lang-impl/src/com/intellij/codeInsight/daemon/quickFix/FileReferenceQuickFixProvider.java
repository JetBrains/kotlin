// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.daemon.impl.quickfix.RenameFileFix;
import com.intellij.codeInsight.daemon.quickFix.CreateFileWithScopeFix.TargetDirectory;
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
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author Maxim.Mossienko
 */
public class FileReferenceQuickFixProvider {
  private FileReferenceQuickFixProvider() {}

  @NotNull
  public static List<? extends LocalQuickFix> registerQuickFix(@NotNull FileReference reference) {
    final FileReferenceSet fileReferenceSet = reference.getFileReferenceSet();
    int index = reference.getIndex();

    if (index < 0) return emptyList();
    final String newFileName = reference.getFileNameToCreate();

    // check if we could create file
    if (newFileName.isEmpty() ||
        newFileName.indexOf('\\') != -1 ||
        newFileName.indexOf('*') != -1 ||
        newFileName.indexOf('?') != -1 ||
        SystemInfo.isWindows && newFileName.indexOf(':') != -1) {
      return emptyList();
    }

    PsiElement element = reference.getElement();
    PsiFile containingFile = element.getContainingFile();

    if (fileReferenceSet.isCaseSensitive()) {
      PsiElement psiElement = containingFile == null ? null : reference.innerSingleResolve(false, containingFile);

      if (psiElement != null) {
        String existingElementName = ((PsiNamedElement)psiElement).getName();

        RenameFileReferenceIntentionAction renameRefAction = new RenameFileReferenceIntentionAction(existingElementName, reference);
        RenameFileFix renameFileFix = new RenameFileFix(newFileName);
        return Arrays.asList(renameRefAction, renameFileFix);
      }
    }

    @Nullable
    Module module = containingFile == null ? null : ModuleUtilCore.findModuleForPsiElement(containingFile);

    if (reference.isLast()) {
      List<TargetDirectory> targetDirectories = getFileTargets(reference, module, newFileName);
      if (targetDirectories.isEmpty()) return emptyList();

      return singletonList(new MyCreateFileFix(newFileName, element, targetDirectories));
    }
    else {
      PsiDirectory directory = getDirectoryTarget(reference, module, newFileName);
      if (directory == null) return emptyList();

      return singletonList(new CreateDirectoryFix(newFileName, element, directory));
    }
  }

  @Nullable
  private static PsiDirectory getDirectoryTarget(@NotNull FileReference reference,
                                                 @Nullable Module module,
                                                 String newFileName) {
    PsiFileSystemItem context = null;
    FileReferenceSet fileReferenceSet = reference.getFileReferenceSet();
    int index = reference.getIndex();

    if (index > 0) {
      context = fileReferenceSet.getReference(index - 1).resolve();
    }
    else { // index == 0
      Collection<PsiFileSystemItem> defaultContexts = fileReferenceSet.getDefaultContexts();
      if (defaultContexts.isEmpty()) {
        return null;
      }

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
    if (context == null) return null;

    VirtualFile virtualFile = context.getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return null;

    PsiDirectory directory = context.getManager().findDirectory(virtualFile);
    if (directory == null) return null;

    try {
      directory.checkCreateSubdirectory(newFileName);
    }
    catch (IncorrectOperationException ex) {
      return null;
    }

    return directory;
  }

  @NotNull
  private static List<TargetDirectory> getFileTargets(@NotNull FileReference reference,
                                                      @Nullable Module module,
                                                      String newFileName) {
    List<PsiFileSystemItem> contexts = getSuitableContexts(reference, module);

    Stream<PsiDirectory> directories = contexts.stream()
      .flatMap(context -> {
        VirtualFile virtualFile = context.getVirtualFile();
        if (virtualFile == null || !virtualFile.isValid()) return Stream.empty();

        PsiDirectory directory = context.getManager().findDirectory(virtualFile);
        if (directory == null) return Stream.empty();

        FileType ft = FileTypeManager.getInstance().getFileTypeByFileName(newFileName);
        if (ft instanceof UnknownFileType) return Stream.empty();

        try {
          directory.checkCreateFile(newFileName);
        }
        catch (IncorrectOperationException ex) {
          return Stream.empty();
        }

        return Stream.of(directory);
      });

    List<TargetDirectory> targetDirectories;
    if (module != null) {
      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();

      targetDirectories = directories
        .map(d -> {
          Module targetModule = projectFileIndex.getModuleForFile(d.getVirtualFile());
          if (targetModule == null) return new TargetDirectory(d);

          return new TargetDirectory(d, getSourceFolder(targetModule, d));
        })
        .collect(Collectors.toList());
    }
    else {
      targetDirectories = directories
        .map(d -> new TargetDirectory(d, null))
        .collect(Collectors.toList());
    }
    return targetDirectories;
  }

  @Nullable
  private static SourceFolder getSourceFolder(@NotNull Module module, @NotNull PsiDirectory directory) {
    ContentEntry[] entries = ModuleRootManager.getInstance(module).getContentEntries();
    for (ContentEntry contentEntry : entries) {
      for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
        if (sourceFolder.getFile() != null
            && VfsUtilCore.isAncestor(sourceFolder.getFile(), directory.getVirtualFile(), false)) {
          return sourceFolder;
        }
      }
    }

    return null;
  }

  private static List<PsiFileSystemItem> getSuitableContexts(@NotNull FileReference reference, @Nullable Module module) {
    FileReferenceSet fileReferenceSet = reference.getFileReferenceSet();
    int index = reference.getIndex();

    if (index > 0) {
      return singletonList(fileReferenceSet.getReference(index - 1).resolve());
    }
    else { // index == 0
      Collection<PsiFileSystemItem> defaultContexts = fileReferenceSet.getDefaultContexts();
      if (defaultContexts.isEmpty()) {
        return emptyList();
      }

      SmartList<PsiFileSystemItem> contexts = new SmartList<>();
      for (PsiFileSystemItem defaultContext : defaultContexts) {
        if (defaultContext != null) {
          VirtualFile virtualFile = defaultContext.getVirtualFile();
          if (virtualFile != null && defaultContext.isDirectory() && virtualFile.isInLocalFileSystem()) {
            if (module != null) {
              if (module == getModuleForContext(defaultContext)) {
                contexts.add(defaultContext);
              }
            }
            else {
              contexts.add(defaultContext);
            }
          }
        }
      }

      if (contexts.isEmpty() && ApplicationManager.getApplication().isUnitTestMode()) {
        return singletonList(defaultContexts.iterator().next());
      }
      return contexts;
    }
  }

  @Nullable
  private static Module getModuleForContext(@NotNull PsiFileSystemItem context) {
    VirtualFile file = context.getVirtualFile();
    return file != null ? ModuleUtilCore.findModuleForFile(file, context.getProject()) : null;
  }

  private static class MyCreateFileFix extends CreateFileWithScopeFix {
    private final String myNewFileTemplateName;

    private MyCreateFileFix(@NotNull String newFileTemplateName,
                            @NotNull PsiElement psiElement,
                            @NotNull List<TargetDirectory> directories) {
      super(newFileTemplateName, psiElement, directories);

      myNewFileTemplateName = newFileTemplateName;
    }

    @Override
    protected String getFileText() {
      if (myNewFileTemplateName != null) {
        Project project = getStartElement().getProject();
        FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
        FileTemplate template = findTemplate(fileTemplateManager);

        if (template != null) {
          try {
            return template.getText(fileTemplateManager.getDefaultProperties());
          }
          catch (IOException ex) {
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
      if (myNewFileTemplateName != null) {
        FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
        FileTemplate template = findTemplate(fileTemplateManager);

        if (template != null && template.isLiveTemplateEnabled()) {
          CreateFromTemplateActionBase.startLiveTemplate(newFile);
        }
      }
    }
  }
}
