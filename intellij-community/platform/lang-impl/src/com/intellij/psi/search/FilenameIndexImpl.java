/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.psi.search;

import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class FilenameIndexImpl extends ScalarIndexExtension<String> {
  @NonNls static final ID<String, Void> NAME = FilenameIndex.NAME;

  @NotNull
  @Override
  public ID<String,Void> getName() {
    return NAME;
  }

  @NotNull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return inputData -> Collections.singletonMap(inputData.getFileName(), null);
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return file -> file instanceof VirtualFileSystemEntry;
  }

  @Override
  public boolean dependsOnFileContent() {
    return false;
  }

  @Override
  public boolean indexDirectories() {
    return true;
  }

  @Override
  public int getVersion() {
    return 3;
  }

  @Override
  public boolean traceKeyHashToVirtualFileMapping() {
    return true;
  }
}
