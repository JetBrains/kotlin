// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.PatternSetRef;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The extension point for the custom compilers
 */
public abstract class ChunkCustomCompilerExtension {
  /**
   * Extension point name
   */
  public static final ExtensionPointName<ChunkCustomCompilerExtension> EP_NAME =
    ExtensionPointName.create("com.intellij.antCustomCompiler");
  /**
   * Comparator that compares extensions using names. It is used for make order of elements predictable.
   */
  protected static final Comparator<ChunkCustomCompilerExtension> COMPARATOR =
    Comparator.comparing(o -> o.getClass().getName());

  /**
   * Generate custom compile task inside compile target. Note that if more
   * than one extension requested custom compilation, all of them are included into ant
   * build and fail task is added to the compile target.
   *
   * @param project          the context project
   * @param chunk            the chunk to compile
   * @param genOptions       an options to compile
   * @param generator        a generator where custom compilation tasks will be added.
   * @param compileTests     if true, tests are compiled
   * @param compilerArgs     the javac compilier arguements
   * @param bootclasspathTag the boot classpath element for the javac compiler
   * @param classpathTag     the classpath tag for the javac compiler
   * @param compilerExcludes the compiler excluded tag
   * @param srcTag           the soruce tag
   * @param outputPathRef    the output path references
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public abstract void generateCustomCompile(Project project,
                                             ModuleChunk chunk,
                                             GenerationOptions genOptions,
                                             boolean compileTests,
                                             CompositeGenerator generator,
                                             Tag compilerArgs,
                                             Tag bootclasspathTag,
                                             Tag classpathTag,
                                             PatternSetRef compilerExcludes,
                                             Tag srcTag,
                                             String outputPathRef);

  /**
   * Generate task registration for custom compiler if needed.
   *
   * @param project    the context project
   * @param genOptions an options to compile
   * @param generator  a generator where custom compilation tasks will be added.
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public abstract void generateCustomCompilerTaskRegistration(Project project, GenerationOptions genOptions, CompositeGenerator generator);

  /**
   * Allows to check if the custom compilation task is required to compile module sources.
   * Such task should be able to process standard java sources as well.
   *
   * @param chunk a chunk to check
   * @return true if the facet requires custom comiplation.
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public abstract boolean hasCustomCompile(final ModuleChunk chunk);

  /**
   * Get custom compilation providers for module chunk
   *
   * @param chunk a chunk to examine
   * @return a list of custom compilators
   */
  public static ChunkCustomCompilerExtension[] getCustomCompile(ModuleChunk chunk) {
    final ChunkCustomCompilerExtension[] extensions = Extensions.getRootArea().getExtensionPoint(EP_NAME).getExtensions();
    return Arrays.stream(extensions)
      .filter(extension -> extension.hasCustomCompile(chunk))
      .sorted(COMPARATOR).toArray(ChunkCustomCompilerExtension[]::new);
  }
}
