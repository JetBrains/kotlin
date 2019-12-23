/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.util.gotoByName;

import org.jetbrains.annotations.NotNull;

public class MatchResult implements Comparable<MatchResult> {
  @NotNull
  public final String elementName;
  public final int matchingDegree;
  private final boolean startMatch;

  public MatchResult(@NotNull String elementName, int matchingDegree, boolean startMatch) {
    this.elementName = elementName;
    this.matchingDegree = matchingDegree;
    this.startMatch = startMatch;
  }

  public int compareDegrees(@NotNull MatchResult that) {
    return Integer.compare(that.matchingDegree, matchingDegree);
  }

  @Override
  public int compareTo(@NotNull MatchResult that) {
    int result = compareDegrees(that);
    return result != 0 ? result : elementName.compareToIgnoreCase(that.elementName);
  }

  @Override
  public String toString() {
    return "MatchResult{" +
           "'" + elementName + '\'' +
           ", degree=" + matchingDegree +
           ", start=" + startMatch +
           '}';
  }
}
