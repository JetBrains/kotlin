/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.compiler.impl;

import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public abstract class StateCache<T> {
  private PersistentHashMap<String, T> myMap;
  private final File myBaseFile;

  public StateCache(@NonNls File storePath) throws IOException {
    myBaseFile = storePath;
    myMap = createMap(storePath);
  }

  protected abstract T read(DataInput stream) throws IOException;

  protected abstract void write(T t, DataOutput out) throws IOException;

  public void force() {
    myMap.force();
  }

  public void close() throws IOException {
    myMap.close();
  }

  public boolean wipe() {
    try {
      myMap.close();
    }
    catch (IOException ignored) {
    }
    PersistentHashMap.deleteFilesStartingWith(myBaseFile);
    try {
      myMap = createMap(myBaseFile);
    }
    catch (IOException ignored) {
      return false;
    }
    return true;
  }

  public void update(@NonNls String url, T state) throws IOException {
    if (state != null) {
      myMap.put(url, state);
    }
    else {
      remove(url);
    }
  }

  public void remove(String url) throws IOException {
    myMap.remove(url);
  }

  public T getState(String url) throws IOException {
    return myMap.get(url);
  }

  public Collection<String> getUrls() throws IOException {
    return myMap.getAllKeysWithExistingMapping();
  }

  public Iterator<String> getUrlsIterator() throws IOException {
    return myMap.getAllKeysWithExistingMapping().iterator();
  }


  private PersistentHashMap<String, T> createMap(final File file) throws IOException {
    return new PersistentHashMap<>(file, EnumeratorStringDescriptor.INSTANCE, new DataExternalizer<T>() {
      @Override
      public void save(@NotNull final DataOutput out, final T value) throws IOException {
        StateCache.this.write(value, out);
      }

      @Override
      public T read(@NotNull final DataInput in) throws IOException {
        return StateCache.this.read(in);
      }
    });
  }

}
