/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.execution.console;

import com.intellij.execution.process.BaseOSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * @author traff
 */
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
      byte[] bytes = charset != null ? line.getBytes(charset) : line.getBytes();
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