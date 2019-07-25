/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.*;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 */
public class CompileModuleChunkTarget extends CompositeGenerator {

  public CompileModuleChunkTarget(final Project project,
                                  ModuleChunk moduleChunk,
                                  VirtualFile[] sourceRoots,
                                  VirtualFile[] testSourceRoots,
                                  File baseDir,
                                  GenerationOptions genOptions) {
    final String moduleChunkName = moduleChunk.getName();
    //noinspection HardCodedStringLiteral
    final Tag compilerArgs = new Tag("compilerarg", Couple.of("line", BuildProperties.propertyRef(
      BuildProperties.getModuleChunkCompilerArgsProperty(moduleChunkName))));
    //noinspection HardCodedStringLiteral
    final Couple<String> classpathRef = Couple.of("refid", BuildProperties.getClasspathProperty(moduleChunkName));
    final Tag classpathTag = new Tag("classpath", classpathRef);
    //noinspection HardCodedStringLiteral
    final Tag bootclasspathTag =
      new Tag("bootclasspath", Couple.of("refid", BuildProperties.getBootClasspathProperty(moduleChunkName)));
    final PatternSetRef compilerExcludes = new PatternSetRef(BuildProperties.getExcludedFromCompilationProperty(moduleChunkName));

    final String mainTargetName = BuildProperties.getCompileTargetName(moduleChunkName);
    final @NonNls String productionTargetName = mainTargetName + ".production";
    final @NonNls String testsTargetName = mainTargetName + ".tests";

    final ChunkCustomCompilerExtension[] customCompilers = moduleChunk.getCustomCompilers();
    final String customCompilersDependency = customCompilers.length != 0 || genOptions.enableFormCompiler ?
                                             BuildProperties.TARGET_REGISTER_CUSTOM_COMPILERS : "";
    final int modulesCount = moduleChunk.getModules().length;
    Target mainTarget = new Target(mainTargetName, productionTargetName + "," + testsTargetName,
                                   CompilerBundle.message("generated.ant.build.compile.modules.main.target.comment", modulesCount,
                                                          moduleChunkName), null);
    String dependenciesProduction = getChunkDependenciesString(moduleChunk);
    if (customCompilersDependency.length() > 0) {
      if (dependenciesProduction != null && dependenciesProduction.length() > 0) {
        dependenciesProduction = customCompilersDependency + "," + dependenciesProduction;
      }
      else {
        dependenciesProduction = customCompilersDependency;
      }
    }
    Target productionTarget = new Target(productionTargetName, dependenciesProduction,
                                         CompilerBundle.message("generated.ant.build.compile.modules.production.classes.target.comment",
                                                                modulesCount, moduleChunkName), null);
    String dependenciesTests = (customCompilersDependency.length() != 0 ? customCompilersDependency + "," : "") + productionTargetName;
    Target testsTarget = new Target(testsTargetName, dependenciesTests,
                                    CompilerBundle.message("generated.ant.build.compile.modules.tests.target.comment", modulesCount,
                                                           moduleChunkName), BuildProperties.PROPERTY_SKIP_TESTS);

    if (sourceRoots.length > 0) {
      final String outputPathRef = BuildProperties.propertyRef(BuildProperties.getOutputPathProperty(moduleChunkName));
      final Tag srcTag = new Tag("src", Couple.of("refid", BuildProperties.getSourcepathProperty(moduleChunkName)));
      productionTarget.add(new Mkdir(outputPathRef));
      createCustomCompilerTasks(project, moduleChunk, genOptions, false, customCompilers, compilerArgs, bootclasspathTag,
                                classpathTag, compilerExcludes, srcTag, outputPathRef, productionTarget);
      if (customCompilers.length == 0 || genOptions.enableFormCompiler) {
        final Javac javac = new Javac(genOptions, moduleChunk, outputPathRef);
        javac.add(compilerArgs);
        javac.add(bootclasspathTag);
        javac.add(classpathTag);
        javac.add(srcTag);
        javac.add(compilerExcludes);
        productionTarget.add(javac);
      }
      productionTarget.add(createCopyTask(project, moduleChunk, sourceRoots, outputPathRef, baseDir, genOptions));
    }

    if (testSourceRoots.length > 0) {

      final String testOutputPathRef = BuildProperties.propertyRef(BuildProperties.getOutputPathForTestsProperty(moduleChunkName));
      final Tag srcTag = new Tag("src", Couple.of("refid", BuildProperties.getTestSourcepathProperty(moduleChunkName)));
      final Couple<String> testClasspathRef = Couple.of("refid", BuildProperties.getTestClasspathProperty(moduleChunkName));
      final Tag testClassPath = new Tag("classpath", testClasspathRef);
      testsTarget.add(new Mkdir(testOutputPathRef));
      createCustomCompilerTasks(project, moduleChunk, genOptions, true, customCompilers, compilerArgs, bootclasspathTag,
                                testClassPath, compilerExcludes, srcTag, testOutputPathRef, testsTarget);
      if (customCompilers.length == 0 || genOptions.enableFormCompiler) {
        final Javac javac = new Javac(genOptions, moduleChunk, testOutputPathRef);
        javac.add(compilerArgs);
        javac.add(bootclasspathTag);
        javac.add(testClassPath);
        javac.add(srcTag);
        javac.add(compilerExcludes);
        testsTarget.add(javac);
      }
      testsTarget.add(createCopyTask(project, moduleChunk, testSourceRoots, testOutputPathRef, baseDir, genOptions));
    }

    add(mainTarget);
    add(productionTarget, 1);
    add(testsTarget, 1);
  }

