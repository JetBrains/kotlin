// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeEditor.printing;

import com.intellij.openapi.editor.EditorBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated use {@link EditorBundle} instead
 */
@Deprecated
public final class CodeEditorBundle {
  @NotNull
  public static String message(@NotNull String key, Object @NotNull ... params) {
    return EditorBundle.message(key, params);
  }
}
