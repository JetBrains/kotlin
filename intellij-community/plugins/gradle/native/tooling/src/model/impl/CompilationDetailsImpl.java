// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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

  private File myCompilerExecutable;
  private ExternalTask myCompileTask;
  private File myCompileWorkingDir;
  @NotNull
  private List<File> myFrameworkSearchPaths;
  @NotNull
  private List<File> mySystemHeaderSearchPaths;
  @NotNull
  private List<File> myUserHeaderSearchPaths;
  @NotNull
  private Set<SourceFile> mySources;
  @NotNull
  private Set<File> myHeaderDirs;
  @NotNull
  private Set<MacroDirective> myMacroDefines;
  @NotNull
  private Set<String> myMacroUndefines;
  @NotNull
  private List<String> myAdditionalArgs;

  public CompilationDetailsImpl() {
    myFrameworkSearchPaths = Collections.emptyList();
    mySystemHeaderSearchPaths = Collections.emptyList();
    myUserHeaderSearchPaths = Collections.emptyList();
    mySources = Collections.emptySet();
    myHeaderDirs = Collections.emptySet();
    myMacroDefines = Collections.emptySet();
    myMacroUndefines = Collections.emptySet();
    myAdditionalArgs = Collections.emptyList();
  }

  public CompilationDetailsImpl(CompilationDetails compilationDetails) {
    myCompilerExecutable = compilationDetails.getCompilerExecutable();
    myCompileTask = new DefaultExternalTask(compilationDetails.getCompileTask());
    myCompileWorkingDir = compilationDetails.getCompileWorkingDir();
    myFrameworkSearchPaths = new ArrayList<File>(compilationDetails.getFrameworkSearchPaths());
    mySystemHeaderSearchPaths = new ArrayList<File>(compilationDetails.getSystemHeaderSearchPaths());
    myUserHeaderSearchPaths = new ArrayList<File>(compilationDetails.getUserHeaderSearchPaths());
    mySources = new LinkedHashSet<SourceFile>(compilationDetails.getSources().size());
    for (SourceFile source : compilationDetails.getSources()) {
      mySources.add(new SourceFileImpl(source));
    }
    myHeaderDirs = new LinkedHashSet<File>(compilationDetails.getHeaderDirs());

    myMacroDefines = new LinkedHashSet<MacroDirective>(compilationDetails.getMacroDefines().size());
    for (MacroDirective macroDirective : compilationDetails.getMacroDefines()) {
      myMacroDefines.add(new MacroDirectiveImpl(macroDirective));
    }
    myMacroUndefines = new LinkedHashSet<String>(compilationDetails.getMacroUndefines());
    myAdditionalArgs = new ArrayList<String>(compilationDetails.getAdditionalArgs());
  }

  @Override
  public ExternalTask getCompileTask() {
    return myCompileTask;
  }

  public void setCompileTask(ExternalTask compileTask) {
    myCompileTask = compileTask;
  }

  @Nullable
  @Override
  public File getCompilerExecutable() {
    return myCompilerExecutable;
  }

  public void setCompilerExecutable(File compilerExecutable) {
    myCompilerExecutable = compilerExecutable;
  }

  @Override
  public File getCompileWorkingDir() {
    return myCompileWorkingDir;
  }

  public void setCompileWorkingDir(File compileWorkingDir) {
    myCompileWorkingDir = compileWorkingDir;
  }

  @NotNull
  @Override
  public List<File> getFrameworkSearchPaths() {
    return myFrameworkSearchPaths;
  }

  public void setFrameworkSearchPaths(@NotNull List<File> frameworkSearchPaths) {
    myFrameworkSearchPaths = frameworkSearchPaths;
  }


  @NotNull
  @Override
  public List<File> getSystemHeaderSearchPaths() {
    return mySystemHeaderSearchPaths;
  }

  public void setSystemHeaderSearchPaths(@NotNull List<File> systemHeaderSearchPaths) {
    mySystemHeaderSearchPaths = systemHeaderSearchPaths;
  }


  @NotNull
  @Override
  public List<File> getUserHeaderSearchPaths() {
    return myUserHeaderSearchPaths;
  }

  public void setUserHeaderSearchPaths(@NotNull List<File> userHeaderSearchPaths) {
    myUserHeaderSearchPaths = userHeaderSearchPaths;
  }


  @NotNull
  @Override
  public Set<? extends SourceFile> getSources() {
    return mySources;
  }

  public void setSources(@NotNull Set<SourceFile> sources) {
    mySources = sources;
  }


  @NotNull
  @Override
  public Set<File> getHeaderDirs() {
    return myHeaderDirs;
  }

  public void setHeaderDirs(@NotNull Set<File> headerDirs) {
    myHeaderDirs = headerDirs;
  }


  @NotNull
  @Override
  public Set<? extends MacroDirective> getMacroDefines() {
    return myMacroDefines;
  }

  public void setMacroDefines(@NotNull Set<MacroDirective> macroDefines) {
    myMacroDefines = macroDefines;
  }


  @NotNull
  @Override
  public Set<String> getMacroUndefines() {
    return myMacroUndefines;
  }

  public void setMacroUndefines(@NotNull Set<String> macroUndefines) {
    myMacroUndefines = macroUndefines;
  }


  @NotNull
  @Override
  public List<String> getAdditionalArgs() {
    return myAdditionalArgs;
  }

  public void setAdditionalArgs(@NotNull List<String> additionalArgs) {
    myAdditionalArgs = additionalArgs;
  }

}
