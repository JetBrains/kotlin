/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

/**
 * @deprecated this interface is part of the obsolete build system which runs as part of the IDE process. Since IDEA 15 implementations of
 * this class aren't used by the IDE, you should integrate into 'external build system' instead
 * (http://www.jetbrains.org/intellij/sdk/docs/reference_guide/frameworks_and_external_apis/external_builder_api.html).
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
public interface TranslatingCompiler extends Compiler {

  /**
   * Defines a single file compiled by the compiler.
   */
  interface OutputItem {
    /**
     * Returns the path to the output file.
     *
     * @return absolute path of the output file ('/' slashes used)
     */
    String getOutputPath();

    /**
     * Returns the path to the source file.
     *
     * @return the source file to be compiled
     */
    VirtualFile getSourceFile();
  }

  interface OutputSink {
    /**
     *
     * @param outputRoot output directory
     * @param items output items that were successfully compiled.
     * @param filesToRecompile virtual files that should be considered as "modified" next time compilation is invoked.
     */
    void add(String outputRoot, Collection<OutputItem> items, VirtualFile[] filesToRecompile);
  }


  /**
   * Checks if the compiler can compile the specified file.
   *
   * @param file    the file to check.
   * @param context the context for the current compile operation.
   * @return true if can compile the file, false otherwise. If the method returns false, {@code file}
   *         will not be included in the list of files passed to {@link #compile(CompileContext, Chunk, VirtualFile[], OutputSink)}.
   */
  boolean isCompilableFile(VirtualFile file, CompileContext context);

  /**
   * Compiles the specified files.
   *
   * @param context the context for the current compile operation.
   * @param moduleChunk contains modules that form a cycle. If project module graph has no cycles, a chunk corresponds to a single module
   * @param files   the source files to compile that correspond to the module chunk
   * @param sink storage that accepts compiler output results
   */
  void compile(CompileContext context, Chunk<Module> moduleChunk, VirtualFile[] files, OutputSink sink);
}
