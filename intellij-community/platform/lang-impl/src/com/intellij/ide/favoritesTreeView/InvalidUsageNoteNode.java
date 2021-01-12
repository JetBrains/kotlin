/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.favoritesTreeView;

import com.intellij.usages.TextChunk;

import java.util.List;
import java.util.Set;

public class InvalidUsageNoteNode {
  private final PercentDone myPercentDone = PercentDone._0;
  private Set<Flag> myFlags;
  private Set<Concept> myConcepts;

  final List<TextChunk> myText;

  public InvalidUsageNoteNode(List<TextChunk> text) {
    myText = text;
  }

  public PercentDone getPercentDone() {
    return myPercentDone;
  }

  public Set<Flag> getFlags() {
    return myFlags;
  }

  public Set<Concept> getConcepts() {
    return myConcepts;
  }

  public List<TextChunk> getText() {
    return myText;
  }
}
