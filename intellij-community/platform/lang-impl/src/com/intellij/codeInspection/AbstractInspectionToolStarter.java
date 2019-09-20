// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.util.ArrayUtilRt;
import com.sampullara.cli.Args;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Roman.Chernyatchik
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public abstract class AbstractInspectionToolStarter implements ApplicationStarter {
  protected InspectionApplication myApplication;
  protected InspectionToolCmdlineOptions myOptions;

  protected abstract AbstractInspectionCmdlineOptions createCmdlineOptions();

  @Override
  public void premain(@NotNull List<String> args) {
    myOptions = createCmdlineOptions();
    try {
      Args.parse(myOptions, ArrayUtilRt.toStringArray(args));
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
  public void main(@NotNull String[] args) {
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

  protected void printArgs(@NotNull List<String> args, @NotNull StringBuilder buff) {
    if (args.size() < 2) {
      buff.append(" no arguments");
    }
    else {
      buff.append(ParametersList.join(args.subList(1, args.size())));
    }
  }

  protected void printHelpAndExit(@NotNull List<String> args, final InspectionToolCmdlineOptions opts) {
    final StringBuilder buff = new StringBuilder();
    buff.append("\n");
    buff.append("Invalid options or syntax:");
    printArgs(args, buff);
    System.err.println(buff.toString());
    opts.printHelpAndExit();
  }
}
