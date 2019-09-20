// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.diagnostic.logging;

import com.intellij.execution.configurations.LogFileOptions;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class LogFilesManager {
  private final LogConsoleManager myManager;
  private final List<LogFile> myLogFiles = new ArrayList<>();
  private final SingleAlarm myUpdateAlarm;

  public LogFilesManager(@NotNull final Project project, @NotNull LogConsoleManager manager, @NotNull Disposable parentDisposable) {
    myManager = manager;

    myUpdateAlarm = new SingleAlarm(new Runnable() {
      @Override
      public void run() {
        if (project.isDisposed()) {
          return;
        }

        for (final LogFile logFile : new ArrayList<>(myLogFiles)) {
          ProcessHandler process = logFile.getProcess();
          if (process != null && process.isProcessTerminated()) {
            myLogFiles.remove(logFile);
            continue;
          }

          final Set<String> oldPaths = logFile.getPaths();
          final Set<String> newPaths = logFile.getOptions().getPaths(); // should not be called in UI thread
          logFile.setPaths(newPaths);

          final Set<String> obsoletePaths = new THashSet<>(oldPaths);
          obsoletePaths.removeAll(newPaths);

          try {
            SwingUtilities.invokeAndWait(() -> {
              if (project.isDisposed()) {
                return;
              }

              addConfigurationConsoles(logFile.getOptions(), file -> !oldPaths.contains(file), newPaths, logFile.getConfiguration());
              for (String each : obsoletePaths) {
                myManager.removeLogConsole(each);
              }
            });
          }
          catch (InterruptedException | InvocationTargetException ignored) { }
        }

        if (!myLogFiles.isEmpty()) {
          myUpdateAlarm.cancelAndRequest();
        }
      }
    }, 500, Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
  }

  public void addLogConsoles(@NotNull RunConfigurationBase<?> runConfiguration, @Nullable ProcessHandler startedProcess) {
    for (LogFileOptions logFileOptions : runConfiguration.getAllLogFiles()) {
      if (logFileOptions.isEnabled()) {
        myLogFiles.add(new LogFile(logFileOptions, runConfiguration, startedProcess));
      }
    }
    myUpdateAlarm.request();
    runConfiguration.createAdditionalTabComponents(myManager, startedProcess);
  }

  private void addConfigurationConsoles(@NotNull LogFileOptions logFile, @NotNull Condition<? super String> shouldInclude, @NotNull Set<String> paths, @NotNull RunConfigurationBase runConfiguration) {
    if (paths.isEmpty()) {
      return;
    }

    TreeMap<String, String> titleToPath = new TreeMap<>();
    if (paths.size() == 1) {
      String path = paths.iterator().next();
      if (shouldInclude.value(path)) {
        titleToPath.put(logFile.getName(), path);
      }
    }
    else {
      for (String path : paths) {
        if (shouldInclude.value(path)) {
          String title = new File(path).getName();
          if (titleToPath.containsKey(title)) {
            title = path;
          }
          titleToPath.put(title, path);
        }
      }
    }

    for (String title : titleToPath.keySet()) {
      String path = titleToPath.get(title);
      assert path != null;
      myManager.addLogConsole(title, path, logFile.getCharset(), logFile.isSkipContent() ? new File(path).length() : 0, runConfiguration);
    }
  }

  private static class LogFile {

    private final LogFileOptions myOptions;
    private final RunConfigurationBase myConfiguration;
    private final ProcessHandler myProcess;
    private Set<String> myPaths = new HashSet<>();

    LogFile(LogFileOptions options, RunConfigurationBase configuration, ProcessHandler process) {
      myOptions = options;
      myConfiguration = configuration;
      myProcess = process;
    }

    public LogFileOptions getOptions() {
      return myOptions;
    }

    public RunConfigurationBase getConfiguration() {
      return myConfiguration;
    }

    public ProcessHandler getProcess() {
      return myProcess;
    }

    public Set<String> getPaths() {
      return myPaths;
    }

    public void setPaths(Set<String> paths) {
      myPaths = paths;
    }
  }
}
