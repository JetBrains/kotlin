// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.UniqueNameBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFilePathWrapper;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DumbModeAccessType;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author yole
 */
public class UniqueVFilePathBuilderImpl extends UniqueVFilePathBuilder {
  @NotNull
  @Override
  public String getUniqueVirtualFilePath(@NotNull Project project, @NotNull VirtualFile file, @NotNull GlobalSearchScope scope) {
    return getUniqueVirtualFilePath(project, file, false, scope);
  }

  @NotNull
  @Override
  public String getUniqueVirtualFilePath(@NotNull Project project, @NotNull VirtualFile vFile) {
    return getUniqueVirtualFilePath(project, vFile, GlobalSearchScope.projectScope(project));
  }

  @NotNull
  @Override
  public String getUniqueVirtualFilePathWithinOpenedFileEditors(@NotNull Project project, @NotNull VirtualFile vFile) {
    return getUniqueVirtualFilePath(project, vFile, true, GlobalSearchScope.projectScope(project));
  }

  private static final Key<CachedValue<Map<GlobalSearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>>>
    ourShortNameBuilderCacheKey = Key.create("project's.short.file.name.builder");
  private static final Key<CachedValue<Map<GlobalSearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>>>
    ourShortNameOpenedBuilderCacheKey = Key.create("project's.short.file.name.opened.builder");
  private static final UniqueNameBuilder<VirtualFile> ourEmptyBuilder = new UniqueNameBuilder<>(null, null, -1);

  private static String getUniqueVirtualFilePath(Project project,
                                                 VirtualFile file,
                                                 boolean skipNonOpenedFiles,
                                                 GlobalSearchScope scope) {
    UniqueNameBuilder<VirtualFile> builder = getUniqueVirtualFileNameBuilder(project,
                                                                             file,
                                                                             skipNonOpenedFiles,
                                                                             scope);
    if (builder != null) {
      return builder.getShortPath(file);
    }
    return file instanceof VirtualFilePathWrapper ? file.getPresentableName() : file.getName();
  }

  @Nullable
  private static UniqueNameBuilder<VirtualFile> getUniqueVirtualFileNameBuilder(Project project,
                                                 VirtualFile file,
                                                 boolean skipNonOpenedFiles,
                                                 GlobalSearchScope scope) {
    Key<CachedValue<Map<GlobalSearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>>> key =
      skipNonOpenedFiles ? ourShortNameOpenedBuilderCacheKey : ourShortNameBuilderCacheKey;
    CachedValue<Map<GlobalSearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>> data = project.getUserData(key);
    if (data == null) {
      project.putUserData(key, data = CachedValuesManager.getManager(project).createCachedValue(
        () -> new CachedValueProvider.Result<>(
          new ConcurrentHashMap<>(2),
          DumbService.getInstance(project),
          getFilenameIndexModificationTracker(project),
          FileEditorManagerImpl.OPEN_FILE_SET_MODIFICATION_COUNT
        ), false));
    }

    ConcurrentMap<GlobalSearchScope, Map<String, UniqueNameBuilder<VirtualFile>>> scope2ValueMap =
      (ConcurrentMap<GlobalSearchScope, Map<String, UniqueNameBuilder<VirtualFile>>>)data.getValue();
    Map<String, UniqueNameBuilder<VirtualFile>> valueMap = scope2ValueMap.get(scope);
    if (valueMap == null) {
      valueMap = ConcurrencyUtil.cacheOrGet(scope2ValueMap, scope, ContainerUtil.createConcurrentSoftValueMap());
    }

    final String fileName = file.getName();
    UniqueNameBuilder<VirtualFile> uniqueNameBuilderForShortName = valueMap.get(fileName);

    if (uniqueNameBuilderForShortName == null) {
      UniqueNameBuilder<VirtualFile> builder = filesWithTheSameName(fileName, project, skipNonOpenedFiles, scope);
      valueMap.put(fileName, builder != null ? builder : ourEmptyBuilder);
      uniqueNameBuilderForShortName = builder;
    }
    else if (uniqueNameBuilderForShortName == ourEmptyBuilder) {
      uniqueNameBuilderForShortName = null;
    }

    if (uniqueNameBuilderForShortName != null && uniqueNameBuilderForShortName.contains(file)) {
      return uniqueNameBuilderForShortName;
    }

    return null;
  }

  @NotNull
  private static ModificationTracker getFilenameIndexModificationTracker(@NotNull Project project) {
    return () -> disableIndexUpToDateCheckInEdt(() -> FileBasedIndex.getInstance().getIndexModificationStamp(FilenameIndex.NAME, project));
  }

  @Nullable
  private static UniqueNameBuilder<VirtualFile> filesWithTheSameName(String fileName,
                                                                     Project project,
                                                                     boolean skipNonOpenedFiles,
                                                                     GlobalSearchScope scope) {
    Collection<VirtualFile> filesWithSameName = skipNonOpenedFiles ? Collections.emptySet() : getFilesByNameFromIndex(fileName, project, scope);
    THashSet<VirtualFile> setOfFilesWithTheSameName = new THashSet<>(filesWithSameName);
    // add open files out of project scope
    for (VirtualFile openFile : FileEditorManager.getInstance(project).getOpenFiles()) {
      if (openFile.getName().equals(fileName)) {
        setOfFilesWithTheSameName.add(openFile);
      }
    }
    if (!skipNonOpenedFiles) {
      for (VirtualFile recentlyEditedFile : EditorHistoryManager.getInstance(project).getFileList()) {
        if (recentlyEditedFile.getName().equals(fileName)) {
          setOfFilesWithTheSameName.add(recentlyEditedFile);
        }
      }
    }

    filesWithSameName = setOfFilesWithTheSameName;

    if (filesWithSameName.size() > 1) {
      String path = project.getBasePath();
      path = path == null ? "" : FileUtil.toSystemIndependentName(path);
      UniqueNameBuilder<VirtualFile> builder = new UniqueNameBuilder<>(path, File.separator, 25);
      for (VirtualFile virtualFile : filesWithSameName) {
        String presentablePath = virtualFile instanceof VirtualFilePathWrapper ?
                                 ((VirtualFilePathWrapper)virtualFile).getPresentablePath() : virtualFile.getPath();
        builder.addPath(virtualFile, presentablePath);
      }
      return builder;
    }

    return null;
  }

  @NotNull
  private static Collection<VirtualFile> getFilesByNameFromIndex(@NotNull String fileName, @NotNull Project project, @NotNull GlobalSearchScope scope) {
    if (!DumbService.isDumb(project)) {
      // get data as is
      Collection<VirtualFile> rawDataFromIndex = disableIndexUpToDateCheckInEdt(() -> FilenameIndex.getVirtualFilesByName(project, fileName, scope));
      // filter only suitable files, we can miss some files but it's ok for presentation reasons
      return ContainerUtil.filter(rawDataFromIndex, f -> fileName.equals(f.getName()));
    }
    else {
      Ref<Collection<VirtualFile>> filesFromIndex = Ref.create();
      FileBasedIndex.getInstance().ignoreDumbMode(() -> filesFromIndex.set(FilenameIndex.getVirtualFilesByName(project, fileName, scope)),
                                                  DumbModeAccessType.RELIABLE_DATA_ONLY);
      return filesFromIndex.get();
    }
  }

  private static <T,E extends Throwable> T disableIndexUpToDateCheckInEdt(@NotNull ThrowableComputable<T, E> computable) throws E {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return ApplicationManager.getApplication().isDispatchThread()
           ? FileBasedIndexImpl.disableUpToDateCheckIn(computable)
           : computable.compute();
  }
}