/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.stubs;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.LogUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ConcurrentIntObjectMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.RecentStringInterner;
import com.intellij.util.io.AbstractStringEnumerator;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.IOUtil;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

/**
 * Author: dmitrylomov
 */
class StubSerializationHelper {
  private final AbstractStringEnumerator myNameStorage;

  private final TIntObjectHashMap<String> myIdToName = new TIntObjectHashMap<>();
  private final TObjectIntHashMap<String> myNameToId = new TObjectIntHashMap<>();
  private final THashMap<String, Computable<ObjectStubSerializer>> myNameToLazySerializer = new THashMap<>();

  private final ConcurrentIntObjectMap<ObjectStubSerializer> myIdToSerializer = ContainerUtil.createConcurrentIntObjectMap();
  private final Map<ObjectStubSerializer, Integer> mySerializerToId = ContainerUtil.newConcurrentMap();

  StubSerializationHelper(@NotNull AbstractStringEnumerator nameStorage, @NotNull Disposable parentDisposable) {
    myNameStorage = nameStorage;
    myStringInterner = new RecentStringInterner(parentDisposable);
  }

  void assignId(@NotNull Computable<ObjectStubSerializer> serializer, String name) throws IOException {
    Computable<ObjectStubSerializer> old = myNameToLazySerializer.put(name, serializer);
    if (old != null) {
      ObjectStubSerializer existing = old.compute();
      ObjectStubSerializer computed = serializer.compute();
      if (existing != computed) {
        throw new AssertionError("ID: " + name + " is not unique, but found in both " +
                                 existing.getClass().getName() + " and " + computed.getClass().getName());
      }
      return;
    }

    int id = myNameStorage.enumerate(name);
    myIdToName.put(id, name);
    myNameToId.put(name, id);
  }

  void copyFrom(@Nullable StubSerializationHelper helper) throws IOException {
    if (helper == null) return;

    for (String name : helper.myNameToLazySerializer.keySet()) {
      assignId(helper.myNameToLazySerializer.get(name), name);
    }
  }

  private ObjectStubSerializer<Stub, Stub> writeSerializerId(Stub stub, @NotNull DataOutput stream)
    throws IOException {
    ObjectStubSerializer<Stub, Stub> serializer = StubSerializationUtil.getSerializer(stub);
    DataInputOutputUtil.writeINT(stream, getClassId(serializer));
    return serializer;
  }

  private void serializeSelf(Stub stub, @NotNull StubOutputStream stream) throws IOException {
    if (((ObjectStubBase)stub).isDangling()) {
      stream.writeByte(0);
    }
    writeSerializerId(stub, stream).serialize(stub, stream);
  }

  private void serializeChildren(@NotNull Stub parent, @NotNull StubOutputStream stream) throws IOException {
    final List<? extends Stub> children = parent.getChildrenStubs();
    DataInputOutputUtil.writeINT(stream, children.size());
    for (Stub child : children) {
      serializeSelf(child, stream);
      serializeChildren(child, stream);
    }
  }

  void serialize(@NotNull Stub rootStub, @NotNull OutputStream stream) throws IOException {
    BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream();
    FileLocalStringEnumerator storage = new FileLocalStringEnumerator(true);
    StubOutputStream stubOutputStream = new StubOutputStream(out, storage);
    boolean doDefaultSerialization = true;

    if (rootStub instanceof PsiFileStubImpl) {
      final PsiFileStub[] roots = ((PsiFileStubImpl<?>)rootStub).getStubRoots();
      if (roots.length == 0) {
        Logger.getInstance(getClass()).error("Incorrect stub files count during serialization:" + rootStub + "," + rootStub.getStubType());
      } else {
        doDefaultSerialization = false;
        DataInputOutputUtil.writeINT(stubOutputStream, roots.length);
        for (PsiFileStub root : roots) {
          serializeRoot(stubOutputStream, root, storage);
        }
      }
    }

    if (doDefaultSerialization) {
      DataInputOutputUtil.writeINT(stubOutputStream, 1);
      serializeRoot(stubOutputStream, rootStub, storage);
    }
    DataOutputStream resultStream = new DataOutputStream(stream);
    DataInputOutputUtil.writeINT(resultStream, storage.myStrings.size());
    byte[] buffer = IOUtil.allocReadWriteUTFBuffer();
    for(String s:storage.myStrings) {
      IOUtil.writeUTFFast(buffer, resultStream, s);
    }
    resultStream.write(out.getInternalBuffer(), 0, out.size());
  }

