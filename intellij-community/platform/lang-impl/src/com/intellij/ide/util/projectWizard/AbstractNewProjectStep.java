// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.projectWizard;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.util.projectWizard.actions.ProjectSpecificAction;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.platform.*;
import com.intellij.platform.templates.ArchivedTemplatesFactory;
import com.intellij.platform.templates.LocalArchivedTemplate;
import com.intellij.platform.templates.TemplateProjectDirectoryGenerator;
import com.intellij.util.PairConsumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.platform.ProjectTemplatesFactory.CUSTOM_GROUP;


public abstract class AbstractNewProjectStep<T> extends DefaultActionGroup implements DumbAware {
  private static final Logger LOG = Logger.getInstance(AbstractNewProjectStep.class);
  private final Customization<T> myCustomization;

  protected AbstractNewProjectStep(@NotNull Customization<T> customization) {
    super(null, true);
    myCustomization = customization;
    updateActions();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    updateActions();
  }

  protected void updateActions() {
    removeAll();
    AbstractCallback<T> callback = myCustomization.createCallback();
    ProjectSpecificAction projectSpecificAction = myCustomization.createProjectSpecificAction(callback);
    addProjectSpecificAction(projectSpecificAction);

    DirectoryProjectGenerator<T>[] generators = myCustomization.getProjectGenerators();

    addAll(myCustomization.getActions(generators, callback));
    if (myCustomization.showUserDefinedProjects()) {
      ArchivedTemplatesFactory factory = new ArchivedTemplatesFactory();
      ProjectTemplate[] templates = factory.createTemplates(CUSTOM_GROUP, null);
      DirectoryProjectGenerator[] projectGenerators = ContainerUtil.map(templates,
                                                                        (ProjectTemplate template) ->
                                                                          new TemplateProjectDirectoryGenerator(
                                                                            (LocalArchivedTemplate)template),
                                                                        new DirectoryProjectGenerator[templates.length]);
      addAll(myCustomization.getActions(projectGenerators, callback));
    }
  }

  protected void addProjectSpecificAction(@NotNull final ProjectSpecificAction projectSpecificAction) {
    addAll(projectSpecificAction.getChildren(null));
  }

  protected static abstract class Customization<T> {
    @NotNull
    protected ProjectSpecificAction createProjectSpecificAction(@NotNull final AbstractCallback<T> callback) {
      DirectoryProjectGenerator<T> emptyProjectGenerator = createEmptyProjectGenerator();
      return new ProjectSpecificAction(emptyProjectGenerator, createProjectSpecificSettingsStep(emptyProjectGenerator, callback));
    }

    @NotNull
    protected abstract AbstractCallback<T> createCallback();

    @NotNull
    protected abstract DirectoryProjectGenerator<T> createEmptyProjectGenerator();

    @NotNull
    protected abstract ProjectSettingsStepBase<T> createProjectSpecificSettingsStep(@NotNull DirectoryProjectGenerator<T> projectGenerator,
                                                                                    @NotNull AbstractCallback<T> callback);


    @NotNull
    protected DirectoryProjectGenerator<T>[] getProjectGenerators() {
      return DirectoryProjectGenerator.EP_NAME.getExtensions();
    }

    public AnAction[] getActions(@NotNull DirectoryProjectGenerator<T>[] generators, @NotNull AbstractCallback<T> callback) {
      final List<AnAction> actions = new ArrayList<>();
      for (DirectoryProjectGenerator<T> projectGenerator : generators) {
        try {
          actions.addAll(Arrays.asList(getActions(projectGenerator, callback)));
        } catch (Throwable throwable) {
          LOG.error("Broken project generator " + projectGenerator, throwable);
        }
      }
      return actions.toArray(AnAction.EMPTY_ARRAY);
    }

    @NotNull
    public AnAction[] getActions(@NotNull DirectoryProjectGenerator<T> generator, @NotNull AbstractCallback<T> callback) {
      if (shouldIgnore(generator)) {
        return AnAction.EMPTY_ARRAY;
      }

      ProjectSettingsStepBase<T> step;
      if (generator instanceof CustomStepProjectGenerator) {
        //noinspection unchecked
        step = (ProjectSettingsStepBase<T>)((CustomStepProjectGenerator<T>)generator).createStep(generator, callback);
      }
      else {
        step = createProjectSpecificSettingsStep(generator, callback);
      }

      ProjectSpecificAction projectSpecificAction = new ProjectSpecificAction(generator, step);
      return projectSpecificAction.getChildren(null);
    }

    protected boolean shouldIgnore(@NotNull DirectoryProjectGenerator<?> generator) {
      return generator instanceof HideableProjectGenerator && ((HideableProjectGenerator)generator).isHidden();
    }

    public boolean showUserDefinedProjects() {
      return false;
    }
  }

  public static class AbstractCallback<T> implements PairConsumer<ProjectSettingsStepBase<T>, ProjectGeneratorPeer<T>> {
    @Override
    public void consume(@Nullable final ProjectSettingsStepBase<T> settings, @NotNull final ProjectGeneratorPeer<T> projectGeneratorPeer) {
      if (settings == null) return;

      // todo projectToClose should be passed from calling action, this is just a quick workaround
      IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
      final Project projectToClose = frame != null ? frame.getProject() : null;
      DirectoryProjectGenerator<?> generator = settings.getProjectGenerator();
      Object actualSettings = projectGeneratorPeer.getSettings();
      doGenerateProject(projectToClose, settings.getProjectLocation(), generator, actualSettings);
    }
  }

  public static Project doGenerateProject(@Nullable Project projectToClose,
                                          @NotNull String locationString,
                                          @Nullable DirectoryProjectGenerator generator,
                                          @NotNull Object settings) {
    Path location = Paths.get(locationString);
    try {
      Files.createDirectories(location);
    }
    catch (IOException e) {
      LOG.warn(e);
      String message = ActionsBundle.message("action.NewDirectoryProject.cannot.create.dir", location.toString());
      Messages.showErrorDialog(projectToClose, message, ActionsBundle.message("action.NewDirectoryProject.title"));
      return null;
    }

    VirtualFile baseDir = WriteAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(location.toString())));
    if (baseDir == null) {
      LOG.error("Couldn't find '" + location + "' in VFS");
      return null;
    }
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir);

    if (baseDir.getChildren().length > 0) {
      String message = ActionsBundle.message("action.NewDirectoryProject.not.empty", location.toString());
      int result = Messages.showYesNoDialog(projectToClose, message, ActionsBundle.message("action.NewDirectoryProject.title"), Messages.getQuestionIcon());
      if (result == Messages.YES) {
        return PlatformProjectOpenProcessor.doOpenProject(location, new OpenProjectTask(), -1);
      }
    }

    RecentProjectsManager.getInstance().setLastProjectCreationLocation(location.getParent());

    OpenProjectTask options = new OpenProjectTask(/* forceOpenInNewFrame = */ false, projectToClose);
    options.isNewProject = true;
    options.isRefreshVfsNeeded = false;
    if (generator instanceof TemplateProjectDirectoryGenerator) {
      ((TemplateProjectDirectoryGenerator<?>)generator).generateProject(baseDir.getName(), locationString);
    }
    else if (generator != null) {
      options.callback = (p, module) -> generator.generateProject(p, baseDir, settings, module);
    }
    return PlatformProjectOpenProcessor.openExistingProject(location, location, options);
  }
}