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
package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.codeInsight.daemon.quickFix.FileReferenceQuickFixProvider;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Query;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.psi.impl.source.resolve.reference.impl.providers.FileTargetContext.toTargetContexts;

/**
 * @author peter
 */
public class PsiFileReferenceHelper extends FileReferenceHelper {

  @NotNull
  @Override
  public List<? extends LocalQuickFix> registerFixes(FileReference reference) {
    return FileReferenceQuickFixProvider.registerQuickFix(reference);
  }

  @Override
  public PsiFileSystemItem findRoot(final Project project, @NotNull final VirtualFile file) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    VirtualFile contentRootForFile = index.getSourceRootForFile(file);
    if (contentRootForFile == null) contentRootForFile = index.getContentRootForFile(file);

    if (contentRootForFile != null) {
      return PsiManager.getInstance(project).findDirectory(contentRootForFile);
    }
    return null;
  }

  @Override
  @NotNull
  public Collection<PsiFileSystemItem> getRoots(@NotNull final Module module) {
    return getContextsForModule(module, "", module.getModuleWithDependenciesScope());
  }

  @NotNull
  @Override
  public Collection<PsiFileSystemItem> getRoots(@NotNull Module module, @NotNull VirtualFile file) {
    Collection<PsiFileSystemItem> contextsForModule = getRoots(module);
    if (contextsForModule.size() <= 1) {
      return contextsForModule;
    }

    Collection<FileTargetContext> targetContexts = toTargetContexts(contextsForModule);
    Collection<FileTargetContext> contexts = sortWithResourcePriority(module.getProject(), file, targetContexts);

    return ContainerUtil.map(contexts, c -> c.getFileSystemItem());
  }

  @NotNull
  @Override
  public Collection<FileTargetContext> getTargetContexts(@NotNull Project project, @NotNull VirtualFile file) {
    PsiFileSystemItem item = getPsiFileSystemItem(project, file);
    if (item != null) {
      PsiFileSystemItem parent = item.getParent();
      if (parent != null) {
        ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile parentFile = parent.getVirtualFile();
        assert parentFile != null;

        VirtualFile root = index.getSourceRootForFile(parentFile);
        if (root != null) {
          String packagePath = VfsUtilCore.getRelativePath(parentFile, root, '.');

          if (packagePath != null) {
            Module module = index.getModuleForFile(file);

            if (module != null) {
              ModuleFileIndex moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex();
              OrderEntry orderEntry = moduleFileIndex.getOrderEntryForFile(file);

              String rootPackagePrefix = getSourceRootPackagePrefix(orderEntry, root);
              if (!rootPackagePrefix.isEmpty()) {
                packagePath += "." + rootPackagePrefix;
              }

              Collection<PsiFileSystemItem> contextsForModule = getContextsForModule(module, packagePath, module.getModuleWithDependenciesScope());

              List<SourceFolder> additionalContexts = getMissingTargetFolders(module, contextsForModule);
              if (additionalContexts.isEmpty()) {
                return sortWithResourcePriority(project, file, toTargetContexts(contextsForModule));
              }

              List<FileTargetContext> joinedContexts = new ArrayList<>(contextsForModule.size() + additionalContexts.size());
              for (PsiFileSystemItem c : contextsForModule) {
                joinedContexts.add(new FileTargetContext(c));
              }
              PsiManager manager = PsiManager.getInstance(module.getProject());

              String[] pathToCreate = getRelativePath(parentFile, root);
              for (SourceFolder sourceFolder : additionalContexts) {
                String srcPackagePrefix = sourceFolder.getPackagePrefix();
                if (!srcPackagePrefix.isEmpty()) {
                  pathToCreate = removeCommonStartPackages(pathToCreate, srcPackagePrefix);
                }

                if (sourceFolder.getFile() != null) {
                  PsiDirectory directory = manager.findDirectory(sourceFolder.getFile());
                  if (directory != null) {

                    joinedContexts.add(new FileTargetContext(directory, pathToCreate));
                  }
                }
              }

              return sortWithResourcePriority(project, file, joinedContexts);
            }
          }
        }
        return toTargetContexts(parent);
      }
    }
    return Collections.emptyList();
  }

  private static Collection<FileTargetContext> sortWithResourcePriority(@NotNull Project project, @NotNull VirtualFile file,
                                                                        @NotNull Collection<FileTargetContext> targetContexts) {
    // here we try to sort target locations depending on src/test origin
    if (targetContexts.isEmpty() || targetContexts.size() == 1) {
      return targetContexts;
    }

    List<FileTargetContextWrapper> targetContextWrappers = findSourceRootTypes(targetContexts);

    // sort only if we different source root types
    if (targetContextWrappers.stream()
        .map(FileTargetContextWrapper::getSourceRootType)
        .distinct()
        .count() < 2) {
      return targetContexts;
    }

    // if file is under sources root then src/resources directories at the top
    // if file is under test sources root then test/resources directories at the top
    ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
    if (projectFileIndex.isInTestSourceContent(file)) {
      targetContextWrappers.sort(PsiFileReferenceHelper::compareTargetsForTests);
    }
    else if (projectFileIndex.isInSourceContent(file)) {
      targetContextWrappers.sort(PsiFileReferenceHelper::compareTargetsForProduction);
    }
    return ContainerUtil.map(targetContextWrappers, FileTargetContextWrapper::getTargetContext);
  }

  private static List<FileTargetContextWrapper> findSourceRootTypes(Collection<FileTargetContext> targetContexts) {
    return ContainerUtil.map(targetContexts, c -> {
      Project project = c.getFileSystemItem().getProject();

      SourceFolder sourceFolder = null;
      VirtualFile file = c.getFileSystemItem().getVirtualFile();
      if (file != null) {
        sourceFolder = getSourceFolder(project, file);
      }

      return new FileTargetContextWrapper(c, sourceFolder != null ? sourceFolder.getRootType() : null);
    });
  }

  @Nullable
  private static SourceFolder getSourceFolder(@NotNull Project project, @NotNull VirtualFile directory) {
    ProjectFileIndexImpl projectFileIndex = (ProjectFileIndexImpl)ProjectRootManager.getInstance(project).getFileIndex();
    return projectFileIndex.getSourceFolder(directory);
  }

  private static int compareTargetsForTests(@NotNull FileTargetContextWrapper d1, @NotNull FileTargetContextWrapper d2) {
    int o1 = getTestsTargetOrdinal(d1);
    int o2 = getTestsTargetOrdinal(d2);

    if (o1 > 0 && o2 > 0) {
      return Integer.compare(o1, o2);
    }

    return compareDirectoryPaths(d1, d2);
  }

  private static int compareTargetsForProduction(@NotNull FileTargetContextWrapper d1, @NotNull FileTargetContextWrapper d2) {
    int o1 = getSourcesTargetOrdinal(d1);
    int o2 = getSourcesTargetOrdinal(d2);

    if (o1 > 0 && o2 > 0) {
      return Integer.compare(o1, o2);
    }

    return compareDirectoryPaths(d1, d2);
  }

  private static int getTestsTargetOrdinal(@NotNull FileTargetContextWrapper item) {
    JpsModuleSourceRootType type = item.getSourceRootType();

    if (isSourceItem(type)) return 4;
    if (isTestSourceItem(type)) return 3;
    if (isResourceItem(type)) return 2;
    if (isTestResourceItem(type)) return 1;

    return 0;
  }

  private static int getSourcesTargetOrdinal(@NotNull FileTargetContextWrapper item) {
    JpsModuleSourceRootType type = item.getSourceRootType();

    if (isTestSourceItem(type)) return 4;
    if (isSourceItem(type)) return 3;
    if (isTestResourceItem(type)) return 2;
    if (isResourceItem(type)) return 1;

    return 0;
  }

  private static boolean isResourceRoot(@NotNull FileTargetContextWrapper d) {
    return isResourceItem(d.getSourceRootType()) ||
           isTestResourceItem(d.getSourceRootType());
  }

  private static int compareDirectoryPaths(@NotNull FileTargetContextWrapper d1, @NotNull FileTargetContextWrapper d2) {
    PsiFileSystemItem directory1 = d1.getTargetContext().getFileSystemItem();
    PsiFileSystemItem directory2 = d2.getTargetContext().getFileSystemItem();

    assert directory1 != null : "Invalid PsiFileSystemItem instances found";
    assert directory2 != null : "Invalid PsiFileSystemItem instances found";

    VirtualFile f1 = directory1.getVirtualFile();
    VirtualFile f2 = directory2.getVirtualFile();
    return f1.getPath().compareTo(f2.getPath());
  }

  private static String[] removeCommonStartPackages(String[] path, String packagePath) {
    List<String> packages = StringUtil.split(packagePath, ".");
    List<String> result = new SmartList<>();

    int i = 0;
    while (i < packages.size() && i < path.length) {
      String part = path[i];
      String existingPackage = packages.get(i);

      if (!Objects.equals(part, existingPackage)) {
        break;
      }

      i++;
    }

    while (i < path.length) {
      result.add(path[i]);
      i++;
    }

    return ArrayUtil.toStringArray(result);
  }

  @NotNull
  private static String[] getRelativePath(VirtualFile file, VirtualFile root) {
    List<String> names = new ArrayList<>();

    VirtualFile parent = file;

    while (parent != null
           && !parent.equals(root)) {
      names.add(parent.getName());
      parent = parent.getParent();
    }

    if (names.isEmpty()) return ArrayUtil.EMPTY_STRING_ARRAY;
    String[] path = new String[names.size()];
    for (int i = 0; i < names.size(); i++) {
      path[i] = names.get(names.size() - i - 1);
    }
    return path;
  }

  @NotNull
  private static List<SourceFolder> getMissingTargetFolders(Module module, Collection<PsiFileSystemItem> contextsForModule) {
    // find additional source folders that can be used to create a file, e.g. they do not have the exact package, but it can be created
    ModuleRootModel model = ModuleRootManager.getInstance(module);

    return Arrays.stream(model.getContentEntries())
      .flatMap(contentEntry -> Arrays.stream(contentEntry.getSourceFolders()))
      .filter(sourceFolder -> {
        if (sourceFolder.getFile() == null) return false;

        for (PsiFileSystemItem contextItem : contextsForModule) {
          if (VfsUtilCore.isAncestor(sourceFolder.getFile(), contextItem.getVirtualFile(), false)) {
            return false;
          }
        }

        return true;
      })
      .collect(Collectors.toCollection(SmartList::new));
  }

  @Override
  @NotNull
  public Collection<PsiFileSystemItem> getContexts(final Project project, @NotNull final VirtualFile file) {
    final PsiFileSystemItem item = getPsiFileSystemItem(project, file);
    if (item != null) {
      final PsiFileSystemItem parent = item.getParent();
      if (parent != null) {
        final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        final VirtualFile parentFile = parent.getVirtualFile();
        assert parentFile != null;
        VirtualFile root = index.getSourceRootForFile(parentFile);
        if (root != null) {
          String path = VfsUtilCore.getRelativePath(parentFile, root, '.');

          if (path != null) {
            Module module = index.getModuleForFile(file);

            if (module != null) {
              OrderEntry orderEntry = ModuleRootManager.getInstance(module).getFileIndex().getOrderEntryForFile(file);

              String rootPackagePrefix = getSourceRootPackagePrefix(orderEntry, root);
              if (!rootPackagePrefix.isEmpty()) {
                path +=  "." + rootPackagePrefix;
              }

              return getContextsForModule(module, path, module.getModuleWithDependenciesScope());
            }
          }

          // TODO: content root
        }
        return Collections.singleton(parent);
      }
    }
    return Collections.emptyList();
  }

  private static String getSourceRootPackagePrefix(OrderEntry orderEntry, VirtualFile sourceRootOfFile) {
    if (orderEntry instanceof ModuleSourceOrderEntry) {
      for (ContentEntry e : ((ModuleSourceOrderEntry)orderEntry).getRootModel().getContentEntries()) {
        for (SourceFolder sf : e.getSourceFolders(JavaModuleSourceRootTypes.SOURCES)) {
          if (sourceRootOfFile.equals(sf.getFile())) {
            String s = sf.getPackagePrefix();
            if (!s.isEmpty()) {
              return s;
            }
          }
        }
      }
    }
    return "";
  }

  @Override
  public boolean isMine(final Project project, @NotNull final VirtualFile file) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    return index.isInSourceContent(file);
  }

  @Override
  @NotNull
  public String trimUrl(@NotNull String url) {
    return url.trim();
  }

  static Collection<PsiFileSystemItem> getContextsForModule(@NotNull Module module, @NotNull String packageName, @Nullable GlobalSearchScope scope) {
    List<PsiFileSystemItem> result = null;
    Query<VirtualFile> query = DirectoryIndex.getInstance(module.getProject()).getDirectoriesByPackageName(packageName, false);
    PsiManager manager = null;

    for(VirtualFile file:query) {
      if (scope != null && !scope.contains(file)) continue;
      if (result == null) {
        result = new ArrayList<>();
        manager = PsiManager.getInstance(module.getProject());
      }
      PsiDirectory psiDirectory = manager.findDirectory(file);
      if (psiDirectory != null) result.add(psiDirectory);
    }

    return result != null ? result:Collections.emptyList();
  }

  private static boolean isTestResourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaResourceRootType.TEST_RESOURCE;
  }

  private static boolean isResourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaResourceRootType.RESOURCE;
  }

  private static boolean isTestSourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaSourceRootType.TEST_SOURCE;
  }

  private static boolean isSourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaSourceRootType.SOURCE;
  }

  private static class FileTargetContextWrapper {
    private final FileTargetContext myTargetContext;
    private final JpsModuleSourceRootType myRootType;

    private FileTargetContextWrapper(FileTargetContext context, @Nullable JpsModuleSourceRootType type) {
      myTargetContext = context;
      myRootType = type;
    }

    private FileTargetContext getTargetContext() {
      return myTargetContext;
    }

    @Nullable
    private JpsModuleSourceRootType getSourceRootType() {
      return myRootType;
    }
  }
}
