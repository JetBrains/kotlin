// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.util.SmartList;
import com.intellij.util.indexing.impl.InputIndexDataExternalizer;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InputMapExternalizer<Key, Value> implements DataExternalizer<Map<Key, Value>> {
  private final DataExternalizer<Value> myValueExternalizer;
  private final DataExternalizer<Collection<Key>> mySnapshotIndexExternalizer;

  public InputMapExternalizer(IndexExtension<Key, Value, ?> extension) {
    myValueExternalizer = extension.getValueExternalizer();
    mySnapshotIndexExternalizer = extension instanceof CustomInputsIndexFileBasedIndexExtension
                                  ? ((CustomInputsIndexFileBasedIndexExtension<Key>)extension).createExternalizer()
                                  : new InputIndexDataExternalizer<>(extension.getKeyDescriptor(), ((IndexExtension<Key, ?, ?>)extension).getName());
  }


  @NotNull
  public DataExternalizer<Value> getValueExternalizer() {
    return myValueExternalizer;
  }

  @Override
  public void save(@NotNull DataOutput stream, Map<Key, Value> data) throws IOException {
    int size = data.size();
    DataInputOutputUtil.writeINT(stream, size);

    if (size > 0) {
      THashMap<Value, List<Key>> values = new THashMap<>();
      List<Key> keysForNullValue = null;
      for (Map.Entry<Key, Value> e : data.entrySet()) {
        Value value = e.getValue();

        List<Key> keys = value != null ? values.get(value):keysForNullValue;
        if (keys == null) {
          if (value != null) values.put(value, keys = new SmartList<>());
          else keys = keysForNullValue = new SmartList<>();
        }
        keys.add(e.getKey());
      }

      if (keysForNullValue != null) {
        myValueExternalizer.save(stream, null);
        mySnapshotIndexExternalizer.save(stream, keysForNullValue);
      }

      for(Value value:values.keySet()) {
        myValueExternalizer.save(stream, value);
        mySnapshotIndexExternalizer.save(stream, values.get(value));
      }
    }
  }

  @Override
  public Map<Key, Value> read(@NotNull DataInput in) throws IOException {
    int pairs = DataInputOutputUtil.readINT(in);
    if (pairs == 0) return Collections.emptyMap();
    Map<Key, Value> result = new THashMap<>(pairs);
    while (((InputStream)in).available() > 0) {
      Value value = myValueExternalizer.read(in);
      Collection<Key> keys = mySnapshotIndexExternalizer.read(in);
      for(Key k:keys) result.put(k, value);
    }
    return result;
  }
}
