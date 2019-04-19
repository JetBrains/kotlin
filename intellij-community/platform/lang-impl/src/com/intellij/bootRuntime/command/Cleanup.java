// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.command;

import com.intellij.bootRuntime.BinTrayUtil;
import com.intellij.bootRuntime.Controller;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;

import java.awt.event.ActionEvent;

public class Cleanup extends Command {
  public Cleanup(Project project, Controller controller) {
    super(project, controller,"Clean-up");
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    runWithProgress("Cleaning up..." , indicator -> {
      FileUtil.delete(BinTrayUtil.getJdkStoragePathFile());
      FileUtil.delete(BinTrayUtil.downloadPath());
      FileUtil.delete(BinTrayUtil.getJdkConfigFilePath());
    });
  }
}
