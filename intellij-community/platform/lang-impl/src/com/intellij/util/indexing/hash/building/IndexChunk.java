// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public final class IndexChunk {
  private final Set<VirtualFile> myRoots;
  private final String myName;
  private String myContentsHash;

  public IndexChunk(@NotNull Set<VirtualFile> roots,
                    @NotNull String name) {
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

  @Nullable
  public String getContentsHash() {
    return myContentsHash;
  }

  public void setContentsHash(@Nullable String contentsHash) {
    myContentsHash = contentsHash;
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
