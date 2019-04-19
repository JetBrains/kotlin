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
package com.intellij.compiler.chainsSearch;

import org.jetbrains.annotations.NotNull;

public class ChainOpAndOccurrences<T extends RefChainOperation> implements Comparable<ChainOpAndOccurrences> {
  private final T myOp;
  private final int myOccurrences;

  public ChainOpAndOccurrences(final T op, final int occurrences) {
    myOp = op;
    myOccurrences = occurrences;
  }

  public T getOperation() {
    return myOp;
  }

  public int getOccurrenceCount() {
    return myOccurrences;
  }

  @Override
  public int compareTo(@NotNull final ChainOpAndOccurrences that) {
    final int sub = -getOccurrenceCount() + that.getOccurrenceCount();
    if (sub != 0) {
      return sub;
    }
    return myOp.hashCode() - that.myOp.hashCode();
  }

  @Override
  public String toString() {
    return getOccurrenceCount() + " for " + myOp;
  }
}
