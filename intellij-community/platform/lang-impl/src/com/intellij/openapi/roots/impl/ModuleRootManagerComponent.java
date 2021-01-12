// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.components.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.ModuleEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import com.intellij.openapi.roots.impl.storage.ClassPathStorageUtil;
import com.intellij.openapi.roots.impl.storage.ClasspathStorage;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

/**
 * This class isn't used in the new implementation of project model, which is based on {@link com.intellij.workspaceModel.ide Workspace Model}.
 * It shouldn't be used directly, its base class {@link com.intellij.openapi.roots.ModuleRootManagerEx} should be used instead.
 */

@State(
  name = "NewModuleRootManager",
  storages = {
    @Storage(StoragePathMacros.MODULE_FILE),
    @Storage(storageClass = ClasspathStorage.class)
  },
  // will be changed only on actual user change, so, to speed up module loading, disable
  useLoadedStateAsExisting = false
)
class ModuleRootManagerComponent extends ModuleRootManagerImpl implements
                                                                      PersistentStateComponentWithModificationTracker<ModuleRootManagerImpl.ModuleRootManagerState>,
                                                                      StateStorageChooserEx {
  ModuleRootManagerComponent(Module module) {
    super(module);
  }

  @NotNull
  @Override
  public Resolution getResolution(@NotNull Storage storage, @NotNull StateStorageOperation operation) {
    boolean isClasspathStorage = storage.storageClass() == ClasspathStorage.class;
    boolean isEffectiveStorage = ClassPathStorageUtil.isClasspathStorage(getModule()) == isClasspathStorage;
    if (operation == StateStorageOperation.READ) {
      return isEffectiveStorage ? Resolution.DO : Resolution.SKIP;
    }
    else {
      // IDEA-133480 Eclipse integration: .iml content is not reduced on setting Dependencies Storage Format = Eclipse
      // We clear any storage except eclipse (because we must not clear shared files).
      // Currently there is only one known non-default storage - ExternalProjectStorage.
      return isEffectiveStorage ? Resolution.DO : isClasspathStorage ? Resolution.SKIP : Resolution.CLEAR;
    }
  }

  @Override
  public long getStateModificationCount() {
    Module module = getModule();
    if (!module.isLoaded() || !(module instanceof ModuleEx)) {
      return myModificationTracker.getModificationCount();
    }

    final long[] result = {myModificationTracker.getModificationCount()};
    result[0] += ((ModuleEx)module).getOptionsModificationCount();
    final List<String> handledLibraryTables = new SmartList<>();
    getRootModel().orderEntries().forEachLibrary(library -> {
      LibraryTable table = library.getTable();
      if (table instanceof LibraryTableBase && !handledLibraryTables.contains(table.getTableLevel())) {
        handledLibraryTables.add(table.getTableLevel());
        long count = ((LibraryTableBase)table).getStateModificationCount();
        if (count > 0 && Registry.is("store.track.module.root.manager.changes", false)) {
          LOG.error("modification count changed due to library  " + library.getName() + " change (" + count + "), module " + getModule().getName());
        }
        result[0] += count;
      }
      return true;
    });
    return result[0] + myRootModel.getStateModificationCount();
  }

  @Override
  @TestOnly
  public long getModificationCountForTests() {
    return getStateModificationCount();
  }
}
