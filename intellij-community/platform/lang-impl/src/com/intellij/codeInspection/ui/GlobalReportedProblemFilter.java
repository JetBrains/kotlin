// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.reference.RefEntity;

public interface GlobalReportedProblemFilter {
  boolean shouldReportProblem(RefEntity refElement, String toolName);
}
