// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.DefaultExternalTask;
import org.jetbrains.plugins.gradle.model.ExternalTask;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CompilationDetails;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.MacroDirective;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.SourceFile;

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class CompilationDetailsImpl implements CompilationDetails {
  private File compilerExecutable;

  private ExternalTask compileTask;

  private File compileWorkingDir;
  @NotNull
  private List<File> frameworkSearchPaths;
  @NotNull
  private List<File> systemHeaderSearchPaths;
  @NotNull
  private List<File> userHeaderSearchPaths;
  @NotNull
  private Set<SourceFile> sources;
  @NotNull
  private Set<File> headerDirs;
  @NotNull
  private Set<MacroDirective> macroDefines;
  @NotNull
  private Set<String> macroUndefines;
  @NotNull
  private List<String> additionalArgs;

  public CompilationDetailsImpl() {
    frameworkSearchPaths = Collections.emptyList();
    systemHeaderSearchPaths = Collections.emptyList();
    userHeaderSearchPaths = Collections.emptyList();
    sources = Collections.emptySet();
    headerDirs = Collections.emptySet();
    macroDefines = Collections.emptySet();
    macroUndefines = Collections.emptySet();
    additionalArgs = Collections.emptyList();
  }

  public CompilationDetailsImpl(CompilationDetails compilationDetails) {
    compilerExecutable = compilationDetails.getCompilerExecutable();
    compileTask = new DefaultExternalTask(compilationDetails.getCompileTask());
    compileWorkingDir = compilationDetails.getCompileWorkingDir();
    frameworkSearchPaths = new ArrayList<File>(compilationDetails.getFrameworkSearchPaths());
    systemHeaderSearchPaths = new ArrayList<File>(compilationDetails.getSystemHeaderSearchPaths());
    userHeaderSearchPaths = new ArrayList<File>(compilationDetails.getUserHeaderSearchPaths());
    sources = new LinkedHashSet<SourceFile>(compilationDetails.getSources().size());
    for (SourceFile source : compilationDetails.getSources()) {
      sources.add(new SourceFileImpl(source));
    }
    headerDirs = new LinkedHashSet<File>(compilationDetails.getHeaderDirs());

    macroDefines = new LinkedHashSet<MacroDirective>(compilationDetails.getMacroDefines().size());
    for (MacroDirective macroDirective : compilationDetails.getMacroDefines()) {
      macroDefines.add(new MacroDirectiveImpl(macroDirective));
    }
    macroUndefines = new LinkedHashSet<String>(compilationDetails.getMacroUndefines());
    additionalArgs = new ArrayList<String>(compilationDetails.getAdditionalArgs());
  }

  @Override
  public ExternalTask getCompileTask() {
    return compileTask;
  }

  public void setCompileTask(ExternalTask compileTask) {
    this.compileTask = compileTask;
  }

  @Nullable
  @Override
  public File getCompilerExecutable() {
    return compilerExecutable;
  }

  public void setCompilerExecutable(File compilerExecutable) {
    this.compilerExecutable = compilerExecutable;
  }

  @Override
  public File getCompileWorkingDir() {
    return compileWorkingDir;
  }

  public void setCompileWorkingDir(File compileWorkingDir) {
    this.compileWorkingDir = compileWorkingDir;
  }

  @NotNull
  @Override
  public List<File> getFrameworkSearchPaths() {
    return frameworkSearchPaths;
  }

  public void setFrameworkSearchPaths(@NotNull List<File> frameworkSearchPaths) {
    this.frameworkSearchPaths = frameworkSearchPaths;
  }

  @NotNull
  @Override
  public List<File> getSystemHeaderSearchPaths() {
    return systemHeaderSearchPaths;
  }

  public void setSystemHeaderSearchPaths(@NotNull List<File> systemHeaderSearchPaths) {
    this.systemHeaderSearchPaths = systemHeaderSearchPaths;
  }

  @NotNull
  @Override
  public List<File> getUserHeaderSearchPaths() {
    return userHeaderSearchPaths;
  }

  public void setUserHeaderSearchPaths(@NotNull List<File> userHeaderSearchPaths) {
    this.userHeaderSearchPaths = userHeaderSearchPaths;
  }

  @NotNull
  @Override
  public Set<? extends SourceFile> getSources() {
    return sources;
  }

  public void setSources(@NotNull Set<SourceFile> sources) {
    this.sources = sources;
  }

  @NotNull
  @Override
  public Set<File> getHeaderDirs() {
    return headerDirs;
  }

  public void setHeaderDirs(@NotNull Set<File> headerDirs) {
    this.headerDirs = headerDirs;
  }


  @NotNull
  @Override
  public Set<? extends MacroDirective> getMacroDefines() {
    return macroDefines;
  }

  public void setMacroDefines(@NotNull Set<MacroDirective> macroDefines) {
    this.macroDefines = macroDefines;
  }

  @NotNull
  @Override
  public Set<String> getMacroUndefines() {
    return macroUndefines;
  }

  public void setMacroUndefines(@NotNull Set<String> macroUndefines) {
    this.macroUndefines = macroUndefines;
  }


  @NotNull
  @Override
  public List<String> getAdditionalArgs() {
    return additionalArgs;
  }

  public void setAdditionalArgs(@NotNull List<String> additionalArgs) {
    this.additionalArgs = additionalArgs;
  }
}
