// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.CompoundIconProvider;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouperKt;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.projectImport.ProjectAttachProcessor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.IconUtil;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.SmartHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Set;

public class PsiDirectoryNode extends BasePsiNode<PsiDirectory> implements NavigatableWithText {
  // the chain from a parent directory to this one usually contains only one virtual file
  private final Set<VirtualFile> chain = new SmartHashSet<>();

  private final PsiFileSystemItemFilter myFilter;

  public PsiDirectoryNode(Project project, @NotNull PsiDirectory value, ViewSettings viewSettings) {
    this(project, value, viewSettings, null);
  }

  public PsiDirectoryNode(Project project, @NotNull PsiDirectory value, ViewSettings viewSettings, @Nullable PsiFileSystemItemFilter filter) {
    super(project, value, viewSettings);
    myFilter = filter;
  }

  @Nullable
  public PsiFileSystemItemFilter getFilter() {
    return myFilter;
  }

  protected boolean shouldShowModuleName() {
    return !PlatformUtils.isCidr();
  }

  protected boolean shouldShowSourcesRoot() {
    return true;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    Project project = getProject();
    assert project != null : this;
    PsiDirectory psiDirectory = getValue();
    assert psiDirectory != null : this;
    VirtualFile directoryFile = psiDirectory.getVirtualFile();
    Object parentValue = getParentValue();
    synchronized (chain) {
      if (chain.isEmpty()) {
        VirtualFile ancestor = getVirtualFile(parentValue);
        if (ancestor != null) {
          for (VirtualFile file = directoryFile; file != null && VfsUtilCore.isAncestor(ancestor, file, true); file = file.getParent()) {
            chain.add(file);
          }
        }
        if (chain.isEmpty()) chain.add(directoryFile);
      }
    }

    if (ProjectRootsUtil.isModuleContentRoot(directoryFile, project)) {
      ProjectFileIndex fi = ProjectRootManager.getInstance(project).getFileIndex();
      Module module = fi.getModuleForFile(directoryFile);

      data.setPresentableText(directoryFile.getName());
      if (module != null) {
        if (!(parentValue instanceof Module)) {
          if (ModuleType.isInternal(module) || !shouldShowModuleName()) {
            data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
          }
          else if (moduleNameMatchesDirectoryName(module, directoryFile, fi)) {
            data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          }
          else {
            data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            data.addText("[" + module.getName() + "]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          }
        }
        else {
          data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        boolean shouldShowUrl = getSettings().isShowURL() && (parentValue instanceof Module || parentValue instanceof Project);
        data.setLocationString(ProjectViewDirectoryHelper.getInstance(project).getLocationString(psiDirectory,
                                                                                                 shouldShowUrl,
                                                                                                 shouldShowSourcesRoot()));
        setupIcon(data, psiDirectory);

        return;
      }
    }

    String name = parentValue instanceof Project
                  ? psiDirectory.getVirtualFile().getPresentableUrl()
                  : ProjectViewDirectoryHelper.getInstance(psiDirectory.getProject()).getNodeName(getSettings(), parentValue, psiDirectory);
    if (name == null) {
      setValue(null);
      return;
    }

    data.setPresentableText(name);
    data.setLocationString(ProjectViewDirectoryHelper.getInstance(project).getLocationString(psiDirectory, false, false));

    setupIcon(data, psiDirectory);
  }

  private static boolean moduleNameMatchesDirectoryName(@NotNull Module module, @NotNull VirtualFile directoryFile, @NotNull ProjectFileIndex fileIndex) {
    if (Registry.is("ide.hide.real.module.name")) return true;
    String moduleName = module.getName();
    String directoryName = directoryFile.getName();
    if (moduleName.equalsIgnoreCase(directoryName)) {
      return true;
    }
    if (ModuleGrouperKt.isQualifiedModuleNamesEnabled(module.getProject()) && StringUtil.endsWithIgnoreCase(moduleName, directoryName)) {
      int parentPrefixLength = moduleName.length() - directoryName.length() - 1;
      if (parentPrefixLength > 0 && moduleName.charAt(parentPrefixLength) == '.') {
        VirtualFile parentDirectory = directoryFile.getParent();
        if (ProjectRootsUtil.isModuleContentRoot(parentDirectory, module.getProject())) {
          Module parentModule = fileIndex.getModuleForFile(parentDirectory);
          if (parentModule != null && parentModule.getName().length() == parentPrefixLength
              && moduleName.startsWith(parentModule.getName())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  protected void setupIcon(PresentationData data, PsiDirectory psiDirectory) {
    final VirtualFile virtualFile = psiDirectory.getVirtualFile();
    if (PlatformUtils.isAppCode()) {
      final Icon icon = IconUtil.getIcon(virtualFile, 0, myProject);
      if (icon != null) {
        data.setIcon(icon);
      }
    }
    else {
      Icon icon = CompoundIconProvider.findIcon(psiDirectory, 0);
      if (icon != null) data.setIcon(icon);
    }
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    return ProjectViewDirectoryHelper.getInstance(myProject).getDirectoryChildren(getValue(), getSettings(), true, getFilter());
  }

  @Override
  @SuppressWarnings("deprecation")
  public String getTestPresentation() {
    return "PsiDirectory: " + getValue().getName();
  }

  public boolean isFQNameShown() {
    return ProjectViewDirectoryHelper.getInstance(getProject()).isShowFQName(getSettings(), getParentValue(), getValue());
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final PsiDirectory value = getValue();
    if (value == null) {
      return false;
    }

    VirtualFile directory = value.getVirtualFile();
    if (directory.getFileSystem() instanceof LocalFileSystem) {
      file = VfsUtil.getLocalFile(file);
    }

    if (!VfsUtilCore.isAncestor(directory, file, false)) {
      return false;
    }

    final Project project = value.getProject();
    PsiFileSystemItemFilter filter = getFilter();
    if (filter != null) {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile != null && !filter.shouldShow(psiFile)) return false;

      PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(file);
      if (psiDirectory != null && !filter.shouldShow(psiDirectory)) return false;
    }

    if (Registry.is("ide.hide.excluded.files")) {
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      return !fileIndex.isExcluded(file);
    }
    else {
      return !FileTypeRegistry.getInstance().isFileIgnored(file);
    }
  }

  /**
   * @return a virtual file that identifies the given element
   */
  @Nullable
  private static VirtualFile getVirtualFile(Object element) {
    if (element instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)element;
      return directory.getVirtualFile();
    }
    return element instanceof VirtualFile ? (VirtualFile)element : null;
  }

  @Override
  public boolean canRepresent(final Object element) {
    VirtualFile file = getVirtualFile(element);
    if (file != null) {
      synchronized (chain) {
        if (chain.contains(file)) return true;
      }
    }
    if (super.canRepresent(element)) return true;
    return ProjectViewDirectoryHelper.getInstance(getProject())
      .canRepresent(element, getValue(), getParentValue(), getSettings());
  }

  @Override
  public boolean isValid() {
    if (!super.isValid()) return false;
    return ProjectViewDirectoryHelper.getInstance(getProject())
      .isValidDirectory(getValue(), getParentValue(), getSettings(), getFilter());
  }

  @Override
  public boolean canNavigate() {
    VirtualFile file = getVirtualFile();
    Project project = getProject();

    ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
    return file != null && (ProjectRootsUtil.isModuleContentRoot(file, project) && service.canOpenModuleSettings() ||
                            ProjectRootsUtil.isModuleSourceRoot(file, project)  && service.canOpenContentEntriesSettings() ||
                            ProjectRootsUtil.isLibraryRoot(file, project) && service.canOpenModuleLibrarySettings());
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public void navigate(final boolean requestFocus) {
    Module module = ModuleUtilCore.findModuleForPsiElement(getValue());
    if (module != null) {
      final VirtualFile file = getVirtualFile();
      final Project project = getProject();
      ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
      if (ProjectRootsUtil.isModuleContentRoot(file, project)) {
        service.openModuleSettings(module);
      }
      else if (ProjectRootsUtil.isLibraryRoot(file, project)) {
        final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(file, module.getProject());
        if (orderEntry != null) {
          service.openLibraryOrSdkSettings(orderEntry);
        }
      }
      else {
        service.openContentEntriesSettings(module);
      }
    }
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    VirtualFile file = getVirtualFile();
    Project project = getProject();

    if (file != null && project != null) {
      if (ProjectRootsUtil.isModuleContentRoot(file, project) || ProjectRootsUtil.isModuleSourceRoot(file, project)) {
        return ActionsBundle.message("action.ModuleSettings.navigate");
      }
      if (ProjectRootsUtil.isLibraryRoot(file, project)) {
        return ActionsBundle.message("action.LibrarySettings.navigate");
      }
    }

    return null;
  }

  @Override
  public int getWeight() {
    ViewSettings settings = getSettings();
    if (settings == null || settings.isFoldersAlwaysOnTop()) {
      return 20;
    }
    return isFQNameShown() ? 70 : 0;
  }

  @Override
  public String getTitle() {
    final PsiDirectory directory = getValue();
    if (directory != null) {
      return PsiDirectoryFactory.getInstance(getProject()).getQualifiedName(directory, true);
    }
    return super.getTitle();
  }

  @Override
  public Comparable getSortKey() {
    if (ProjectAttachProcessor.canAttachToProject()) {
      // primary module is always on top; attached modules are sorted alphabetically
      final VirtualFile file = getVirtualFile();
      if (Comparing.equal(file, myProject.getBaseDir())) {
        return "";    // sorts before any other name
      }
      return toString();
    }
    return null;
  }

  @Override
  public Comparable getTypeSortKey() {
    VirtualFile file = getVirtualFile();
    if (file != null) {
      String extension = file.getExtension();
      if (extension != null) {
        return new PsiFileNode.ExtensionSortKey(extension);
      }
    }
    return null;
  }

  @Override
  public String getQualifiedNameSortKey() {
    final PsiDirectoryFactory factory = PsiDirectoryFactory.getInstance(getProject());
    return factory.getQualifiedName(getValue(), true);
  }

  @Override
  public int getTypeSortWeight(final boolean sortByType) {
    return 3;
  }

  @Override
  public boolean shouldDrillDownOnEmptyElement() {
    return true;
  }

  @Override
  public boolean isAlwaysShowPlus() {
    final VirtualFile file = getVirtualFile();
    return file == null || file.getChildren().length > 0;
  }
}
