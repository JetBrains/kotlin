// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.application.WriteAction;
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

import java.util.function.Consumer;

final class LibraryKindLoader implements ApplicationInitializedListener {
  @Override
  public void componentsInitialized() {
    //todo[nik] this is temporary workaround for IDEA-98118: we need to initialize all library types to ensure that their kinds are created and registered in LibraryKind.ourAllKinds
    //In order to properly fix the problem we should extract all UI-related methods from LibraryType to a separate class and move LibraryType to intellij.platform.projectModel.impl module
    LibraryType.EP_NAME.getExtensionList();

    LibraryType.EP_NAME.addExtensionPointListener(new ExtensionPointListener<LibraryType<?>>() {
      @Override
      public void extensionAdded(@NotNull LibraryType<?> extension, @NotNull PluginDescriptor pluginDescriptor) {
        WriteAction.run(() -> {
          LibraryKind.registerKind(extension.getKind());
          processAllLibraries(library -> rememberKind(extension.getKind(), library));
        });
      }

      @Override
      public void extensionRemoved(@NotNull LibraryType<?> extension, @NotNull PluginDescriptor pluginDescriptor) {
        LibraryKind.unregisterKind(extension.getKind());
        processAllLibraries(library -> forgetKind(extension.getKind(), library));
      }
    }, null);
  }

  private static void processAllLibraries(@NotNull Consumer<Library> processor) {
    processLibraries(LibraryTablesRegistrar.getInstance().getLibraryTable(), processor);
    for (LibraryTable table : LibraryTablesRegistrar.getInstance().getCustomLibraryTables()) {
      processLibraries(table, processor);
    }
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      processLibraries(LibraryTablesRegistrar.getInstance().getLibraryTable(project), processor);
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        for (Library library : OrderEntryUtil.getModuleLibraries(ModuleRootManager.getInstance(module))) {
          processor.accept(library);
        }
      }
    }
  }

  private static void processLibraries(@NotNull LibraryTable table, Consumer<Library> processor) {
    for (Library library : table.getLibraries()) {
      processor.accept(library);
    }
  }

  private static void forgetKind(@NotNull PersistentLibraryKind<?> kind, @NotNull Library library) {
    if (kind.equals(((LibraryEx)library).getKind())) {
      LibraryEx.ModifiableModelEx model = (LibraryEx.ModifiableModelEx)library.getModifiableModel();
      model.forgetKind();
      model.commit();
    }
  }

  private static void rememberKind(@NotNull PersistentLibraryKind<?> kind, @NotNull Library library) {
    PersistentLibraryKind<?> libraryKind = ((LibraryEx)library).getKind();
    if (libraryKind instanceof UnknownLibraryKind && libraryKind.getKindId().equals(kind.getKindId())) {
      LibraryEx.ModifiableModelEx model = (LibraryEx.ModifiableModelEx)library.getModifiableModel();
      model.restoreKind();
      model.commit();
    }
  }
}
