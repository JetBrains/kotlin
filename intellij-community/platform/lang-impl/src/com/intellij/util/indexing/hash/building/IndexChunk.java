// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class IndexChunk {
  private final Set<VirtualFile> myRoots;
  private final String myName;

  public IndexChunk(@NotNull Set<VirtualFile> roots, @NotNull String name) {
    myRoots = roots;
    myName = name;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public Set<VirtualFile> getRoots() {
    return myRoots;
  }

  @NotNull
  static IndexChunk mergeUnsafe(@NotNull IndexChunk ch1, @NotNull IndexChunk ch2) {
    ch1.getRoots().addAll(ch2.getRoots());
    return ch1;
  }

  @NotNull
  static Stream<IndexChunk> generate(@NotNull Module module) {
    Stream<IndexChunk> libChunks = Arrays.stream(ModuleRootManager.getInstance(module).getOrderEntries())
            .map(orderEntry -> {
              if (orderEntry instanceof LibraryOrSdkOrderEntry) {
                VirtualFile[] sources = orderEntry.getFiles(OrderRootType.SOURCES);
                VirtualFile[] classes = orderEntry.getFiles(OrderRootType.CLASSES);
                String name = null;
                if (orderEntry instanceof JdkOrderEntry) {
                  name = ((JdkOrderEntry)orderEntry).getJdkName();
                }
                else if (orderEntry instanceof LibraryOrderEntry) {
                  name = ((LibraryOrderEntry)orderEntry).getLibraryName();
                }
                if (name == null) {
                  name = "unknown";
                }
                return new IndexChunk(ContainerUtil.union(Arrays.asList(sources), Arrays.asList(classes)), reducePath(splitByDots(name)));
              }
              return null;
            })
            .filter(Objects::nonNull);

    Set<VirtualFile> roots =
            ContainerUtil.union(ContainerUtil.newTroveSet(ModuleRootManager.getInstance(module).getContentRoots()),
            ContainerUtil.newTroveSet(ModuleRootManager.getInstance(module).getSourceRoots()));
    Stream<IndexChunk> srcChunks = Stream.of(new IndexChunk(roots, getChunkName(module)));

    return Stream.concat(libChunks, srcChunks);
  }

  @NotNull
  private static String getChunkName(@NotNull Module module) {
    ModuleManager moduleManager = ModuleManager.getInstance(module.getProject());
    String[] path;
    if (moduleManager.hasModuleGroups()) {
      path = moduleManager.getModuleGroupPath(module);
      assert path != null;
    } else {
      path = splitByDots(module.getName());
    }
    return reducePath(path);
  }

  @NotNull
  private static String reducePath(String[] path) {
    String[] reducedPath = Arrays.copyOfRange(path, 0, Math.min(1, path.length));
    return StringUtil.join(reducedPath, ".");
  }

  private static String[] splitByDots(String name) {
    return name.split("[-|:.]");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IndexChunk chunk = (IndexChunk)o;
    return Objects.equals(myRoots, chunk.myRoots) &&
            Objects.equals(myName, chunk.myName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myRoots, myName);
  }
}
