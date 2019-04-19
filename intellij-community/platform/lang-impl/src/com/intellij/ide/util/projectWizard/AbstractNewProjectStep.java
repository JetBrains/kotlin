// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.projectWizard;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.util.projectWizard.actions.ProjectSpecificAction;
import com.intellij.idea.ActionsBundle;
import com.intellij.internal.statistic.beans.ConvertUsagesUtil;
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
import com.intellij.projectImport.ProjectOpenedCallback;
import com.intellij.util.PairConsumer;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

import static com.intellij.platform.ProjectTemplatesFactory.CUSTOM_GROUP;


public class AbstractNewProjectStep<T> extends DefaultActionGroup implements DumbAware {
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
    myCustomization.setUpBasicAction(projectSpecificAction, generators);

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
    addAll(myCustomization.getExtraActions(callback));
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
      final List<AnAction> actions = ContainerUtil.newArrayList();
      for (DirectoryProjectGenerator<T> projectGenerator : generators) {
        try {
          actions.addAll(ContainerUtil.list(getActions(projectGenerator, callback)));
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

      ProjectSettingsStepBase<T> step = generator instanceof CustomStepProjectGenerator ?
                                     ((ProjectSettingsStepBase<T>)((CustomStepProjectGenerator<T>)generator).createStep(generator, callback)) :
                                     createProjectSpecificSettingsStep(generator, callback);

      ProjectSpecificAction projectSpecificAction = new ProjectSpecificAction(generator, step);
      return projectSpecificAction.getChildren(null);
    }

    protected boolean shouldIgnore(@NotNull DirectoryProjectGenerator generator) {
      return generator instanceof HideableProjectGenerator && ((HideableProjectGenerator)generator).isHidden();
    }

    @NotNull
    public AnAction[] getExtraActions(@NotNull AbstractCallback<T> callback) {
      return AnAction.EMPTY_ARRAY;
    }

    public void setUpBasicAction(@NotNull ProjectSpecificAction projectSpecificAction, @NotNull DirectoryProjectGenerator[] generators) {
    }

    public boolean showUserDefinedProjects(){
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
      final DirectoryProjectGenerator generator = settings.getProjectGenerator();

      Object actualSettings = projectGeneratorPeer.getSettings();

      doGenerateProject(projectToClose, settings.getProjectLocation(), generator, actualSettings);
    }
  }

  public static Project doGenerateProject(@Nullable final Project projectToClose,
                                          @NotNull final String locationString,
                                          @Nullable final DirectoryProjectGenerator generator,
                                          @NotNull Object settings) {
    final File location = new File(FileUtil.toSystemDependentName(locationString));
    if (!location.exists() && !location.mkdirs()) {
      String message = ActionsBundle.message("action.NewDirectoryProject.cannot.create.dir", location.getAbsolutePath());
      Messages.showErrorDialog(projectToClose, message, ActionsBundle.message("action.NewDirectoryProject.title"));
      return null;
    }

    final VirtualFile baseDir =
      WriteAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location));
    if (baseDir == null) {
      LOG.error("Couldn't find '" + location + "' in VFS");
      return null;
    }
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir);

    if (baseDir.getChildren().length > 0) {
      String message = ActionsBundle.message("action.NewDirectoryProject.not.empty", location.getAbsolutePath());
      int rc = Messages.showYesNoDialog(projectToClose, message, ActionsBundle.message("action.NewDirectoryProject.title"), Messages.getQuestionIcon());
      if (rc == Messages.YES) {
        return PlatformProjectOpenProcessor.getInstance().doOpenProject(baseDir, null, false);
      }
    }

    String generatorName = generator == null ? "empty" : ConvertUsagesUtil.ensureProperKey(generator.getName());

    RecentProjectsManager.getInstance().setLastProjectCreationLocation(PathUtil.toSystemIndependentName(location.getParent()));

    ProjectOpenedCallback callback = null;
    if(generator instanceof TemplateProjectDirectoryGenerator){
      ((TemplateProjectDirectoryGenerator)generator).generateProject(baseDir.getName(), locationString);
    } else {
      callback = (p, module) -> {
        if (generator != null) {
          generator.generateProject(p, baseDir, settings, module);
        }
      };
    }
    EnumSet<PlatformProjectOpenProcessor.Option> options = EnumSet.noneOf(PlatformProjectOpenProcessor.Option.class);
    return PlatformProjectOpenProcessor.doOpenProject(baseDir, projectToClose, -1, callback, options);
  }
}