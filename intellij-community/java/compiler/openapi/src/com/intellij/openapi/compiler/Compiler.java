// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.compiler;

import com.intellij.openapi.extensions.ProjectExtensionPointName;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for a custom compiler which participates in the build process and should be executed inside the IDE process.
 * @deprecated since IDEA 15 compilers need to be executed inside a separate (external) build process, see
 * <a href="http://www.jetbrains.org/intellij/sdk/docs/reference_guide/frameworks_and_external_apis/external_builder_api.html">this guide</a>
 * for details. If you need to run some code inside the IDE process before the external build process starts or after it finishes, use
 * {@link CompileTask} extension point instead. Implementations of this class aren't used by the IDE (except those which implement
 * {@link Validator} or {@link SourceInstrumentingCompiler}).
 */
@Deprecated
public interface Compiler {
  ProjectExtensionPointName<Compiler> EP_NAME = new ProjectExtensionPointName<>("com.intellij.compiler");

  /**
   * Returns the description of the compiler. All registered compilers should have unique description.
   *
   * @return the description string.
   */
  @NotNull
  String getDescription();

  /**
   * Called before compilation starts. If at least one of registered compilers returned false, compilation won't start.
   *
   * @param scope the scope on which the compilation is started.
   * @return true if everything is ok, false otherwise
   */
  boolean validateConfiguration(CompileScope scope);
}
