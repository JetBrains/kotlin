// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.compiler.actions.GenerateAntBuildAction;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * @author max
 */
public class GenerateAntApplication {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.ex.InspectionApplication");

  public String myProjectPath = null;
  public String myOutPath = null;
  private Project myProject;
  private int myVerboseLevel = 0;

  public void startup() {
    if (myProjectPath == null || myOutPath == null) {
      GenerateAntMain.printHelp();
    }

    SwingUtilities.invokeLater(() -> {
      ApplicationEx application = ApplicationManagerEx.getApplicationEx();
      try {
        logMessage(0, "Starting app... ");
        application.setSaveAllowed(false);
        application.load();
        logMessageLn(0, "done");

        this.run();
      }
      catch (Exception e) {
        LOG.error(e);
      }
      finally {
        application.exit(true, true);
      }
    });
  }

  public void run() {
    try {
      myProjectPath = myProjectPath.replace(File.separatorChar, '/');
      VirtualFile vfsProject = LocalFileSystem.getInstance().findFileByPath(myProjectPath);
      if (vfsProject == null) {
        logError(InspectionsBundle.message("inspection.application.file.cannot.be.found", myProjectPath));
        GenerateAntMain.printHelp();
      }

      logMessage(0, "Loading project...");
      myProject = ProjectManagerEx.getInstanceEx().loadProject(myProjectPath);

      logMessageLn(0, " done");

      GenerateAntBuildAction.generateSingleFileBuild(myProject,
                                                     new GenerationOptionsImpl(myProject, true, true, false, false, new String[] {}),
                                                     new File("/Users/max/build/build.xml"),
                                                     new File("/Users/max/build/build.properties"));

      logMessage(0, "Hello!");
    }
    catch (IOException e) {
      LOG.error(e);
      GenerateAntMain.printHelp();
    }
    catch (Exception e) {
      LOG.error(e);
      System.exit(1);
    }
  }

  public void setVerboseLevel(int verboseLevel) {
    myVerboseLevel = verboseLevel;
  }

  private void logMessage(int minVerboseLevel, String message) {
    if (myVerboseLevel >= minVerboseLevel) {
      System.out.print(message);
    }
  }

  private void logError(String message) {
    System.err.println(message);
  }

  private void logMessageLn(int minVerboseLevel, String message) {
    if (myVerboseLevel >= minVerboseLevel) {
      System.out.println(message);
    }
  }
}
