// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public interface CompilerReferenceReaderFactory<Reader extends CompilerReferenceReader<?>> {
  int expectedIndexVersion();

  @Nullable
  Reader create(Project project);
}
