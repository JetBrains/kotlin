// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import com.intellij.util.Function;

public class HashCodeAggregateFunction<Param> implements AggregateFunction<Integer, Param> {
  private int myResult;
  private final Function<? super Param, ?> myMapper;

  public HashCodeAggregateFunction(Function<? super Param, ?> mapper) {
    myMapper = mapper;
  }

  @Override
  public void consume(Param item) {
    myResult = 31 * myResult + (item == null ? 0 : myMapper.fun(item).hashCode());
  }

  @Override
  public Integer get() {
    return myResult;
  }
}
