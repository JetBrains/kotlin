// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.util.indexing.ValueContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MergedValueContainer<Value> extends ValueContainer<Value> {
  private final ValueContainer<Value> myContainer1;
  private final ValueContainer<Value> myContainer2;

  @NotNull
  public static <Value> ValueContainer<Value> merge(@NotNull ValueContainer<Value> container1, @NotNull ValueContainer<Value> container2) {
    if (container1.size() == 0) return container2;
    if (container2.size() == 0) return container1;
    return new MergedValueContainer<>(container1, container2);
  }

  private MergedValueContainer(@NotNull ValueContainer<Value> container1, @NotNull ValueContainer<Value> container2) {
    myContainer1 = container1;
    myContainer2 = container2;
  }

  @NotNull
  @Override
  public ValueIterator<Value> getValueIterator() {
    return new ValueIterator<Value>() {
      boolean mySecondIsUsed;
      ValueIterator<Value> myCurrent = myContainer1.getValueIterator();

      @NotNull
      @Override
      public IntIterator getInputIdsIterator() {
        return myCurrent.getInputIdsIterator();
      }

      @Nullable
      @Override
      public IntPredicate getValueAssociationPredicate() {
        return myCurrent.getValueAssociationPredicate();
      }

      @Override
      public boolean hasNext() {
        if (myCurrent.hasNext()) return true;
        if (!mySecondIsUsed) {
          myCurrent = myContainer2.getValueIterator();
          mySecondIsUsed = true;
          return hasNext();
        }
        return false;
      }

      @Override
      public Value next() {
        return myCurrent.next();
      }
    };
  }

  @Override
  public int size() {
    return myContainer1.size() + myContainer2.size();
  }
}
