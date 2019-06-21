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

package com.intellij.ide.highlighter.custom.impl;

import com.intellij.codeInsight.editorActions.TypedHandler;
import com.intellij.ide.highlighter.FileTypeRegistrator;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.lang.Commenter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import org.jetbrains.annotations.NotNull;

public class StandardFileTypeRegistrator implements FileTypeRegistrator {
  @Override
  public void initFileType(@NotNull final FileType fileType) {
    if (fileType instanceof AbstractFileType) {
      init(((AbstractFileType)fileType));
    }
  }

  private static void init(final AbstractFileType abstractFileType) {
    SyntaxTable table = abstractFileType.getSyntaxTable();

    if (!isEmpty(table.getStartComment()) && !isEmpty(table.getEndComment()) ||
        !isEmpty(table.getLineComment())) {
      abstractFileType.setCommenter(new MyCommenter(abstractFileType));
    }

    TypedHandler.registerQuoteHandler(abstractFileType, new CustomFileTypeQuoteHandler());

  }

  private static class MyCommenter implements Commenter {
    private final CustomSyntaxTableFileType myAbstractFileType;

    MyCommenter(final CustomSyntaxTableFileType abstractFileType) {

      myAbstractFileType = abstractFileType;
    }

    @Override
    public String getLineCommentPrefix() {
      return myAbstractFileType.getSyntaxTable().getLineComment();
    }

    @Override
    public String getBlockCommentPrefix() {
      return myAbstractFileType.getSyntaxTable().getStartComment();
    }

    @Override
    public String getBlockCommentSuffix() {
      return myAbstractFileType.getSyntaxTable().getEndComment();
    }

    @Override
    public String getCommentedBlockCommentPrefix() {
      return null;
    }

    @Override
    public String getCommentedBlockCommentSuffix() {
      return null;
    }
  }

  private static boolean isEmpty(String str) {
    return str==null || str.length() == 0;
  }

}
