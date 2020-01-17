// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class OutputListener extends ProcessAdapter {
  private final StringBuilder out;
  private final StringBuilder err;
  private int myExitCode;

  public OutputListener() {
    out = new StringBuilder();
    err = new StringBuilder();
  }

  public OutputListener(@NotNull final StringBuilder out, @NotNull final StringBuilder err) {
    this.out = out;
    this.err = err;
  }

  @Override
  public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
    if (outputType == ProcessOutputTypes.STDERR) {
      err.append(event.getText());
    }
    else if (outputType == ProcessOutputTypes.SYSTEM) {
      // skip
    }
    else {
      out.append(event.getText());
    }
  }

  @Override
  public void processTerminated(@NotNull ProcessEvent event) {
    myExitCode = event.getExitCode();
  }

  public Output getOutput() {
    return new Output(out.toString(), err.toString(), myExitCode);
  }
}
