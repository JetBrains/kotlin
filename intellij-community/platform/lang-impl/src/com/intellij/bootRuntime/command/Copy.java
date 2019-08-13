// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.command;

import com.intellij.bootRuntime.Controller;
import com.intellij.bootRuntime.bundles.Runtime;
import com.intellij.openapi.project.Project;

import java.awt.event.ActionEvent;

public class Copy extends RuntimeCommand {
  public Copy(Project project, Controller controller, Runtime runtime) {
    super(project, controller, "Copy", runtime);
  }

  @Override
  public void actionPerformed(ActionEvent e) {}
}
