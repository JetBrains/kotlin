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

package com.intellij.codeInsight.editorActions.enter;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public interface EnterHandlerDelegate {
  ExtensionPointName<EnterHandlerDelegate> EP_NAME = ExtensionPointName.create("com.intellij.enterHandlerDelegate");

  enum Result {
    Default, Continue, DefaultForceIndent, DefaultSkipIndent, Stop
  }

  /**
   * Called before the actual Enter processing is done.
   * <b>Important Note: A document associated with the editor may have modifications which are not reflected yet in the PSI file. If any
   * operations with PSI are needed including a search for PSI elements, the document must be committed first to update the PSI.
   * For example:</b>
   * <code><pre>
   *   PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument);
   * </pre></code>
   *
   * @param file            The PSI file associated with the document.
   * @param editor          The editor.
   * @param caretOffset     Indicates a place where line break is to be inserted (it's a caret position initially). Method implementation
   *                        can change this value to adjust target line break position.
   * @param caretAdvance    A reference to the number of columns by which the caret must be moved forward.
   * @param dataContext     The data context passed to the enter handler.
   * @param originalHandler The original handler.
   * @return One of <code>{@link Result} values.</code>
   */
  Result preprocessEnter(@NotNull final PsiFile file, @NotNull final Editor editor, @NotNull final Ref<Integer> caretOffset,
                         @NotNull final Ref<Integer> caretAdvance, @NotNull final DataContext dataContext,
                         @Nullable final EditorActionHandler originalHandler);

  /**
   * Called at the end of Enter handling after line feed insertion and indentation adjustment.
   * <p>
   * <b>Important Note: A document associated with the editor has modifications which are not reflected yet in the PSI file. If any
   * operations with PSI are needed including a search for PSI elements, the document must be committed first to update the PSI.
   * For example:</b>
   * <code><pre>
   *   PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument);
   * </pre></code>
   *
   * @param file        The PSI file associated with the document.
   * @param editor      The editor.
   * @param dataContext The data context passed to the Enter handler.
   * @return One of <code>{@link Result} values.</code>
   * @see DataContext
   * @see com.intellij.psi.PsiDocumentManager
   */
  Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext);
}
