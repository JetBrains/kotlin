// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformIcons;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ExternalLibrariesNode extends ProjectViewNode<String> {
  private static final Logger LOG = Logger.getInstance(ExternalLibrariesNode.class);

  public ExternalLibrariesNode(@NotNull Project project, ViewSettings viewSettings) {
    super(project, "External Libraries", viewSettings);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    Project project = Objects.requireNonNull(getProject());
    ProjectFileIndex index = ProjectFileIndex.getInstance(project);
    if (!index.isInLibrary(file)) return false;
    return someChildContainsFile(file, false);
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    Project project = Objects.requireNonNull(getProject());
    List<AbstractTreeNode> children = new ArrayList<>();
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    Module[] modules = ModuleManager.getInstance(project).getModules();
    Set<Library> processedLibraries = new THashSet<>();
    Set<Sdk> processedSdk = new THashSet<>();

    for (Module module : modules) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
      for (final OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof LibraryOrderEntry) {
          final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
          final Library library = libraryOrderEntry.getLibrary();
          if (library == null) continue;
          if (processedLibraries.contains(library)) continue;
          processedLibraries.add(library);

          if (!hasExternalEntries(fileIndex, libraryOrderEntry)) continue;

          final String libraryName = library.getName();
          if (libraryName == null || libraryName.length() == 0) {
            addLibraryChildren(libraryOrderEntry, children, project, this);
          }
          else {
            children.add(new NamedLibraryElementNode(project, new NamedLibraryElement(null, libraryOrderEntry), getSettings()));
          }
        }
        else if (orderEntry instanceof JdkOrderEntry) {
          final JdkOrderEntry jdkOrderEntry = (JdkOrderEntry)orderEntry;
          final Sdk jdk = jdkOrderEntry.getJdk();
          if (jdk != null) {
            if (processedSdk.contains(jdk)) continue;
            processedSdk.add(jdk);
            children.add(new NamedLibraryElementNode(project, new NamedLibraryElement(null, jdkOrderEntry), getSettings()));
          }
        }
      }
    }
    for (AdditionalLibraryRootsProvider provider : AdditionalLibraryRootsProvider.EP_NAME.getExtensions()) {
      Collection<SyntheticLibrary> libraries = provider.getAdditionalProjectLibraries(project);
      for (SyntheticLibrary library : libraries) {
        if (library.isShowInExternalLibrariesNode()) {
          if (!(library instanceof ItemPresentation)) {
            LOG.warn("Synthetic library must implement ItemPresentation to be shown in External Libraries node: "
                     + libraries.getClass().getSimpleName());
            continue;
          }
          children.add(new SyntheticLibraryElementNode(project, library, (ItemPresentation)library, getSettings()));
        }
      }
    }
    return children;
  }

  public static void addLibraryChildren(final LibraryOrderEntry entry, final List<? super AbstractTreeNode> children, Project project, ProjectViewNode node) {
    final PsiManager psiManager = PsiManager.getInstance(project);
    final VirtualFile[] files = entry.getRootFiles(OrderRootType.CLASSES);
    for (final VirtualFile file : files) {
      final PsiDirectory psiDir = psiManager.findDirectory(file);
      if (psiDir == null) {
        continue;
      }
      children.add(new PsiDirectoryNode(project, psiDir, node.getSettings()));
    }
  }

  private static boolean hasExternalEntries(ProjectFileIndex index, LibraryOrderEntry orderEntry) {
    for (VirtualFile file : LibraryGroupNode.getLibraryRoots(orderEntry)) {
      if (!index.isInContent(VfsUtil.getLocalFile(file))) return true;
    }
    return false;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(IdeBundle.message("node.projectview.external.libraries"));
    presentation.setIcon(PlatformIcons.LIBRARY_ICON);
  }
}