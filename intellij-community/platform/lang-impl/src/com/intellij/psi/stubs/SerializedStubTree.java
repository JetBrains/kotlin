/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.psi.stubs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ThreadLocalCachedValue;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.CompressionUtil;
import com.intellij.util.io.*;
import one.util.streamex.IntStreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.security.MessageDigest;
import java.util.Map;

public class SerializedStubTree {
  private static final Logger LOG = Logger.getInstance(SerializedStubTree.class);
  private static final ThreadLocalCachedValue<MessageDigest> HASHER = new ThreadLocalCachedValue<MessageDigest>() {
    @NotNull
    @Override
    protected MessageDigest create() {
      return DigestUtil.sha256();
    }
  };

  private final byte[] myBytes;
  private final int myLength;
  private Stub myStubElement;
  private IndexedStubs myIndexedStubs;

  public SerializedStubTree(final byte[] bytes, int length, @Nullable Stub stubElement) {
    myBytes = bytes;
    myLength = length;
    myStubElement = stubElement;
  }

  public SerializedStubTree(DataInput in) throws IOException {
    if (PersistentHashMapValueStorage.COMPRESSION_ENABLED) {
      int serializedStubsLength = DataInputOutputUtil.readINT(in);
      byte[] bytes = new byte[serializedStubsLength];
      in.readFully(bytes);
      myBytes = bytes;
      myLength = myBytes.length;
    }
    else {
      myBytes = CompressionUtil.readCompressed(in);
      myLength = myBytes.length;
    }
  }

  public void write(DataOutput out) throws IOException {
    if (PersistentHashMapValueStorage.COMPRESSION_ENABLED) {
      DataInputOutputUtil.writeINT(out, myLength);
      out.write(myBytes, 0, myLength);
    }
    else {
      CompressionUtil.writeCompressed(out, myBytes, 0, myLength);
    }
  }

  @NotNull
  public SerializedStubTree reSerialize(@NotNull SerializationManagerImpl currentSerializationManager,
                                        @NotNull SerializationManagerImpl newSerializationManager) throws IOException {
    BufferExposingByteArrayOutputStream outStub = new BufferExposingByteArrayOutputStream();
    currentSerializationManager.reSerialize(new ByteArrayInputStream(myBytes, 0, myLength), outStub, newSerializationManager);
    SerializedStubTree reSerialized = new SerializedStubTree(outStub.getInternalBuffer(), outStub.size(), null);
    reSerialized.setIndexedStubs(getIndexedStubs());
    return reSerialized;
  }

  // willIndexStub is one time optimization hint, once can safely pass false
  @NotNull
  public Stub getStub(boolean willIndexStub) throws SerializerNotFoundException {
    return getStub(willIndexStub, SerializationManagerEx.getInstanceEx());
  }

  @NotNull
  public Stub getStub(boolean willIndexStub, @NotNull SerializationManagerEx serializationManager) throws SerializerNotFoundException {
    Stub stubElement = myStubElement;
    if (stubElement != null) {
      // not null myStubElement means we just built SerializedStubTree for indexing,
      // if we request stub for indexing we can safely use it
      myStubElement = null;
      if (willIndexStub) return stubElement;
    }
    return serializationManager.deserialize(new UnsyncByteArrayInputStream(myBytes));
  }

  public void indexTree() throws SerializerNotFoundException {
    ObjectStubBase root = (ObjectStubBase)getStub(true);
    myIndexedStubs = new IndexedStubs(calculateHash(myBytes, myLength), indexTree(root));
  }

  @NotNull
  IndexedStubs getIndexedStubs() {
    return myIndexedStubs;
  }

  void setIndexedStubs(@NotNull IndexedStubs indexedStubs) {
    myIndexedStubs = indexedStubs;
  }

  public boolean equals(final Object that) {
    if (this == that) {
      return true;
    }
    if (!(that instanceof SerializedStubTree)) {
      return false;
    }
    final SerializedStubTree thatTree = (SerializedStubTree)that;

    final int length = myLength;
    if (length != thatTree.myLength) {
      return false;
    }

    final byte[] thisBytes = myBytes;
    final byte[] thatBytes = thatTree.myBytes;
    for (int i = 0; i < length; i++) {
      if (thisBytes[i] != thatBytes[i]) {
        return false;
      }
    }

    return true;
  }

  public int hashCode() {
    if (myBytes == null) {
      return 0;
    }

    int result = 1;
    for (int i = 0; i < myLength; i++) {
      result = 31 * result + myBytes[i];
    }

    return result;
  }

  @NotNull
  private String dumpStub() {
    String deserialized;
    try {
      deserialized = "stub: " + DebugUtil.stubTreeToString(getStub(true));
    }
    catch (SerializerNotFoundException e) {
      LOG.error(e);
      deserialized = "error while stub deserialization: " + e.getMessage();
    }
    return deserialized + "\n bytes: " + toHexString(myBytes, myLength);
  }

  @NotNull
  static Map<StubIndexKey, Map<Object, StubIdList>> indexTree(@NotNull Stub root) {
    ObjectStubTree objectStubTree = root instanceof PsiFileStub ? new StubTree((PsiFileStub)root, false) :
                                    new ObjectStubTree((ObjectStubBase)root, false);
    Map<StubIndexKey, Map<Object, int[]>> map = objectStubTree.indexStubTree();

    // xxx:fix refs inplace
    for (StubIndexKey key : map.keySet()) {
      Map<Object, int[]> value = map.get(key);
      for (Object k : value.keySet()) {
        int[] ints = value.get(k);
        StubIdList stubList = ints.length == 1 ? new StubIdList(ints[0]) : new StubIdList(ints, ints.length);
        ((Map<Object, StubIdList>)(Map)value).put(k, stubList);
      }
    }
    return (Map<StubIndexKey, Map<Object, StubIdList>>)(Map)map;
  }

  @NotNull
  private static byte[] calculateHash(@NotNull byte[] content, int length) {
    MessageDigest digest = HASHER.getValue();
    digest.update(content, 0, length);
    return digest.digest();
  }

  static void reportStubTreeHashCollision(@NotNull SerializedStubTree newTree,
                                          @NotNull SerializedStubTree existingTree,
                                          @NotNull byte[] hash) {
    String oldTreeDump = "\nexisting tree " + existingTree.dumpStub();
    String newTreeDump = "\nnew tree " + newTree.dumpStub();
    LOG.info("Stub tree hashing collision. Different trees have the same hash = " + toHexString(hash, hash.length) +
             ". Hashing algorithm = " + HASHER.getValue().getAlgorithm() + "." + oldTreeDump + newTreeDump, new Exception());
  }

  private static String toHexString(byte[] hash, int length) {
    return IntStreamEx.of(hash).limit(length).mapToObj(b -> String.format("%02x", b & 0xFF)).joining();
  }
}