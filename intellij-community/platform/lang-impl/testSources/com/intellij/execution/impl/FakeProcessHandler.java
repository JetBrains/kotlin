/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.impl;

import com.intellij.execution.KillableProcess;
import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class FakeProcessHandler extends ProcessHandler implements KillableProcess {

  private final boolean mySurviveSoftKill;

  public FakeProcessHandler(boolean surviveSoftKill) {
    mySurviveSoftKill = surviveSoftKill;
  }

  @Override
  protected void destroyProcessImpl() {
    if (!mySurviveSoftKill) {
      notifyProcessTerminated(0);
    }
  }

  @Override
  protected void detachProcessImpl() {
    notifyProcessTerminated(0);
  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  @Nullable
  @Override
  public OutputStream getProcessInput() {
    return null;
  }

  @Override
  public boolean canKillProcess() {
    return true;
  }

  @Override
  public void killProcess() {
    notifyProcessTerminated(0);
  }
}
