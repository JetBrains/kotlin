// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.ui.configuration.CommonContentEntriesEditor;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.FilteredQuery;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Query;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

// TODO this should eventually replace PackageUtil from intellij.java.impl
public final class PlatformPackageUtil {

  private static final Logger LOG = Logger.getInstance(PlatformPackageUtil.class);

  @Nullable
  private static String findLongestExistingPackage(Project project, String packageName, GlobalSearchScope scope) {
    final PsiManager manager = PsiManager.getInstance(project);
    DirectoryIndex index = DirectoryIndex.getInstance(project);

    String nameToMatch = packageName;
    while (true) {
      Query<VirtualFile> vFiles = index.getDirectoriesByPackageName(nameToMatch, false);
      PsiDirectory directory = getWritableModuleDirectory(vFiles, scope, manager);
      if (directory != null) return index.getPackageName(directory.getVirtualFile());

      int lastDotIndex = nameToMatch.lastIndexOf('.');
      if (lastDotIndex >= 0) {
        nameToMatch = nameToMatch.substring(0, lastDotIndex);
      }
      else {
        return null;
      }
    }
  }

  @Nullable
  private static PsiDirectory getWritableModuleDirectory(@NotNull Query<? extends VirtualFile> vFiles,
                                                         GlobalSearchScope scope,
                                                         PsiManager manager) {
    for (VirtualFile vFile : vFiles) {
      if (!scope.contains(vFile)) continue;
      PsiDirectory directory = manager.findDirectory(vFile);
      if (directory != null && directory.isValid() && directory.isWritable()) {
        return directory;
      }
    }
    return null;
  }

