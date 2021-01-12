// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eugene Zhuravlev
 */
public final class UrlUtil {
  private static final String JAR_SEPARATOR = URLUtil.JAR_SEPARATOR;
  private static final String URL_PATH_SEPARATOR = "/";
  private static final String FILE_PROTOCOL = URLUtil.FILE_PROTOCOL;
  private static final String FILE_PROTOCOL_PREFIX = FILE_PROTOCOL + ":";

  @NotNull
  public static String loadText(@NotNull URL url) throws IOException {
    try (InputStream stream = new BufferedInputStream(URLUtil.openStream(url))) {
      return new String(FileUtil.loadBytes(stream), FileTemplate.ourEncoding);
    }
  }

  @NotNull
  public static List<String> getChildrenRelativePaths(@NotNull URL root) throws IOException {
    final String protocol = root.getProtocol();
    if ("jar".equalsIgnoreCase(protocol)) {
      return getChildPathsFromJar(root);
    }
    if (FILE_PROTOCOL.equalsIgnoreCase(protocol)){
      return getChildPathsFromFile(root);
    }
    return Collections.emptyList();
  }

  @NotNull
  private static List<String> getChildPathsFromFile(@NotNull URL root) {
    final List<String> paths = new ArrayList<>();
    final File rootFile = new File(FileUtil.unquote(root.getPath()));
    new Object() {
      void collectFiles(File fromFile, String prefix) {
        final File[] list = fromFile.listFiles();
        if (list != null) {
          for (File file : list) {
            final String childRelativePath = prefix.isEmpty() ? file.getName() : prefix + URL_PATH_SEPARATOR + file.getName();
            if (file.isDirectory()) {
              collectFiles(file, childRelativePath);
            }
            else {
              paths.add(childRelativePath);
            }
          }
        }
      }
    }.collectFiles(rootFile, "");
    return paths;
  }

  @NotNull
  private static List<String> getChildPathsFromJar(@NotNull URL root) throws IOException {
    String file = root.getFile();
    file = StringUtil.trimStart(file, FILE_PROTOCOL_PREFIX);
    final int jarSeparatorIndex = file.indexOf(JAR_SEPARATOR);
    assert jarSeparatorIndex > 0;

    String rootDirName = file.substring(jarSeparatorIndex + 2);
    if (!rootDirName.endsWith(URL_PATH_SEPARATOR)) {
      rootDirName += URL_PATH_SEPARATOR;
    }
    try (ZipFile zipFile = new ZipFile(FileUtil.unquote(file.substring(0, jarSeparatorIndex)))) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      final List<String> paths = new ArrayList<>();
      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory()) {
          final String relPath = entry.getName();
          if (relPath.startsWith(rootDirName)) {
            paths.add(relPath.substring(rootDirName.length()));
          }
        }
      }
      return paths;
    }
  }
}
