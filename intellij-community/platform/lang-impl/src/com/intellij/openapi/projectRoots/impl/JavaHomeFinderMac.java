// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

class JavaHomeFinderMac extends JavaHomeFinderBasic {
  public static final String JAVA_HOME_FIND_UTIL = "/usr/libexec/java_home";

  static String defaultJavaLocation = "/Library/Java/JavaVirtualMachines";

  JavaHomeFinderMac(boolean forceEmbeddedJava) {
    super(forceEmbeddedJava,
          defaultJavaLocation,
          "/System/Library/Java/JavaVirtualMachines",
          FileUtil.expandUserHome("~/Library/Java/JavaVirtualMachines")
    );

    registerFinder(() -> scanAll(getSystemDefaultJavaHome(), false));
  }

  @Nullable
  private static File getSystemDefaultJavaHome() {
    String homePath = null;
    if (SystemInfo.isMacOSLeopard) {
      // since version 10.5
      if (new File(JAVA_HOME_FIND_UTIL).canExecute()) {
        homePath = ExecUtil.execAndReadLine(new GeneralCommandLine(JAVA_HOME_FIND_UTIL));
      }
    }
    else {
      // before version 10.5
      homePath = "/Library/Java/Home";
    }

    if (homePath != null) {
      return new File(homePath);
    }

    return null;
  }

  @NotNull
  @Override
  protected List<File> listPossibleJdkHomesFromInstallRoot(@NotNull File file) {
    return Arrays.asList(file, new File(file, "/Home"), new File(file, "Contents/Home"));
  }

  @Override
  protected @NotNull List<File> listPossibleJdkInstallRootsFromHomes(@NotNull File file) {
    List<File> result = new ArrayList<>();
    result.add(file);

    if (file.getName().equalsIgnoreCase("Home")) {
      File parentFile = file.getParentFile();
      if (parentFile != null) {
        result.add(parentFile);

        if (parentFile.getName().equalsIgnoreCase("Contents")) {
          File parentParentFile = parentFile.getParentFile();
          if (parentParentFile != null) {
            result.add(parentParentFile);
          }
        }
      }
    }

    return result;
  }
}
