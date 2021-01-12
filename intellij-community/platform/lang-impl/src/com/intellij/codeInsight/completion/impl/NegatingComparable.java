/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion.impl;

import com.intellij.psi.ForceableComparable;
import org.jetbrains.annotations.NotNull;

/**
* @author peter
*/
public class NegatingComparable<T extends NegatingComparable<T>> implements Comparable<T>, ForceableComparable {
  private final Comparable myWeigh;

  public NegatingComparable(Comparable weigh) {
    myWeigh = weigh;
  }

  @Override
  public void force() {
    if (myWeigh instanceof ForceableComparable) {
      ((ForceableComparable)myWeigh).force();
    }
  }

  @Override
  public int compareTo(@NotNull T o) {
    final Comparable w1 = myWeigh;
    final Comparable w2 = ((NegatingComparable)o).myWeigh;
    if (w1 == null && w2 == null) return 0;
    if (w1 == null) return 1;
    if (w2 == null) return -1;

    return -w1.compareTo(w2);
  }

  @Override
  public String toString() {
    return String.valueOf(myWeigh);
  }
}
