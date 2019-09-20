// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes.impl;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.text.CharSequenceHashingStrategy;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author max
 */
public class FileTypeAssocTable<T> {
  private final Map<CharSequence, T> myExtensionMappings;
  private final Map<CharSequence, T> myExactFileNameMappings;
  private final Map<CharSequence, T> myExactFileNameAnyCaseMappings;
  private final List<Pair<FileNameMatcher, T>> myMatchingMappings;

  private FileTypeAssocTable(@NotNull Map<? extends CharSequence, ? extends T> extensionMappings,
                             @NotNull Map<? extends CharSequence, ? extends T> exactFileNameMappings,
                             @NotNull Map<? extends CharSequence, T> exactFileNameAnyCaseMappings,
                             @NotNull List<? extends Pair<FileNameMatcher, T>> matchingMappings) {
    myExtensionMappings = new THashMap<>(Math.max(10, extensionMappings.size()), 0.5f, CharSequenceHashingStrategy.CASE_INSENSITIVE);
    myExtensionMappings.putAll(extensionMappings);
    myExactFileNameMappings = new THashMap<>(Math.max(10, exactFileNameMappings.size()), 0.5f, CharSequenceHashingStrategy.CASE_SENSITIVE);
    myExactFileNameMappings.putAll(exactFileNameMappings);
    myExactFileNameAnyCaseMappings = new THashMap<>(Math.max(10, exactFileNameAnyCaseMappings.size()), 0.5f, CharSequenceHashingStrategy.CASE_INSENSITIVE);
    myExactFileNameAnyCaseMappings.putAll(exactFileNameAnyCaseMappings);
    myMatchingMappings = new ArrayList<>(matchingMappings);
  }

  public FileTypeAssocTable() {
    this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
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

  boolean removeAssociation(@NotNull FileNameMatcher matcher, @NotNull T type) {
    if (matcher instanceof ExtensionFileNameMatcher) {
      String extension = ((ExtensionFileNameMatcher)matcher).getExtension();
      if (myExtensionMappings.get(extension) == type) {
        myExtensionMappings.remove(extension);
        return true;
      }
      return false;
    }

    if (matcher instanceof ExactFileNameMatcher) {
      final ExactFileNameMatcher exactFileNameMatcher = (ExactFileNameMatcher)matcher;
      String fileName = exactFileNameMatcher.getFileName();

      final Map<CharSequence, T> mapToUse = exactFileNameMatcher.isIgnoreCase() ? myExactFileNameAnyCaseMappings : myExactFileNameMappings;
      if(mapToUse.get(fileName) == type) {
        mapToUse.remove(fileName);
        return true;
      }
      return false;
    }

    return myMatchingMappings.removeIf(assoc -> matcher.equals(assoc.getFirst()));
  }

  boolean removeAllAssociations(@NotNull T type) {
    boolean changed = removeAssociationsFromMap(myExtensionMappings, type, false);

    changed = removeAssociationsFromMap(myExactFileNameAnyCaseMappings, type, changed);
    changed = removeAssociationsFromMap(myExactFileNameMappings, type, changed);

    return myMatchingMappings.removeIf(assoc -> assoc.getSecond() == type);
  }

  private boolean removeAssociationsFromMap(@NotNull Map<CharSequence, T> extensionMappings, @NotNull T type, boolean changed) {
    return extensionMappings.entrySet().removeIf(entry -> entry.getValue() == type) || changed;
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
      final Pair<FileNameMatcher, T> mapping = myMatchingMappings.get(i);
      if (mapping.getFirst().acceptsCharSequence(fileName)) return mapping.getSecond();
    }

    return findByExtension(FileUtilRt.getExtension(fileName));
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

  @Deprecated
  @NotNull
  String[] getAssociatedExtensions(@NotNull T type) {
    List<String> exts = new ArrayList<>();
    for (Map.Entry<CharSequence, T> entry : myExtensionMappings.entrySet()) {
      if (entry.getValue() == type) {
        exts.add(entry.getKey().toString());
      }
    }
    return ArrayUtilRt.toStringArray(exts);
  }

  @NotNull
  public FileTypeAssocTable<T> copy() {
    return new FileTypeAssocTable<>(myExtensionMappings, myExactFileNameMappings, myExactFileNameAnyCaseMappings, myMatchingMappings);
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

  boolean hasAssociationsFor(@NotNull T fileType) {
    if (myExtensionMappings.containsValue(fileType) ||
        myExactFileNameMappings.containsValue(fileType) ||
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

    FileTypeAssocTable<?> that = (FileTypeAssocTable)o;
    return myExtensionMappings.equals(that.myExtensionMappings) &&
           myMatchingMappings.equals(that.myMatchingMappings) &&
           myExactFileNameMappings.equals(that.myExactFileNameMappings) &&
           myExactFileNameAnyCaseMappings.equals(that.myExactFileNameAnyCaseMappings);
  }

  @Override
  public int hashCode() {
    int result = myExtensionMappings.hashCode();
    result = 31 * result + myMatchingMappings.hashCode();
    result = 31 * result + myExactFileNameMappings.hashCode();
    result = 31 * result + myExactFileNameAnyCaseMappings.hashCode();
    return result;
  }
}
