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
package com.intellij.compiler;

import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.util.TripleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CompilerMessageImpl implements CompilerMessage {

  private final Project myProject;
  private final CompilerMessageCategory myCategory;
  @Nullable private Navigatable myNavigatable;
  private final String myMessage;
  private final VirtualFile myFile;
  private final int myRow;
  private final int myColumn;
  @NotNull
  private TripleFunction<? super CompilerMessage, ? super Integer, ? super Integer, Integer> myColumnAdjuster = (msg, line, col) -> col;

  public CompilerMessageImpl(Project project, CompilerMessageCategory category, String message) {
    this(project, category, message, null, -1, -1, null);
  }

  public CompilerMessageImpl(Project project,
                             @NotNull CompilerMessageCategory category,
                             String message,
                             @Nullable final VirtualFile file,
                             int row,
                             int column,
                             @Nullable final Navigatable navigatable) {
    myProject = project;
    myCategory = category;
    myNavigatable = navigatable;
    myMessage = message == null ? "" : message;
    myRow = row;
    myColumn = column;
    myFile = file;
  }

  public void setColumnAdjuster(@NotNull TripleFunction<? super CompilerMessage, ? super Integer, ? super Integer, Integer> columnAdjuster) {
    myColumnAdjuster = columnAdjuster;
  }

  @NotNull
  @Override
  public CompilerMessageCategory getCategory() {
    return myCategory;
  }

  @Override
  public String getMessage() {
    return myMessage;
  }

  @Override
  public Navigatable getNavigatable() {
    if (myNavigatable != null) {
      return myNavigatable;
    }
    final VirtualFile virtualFile = getVirtualFile();
    if (virtualFile != null && virtualFile.isValid() && !virtualFile.getFileType().isBinary()) {
      final int line = getLine() - 1; // editor lines are zero-based
      if (line >= 0) {
        return myNavigatable = new OpenFileDescriptor(myProject, virtualFile, line, myColumnAdjuster.fun(this, line, Math.max(0, getColumn()-1))) ;
      }
    }
    return null;
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myFile;
  }

  @Override
  public String getExportTextPrefix() {
    if (getLine() >= 0) {
      return CompilerBundle.message("compiler.results.export.text.prefix", getLine());
    }
    return "";
  }

  @Override
  public String getRenderTextPrefix() {
    if (getLine() >= 0) {
      return "(" + getLine() + ", " + getColumn() + ")";
    }
    return "";
  }

  public int getLine() {
    return myRow;
  }

  public int getColumn() {
    return myColumn;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CompilerMessage)) return false;

    final CompilerMessageImpl compilerMessage = (CompilerMessageImpl)o;

    if (myColumn != compilerMessage.myColumn) return false;
    if (myRow != compilerMessage.myRow) return false;
    if (!myCategory.equals(compilerMessage.myCategory)) return false;
    if (myFile != null ? !myFile.equals(compilerMessage.myFile) : compilerMessage.myFile != null) return false;
    if (!myMessage.equals(compilerMessage.myMessage)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myCategory.hashCode();
    result = 29 * result + myMessage.hashCode();
    result = 29 * result + (myFile != null ? myFile.hashCode() : 0);
    result = 29 * result + myRow;
    result = 29 * result + myColumn;
    return result;
  }

  public String toString() {
    return myMessage;
  }
}
