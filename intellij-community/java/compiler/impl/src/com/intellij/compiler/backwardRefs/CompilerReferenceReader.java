// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.StorageException;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.backwardRefs.CompilerRef;
import org.jetbrains.jps.backwardRefs.NameEnumerator;
import org.jetbrains.jps.backwardRefs.index.CompilerReferenceIndex;

import java.io.File;
import java.util.Map;

public abstract class CompilerReferenceReader<Index extends CompilerReferenceIndex<?>> {
  protected final Index myIndex;
  protected final File myBuildDir;

  public CompilerReferenceReader(File buildDir, Index index) {
    myIndex = index;
    myBuildDir = buildDir;
  }

  @NotNull
  public NameEnumerator getNameEnumerator() {
    return myIndex.getByteSeqEum();
  }

  public void close(boolean removeIndex) {
    myIndex.close();
    if (removeIndex) {
      CompilerReferenceIndex.removeIndexFiles(myBuildDir);
    }
  }

  public Index getIndex() {
    return myIndex;
  }

  @Nullable
  public abstract TIntHashSet findReferentFileIds(@NotNull CompilerRef ref, boolean checkBaseClassAmbiguity) throws StorageException;

  @Nullable
  public abstract TIntHashSet findFileIdsWithImplicitToString(@NotNull CompilerRef ref) throws StorageException;

  @Nullable
  public abstract Map<VirtualFile, SearchId[]> getDirectInheritors(@NotNull CompilerRef searchElement,
                                                                   @NotNull GlobalSearchScope searchScope,
                                                                   @NotNull GlobalSearchScope dirtyScope,
                                                                   @NotNull FileType fileType,
                                                                   @NotNull CompilerHierarchySearchType searchType) throws StorageException;

  @Nullable
  public abstract Integer getAnonymousCount(@NotNull CompilerRef.CompilerClassHierarchyElementDef classDef, boolean checkDefinitions);

  public abstract int getOccurrenceCount(@NotNull CompilerRef element);

  @Nullable("return null if the class hierarchy contains ambiguous qualified names")
  public abstract CompilerRef.CompilerClassHierarchyElementDef[] getHierarchy(CompilerRef.CompilerClassHierarchyElementDef hierarchyElement,
                                                                              boolean checkBaseClassAmbiguity,
                                                                              boolean includeAnonymous,
                                                                              int interruptNumber);
}
