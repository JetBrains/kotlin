/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.build.events.impl;

import com.intellij.build.events.SuccessResult;
import com.intellij.build.events.Warning;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class SuccessResultImpl implements SuccessResult {

  private final boolean myUpToDate;

  public SuccessResultImpl() {
    this(false);
  }

  public SuccessResultImpl(boolean isUpToDate) {
    myUpToDate = isUpToDate;
  }

  @Override
  public boolean isUpToDate() {
    return myUpToDate;
  }

  @Override
  public List<? extends Warning> getWarnings() {
    return Collections.emptyList();
  }
}
