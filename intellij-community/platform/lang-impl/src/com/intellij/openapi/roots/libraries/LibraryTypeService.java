/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.libraries;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public abstract class LibraryTypeService {
  public static LibraryTypeService getInstance() {
    return ServiceManager.getService(LibraryTypeService.class);
  }

  @Nullable
  public abstract NewLibraryConfiguration createLibraryFromFiles(@NotNull LibraryRootsComponentDescriptor descriptor,
                                                                 @NotNull JComponent parentComponent,
                                                                 @Nullable VirtualFile contextDirectory,
                                                                 @Nullable LibraryType<?> type,
                                                                 final @Nullable Project project);
}
