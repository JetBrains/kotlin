// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.compiler;

import org.jetbrains.annotations.NotNull;

/**
 * @deprecated use {@link JavaCompilerBundle} instead
 */
@Deprecated
public class CompilerBundle {
  @NotNull
  public static String message(@NotNull String key, Object @NotNull ... params) {
    return JavaCompilerBundle.message(key, params);
  }
}
