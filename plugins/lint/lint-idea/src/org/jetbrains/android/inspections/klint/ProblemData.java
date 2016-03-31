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
