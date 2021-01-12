// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class OutputWrapper extends OutputStream {

  @NotNull private final ExternalSystemTaskNotificationListener myListener;
  @NotNull private final ExternalSystemTaskId myTaskId;
  @Nullable private StringBuilder myBuffer;
  private final boolean myStdOut;

  public OutputWrapper(@NotNull ExternalSystemTaskNotificationListener listener, @NotNull ExternalSystemTaskId taskId, boolean stdOut) {
    myListener = listener;
    myTaskId = taskId;
    myStdOut = stdOut;
  }

  @Override
  public void write(int b) {
    if (myBuffer == null) {
      myBuffer = new StringBuilder();
    }
    myBuffer.append((char)b);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    if (myBuffer == null) {
      myBuffer = new StringBuilder();
    }
    myBuffer.append(new String(b, off, len, StandardCharsets.UTF_8));
  }

  @Override
  public void flush() {
    doFlush();
  }

  private void doFlush() {
    if (myBuffer == null) {
      return;
    }
    myListener.onTaskOutput(myTaskId, myBuffer.toString(), myStdOut);
    myBuffer.setLength(0);
  }
}
