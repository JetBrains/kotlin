// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.persistent.ContentHashesUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.impl.MapReduceIndex;
import com.intellij.util.indexing.snapshot.ContentHashesSupport;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HashBasedIndexGenerator<K, V> {
  @NotNull private final File myOut;
  @NotNull private final File myHashOut;
  @NotNull private final FakeIndexExtension<K, V> myExtension;
  @NotNull private final FileBasedIndex.InputFilter myInputFilter;

  protected ContentHashesUtil.HashEnumerator myHashEnumerator;
  private InvertedIndex<K, V, FileContent> myIndex;

  public HashBasedIndexGenerator(@NotNull FileBasedIndexExtension<K, V> indexExtension, @NotNull File out) {
    this(indexExtension.getKeyDescriptor(),
         indexExtension.getValueExternalizer(),
         indexExtension,
         out,
         out);
  }

  public HashBasedIndexGenerator(@NotNull KeyDescriptor<K> keyDescriptor,
                                 @NotNull DataExternalizer<V> valueExternalizer,
                                 @NotNull FileBasedIndexExtension<K, V> originalExtension,
                                 @NotNull File out,
                                 @NotNull File hashOut) {
    myExtension = new FakeIndexExtension<>(keyDescriptor, valueExternalizer, originalExtension);
    myOut = out;
    myHashOut = hashOut;

    FileBasedIndex.InputFilter filter = originalExtension.getInputFilter();

    if (filter instanceof FileBasedIndex.FileTypeSpecificInputFilter) {
      Set<FileType> fileTypes = new HashSet<>();
      ((FileBasedIndex.FileTypeSpecificInputFilter)filter).registerFileTypesUsedForIndexing(fileTypes::add);
      myInputFilter = file -> fileTypes.contains(file.getFileType()) && filter.acceptInput(file);
    } else {
      myInputFilter = filter;
    }
  }

  @NotNull
  public File getOut() {
    return myOut;
  }

  public void openIndex() throws IOException {
    myHashEnumerator = getHashEnumerator();
    String indexName = myExtension.getName().getName();
    myIndex = new MapReduceIndex<K, V, FileContent>(myExtension, new MapIndexStorage<K, V>(new File(new File(myOut, StringUtil.toLowerCase(indexName)), indexName),
                                                                                           myExtension.getKeyDescriptor(),
                                                                                           myExtension.getValueExternalizer(),
                                                                                           myExtension.getCacheSize(),
                                                                                           myExtension.keyIsUniqueForIndexedFile()) {
      @Override
      protected void checkCanceled() {
        //ignore
      }
    }, null, null) {
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
        throw new RuntimeException(e);
      }
    };
  }

  @NotNull
  protected ContentHashesUtil.HashEnumerator getHashEnumerator() throws IOException {
    return new ContentHashesUtil.HashEnumerator(new File(myHashOut, "hashes"));
  }

  protected void visitInputData(int hashId, @NotNull InputData<K, V> data) throws StorageException {

  }

  public void closeIndex() throws IOException {
    if (myIndex != null) myIndex.dispose();
    if (myHashEnumerator != null) myHashEnumerator.close();
  }


  public final void generate(@NotNull Collection<VirtualFile> roots) {
    try {
      openIndex();

      for (VirtualFile root : roots) {
        VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Boolean>() {
          @Override
          public boolean visitFile(@NotNull VirtualFile file) {
            if (!file.isDirectory() && myInputFilter.acceptInput(file)) {
              indexFile(file);
            }
            return true;
          }
        });
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      try {
        closeIndex();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected void indexFile(@NotNull VirtualFile f) {
    try {
      FileContentImpl fc = new FileContentImpl(f, f.contentsToByteArray());
      byte[] hash = ContentHashesSupport.calculateHash(fc.getContent(), fc.getCharset(), f.getFileType(), fc.getFileType());
      int hashId = myHashEnumerator.enumerate(hash);
      if (!myIndex.update(hashId, fc).compute()) {
        throw new RuntimeException();
      }
    }
    catch (IOException e) {
      throw new RuntimeException("cant index " + f.getPath() + " for " + myExtension.getName().getName(), e);
    }
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
