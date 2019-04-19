/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.libraries.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Describes an 'attach' button in the library roots component.
 *
 * @see ChooserBasedAttachRootButtonDescriptor
 * @author nik
 */
public abstract class AttachRootButtonDescriptor {
  private final OrderRootType myOrderRootType;
  protected final String myButtonText;
  private final Icon myToolbarIcon;

  /**
   * Creates a descriptor for 'attach' button shown in popup when user click on '+' button.
   * Consider using {@link #AttachRootButtonDescriptor(com.intellij.openapi.roots.OrderRootType, javax.swing.Icon, String)} instead.
   */
  protected AttachRootButtonDescriptor(@NotNull OrderRootType orderRootType, @NotNull String buttonText) {
    myOrderRootType = orderRootType;
    myButtonText = buttonText;
    myToolbarIcon = null;
  }

  /**
   * Creates a descriptor for 'attach' button shown in toolbar of a library editor
   */
  protected AttachRootButtonDescriptor(@NotNull OrderRootType orderRootType, @NotNull Icon toolbarIcon, @NotNull String description) {
    myOrderRootType = orderRootType;
    myButtonText = description;
    myToolbarIcon = toolbarIcon;
  }

  public abstract VirtualFile[] selectFiles(@NotNull JComponent parent, @Nullable VirtualFile initialSelection,
                                            @Nullable Module contextModule, @NotNull LibraryEditor libraryEditor);

  public String getButtonText() {
    return myButtonText;
  }

  public OrderRootType getRootType() {
    return myOrderRootType;
  }

  public boolean addAsJarDirectories() {
    return false;
  }

  @NotNull
  public VirtualFile[] scanForActualRoots(@NotNull VirtualFile[] rootCandidates, JComponent parent) {
    return rootCandidates;
  }

  @Nullable
  public Icon getToolbarIcon() {
    return myToolbarIcon;
  }
}
