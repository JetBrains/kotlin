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
package com.intellij.codeInspection.ui;

import org.jetbrains.annotations.NotNull;

public class PreviewEditorFoldingRegion implements Comparable<PreviewEditorFoldingRegion> {
  private final int myStartLine;
  private final int myEndLine;

  public PreviewEditorFoldingRegion(int startLine, int endLine) {
    myStartLine = startLine;
    myEndLine = endLine;
  }

  public int getStartLine() {
    return myStartLine;
  }

  public int getEndLine() {
    return myEndLine;
  }

  public boolean contain(int position) {
    return myStartLine <= position && myEndLine > position;
  }

  @Override
  public int compareTo(@NotNull PreviewEditorFoldingRegion o) {
    return Integer.compare(myStartLine, o.myStartLine);
  }
}
