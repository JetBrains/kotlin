// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Eugene Zhuravlev
 */
public class GenerationUtils {
  private GenerationUtils() {
  }

  /**
   * Get relative file
   *
   * @param file       a valid file (must be either belong to {@link com.intellij.openapi.vfs.LocalFileSystem}  or to point to the root entry on
   *                   {@link com.intellij.openapi.vfs.JarFileSystem}.
   * @param chunk      a module chunk.
   * @param genOptions generation options
   * @return a relative path
   */
  @Nullable
  public static String toRelativePath(final VirtualFile file, final ModuleChunk chunk, final GenerationOptions genOptions) {
    final Module module = chunk.getModules()[0];
    final File moduleBaseDir = chunk.getBaseDir();
    return toRelativePath(file, moduleBaseDir, BuildProperties.getModuleBasedirProperty(module), genOptions);
  }

  public static String toRelativePath(final String file, final File baseDir, final Module module, final GenerationOptions genOptions) {
    return toRelativePath(file, baseDir, BuildProperties.getModuleBasedirProperty(module), genOptions);
  }

  public static String toRelativePath(final String path, final ModuleChunk chunk, final GenerationOptions genOptions) {
    return toRelativePath(path, chunk.getBaseDir(), BuildProperties.getModuleChunkBasedirProperty(chunk), genOptions);
  }

  /**
   * Get relative file
   *
   * @param file                a valid file (must be either belong to {@link com.intellij.openapi.vfs.LocalFileSystem}  or to point to the root entry on
   *                            {@link com.intellij.openapi.vfs.JarFileSystem}.
   * @param baseDir             base director for relative path calculation
   * @param baseDirPropertyName property name for the base directory
   * @param genOptions          generation options
   * @return a relative path
   */
  @Nullable
  public static String toRelativePath(final VirtualFile file,
                                      final File baseDir,
                                      final String baseDirPropertyName,
                                      final GenerationOptions genOptions) {
    final String localPath = PathUtil.getLocalPath(file);
    if (localPath == null) {
      return null;
    }
    return toRelativePath(localPath, baseDir, baseDirPropertyName, genOptions);
  }

  public static String toRelativePath(@NotNull String path,
                                      File baseDir,
                                      @NonNls final String baseDirPropertyName,
                                      GenerationOptions genOptions) {
    path = FileUtilRt.toSystemIndependentName(path);
    if (path.length() == 0) {
      return path;
    }
    final String substitutedPath = genOptions.subsitutePathWithMacros(path);
    if (!substitutedPath.equals(path)) {
      // path variable substitution has highest priority
      return substitutedPath;
    }
    if (baseDir != null) {
      File base;
      try {
        // use canonical paths in order to resolve symlinks
        base = baseDir.getCanonicalFile();
      }
      catch (IOException e) {
        base = baseDir;
      }
      final String relativepath = FileUtil.getRelativePath(base, new File(path));
      if (relativepath != null) {
        final String _relativePath = relativepath.replace(File.separatorChar, '/');
        final String root = BuildProperties.propertyRef(baseDirPropertyName);
        return ".".equals(_relativePath) ? root : root + "/" + _relativePath;
      }
    }
    return substitutedPath;
  }

  public static String trimJarSeparator(final String path) {
    return path.endsWith(JarFileSystem.JAR_SEPARATOR) ? path.substring(0, path.length() - JarFileSystem.JAR_SEPARATOR.length()) : path;
  }
}
