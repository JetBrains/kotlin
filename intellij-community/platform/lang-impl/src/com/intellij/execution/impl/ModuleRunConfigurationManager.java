// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.ProjectTopics;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@State(name = "ModuleRunConfigurationManager")
public final class ModuleRunConfigurationManager implements PersistentStateComponent<Element> {
  @NonNls
  @NotNull
  private static final String STORE_LOCAL_REGISTRY_OPTION = "ruby.store.local.run.conf.in.modules";
  private static final String SHARED = "shared";
  private static final String LOCAL = "local";
  private static final Logger LOG = Logger.getInstance(ModuleRunConfigurationManager.class);
  @NotNull
  private final Module myModule;
  @NotNull
  private final Condition<RunnerAndConfigurationSettings> myModuleConfigCondition =
    settings -> settings != null && usesMyModule(settings.getConfiguration());

  public ModuleRunConfigurationManager(@NotNull Module module) {
    myModule = module;
    myModule.getMessageBus().connect().subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void beforeModuleRemoved(@NotNull Project project, @NotNull Module module) {
        if (myModule.equals(module)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("time to remove something from project (" + project + ")");
          }
          getRunManager().removeConfigurations(getModuleRunConfigurationSettings());
        }
      }
    });
  }

  static class ModuleRunConfigurationManagerStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
      if (!project.isDefault()) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
          if (!module.isDisposed()) {
            ModuleServiceManager.getService(module, ModuleRunConfigurationManager.class);
          }
        }
      }
    }
  }

  @Nullable
  @Override
  public Element getState() {
    try {
      Element element = new Element("state")
        .addContent(writeExternal(new Element(SHARED), true));

      if (Registry.is(STORE_LOCAL_REGISTRY_OPTION)) {
        element.addContent(writeExternal(new Element(LOCAL), false));
      }

      return element;
    }
    catch (WriteExternalException e1) {
      LOG.error(e1);
      return null;
    }
  }

  @Override
  public void loadState(@NotNull Element state) {
    try {
      readExternal(state);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }

  @NotNull
  private Collection<? extends RunnerAndConfigurationSettings> getModuleRunConfigurationSettings() {
    return ContainerUtil.filter(getRunManager().getAllSettings(), myModuleConfigCondition);
  }

  @NotNull
  private RunManagerImpl getRunManager() {
    return (RunManagerImpl)RunManager.getInstance(myModule.getProject());
  }

  private boolean usesMyModule(RunConfiguration config) {
    return config instanceof ModuleBasedConfiguration
           && myModule.equals(((ModuleBasedConfiguration)config).getConfigurationModule().getModule());
  }

  public Element writeExternal(@NotNull final Element element, boolean isShared) throws WriteExternalException {
    LOG.debug("writeExternal(" + myModule + "); shared: " + isShared);
    getRunManager().writeConfigurations(
      element,
      ContainerUtil.filter(getModuleRunConfigurationSettings(), settings -> settings.isShared() == isShared)
    );
    return element;
  }

  public void readExternal(@NotNull Element element) {
    Element sharedElement = element.getChild(SHARED);
    if (sharedElement != null) {
      doReadExternal(sharedElement, true);
    }
    Element localElement = element.getChild(LOCAL);
    if (localElement != null) {
      doReadExternal(localElement, false);
    }
  }

  private void doReadExternal(@NotNull Element element, boolean isShared) {
    LOG.debug("readExternal(" + myModule + ");  shared: " + isShared);

    RunManagerImpl runManager = getRunManager();
    for (final Element child : element.getChildren(RunManagerImpl.CONFIGURATION)) {
      runManager.loadConfiguration(child, isShared);
    }

    runManager.requestSort();
  }
}
