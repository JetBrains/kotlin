// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerEncodingService;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.EffectiveLanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JpsJavaSdkType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Module chunk consists of interdependent modules.
 *
 * @author Eugene Zhuravlev
 */
public class ModuleChunk {
  /* Modules in the chunk */
  private final Module[] myModules;
  /* An array of custom compilation providers. */
  private final ChunkCustomCompilerExtension[] myCustomCompilers;
  /* The main module in the chunk (guessed by heuristic or selected by user) */
  private Module myMainModule;
  /* Chunk dependencies */
  private ModuleChunk[] myDependentChunks;
  private File myBaseDir = null;

  public ModuleChunk(Module[] modules) {
    myModules = modules;
    Arrays.sort(myModules, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    myMainModule = myModules[0];
    myCustomCompilers = ChunkCustomCompilerExtension.getCustomCompile(this);
  }

  public String getName() {
    return myMainModule.getName();
  }

  /**
   * @return an array of custom compilers for the module chunk
   */
  public ChunkCustomCompilerExtension[] getCustomCompilers() {
    return myCustomCompilers;
  }

  public Module[] getModules() {
    return myModules;
  }

  @Nullable
  public String getOutputDirUrl() {
    CompilerModuleExtension extension = CompilerModuleExtension.getInstance(myMainModule);
    return extension != null ? extension.getCompilerOutputUrl() : null;
  }

  @Nullable
  public String getTestsOutputDirUrl() {
    CompilerModuleExtension extension = CompilerModuleExtension.getInstance(myMainModule);
    return extension != null ? extension.getCompilerOutputUrlForTests() : null;
  }

  public boolean isJdkInherited() {
    return ModuleRootManager.getInstance(myMainModule).isSdkInherited();
  }

  @Nullable
  public Sdk getJdk() {
    return ModuleRootManager.getInstance(myMainModule).getSdk();
  }

  public ModuleChunk[] getDependentChunks() {
    return myDependentChunks;
  }

  public void setDependentChunks(ModuleChunk[] dependentChunks) {
    myDependentChunks = dependentChunks;
  }

  public File getBaseDir() {
    return myBaseDir != null ? myBaseDir : new File(myMainModule.getModuleFilePath()).getParentFile();
  }

  public void setBaseDir(File baseDir) {
    myBaseDir = baseDir;
  }

  public void setMainModule(Module module) {
    myMainModule = module;
  }

  public Project getProject() {
    return myMainModule.getProject();
  }

  public String getChunkSpecificCompileOptions() {
    List<String> options = new ArrayList<>();

    Charset encoding = CompilerEncodingService.getInstance(getProject()).getPreferredModuleEncoding(myMainModule);
    if (encoding != null) {
      options.add("-encoding");
      options.add(encoding.name());
    }

    LanguageLevel languageLevel = ReadAction.compute(() -> EffectiveLanguageLevelUtil.getEffectiveLanguageLevel(myMainModule));
    String sourceVersion = JpsJavaSdkType.complianceOption(languageLevel.toJavaVersion());
    options.add("-source");
    options.add(sourceVersion);

    if (languageLevel.isPreview()) {
      options.add("--enable-preview");
    }

    String bytecodeTarget = CompilerConfiguration.getInstance(getProject()).getBytecodeTargetLevel(myMainModule);
    if (StringUtil.isEmpty(bytecodeTarget)) {
      // according to IDEA rule: if not specified explicitly, set target to be the same as source language level
      bytecodeTarget = sourceVersion;
    }
    options.add("-target");
    options.add(bytecodeTarget);

    return StringUtil.join(options, " ");
  }

  public boolean contains(final Module module) {
    return ArrayUtil.contains(module, myModules);
  }
}