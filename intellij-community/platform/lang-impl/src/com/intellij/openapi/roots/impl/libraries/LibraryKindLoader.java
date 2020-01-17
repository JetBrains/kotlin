// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.libraries.*;
import org.jetbrains.annotations.NotNull;

public class LibraryKindLoader implements ApplicationInitializedListener {
  @Override
  public void componentsInitialized() {
    //todo[nik] this is temporary workaround for IDEA-98118: we need to initialize all library types to ensure that their kinds are created and registered in LibraryKind.ourAllKinds
    //In order to properly fix the problem we should extract all UI-related methods from LibraryType to a separate class and move LibraryType to intellij.platform.projectModel.impl module
    LibraryType.EP_NAME.getExtensionList();

    LibraryType.EP_NAME.addExtensionPointListener(new ExtensionPointListener<LibraryType<?>>() {
      @Override
      public void extensionRemoved(@NotNull LibraryType<?> extension, @NotNull PluginDescriptor pluginDescriptor) {
        clearKindInAllLibraries(extension.getKind());
      }
    }, null);
  }

  private static void clearKindInAllLibraries(@NotNull PersistentLibraryKind<?> kind) {
    clearKindInLibraries(kind, LibraryTablesRegistrar.getInstance().getLibraryTable());
    for (LibraryTable table : LibraryTablesRegistrar.getInstance().getCustomLibraryTables()) {
      clearKindInLibraries(kind, table);
    }
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      clearKindInLibraries(kind, LibraryTablesRegistrar.getInstance().getLibraryTable(project));
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        for (Library library : OrderEntryUtil.getModuleLibraries(ModuleRootManager.getInstance(module))) {
          clearKind(kind, library);
        }
      }
    }
  }

  private static void clearKindInLibraries(@NotNull PersistentLibraryKind<?> kind, @NotNull LibraryTable table) {
    for (Library library : table.getLibraries()) {
      clearKind(kind, library);
    }
  }

  private static void clearKind(@NotNull PersistentLibraryKind<?> kind, @NotNull Library library) {
    if (kind.equals(((LibraryEx)library).getKind())) {
      LibraryEx.ModifiableModelEx model = (LibraryEx.ModifiableModelEx)library.getModifiableModel();
      model.clearKind();
      model.commit();
    }
  }
}