  @Nullable
  public static PsiDirectory findOrCreateDirectoryForPackage(@NotNull final Project project,
                                                             @Nullable Module module,
                                                             GlobalSearchScope scope,
                                                             String packageName,
                                                             PsiDirectory baseDir,
                                                             boolean askUserToCreate,
                                                             ThreeState chooseFlag) throws IncorrectOperationException {
    PsiDirectory psiDirectory = null;
    if (chooseFlag == ThreeState.UNSURE && StringUtil.isNotEmpty(packageName)) {
      String rootPackage = findLongestExistingPackage(project, packageName, scope);
      if (rootPackage != null) {
        int beginIndex = rootPackage.length() + 1;
        packageName = beginIndex < packageName.length() ? packageName.substring(beginIndex) : "";
        String postfixToShow = packageName.replace('.', File.separatorChar);
        if (packageName.length() > 0) {
          postfixToShow = File.separatorChar + postfixToShow;
        }
        psiDirectory =
          DirectoryChooserUtil.selectDirectory(project, getPackageDirectories(project, rootPackage, scope), baseDir, postfixToShow);
        if (psiDirectory == null) return null;
      }
    }

    if (psiDirectory == null) {
      if (chooseFlag == ThreeState.NO && baseDir != null) {
        VirtualFile sourceRoot = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(baseDir.getVirtualFile());
        psiDirectory = sourceRoot != null ? PsiManager.getInstance(project).findDirectory(sourceRoot) : null;
      }
      else {
        if (module != null && !checkSourceRootsConfigured(module)) return null;
        final GlobalSearchScope scope_ = scope;
        List<PsiDirectory> dirs =
          ContainerUtil
            .mapNotNull(ProjectRootManager.getInstance(project).getContentSourceRoots(),
                        virtualFile -> scope_.contains(virtualFile) ? PsiManager.getInstance(project).findDirectory(virtualFile) : null);
        psiDirectory = DirectoryChooserUtil.selectDirectory(project, dirs.toArray(PsiDirectory.EMPTY_ARRAY), baseDir,
                                                            File.separatorChar + packageName.replace('.', File.separatorChar));
        if (psiDirectory == null) return null;
        final VirtualFile sourceRoot = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(psiDirectory.getVirtualFile());
        psiDirectory = sourceRoot != null ? PsiManager.getInstance(project).findDirectory(sourceRoot) : null;
      }
    }

    String restOfName = packageName;
    boolean askedToCreate = false;
    while (restOfName.length() > 0) {
      final String name = getLeftPart(restOfName);
      PsiDirectory foundExistingDirectory = psiDirectory != null ? psiDirectory.findSubdirectory(name) : null;
      if (foundExistingDirectory == null) {
        if (!askedToCreate && askUserToCreate) {
          if (!ApplicationManager.getApplication().isUnitTestMode()) {
            int toCreate = Messages.showYesNoDialog(project,
                                                    IdeBundle.message("prompt.create.non.existing.package", packageName),
                                                    IdeBundle.message("title.package.not.found"),
                                                    Messages.getQuestionIcon());
            if (toCreate != Messages.YES) {
              return null;
            }
          }
          askedToCreate = true;
        }

        final PsiDirectory psiDirectory_ = psiDirectory;
        try {
          psiDirectory = WriteAction.compute(() -> psiDirectory_ != null ? psiDirectory_.createSubdirectory(name) : null);
        }
        catch (IncorrectOperationException e) {
          throw e;
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
      else {
        psiDirectory = foundExistingDirectory;
      }
      restOfName = cutLeftPart(restOfName);
    }
    return psiDirectory;
  }

  public static GlobalSearchScope adjustScope(PsiDirectory baseDir,
                                              GlobalSearchScope scope,
                                              boolean skipSourceDirsForBaseTestDirectory,
                                              boolean skipTestDirsForBaseSourceDirectory) {
    if (baseDir != null) {
      if (TestSourcesFilter.isTestSources(baseDir.getVirtualFile(), baseDir.getProject())) {
        if (skipSourceDirsForBaseTestDirectory) {
          return scope.intersectWith(GlobalSearchScopesCore.projectTestScope(baseDir.getProject()));
        }
      }
      else {
        if (skipTestDirsForBaseSourceDirectory) {
          return scope.intersectWith(GlobalSearchScopesCore.projectProductionScope(baseDir.getProject()));
        }
      }
    }
    return scope;
  }

  private static PsiDirectory[] getPackageDirectories(Project project, String rootPackage, final GlobalSearchScope scope) {
    final PsiManager manager = PsiManager.getInstance(project);

    Query<VirtualFile> query = DirectoryIndex.getInstance(scope.getProject()).getDirectoriesByPackageName(rootPackage, true);
    query = new FilteredQuery<>(query, scope::contains);

    List<PsiDirectory> directories = ContainerUtil.mapNotNull(query.findAll(), manager::findDirectory);
    return directories.toArray(PsiDirectory.EMPTY_ARRAY);
  }

  private static boolean checkSourceRootsConfigured(final Module module) {
    VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
    if (sourceRoots.length == 0) {
      Messages.showErrorDialog(
        module.getProject(),
        ProjectBundle.message("module.source.roots.not.configured.error", module.getName()),
        ProjectBundle.message("module.source.roots.not.configured.title")
      );

      ProjectSettingsService
        .getInstance(module.getProject()).showModuleConfigurationDialog(module.getName(), CommonContentEntriesEditor.getName());

      sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
      if (sourceRoots.length == 0) {
        return false;
      }
    }
    return true;
  }

  private static String getLeftPart(String packageName) {
    int index = packageName.indexOf('.');
    return index > -1 ? packageName.substring(0, index) : packageName;
  }

  private static String cutLeftPart(String packageName) {
    int index = packageName.indexOf('.');
    return index > -1 ? packageName.substring(index + 1) : "";
  }

  @Nullable
  public static PsiDirectory getDirectory(@Nullable PsiElement element) {
    if (element == null) return null;
    // handle injection and fragment editor
    PsiFile file = FileContextUtil.getContextFile(element);
    return file == null ? null : file.getContainingDirectory();
  }
}
