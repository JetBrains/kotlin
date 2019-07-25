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
package org.jetbrains.plugins.gradle.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ReferenceType;
import org.gradle.groovy.scripts.TextResourceScriptSource;
import org.gradle.internal.resource.BasicTextResourceLoader;
import org.gradle.internal.resource.TextResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.debugger.ScriptPositionManagerHelper;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.runner.GroovyScriptUtil;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GradlePositionManager extends ScriptPositionManagerHelper {
  private static final Pattern GRADLE_CLASS_PATTERN = Pattern.compile(".*_gradle_.*");
  private static final String SCRIPT_CLOSURE_PREFIX = "build_";
  private static final Key<CachedValue<Map<File, String>>> GRADLE_CLASS_NAME = Key.create("GRADLE_CLASS_NAME");

  @Override
  public boolean isAppropriateRuntimeName(@NotNull final String runtimeName) {
    return runtimeName.startsWith(SCRIPT_CLOSURE_PREFIX) || GRADLE_CLASS_PATTERN.matcher(runtimeName).matches();
  }

  @Override
  public boolean isAppropriateScriptFile(@NotNull final GroovyFile scriptFile) {
    return GroovyScriptUtil.isSpecificScriptFile(scriptFile, GradleScriptType.INSTANCE);
  }

  @Override
  @NotNull
  public String getRuntimeScriptName(@NotNull GroovyFile groovyFile) {
    VirtualFile virtualFile = groovyFile.getVirtualFile();
    if (virtualFile == null) return "";

    final Module module = ModuleUtilCore.findModuleForPsiElement(groovyFile);
    if (module == null) {
      return "";
    }

    final File scriptFile = VfsUtilCore.virtualToIoFile(virtualFile);
    final String className = CachedValuesManager.getManager(module.getProject())
                                                .getCachedValue(module, GRADLE_CLASS_NAME, new ScriptSourceMapCalculator(module), false)
                                                .get(scriptFile);
    return className == null ? "" : className;
  }

  @Nullable
  @Override
  public String customizeClassName(@NotNull PsiClass psiClass) {
    PsiFile file = psiClass.getContainingFile();
    if (file instanceof GroovyFile) {
      return getRuntimeScriptName((GroovyFile)file);
    }
    else {
      return null;
    }
  }

  @Override
  public PsiFile getExtraScriptIfNotFound(@NotNull ReferenceType refType,
                                          @NotNull String runtimeName,
                                          @NotNull Project project,
                                          @NotNull GlobalSearchScope scope) {
    String sourceFilePath = getScriptForClassName(refType);
    if (sourceFilePath == null) return null;

    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(sourceFilePath));
    if (virtualFile == null) return null;

    return PsiManager.getInstance(project).findFile(virtualFile);
  }

  @Nullable
  private static String getScriptForClassName(@NotNull ReferenceType refType) {
    try {
      final List<String> data = refType.sourcePaths(null);
      if (!data.isEmpty()) {
        return data.get(0);
      }
    }
    catch (AbsentInformationException ignored) {
    }
    return null;
  }

  private static class ScriptSourceMapCalculator implements CachedValueProvider<Map<File, String>> {
    private final Module myModule;

    ScriptSourceMapCalculator(Module module) {
      myModule = module;
    }

    @Override
    public Result<Map<File, String>> compute() {
      final Map<File, String> result = ConcurrentFactoryMap.createMap(ScriptSourceMapCalculator::calcClassName);
      return Result.create(result, ProjectRootManager.getInstance(myModule.getProject()));
    }

    @Nullable
    private static String calcClassName(File scriptFile) {
      TextResource resource = new BasicTextResourceLoader().loadFile("script", scriptFile);
      TextResourceScriptSource scriptSource = new TextResourceScriptSource(resource);
      return scriptSource.getClassName();
    }
  }
}
