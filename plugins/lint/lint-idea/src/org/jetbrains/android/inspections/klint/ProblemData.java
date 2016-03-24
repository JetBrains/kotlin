/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.android.inspections.klint;

import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProblemData {
  private final Issue myIssue;
  private final String myMessage;
  private final TextRange myTextRange;
  private final Severity myConfiguredSeverity;

  ProblemData(@NotNull Issue issue, @NotNull String message, @NotNull TextRange textRange, @Nullable Severity configuredSeverity) {
    myIssue = issue;
    myTextRange = textRange;
    myMessage = message;
    myConfiguredSeverity = configuredSeverity;
  }

  @NotNull
  public Issue getIssue() {
    return myIssue;
  }

  @NotNull
  public TextRange getTextRange() {
    return myTextRange;
  }

  @NotNull
  public String getMessage() {
    return myMessage;
  }

  @Nullable
  public Severity getConfiguredSeverity() {
    return myConfiguredSeverity;
  }
}
