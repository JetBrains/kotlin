/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.compiler.ant;

import com.intellij.openapi.application.ApplicationStarter;
import org.jetbrains.annotations.NonNls;

/**
 * @author max
 */
public class GenerateAntMain implements ApplicationStarter {
  private GenerateAntApplication myApplication;

  @Override
  @NonNls
  public String getCommandName() {
    return "ant";
  }

  @Override
  public void premain(String[] args) {
    System.setProperty("idea.load.plugins", "false");
    myApplication = new GenerateAntApplication();

    myApplication.myProjectPath = args[1];
    myApplication.myOutPath = args[2];
  }

  @Override
  public void main(String[] args) {
    myApplication.startup();
  }

  public static void printHelp() {
    System.out.println("Wrong params");
    System.exit(1);
  }
}
