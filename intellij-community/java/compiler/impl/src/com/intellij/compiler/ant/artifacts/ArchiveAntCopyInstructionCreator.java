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
package com.intellij.compiler.ant.artifacts;

import com.intellij.compiler.ant.Generator;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.taskdefs.ZipFileSet;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class ArchiveAntCopyInstructionCreator implements AntCopyInstructionCreator {
  private final String myPrefix;

  public ArchiveAntCopyInstructionCreator(String prefix) {
    myPrefix = prefix;
  }

  @Override
  @NotNull
  public Tag createDirectoryContentCopyInstruction(@NotNull String dirPath) {
    return new ZipFileSet(dirPath, myPrefix, true);
  }

  @Override
  @NotNull
  public Tag createFileCopyInstruction(@NotNull String filePath, String outputFileName) {
    final String relativePath = myPrefix + "/" + outputFileName;
    return new ZipFileSet(filePath, relativePath, false);
  }

  @Override
  @NotNull
  public AntCopyInstructionCreator subFolder(@NotNull String directoryName) {
    return new ArchiveAntCopyInstructionCreator(myPrefix + "/" + directoryName);
  }

  @Override
  public Generator createSubFolderCommand(@NotNull String directoryName) {
    return null;
  }

  @NotNull
  @Override
  public Generator createExtractedDirectoryInstruction(@NotNull String jarPath) {
    return ZipFileSet.createUnpackedSet(jarPath, myPrefix, true);
  }
}
