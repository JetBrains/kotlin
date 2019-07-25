/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.build.process.BuildProcessHandler;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.io.StreamUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemProcessHandler extends BuildProcessHandler implements AnsiEscapeDecoder.ColoredTextAcceptor, Disposable {
  private static final Logger LOG = Logger.getInstance(ExternalSystemProcessHandler.class);
  private final String myExecutionName;
  @Nullable
  private ExternalSystemTask myTask;
  private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();
  @Nullable
  private OutputStream myProcessInput;

  public ExternalSystemProcessHandler(@NotNull ExternalSystemTask task, String executionName) {
    myTask = task;
    myExecutionName = executionName;
    if (task instanceof UserDataHolder) {
      try {
        Pipe pipe = Pipe.open();
        InputStream inputStream = new BufferedInputStream(Channels.newInputStream(pipe.source()));
        myProcessInput = new BufferedOutputStream(Channels.newOutputStream(pipe.sink()));
        ((UserDataHolder)task).putUserData(ExternalSystemRunConfiguration.RUN_INPUT_KEY, inputStream);
      }
      catch (IOException e) {
        LOG.warn("Unable to setup process input", e);
      }
    }
  }

  @Override
  public String getExecutionName() {
    return myExecutionName;
  }

  @Override
  public void notifyTextAvailable(@NotNull final String text, @NotNull final Key outputType) {
    myAnsiEscapeDecoder.escapeText(text, outputType, this);
  }

  @Override
  protected void destroyProcessImpl() {
    ExternalSystemTask task = myTask;
    if (task != null) {
      task.cancel();
    }
    closeInput();
  }

  @Override
  protected void detachProcessImpl() {
    notifyProcessDetached();
    closeInput();
  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  @Nullable
  @Override
  public OutputStream getProcessInput() {
    return myProcessInput;
  }

  @Override
  public void notifyProcessTerminated(int exitCode) {
    super.notifyProcessTerminated(exitCode);
    closeInput();
  }

  @Override
  public void coloredTextAvailable(@NotNull String text, @NotNull Key attributes) {
    super.notifyTextAvailable(text, attributes);
  }

  protected void closeInput() {
    ExternalSystemTask task = myTask;
    if (task instanceof UserDataHolder) {
      ((UserDataHolder)task).putUserData(ExternalSystemRunConfiguration.RUN_INPUT_KEY, null);
    }
    StreamUtil.closeStream(myProcessInput);
    myProcessInput = null;
  }

  @Override
  public void dispose() {
    try {
      detachProcessImpl();
    }
    finally {
      myTask = null;
    }
  }
}
