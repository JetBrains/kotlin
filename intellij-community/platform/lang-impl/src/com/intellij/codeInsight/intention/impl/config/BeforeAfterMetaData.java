// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import org.jetbrains.annotations.NotNull;

public interface BeforeAfterMetaData {

  @NotNull
  TextDescriptor[] getExampleUsagesBefore();
  
  @NotNull
  TextDescriptor[] getExampleUsagesAfter();
  
  @NotNull
  TextDescriptor getDescription();
}
