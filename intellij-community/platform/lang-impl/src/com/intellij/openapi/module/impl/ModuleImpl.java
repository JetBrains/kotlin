// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module.impl;

import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.impl.PlatformComponentManagerImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.module.impl.scopes.ModuleScopeProviderImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.roots.ExternalProjectSystemRegistry;
import com.intellij.openapi.roots.ProjectModelElement;
import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Property;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;

import java.util.List;
import java.util.Map;

/**
 * @author max
 */
public class ModuleImpl extends PlatformComponentManagerImpl implements ModuleEx {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.module.impl.ModuleImpl");

  @NotNull private final Project myProject;
  private final VirtualFilePointer myImlFilePointer;
  private volatile boolean isModuleAdded;

  private String myName;

  private final ModuleScopeProvider myModuleScopeProvider;

  ModuleImpl(@NotNull String name, @NotNull Project project, @NotNull String filePath) {
    super(project, "Module " + name);

    getPicoContainer().registerComponentInstance(Module.class, this);

    myProject = project;
    myModuleScopeProvider = new ModuleScopeProviderImpl(this);

    myName = name;
    myImlFilePointer = VirtualFilePointerManager.getInstance().create(VfsUtilCore.pathToUrl(filePath), this, null);
  }

  @Override
  protected void bootstrapPicoContainer(@NotNull String name) {
    Extensions.instantiateArea(ExtensionAreas.IDEA_MODULE, this, (AreaInstance)getParentComponentManager());
    super.bootstrapPicoContainer(name);
  }

  @Override
  public void init(@Nullable Runnable beforeComponentCreation) {
    // do not measure (activityNamePrefix method not overridden by this class)
    // because there are a lot of modules and no need to measure each one
    registerComponents(PluginManagerCore.getLoadedPlugins());
    if (beforeComponentCreation != null) {
      beforeComponentCreation.run();
    }
    createComponents(null);
  }

  @Nullable
  @Override
  protected ProgressIndicator getProgressIndicator() {
    // module loading progress is not tracked, progress updated by ModuleManagerImpl on module load
    return null;
  }

  @Override
  public boolean isDisposed() {
    // in case of light project in tests when it's temporarily disposed, the module should be treated as disposed too.
    //noinspection TestOnlyProblems
    return super.isDisposed() || ((ProjectImpl)myProject).isLight() && myProject.isDisposed();
  }

  @Override
  protected boolean isComponentSuitable(@NotNull ComponentConfig componentConfig) {
    if (!super.isComponentSuitable(componentConfig)) {
      return false;
    }

    Map<String, String> options = componentConfig.options;
    if (options == null || options.isEmpty()) {
      return true;
    }

    for (String optionName : options.keySet()) {
      if ("workspace".equals(optionName) || "overrides".equals(optionName)) {
        continue;
      }

      // we cannot filter using module options because at this moment module file data could be not loaded
      String message = "Don't specify " + optionName + " in the component registration, transform component to service and implement your logic in your getInstance() method";
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        LOG.error(message);
      }
      else {
        LOG.warn(message);
      }
    }

