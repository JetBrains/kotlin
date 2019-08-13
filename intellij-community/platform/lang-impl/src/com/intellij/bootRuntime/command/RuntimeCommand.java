// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.command;

import com.intellij.bootRuntime.Controller;
import com.intellij.bootRuntime.bundles.Runtime;
import com.intellij.openapi.project.Project;

public abstract class RuntimeCommand extends Command {
  protected final Runtime myRuntime;

  RuntimeCommand(Project project, Controller controller, String name, Runtime runtime) {
    super(project, controller, name);
    myRuntime = runtime;
  }

  protected Runtime getRuntime() {
    return myRuntime;
  }

  @Override
  protected void handleFinished() {
    myController.runtimeSelected(myRuntime);
  }
}
