// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView;

import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public interface ProjectViewSettings extends ViewSettings {
  boolean isShowExcludedFiles();

  /**
   * If {@code true} then {@link com.intellij.ide.projectView.impl.NestingTreeStructureProvider} will modify the tree presentation
   * according to the rules managed by {@link com.intellij.ide.projectView.impl.ProjectViewFileNestingService}: some peer files will be
   * shown as nested, for example generated {@code foo.js} and {@code foo.js.map} file nodes will be shown as children of the
   * original {@code foo.ts} node in the Project View.
   */
  default boolean isUseFileNestingRules() {return true;}

  class Immutable extends ViewSettings.Immutable implements ProjectViewSettings {
    public static final ProjectViewSettings DEFAULT = new ProjectViewSettings.Immutable(null);

    private final boolean myShowExcludedFiles;
    private final boolean myUseFileNestingRules;

    public Immutable(ProjectViewSettings settings) {
      super(settings);
      myShowExcludedFiles = settings != null && settings.isShowExcludedFiles();
      myUseFileNestingRules = settings == null || settings.isUseFileNestingRules();
    }

    @Override
    public boolean isShowExcludedFiles() {
      return myShowExcludedFiles;
    }

    @Override
    public boolean isUseFileNestingRules() {
      return myUseFileNestingRules;
    }

    @Override
    public boolean equals(Object object) {
      if (object == this) return true;
      if (!super.equals(object)) return false;
      ProjectViewSettings settings = (ProjectViewSettings)object;
      return settings.isShowExcludedFiles() == isShowExcludedFiles() &&
             settings.isUseFileNestingRules() == isUseFileNestingRules();
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + Boolean.hashCode(isShowExcludedFiles());
      result = 31 * result + Boolean.hashCode(isUseFileNestingRules());
      return result;
    }
  }

  final class Delegate implements ProjectViewSettings {
    private final Project project;
    private final String id;

    /**
     * @param project a project used to retrieve a corresponding view
     * @param id      an identifier of a pane or {@code null} for current one
     */
    public Delegate(@NotNull Project project, @Nullable String id) {
      this.project = project;
      this.id = id;
    }

    @Override
    public boolean isShowExcludedFiles() {
      ProjectView view = getProjectView();
      return view != null && view.isShowExcludedFiles(getPaneID(view));
    }

    @Override
    public boolean isUseFileNestingRules() {
      ProjectViewSettings settings = getProjectViewSettings();
      return settings != null && settings.isUseFileNestingRules();
    }

    @Override
    public boolean isFoldersAlwaysOnTop() {
      ProjectView view = getProjectView();
      return view != null && view.isFoldersAlwaysOnTop(getPaneID(view));
    }

    @Override
    public boolean isShowMembers() {
      ProjectView view = getProjectView();
      return view != null && view.isShowMembers(getPaneID(view));
    }

    @Override
    public boolean isStructureView() {
      return false;
    }

    @Override
    public boolean isShowModules() {
      ProjectView view = getProjectView();
      return view != null && view.isShowModules(getPaneID(view));
    }

    @Override
    public boolean isFlattenModules() {
      ProjectView view = getProjectView();
      return view != null && view.isFlattenModules(getPaneID(view));
    }

    @Override
    public boolean isShowURL() {
      ProjectView view = getProjectView();
      return view != null && view.isShowURL(getPaneID(view));
    }

    @Override
    public boolean isFlattenPackages() {
      ProjectViewDirectoryHelper helper = getProjectViewDirectoryHelper();
      if (helper == null || !helper.supportsFlattenPackages()) return false;
      ProjectView view = getProjectView();
      return view != null && view.isFlattenPackages(getPaneID(view));
    }

    @Override
    public boolean isAbbreviatePackageNames() {
      ProjectView view = getProjectView();
      return view != null && view.isAbbreviatePackageNames(getPaneID(view));
    }

    @Override
    public boolean isHideEmptyMiddlePackages() {
      ProjectViewDirectoryHelper helper = getProjectViewDirectoryHelper();
      if (helper == null || !helper.supportsHideEmptyMiddlePackages()) return false;
      ProjectView view = getProjectView();
      return view != null && view.isHideEmptyMiddlePackages(getPaneID(view));
    }

    @Override
    public boolean isCompactDirectories() {
      ProjectView view = getProjectView();
      return view != null && view.isCompactDirectories(getPaneID(view));
    }

    @Override
    public boolean isShowLibraryContents() {
      ProjectView view = getProjectView();
      return view != null && view.isShowLibraryContents(getPaneID(view));
    }

    @Nullable
    private ProjectViewDirectoryHelper getProjectViewDirectoryHelper() {
      return project.isDisposed() ? null : ProjectViewDirectoryHelper.getInstance(project);
    }

    @Nullable
    private ProjectView getProjectView() {
      return project.isDisposed() ? null : ProjectView.getInstance(project);
    }

    @Nullable
    private String getPaneID(@NotNull ProjectView view) {
      return id != null ? id : view.getCurrentViewId();
    }

    @Nullable
    private AbstractTreeStructure getStructure(@NotNull ProjectView view) {
      AbstractProjectViewPane pane = id == null ? view.getCurrentProjectViewPane() : view.getProjectViewPaneById(id);
      return pane == null ? null : pane.getTreeStructure();
    }

    @Nullable
    private ProjectViewSettings getProjectViewSettings() {
      ProjectView view = getProjectView();
      AbstractTreeStructure structure = view == null ? null : getStructure(view);
      return structure instanceof ProjectViewSettings ? (ProjectViewSettings)structure : null;
    }
  }
}
