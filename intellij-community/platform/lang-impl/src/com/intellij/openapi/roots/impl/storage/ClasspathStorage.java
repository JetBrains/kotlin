// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.storage;

import com.intellij.ProjectTopics;
import com.intellij.application.options.PathMacrosCollector;
import com.intellij.configurationStore.*;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PathMacroSubstitutor;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl.ModuleRootManagerState;
import com.intellij.openapi.roots.impl.RootModelImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.Function;
import com.intellij.util.messages.MessageBusConnection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

// Boolean - false as not loaded, true as loaded
public final class ClasspathStorage extends StateStorageBase<Boolean> {
  private static final Key<Boolean> ERROR_NOTIFIED_KEY = Key.create("ClasspathStorage.ERROR_NOTIFIED_KEY");
  private static final Logger LOG = Logger.getInstance(ClasspathStorage.class);

  private final ClasspathStorageProvider.ClasspathConverter myConverter;

  private final PathMacroSubstitutor myPathMacroSubstitutor;

  public ClasspathStorage(@NotNull final Module module, @NotNull StateStorageManager storageManager) {
    String storageType = module.getOptionValue(JpsProjectLoader.CLASSPATH_ATTRIBUTE);
    if (storageType == null) {
      throw new IllegalStateException("Classpath storage requires non-default storage type");
    }

    ClasspathStorageProvider provider = getProvider(storageType);
    if (provider == null) {
      if (module.getUserData(ERROR_NOTIFIED_KEY) == null) {
        Notification n = new Notification(StorageUtilKt.NOTIFICATION_GROUP_ID, "Cannot load module '" + module.getName() + "'",
                                          "Support for " + storageType + " format is not installed.", NotificationType.ERROR);
        n.notify(module.getProject());
        module.putUserData(ERROR_NOTIFIED_KEY, Boolean.TRUE);
        LOG.info("Classpath storage provider " + storageType + " not found");
      }

      myConverter = new MissingClasspathConverter();
    } else {
      myConverter = provider.createConverter(module);
    }

    myPathMacroSubstitutor = storageManager.getMacroSubstitutor();

    final List<String> paths = myConverter.getFilePaths();
    MessageBusConnection busConnection = module.getMessageBus().connect();
    busConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        if (paths.isEmpty()) {
          return;
        }

        for (VFileEvent event : events) {
          if (!event.isFromRefresh() ||
              !(event instanceof VFileContentChangeEvent) ||
              !StateStorageManagerKt.isFireStorageFileChangedEvent(event)) {
            continue;
          }

          String eventPath = event.getPath();
          for (String path : paths) {
            if (path.equals(eventPath)) {
              StoreReloadManager.getInstance().storageFilesChanged(Collections.singletonMap(module, Collections.singletonList(ClasspathStorage.this)));
              return;
            }
          }
        }
      }
    });

    busConnection.subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void modulesRenamed(@NotNull Project project,
                                 @NotNull List<Module> modules,
                                 @NotNull Function<Module, String> oldNameProvider) {
        for (Module renamedModule : modules) {
          if (renamedModule.equals(module)) {
            ClasspathStorageProvider provider = getProvider(ClassPathStorageUtil.getStorageType(module));
            if (provider != null) {
              provider.moduleRenamed(module, oldNameProvider.fun(module), module.getName());
              provider.modulePathChanged(module);
            }
          }
        }
      }
    });
  }

  @Nullable
  @Override
  public <S> S deserializeState(@Nullable Element serializedState, @NotNull Class<S> stateClass, @Nullable S mergeInto) {
    if (serializedState == null) {
      return null;
    }

    ModuleRootManagerState state = new ModuleRootManagerState();
    state.readExternal(serializedState);
    //noinspection unchecked
    return (S)state;
  }

  @Override
  protected boolean hasState(@NotNull Boolean storageData, @NotNull String componentName) {
    return !storageData;
  }

  @Nullable
  @Override
  public Element getSerializedState(@NotNull Boolean storageData, Object component, @NotNull String componentName, boolean archive) {
    if (storageData) {
      return null;
    }

    Element element = new Element("component");
    ApplicationManager.getApplication().runReadAction(() -> {
      ModifiableRootModel model = null;
      try {
        model = ((ModuleRootManagerImpl)component).getModifiableModel();
        // IDEA-137969 Eclipse integration: external remove of classpathentry is not synchronized
        model.clear();
        try {
          myConverter.readClasspath(model);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
        ((RootModelImpl)model).writeExternal(element);
      }
      catch (WriteExternalException e) {
        LOG.error(e);
      }
      finally {
        if (model != null) {
          model.dispose();
        }
      }
    });


    if (myPathMacroSubstitutor != null) {
      myPathMacroSubstitutor.expandPaths(element);
      if (myPathMacroSubstitutor instanceof TrackingPathMacroSubstitutor) {
        ((TrackingPathMacroSubstitutor)myPathMacroSubstitutor).addUnknownMacros("NewModuleRootManager", PathMacrosCollector.getMacroNames(element));
      }
    }

    getStorageDataRef().set(true);
    return element;
  }

  @NotNull
  @Override
  protected Boolean loadData() {
    return false;
  }

  @Override
  @Nullable
  public SaveSessionProducer createSaveSessionProducer() {
    return myConverter.startExternalization();
  }

  @Override
  public void analyzeExternalChangesAndUpdateIfNeeded(@NotNull Set<? super String> componentNames) {
    // if some file changed, so, changed
    componentNames.add("NewModuleRootManager");
    getStorageDataRef().set(false);
  }

  @Override
  public boolean isUseVfsForWrite() {
    return true;
  }

  @Nullable
  public static ClasspathStorageProvider getProvider(@NotNull String type) {
    if (type.equals(ClassPathStorageUtil.DEFAULT_STORAGE)) {
      return null;
    }

    for (ClasspathStorageProvider provider : ClasspathStorageProvider.EXTENSION_POINT_NAME.getExtensions()) {
      if (type.equals(provider.getID())) {
        return provider;
      }
    }
    return null;
  }

  @NotNull
  public static String getStorageRootFromOptions(@NotNull Module module) {
    String moduleRoot = ModuleUtilCore.getModuleDirPath(module);
    String storageRef = module.getOptionValue(JpsProjectLoader.CLASSPATH_DIR_ATTRIBUTE);
    if (storageRef == null) {
      return moduleRoot;
    }

    storageRef = FileUtil.toSystemIndependentName(storageRef);
    if (SystemInfo.isWindows ? FileUtil.isAbsolutePlatformIndependent(storageRef) : FileUtil.isUnixAbsolutePath(storageRef)) {
      return storageRef;
    }
    else {
      return moduleRoot + '/' + storageRef;
    }
  }

  public static void setStorageType(@NotNull ModuleRootModel model, @NotNull String storageId) {
    Module module = model.getModule();
    String oldStorageType = ClassPathStorageUtil.getStorageType(module);
    if (oldStorageType.equals(storageId)) {
      return;
    }

    ClasspathStorageProvider provider = getProvider(oldStorageType);
    if (provider != null) {
      provider.detach(module);
    }

    provider = getProvider(storageId);
    module.setOption(JpsProjectLoader.CLASSPATH_ATTRIBUTE, provider == null ? null : storageId);
    module.setOption(JpsProjectLoader.CLASSPATH_DIR_ATTRIBUTE, provider == null ? null : provider.getContentRoot(model));
  }

  public static void modulePathChanged(@NotNull Module module) {
    ClasspathStorageProvider provider = getProvider(ClassPathStorageUtil.getStorageType(module));
    if (provider != null) {
      provider.modulePathChanged(module);
    }
  }

  private static class MissingClasspathConverter implements ClasspathStorageProvider.ClasspathConverter {
    @NotNull
    @Override
    public List<String> getFilePaths() {
      return Collections.emptyList();
    }

    @Override
    public void readClasspath(@NotNull ModifiableRootModel model) {
    }
  }
}