  private int getClassId(final ObjectStubSerializer serializer) {
    Integer idValue = mySerializerToId.get(serializer);
    if (idValue == null) {
      String name = serializer.getExternalId();
      idValue = myNameToId.get(name);
      assert idValue > 0 : "No ID found for serializer " + LogUtil.objectAndClass(serializer) +
                           ", external id:" + name +
                           (serializer instanceof IElementType ? ", language:" + ((IElementType)serializer).getLanguage() + ", " + serializer : "");
      mySerializerToId.put(serializer, idValue);
    }
    return idValue;
  }

  private final RecentStringInterner myStringInterner;
  private static final ThreadLocal<ObjectStubSerializer> ourRootStubSerializer = new ThreadLocal<>();

  @NotNull
  Stub deserialize(@NotNull InputStream stream) throws IOException, SerializerNotFoundException {
    FileLocalStringEnumerator storage = new FileLocalStringEnumerator(false);
    StubInputStream inputStream = new StubInputStream(stream, storage);
    final int numberOfStrings = DataInputOutputUtil.readINT(inputStream);
    byte[] buffer = IOUtil.allocReadWriteUTFBuffer();
    storage.myStrings.ensureCapacity(numberOfStrings);

    int i = 0;
    while(i < numberOfStrings) {
      String s = myStringInterner.get(IOUtil.readUTFFast(buffer, inputStream));
      storage.myStrings.add(s);
      ++i;
    }

    final int stubFilesCount = DataInputOutputUtil.readINT(inputStream);
    if (stubFilesCount <= 0) {
      Logger.getInstance(getClass()).error("Incorrect stub files count during deserialization:"+stubFilesCount);
    }

    Stub baseStub = deserializeRoot(storage, inputStream);
    final List<PsiFileStub> stubs = ContainerUtil.newArrayListWithCapacity(stubFilesCount);
    if (baseStub instanceof PsiFileStub) stubs.add((PsiFileStub)baseStub);
    for (int j = 1; j < stubFilesCount; j++) {
      Stub deserialize = deserializeRoot(storage, inputStream);
      if (deserialize instanceof PsiFileStub) {
        final PsiFileStub fileStub = (PsiFileStub)deserialize;
        stubs.add(fileStub);
      }
      else {
        Logger.getInstance(getClass()).error("Stub root must be PsiFileStub for files with several stub roots");
      }
    }
    final PsiFileStub[] stubsArray = stubs.toArray(PsiFileStub.EMPTY_ARRAY);
    for (PsiFileStub stub : stubsArray) {
      if (stub instanceof PsiFileStubImpl) {
        ((PsiFileStubImpl)stub).setStubRoots(stubsArray);
      }
    }
    return baseStub;
  }

  private Stub deserializeRoot(FileLocalStringEnumerator storage, StubInputStream inputStream) throws IOException, SerializerNotFoundException {
    ObjectStubSerializer<?, Stub> serializer = getClassById(DataInputOutputUtil.readINT(inputStream), null);
    ourRootStubSerializer.set(serializer);
    try {
      Stub stub = serializer.deserialize(inputStream, null);
      if (stub instanceof StubBase) {
        deserializeStubList(storage, inputStream, (StubBase)stub, serializer);
      } else {
        deserializeChildren(inputStream, stub);
      }
      return stub;
    }
    finally {
      ourRootStubSerializer.set(null);
    }
  }

  private void serializeRoot(StubOutputStream out, Stub root, AbstractStringEnumerator storage) throws IOException {
    serializeSelf(root, out);
    if (root instanceof StubBase) {
      StubList stubList = ((StubBase)root).myStubList;
      if (root != stubList.get(0)) {
        throw new IllegalArgumentException("Serialization is supported only for root stubs");
      }
      serializeStubList(out, storage, stubList);
    } else {
      serializeChildren(root, out);
    }
  }

