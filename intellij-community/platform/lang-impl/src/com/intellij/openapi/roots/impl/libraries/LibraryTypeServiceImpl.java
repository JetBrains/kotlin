// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.libraries.LibraryProperties;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.LibraryTypeService;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class LibraryTypeServiceImpl extends LibraryTypeService {
  private static final String DEFAULT_LIBRARY_NAME = "Unnamed";

  @Override
  public NewLibraryConfiguration createLibraryFromFiles(@NotNull LibraryRootsComponentDescriptor descriptor,
                                                        @NotNull JComponent parentComponent,
                                                        @Nullable VirtualFile contextDirectory,
                                                        LibraryType<?> type,
                                                        final Project project) {
    final FileChooserDescriptor chooserDescriptor = descriptor.createAttachFilesChooserDescriptor(null);
    chooserDescriptor.setTitle(ProjectBundle.message("chooser.title.select.library.files"));
    final VirtualFile[] rootCandidates = FileChooser.chooseFiles(chooserDescriptor, parentComponent, project, contextDirectory);
    if (rootCandidates.length == 0) {
      return null;
    }

    final List<OrderRoot> roots = RootDetectionUtil.detectRoots(Arrays.asList(rootCandidates), parentComponent, project, descriptor);
    if (roots.isEmpty()) return null;
    String name = suggestLibraryName(roots);
    return doCreate(type, name, roots);
  }

  @NotNull
  private static <P extends LibraryProperties<?>> NewLibraryConfiguration doCreate(final LibraryType<P> type, final String name, final List<? extends OrderRoot> roots) {
    return new NewLibraryConfiguration(name, type, type != null ? type.getKind().createDefaultProperties() : null) {
      @Override
      public void addRoots(@NotNull LibraryEditor editor) {
        editor.addRoots(roots);
      }
    };
  }

  @NotNull
  public static String suggestLibraryName(VirtualFile @NotNull [] classesRoots) {
    if (classesRoots.length >= 1) {
      return FileUtilRt.getNameWithoutExtension(PathUtil.getFileName(classesRoots[0].getPath()));
    }
    return DEFAULT_LIBRARY_NAME;
  }

  @NotNull
  public static String suggestLibraryName(@NotNull List<? extends OrderRoot> roots) {
    if (roots.size() >= 1) {
      return FileUtilRt.getNameWithoutExtension(PathUtil.getFileName(roots.get(0).getFile().getPath()));
    }
    return DEFAULT_LIBRARY_NAME;
  }
}
