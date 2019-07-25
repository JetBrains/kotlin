/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.groovy.GroovyBuilderExtension;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleDependency;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.jetbrains.jps.gradle.model.impl.GradleResourcesTargetType.buildModuleTargets;

public class GroovyResourcesTargetExtension implements GroovyBuilderExtension {

  @NotNull
  @Override
  public Collection<String> getCompilationClassPath(@NotNull CompileContext context, @NotNull ModuleChunk chunk) {
    return getModuleDependencies(chunk).stream()
      .flatMap(module -> buildModuleTargets(module, chunk.containsTests()).stream())
      .flatMap(target -> target.getOutputRoots(context).stream())
      .map(file -> FileUtil.toCanonicalPath(file.getPath()))
      .collect(Collectors.toSet());
  }

  private static Collection<JpsModule> getModuleDependencies(@NotNull ModuleChunk chunk) {
    return chunk.getModules().stream()
      .flatMap(module -> module.getDependenciesList().getDependencies().stream())
      .map(dep -> {
        if (dep instanceof JpsModuleDependency) return ((JpsModuleDependency)dep).getModule();
        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  @NotNull
  @Override
  public Collection<String> getCompilationUnitPatchers(@NotNull CompileContext context, @NotNull ModuleChunk chunk) {
    return Collections.emptyList();
  }
}