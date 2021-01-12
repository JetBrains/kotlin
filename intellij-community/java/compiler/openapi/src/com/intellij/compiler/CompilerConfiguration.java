// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.openapi.compiler.options.ExcludesConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

import java.util.List;

public abstract class CompilerConfiguration {
  public static CompilerConfiguration getInstance(@NotNull Project project) {
    return project.getService(CompilerConfiguration.class);
  }

  public abstract int getBuildProcessHeapSize(int javacPreferredHeapSize);
  public abstract void setBuildProcessHeapSize(int size);

  public abstract String getBuildProcessVMOptions();
  public abstract void setBuildProcessVMOptions(String options);

  /**
   * Specifies whether '--release' cross-compilation option should be used. Applicable to jdk 9 and later
   */
  public abstract boolean useReleaseOption();
  public abstract void setUseReleaseOption(boolean useReleaseOption);

  @Nullable
  public abstract String getProjectBytecodeTarget();
  public abstract void setProjectBytecodeTarget(String level);

  @Nullable
  public abstract String getBytecodeTargetLevel(Module module);
  public abstract void setBytecodeTargetLevel(Module module, String level);

  /**
   * Returns additional compiler options applicable to the given module, if any.
   */
  @NotNull
  public abstract List<String> getAdditionalOptions(@NotNull Module module);
  public abstract void setAdditionalOptions(@NotNull Module module, @NotNull List<String> options);

  @NotNull
  public abstract AnnotationProcessingConfiguration getAnnotationProcessingConfiguration(Module module);

  /**
   * Returns true if at least one enabled annotation processing profile exists.
   */
  public abstract boolean isAnnotationProcessorsEnabled();

  public abstract boolean isExcludedFromCompilation(VirtualFile virtualFile);
  public abstract boolean isResourceFile(VirtualFile virtualFile);
  public abstract boolean isResourceFile(String path);
  public abstract boolean isCompilableResourceFile(Project project, VirtualFile file);

  public abstract void addResourceFilePattern(String namePattern) throws MalformedPatternException;

  public abstract boolean isAddNotNullAssertions();
  public abstract void setAddNotNullAssertions(boolean enabled);

  public abstract ExcludesConfiguration getExcludedEntriesConfiguration();
}