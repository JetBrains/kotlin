// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public class ConsoleViewRunningState extends ConsoleState {
  private final ConsoleViewImpl myConsole;
  private final ProcessHandler myProcessHandler;
  private final ConsoleState myFinishedStated;
  private final Writer myUserInputWriter;
  private final ProcessStreamsSynchronizer myStreamsSynchronizer;

  private final ProcessAdapter myProcessListener = new ProcessAdapter() {
    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      if (outputType instanceof ProcessOutputType) {
        myStreamsSynchronizer.doWhenStreamsSynchronized(event.getText(), (ProcessOutputType)outputType, () -> {
          print(event.getText(), outputType);
        });
      }
      else {
        print(event.getText(), outputType);
      }
    }
  };

  public ConsoleViewRunningState(final ConsoleViewImpl console,
                                 final ProcessHandler processHandler,
                                 final ConsoleState finishedStated,
                                 final boolean attachToStdOut,
                                 final boolean attachToStdIn) {

    myConsole = console;
    myProcessHandler = processHandler;
    myFinishedStated = finishedStated;
    myStreamsSynchronizer = new ProcessStreamsSynchronizer(console);

    // attach to process stdout
    if (attachToStdOut) {
      processHandler.addProcessListener(myProcessListener);
    }

    // attach to process stdin
    if (attachToStdIn) {
      final OutputStream processInput = myProcessHandler.getProcessInput();
      myUserInputWriter = processInput == null ? null : createOutputStreamWriter(processInput, processHandler);
    }
    else {
      myUserInputWriter = null;
    }
  }

  private static OutputStreamWriter createOutputStreamWriter(OutputStream processInput, ProcessHandler processHandler) {
    Charset charset = null;
    if (processHandler instanceof OSProcessHandler) {
      charset = ((OSProcessHandler)processHandler).getCharset();
    }
    if (charset == null) {
      charset = EncodingManager.getInstance().getDefaultCharset();
    }
    return new OutputStreamWriter(processInput, charset);
  }

  private void print(@NotNull String text, @NotNull Key<?> outputType) {
    myConsole.print(text, ConsoleViewContentType.getConsoleViewType(outputType));
  }

  @Override
  @NotNull
  public ConsoleState dispose() {
    if (myProcessHandler != null) {
      myProcessHandler.removeProcessListener(myProcessListener);
    }
    return myFinishedStated;
  }

  @Override
  public boolean isCommandLine(@NotNull String line) {
    return myProcessHandler instanceof BaseProcessHandler && line.equals(((BaseProcessHandler)myProcessHandler).getCommandLine());
  }

  @Override
  public boolean isFinished() {
    return myProcessHandler == null || myProcessHandler.isProcessTerminated();
  }

  @Override
  public boolean isRunning() {
    return myProcessHandler != null && !myProcessHandler.isProcessTerminated();
  }

  @Override
  public void sendUserInput(@NotNull final String input) throws IOException {
    if (myUserInputWriter == null) {
      throw new IOException(ExecutionBundle.message("no.user.process.input.error.message"));
    }
    myUserInputWriter.write(input);
    myUserInputWriter.flush();
  }

  @NotNull
  @Override
  public ConsoleState attachTo(@NotNull final ConsoleViewImpl console, final ProcessHandler processHandler) {
    return dispose().attachTo(console, processHandler);
  }

  @TestOnly
  @NotNull
  ProcessStreamsSynchronizer getStreamsSynchronizer() {
    return myStreamsSynchronizer;
  }

  @Override
  public String toString() {
    return "Running state";
  }
}