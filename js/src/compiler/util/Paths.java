// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package compiler.util;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for obtaining relative paths from files and translating
 * relative paths into files.
 */
public class Paths {

  /**
   * Answer the relative path from baseFile to relFile
   * 
   * @param baseFile the base file (not <code>null</code>) from which the
   *          desired path starts
   * @param relFile the file referenced by the desired relative path (not
   *          <code>null</code>)
   * @return a relative path (not <code>null</code>)
   */
  public static String relativePathFor(File baseFile, File relFile) {
    String baseFilePath = baseFile.getPath().replace(File.separatorChar, '/');
    String relFilePath = relFile.getPath().replace(File.separatorChar, '/');
    int baseFilePathLen = baseFilePath.length();
    int relFilePathLen = relFilePath.length();

    // Find the com.google.dart.compiler.backend.common path elements
    int index = 0;
    while (index < baseFilePathLen - 1 && index < relFilePathLen - 1) {
      if (baseFilePath.charAt(index) != relFilePath.charAt(index)) {
        break;
      }
      index++;
    }
    while (index >= 0
        && (baseFilePath.charAt(index) != '/' || relFilePath.charAt(index) != '/')) {
      index--;
    }
    int commonStart = index;

    // Build a path up from the base file
    StringBuilder relPath = new StringBuilder(baseFilePathLen + relFilePathLen
        - commonStart * 2);
    index = commonStart + 1;
    while (true) {
      index = baseFilePath.indexOf('/', index);
      if (index == -1) {
        break;
      }
      relPath.append("../");
      index++;
    }
    relPath.append(relFilePath.substring(commonStart + 1));
    return relPath.toString();
  }

  /**
   * Answer the file relative to the specified file
   * 
   * @param baseFile the base file (not <code>null</code>)
   * @param relPath the path to the desired file relative to baseFile
   * @return the file (not <code>null</code>)
   */
  public static File relativePathToFile(File baseFile, String relPath) {
    if (relPath.startsWith("/")) {
      return new File(relPath);
    }
    File parentFile = baseFile.getParentFile();
    String name;
    if (parentFile == null) {
      name = ".";
    } else {
      name = parentFile.getPath().replace(File.separatorChar, '/');
    }
    name = URI.create(name + "/" + relPath).normalize().getPath();
    return new File(name);
  }

  /**
   * Given a collection of paths, return a collection of files
   * 
   * @param a collection of paths to various files (not <code>null</code>,
   *          contains no <code>null</code>s)
   * @return a collection of files (not <code>null</code>, contains no
   *         <code>null</code>s)
   */
  public static List<File> toFiles(List<String> filePaths) {
    List<File> files = new ArrayList<File>();
    for (String path : filePaths) {
      files.add(new File(path));
    }
    return files;
  }
}
