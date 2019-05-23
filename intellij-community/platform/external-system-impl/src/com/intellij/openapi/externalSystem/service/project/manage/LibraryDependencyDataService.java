// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.OrderAware;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public class LibraryDependencyDataService extends AbstractDependencyDataService<LibraryDependencyData, LibraryOrderEntry> {
  private static final Logger LOG = Logger.getInstance(LibraryDependencyDataService.class);

  @NotNull
  @Override
  public Key<LibraryDependencyData> getTargetDataKey() {
    return ProjectKeys.LIBRARY_DEPENDENCY;
  }

  @NotNull
  @Override
  public Class<LibraryOrderEntry> getOrderEntryType() {
    return LibraryOrderEntry.class;
  }

  @Override
  protected Map<OrderEntry, OrderAware> importData(@NotNull final Collection<DataNode<LibraryDependencyData>> nodesToImport,
                                                   @NotNull final Module module,
                                                   @NotNull final IdeModifiableModelsProvider modelsProvider) {
    // The general idea is to import all external project library dependencies and module libraries which don't present at the
    // ide side yet and remove all project library dependencies and module libraries which present at the ide but not at
    // the given collection.
    // The trick is that we should perform module settings modification inside try/finally block against target root model.
    // That means that we need to prepare all necessary data, obtain a model and modify it as necessary.
    final Map<Set<String>/* library paths */, LibraryDependencyData> moduleLibrariesToImport =
      new HashMap<>();
    final Map<String/* library name + scope */, LibraryDependencyData> projectLibrariesToImport =
      new HashMap<>();
    final Set<LibraryDependencyData> toImport = new LinkedHashSet<>();
    final Map<OrderEntry, OrderAware> orderEntryDataMap = new LinkedHashMap<>();

    boolean hasUnresolved = false;
    for (DataNode<LibraryDependencyData> dependencyNode : nodesToImport) {
      LibraryDependencyData dependencyData = dependencyNode.getData();
      LibraryData libraryData = dependencyData.getTarget();
      hasUnresolved |= libraryData.isUnresolved();
      switch (dependencyData.getLevel()) {
        case MODULE:
          Set<String> paths = new HashSet<>();
            for (String path : libraryData.getPaths(LibraryPathType.BINARY)) {
              paths.add(ExternalSystemApiUtil.toCanonicalPath(path) + dependencyData.getScope().name());
            }
            moduleLibrariesToImport.put(paths, dependencyData);
            toImport.add(dependencyData);
          break;
        case PROJECT:
          projectLibrariesToImport.put(libraryData.getInternalName() + dependencyData.getScope().name(), dependencyData);
          toImport.add(dependencyData);
      }
    }

    final boolean finalHasUnresolved = hasUnresolved;

    final ModifiableRootModel modifiableRootModel = modelsProvider.getModifiableRootModel(module);
    LibraryTable moduleLibraryTable = modifiableRootModel.getModuleLibraryTable();
    syncExistingAndRemoveObsolete(
      modelsProvider, moduleLibrariesToImport, projectLibrariesToImport, toImport, orderEntryDataMap, modifiableRootModel,
      finalHasUnresolved);
    // Import missing library dependencies.
    if (!toImport.isEmpty()) {
      importMissing(modelsProvider, toImport, orderEntryDataMap, modifiableRootModel, moduleLibraryTable, module);
    }
    return orderEntryDataMap;
  }

  private static void importMissing(@NotNull IdeModifiableModelsProvider modelsProvider,
                                    @NotNull Set<LibraryDependencyData> toImport,
                                    @NotNull Map<OrderEntry, OrderAware> orderEntryDataMap,
                                    @NotNull ModifiableRootModel moduleRootModel,
                                    @NotNull LibraryTable moduleLibraryTable,
                                    @NotNull Module module) {
    for (final LibraryDependencyData dependencyData : toImport) {
      final LibraryData libraryData = dependencyData.getTarget();
      final String libraryName = libraryData.getInternalName();
      switch (dependencyData.getLevel()) {
        case MODULE:
          final Library moduleLib;
          if (libraryName.isEmpty()) {
            moduleLib = moduleLibraryTable.createLibrary();
          }
          else {
            moduleLib = moduleLibraryTable.createLibrary(libraryName);
          }
          final LibraryOrderEntry existingLibraryDependency =
            syncExistingLibraryDependency(modelsProvider, dependencyData, moduleLib, moduleRootModel, module);
          orderEntryDataMap.put(existingLibraryDependency, dependencyData);
          break;
        case PROJECT:
          final Library projectLib = modelsProvider.getLibraryByName(libraryName);
          if (projectLib == null) {
            final LibraryOrderEntry existingProjectLibraryDependency = syncExistingLibraryDependency(
              modelsProvider, dependencyData, moduleLibraryTable.createLibrary(libraryName), moduleRootModel, module);
            orderEntryDataMap.put(existingProjectLibraryDependency, dependencyData);
            break;
          }
          LibraryOrderEntry orderEntry = moduleRootModel.addLibraryEntry(projectLib);
          setLibraryScope(orderEntry, projectLib, module, dependencyData);
          ModuleOrderEntry substitutionEntry = modelsProvider.trySubstitute(module, orderEntry, libraryData);
          if (substitutionEntry != null) {
            orderEntryDataMap.put(substitutionEntry, dependencyData);
          }
          else {
            orderEntryDataMap.put(orderEntry, dependencyData);
          }
      }
    }
  }

  private static void setLibraryScope(@NotNull LibraryOrderEntry orderEntry,
                                      @NotNull Library lib,
                                      @NotNull Module module,
                                      @NotNull LibraryDependencyData dependencyData) {
    orderEntry.setExported(dependencyData.isExported());
    orderEntry.setScope(dependencyData.getScope());
    if(LOG.isDebugEnabled()) {
      LOG.debug(String.format(
        "Configuring library '%s' of module '%s' to be%s exported and have scope %s",
        lib, module.getName(), dependencyData.isExported() ? " not" : "", dependencyData.getScope()
      ));
    }
  }

  private static void syncExistingAndRemoveObsolete(@NotNull IdeModifiableModelsProvider modelsProvider,
                                                    @NotNull Map<Set<String>, LibraryDependencyData> moduleLibrariesToImport,
                                                    @NotNull Map<String, LibraryDependencyData> projectLibrariesToImport,
                                                    @NotNull Set<LibraryDependencyData> toImport,
                                                    @NotNull Map<OrderEntry, OrderAware> orderEntryDataMap,
                                                    @NotNull ModifiableRootModel moduleRootModel,
                                                    boolean hasUnresolvedLibraries) {
    for (OrderEntry entry : moduleRootModel.getOrderEntries()) {
      if (OrderEntryUtil.isModuleLibraryOrderEntry(entry)) {
        LibraryOrderEntry moduleLibraryOrderEntry = (LibraryOrderEntry)entry;
        Library library = moduleLibraryOrderEntry.getLibrary();
        final VirtualFile[] libraryFiles = library.getFiles(OrderRootType.CLASSES);
        final Set<String> moduleLibraryKey = new HashSet<>(libraryFiles.length);
        for (VirtualFile file : libraryFiles) {
          moduleLibraryKey.add(ExternalSystemApiUtil.getLocalFileSystemPath(file) + moduleLibraryOrderEntry.getScope().name());
        }
        LibraryDependencyData existing = moduleLibrariesToImport.remove(moduleLibraryKey);
        if (existing == null || !StringUtil.equals(StringUtil.nullize(existing.getInternalName()), library.getName())) {
          moduleRootModel.removeOrderEntry(entry);
        }
        else {
          orderEntryDataMap.put(entry, existing);
          syncExistingLibraryDependency(modelsProvider, existing, library, moduleRootModel, moduleLibraryOrderEntry.getOwnerModule());
          toImport.remove(existing);
        }
      }
      else if (entry instanceof LibraryOrderEntry) {
        LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)entry;
        String libraryName = libraryOrderEntry.getLibraryName();
        LibraryDependencyData existing = projectLibrariesToImport.remove(libraryName + libraryOrderEntry.getScope().name());
        if (existing != null) {
          String module = modelsProvider.findModuleByPublication(existing.getTarget());
          if(module == null) {
            toImport.remove(existing);
            orderEntryDataMap.put(entry, existing);
            libraryOrderEntry.setExported(existing.isExported());
            libraryOrderEntry.setScope(existing.getScope());
          } else {
            moduleRootModel.removeOrderEntry(entry);
          }
        }
        else if (!hasUnresolvedLibraries) {
          // There is a possible case that a project has been successfully imported from external model and after
          // that network/repo goes down. We don't want to drop existing binary mappings then.
          moduleRootModel.removeOrderEntry(entry);
        }
      }
    }
  }

  private static LibraryOrderEntry syncExistingLibraryDependency(@NotNull IdeModifiableModelsProvider modelsProvider,
                                                                 @NotNull final LibraryDependencyData libraryDependencyData,
                                                                 @NotNull final Library library,
                                                                 @NotNull final ModifiableRootModel moduleRootModel,
                                                                 @NotNull final Module module) {
    final Library.ModifiableModel libraryModel = modelsProvider.getModifiableLibraryModel(library);
    final String libraryName = libraryDependencyData.getInternalName();
    final LibraryData libraryDependencyDataTarget = libraryDependencyData.getTarget();
    Map<OrderRootType, Collection<File>> files = ProjectDataService.EP_NAME.findExtensionOrFail(LibraryDataService.class)
      .prepareLibraryFiles(libraryDependencyDataTarget);
    LibraryDataService.registerPaths(libraryDependencyDataTarget.isUnresolved(), files, libraryModel, libraryName);
    LibraryOrderEntry orderEntry = findLibraryOrderEntry(moduleRootModel, library, libraryDependencyData.getScope());

    assert orderEntry != null;
    setLibraryScope(orderEntry, library, module, libraryDependencyData);
    return orderEntry;
  }

  @Nullable
  private static LibraryOrderEntry findLibraryOrderEntry(@NotNull ModifiableRootModel moduleRootModel,
                                                         @NotNull Library library,
                                                         @NotNull DependencyScope scope) {
    LibraryOrderEntry candidate = null;
    for (OrderEntry orderEntry : moduleRootModel.getOrderEntries()) {
      if (orderEntry instanceof LibraryOrderEntry) {
        final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
        if (library == libraryOrderEntry.getLibrary()) {
          return libraryOrderEntry;
        }
        if (library.equals(libraryOrderEntry.getLibrary())) {
          if (libraryOrderEntry.getScope() == scope) {
            return libraryOrderEntry;
          }
          else {
            candidate = libraryOrderEntry;
          }
        }
      }
    }
    return candidate;
  }
}
