/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.compiler.actions;

import com.intellij.lang.IdeLanguageCustomization;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.MultiSelectionListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.intellij.task.ProjectTaskManager;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class BuildArtifactAction extends DumbAwareAction {
  private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.balloonGroup("Clean artifact");

  public BuildArtifactAction() {
    super("Build Artifacts...", "Select and build artifacts configured in the project", null);
  }
  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = getEventProject(e);
    final Presentation presentation = e.getPresentation();
    boolean enabled = project != null && !ArtifactUtil.getArtifactWithOutputPaths(project).isEmpty();
    if (IdeLanguageCustomization.getInstance().getPrimaryIdeLanguages().contains(StdFileTypes.JAVA.getLanguage())
        && ActionPlaces.MAIN_MENU.equals(e.getPlace())) {
      //building artifacts is a valuable functionality for Java IDEs, let's not hide 'Build Artifacts' item from the main menu
      presentation.setEnabled(enabled);
    }
    else {
      presentation.setEnabledAndVisible(enabled);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = getEventProject(e);
    if (project == null) return;

    final List<Artifact> artifacts = ArtifactUtil.getArtifactWithOutputPaths(project);
    if (artifacts.isEmpty()) return;

    List<ArtifactPopupItem> items = new ArrayList<>();
    if (artifacts.size() > 1) {
      items.add(0, new ArtifactPopupItem(null, "All Artifacts", EmptyIcon.ICON_16));
    }
    Set<Artifact> selectedArtifacts = new HashSet<>(ArtifactsWorkspaceSettings.getInstance(project).getArtifactsToBuild());
    TIntArrayList selectedIndices = new TIntArrayList();
    if (Comparing.haveEqualElements(artifacts, selectedArtifacts) && selectedArtifacts.size() > 1) {
      selectedIndices.add(0);
      selectedArtifacts.clear();
    }

    for (Artifact artifact : artifacts) {
      final ArtifactPopupItem item = new ArtifactPopupItem(artifact, artifact.getName(), artifact.getArtifactType().getIcon());
      if (selectedArtifacts.contains(artifact)) {
        selectedIndices.add(items.size());
      }
      items.add(item);
    }

    final ProjectSettingsService projectSettingsService = ProjectSettingsService.getInstance(project);
    final ArtifactAwareProjectSettingsService settingsService = projectSettingsService instanceof ArtifactAwareProjectSettingsService ? (ArtifactAwareProjectSettingsService)projectSettingsService : null;

    final ChooseArtifactStep step = new ChooseArtifactStep(items, artifacts.get(0), project, settingsService);
    step.setDefaultOptionIndices(selectedIndices.toNativeArray());

    final ListPopupImpl popup = (ListPopupImpl)JBPopupFactory.getInstance().createListPopup(step);
    final KeyStroke editKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getEditSource());
    if (settingsService != null && editKeyStroke != null) {
      popup.registerAction("editArtifact", editKeyStroke, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Object[] values = popup.getSelectedValues();
          popup.cancel();
          settingsService.openArtifactSettings(values.length > 0 ? ((ArtifactPopupItem)values[0]).getArtifact() : null);
        }
      });
    }
    popup.showCenteredInCurrentWindow(project);
  }

  private static void doBuild(@NotNull Project project, final @NotNull List<? extends ArtifactPopupItem> items, boolean rebuild) {
    final Artifact[] artifacts = getArtifacts(items, project);
    if (rebuild) {
      ProjectTaskManager.getInstance(project).rebuild(artifacts);
    }
    else {
      ProjectTaskManager.getInstance(project).build(artifacts);
    }
  }

  private static Artifact[] getArtifacts(final List<? extends ArtifactPopupItem> items, final Project project) {
    Set<Artifact> artifacts = new LinkedHashSet<>();
    for (ArtifactPopupItem item : items) {
      artifacts.addAll(item.getArtifacts(project));
    }
    return artifacts.toArray(new Artifact[0]);
  }

  private static class BuildArtifactItem extends ArtifactActionItem {
    private BuildArtifactItem(List<ArtifactPopupItem> item, Project project) {
      super(item, project, "Build");
    }

    @Override
    public void run() {
      doBuild(myProject, myArtifactPopupItems, false);
    }
  }

  private static class CleanArtifactItem extends ArtifactActionItem {
    private CleanArtifactItem(@NotNull List<ArtifactPopupItem> item, @NotNull Project project) {
      super(item, project, "Clean");
    }

    @Override
    public void run() {
      Set<VirtualFile> parents = new HashSet<>();
      final VirtualFile[] roots = ProjectRootManager.getInstance(myProject).getContentSourceRoots();
      for (VirtualFile root : roots) {
        VirtualFile parent = root;
        while (parent != null && !parents.contains(parent)) {
          parents.add(parent);
          parent = parent.getParent();
        }
      }

      Map<String, String> outputPathContainingSourceRoots = new HashMap<>();
      final List<Pair<File, Artifact>> toClean = new ArrayList<>();
      Artifact[] artifacts = getArtifacts(myArtifactPopupItems, myProject);
      for (Artifact artifact : artifacts) {
        String outputPath = artifact.getOutputFilePath();
        if (outputPath != null) {
          toClean.add(Pair.create(new File(FileUtil.toSystemDependentName(outputPath)), artifact));
          final VirtualFile outputFile = LocalFileSystem.getInstance().findFileByPath(outputPath);
          if (parents.contains(outputFile)) {
            outputPathContainingSourceRoots.put(artifact.getName(), outputPath);
          }
        }
      }

      if (!outputPathContainingSourceRoots.isEmpty()) {
        final String message;
        if (outputPathContainingSourceRoots.size() == 1 && outputPathContainingSourceRoots.values().size() == 1) {
          final String name = ContainerUtil.getFirstItem(outputPathContainingSourceRoots.keySet());
          final String output = outputPathContainingSourceRoots.get(name);
          message = "The output directory '" + output + "' of '" + name + "' artifact contains source roots of the project. Do you want to continue and clear it?";
        }
        else {
          StringBuilder info = new StringBuilder();
          for (String name : outputPathContainingSourceRoots.keySet()) {
            info.append(" '").append(name).append("' artifact ('").append(outputPathContainingSourceRoots.get(name)).append("')\n");
          }
          message = "The output directories of the following artifacts contains source roots:\n" +
                    info + "Do you want to continue and clear these directories?";
        }
        final int answer = Messages.showYesNoDialog(myProject, message, "Clean Artifacts", null);
        if (answer != Messages.YES) {
          return;
        }
      }

      new Task.Backgroundable(myProject, "Cleaning Artifacts", true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          List<File> deleted = new ArrayList<>();
          for (Pair<File, Artifact> pair : toClean) {
            indicator.checkCanceled();
            File file = pair.getFirst();
            if (!FileUtil.delete(file)) {
              NOTIFICATION_GROUP.createNotification("Cannot clean '" + pair.getSecond().getName() + "' artifact", "cannot delete '" + file.getAbsolutePath() + "'", NotificationType.ERROR, null).notify(myProject);
            }
            else {
              deleted.add(file);
            }
          }
          LocalFileSystem.getInstance().refreshIoFiles(deleted, true, true, null);
        }
      }.queue();
    }
  }

  private static class RebuildArtifactItem extends ArtifactActionItem {
    private RebuildArtifactItem(List<ArtifactPopupItem> item, Project project) {
      super(item, project, "Rebuild");
    }

    @Override
    public void run() {
      doBuild(myProject, myArtifactPopupItems, true);
    }
  }

  private static class EditArtifactItem extends ArtifactActionItem {
    private final ArtifactAwareProjectSettingsService mySettingsService;

    private EditArtifactItem(List<ArtifactPopupItem> item, Project project, final ArtifactAwareProjectSettingsService projectSettingsService) {
      super(item, project, "Edit...");
      mySettingsService = projectSettingsService;
    }

    @Override
    public void run() {
      mySettingsService.openArtifactSettings(myArtifactPopupItems.get(0).getArtifact());
    }
  }

  private static abstract class ArtifactActionItem implements Runnable {
    protected final List<ArtifactPopupItem> myArtifactPopupItems;
    protected final Project myProject;
    private final String myActionName;

    protected ArtifactActionItem(@NotNull List<ArtifactPopupItem> item, @NotNull Project project, @NotNull String name) {
      myArtifactPopupItems = item;
      myProject = project;
      myActionName = name;
    }

    public String getActionName() {
      return myActionName;
    }
  }

  private static class ArtifactPopupItem {
    @Nullable private final Artifact myArtifact;
    private final String myText;
    private final Icon myIcon;

    private ArtifactPopupItem(@Nullable Artifact artifact, String text, Icon icon) {
      myArtifact = artifact;
      myText = text;
      myIcon = icon;
    }

    @Nullable
    public Artifact getArtifact() {
      return myArtifact;
    }

    public String getText() {
      return myText;
    }

    public Icon getIcon() {
      return myIcon;
    }

    public List<Artifact> getArtifacts(Project project) {
      final Artifact artifact = getArtifact();
      return artifact != null ? Collections.singletonList(artifact) : ArtifactUtil.getArtifactWithOutputPaths(project);
    }
  }
  
  private static class ChooseArtifactStep extends MultiSelectionListPopupStep<ArtifactPopupItem> {
    private final Artifact myFirst;
    private final Project myProject;
    private final ArtifactAwareProjectSettingsService mySettingsService;

    ChooseArtifactStep(List<ArtifactPopupItem> artifacts,
                              Artifact first,
                              Project project, final ArtifactAwareProjectSettingsService settingsService) {
      super("Build Artifact", artifacts);
      myFirst = first;
      myProject = project;
      mySettingsService = settingsService;
    }

    @Override
    public boolean isSpeedSearchEnabled() {
      return true;
    }

    @Override
    public Icon getIconFor(ArtifactPopupItem aValue) {
      return aValue.getIcon();
    }

    @NotNull
    @Override
    public String getTextFor(ArtifactPopupItem value) {
      return value.getText();
    }

    @Override
    public boolean hasSubstep(List<? extends ArtifactPopupItem> selectedValues) {
      return true;
    }

    @Override
    public ListSeparator getSeparatorAbove(ArtifactPopupItem value) {
      return myFirst.equals(value.getArtifact()) ? new ListSeparator() : null;
    }

    @Override
    public PopupStep<?> onChosen(final List<ArtifactPopupItem> selectedValues, boolean finalChoice) {
      if (finalChoice) {
        return doFinalStep(() -> doBuild(myProject, selectedValues, false));
      }
      final List<ArtifactActionItem> actions = new ArrayList<>();
      actions.add(new BuildArtifactItem(selectedValues, myProject));
      actions.add(new RebuildArtifactItem(selectedValues, myProject));
      actions.add(new CleanArtifactItem(selectedValues, myProject));
      if (mySettingsService != null) {
        actions.add(new EditArtifactItem(selectedValues, myProject, mySettingsService));
      }
      return new BaseListPopupStep<ArtifactActionItem>(selectedValues.size() == 1 ? "Action" : "Action for " + selectedValues.size() + " artifacts", actions) {
        @NotNull
        @Override
        public String getTextFor(ArtifactActionItem value) {
          return value.getActionName();
        }

        @Override
        public PopupStep onChosen(ArtifactActionItem selectedValue, boolean finalChoice) {
          return doFinalStep(selectedValue);
        }
      };
    }
  }
}
