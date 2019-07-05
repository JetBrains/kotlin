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
import com.intellij.util.io.*;
import one.util.streamex.IntStreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.io.DataOutputStream;
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

  // serialized tree
  final byte[] myTreeBytes;
  final int myTreeByteLength;
  final byte[] mySerializedTreeHash;
  private Stub myStubElement;

  // stub forward indexes
  final byte[] myIndexedStubBytes;
  final int myIndexedStubByteLength;
  private Map<StubIndexKey, Map<Object, StubIdList>> myIndexedStubs;

  public SerializedStubTree(@NotNull byte[] treeBytes, int treeByteLength, @Nullable byte[] serializedTreeHash, @Nullable Stub stubElement,
                            @NotNull byte[] indexedStubBytes, int indexedStubByteLength, @Nullable Map<StubIndexKey, Map<Object, StubIdList>> indexedStubs) {
    myTreeBytes = treeBytes;
    myTreeByteLength = treeByteLength;
    mySerializedTreeHash = serializedTreeHash == null ? calculateHash(treeBytes, treeByteLength) : serializedTreeHash;
    myStubElement = stubElement;

    myIndexedStubBytes = indexedStubBytes;
    myIndexedStubByteLength = indexedStubByteLength;
    myIndexedStubs = indexedStubs;
  }

  public SerializedStubTree(@NotNull Stub rootStub,
                            @NotNull SerializationManagerEx serializationManager,
                            @NotNull StubForwardIndexExternalizer<?> forwardIndexExternalizer) throws IOException {
    final BufferExposingByteArrayOutputStream bytes = new BufferExposingByteArrayOutputStream();
    serializationManager.serialize(rootStub, bytes);
    myTreeBytes = bytes.getInternalBuffer();
    myTreeByteLength = bytes.size();
    mySerializedTreeHash = calculateHash(myTreeBytes, myTreeByteLength);
    ObjectStubBase root = (ObjectStubBase)rootStub;
    myIndexedStubs = indexTree(root);
    final BufferExposingByteArrayOutputStream indexBytes = new BufferExposingByteArrayOutputStream();
    forwardIndexExternalizer.save(new DataOutputStream(indexBytes), myIndexedStubs);
    myIndexedStubBytes = indexBytes.getInternalBuffer();
    myIndexedStubByteLength = indexBytes.size();
  }

  @NotNull
  public SerializedStubTree reSerialize(@NotNull SerializationManagerImpl currentSerializationManager,
                                        @NotNull SerializationManagerImpl newSerializationManager,
                                        @NotNull StubForwardIndexExternalizer currentForwardIndexSerializer,
                                        @NotNull StubForwardIndexExternalizer newForwardIndexSerializer) throws IOException {
    BufferExposingByteArrayOutputStream outStub = new BufferExposingByteArrayOutputStream();
    currentSerializationManager.reSerialize(new ByteArrayInputStream(myTreeBytes, 0, myTreeByteLength), outStub, newSerializationManager);

    byte[] reSerializedIndexBytes;
    int reSerializedIndexByteLength;

    if (currentForwardIndexSerializer == newForwardIndexSerializer) {
      reSerializedIndexBytes = myIndexedStubBytes;
      reSerializedIndexByteLength = myIndexedStubByteLength;
    }
    else {
      BufferExposingByteArrayOutputStream reSerializedStubIndices = new BufferExposingByteArrayOutputStream();
      if (myIndexedStubs == null) {
        restoreIndexedStubs(currentForwardIndexSerializer);
      }
      assert myIndexedStubs != null;
      newForwardIndexSerializer.save(new DataOutputStream(reSerializedStubIndices), myIndexedStubs);
      reSerializedIndexBytes = reSerializedStubIndices.getInternalBuffer();
      reSerializedIndexByteLength = reSerializedStubIndices.size();
    }

    return new SerializedStubTree(outStub.getInternalBuffer(), outStub.size(), mySerializedTreeHash, null,
                                  reSerializedIndexBytes, reSerializedIndexByteLength, myIndexedStubs);
  }

  public void restoreIndexedStubs(@NotNull StubForwardIndexExternalizer<?> dataExternalizer) throws IOException {
    if (myIndexedStubs == null) {
      myIndexedStubs = dataExternalizer.read(new DataInputStream(new ByteArrayInputStream(myIndexedStubBytes, 0, myIndexedStubByteLength)));
    }
  }

  @NotNull
  Map<StubIndexKey, Map<Object, StubIdList>> getStubIndicesValueMap() {
    return myIndexedStubs;
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
    return retrieveStubFromBytes(serializationManager);
  }

  @NotNull
  Stub retrieveStubFromBytes(@NotNull SerializationManagerEx serializationManager) throws SerializerNotFoundException {
    return serializationManager.deserialize(new UnsyncByteArrayInputStream(myTreeBytes, 0, myTreeByteLength));
  }

  public boolean equals(final Object that) {
    if (this == that) {
      return true;
    }
    if (!(that instanceof SerializedStubTree)) {
      return false;
    }
    final SerializedStubTree thatTree = (SerializedStubTree)that;

    final int length = myTreeByteLength;
    if (length != thatTree.myTreeByteLength) {
      return false;
    }

    final byte[] thisBytes = myTreeBytes;
    final byte[] thatBytes = thatTree.myTreeBytes;
    for (int i = 0; i < length; i++) {
      if (thisBytes[i] != thatBytes[i]) {
        return false;
      }
    }

    return true;
  }

  public int hashCode() {
    if (myTreeBytes == null) {
      return 0;
    }

    int result = 1;
    for (int i = 0; i < myTreeByteLength; i++) {
      result = 31 * result + myTreeBytes[i];
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
    return deserialized + "\n bytes: " + toHexString(myTreeBytes, myTreeByteLength);
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
                                          @NotNull SerializedStubTree existingTree) {
    String oldTreeDump = "\nexisting tree " + existingTree.dumpStub();
    String newTreeDump = "\nnew tree " + newTree.dumpStub();
    byte[] hash = newTree.mySerializedTreeHash;
    LOG.info("Stub tree hashing collision. Different trees have the same hash = " + toHexString(hash, hash.length) +
             ". Hashing algorithm = " + HASHER.getValue().getAlgorithm() + "." + oldTreeDump + newTreeDump, new Exception());
  }

  private static String toHexString(byte[] hash, int length) {
    return IntStreamEx.of(hash).limit(length).mapToObj(b -> String.format("%02x", b & 0xFF)).joining();
  }
}