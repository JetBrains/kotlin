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
package com.intellij.compiler.backwardRefs;

import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class OccurrenceCounter<T> {
  private final TObjectIntHashMap<T> myOccurrenceMap;
  private T myBest;
  private int myBestOccurrences;

  OccurrenceCounter() {
    myOccurrenceMap = new TObjectIntHashMap<>();
  }

  void add(@NotNull T element) {
    int prevOccurrences = myOccurrenceMap.get(element);
    if (prevOccurrences == 0) {
      myOccurrenceMap.put(element, 1);
    } else {
      myOccurrenceMap.adjustValue(element, 1);
    }

    if (myBest == null) {
      myBestOccurrences = 1;
      myBest = element;
    } else if (myBest.equals(element)) {
      myBestOccurrences++;
    } else {
      myBestOccurrences = prevOccurrences + 1;
      myBest = element;
    }
  }

  @Nullable
  T getBest() {
    return myBest;
  }

  int getBestOccurrences() {
    return myBestOccurrences;
  }
}
