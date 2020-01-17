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
 * @author: Eugene Zhuravlev
 */
package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.compiler.ValidityStateFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Collection;

public class FileProcessingCompilerStateCache {
  private static final Logger LOG = Logger.getInstance(FileProcessingCompilerStateCache.class);
  private final StateCache<MyState> myCache;

  public FileProcessingCompilerStateCache(File storeDirectory, final ValidityStateFactory stateFactory) throws IOException {
    myCache = new StateCache<MyState>(new File(storeDirectory, "timestamps")) {
      @Override
      public MyState read(DataInput stream) throws IOException {
        return new MyState(stream.readLong(), stateFactory.createValidityState(stream));
      }

      @Override
      public void write(MyState state, DataOutput out) throws IOException {
        out.writeLong(state.getTimestamp());
        final ValidityState extState = state.getExtState();
        if (extState != null) {
          extState.save(out);
        }
      }
    };
  }

  public void update(VirtualFile sourceFile, ValidityState extState) throws IOException {
    if (sourceFile.isValid()) {
      // only mark as up-to-date if the file did not become invalid during make
      myCache.update(sourceFile.getUrl(), new MyState(sourceFile.getTimeStamp(), extState));
    }
  }

  public void remove(String url) throws IOException {
    myCache.remove(url);
  }

  public long getTimestamp(String url) throws IOException {
    MyState state = myCache.getState(url);
    return (state != null)? state.getTimestamp() : -1L;
  }

  public ValidityState getExtState(String url) throws IOException {
    MyState state = myCache.getState(url);
    return (state != null)? state.getExtState() : null;
  }

  public void force() {
    myCache.force();
  }

  public Collection<String> getUrls() throws IOException {
    return myCache.getUrls();
  }

  public boolean wipe() {
    return myCache.wipe();
  }

  public void close() {
    try {
      myCache.close();
    }
    catch (IOException ignored) {
      LOG.info(ignored);
    }
  }

  private static class MyState implements Serializable {
    private final long myTimestamp;
    private final ValidityState myExtState;

    MyState(long timestamp, @Nullable ValidityState extState) {
      myTimestamp = timestamp;
      myExtState = extState;
    }

    public long getTimestamp() {
      return myTimestamp;
    }

    public @Nullable ValidityState getExtState() {
      return myExtState;
    }
  }

}
