/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.psi.impl.source.resolve.reference.impl.manipulators;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiPlainTextFile;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PlainFileManipulator extends AbstractElementManipulator<PsiPlainTextFile> {
  @Override
  public PsiPlainTextFile handleContentChange(@NotNull PsiPlainTextFile file, @NotNull TextRange range, String newContent)
  throws IncorrectOperationException {
    final Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
    document.replaceString(range.getStartOffset(), range.getEndOffset(), newContent);
    PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);

    return file;
  }
}
