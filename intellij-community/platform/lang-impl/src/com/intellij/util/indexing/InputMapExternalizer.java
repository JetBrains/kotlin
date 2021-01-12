// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.util.indexing.impl.InputIndexDataExternalizer;
import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public final class InputMapExternalizer<Key, Value> implements DataExternalizer<Map<Key, Value>> {

  private final DataExternalizer<Map<Key, Value>> myMapDataExternalizer;

  public InputMapExternalizer(@NotNull IndexExtension<Key, Value, ?> extension) {
    DataExternalizer<Value> myValueExternalizer = extension.getValueExternalizer();
    DataExternalizer<Collection<Key>> myKeysExternalizer =
      extension instanceof CustomInputsIndexFileBasedIndexExtension
      ? ((CustomInputsIndexFileBasedIndexExtension<Key>)extension).createExternalizer()
      : new InputIndexDataExternalizer<>(extension.getKeyDescriptor(), ((IndexExtension<Key, ?, ?>)extension).getName());
    myMapDataExternalizer = new MapDataExternalizer<>(myValueExternalizer, myKeysExternalizer);
  }

  @Override
  public void save(@NotNull DataOutput out, Map<Key, Value> value) throws IOException {
    myMapDataExternalizer.save(out, value);
  }

  @Override
  public Map<Key, Value> read(@NotNull DataInput in) throws IOException {
    return myMapDataExternalizer.read(in);
  }
}