  /**
   * Create custom compiler tasks
   *
   * @param project          the project
   * @param moduleChunk      the module chunk
   * @param genOptions       generation options
   * @param compileTests     if true tests are being compiled
   * @param customCompilers  an array of custom compilers for this chunk
   * @param compilerArgs     the javac compiler arguments
   * @param bootclasspathTag the boot classpath element for the javac compiler
   * @param classpathTag     the classpath tag for the javac compiler
   * @param compilerExcludes the compiler excluded tag
   * @param srcTag           the source tag
   * @param outputPathRef    the output path references
   * @param target           the target where to add custom compiler
   */
  private static void createCustomCompilerTasks(Project project,
                                                ModuleChunk moduleChunk,
                                                GenerationOptions genOptions,
                                                boolean compileTests,
                                                ChunkCustomCompilerExtension[] customCompilers,
                                                Tag compilerArgs,
                                                Tag bootclasspathTag,
                                                Tag classpathTag,
                                                PatternSetRef compilerExcludes,
                                                Tag srcTag,
                                                String outputPathRef,
                                                Target target) {
    if (customCompilers.length > 1) {
      target.add(new Tag("fail", Couple.of("message", CompilerBundle.message(
        "generated.ant.build.compile.modules.fail.custom.compilers"))));
    }
    for (ChunkCustomCompilerExtension ext : customCompilers) {
      ext.generateCustomCompile(project, moduleChunk, genOptions, compileTests, target, compilerArgs, bootclasspathTag,
                                classpathTag, compilerExcludes, srcTag, outputPathRef);
    }
  }

  private static String getChunkDependenciesString(ModuleChunk moduleChunk) {
    final StringBuilder moduleDependencies = new StringBuilder();
    final ModuleChunk[] dependencies = moduleChunk.getDependentChunks();
    for (int idx = 0; idx < dependencies.length; idx++) {
      final ModuleChunk dependency = dependencies[idx];
      if (idx > 0) {
        moduleDependencies.append(",");
      }
      moduleDependencies.append(BuildProperties.getCompileTargetName(dependency.getName()));
    }
    return moduleDependencies.toString();
  }

  private static Generator createCopyTask(final Project project,
                                          ModuleChunk chunk,
                                          VirtualFile[] sourceRoots,
                                          String toDir,
                                          File baseDir,
                                          final GenerationOptions genOptions) {
    //noinspection HardCodedStringLiteral
    final Tag filesSelector = new Tag("type", Couple.of("type", "file"));
    final PatternSetRef excludes = CompilerExcludes.isAvailable(project) ? new PatternSetRef(
      BuildProperties.getExcludedFromCompilationProperty(chunk.getName())) : null;
    final PatternSetRef resourcePatternsPatternSet = new PatternSetRef(BuildProperties.PROPERTY_COMPILER_RESOURCE_PATTERNS);
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final CompositeGenerator composite = new CompositeGenerator();
    final Map<String, Copy> outputDirToTaskMap = new HashMap<>();
    for (final VirtualFile root : sourceRoots) {
      final String packagePrefix = fileIndex.getPackageNameByDirectory(root);
      final String targetDir =
        packagePrefix != null && packagePrefix.length() > 0 ? toDir + "/" + packagePrefix.replace('.', '/') : toDir;
      Copy copy = outputDirToTaskMap.get(targetDir);
      if (copy == null) {
        copy = new Copy(targetDir);
        outputDirToTaskMap.put(targetDir, copy);
        composite.add(copy);
      }
      final FileSet fileSet = new FileSet(
        GenerationUtils.toRelativePath(root, baseDir, BuildProperties.getModuleChunkBasedirProperty(chunk), genOptions));
      fileSet.add(resourcePatternsPatternSet);
      fileSet.add(filesSelector);
      if (excludes != null) {
        fileSet.add(excludes);
      }
      copy.add(fileSet);
    }
    return composite;
  }
}
