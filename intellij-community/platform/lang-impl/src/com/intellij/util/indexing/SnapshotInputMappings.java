// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.CompressionUtil;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.indexing.impl.DebugAssertions;
import com.intellij.util.indexing.impl.forward.AbstractForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.PersistentMapBasedForwardIndex;
import com.intellij.util.io.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

class SnapshotInputMappings<Key, Value, Input> implements UpdatableSnapshotInputMappingIndex<Key, Value, Input> {
  private static final Logger LOG = Logger.getInstance(SnapshotInputMappings.class);
  private static final boolean doReadSavedPersistentData = SystemProperties.getBooleanProperty("idea.read.saved.persistent.index", true);

  private final ID<Key, Value> myIndexId;
  private final InputMapExternalizer<Key, Value> myMapExternalizer;
  private final DataIndexer<Key, Value, Input> myIndexer;
  private final PersistentMapBasedForwardIndex myContents;
  private volatile PersistentHashMap<Integer, String> myIndexingTrace;

  private final HashIdForwardIndexAccessor<Key, Value, Input> myHashIdForwardIndexAccessor;

  private final boolean myIsPsiBackedIndex;

  SnapshotInputMappings(IndexExtension<Key, Value, Input> indexExtension) throws IOException {
    myIndexId = (ID<Key, Value>)indexExtension.getName();
    myIsPsiBackedIndex = indexExtension instanceof PsiDependentIndex;
    myMapExternalizer = new InputMapExternalizer<>(indexExtension);
    myIndexer = indexExtension.getIndexer();
    myContents = createContentsIndex();
    myHashIdForwardIndexAccessor = new HashIdForwardIndexAccessor<>(this);
    myIndexingTrace = DebugAssertions.EXTRA_SANITY_CHECKS ? createIndexingTrace() : null;
  }

  HashIdForwardIndexAccessor<Key, Value, Input> getForwardIndexAccessor() {
    return myHashIdForwardIndexAccessor;
  }

  File getInputIndexStorageFile() {
    return new File(IndexInfrastructure.getIndexRootDir(myIndexId), "fileIdToHashId");
  }

  @NotNull
  @Override
  public Map<Key, Value> readData(int hashId) throws IOException {
    ByteArraySequence byteSequence = readContents(hashId);
    if (byteSequence != null) {
      return AbstractForwardIndexAccessor.deserializeFromByteSeq(byteSequence, myMapExternalizer);
    }
    return Collections.emptyMap();
  }

  @Nullable
  @Override
  public Map<Key, Value> readData(@NotNull Input content) throws IOException {
    Map<Key, Value> data = null;

    if (doReadSavedPersistentData) {
      if (myContents == null || !myContents.isBusyReading() || DebugAssertions.EXTRA_SANITY_CHECKS) { // avoid blocking read, we can calculate index value
        int hashId = getHashId(content);
        ByteArraySequence bytes = readContents(hashId);

        if (bytes != null) {
          data = AbstractForwardIndexAccessor.deserializeFromByteSeq(bytes, myMapExternalizer);
          if (DebugAssertions.EXTRA_SANITY_CHECKS) {
            Map<Key, Value> contentData = myIndexer.map(content);
            boolean sameValueForSavedIndexedResultAndCurrentOne = contentData.equals(data);
            if (!sameValueForSavedIndexedResultAndCurrentOne) {
              DebugAssertions.error(
                "Unexpected difference in indexing of %s by index %s\ndiff %s\nprevious indexed info %s",
                getContentDebugData(content),
                myIndexId,
                buildDiff(data, contentData),
                myIndexingTrace.get(hashId)
              );
            }
          }
        }
      }
    }
    return data;
  }

  @Override
  public void putData(@Nullable Input content, @NotNull Map<Key, Value> data) throws IOException {
    int hashId = getHashId(content);
    boolean saved = savePersistentData(data, hashId);
    if (DebugAssertions.EXTRA_SANITY_CHECKS) {
      if (saved) {
        try {
          myIndexingTrace.put(hashId, getContentDebugData(content) +
                                      "," +
                                      ExceptionUtil.getThrowableText(new Throwable()));
        }
        catch (IOException ex) {
          LOG.error(ex);
        }
      }
    }
  }

  @NotNull
  private String getContentDebugData(Input input) {
    FileContentImpl content = (FileContentImpl) input;
    return "[" + content.getFile().getPath() + ";" + content.getFileType().getName() + ";" + content.getCharset() + "]";
  }

