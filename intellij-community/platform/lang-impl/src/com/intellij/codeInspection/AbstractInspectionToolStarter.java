/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.util.containers.ContainerUtil;
import com.sampullara.cli.Args;
import org.jetbrains.annotations.NotNull;

/**
 * @author Roman.Chernyatchik
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public abstract class AbstractInspectionToolStarter implements ApplicationStarter {
  protected InspectionApplication myApplication;
  protected InspectionToolCmdlineOptions myOptions;

  protected abstract AbstractInspectionCmdlineOptions createCmdlineOptions();

  @Override
  public void premain(String[] args) {
    myOptions = createCmdlineOptions();
    try {
      Args.parse(myOptions, args);
    }
    catch (Exception e) {
      printHelpAndExit(args, myOptions);
      return;
    }

    if (verbose(myOptions) && !myOptions.suppressHelp()) {
      final StringBuilder buff = new StringBuilder("Options:");
      printArgs(args, buff);
      buff.append("\n");
      System.out.println(buff.toString());
    }

    // TODO[romeo] : if config given - parse config and set attrs
    //Properties p = new Properties();
    // p.put("input", "inputfile");
    // p.put("o", "outputfile");
    // p.put("someflag", "true");
    // p.put("m", "10");
    // p.put("values", "1:2:3");
    // p.put("strings", "sam;dave;jolly");
    // PropertiesArgs.parse(tc, p);
    try {
      myOptions.validate();
    }
    catch (InspectionToolCmdlineOptions.CmdlineArgsValidationException e) {
      System.err.println(e.getMessage());
      if (!myOptions.suppressHelp()) {
        printHelpAndExit(args, myOptions);
      }
      System.exit(1);
    }

    myApplication = new InspectionApplication();
    initApplication(myApplication, myOptions);

    // TODO: keep application settings in Memory
  }

  protected InspectionApplication getApplication() {
    return myApplication;
  }

  @Override
  public void main(String[] args) {
    myOptions.beforeStartup();
    myApplication.startup();
  }

  private static void initApplication(@NotNull final InspectionApplication application,
                                      @NotNull final InspectionToolCmdlineOptions opts) {
    opts.initApplication(application);
  }

  private static boolean verbose(final InspectionToolCmdlineOptions opts) {
    return opts.getVerboseLevelProperty() > 0;
  }

  protected void printArgs(String[] args, StringBuilder buff) {
    if (args.length < 2) {
      buff.append(" no arguments");
    }
    else {
      final String argString = ParametersList.join(ContainerUtil.newArrayList(args, 1, args.length));
      buff.append(argString);
    }
  }

  protected void printHelpAndExit(final String[] args, final InspectionToolCmdlineOptions opts) {
    final StringBuilder buff = new StringBuilder();
    buff.append("\n");
    buff.append("Invalid options or syntax:");
    printArgs(args, buff);
    System.err.println(buff.toString());
    opts.printHelpAndExit();
  }
}
