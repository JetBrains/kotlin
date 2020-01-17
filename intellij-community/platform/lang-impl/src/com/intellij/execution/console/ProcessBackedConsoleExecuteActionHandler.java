// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.process.BaseOSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ProcessBackedConsoleExecuteActionHandler extends BaseConsoleExecuteActionHandler implements Condition<LanguageConsoleView> {
  private volatile ProcessHandler myProcessHandler;

  public ProcessBackedConsoleExecuteActionHandler(ProcessHandler processHandler, boolean preserveMarkup) {
    super(preserveMarkup);

    myProcessHandler = processHandler;
  }

  public void setProcessHandler(@NotNull ProcessHandler processHandler) {
    myProcessHandler = processHandler;
  }

  @Override
  protected void execute(@NotNull String text, @NotNull LanguageConsoleView console) {
    processLine(text);
  }

  public void processLine(@NotNull String line) {
    sendText(line + "\n");
  }

  public void sendText(String line) {
    final Charset charset = myProcessHandler instanceof BaseOSProcessHandler ?
                            ((BaseOSProcessHandler)myProcessHandler).getCharset() : null;
    final OutputStream outputStream = myProcessHandler.getProcessInput();
    assert outputStream != null : "output stream is null";
    try {
      byte[] bytes = charset != null ? line.getBytes(charset) : line.getBytes(StandardCharsets.UTF_8);
      outputStream.write(bytes);
      outputStream.flush();
    }
    catch (IOException ignored) {
    }
  }

  public final boolean isProcessTerminated() {
    final ProcessHandler handler = myProcessHandler;
    return handler == null || handler.isProcessTerminated();
  }

  @Override
  public boolean value(LanguageConsoleView console) {
    return !isProcessTerminated();
  }
}