// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Stream;

public class BinTrayUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.actions.SwitchBootJdkAction");
  @NotNull
  private static final String productJdkConfigFileName;
  @Nullable
  private static final String pathsSelector;
  @NotNull
  private static final File productJdkConfigDir;
  @NotNull
  private static final File productJdkConfigFile;

  private static String getExecutable() {
    String executable = System.getProperty("idea.executable");
    return executable != null ? executable : StringUtil.toLowerCase(ApplicationNamesInfo.getInstance().getProductName());
  }

  @NotNull
  public static File getJdkConfigFilePath() {
    if (!productJdkConfigDir.exists()) {
      try {
        if (!productJdkConfigDir.mkdirs()) {
          LOG.error("Could not create " + productJdkConfigDir + " productJdkConfigDir");
          return null;
        }

        if (!productJdkConfigFile.exists() && !productJdkConfigFile.createNewFile()) {
          LOG.error("Could not create " + productJdkConfigFileName + " productJdkConfigFile");
        }
      } catch (IOException var1) {
        LOG.error(var1);
      }
    }

    return productJdkConfigFile;
  }

  @NotNull
  public static String archveToDirectoryName(String archiveFileName) {
    return archiveFileName.substring(0, archiveFileName.lastIndexOf(".tar.gz"));
  }

  @NotNull
  public static File downloadPath() {
    return new File(PathManager.getPluginTempPath(), "jdk_archives");
  }

  public static void updateJdkConfigFileAndRestart(File directoryToExtractFile) {
    File jdkConfigFilePath = getJdkConfigFilePath();
    if (jdkConfigFilePath != null) {
      try {
        FileWriter fooWriter = new FileWriter(jdkConfigFilePath, false);
        Throwable var4 = null;

        try {
          File jdkPath = SystemInfo.isMac ? new File(directoryToExtractFile, "jdk") : directoryToExtractFile;
          String jdk = jdkPath.getPath();
          fooWriter.write(jdk);
        } catch (Throwable var15) {
          var4 = var15;
          throw var15;
        } finally {
          if (fooWriter != null) {
            if (var4 != null) {
              try {
                fooWriter.close();
              } catch (Throwable var14) {
                var4.addSuppressed(var14);
              }
            } else {
              fooWriter.close();
            }
          }

        }
      } catch (IOException var17) {
        var17.printStackTrace();
      }

      ApplicationManager.getApplication().restart();
    }
  }

  public static File getJdkStoragePathFile() {
    return new File(PathManager.getConfigPath() + File.separator + "jdks" + File.separator);
  }

  public static boolean isInstalled(String selectedItem) {
    File jdkBundleDirFile = new File(getJdkStoragePathFile(), archveToDirectoryName(selectedItem));
    File bundlePathFromItem = SystemInfo.isMac ? new File(jdkBundleDirFile, "jdk") : jdkBundleDirFile;
    return bundlePathFromItem.exists() && isActiveBundle(selectedItem);
  }

  public static boolean isActiveBundle(String selectedItem) {
    File jdkConfigFilePath = getJdkConfigFilePath();
    File jdkBundleDirFile = new File(getJdkStoragePathFile(), archveToDirectoryName(selectedItem));
    File bundlePathFromItem = SystemInfo.isMac ? new File(jdkBundleDirFile, "jdk") : jdkBundleDirFile;
    if (jdkConfigFilePath != null && jdkConfigFilePath.exists()) {
      try {
        Stream<String> lines = Files.lines(jdkConfigFilePath.toPath(), Charset.defaultCharset());
        Throwable var5 = null;

        boolean var6;
        try {
          var6 = lines != null && lines.anyMatch((pathToBundle) -> FileUtil.filesEqual(bundlePathFromItem, new File(pathToBundle)));
        } catch (Throwable var16) {
          var5 = var16;
          throw var16;
        } finally {
          if (lines != null) {
            if (var5 != null) {
              try {
                lines.close();
              } catch (Throwable var15) {
                var5.addSuppressed(var15);
              }
            } else {
              lines.close();
            }
          }

        }

        return var6;
      } catch (IOException ioe) {
        LOG.warn(ioe);
      }
    }

    return false;
  }

  static {
    productJdkConfigFileName = getExecutable() + (SystemInfo.isWindows ? (SystemInfo.is64Bit ? "64.exe.jdk" : ".exe.jdk") : ".jdk");
    pathsSelector = PathManager.getPathsSelector();
    productJdkConfigDir = new File(pathsSelector != null ? PathManager.getDefaultConfigPathFor(pathsSelector) : PathManager.getConfigPath());
    productJdkConfigFile = new File(productJdkConfigDir, productJdkConfigFileName);
  }
}
