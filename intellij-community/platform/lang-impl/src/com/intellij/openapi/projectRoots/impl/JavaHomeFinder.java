// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SystemProperties;
import com.intellij.util.lang.JavaVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class JavaHomeFinder {

  /**
   * Tries to find existing Java SDKs on this computer.
   * If no JDK found, returns possible folders to start file chooser.
   * @return suggested sdk home paths (sorted)
   */
  @NotNull
  public static List<String> suggestHomePaths() {
    return suggestHomePaths(false);
  }

  /**
   * Do the same as {@link #suggestHomePaths()} but always considers the embedded JRE,
   * for using in tests that are performed when the registry is not properly initialized
   * or that need the embedded JetBrains Runtime.
   */
  @NotNull
  public static List<String> suggestHomePaths(boolean forceEmbeddedJava) {
    JavaHomeFinder javaFinder = getFinder(forceEmbeddedJava);
    Collection<String> foundPaths = javaFinder.findExistingJdks();
    ArrayList<String> paths = new ArrayList<>(foundPaths);
    paths.sort((o1, o2) -> Comparing.compare(JavaVersion.tryParse(o2), JavaVersion.tryParse(o1)));
    return paths;
  }

  @NotNull
  protected abstract List<String> findExistingJdks();

  private static JavaHomeFinder getFinder(boolean forceEmbeddedJava) {
    boolean detectorIsEnabled = forceEmbeddedJava || Registry.is("java.detector.enabled", true);
    if (!detectorIsEnabled) {
      return new JavaHomeFinder() {
        @NotNull
        @Override
        protected List<String> findExistingJdks() {
          return Collections.emptyList();
        }
      };
    }

    if (SystemInfo.isWindows) {
      return new JavaHomeFinderWindows();
    }
    if (SystemInfo.isMac) {
      return new MacFinder(forceEmbeddedJava);
    }
    if (SystemInfo.isLinux) {
      return new DefaultFinder(forceEmbeddedJava, "/usr/java", "/opt/java", "/usr/lib/jvm");
    }
    if (SystemInfo.isSolaris) {
      return new DefaultFinder(forceEmbeddedJava, "/usr/jdk");
    }
    return new DefaultFinder(forceEmbeddedJava);
  }

  protected void scanFolder(@NotNull File folder, boolean includeNestDirs, @NotNull List<? super String> result) {
    if (JdkUtil.checkForJdk(folder)) {
      result.add(folder.getAbsolutePath());
    }
    else if (includeNestDirs) {
      for (File file : ObjectUtils.notNull(folder.listFiles(), ArrayUtilRt.EMPTY_FILE_ARRAY)) {
        file = adjustPath(file);
        if (JdkUtil.checkForJdk(file)) {
          result.add(file.getAbsolutePath());
        }
      }
    }
  }

  protected File adjustPath(File file) {
    return file;
  }

  protected static File getJavaHome() {
    String property = SystemProperties.getJavaHome();
    if (property == null)
      return null;

    File javaHome = new File(property).getParentFile();//actually java.home points to to jre home
    return javaHome == null || !javaHome.isDirectory() ? null : javaHome;
  }

  protected static class DefaultFinder extends JavaHomeFinder {

    private final String[] myPaths;

    protected DefaultFinder(boolean forceEmbeddedJava, String... paths) {
      File javaHome = null;
      if (forceEmbeddedJava || Registry.is("java.detector.include.embedded", false)) javaHome = getJavaHome();
      myPaths = javaHome == null ? paths : ArrayUtil.prepend(javaHome.getAbsolutePath(), paths);
    }

    @NotNull
    @Override
    public List<String> findExistingJdks() {
      ArrayList<String> result = new ArrayList<>();
      for (String path : myPaths) {
        scanFolder(new File(path), true, result);
      }
      for (File dir : guessByPathVariable()) {
        scanFolder(dir, false, result);
      }
      removeDuplicates(result, SystemInfo.isFileSystemCaseSensitive);
      return result;
    }

    public Collection<File> guessByPathVariable() {
      String pathVarString = System.getenv("PATH");
      if (pathVarString == null || pathVarString.isEmpty()) return Collections.emptyList();
      boolean isWindows = SystemInfo.isWindows;
      String suffix = isWindows ? ".exe" : "";
      ArrayList<File> dirsToCheck = new ArrayList<>(1);
      String[] pathEntries = pathVarString.split(File.pathSeparator);
      for (String p : pathEntries) {
        File dir = new File(p);
        if (StringUtilRt.equal(dir.getName(), "bin", SystemInfo.isFileSystemCaseSensitive)) {
          File f1 = new File(p, "java" + suffix);
          File f2 = new File(p, "javac" + suffix);
          if (f1.isFile() && f2.isFile()) {
            File f1c = canonize(f1);
            File f2c = canonize(f2);
            File d1 = granny(f1c);
            File d2 = granny(f2c);
            if (d1 != null && d2 != null && FileUtil.filesEqual(d1, d2)) {
              dirsToCheck.add(d1);
            }
          }
        }
      }
      return dirsToCheck;
    }
  }

  private static class MacFinder extends DefaultFinder {

    public static final String JAVA_HOME_FIND_UTIL = "/usr/libexec/java_home";

    MacFinder(boolean forceEmbeddedJava) {
      super(forceEmbeddedJava, "/Library/Java/JavaVirtualMachines", "/System/Library/Java/JavaVirtualMachines");
    }

    @NotNull
    @Override
    public List<String> findExistingJdks() {
      List<String> list = super.findExistingJdks();
      String defaultJavaHome = getSystemDefaultJavaHome();
      if (defaultJavaHome != null) {
        ArrayList<String> list2 = new ArrayList<>(list.size() + 1);
        list2.add(defaultJavaHome);
        list2.addAll(list);
        list = list2;
      }
      return list;
    }

    @Nullable
    private String getSystemDefaultJavaHome() {
      String homePath = null;
      if (SystemInfo.isMacOSLeopard) {
        // since version 10.5
        if (canExecute(JAVA_HOME_FIND_UTIL)) homePath = ExecUtil.execAndReadLine(new GeneralCommandLine(JAVA_HOME_FIND_UTIL));
      }
      else {
        // before version 10.5
        homePath = "/Library/Java/Home";
      }

      return isDirectory(homePath) ? homePath : null;
    }

    private static boolean canExecute(@Nullable String filePath) {
      if (filePath == null) return false;
      File file = new File(filePath);
      return file.canExecute();
    }

    private static boolean isDirectory(@Nullable String path) {
      if (path == null) return false;
      File dir = new File(path);
      return dir.isDirectory();
    }


    @Override
    protected File adjustPath(File file) {
      File home = new File(file, "/Home");
      if (home.exists()) return home;

      home = new File(file, "Contents/Home");
      if (home.exists()) return home;

      return file;
    }
  }

  @NotNull
  private static File canonize(@NotNull File file) {
    try {
      return file.getCanonicalFile();
    }
    catch (IOException ioe) {
      return file.getAbsoluteFile();
    }
  }

  @Nullable
  private static File granny(@Nullable File file) {
    File parent = file.getParentFile();
    return parent != null ? parent.getParentFile() : null;
  }

  private static void removeDuplicates(@NotNull ArrayList<String> strings, boolean caseSensitive) {
    int k = strings.size() - 1;
    while (k > 0) {
      String s = strings.get(k);
      for (int i = 0; i < k; i++) {
        if (StringUtil.equal(strings.get(i), s, caseSensitive)) {
          strings.remove(k);
          break;
        }
      }
      k--;
    }
  }

}
