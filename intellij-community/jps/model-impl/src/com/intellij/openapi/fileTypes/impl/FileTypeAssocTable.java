// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes.impl;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.CollectionFactory;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class FileTypeAssocTable<T> {
  private final Map<CharSequence, T> myExtensionMappings;
  private final Map<CharSequence, T> myExactFileNameMappings;
  private final Map<CharSequence, T> myExactFileNameAnyCaseMappings;
  private final List<Pair<FileNameMatcher, T>> myMatchingMappings;
  private final Map<String, T> myHashBangMap;

  private FileTypeAssocTable(@NotNull Map<? extends CharSequence, ? extends T> extensionMappings,
                             @NotNull Map<? extends CharSequence, ? extends T> exactFileNameMappings,
                             @NotNull Map<? extends CharSequence, ? extends T> exactFileNameAnyCaseMappings,
                             @NotNull Map<String, ? extends T> hashBangMap,
                             @NotNull List<? extends Pair<FileNameMatcher, T>> matchingMappings) {
    myExtensionMappings = CollectionFactory.createCharSequenceMap(false, Math.max(10, extensionMappings.size()), 0.5f);
    myExtensionMappings.putAll(extensionMappings);
    myExactFileNameMappings = CollectionFactory.createCharSequenceMap(true, Math.max(10, exactFileNameMappings.size()), 0.5f);
    myExactFileNameMappings.putAll(exactFileNameMappings);
    myExactFileNameAnyCaseMappings = CollectionFactory.createCharSequenceMap(false, Math.max(10, exactFileNameAnyCaseMappings.size()), 0.5f);
    myExactFileNameAnyCaseMappings.putAll(exactFileNameAnyCaseMappings);
    myHashBangMap = new Object2ObjectOpenHashMap<>(Math.max(10, hashBangMap.size()), 0.5f);
    myHashBangMap.putAll(hashBangMap);
    myMatchingMappings = new ArrayList<>(matchingMappings);
  }

  public FileTypeAssocTable() {
    this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
  }

  boolean isAssociatedWith(@NotNull T type, @NotNull FileNameMatcher matcher) {
    if (matcher instanceof ExtensionFileNameMatcher || matcher instanceof ExactFileNameMatcher) {
      return findAssociatedFileType(matcher) == type;
    }

    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (matcher.equals(mapping.getFirst()) && type == mapping.getSecond()) return true;
    }

    return false;
  }

  public void addAssociation(@NotNull FileNameMatcher matcher, @NotNull T type) {
    if (matcher instanceof ExtensionFileNameMatcher) {
      myExtensionMappings.put(((ExtensionFileNameMatcher)matcher).getExtension(), type);
    }
    else if (matcher instanceof ExactFileNameMatcher) {
      final ExactFileNameMatcher exactFileNameMatcher = (ExactFileNameMatcher)matcher;

      Map<CharSequence, T> mapToUse = exactFileNameMatcher.isIgnoreCase() ? myExactFileNameAnyCaseMappings : myExactFileNameMappings;
      mapToUse.put(exactFileNameMatcher.getFileName(), type);
    }
    else {
      myMatchingMappings.add(Pair.create(matcher, type));
    }
  }

  void addHashBangPattern(@NotNull String hashBang, @NotNull T type) {
    myHashBangMap.put(hashBang, type);
  }
  void removeHashBangPattern(@NotNull String hashBang, @NotNull T type) {
    myHashBangMap.remove(hashBang, type);
  }

  void removeAssociation(@NotNull FileNameMatcher matcher, @NotNull T type) {
    if (matcher instanceof ExtensionFileNameMatcher) {
      String extension = ((ExtensionFileNameMatcher)matcher).getExtension();
      if (myExtensionMappings.get(extension) == type) {
        myExtensionMappings.remove(extension);
        return;
      }
      return;
    }

    if (matcher instanceof ExactFileNameMatcher) {
      final ExactFileNameMatcher exactFileNameMatcher = (ExactFileNameMatcher)matcher;
      String fileName = exactFileNameMatcher.getFileName();

      final Map<CharSequence, T> mapToUse = exactFileNameMatcher.isIgnoreCase() ? myExactFileNameAnyCaseMappings : myExactFileNameMappings;
      if (mapToUse.get(fileName) == type) {
        mapToUse.remove(fileName);
      }
      return;
    }
    myMatchingMappings.removeIf(assoc -> matcher.equals(assoc.getFirst()));
  }

  void removeAllAssociations(@NotNull T type) {
    removeAssociationsFromMap(myExtensionMappings, type);

    removeAssociationsFromMap(myExactFileNameAnyCaseMappings, type);
    removeAssociationsFromMap(myExactFileNameMappings, type);

    myMatchingMappings.removeIf(assoc -> assoc.getSecond() == type);
    myHashBangMap.entrySet().removeIf(e -> e.getValue().equals(type));
  }

  private void removeAssociationsFromMap(@NotNull Map<CharSequence, T> extensionMappings, @NotNull T type) {
    extensionMappings.entrySet().removeIf(entry -> entry.getValue() == type);
  }

  @Nullable
  public T findAssociatedFileType(@NotNull @NonNls CharSequence fileName) {
    if (!myExactFileNameMappings.isEmpty()) {
      T t = myExactFileNameMappings.get(fileName);
      if (t != null) return t;
    }

    if (!myExactFileNameAnyCaseMappings.isEmpty()) {   // even hash lookup with case insensitive hasher is costly for isIgnored checks during compile
      T t = myExactFileNameAnyCaseMappings.get(fileName);
      if (t != null) return t;
    }

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myMatchingMappings.size(); i++) {
      Pair<FileNameMatcher, T> mapping = myMatchingMappings.get(i);
      if (mapping.getFirst().acceptsCharSequence(fileName)) return mapping.getSecond();
    }

    return findByExtension(FileUtilRt.getExtension(fileName));
  }

  @Nullable
  T findAssociatedFileTypeByHashBang(@NotNull CharSequence content) {
    for (Map.Entry<String, T> entry : myHashBangMap.entrySet()) {
      String hashBang = entry.getKey();
      if (FileUtil.isHashBangLine(content, hashBang)) return entry.getValue();
    }
    return null;
  }

  @Nullable
  T findAssociatedFileType(@NotNull FileNameMatcher matcher) {
    if (matcher instanceof ExtensionFileNameMatcher) {
      return findByExtension(((ExtensionFileNameMatcher)matcher).getExtension());
    }

    if (matcher instanceof ExactFileNameMatcher) {
      final ExactFileNameMatcher exactFileNameMatcher = (ExactFileNameMatcher)matcher;

      Map<CharSequence, T> mapToUse = exactFileNameMatcher.isIgnoreCase() ? myExactFileNameAnyCaseMappings : myExactFileNameMappings;
      return mapToUse.get(exactFileNameMatcher.getFileName());
    }

    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (matcher.equals(mapping.getFirst())) return mapping.getSecond();
    }

    return null;
  }

  T findByExtension(@NotNull CharSequence extension) {
    return myExtensionMappings.get(extension);
  }

  String @NotNull [] getAssociatedExtensions(@NotNull T type) {
    List<String> extensions = new ArrayList<>();
    for (Map.Entry<CharSequence, T> entry : myExtensionMappings.entrySet()) {
      if (entry.getValue() == type) {
        extensions.add(entry.getKey().toString());
      }
    }
    return ArrayUtilRt.toStringArray(extensions);
  }

  @NotNull
  public FileTypeAssocTable<T> copy() {
    return new FileTypeAssocTable<>(myExtensionMappings, myExactFileNameMappings, myExactFileNameAnyCaseMappings, myHashBangMap, myMatchingMappings);
  }

  @NotNull
  public List<FileNameMatcher> getAssociations(@NotNull T type) {
    List<FileNameMatcher> result = new ArrayList<>();
    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (mapping.getSecond() == type) {
        result.add(mapping.getFirst());
      }
    }

    for (Map.Entry<CharSequence, T> entry : myExactFileNameMappings.entrySet()) {
      if (entry.getValue() == type) {
        result.add(new ExactFileNameMatcher(entry.getKey().toString(), false));
      }
    }
    for (Map.Entry<CharSequence, T> entry : myExactFileNameAnyCaseMappings.entrySet()) {
      if (entry.getValue() == type) {
        result.add(new ExactFileNameMatcher(entry.getKey().toString(), true));
      }
    }
    for (Map.Entry<CharSequence, T> entry : myExtensionMappings.entrySet()) {
      if (entry.getValue() == type) {
        result.add(new ExtensionFileNameMatcher(entry.getKey().toString()));
      }
    }

    return result;
  }

  @NotNull
  public List<String> getHashBangPatterns(@NotNull T type) {
    return myHashBangMap.entrySet().stream()
      .filter(e -> e.getValue().equals(type))
      .map(e->e.getKey())
      .collect(Collectors.toList());
  }

  boolean hasAssociationsFor(@NotNull T fileType) {
    if (myExtensionMappings.containsValue(fileType) ||
        myExactFileNameMappings.containsValue(fileType) ||
        myHashBangMap.containsValue(fileType) ||
        myExactFileNameAnyCaseMappings.containsValue(fileType)) {
      return true;
    }
    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (mapping.getSecond() == fileType) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  Map<FileNameMatcher, T> getRemovedMappings(@NotNull FileTypeAssocTable<T> newTable, @NotNull Collection<? extends T> keys) {
    Map<FileNameMatcher, T> map = new HashMap<>();
    for (T key : keys) {
      List<FileNameMatcher> associations = getAssociations(key);
      associations.removeAll(newTable.getAssociations(key));
      for (FileNameMatcher matcher : associations) {
        map.put(matcher, key);
      }
    }
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FileTypeAssocTable<?> that = (FileTypeAssocTable<?>)o;
    return myExtensionMappings.equals(that.myExtensionMappings) &&
           myMatchingMappings.equals(that.myMatchingMappings) &&
           myExactFileNameMappings.equals(that.myExactFileNameMappings) &&
           myHashBangMap.equals(that.myHashBangMap) &&
           myExactFileNameAnyCaseMappings.equals(that.myExactFileNameAnyCaseMappings);
  }

  @Override
  public int hashCode() {
    int result = myExtensionMappings.hashCode();
    result = 31 * result + myMatchingMappings.hashCode();
    result = 31 * result + myHashBangMap.hashCode();
    result = 31 * result + myExactFileNameMappings.hashCode();
    result = 31 * result + myExactFileNameAnyCaseMappings.hashCode();
    return result;
  }

  @NotNull Map<String, T> getAllHashBangPatterns() {
    return Object2ObjectMaps.unmodifiable(new Object2ObjectOpenHashMap<>(myHashBangMap));
  }
}
