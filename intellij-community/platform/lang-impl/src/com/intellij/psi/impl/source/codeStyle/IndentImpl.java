/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.psi.impl.source.codeStyle;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.Indent;

public class IndentImpl implements Indent{
  private final CodeStyleSettings mySettings;
  private final int myIndentLevel;
  private final int mySpaceCount;
  private final FileType myFileType;

  public IndentImpl(CodeStyleSettings settings, int indentLevel, int spaceCount, FileType fileType) {
    mySettings = settings;
    myIndentLevel = indentLevel;
    mySpaceCount = spaceCount;
    myFileType = fileType;
  }

  int getIndentLevel() {
    return myIndentLevel;
  }

  int getSpaceCount() {
    return mySpaceCount;
  }

  public boolean equals(Object o) {
    if (!(o instanceof IndentImpl)) return false;

    IndentImpl indent = (IndentImpl)o;

    if (myIndentLevel != indent.myIndentLevel) return false;
    if (mySpaceCount != indent.mySpaceCount) return false;
    if (!mySettings.equals(indent.mySettings)) return false;

    return true;
  }

  public int hashCode() {
    return myIndentLevel + mySpaceCount;
  }

  @Override
  public boolean isGreaterThan(Indent indent) {
    return getSize() > ((IndentImpl)indent).getSize();
  }

  @Override
  public Indent min(Indent anotherIndent) {
    return isGreaterThan(anotherIndent) ? anotherIndent : this;
  }

  @Override
  public Indent max(Indent anotherIndent) {
    return isGreaterThan(anotherIndent) ? this : anotherIndent;
  }

  @Override
  public Indent add(Indent indent) {
    IndentImpl indent1 = (IndentImpl)indent;
    return new IndentImpl(mySettings, myIndentLevel + indent1.myIndentLevel, mySpaceCount + indent1.mySpaceCount, myFileType);
  }

  @Override
  public Indent subtract(Indent indent) {
    IndentImpl indent1 = (IndentImpl)indent;
    return new IndentImpl(mySettings, myIndentLevel - indent1.myIndentLevel, mySpaceCount - indent1.mySpaceCount, myFileType);
  }

  @Override
  public boolean isZero() {
    return myIndentLevel == 0 && mySpaceCount == 0;
  }

  private int getSize(){
    return myIndentLevel * mySettings.getIndentSize(myFileType) + mySpaceCount;
  }
}