  private void deserializeStubList(FileLocalStringEnumerator storage, StubInputStream inputStream, StubBase<?> root, ObjectStubSerializer rootType)
    throws IOException, SerializerNotFoundException {
    int stubCount = DataInputOutputUtil.readINT(inputStream);
    LazyStubList stubList = new LazyStubList(stubCount, root, rootType);

    MostlyUShortIntList parentsAndStarts = new MostlyUShortIntList(stubCount * 2);
    BitSet allStarts = new BitSet();

    new Object() {
      int currentIndex = 1;
      private void deserializeStub(int parentIndex) throws IOException, SerializerNotFoundException {
        int index = currentIndex;
        currentIndex++;

        int serializerId = DataInputOutputUtil.readINT(inputStream);
        int start = DataInputOutputUtil.readINT(inputStream);

        allStarts.set(start);

        addStub(parentIndex, index, start, (IElementType)getClassById(serializerId, null));
        deserializeChildren(index);
      }

      private void addStub(int parentIndex, int index, int start, IElementType type) {
        parentsAndStarts.add(parentIndex);
        parentsAndStarts.add(start);
        stubList.addLazyStub(type, index, parentIndex);
      }

      private void deserializeChildren(int parentIndex) throws IOException, SerializerNotFoundException {
        int childrenCount = DataInputOutputUtil.readINT(inputStream);
        stubList.prepareForChildren(parentIndex, childrenCount);
        for (int i = 0; i < childrenCount; i++) {
          deserializeStub(parentIndex);
        }
      }

      void deserializeRoot() throws IOException, SerializerNotFoundException {
        addStub(0, 0, 0, (IElementType)rootType);
        deserializeChildren(0);
      }
    }.deserializeRoot();
    byte[] serializedStubs = readByteArray(inputStream);
    stubList.setStubData(new LazyStubData(storage, parentsAndStarts, serializedStubs, allStarts));
  }

  private void serializeStubList(DataOutput out, AbstractStringEnumerator storage, StubList stubList) throws IOException {
    if (!stubList.isChildrenLayoutOptimal()) {
      throw new IllegalArgumentException("Manually assembled stubs should be normalized before serialization, consider wrapping them into StubTree");
    }

    DataInputOutputUtil.writeINT(out, stubList.size());
    DataInputOutputUtil.writeINT(out, stubList.getChildrenCount(0));

    BufferExposingByteArrayOutputStream tempBuffer = new BufferExposingByteArrayOutputStream();
    ByteArrayInterner interner = new ByteArrayInterner();

    for (int i = 1; i < stubList.size(); i++) {
      StubBase<?> stub = stubList.get(i);
      ObjectStubSerializer<Stub, Stub> serializer = writeSerializerId(stub, out);
      DataInputOutputUtil.writeINT(out, interner.internBytes(serializeStub(serializer, storage, stub, tempBuffer)));
      DataInputOutputUtil.writeINT(out, stubList.getChildrenCount(stub.id));
    }

    writeByteArray(out, interner.joinedBuffer.getInternalBuffer(), interner.joinedBuffer.size());
  }

  private static byte[] serializeStub(ObjectStubSerializer<Stub, Stub> serializer,
                                      AbstractStringEnumerator storage,
                                      StubBase<?> stub, BufferExposingByteArrayOutputStream tempBuffer) throws IOException {
    tempBuffer.reset();
    StubOutputStream stubOut = new StubOutputStream(tempBuffer, storage);
    serializer.serialize(stub, stubOut);
    if (stub.isDangling()) {
      stubOut.writeByte(0);
    }
    return tempBuffer.size() == 0 ? ArrayUtil.EMPTY_BYTE_ARRAY : tempBuffer.toByteArray();
  }

  private static class ByteArrayInterner {
    private static final TObjectHashingStrategy<byte[]> BYTE_ARRAY_STRATEGY = new TObjectHashingStrategy<byte[]>() {
      @Override
      public int computeHashCode(byte[] object) {
        return Arrays.hashCode(object);
      }

      @Override
      public boolean equals(byte[] o1, byte[] o2) {
        return Arrays.equals(o1, o2);
      }
    };
    private final TObjectIntHashMap<byte[]> arrayToStart = new TObjectIntHashMap<>(BYTE_ARRAY_STRATEGY);
    final BufferExposingByteArrayOutputStream joinedBuffer = new BufferExposingByteArrayOutputStream();

