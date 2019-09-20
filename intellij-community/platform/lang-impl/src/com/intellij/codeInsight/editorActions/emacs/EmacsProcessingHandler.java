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
package com.intellij.codeInsight.editorActions.emacs;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * This interface is assumed to define general contract for Emacs-like functionality.
 * 
 * @author Denis Zhdanov
 */
public interface EmacsProcessingHandler {

  /**
   * Enumerates possible processing results.
   */
  enum Result {
    /**
     * Proceed to the next handler in a chain.
     */
    CONTINUE,

    /**
     * Stop current processing as everything is done by the current handler
     */
    STOP
  }

  /**
   * Emacs handles {@code Tab} pressing as
   * <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Basic-Indent.html#Basic-Indent">'auto indent line'</a>
   * most of the time. However, there are extensions to this like <a href="https://launchpad.net/python-mode">python-mode</a>
   * that changes indentation level of the current line (makes it belong to the other code block).
   * <p/>
   * So, current method may be implemented by changing code block for the active line by changing its indentation.
   * {@link Result#STOP} should be returned then.
   * 
   * @param project     current project
   * @param editor      current editor
   * @param file        current file
   * @return            processing result
   */
  @NotNull
  Result changeIndent(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file);
}
