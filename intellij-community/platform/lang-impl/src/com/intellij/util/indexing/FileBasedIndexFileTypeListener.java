// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.fileTypes.*;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class FileBasedIndexFileTypeListener implements FileTypeListener {
  @Nullable private Map<FileType, Set<String>> myTypeToExtensionMap;

  @Override
  public void beforeFileTypesChanged(@NotNull final FileTypeEvent event) {
    FileBasedIndexImpl.cleanupProcessedFlag();
    myTypeToExtensionMap = new THashMap<>();
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    for (FileType type : fileTypeManager.getRegisteredFileTypes()) {
      myTypeToExtensionMap.put(type, getExtensions(type, fileTypeManager));
    }
  }

  @Override
  public void fileTypesChanged(@NotNull final FileTypeEvent event) {
    final Map<FileType, Set<String>> oldTypeToExtensionsMap = myTypeToExtensionMap;
    myTypeToExtensionMap = null;

    // file type added
    FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    if (event.getAddedFileType() != null) {
      fileBasedIndex.rebuildAllIndices("The following file type was added: " + event.getAddedFileType());
      return;
    }

    if (oldTypeToExtensionsMap == null) {
      return;
    }

    final Map<FileType, Set<String>> newTypeToExtensionsMap = new THashMap<>();
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    for (FileType type : fileTypeManager.getRegisteredFileTypes()) {
      newTypeToExtensionsMap.put(type, getExtensions(type, fileTypeManager));
    }
    // file type changes and removals
    if (!newTypeToExtensionsMap.keySet().containsAll(oldTypeToExtensionsMap.keySet())) {
      Set<FileType> removedFileTypes = new HashSet<>(oldTypeToExtensionsMap.keySet());
      removedFileTypes.removeAll(newTypeToExtensionsMap.keySet());
      fileBasedIndex
        .rebuildAllIndices("The following file types were removed/are no longer associated: " + removedFileTypes);
      return;
    }
    for (Map.Entry<FileType, Set<String>> entry : oldTypeToExtensionsMap.entrySet()) {
      FileType fileType = entry.getKey();
      Set<String> oldMatchers = entry.getValue();
      Set<String> newMatchers = newTypeToExtensionsMap.get(fileType);
      if (!newMatchers.equals(oldMatchers)) {
        Set<String> removed = complement(oldMatchers, newMatchers);
        Set<String> added = complement(newMatchers, oldMatchers);
        fileBasedIndex.rebuildAllIndices(fileType.getName()
                                         + " is no longer associated with matchers "
                                         + String.join(",", removed)
                                         + ", added matchers "
                                         + String.join(",", added));
        return;
      }
    }
  }

  @NotNull
  private static Set<String> getExtensions(@NotNull FileType type, @NotNull FileTypeManager fileTypeManager) {
    return fileTypeManager
      .getAssociations(type)
      .stream()
      .map(FileNameMatcher::getPresentableString)
      .collect(Collectors.toCollection(THashSet::new));
  }

  private static Set<String> complement(Set<String> set, Set<String> toRemove) {
    THashSet<String> result = new THashSet<>(set);
    result.removeAll(toRemove);
    return result;
  }
}