    int internBytes(byte[] bytes) {
      if (bytes.length == 0) return 0;

      int start = arrayToStart.get(bytes);
      if (start == 0) {
        start = joinedBuffer.size() + 1; // should be positive
        arrayToStart.put(bytes, start);
        joinedBuffer.write(bytes, 0, bytes.length);
      }
      return start;
    }
  }

  private byte[] readByteArray(StubInputStream inputStream) throws IOException {
    int length = DataInputOutputUtil.readINT(inputStream);
    if (length == 0) return ArrayUtil.EMPTY_BYTE_ARRAY;

    byte[] array = new byte[length];
    int read = inputStream.read(array);
    if (read != array.length) {
      Logger.getInstance(getClass()).error("Serialized array length mismatch");
    }
    return array;
  }

  private static void writeByteArray(DataOutput out, byte[] array, int len) throws IOException {
    DataInputOutputUtil.writeINT(out, len);
    out.write(array, 0, len);
  }

  String intern(String str) {
    return myStringInterner.get(str);
  }

  @SuppressWarnings("unchecked")
  private ObjectStubSerializer<?, Stub> getClassById(int id, @Nullable Stub parentStub) throws SerializerNotFoundException {
    ObjectStubSerializer<?, Stub> serializer = myIdToSerializer.get(id);
    if (serializer == null) {
      myIdToSerializer.put(id, serializer = instantiateSerializer(id, parentStub));
    }
    return serializer;
  }

  @NotNull
  private ObjectStubSerializer instantiateSerializer(int id, @Nullable Stub parentStub) throws SerializerNotFoundException {
    String name = myIdToName.get(id);
    Computable<ObjectStubSerializer> lazy = name == null ? null : myNameToLazySerializer.get(name);
    ObjectStubSerializer serializer = lazy == null ? null : lazy.compute();
    if (serializer == null) {
      throw reportMissingSerializer(id, parentStub);
    }
    return serializer;
  }

  private SerializerNotFoundException reportMissingSerializer(int id, @Nullable Stub parentStub) {
    String externalId = null;
    try {
      externalId = myNameStorage.valueOf(id);
    } catch (Throwable ignore) {}
    return new SerializerNotFoundException(
      brokenStubFormat(ourRootStubSerializer.get()) +
      "Internal details, no serializer registered for stub: ID=" + id + ", externalId:" + externalId +
      "; parent stub class=" + (parentStub != null? parentStub.getClass().getName() +", parent stub type:" + parentStub.getStubType() : "null"));
  }

  static String brokenStubFormat(ObjectStubSerializer root) {
    return "Broken stub format, most likely version of " + root + " was not updated after serialization changes\n";
  }

  private void deserializeChildren(StubInputStream stream, Stub parent) throws IOException, SerializerNotFoundException {
    int childCount = DataInputOutputUtil.readINT(stream);
    for (int i = 0; i < childCount; i++) {
      boolean dangling = false;
      int id = DataInputOutputUtil.readINT(stream);
      if (id == 0) {
        dangling = true;
        id = DataInputOutputUtil.readINT(stream);
      }

      Stub child = getClassById(id, parent).deserialize(stream, parent);
      if (dangling) {
        ((ObjectStubBase) child).markDangling();
      }
      deserializeChildren(stream, child);
    }
  }

  private static class FileLocalStringEnumerator implements AbstractStringEnumerator {
    private final TObjectIntHashMap<String> myEnumerates;
    private final ArrayList<String> myStrings = new ArrayList<>();

    FileLocalStringEnumerator(boolean forSavingStub) {
      myEnumerates = forSavingStub ? new TObjectIntHashMap<>() : null;
    }

    @Override
    public int enumerate(@Nullable String value) {
      if (value == null) return 0;
      assert myEnumerates != null; // enumerate possible only when writing stub
      int i = myEnumerates.get(value);
      if (i == 0) {
        myEnumerates.put(value, i = myStrings.size() + 1);
        myStrings.add(value);
      }
      return i;
    }

    @Override
    public String valueOf(int idx) {
      if (idx == 0) return null;
      return myStrings.get(idx - 1);
    }

    @Override
    public void markCorrupted() {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isDirty() {
      return false;
    }

    @Override
    public void force() {
    }
  }
}
