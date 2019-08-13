// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.command;

import com.intellij.bootRuntime.Controller;
import com.intellij.bootRuntime.bundles.Runtime;
import com.intellij.openapi.project.Project;

import java.awt.event.ActionEvent;

public class RemoteInstall extends RuntimeCommand {
  RemoteInstall(Project project, Controller controller, Runtime runtime) {
    super(project, controller, "Install", runtime);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Processor.process(
      CommandFactory.produce(CommandFactory.Type.DOWNLOAD, myRuntime),
      CommandFactory.produce(CommandFactory.Type.EXTRACT, myRuntime),
      CommandFactory.produce(CommandFactory.Type.COPY, myRuntime),
      CommandFactory.produce(CommandFactory.Type.INSTALL, myRuntime)
    );
  }
}
