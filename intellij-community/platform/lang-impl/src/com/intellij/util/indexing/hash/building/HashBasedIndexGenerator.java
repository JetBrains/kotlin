// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.impl.MapReduceIndex;
import com.intellij.util.indexing.impl.forward.ForwardIndex;
import com.intellij.util.indexing.impl.forward.ForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.MapForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.PersistentMapBasedForwardIndex;
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
    ID<K, V> indexId = originalExtension.getName();
    myExtension = new FakeIndexExtension<>(keyDescriptor, valueExternalizer, originalExtension);
    myStorageFile = getSharedIndexPath(outRoot, indexId).resolve(indexId.getName());

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

  @NotNull
  public static Path getSharedIndexPath(@NotNull Path root, ID<?, ?> id) {
    return root.resolve(id.getName());
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

    FileBasedIndexExtension<K, V> originalExtension = myExtension.myOriginalExtension;
    boolean isSingleEntryIndex = originalExtension instanceof SingleEntryFileBasedIndexExtension;
    Path forwardIndexPath = myStorageFile.getParent().resolve(myStorageFile.getFileName() + ".forward");
    ForwardIndex forwardIndex = isSingleEntryIndex ? null : new PersistentMapBasedForwardIndex(forwardIndexPath, false);
    ForwardIndexAccessor<K, V> forwardIndexAccessor = isSingleEntryIndex ? null : new MapForwardIndexAccessor<>(new InputMapExternalizer<>(originalExtension));
    myIndex = new MapReduceIndex<K, V, FileContent>(myExtension, indexStorage, forwardIndex, forwardIndexAccessor) {
      @NotNull
      @Override
      protected Map<K, V> mapByIndexer(int inputId, @NotNull FileContent content) {
        Map<K, V> data = super.mapByIndexer(inputId, content);
        if (isSingleEntryIndex && !data.isEmpty()) {
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

  public void indexFile(int hashId, @NotNull FileContent fileContent) {
    if (!myInputFilter.acceptInput(fileContent.getFile())) {
      return;
    }
    myIndexedFilesNumber.incrementAndGet();
    if (!myIndex.update(hashId, fileContent).compute()) {
      throw new RuntimeException("Index computation returned false for hashId = " + hashId + ", " +
                                 "file = " + fileContent.getFile().getPath() + ", " +
                                 "index = " + myExtension.getName().getName());
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
