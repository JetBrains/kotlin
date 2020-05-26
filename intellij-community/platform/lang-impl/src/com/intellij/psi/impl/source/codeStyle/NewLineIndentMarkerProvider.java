// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.lang.LanguageExtension;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows providing a dummy identifier to be used for preserving spaces in live templates. For more details on its purpose,
 * please see {@link CodeStyleManagerImpl#insertNewLineIndentMarker(com.intellij.psi.PsiFile, com.intellij.openapi.editor.Document, int)}.
 * The default logic is to create a line or block comment with the language's {@link com.intellij.lang.Commenter commenter} and use it as a marker, but
 * in some cases the commenting logic is too complicated and is unsuitable.
 */
public interface NewLineIndentMarkerProvider {
  LanguageExtension<NewLineIndentMarkerProvider> EP = new LanguageExtension<>("com.intellij.lang.formatter.newLineIndentMarkerProvider");

  /**
   * @return marker to be inserted into the document of the {@code file} at the {@code offset} in order to preserve whitespace during formatting. The identifier should not
   * change the semantics of the code (e.g., by opening a block element). Falls back to the default behavior if {@code null} is returned.
   */
  @Nullable String createMarker(@NotNull PsiFile file, int offset);
}
