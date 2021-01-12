// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.index.PrebuiltIndexProvider;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.KeyedExtensionCollector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.BitUtil;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.SystemProperties;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexDebugProperties;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.forward.EmptyForwardIndex;
import com.intellij.util.indexing.impl.forward.ForwardIndex;
import com.intellij.util.indexing.impl.forward.ForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.IntMapForwardIndex;
import com.intellij.util.indexing.impl.storage.TransientChangesIndexStorage;
import com.intellij.util.indexing.impl.storage.VfsAwareMapReduceIndex;
import com.intellij.util.indexing.snapshot.SnapshotInputMappings;
import com.intellij.util.io.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class StubUpdatingIndex extends SingleEntryFileBasedIndexExtension<SerializedStubTree>
  implements CustomImplementationFileBasedIndexExtension<Integer, SerializedStubTree> {
  static final Logger LOG = Logger.getInstance(StubUpdatingIndex.class);
  public static final boolean USE_SNAPSHOT_MAPPINGS = SystemProperties.is("stubs.use.snapshot.mappings");

  private static final int VERSION = 45 + (PersistentHashMapValueStorage.COMPRESSION_ENABLED ? 1 : 0);

  // todo remove once we don't need this for stub-ast mismatch debug info
  private static final FileAttribute INDEXED_STAMP = new FileAttribute("stubIndexStamp", 3, true);

  public static final ID<Integer, SerializedStubTree> INDEX_ID = ID.create("Stubs");

  private static final FileBasedIndex.InputFilter INPUT_FILTER = StubUpdatingIndex::canHaveStub;

  @NotNull
  private final StubForwardIndexExternalizer<?> myStubIndexesExternalizer;

  @NotNull
  private final SerializationManagerEx mySerializationManager;

  public StubUpdatingIndex() {
    this(StubForwardIndexExternalizer.getIdeUsedExternalizer(), SerializationManagerEx.getInstanceEx());
  }

  public StubUpdatingIndex(@NotNull StubForwardIndexExternalizer<?> stubIndexesExternalizer,
                           @NotNull SerializationManagerEx serializationManager) {
    myStubIndexesExternalizer = stubIndexesExternalizer;
    mySerializationManager = serializationManager;
  }

  @Override
  public boolean hasSnapshotMapping() {
    return USE_SNAPSHOT_MAPPINGS;
  }

  public static boolean canHaveStub(@NotNull VirtualFile file) {
    Project project = ProjectUtil.guessProjectForFile(file);
    FileType fileType = SubstitutedFileType.substituteFileType(file, file.getFileType(), project);
    if (fileType instanceof LanguageFileType) {
      final Language l = ((LanguageFileType)fileType).getLanguage();
      final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(l);
      if (parserDefinition == null) {
        return false;
      }

      final IFileElementType elementType = parserDefinition.getFileNodeType();
      if (elementType instanceof IStubFileElementType && ((IStubFileElementType)elementType).shouldBuildStubFor(file)) {
        return true;
      }
    }
    final BinaryFileStubBuilder builder = BinaryFileStubBuilders.INSTANCE.forFileType(fileType);
    return builder != null && builder.acceptsFile(file);
  }

  @NotNull
  @Override
  public ID<Integer, SerializedStubTree> getName() {
    return INDEX_ID;
  }

  @NotNull
  @Override
  public SingleEntryIndexer<SerializedStubTree> getIndexer() {
    return new SingleEntryCompositeIndexer<SerializedStubTree, StubBuilderType, String>(false) {
      @Override
      public boolean requiresContentForSubIndexerEvaluation(@NotNull IndexedFile file) {
        return StubTreeBuilder.requiresContentToFindBuilder(file.getFileType());
      }

      @Nullable
      @Override
      public StubBuilderType calculateSubIndexer(@NotNull IndexedFile file) {
        return StubTreeBuilder.getStubBuilderType(file, true);
      }

      @NotNull
      @Override
      public String getSubIndexerVersion(@NotNull StubBuilderType type) {
        return type.getVersion();
      }

      @NotNull
      @Override
      public KeyDescriptor<String> getSubIndexerVersionDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
      }

      @Override
      @Nullable
      protected SerializedStubTree computeValue(@NotNull final FileContent inputData, @NotNull StubBuilderType type) {
        try {
          SerializedStubTree prebuiltTree = findPrebuiltSerializedStubTree(inputData);
          if (prebuiltTree != null) {
            prebuiltTree = prebuiltTree.reSerialize(mySerializationManager, myStubIndexesExternalizer);
            if (PrebuiltIndexProvider.DEBUG_PREBUILT_INDICES) {
              assertPrebuiltStubTreeMatchesActualTree(prebuiltTree, inputData, type);
            }
            return prebuiltTree;
          }
        } catch (ProcessCanceledException pce) {
          throw pce;
        } catch (Exception e) {
          LOG.error("Error while indexing: " + inputData.getFileName() + " using prebuilt stub index", e);
        }

        try {
          Stub stub = StubTreeBuilder.buildStubTree(inputData, type);
          if (stub == null) {
            return null;
          }
          SerializedStubTree serializedStubTree = SerializedStubTree.serializeStub(stub, mySerializationManager, myStubIndexesExternalizer);
          if (IndexDebugProperties.DEBUG) {
            assertDeserializedStubMatchesOriginalStub(serializedStubTree, stub);
          }
          return serializedStubTree;
        }
        catch (ProcessCanceledException pce) {
          throw pce;
        }
        catch (Throwable t) {
          LOG.error("Error indexing:" + inputData.getFile(), t);
          return null;
        }
      }
    };
  }

  @Nullable
  static SerializedStubTree findPrebuiltSerializedStubTree(@NotNull FileContent fileContent) {
    if (!PrebuiltIndexProvider.USE_PREBUILT_INDEX) {
      return null;
    }
    PrebuiltStubsProvider prebuiltStubsProvider = PrebuiltStubsKt.getPrebuiltStubsProvider().forFileType(fileContent.getFileType());
    if (prebuiltStubsProvider == null) {
      return null;
    }
    return prebuiltStubsProvider.findStub(fileContent);
  }

  private static void assertDeserializedStubMatchesOriginalStub(@NotNull SerializedStubTree stubTree,
                                                                @NotNull Stub originalStub) {
    Stub deserializedStub;
    try {
      deserializedStub = stubTree.getStub();
    }
    catch (SerializerNotFoundException e) {
      throw new RuntimeException("Failed to deserialize stub tree", e);
    }
    assertStubsAreSimilar(originalStub, deserializedStub);
  }

  private static void assertStubsAreSimilar(@NotNull Stub stub, @NotNull Stub stub2) {
    assert stub.getStubType() == stub2.getStubType();
    List<? extends Stub> stubs = stub.getChildrenStubs();
    List<? extends Stub> stubs2 = stub2.getChildrenStubs();
    assert stubs.size() == stubs2.size();
    for (int i = 0, len = stubs.size(); i < len; ++i) {
      assertStubsAreSimilar(stubs.get(i), stubs2.get(i));
    }
  }

  private void assertPrebuiltStubTreeMatchesActualTree(@NotNull SerializedStubTree prebuiltStubTree,
                                                       @NotNull FileContent fileContent,
                                                       @NotNull StubBuilderType type) {
    try {
      Stub stub = StubTreeBuilder.buildStubTree(fileContent, type);
      if (stub == null) {
        return;
      }
      SerializedStubTree actualTree = SerializedStubTree.serializeStub(stub, mySerializationManager, myStubIndexesExternalizer);
      if (!IndexDataComparer.INSTANCE.areStubTreesTheSame(actualTree, prebuiltStubTree)) {
        throw new RuntimeExceptionWithAttachments(
          "Prebuilt stub tree does not match actual stub tree",
          new Attachment("actual-stub-tree.txt", IndexDataPresenter.INSTANCE.getPresentableSerializedStubTree(actualTree)),
          new Attachment("prebuilt-stub-tree.txt", IndexDataPresenter.INSTANCE.getPresentableSerializedStubTree(prebuiltStubTree))
        );
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final byte IS_BINARY_MASK = 1;
  private static final byte BYTE_AND_CHAR_LENGTHS_ARE_THE_SAME_MASK = 1 << 1;
  private static void rememberIndexingStamp(@NotNull FileContent content) {
    VirtualFile file = content.getFile();
    boolean isBinary = file.getFileType().isBinary();
    int contentLength = isBinary ? -1 : content.getPsiFile().getTextLength();
    long byteLength = file.getLength();
    rememberIndexingStamp(file, isBinary, byteLength, contentLength);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Indexing stubs for " + file + "; " + IndexingStampInfo.dumpSize(byteLength, contentLength));
    }
  }

  private static void rememberIndexingStamp(@NotNull VirtualFile file, boolean isBinary, long contentByteLength, int contentCharLength) {
    try (DataOutputStream stream = INDEXED_STAMP.writeAttribute(file)) {
      DataInputOutputUtil.writeTIME(stream, file.getTimeStamp());
      DataInputOutputUtil.writeLONG(stream, contentByteLength);

      boolean lengthsAreTheSame = contentByteLength == contentCharLength;
      byte flags = 0;
      flags = BitUtil.set(flags, IS_BINARY_MASK, isBinary);
      flags = BitUtil.set(flags, BYTE_AND_CHAR_LENGTHS_ARE_THE_SAME_MASK, lengthsAreTheSame);
      stream.writeByte(flags);

      if (!lengthsAreTheSame && !isBinary) {
        DataInputOutputUtil.writeINT(stream, contentCharLength);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Nullable
  static IndexingStampInfo getIndexingStampInfo(@NotNull VirtualFile file) {
    try (DataInputStream stream = INDEXED_STAMP.readAttribute(file)) {
      if (stream == null) {
        return null;
      }
      long stamp = DataInputOutputUtil.readTIME(stream);
      long byteLength = DataInputOutputUtil.readLONG(stream);

      byte flags = stream.readByte();
      boolean isBinary = BitUtil.isSet(flags, IS_BINARY_MASK);
      boolean readOnlyOneLength = BitUtil.isSet(flags, BYTE_AND_CHAR_LENGTHS_ARE_THE_SAME_MASK);

      int charLength;
      if (isBinary) {
        charLength = -1;
      }
      else if (readOnlyOneLength) {
        charLength = (int)byteLength;
      }
      else {
        charLength = DataInputOutputUtil.readINT(stream);
      }
      return new IndexingStampInfo(stamp, byteLength, charLength);
    }
    catch (IOException e) {
      LOG.error(e);
      return null;
    }
  }

  @NotNull
  @Override
  public DataExternalizer<SerializedStubTree> getValueExternalizer() {
    ensureSerializationManagerInitialized(mySerializationManager);
    return new SerializedStubTreeDataExternalizer(true, mySerializationManager, myStubIndexesExternalizer);
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return INPUT_FILTER;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @NotNull
  @Override
  public UpdatableIndex<Integer, SerializedStubTree, FileContent> createIndexImplementation(@NotNull final FileBasedIndexExtension<Integer, SerializedStubTree> extension,
                                                                                            @NotNull IndexStorage<Integer, SerializedStubTree> storage)
    throws StorageException, IOException {
    ((StubIndexImpl)StubIndex.getInstance()).initializeStubIndexes();
    if (storage instanceof TransientChangesIndexStorage) {
      final TransientChangesIndexStorage<Integer, SerializedStubTree>
        memStorage = (TransientChangesIndexStorage<Integer, SerializedStubTree>)storage;
      memStorage.addBufferingStateListener(new TransientChangesIndexStorage.BufferingStateListener() {
        @Override
        public void bufferingStateChanged(final boolean newState) {
          ((StubIndexImpl)StubIndex.getInstance()).setDataBufferingEnabled(newState);
        }

        @Override
        public void memoryStorageCleared() {
          ((StubIndexImpl)StubIndex.getInstance()).cleanupMemoryStorage();
        }
      });
    }
    checkNameStorage();

    boolean hasSnapshotMapping = VfsAwareMapReduceIndex.hasSnapshotMapping(this);
    StubUpdatingForwardIndexAccessor stubForwardIndexAccessor = new StubUpdatingForwardIndexAccessor(extension);

    SnapshotInputMappings<Integer, SerializedStubTree> snapshotInputMappings =
      hasSnapshotMapping
      ? new SnapshotInputMappings<>(this, stubForwardIndexAccessor)
      : null;

    ForwardIndex forwardIndex =
      hasSnapshotMapping
      ? new IntMapForwardIndex(snapshotInputMappings.getInputIndexStorageFile(), true)
      : new EmptyForwardIndex();

    ForwardIndexAccessor<Integer, SerializedStubTree> accessor =
      hasSnapshotMapping
      ? snapshotInputMappings.getForwardIndexAccessor()
      : stubForwardIndexAccessor;

    return new MyIndex(extension, storage, forwardIndex, accessor, snapshotInputMappings);
  }

  private void checkNameStorage() throws StorageException {
    if (mySerializationManager.isNameStorageCorrupted()) {
      mySerializationManager.repairNameStorage();
      throw new StorageException("NameStorage for stubs serialization has been corrupted");
    }
  }

  private static class MyIndex extends VfsAwareMapReduceIndex<Integer, SerializedStubTree> {
    private StubIndexImpl myStubIndex;
    @Nullable
    private final CompositeBinaryBuilderMap myCompositeBinaryBuilderMap = FileBasedIndex.USE_IN_MEMORY_INDEX ? null : new CompositeBinaryBuilderMap();

    MyIndex(@NotNull FileBasedIndexExtension<Integer, SerializedStubTree> extension,
            @NotNull IndexStorage<Integer, SerializedStubTree> storage,
            @Nullable ForwardIndex forwardIndex,
            @Nullable ForwardIndexAccessor<Integer, SerializedStubTree> forwardIndexAccessor,
            @Nullable SnapshotInputMappings<Integer, SerializedStubTree> snapshotInputMappings) throws IOException {
      super(extension, storage, forwardIndex, forwardIndexAccessor, snapshotInputMappings, null);
    }

    @Override
    protected void doFlush() throws IOException, StorageException {
      final StubIndexImpl stubIndex = getStubIndex();
      try {
        stubIndex.flush();
      }
      finally {
        super.doFlush();
      }
    }

    @NotNull
    private StubIndexImpl getStubIndex() {
      StubIndexImpl index = myStubIndex;
      if (index == null) {
        myStubIndex = index = (StubIndexImpl)StubIndex.getInstance();
      }
      return index;
    }

    @Override
    protected @NotNull InputData<Integer, SerializedStubTree> mapInput(int inputId, @Nullable FileContent content) {
      InputData<Integer, SerializedStubTree> data = super.mapInput(inputId, content);
      if (content != null && !data.getKeyValues().isEmpty()) {
        rememberIndexingStamp(content);
      }
      return data;
    }

    @Override
    protected void removeTransientDataForInMemoryKeys(int inputId, @NotNull Map<? extends Integer, ? extends SerializedStubTree> map) {
      super.removeTransientDataForInMemoryKeys(inputId, map);
      removeStubIndexKeys(inputId, getStubIndexMaps(map));
    }

    @Override
    public void removeTransientDataForKeys(int inputId, @NotNull Collection<? extends Integer> keys) {
      Map<StubIndexKey<?, ?>, Map<Object, StubIdList>> maps;
      try {
        Map<Integer, SerializedStubTree> data = getIndexedFileData(inputId);
        maps = getStubIndexMaps(data);
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
      super.removeTransientDataForKeys(inputId, keys);
      removeStubIndexKeys(inputId, maps);
    }

    private static void removeStubIndexKeys(int inputId, @NotNull Map<StubIndexKey<?, ?>, Map<Object, StubIdList>> indexedStubs) {
      StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
      for (StubIndexKey key : indexedStubs.keySet()) {
        stubIndex.removeTransientDataForFile(key, inputId, indexedStubs.get(key).keySet());
      }
    }

    @NotNull
    private static Map<StubIndexKey<?, ?>, Map<Object, StubIdList>> getStubIndexMaps(@NotNull Map<? extends Integer, ? extends SerializedStubTree> data) {
      if (data.isEmpty()) {
        return Collections.emptyMap();
      }
      SerializedStubTree tree = data.values().iterator().next();
      return tree == null ? Collections.emptyMap() : tree.getStubIndicesValueMap();
    }

    @Override
    protected void doClear() throws StorageException, IOException {
      final StubIndexImpl stubIndex = StubIndexImpl.getInstanceOrInvalidate();
      if (stubIndex != null) {
        stubIndex.clearAllIndices();
      }
      super.doClear();
    }

    @Override
    protected void doDispose() throws StorageException {
      try {
        super.doDispose();
      }
      finally {
        getStubIndex().dispose();
      }
    }

    @Override
    public void setIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
      super.setIndexedStateForFile(fileId, file);
      setBinaryBuilderConfiguration(fileId, file);
    }

    @Override
    protected boolean isIndexConfigurationUpToDate(int fileId, @NotNull IndexedFile file) {
      if (myCompositeBinaryBuilderMap == null) return true;
      try {
        return myCompositeBinaryBuilderMap.isUpToDateState(fileId, file.getFile());
      } catch (IOException e) {
        LOG.error(e);
        return false;
      }
    }

    @Override
    protected void setIndexConfigurationUpToDate(int fileId, @NotNull IndexedFile file) {
      setBinaryBuilderConfiguration(fileId, file);
    }

    private void setBinaryBuilderConfiguration(int fileId, @NotNull IndexedFile file) {
      if (myCompositeBinaryBuilderMap != null) {
        try {
          myCompositeBinaryBuilderMap.persistState(fileId, file.getFile());
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    }
  }

  private static void ensureSerializationManagerInitialized(@NotNull SerializationManagerEx serializationManager) {
    ProgressManager.getInstance().executeNonCancelableSection(() -> {
      instantiateElementTypesFromFields();
      StubIndexEx.initExtensions();
      serializationManager.initSerializers();
    });
  }

  private static void instantiateElementTypesFromFields() {
    // load stub serializers before usage
    FileTypeRegistry.getInstance().getRegisteredFileTypes();
    getExtensions(BinaryFileStubBuilders.INSTANCE).forEach(builder -> {});
    getExtensions(LanguageParserDefinitions.INSTANCE).forEach(ParserDefinition::getFileNodeType);
  }

  private static @NotNull <T> Stream<T> getExtensions(@NotNull KeyedExtensionCollector<T, ?> collector) {
    ExtensionPoint<KeyedLazyInstance<T>> point = collector.getPoint();
    return point == null ? Stream.empty() : point.extensions().map(KeyedLazyInstance::getInstance);
  }
}
