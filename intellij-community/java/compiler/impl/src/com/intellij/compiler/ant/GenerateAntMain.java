// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.openapi.application.ApplicationStarter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class GenerateAntMain implements ApplicationStarter {
  private GenerateAntApplication myApplication;

  @Override
  @NonNls
  public String getCommandName() {
    return "ant";
  }

  @Override
  public void premain(@NotNull List<String> args) {
    System.setProperty("idea.load.plugins", "false");
    myApplication = new GenerateAntApplication();

    myApplication.myProjectPath = args.get(1);
    myApplication.myOutPath = args.get(2);
  }

  @Override
  public void main(@NotNull String[] args) {
    myApplication.startup();
  }

  public static void printHelp() {
    System.out.println("Wrong params");
    System.exit(1);
  }
}
