// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module.impl;

import com.intellij.ProjectTopics;
import com.intellij.diagnostic.Activity;
import com.intellij.diagnostic.StartUpMeasurer;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.UnknownModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.impl.ProjectServiceContainerInitializedListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class isn't used in the new implementation of project model, which is based on {@link com.intellij.workspaceModel.ide Workspace Model}.
 * It shouldn't be used directly, its base class {@link ModuleManagerEx} should be used instead.
 */
@State(
  name = JpsProjectLoader.MODULE_MANAGER_COMPONENT,
  storages = @Storage("modules.xml"),
  useLoadedStateAsExisting = false /* why after loadState we get empty state on getState, test CMakeWorkspaceContentRootsTest */
)
@ApiStatus.Internal
public class ModuleManagerComponent extends ModuleManagerImpl {
  private final MessageBusConnection myMessageBusConnection;

  public ModuleManagerComponent(@NotNull Project project) {
    super(project);

    myMessageBusConnection = project.getMessageBus().connect(this);
    myMessageBusConnection.setDefaultHandler((event, params) -> cleanCachedStuff());
    myMessageBusConnection.subscribe(ProjectTopics.PROJECT_ROOTS);

    // default project doesn't have modules
    if (project.isDefault()) {
      return;
    }

    myMessageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new ModuleFileListener(this));
  }

  static final class MyProjectServiceContainerInitializedListener implements ProjectServiceContainerInitializedListener {
    @Override
    public void serviceCreated(@NotNull Project project) {
      Activity activity = StartUpMeasurer.startMainActivity("module loading");
      ModuleManager moduleManager = getInstance(project);
      if (!(moduleManager instanceof ModuleManagerImpl)) {
        return;
      }

      ModuleManagerImpl manager = (ModuleManagerImpl)moduleManager;
      manager.loadModules(manager.myModuleModel);
      activity.end();
      activity.setDescription("module count: " + manager.myModuleModel.getModules().length);
    }
  }

  @Override
  protected void unloadNewlyAddedModulesIfPossible(@NotNull Set<ModulePath> modulesToLoad, @NotNull List<UnloadedModuleDescriptionImpl> modulesToUnload) {
    UnloadedModulesListChange change = AutomaticModuleUnloader.getInstance(myProject).processNewModules(modulesToLoad, modulesToUnload);
    modulesToLoad.removeAll(change.getToUnload());
    modulesToUnload.addAll(change.getToUnloadDescriptions());
  }

  @Override
  protected void showUnknownModuleTypeNotification(@NotNull List<? extends Module> modulesWithUnknownTypes) {
    if (!ApplicationManager.getApplication().isHeadlessEnvironment() && !modulesWithUnknownTypes.isEmpty()) {
      String message;
      if (modulesWithUnknownTypes.size() == 1) {
        message = ProjectBundle.message("module.unknown.type.single.error", modulesWithUnknownTypes.get(0).getName(),
                                        ModuleType.get(modulesWithUnknownTypes.get(0)).getId());
      }
      else {
        StringBuilder modulesBuilder = new StringBuilder();
        for (final Module module : modulesWithUnknownTypes) {
          modulesBuilder.append("<br>\"");
          modulesBuilder.append(module.getName()).append("\" (type '").append(ModuleType.get(module).getId()).append("')");
        }
        modulesBuilder.append("<br>");
        message = ProjectBundle.message("module.unknown.type.multiple.error", modulesBuilder.toString());
      }
      // it is not modal warning at all
      //Messages.showWarningDialog(myProject, message, ProjectBundle.message("module.unknown.type.title"));
      Notifications.Bus.notify(new Notification(
        NotificationGroup.createIdWithTitle("Module Manager", ProjectBundle.message("notification.group.module.manager")),
        ProjectBundle.message("module.unknown.type.title"),
        message,
        NotificationType.WARNING
      ), myProject);
    }
  }

  @NotNull
  @Override
  protected ModuleEx createModule(@NotNull String filePath) {
    return new ModuleImpl(ModulePathKt.getModuleNameByFilePath(filePath), myProject, filePath);
  }

  @NotNull
  @Override
  protected ModuleEx createNonPersistentModule(@NotNull String name) {
    return new ModuleImpl(name, myProject, null);
  }

  @NotNull
  @Override
  protected ModuleEx createAndLoadModule(@NotNull String filePath) {
    return createModule(filePath);
  }

  @Override
  protected void setUnloadedModuleNames(@NotNull List<String> unloadedModuleNames) {
    super.setUnloadedModuleNames(unloadedModuleNames);
    if (!unloadedModuleNames.isEmpty()) {
      List<String> loadedModules = new ArrayList<>(myModuleModel.myModules.keySet());
      loadedModules.removeAll(new HashSet<>(unloadedModuleNames));
      AutomaticModuleUnloader.getInstance(myProject).setLoadedModules(loadedModules);
    }
  }

  @Override
  protected boolean isUnknownModuleType(@NotNull Module module) {
    return ModuleType.get(module) instanceof UnknownModuleType;
  }

  @Override
  protected void fireModulesAdded() {
    for (Module module : myModuleModel.getModules()) {
      fireModuleAddedInWriteAction((ModuleEx)module);
    }
  }

  @Override
  protected void deliverPendingEvents() {
    myMessageBusConnection.deliverImmediately();
  }
}
