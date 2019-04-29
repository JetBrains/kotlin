/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.compiler;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

import java.io.File;
import java.util.Arrays;

/**
 * A set of utility methods for working with paths
 */
public class CompilerPaths {
  private static final Logger LOG = Logger.getInstance(CompilerPaths.class);

  /**
   * @return a root directory where generated files for various compilers are stored
   */
  public static File getGeneratedDataDirectory(Project project) {
    return new File(getCompilerSystemDirectory(project), ".generated");
  }

  /**
   * @return a root directory where compiler caches for the given project are stored
   */
  public static File getCacheStoreDirectory(final Project project) {
    return new File(getCompilerSystemDirectory(project), ".caches");
  }

  /**
   * @return a directory under IDEA "system" directory where all files related to compiler subsystem are stored (such as compiler caches or generated files)
   */
  @NotNull
  public static File getCompilerSystemDirectory(@NotNull Project project) {
    return ProjectUtil.getProjectCachePath(project, "compiler").toFile();
  }

  /**
   * @param forTestClasses true if directory for test sources, false - for sources.
   * @return a directory to which the sources (or test sources depending on the second parameter) should be compiled.
   * Null is returned if output directory is not specified or is not valid
   */
  @Nullable
  public static VirtualFile getModuleOutputDirectory(@NotNull Module module, boolean forTestClasses) {
    final CompilerModuleExtension compilerModuleExtension = CompilerModuleExtension.getInstance(module);
    if (compilerModuleExtension == null) {
      return null;
    }
    VirtualFile outPath;
    if (forTestClasses) {
      final VirtualFile path = compilerModuleExtension.getCompilerOutputPathForTests();
      if (path != null) {
        outPath = path;
      }
      else {
        outPath = compilerModuleExtension.getCompilerOutputPath();
      }
    }
    else {
      outPath = compilerModuleExtension.getCompilerOutputPath();
    }
    if (outPath == null) {
      return null;
    }
    if (!outPath.isValid()) {
      LOG.info("Requested output path for module " + module.getName() + " is not valid");
      return null;
    }
    return outPath;
  }

  /**
   * The same as {@link #getModuleOutputDirectory} but returns String.
   * The method still returns a non-null value if the output path is specified in Settings but does not exist on disk.
   */
  @Nullable
  public static String getModuleOutputPath(final Module module, boolean forTestClasses) {
    final CompilerModuleExtension extension = CompilerModuleExtension.getInstance(module);
    if (extension == null) {
      return null;
    }
    final String outPathUrl;
    final Application application = ApplicationManager.getApplication();
    if (forTestClasses) {
      if (application.isDispatchThread()) {
        final String url = extension.getCompilerOutputUrlForTests();
        outPathUrl = url != null ? url : extension.getCompilerOutputUrl();
      }
      else {
        outPathUrl = ReadAction.compute(() -> {
          final String url = extension.getCompilerOutputUrlForTests();
          return url != null ? url : extension.getCompilerOutputUrl();
        });
      }
    }
    else { // for ordinary classes
      if (application.isDispatchThread()) {
        outPathUrl = extension.getCompilerOutputUrl();
      }
      else {
        outPathUrl = ReadAction.compute(() -> extension.getCompilerOutputUrl());
      }
    }
    return outPathUrl != null? VirtualFileManager.extractPath(outPathUrl) : null;
  }

  @Nullable
  public static String getAnnotationProcessorsGenerationPath(Module module, boolean forTests) {
    final AnnotationProcessingConfiguration config = CompilerConfiguration.getInstance(module.getProject()).getAnnotationProcessingConfiguration(module);
    final String sourceDirName = config.getGeneratedSourcesDirectoryName(forTests);
    if (config.isOutputRelativeToContentRoot()) {
      final String[] roots = ModuleRootManager.getInstance(module).getContentRootUrls();
      if (roots.length == 0) {
        return null;
      }
      if (roots.length > 1) {
        Arrays.sort(roots);
      }
      return StringUtil.isEmpty(sourceDirName)? VirtualFileManager.extractPath(roots[0]): VirtualFileManager.extractPath(roots[0]) + "/" + sourceDirName;
    }


    final String path = getModuleOutputPath(module, forTests);
    if (path == null) {
      return null;
    }
    return StringUtil.isEmpty(sourceDirName)? path : path + "/" + sourceDirName;
  }

}