    return true;
  }

  @Override
  @Nullable
  public VirtualFile getModuleFile() {
    return myImlFilePointer.getFile();
  }

  @Override
  public void rename(@NotNull String newName, boolean notifyStorage) {
    myName = newName;
    if (notifyStorage) {
      ServiceKt.getStateStore(this).getStorageManager()
        .rename(StoragePathMacros.MODULE_FILE, newName + ModuleFileType.DOT_DEFAULT_EXTENSION);
    }
  }

  @Override
  @NotNull
  public String getModuleFilePath() {
    return ServiceKt.getStateStore(this).getStorageManager().expandMacros(StoragePathMacros.MODULE_FILE);
  }

  @Override
  public synchronized void dispose() {
    isModuleAdded = false;
    disposeComponents();
    Extensions.disposeArea(this);
    super.dispose();
  }

  @NotNull
  @Override
  public List<ComponentConfig> getMyComponentConfigsFromDescriptor(@NotNull IdeaPluginDescriptor plugin) {
    return plugin.getModuleComponents();
  }

  @NotNull
  @Override
  protected List<ServiceDescriptor> getServices(@NotNull IdeaPluginDescriptor pluginDescriptor) {
    return ((IdeaPluginDescriptorImpl)pluginDescriptor).getModuleServices();
  }

  @Override
  public void projectOpened() {
    for (ModuleComponent component : getComponentInstancesOfType(ModuleComponent.class)) {
      try {
        //noinspection deprecation
        component.projectOpened();
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void projectClosed() {
    List<ModuleComponent> components = getComponentInstancesOfType(ModuleComponent.class);
    for (int i = components.size() - 1; i >= 0; i--) {
      try {
        //noinspection deprecation
        components.get(i).projectClosed();
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  @Override
  @NotNull
  public Project getProject() {
    return myProject;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  public boolean isLoaded() {
    return isModuleAdded;
  }

  @Override
  public void moduleAdded() {
    isModuleAdded = true;
    for (ModuleComponent component : getComponentInstancesOfType(ModuleComponent.class)) {
      component.moduleAdded();
    }
  }

  @Override
  public void setOption(@NotNull String key, @Nullable String value) {
    DeprecatedModuleOptionManager manager = getOptionManager();
    if (value == null) {
      if (manager.state.options.remove(key) != null) {
        manager.incModificationCount();
      }
    }
    else if (!value.equals(manager.state.options.put(key, value))) {
      manager.incModificationCount();
    }
  }

  @NotNull
  private DeprecatedModuleOptionManager getOptionManager() {
    //noinspection ConstantConditions
    return ModuleServiceManager.getService(this, DeprecatedModuleOptionManager.class);
  }

  @Override
  public String getOptionValue(@NotNull String key) {
    return getOptionManager().state.options.get(key);
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleScope() {
    return myModuleScopeProvider.getModuleScope();
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleScope(boolean includeTests) {
    return myModuleScopeProvider.getModuleScope(includeTests);
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleWithLibrariesScope() {
    return myModuleScopeProvider.getModuleWithLibrariesScope();
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleWithDependenciesScope() {
    return myModuleScopeProvider.getModuleWithDependenciesScope();
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleContentScope() {
    return myModuleScopeProvider.getModuleContentScope();
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleContentWithDependenciesScope() {
    return myModuleScopeProvider.getModuleContentWithDependenciesScope();
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests) {
    return myModuleScopeProvider.getModuleWithDependenciesAndLibrariesScope(includeTests);
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleWithDependentsScope() {
    return myModuleScopeProvider.getModuleWithDependentsScope();
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleTestsWithDependentsScope() {
    return myModuleScopeProvider.getModuleTestsWithDependentsScope();
  }

  @NotNull
  @Override
  public GlobalSearchScope getModuleRuntimeScope(boolean includeTests) {
    return myModuleScopeProvider.getModuleRuntimeScope(includeTests);
  }

  @Override
  public void clearScopesCache() {
    myModuleScopeProvider.clearCache();
  }

  @Override
  @SuppressWarnings("HardCodedStringLiteral")
  public String toString() {
    if (myName == null) return "Module (not initialized)";
    return "Module: '" + getName() + "'";
  }

  @NotNull
  @Override
  public <T> T[] getExtensions(@NotNull final ExtensionPointName<T> extensionPointName) {
    return Extensions.getArea(this).getExtensionPoint(extensionPointName).getExtensions();
  }

  @NotNull
  @Override
  protected MutablePicoContainer createPicoContainer() {
    return Extensions.getArea(this).getPicoContainer();
  }

  @Override
  public long getOptionsModificationCount() {
    return getOptionManager().getModificationCount();
  }

  @State(name = "DeprecatedModuleOptionManager", useLoadedStateAsExisting = false /* doesn't make sense to check it */)
  static class DeprecatedModuleOptionManager extends SimpleModificationTracker implements PersistentStateComponent<DeprecatedModuleOptionManager.State>,
                                                                                          ProjectModelElement {
    private final Module module;

    DeprecatedModuleOptionManager(@NotNull Module module) {
      this.module = module;
    }

    @Override
    @Nullable
    public ProjectModelExternalSource getExternalSource() {
      if (state.options.size() > 1 || state.options.size() == 1 && !state.options.containsKey(Module.ELEMENT_TYPE) /* unrealistic case, but just to be sure */) {
        return null;
      }
      return ExternalProjectSystemRegistry.getInstance().getExternalSource(module);
    }

    static final class State {
      @Property(surroundWithTag = false)
      @MapAnnotation(surroundKeyWithTag = false, surroundValueWithTag = false, surroundWithTag = false, entryTagName = "option")
      public final Map<String, String> options = new THashMap<>();
    }

    private State state = new State();

    @Nullable
    @Override
    public State getState() {
      return state;
    }

    @Override
    public void loadState(@NotNull State state) {
      this.state = state;
    }
  }
}
