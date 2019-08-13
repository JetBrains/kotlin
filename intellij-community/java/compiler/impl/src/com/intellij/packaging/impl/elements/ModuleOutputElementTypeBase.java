// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.compiler.CompilerBundle;
import org.jetbrains.annotations.NotNull;

public abstract class ModuleOutputElementTypeBase<E extends ModulePackagingElementBase> extends ModuleElementTypeBase<E> {
  public ModuleOutputElementTypeBase(String id, String presentableName) {
    super(id, presentableName);
  }

  @NotNull
  @Override
  public String getElementText(@NotNull String moduleName) {
    return CompilerBundle.message("node.text.0.compile.output", moduleName);
  }
}
