// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

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
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileTargetContext;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author Maxim.Mossienko
 */
public final class FileReferenceQuickFixProvider {
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


    if (reference.isLast()) {
      NewFileLocation location = getNewFileLocation(reference, newFileName, containingFile, false);
      if (location == null) return emptyList();
      return singletonList(new MyCreateFileFix(element, location, reference.getNewFileTemplateName()));
    }
    else {
      NewFileLocation location = getNewFileLocation(reference, newFileName, containingFile, true);
      if (location == null) return emptyList();
      return singletonList(new CreateDirectoryPathFix(element, location));
    }
  }

  @Nullable
  public static NewFileLocation getNewFileLocation(@NotNull FileReference reference,
                                                   String newFileName,
                                                   boolean isDirectory) {
    return getNewFileLocation(reference, newFileName, reference.getElement().getContainingFile(), isDirectory);
  }

  @Nullable
  private static NewFileLocation getNewFileLocation(@NotNull FileReference reference,
                                                    String newFileName,
                                                    PsiFile containingFile,
                                                    boolean isDirectory) {
    @Nullable
    Module module = ModuleUtilCore.findModuleForPsiElement(containingFile);

    List<TargetDirectory> targetDirectories = getTargets(reference, module, newFileName, isDirectory);
    if (targetDirectories.isEmpty()) {
      return null;
    }
    return new NewFileLocation(targetDirectories, getPathToReferencePart(reference), newFileName);
  }

  @NotNull
  private static List<TargetDirectory> getTargets(@NotNull FileReference reference,
                                                  @Nullable Module module,
                                                  String newFileName,
                                                  boolean isDirectory) {
    List<FileTargetContext> contexts = getSuitableContexts(reference, module);

    List<TargetDirectory> targetDirectories = new SmartList<>();

    for (FileTargetContext targetContext : contexts) {
      PsiFileSystemItem context = targetContext.getFileSystemItem();

      VirtualFile virtualFile = context.getVirtualFile();
      if (virtualFile == null || !virtualFile.isValid()) continue;

      if (!isDirectory) {
        FileType ft = FileTypeManager.getInstance().getFileTypeByFileName(newFileName);
        if (ft instanceof UnknownFileType) continue;
      }

      PsiDirectory directory = context.getManager().findDirectory(virtualFile);
      if (directory == null) continue;

      if (!checkFileWriteAccess(reference, directory, targetContext.getPathToCreate(), newFileName, isDirectory)) {
        continue;
      }

      if (module != null) {
        targetDirectories.add(new TargetDirectory(directory, targetContext.getPathToCreate()));
      }
      else {
        targetDirectories.add(new TargetDirectory(directory));
      }
    }
    return targetDirectories;
  }

  private static boolean checkFileWriteAccess(FileReference reference,
                                              PsiDirectory targetRoot,
                                              String[] pathToCreate,
                                              String newFileName,
                                              boolean isDirectory) {
    PsiDirectory currentDirectory = targetRoot;
    for (String part : pathToCreate) {
      PsiDirectory subDirectory = currentDirectory.findSubdirectory(part);
      if (subDirectory == null) {
        return checkCreateSubdirectory(currentDirectory, part);
      }

      currentDirectory = subDirectory;
    }

    if (reference.getIndex() > 0) {
      FileReference[] references = reference.getFileReferenceSet().getAllReferences();

      // check that we can create first unresolved directory
      for (int i = 0; i < references.length - 1; i++) {
        String part = references[i].getFileNameToCreate();

        PsiDirectory subDirectory = currentDirectory.findSubdirectory(part);
        if (subDirectory == null) {
          return checkCreateSubdirectory(currentDirectory, part);
        }

        currentDirectory = subDirectory;
      }
    }

    if (isDirectory) {
      return checkCreateSubdirectory(currentDirectory, newFileName);
    } else {
      // if all directories exist check if we can create file in the last
      return checkCreateFile(currentDirectory, newFileName);
    }
  }

  private static boolean checkCreateFile(PsiDirectory directory, String newFileName) {
    try {
      directory.checkCreateFile(newFileName);
    }
    catch (IncorrectOperationException ex) {
      return false;
    }
    return true;
  }

  private static boolean checkCreateSubdirectory(PsiDirectory directory, String part) {
    try {
      directory.checkCreateSubdirectory(part);
    }
    catch (IncorrectOperationException ex) {
      return false;
    }
    // we assume that we will be able create the rest of file tree in a new directory
    return true;
  }

  private static String @NotNull [] getPathToReferencePart(FileReference reference) {
    if (reference.getIndex() == 0) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    FileReference[] references = reference.getFileReferenceSet().getAllReferences();
    String[] path = new String[reference.getIndex()];
    for (int i = 0; i < reference.getIndex(); i++) {
      path[i] = references[i].getFileNameToCreate();
    }
    return path;
  }

  @NotNull
  private static List<FileTargetContext> getSuitableContexts(@NotNull FileReference reference, @Nullable Module module) {
    FileReferenceSet fileReferenceSet = reference.getFileReferenceSet();

    Collection<FileTargetContext> targetContexts = fileReferenceSet.getTargetContexts();

    if (targetContexts.isEmpty()) {
      return emptyList();
    }

    SmartList<FileTargetContext> contexts = new SmartList<>();
    for (FileTargetContext targetContext : targetContexts) {
      PsiFileSystemItem fsContext = targetContext.getFileSystemItem();
      if (module != null) {
        if (module == getModuleForContext(fsContext)) {
          contexts.add(targetContext);
        }
      }
      else {
        contexts.add(targetContext);
      }
    }

    if (contexts.isEmpty() && ApplicationManager.getApplication().isUnitTestMode()) {
      return singletonList(targetContexts.iterator().next());
    }
    return contexts;
  }

  @Nullable
  private static Module getModuleForContext(@NotNull PsiFileSystemItem context) {
    VirtualFile file = context.getVirtualFile();
    return file != null ? ModuleUtilCore.findModuleForFile(file, context.getProject()) : null;
  }

  private static class MyCreateFileFix extends CreateFilePathFix {
    private final String myNewFileTemplateName;

    private MyCreateFileFix(@NotNull PsiElement psiElement,
                            @NotNull NewFileLocation newFileLocation,
                            @Nullable String newFileTemplateName) {
      super(psiElement, newFileLocation);

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
