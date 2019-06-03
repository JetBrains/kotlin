// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events.impl;

import com.intellij.build.events.DerivedResult;
import com.intellij.build.events.FailureResult;
import com.intellij.build.events.SuccessResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class DerivedResultImpl implements DerivedResult {

  @NotNull private final Supplier<SuccessResult> mySuccess;
  @NotNull private final Supplier<FailureResult> myFail;

  public DerivedResultImpl() {
    this(null, null);
  }

  public DerivedResultImpl(@Nullable Supplier<SuccessResult> onSuccess, @Nullable Supplier<FailureResult> onFail) {
    mySuccess = onSuccess != null ? onSuccess : SuccessResultImpl::new;
    myFail = onFail != null ? onFail : FailureResultImpl::new;
  }

  @NotNull
  @Override
  public FailureResult createFailureResult() {
    FailureResult result = myFail.get();
    if (result == null) {
      return new FailureResultImpl();
    }
    return result;
  }

  @NotNull
  @Override
  public SuccessResult createSuccessResult() {
    SuccessResult result = mySuccess.get();
    if (result == null) {
      return new SuccessResultImpl();
    }
    return result;
  }
}
