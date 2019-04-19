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

import com.intellij.util.CompressionUtil;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.PersistentHashMapValueStorage;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

public class SerializedStubTree {
  private final byte[] myBytes;
  private final int myLength;
  private final long myByteContentLength;
  private final int myCharContentLength;
  private Stub myStubElement;
  private IndexedStubs myIndexedStubs;

  public SerializedStubTree(final byte[] bytes, int length, @Nullable Stub stubElement, long byteContentLength, int charContentLength) {
    myBytes = bytes;
    myLength = length;
    myByteContentLength = byteContentLength;
    myCharContentLength = charContentLength;
    myStubElement = stubElement;
  }

  public SerializedStubTree(DataInput in) throws IOException {
    if (PersistentHashMapValueStorage.COMPRESSION_ENABLED) {
      int serializedStubsLength = DataInputOutputUtil.readINT(in);
      byte[] bytes = new byte[serializedStubsLength];
      in.readFully(bytes);
      myBytes = bytes;
      myLength = myBytes.length;
      myByteContentLength = DataInputOutputUtil.readLONG(in);
      myCharContentLength = DataInputOutputUtil.readINT(in);
    }
    else {
      myBytes = CompressionUtil.readCompressed(in);
      myLength = myBytes.length;
      myByteContentLength = in.readLong();
      myCharContentLength = in.readInt();
    }
  }

  public void write(DataOutput out) throws IOException {
    if (PersistentHashMapValueStorage.COMPRESSION_ENABLED) {
      DataInputOutputUtil.writeINT(out, myLength);
      out.write(myBytes, 0, myLength);
      DataInputOutputUtil.writeLONG(out, myByteContentLength);
      DataInputOutputUtil.writeINT(out, myCharContentLength);
    }
    else {
      CompressionUtil.writeCompressed(out, myBytes, 0, myLength);
      out.writeLong(myByteContentLength);
      out.writeInt(myCharContentLength);
    }
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

  void indexTree(int fileId) throws SerializerNotFoundException {
    ObjectStubBase root = (ObjectStubBase)getStub(true);
    ObjectStubTree objectStubTree = root instanceof PsiFileStub ? new StubTree((PsiFileStub)root, false) :
                                    new ObjectStubTree(root, false);
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

    myIndexedStubs = new IndexedStubs(fileId, (Map)map);
  }

  @NotNull
  IndexedStubs getIndexedStubs() {
    return myIndexedStubs;
  }

  public boolean contentLengthMatches(long byteContentLength, int charContentLength) {
    if (myCharContentLength >= 0 && charContentLength >= 0) {
      return myCharContentLength == charContentLength;
    }
    return myByteContentLength == byteContentLength;
  }

  String dumpLengths() {
    return "{chars=" + myCharContentLength + ", bytes=" + myByteContentLength + "}";
  }

  public boolean equals(final Object that) {
    if (this == that) {
      return true;
    }
    if (!(that instanceof SerializedStubTree)) {
      return false;
    }
    final SerializedStubTree thatTree = (SerializedStubTree)that;
    
    if (myCharContentLength != thatTree.myCharContentLength ||
        myByteContentLength != thatTree.myByteContentLength
    ) {
      return false;
    }
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

  public long getByteContentLength() {
    return myByteContentLength;
  }

  public int getCharContentLength() {
    return myCharContentLength;
  }
}