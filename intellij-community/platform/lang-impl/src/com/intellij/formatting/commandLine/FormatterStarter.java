// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.commandLine;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSettingsLoader;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A launcher class for command-line formatter.
 */
public class FormatterStarter implements ApplicationStarter {

  public static final String FORMAT_COMMAND_NAME = "format";
  private static final Logger LOG = Logger.getInstance(FormatterStarter.class);

  @Override
  public String getCommandName() {
    return FORMAT_COMMAND_NAME;
  }

  @Override
  public void main(@NotNull String[] args) {
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    MessageOutput messageOutput = new MessageOutput(
      new PrintWriter(System.out),
      new PrintWriter(System.err));
    messageOutput.info(getAppInfo() + " Formatter\n");
    FileSetFormatter fileSetFormatter = new FileSetFormatter(messageOutput);
    logArgs(args);
    if (args.length < 2) {
      showUsageInfo(messageOutput);
    }
    for (int i = 1; i < args.length; i++) {
      if (args[i].startsWith("-")) {
        if (checkOption(args[i], "-h", "-help")) {
          showUsageInfo(messageOutput);
        }
        if (checkOption(args[i], "-s", "-settings")) {
          //noinspection AssignmentToForLoopParameter
          i++;
          if (i >= args.length) {
            fatalError(messageOutput, "Missing settings file path.");
          }
          try {
            CodeStyleSettings settings = readSettings(args[i]);
            fileSetFormatter.setCodeStyleSettings(settings);
          }
          catch (SchemeImportException e) {
            fatalError(messageOutput, e.getLocalizedMessage() + "\n");
          }
        }
        else if (checkOption(args[i], "-r", "-R")) {
          fileSetFormatter.setRecursive();
        }
        else if (checkOption(args[i], "-m", "-mask")) {
          //noinspection AssignmentToForLoopParameter
          i++;
          if (i >= args.length) {
            fatalError(messageOutput, "Missing file mask(s).");
          }
          for (String mask : args[i].split(",")) {
            if (!mask.isEmpty()) {
              fileSetFormatter.addFileMask(mask);
            }
          }
        }
        else {
          fatalError(messageOutput, "Unknown option " + args[i]);
        }
      }
      else {
        try {
          fileSetFormatter.addEntry(args[i]);
        }
        catch (IOException e) {
          fatalError(messageOutput, e.getLocalizedMessage());
        }
      }
    }
    try {
      fileSetFormatter.processFiles();
      messageOutput.info("\n" + fileSetFormatter.getProcessedFiles() + " file(s) formatted.\n");
    }
    catch (IOException e) {
      fatalError(messageOutput, e.getLocalizedMessage());
    }
    ((ApplicationEx)ApplicationManager.getApplication()).exit(true, true);
  }

  private static void fatalError(@NotNull MessageOutput messageOutput, @NotNull String message) {
    messageOutput.error("ERROR: " + message + "\n");
    System.exit(1);
  }

  private static CodeStyleSettings readSettings(@NotNull String settingsPath) throws SchemeImportException {
    VirtualFile vFile = VfsUtil.findFileByIoFile(new File(settingsPath), true);
    CodeStyleSettingsLoader loader = new CodeStyleSettingsLoader();
    if (vFile == null) {
      throw new SchemeImportException("Cannot find file " + settingsPath);
    }
    return loader.loadSettings(vFile);
  }

  private static boolean checkOption(@NotNull String arg, String... variants) {
    return ArrayUtil.contains(arg, variants);
  }

  private static String getAppInfo() {
    ApplicationInfoImpl appInfo = (ApplicationInfoImpl)ApplicationInfoEx.getInstanceEx();
    return String.format("%s, build %s", appInfo.getFullApplicationName(), appInfo.getBuild().asString());
  }


  private static void showUsageInfo(@NotNull MessageOutput messageOutput) {
    messageOutput.info("Usage: format [-h] [-r|-R] [-s|-settings settingsPath] path1 path2...\n");
    messageOutput.info("  -h|-help       Show a help message and exit.\n");
    messageOutput.info("  -s|-settings   A path to Intellij IDEA code style settings .xml file.\n");
    messageOutput.info("  -r|-R          Scan directories recursively.\n");
    messageOutput.info("  -m|-mask       A comma-separated list of file masks.\n");
    messageOutput.info("  path<n>        A path to a file or a directory.\n");
    System.exit(0);
  }

  private static void logArgs(@NotNull String[] args) {
    LOG.info("Arguments: " + String.join(",", args));
  }
}
