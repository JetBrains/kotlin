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

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LibraryGroupNode extends ProjectViewNode<LibraryGroupElement> {

  public LibraryGroupNode(Project project, @NotNull LibraryGroupElement value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode> getChildren() {
    Module module = getValue().getModule();
    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final List<AbstractTreeNode> children = new ArrayList<>();
    final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
    for (final OrderEntry orderEntry : orderEntries) {
      if (orderEntry instanceof LibraryOrderEntry) {
        final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
        final Library library = libraryOrderEntry.getLibrary();
        if (library == null) {
          continue;
        }
        final String libraryName = library.getName();
        if (libraryName == null || libraryName.length() == 0) {
          addLibraryChildren(libraryOrderEntry, children, getProject(), this);
        }
        else {
          children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(module, libraryOrderEntry), getSettings()));
        }
      }
      else if (orderEntry instanceof JdkOrderEntry) {
        final JdkOrderEntry jdkOrderEntry = (JdkOrderEntry)orderEntry;
        final Sdk jdk = jdkOrderEntry.getJdk();
        if (jdk != null) {
          children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(module, jdkOrderEntry), getSettings()));
        }
      }
    }
    return children;
  }

  public static void addLibraryChildren(final LibraryOrSdkOrderEntry entry, final List<? super AbstractTreeNode> children, Project project, ProjectViewNode node) {
    final PsiManager psiManager = PsiManager.getInstance(project);
    VirtualFile[] files =
      entry instanceof LibraryOrderEntry ? getLibraryRoots((LibraryOrderEntry)entry) : entry.getRootFiles(OrderRootType.CLASSES);
    for (final VirtualFile file : files) {
      if (!file.isValid()) continue;
      if (file.isDirectory()) {
        final PsiDirectory psiDir = psiManager.findDirectory(file);
        if (psiDir == null) {
          continue;
        }
        children.add(new PsiDirectoryNode(project, psiDir, node.getSettings()));
      }
      else {
        final PsiFile psiFile = psiManager.findFile(file);
        if (psiFile == null) continue;
        children.add(new PsiFileNode(project, psiFile, node.getSettings()));
      }
    }
  }


  @Override
  public String getTestPresentation() {
    return "Libraries";
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
    if (!index.isInLibrary(file)) {
      return false;
    }

    return someChildContainsFile(file, false);
  }

  @Override
  public void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(IdeBundle.message("node.projectview.libraries"));
    presentation.setIcon(PlatformIcons.LIBRARY_ICON);
  }

  @Override
  public boolean canNavigate() {
    return ProjectSettingsService.getInstance(myProject).canOpenModuleLibrarySettings();
  }

  @Override
  public void navigate(final boolean requestFocus) {
    Module module = getValue().getModule();
    ProjectSettingsService.getInstance(myProject).openModuleLibrarySettings(module);
  }

  @NotNull
  public static VirtualFile[] getLibraryRoots(@NotNull LibraryOrderEntry orderEntry) {
    Library library = orderEntry.getLibrary();
    if (library == null) return VirtualFile.EMPTY_ARRAY;
    OrderRootType[] rootTypes = LibraryType.DEFAULT_EXTERNAL_ROOT_TYPES;
    if (library instanceof LibraryEx) {
      if (((LibraryEx)library).isDisposed()) return VirtualFile.EMPTY_ARRAY;
      PersistentLibraryKind<?> libKind = ((LibraryEx)library).getKind();
      if (libKind != null) {
        rootTypes = LibraryType.findByKind(libKind).getExternalRootTypes();
      }
    }
    final ArrayList<VirtualFile> files = new ArrayList<>();
    for (OrderRootType rootType : rootTypes) {
      files.addAll(Arrays.asList(library.getFiles(rootType)));
    }
    return VfsUtilCore.toVirtualFileArray(files);
  }
}