  @Override
  public int getHashId(@Nullable Input content) throws IOException {
    return content == null ? 0 : getHashOfContent((FileContent) content);
  }

  @Override
  public void flush() {
    if (myContents != null) myContents.force();
    if (myIndexingTrace != null) myIndexingTrace.force();
  }

  @Override
  public void clear() throws IOException {
    try {
      if (myIndexingTrace != null) {
        File baseFile = myIndexingTrace.getBaseFile();
        try {
          myIndexingTrace.close();
        }
        catch (Exception e) {
          LOG.error(e);
        }
        PersistentHashMap.deleteFilesStartingWith(baseFile);
        myIndexingTrace = createIndexingTrace();
      }
    } finally {
      if (myContents != null) {
        try {
          myContents.clear();
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    Stream.of(myContents, myIndexingTrace).filter(Objects::nonNull).forEach(index -> {
      try {
        index.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    });
  }

  private PersistentMapBasedForwardIndex createContentsIndex() throws IOException {
    if (SharedIndicesData.ourFileSharedIndicesEnabled && !SharedIndicesData.DO_CHECKS) return null;
    final File saved = new File(IndexInfrastructure.getPersistentIndexRootDir(myIndexId), "values");
    try {
      return new PersistentMapBasedForwardIndex(saved);
    }
    catch (IOException ex) {
      IOUtil.deleteAllFilesStartingWith(saved);
      throw ex;
    }
  }

  private PersistentHashMap<Integer, String> createIndexingTrace() throws IOException {
    final File mapFile = new File(IndexInfrastructure.getIndexRootDir(myIndexId), "indextrace");
    try {
      return new PersistentHashMap<>(mapFile, EnumeratorIntegerDescriptor.INSTANCE,
                                     new DataExternalizer<String>() {
                                       @Override
                                       public void save(@NotNull DataOutput out, String value) throws IOException {
                                         out.write((byte[])CompressionUtil.compressStringRawBytes(value));
                                       }

                                       @Override
                                       public String read(@NotNull DataInput in) throws IOException {
                                         byte[] b = new byte[((InputStream)in).available()];
                                         in.readFully(b);
                                         return (String)CompressionUtil.uncompressStringRawBytes(b);
                                       }
                                     }, 4096);
    }
    catch (IOException ex) {
      IOUtil.deleteAllFilesStartingWith(mapFile);
      throw ex;
    }
  }

  private ByteArraySequence readContents(Integer hashId) throws IOException {
    if (SharedIndicesData.ourFileSharedIndicesEnabled) {
      if (SharedIndicesData.DO_CHECKS) {
        synchronized (myContents) {
          ByteArraySequence contentBytes = SharedIndicesData.recallContentData(hashId, myIndexId, ByteSequenceDataExternalizer.INSTANCE);
          ByteArraySequence contentBytesFromContents = myContents.get(hashId);

          if (contentBytes == null && contentBytesFromContents != null ||
              !Comparing.equal(contentBytesFromContents, contentBytes)) {
            SharedIndicesData.associateContentData(hashId, myIndexId, contentBytesFromContents, ByteSequenceDataExternalizer.INSTANCE);
            if (contentBytes != null) {
              LOG.error("Unexpected indexing diff with hash id " + myIndexId + "," + hashId);
            }
            contentBytes = contentBytesFromContents;
          }
          return contentBytes;
        }
      } else {
        return SharedIndicesData.recallContentData(hashId, myIndexId, ByteSequenceDataExternalizer.INSTANCE);
      }
    }

    return myContents.get(hashId);
  }

  private Integer getHashOfContent(FileContent content) throws IOException {
    FileType fileType = content.getFileType();
    if (myIsPsiBackedIndex && content instanceof FileContentImpl) {
      // psi backed index should use existing psi to build index value (FileContentImpl.getPsiFileForPsiDependentIndex())
      // so we should use different bytes to calculate hash(Id)
      Integer previouslyCalculatedUncommittedHashId = content.getUserData(ourSavedUncommittedHashIdKey);

      if (previouslyCalculatedUncommittedHashId == null) {
        Document document = FileDocumentManager.getInstance().getCachedDocument(content.getFile());

        if (document != null) {  // if document is not committed
          PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(content.getProject());

          if (psiDocumentManager.isUncommited(document)) {
            PsiFile file = psiDocumentManager.getCachedPsiFile(document);
            Charset charset = ((FileContentImpl)content).getCharset();

            if (file != null) {
              previouslyCalculatedUncommittedHashId = ContentHashesSupport
                .calcContentHashIdWithFileType(file.getText().getBytes(charset), charset,
                                               fileType);
              content.putUserData(ourSavedUncommittedHashIdKey, previouslyCalculatedUncommittedHashId);
            }
          }
        }
      }
      if (previouslyCalculatedUncommittedHashId != null) return previouslyCalculatedUncommittedHashId;
    }

    Integer previouslyCalculatedContentHashId = content.getUserData(ourSavedContentHashIdKey);
    if (previouslyCalculatedContentHashId == null) {
      byte[] hash = content instanceof FileContentImpl ? ((FileContentImpl)content).getHash():null;
      if (hash == null) {
        if (fileType.isBinary()) {
          previouslyCalculatedContentHashId = ContentHashesSupport.calcContentHashId(content.getContent(), fileType);
        } else {
          Charset charset = content instanceof FileContentImpl ? ((FileContentImpl)content).getCharset() : null;
          previouslyCalculatedContentHashId = ContentHashesSupport
            .calcContentHashIdWithFileType(content.getContent(), charset, fileType);
        }
      } else {
        previouslyCalculatedContentHashId =  ContentHashesSupport.enumerateHash(hash);
      }
      content.putUserData(ourSavedContentHashIdKey, previouslyCalculatedContentHashId);
    }
    return previouslyCalculatedContentHashId;
  }
  private static final com.intellij.openapi.util.Key<Integer> ourSavedContentHashIdKey = com.intellij.openapi.util.Key.create("saved.content.hash.id");
  private static final com.intellij.openapi.util.Key<Integer> ourSavedUncommittedHashIdKey = com.intellij.openapi.util.Key.create("saved.uncommitted.hash.id");


  private StringBuilder buildDiff(Map<Key, Value> data, Map<Key, Value> contentData) {
    StringBuilder moreInfo = new StringBuilder();
    if (contentData.size() != data.size()) {
      moreInfo.append("Indexer has different number of elements, previously ").append(data.size()).append(" after ")
        .append(contentData.size()).append("\n");
    } else {
      moreInfo.append("total ").append(contentData.size()).append(" entries\n");
    }

    for(Map.Entry<Key, Value> keyValueEntry:contentData.entrySet()) {
      if (!data.containsKey(keyValueEntry.getKey())) {
        moreInfo.append("Previous data doesn't contain:").append(keyValueEntry.getKey()).append( " with value ").append(keyValueEntry.getValue()).append("\n");
      }
      else {
        Value value = data.get(keyValueEntry.getKey());
        if (!Comparing.equal(keyValueEntry.getValue(), value)) {
          moreInfo.append("Previous data has different value for key:").append(keyValueEntry.getKey()).append( ", new value ").append(keyValueEntry.getValue()).append( ", oldValue:").append(value).append("\n");
        }
      }
    }

    for(Map.Entry<Key, Value> keyValueEntry:data.entrySet()) {
      if (!contentData.containsKey(keyValueEntry.getKey())) {
        moreInfo.append("New data doesn't contain:").append(keyValueEntry.getKey()).append( " with value ").append(keyValueEntry.getValue()).append("\n");
      }
      else {
        Value value = contentData.get(keyValueEntry.getKey());
        if (!Comparing.equal(keyValueEntry.getValue(), value)) {
          moreInfo.append("New data has different value for key:").append(keyValueEntry.getKey()).append( " new value ").append(value).append( ", oldValue:").append(keyValueEntry.getValue()).append("\n");
        }
      }
    }
    return moreInfo;
  }

  private boolean savePersistentData(Map<Key, Value> data, int id) {
    try {
      if (myContents != null && myContents.isBusyReading() && myContents.containsMapping(id)) return false;
      saveContents(id, AbstractForwardIndexAccessor.serializeToByteSeq(data, myMapExternalizer, data.size()));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return true;
  }

  private void saveContents(int id, ByteArraySequence byteSequence) throws IOException {
    if (SharedIndicesData.ourFileSharedIndicesEnabled) {
      if (SharedIndicesData.DO_CHECKS) {
        synchronized (myContents) {
          myContents.put(id, byteSequence);
          SharedIndicesData.associateContentData(id, myIndexId, byteSequence, ByteSequenceDataExternalizer.INSTANCE);
        }
      } else {
        SharedIndicesData.associateContentData(id, myIndexId, byteSequence, ByteSequenceDataExternalizer.INSTANCE);
      }
    } else {
      myContents.put(id, byteSequence);
    }
  }
}
