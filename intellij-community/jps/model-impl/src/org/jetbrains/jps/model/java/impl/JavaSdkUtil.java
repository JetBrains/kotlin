// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileFilters;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.intellij.util.ObjectUtils.notNull;

/**
 * @author nik
 */
public class JavaSdkUtil {
  @NotNull
  public static List<File> getJdkClassesRoots(@NotNull File home, boolean isJre) {
    File[] jarDirs;
    if (SystemInfo.isMac && !home.getName().startsWith("mockJDK")) {
      File openJdkRtJar = new File(home, "jre/lib/rt.jar");
      if (openJdkRtJar.isFile()) {
        File libDir = new File(home, "lib");
        File classesDir = openJdkRtJar.getParentFile();
        File libExtDir = new File(openJdkRtJar.getParentFile(), "ext");
        File libEndorsedDir = new File(libDir, "endorsed");
        jarDirs = new File[]{libEndorsedDir, libDir, classesDir, libExtDir};
      }
      else {
        File libDir = new File(home, "lib");
        File classesDir = new File(home, "../Classes");
        File libExtDir = new File(libDir, "ext");
        File libEndorsedDir = new File(libDir, "endorsed");
        jarDirs = new File[]{libEndorsedDir, libDir, classesDir, libExtDir};
      }
    }
    else if (new File(home, "lib/jrt-fs.jar").exists()) {
      jarDirs = ArrayUtilRt.EMPTY_FILE_ARRAY;
    }
    else {
      File libDir = new File(home, isJre ? "lib" : "jre/lib");
      File libExtDir = new File(libDir, "ext");
      File libEndorsedDir = new File(libDir, "endorsed");
      jarDirs = new File[]{libEndorsedDir, libDir, libExtDir};
    }

    FileFilter jarFileFilter = FileFilters.filesWithExtension("jar");
    Set<String> pathFilter = ContainerUtil.newTroveSet(FileUtil.PATH_HASHING_STRATEGY);
    List<File> rootFiles = new ArrayList<>();
    if (Registry.is("project.structure.add.tools.jar.to.new.jdk")) {
      File toolsJar = new File(home, "lib/tools.jar");
      if (toolsJar.isFile()) {
        rootFiles.add(toolsJar);
      }
    }
    for (File jarDir : jarDirs) {
      if (jarDir != null && jarDir.isDirectory()) {
        File[] jarFiles = listFiles(jarDir, jarFileFilter);
        for (File jarFile : jarFiles) {
          String jarFileName = jarFile.getName();
          if (jarFileName.equals("alt-rt.jar") || jarFileName.equals("alt-string.jar")) {
            continue;  // filter out alternative implementations
          }
          String canonicalPath = getCanonicalPath(jarFile);
          if (canonicalPath == null || !pathFilter.add(canonicalPath)) {
            continue;  // filter out duplicate (symbolically linked) .jar files commonly found in OS X JDK distributions
          }
          rootFiles.add(jarFile);
        }
      }
    }

    List<File> ibmJdkLookupDirs = ContainerUtil.newArrayList(new File(home, isJre ? "bin" : "jre/bin"));
    ContainerUtil.addAll(ibmJdkLookupDirs, listFiles(new File(home, isJre ? "lib" : "jre/lib"), FileUtilRt.ALL_DIRECTORIES));
    for (File candidate : ibmJdkLookupDirs) {
      File[] vmJarDirs = listFiles(new File(candidate, "default"), f -> f.getName().startsWith("jclSC") && f.isDirectory());
      for (File dir : vmJarDirs) {
        File vmJar = new File(dir, "vm.jar");
        if (vmJar.isFile()) {
          rootFiles.add(vmJar);
        }
      }
    }

    File classesZip = new File(home, "lib/classes.zip");
    if (classesZip.isFile()) {
      rootFiles.add(classesZip);
    }

    if (rootFiles.isEmpty()) {
      File classesDir = new File(home, "classes");
      if (classesDir.isDirectory()) {
        rootFiles.add(classesDir);
      }
    }

    return rootFiles;
  }

  private static File[] listFiles(File dir, FileFilter filter) {
    return notNull(dir.listFiles(filter), ArrayUtilRt.EMPTY_FILE_ARRAY);
  }

  @Nullable
  private static String getCanonicalPath(File file) {
    try {
      return file.getCanonicalPath();
    }
    catch (IOException e) {
      return null;
    }
  }
}