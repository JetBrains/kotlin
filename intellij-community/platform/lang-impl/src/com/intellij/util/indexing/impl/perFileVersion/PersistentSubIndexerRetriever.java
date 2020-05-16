// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.impl.perFileVersion;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.util.indexing.CompositeDataIndexer;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.IndexInfrastructure;
import com.intellij.util.indexing.IndexedFile;
import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class PersistentSubIndexerRetriever<SubIndexerType, SubIndexerVersion> implements Closeable {
  private static final String INDEXED_VERSIONS = "indexed_versions";

  @NotNull
  private final PersistentSubIndexerVersionEnumerator<SubIndexerVersion> myPersistentVersionEnumerator;
  @NotNull
  private final FileAttribute myFileAttribute;
  @NotNull
  private final CompositeDataIndexer<?, ?, SubIndexerType, SubIndexerVersion> myIndexer;

  public PersistentSubIndexerRetriever(@NotNull ID<?, ?> id,
                                int indexVersion,
                                @NotNull CompositeDataIndexer<?, ?, SubIndexerType, SubIndexerVersion> indexer) throws IOException {
    this(IndexInfrastructure.getIndexRootDir(id), id.getName(), indexVersion, indexer);
  }

  @TestOnly
  PersistentSubIndexerRetriever(@NotNull File root,
                                @NotNull String indexName,
                                int indexVersion,
                                @NotNull CompositeDataIndexer<?, ?, SubIndexerType, SubIndexerVersion> indexer) throws IOException {
    Path versionMapRoot = root.toPath().resolve(versionMapRoot());
    myFileAttribute = getFileAttribute(indexName, indexVersion);
    myIndexer = indexer;
    myPersistentVersionEnumerator = new PersistentSubIndexerVersionEnumerator<>(
      versionMapRoot.resolve(INDEXED_VERSIONS).toFile(),
      indexer.getSubIndexerVersionDescriptor());
  }

  public void clear() throws IOException {
    myPersistentVersionEnumerator.clear();
  }

  @Override
  public void close() throws IOException {
    myPersistentVersionEnumerator.close();
  }

  public void flush() throws IOException {
    myPersistentVersionEnumerator.flush();
  }

  private static Path versionMapRoot() {
    return Paths.get(".perFileVersion", INDEXED_VERSIONS);
  }

  public void setIndexedState(int fileId, @NotNull IndexedFile file) throws IOException {
    try (DataOutputStream stream = FSRecords.writeAttribute(fileId, myFileAttribute)) {
      DataInputOutputUtil.writeINT(stream, ProgressManager.getInstance().computeInNonCancelableSection(() -> getFileIndexerId(file)));
    }
  }

  public boolean isIndexed(int fileId, @NotNull IndexedFile file) throws IOException {
    DataInputStream stream = FSRecords.readAttributeWithLock(fileId, myFileAttribute);
    int currentIndexedVersion;
    if (stream != null) {
      currentIndexedVersion = DataInputOutputUtil.readINT(stream);
      int actualVersion = getFileIndexerId(file);
      return actualVersion == currentIndexedVersion;
    }
    return false;
  }

  public int getFileIndexerId(@NotNull IndexedFile file) throws IOException {
    SubIndexerVersion version = getVersion(file);
    if (version == null) return -1;
    return myPersistentVersionEnumerator.enumerate(version);
  }

  @Nullable
  public SubIndexerVersion getVersion(@NotNull IndexedFile file) {
    SubIndexerType type = myIndexer.calculateSubIndexer(file);
    if (type == null) return null;
    return myIndexer.getSubIndexerVersion(type);
  }

  private static final Map<Pair<String, Integer>, FileAttribute> ourAttributes = new THashMap<>();
  private static FileAttribute getFileAttribute(String name, int version) {
    synchronized (ourAttributes) {
      return ourAttributes.computeIfAbsent(Pair.create(name, version), __ -> new FileAttribute(name + ".index.version", version, false));
    }
  }
}