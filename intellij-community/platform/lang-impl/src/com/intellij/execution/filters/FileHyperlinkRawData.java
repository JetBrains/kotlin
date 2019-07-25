/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.filters;

import org.jetbrains.annotations.NotNull;

public class FileHyperlinkRawData {

  private final String myFilePath;
  private final int myDocumentLine;
  private final int myDocumentColumn;
  private final int myHyperlinkStartInd;
  private final int myHyperlinkEndInd;

  public FileHyperlinkRawData(@NotNull String filePath,
                              int documentLine,
                              int documentColumn,
                              int hyperlinkStartInd,
                              int hyperlinkEndInd) {
    myFilePath = filePath;
    myDocumentLine = documentLine;
    myDocumentColumn = documentColumn;
    myHyperlinkStartInd = hyperlinkStartInd;
    myHyperlinkEndInd = hyperlinkEndInd;
  }

  @NotNull
  public String getFilePath() {
    return myFilePath;
  }

  public int getDocumentLine() {
    return myDocumentLine;
  }

  public int getDocumentColumn() {
    return myDocumentColumn;
  }

  public int getHyperlinkStartInd() {
    return myHyperlinkStartInd;
  }

  public int getHyperlinkEndInd() {
    return myHyperlinkEndInd;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FileHyperlinkRawData data = (FileHyperlinkRawData)o;

    return myDocumentLine == data.myDocumentLine &&
           myDocumentColumn == data.myDocumentColumn &&
           myHyperlinkStartInd == data.myHyperlinkStartInd &&
           myHyperlinkEndInd == data.myHyperlinkEndInd &&
           myFilePath.equals(data.myFilePath);
  }

  @Override
  public int hashCode() {
    int result = myFilePath.hashCode();
    result = 31 * result + myDocumentLine;
    result = 31 * result + myDocumentColumn;
    result = 31 * result + myHyperlinkStartInd;
    result = 31 * result + myHyperlinkEndInd;
    return result;
  }

  @Override
  public String toString() {
    return "filePath=" + myFilePath +
           ", line=" + myDocumentLine +
           ", column=" + myDocumentColumn +
           ", hyperlinkStartOffset=" + myHyperlinkStartInd +
           ", hyperlinkEndOffset=" + myHyperlinkEndInd;
  }
}
