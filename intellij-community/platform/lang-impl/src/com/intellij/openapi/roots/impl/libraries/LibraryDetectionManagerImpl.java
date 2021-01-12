// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.roots.libraries.*;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LibraryDetectionManagerImpl extends LibraryDetectionManager implements Disposable {
  private final Map<List<? extends VirtualFile>, List<Pair<LibraryKind, LibraryProperties>>> myCache = Collections.synchronizedMap(new HashMap<>());

  public LibraryDetectionManagerImpl() {
    Runnable listener = myCache::clear;
    LibraryType.EP_NAME.addChangeListener(listener, this);
    LibraryPresentationProvider.EP_NAME.addChangeListener(listener, this);
  }

  @Override
  public boolean processProperties(@NotNull List<? extends VirtualFile> files, @NotNull LibraryPropertiesProcessor processor) {
    for (Pair<LibraryKind, LibraryProperties> pair : getOrComputeKinds(files)) {
      if (!processor.processProperties(pair.getFirst(), pair.getSecond())) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  @Override
  public Pair<LibraryType<?>, LibraryProperties<?>> detectType(@NotNull List<? extends VirtualFile> files) {
    Pair<LibraryType<?>, LibraryProperties<?>> result = null;
    for (LibraryType<?> type : LibraryType.EP_NAME.getExtensions()) {
      final LibraryProperties<?> properties = type.detect((List<VirtualFile>)files);
      if (properties != null) {
        if (result != null) {
          return null;
        }
        result = Pair.create(type, properties);
      }
    }
    return result;
  }

  private List<Pair<LibraryKind, LibraryProperties>> getOrComputeKinds(List<? extends VirtualFile> files) {
    List<Pair<LibraryKind, LibraryProperties>> result = myCache.get(files);
    if (result == null) {
      result = computeKinds(files);
      myCache.put(files, result);
    }
    return result;
  }

  private static List<Pair<LibraryKind, LibraryProperties>> computeKinds(List<? extends VirtualFile> files) {
    final SmartList<Pair<LibraryKind, LibraryProperties>> result = new SmartList<>();
    final LibraryType<?>[] libraryTypes = LibraryType.EP_NAME.getExtensions();
    final LibraryPresentationProvider[] presentationProviders = LibraryPresentationProvider.EP_NAME.getExtensions();
    for (LibraryPresentationProvider provider : ContainerUtil.concat(libraryTypes, presentationProviders)) {
      final LibraryProperties properties = provider.detect(files);
      if (properties != null) {
        result.add(Pair.create(provider.getKind(), properties));
      }
    }
    return result;
  }

  @Override
  public void dispose() {
  }
}
