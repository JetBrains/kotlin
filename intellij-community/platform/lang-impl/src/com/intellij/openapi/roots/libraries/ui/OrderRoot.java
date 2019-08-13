/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.roots.libraries.ui;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class OrderRoot {
  private final VirtualFile myFile;
  private final OrderRootType myType;
  private final boolean myJarDirectory;

  public OrderRoot(@NotNull VirtualFile file, @NotNull OrderRootType type) {
    this(file, type, false);
  }

  public OrderRoot(@NotNull VirtualFile file, @NotNull OrderRootType type, boolean jarDirectory) {
    myFile = file;
    myType = type;
    myJarDirectory = jarDirectory;
  }

  @NotNull
  public VirtualFile getFile() {
    return myFile;
  }

  @NotNull
  public OrderRootType getType() {
    return myType;
  }

  public boolean isJarDirectory() {
    return myJarDirectory;
  }
}
