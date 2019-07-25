// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.command;

import com.intellij.bootRuntime.BinTrayUtil;
import com.intellij.bootRuntime.Controller;
import com.intellij.bootRuntime.bundles.Runtime;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class Download extends RuntimeCommand {

  private static final Logger LOG = Logger.getInstance("#com.intellij.bootRuntime.command.Download");

  public Download(Project project, Controller controller, Runtime runtime) {
    super(project,controller,"Download", runtime);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    File downloadDirectoryFile = myRuntime.getDownloadPath();
    if (!BinTrayUtil.downloadPath().exists()) {
      BinTrayUtil.downloadPath().mkdir();
    }
    if (!downloadDirectoryFile.exists()) {
      String link = "https://bintray.com/jetbrains/intellij-jdk/download_file?file_path=" + getRuntime().getFileName();

    runWithProgress("Downloading...", (progressIndicator) -> {
      progressIndicator.setIndeterminate(true);
      try {
        HttpRequests.request(link).saveToFile(downloadDirectoryFile, progressIndicator);
      } catch (IOException ioe) {
        LOG.warn(ioe);
      }
    });
  }
}
}
