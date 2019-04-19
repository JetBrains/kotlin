// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.command;

import com.intellij.bootRuntime.BinTrayUtil;
import com.intellij.bootRuntime.Controller;
import com.intellij.bootRuntime.bundles.Runtime;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.jetbrains.io.TarKt.unpackTarGz;

public class Extract extends RuntimeCommand {

  private static final Logger LOG = Logger.getInstance("#com.intellij.bootRuntime.command.Extract");

  public Extract(Project project, Controller controller, Runtime runtime) {
    super(project, controller, "Extract", runtime);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    runWithProgress("Extracting...", indicator -> {
      String archiveFileName = getRuntime().getFileName();
      String directoryToExtractName = BinTrayUtil.archveToDirectoryName(archiveFileName);
      File jdkStoragePathFile = BinTrayUtil.getJdkStoragePathFile();
      if (!jdkStoragePathFile.exists()) {
        FileUtil.createDirectory(jdkStoragePathFile);
      }

      File directoryToExtractFile = new File(jdkStoragePathFile, directoryToExtractName);
      if (!directoryToExtractFile.exists()) {
        FileUtil.createDirectory(directoryToExtractFile);
        try (FileInputStream inputStream = new FileInputStream(myRuntime.getDownloadPath())) {
          unpackTarGz(inputStream, directoryToExtractFile.toPath());
          FileUtil.delete(myRuntime.getDownloadPath());
        }
        catch (IOException ex) {
          LOG.warn(ex);
        }
      }
      BinTrayUtil.updateJdkConfigFileAndRestart(directoryToExtractFile);
    });
  }
}
