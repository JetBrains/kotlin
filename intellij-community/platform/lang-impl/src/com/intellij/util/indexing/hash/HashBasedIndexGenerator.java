// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.ContentHashesUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.impl.MapReduceIndex;
import com.intellij.util.indexing.snapshot.IndexedHashesSupport;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HashBasedIndexGenerator<K, V> {
  @NotNull
  private final FakeIndexExtension<K, V> myExtension;
  @NotNull
  private final FileBasedIndex.InputFilter myInputFilter;

  private final Path myStorageFile;

  private final AtomicInteger myIndexedFilesNumber = new AtomicInteger();

  private final AtomicBoolean myIsEmpty = new AtomicBoolean(true);

  private InvertedIndex<K, V, FileContent> myIndex;

  public HashBasedIndexGenerator(@NotNull FileBasedIndexExtension<K, V> indexExtension, @NotNull Path outRoot) {
    this(indexExtension.getKeyDescriptor(), indexExtension.getValueExternalizer(), indexExtension, outRoot);
  }

  public HashBasedIndexGenerator(@NotNull KeyDescriptor<K> keyDescriptor,
                                 @NotNull DataExternalizer<V> valueExternalizer,
                                 @NotNull FileBasedIndexExtension<K, V> originalExtension,
                                 @NotNull Path outRoot) {
    myExtension = new FakeIndexExtension<>(keyDescriptor, valueExternalizer, originalExtension);

    String indexName = originalExtension.getName().getName();
    myStorageFile = outRoot.resolve(StringUtil.toLowerCase(indexName)).resolve(indexName);

    FileBasedIndex.InputFilter filter = originalExtension.getInputFilter();

    if (filter instanceof FileBasedIndex.FileTypeSpecificInputFilter) {
      Set<FileType> fileTypes = new HashSet<>();
      ((FileBasedIndex.FileTypeSpecificInputFilter)filter).registerFileTypesUsedForIndexing(fileTypes::add);
      myInputFilter = file -> fileTypes.contains(file.getFileType()) && filter.acceptInput(file);
    }
    else {
      myInputFilter = filter;
    }
  }

  public void openIndex() throws IOException {
    IndexStorage<K, V> indexStorage = new MapIndexStorage<K, V>(myStorageFile,
                                                             myExtension.getKeyDescriptor(),
                                                             myExtension.getValueExternalizer(),
                                                             myExtension.getCacheSize(),
                                                             myExtension.keyIsUniqueForIndexedFile()) {
      @Override
      protected void checkCanceled() {
        //ignore
      }

      @Override
      public void addValue(K k, int inputId, V v) throws StorageException {
        super.addValue(k, inputId, v);
        myIsEmpty.set(false);
      }

      @Override
      public void removeAllValues(@NotNull K k, int inputId) {
        throw new AssertionError("Must not happen in index generator");
      }
    };

    myIndex = new MapReduceIndex<K, V, FileContent>(myExtension, indexStorage, null, null) {
      @NotNull
      @Override
      protected Map<K, V> mapByIndexer(int inputId, @NotNull FileContent content) {
        Map<K, V> data = super.mapByIndexer(inputId, content);
        boolean singleEntry = HashBasedIndexGenerator.this.myExtension.myOriginalExtension instanceof SingleEntryFileBasedIndexExtension;
        if (singleEntry && !data.isEmpty()) {
          data = Collections.singletonMap((K)(Integer)inputId, data.values().iterator().next());
        }
        return data;
      }

      @Override
      protected void updateForwardIndex(int inputId, @NotNull InputData<K, V> data) throws IOException {
        super.updateForwardIndex(inputId, data);
        try {
          visitInputData(inputId, data);
        }
        catch (StorageException e) {
          throw new IOException(e);
        }
      }

      @Override
      public void checkCanceled() {
        //ignore
      }

      @Override
      protected void requestRebuild(@NotNull Throwable e) {
        throw new RuntimeException("Error while processing " + myExtension.getName().getName(), e);
      }
    };
  }

  protected void visitInputData(int hashId, @NotNull InputData<K, V> data) throws StorageException {

  }

  public void closeIndex() throws IOException {
    if (myIndex != null) {
      myIndex.dispose();
    }
  }

  public void indexFile(@NotNull VirtualFile f, @NotNull Project project, @NotNull ContentHashesUtil.HashEnumerator hashEnumerator) {
    if (!myInputFilter.acceptInput(f)) {
      return;
    }
    myIndexedFilesNumber.incrementAndGet();
    try {
      FileContentImpl fc = new FileContentImpl(f, f.contentsToByteArray());
      byte[] hash = IndexedHashesSupport.getOrInitIndexedHash(fc, false);
      int hashId = Math.abs(hashEnumerator.enumerate(hash));
      fc.putUserData(IndexingDataKeys.PROJECT, project);
      if (!myIndex.update(hashId, fc).compute()) {
        throw new RuntimeException();
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Can't index " + f.getPath() + " for " + myExtension.getName().getName(), e);
    }
  }

  @NotNull
  public Path getIndexRoot() {
    return myStorageFile.getParent();
  }

  public int getIndexedFilesNumber() {
    return myIndexedFilesNumber.get();
  }

  public boolean isEmpty() {
    return myIsEmpty.get();
  }

  public FileBasedIndexExtension<K, V> getExtension() {
    return myExtension;
  }

  public InvertedIndex<K, V, FileContent> getIndex() {
    return myIndex;
  }

  private static final class FakeIndexExtension<K, V> extends FileBasedIndexExtension<K, V> {
    @NotNull
    private final KeyDescriptor<K> myKeyDescriptor;
    @NotNull
    private final DataExternalizer<V> myValueExternalizer;
    @NotNull
    private final FileBasedIndexExtension<K, V> myOriginalExtension;

    private FakeIndexExtension(@NotNull KeyDescriptor<K> descriptor,
                               @NotNull DataExternalizer<V> externalizer,
                               @NotNull FileBasedIndexExtension<K, V> originalExtension) {
      myKeyDescriptor = descriptor;
      myValueExternalizer = externalizer;
      myOriginalExtension = originalExtension;
    }

    @NotNull
    @Override
    public ID<K, V> getName() {
      return myOriginalExtension.getName();
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
      return myOriginalExtension.getInputFilter();
    }

    @Override
    public boolean dependsOnFileContent() {
      return true;
    }

    @Override
    public int getCacheSize() {
      return myOriginalExtension.getCacheSize();
    }

    @Override
    public boolean keyIsUniqueForIndexedFile() {
      return myOriginalExtension.keyIsUniqueForIndexedFile();
    }

    @NotNull
    @Override
    public DataIndexer<K, V, FileContent> getIndexer() {
      return myOriginalExtension.getIndexer();
    }

    @NotNull
    @Override
    public KeyDescriptor<K> getKeyDescriptor() {
      return myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<V> getValueExternalizer() {
      return myValueExternalizer;
    }

    @Override
    public int getVersion() {
      return myOriginalExtension.getVersion();
    }
  }
}
