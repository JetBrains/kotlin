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

/*
 * @author max
 */
package com.intellij.ide.impl;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.ModifiableModelCommitter;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility to patch project model by excluding folders/files from content roots.
 * Can be used for running offline inspections (from command-line directly or in teamcity).
 *
 * The main logic is in the method {@link #patchProject(com.intellij.openapi.project.Project)}.
 *
 * @see com.intellij.codeInspection.InspectionApplication
 */
public class PatchProjectUtil {
  private PatchProjectUtil() {
  }

  /**
   * Excludes folders specified in patterns in the {@code idea.exclude.patterns} system property from the project.
   *
   * <p>Pattern syntax:
   * <br>
   *
   * <ul>
   *   <li>{@code patterns := pattern(';'pattern)*}
   *   <li>{@code pattern := ('['moduleRegEx']')? directoryAntPattern}
   * </ul>
   *
   * Where
   * <ul>
   *   <li> {@code moduleRegex} - regular expression to match module name.
   *   <li> {@code directoryAntPattern} - ant-style pattern to match folder in a module.
   *        {@code directoryAntPattern} considers paths <b>relative</b> to a content root of a module.
   * </ul>
   *
   *
   * <p>
   * Example:<br>
   * {@code
   *   -Didea.exclude.patterns=testData/**;.reports/**;[sql]/test/*.sql;[graph]/**;[graph-openapi]/**
   * }
   * <br>
   *
   * In this example the {@code testData/**} pattern is applied to all modules
   * and the pattern {@code /test/*.sql} to applied to the module {@code sql} only.
   *
   * @param project project to patch
   * @see <a href="http://ant.apache.org/manual/dirtasks.html">http://ant.apache.org/manual/dirtasks.html</a>
   */
  public static void patchProject(final Project project) {
    final Map<Pattern, Set<Pattern>> excludePatterns = loadPatterns("idea.exclude.patterns");
    final Map<Pattern, Set<Pattern>> includePatterns = loadPatterns("idea.include.patterns");

    if (excludePatterns.isEmpty() && includePatterns.isEmpty()) return;
    patchProject(project, excludePatterns, includePatterns);
  }

  @VisibleForTesting
  public static void patchProject(Project project,
                                  Map<Pattern, Set<Pattern>> excludePatterns,
                                  Map<Pattern, Set<Pattern>> includePatterns) {
    final ModifiableModuleModel modulesModel = ModuleManager.getInstance(project).getModifiableModel();
    final Module[] modules = modulesModel.getModules();
    final ModifiableRootModel[] models = new ModifiableRootModel[modules.length];
    for (int i = 0; i < modules.length; i++) {
      ModuleRootManager rootManager = ModuleRootManager.getInstance(modules[i]);
      models[i] = rootManager.getModifiableModel();
      String moduleName = modules[i].getName();
      final ContentEntry[] contentEntries = models[i].getContentEntries();
      for (final ContentEntry contentEntry : contentEntries) {
        final VirtualFile contentRoot = contentEntry.getFile();
        if (contentRoot == null) continue;
        final Set<VirtualFile> included = new HashSet<>();
        VfsUtilCore.visitChildrenRecursively(contentRoot, new VirtualFileVisitor<Void>() {
          @NotNull
          @Override
          public Result visitFileEx(@NotNull VirtualFile fileOrDir) {
            String relativeName = VfsUtilCore.getRelativePath(fileOrDir, contentRoot, '/');
            
            for (Pattern module : excludePatterns.keySet()) {
              if (module == null || module.matcher(moduleName).matches()) {
                final Set<Pattern> dirPatterns = excludePatterns.get(module);
                for (Pattern pattern : dirPatterns) {
                  if (pattern.matcher(relativeName).matches()) {
                    contentEntry.addExcludeFolder(fileOrDir);
                    return relativeName.isEmpty() ? CONTINUE : SKIP_CHILDREN;
                  }
                }
              }
            }
            if (includePatterns.isEmpty()) return CONTINUE;
            for (Pattern module : includePatterns.keySet()) {
              if (module == null || module.matcher(moduleName).matches()) {
                final Set<Pattern> dirPatterns = includePatterns.get(module);
                for (Pattern pattern : dirPatterns) {
                  if (pattern.matcher(relativeName).matches()) {
                    included.add(fileOrDir);
                    return CONTINUE;
                  }
                }
              }
            }
            return CONTINUE;
          }
        });
        processIncluded(contentEntry, included);
      }
    }

    ApplicationManager.getApplication().runWriteAction(() -> ModifiableModelCommitter.multiCommit(models, modulesModel));
  }

  public static void processIncluded(final ContentEntry contentEntry, final Set<? extends VirtualFile> included) {
    if (included.isEmpty()) return;
    final Set<VirtualFile> parents = new HashSet<>();
    for (VirtualFile file : included) {
      if (Comparing.equal(file, contentEntry.getFile())) return;
      final VirtualFile parent = file.getParent();
      if (parent == null || parents.contains(parent)) continue;
      parents.add(parent);
      for (VirtualFile toExclude : parent.getChildren()) {  // if it will ever dead-loop on symlink blame anna.kozlova
        boolean toExcludeSibling = true;
        for (VirtualFile includeRoot : included) {
          if (VfsUtilCore.isAncestor(toExclude, includeRoot, false)) {
            toExcludeSibling = false;
            break;
          }
        }
        if (toExcludeSibling) {
          contentEntry.addExcludeFolder(toExclude);
        }
      }
    }
    processIncluded(contentEntry, parents);
  }

  /**
   * Parses patterns for exclude items.
   *
   * @param propertyKey system property key for pattern
   * @return A map in the form {@code ModulePattern -> DirectoryPattern*}.
   *         ModulePattern may be null (meaning that a directory pattern is applied to all modules).
   */
  public static Map<Pattern, Set<Pattern>> loadPatterns(@NonNls String propertyKey) {
    final Map<Pattern, Set<Pattern>> result = new HashMap<>();
    final String patterns = System.getProperty(propertyKey);
    if (patterns != null) {
      final String[] pathPatterns = patterns.split(";");
      for (String excludedPattern : pathPatterns) {
        String module = null;
        int idx = 0;
        if (excludedPattern.startsWith("[")) {
          idx = excludedPattern.indexOf("]") + 1;
          module = excludedPattern.substring(1, idx - 1);
        }
        final Pattern modulePattern = module != null ? Pattern.compile(StringUtil.replace(module, "*", ".*")) : null;
        final Pattern pattern = Pattern.compile(FileUtil.convertAntToRegexp(excludedPattern.substring(idx)));
        Set<Pattern> dirPatterns = result.get(modulePattern);
        if (dirPatterns == null) {
          dirPatterns = new HashSet<>();
          result.put(modulePattern, dirPatterns);
        }
        dirPatterns.add(pattern);
      }
    }
    return result;
  }
}