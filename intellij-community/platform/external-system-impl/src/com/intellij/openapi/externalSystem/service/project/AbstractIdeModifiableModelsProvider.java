/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetModel;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectCoordinate;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.impl.ModifiableModelCommitter;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ArtifactModel;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.ManifestFileProvider;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.DefaultManifestFileProvider;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphGenerator;
import com.intellij.util.graph.InboundSemiGraph;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.isRelated;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.toCanonicalPath;

public abstract class AbstractIdeModifiableModelsProvider extends IdeModelsProviderImpl implements IdeModifiableModelsProvider {
  private static final Logger LOG = Logger.getInstance(AbstractIdeModifiableModelsProvider.class);

  private ModifiableModuleModel myModifiableModuleModel;
  private final Map<Module, ModifiableRootModel> myModifiableRootModels = new THashMap<>();
  private final Map<Module, ModifiableFacetModel> myModifiableFacetModels = new THashMap<>();
  private final Map<Module, String> myProductionModulesForTestModules = new THashMap<>();
  private final Map<Library, Library.ModifiableModel> myModifiableLibraryModels = new IdentityHashMap<>();
  private ModifiableArtifactModel myModifiableArtifactModel;
  private AbstractIdeModifiableModelsProvider.MyPackagingElementResolvingContext myPackagingElementResolvingContext;
  private final ArtifactExternalDependenciesImporter myArtifactExternalDependenciesImporter;
  @Nullable
  private ModifiableWorkspace myModifiableWorkspace;
  private final MyUserDataHolderBase myUserData;
  private volatile boolean myDisposed;

  public AbstractIdeModifiableModelsProvider(@NotNull Project project) {
    super(project);
    myUserData = new MyUserDataHolderBase();
    myArtifactExternalDependenciesImporter = new ArtifactExternalDependenciesImporterImpl();
  }

  protected abstract ModifiableArtifactModel doGetModifiableArtifactModel();

  protected abstract ModifiableModuleModel doGetModifiableModuleModel();

  protected abstract ModifiableRootModel doGetModifiableRootModel(Module module);

  protected abstract ModifiableFacetModel doGetModifiableFacetModel(Module module);

  protected abstract Library.ModifiableModel doGetModifiableLibraryModel(Library library);

  @NotNull
  @Override
  public abstract LibraryTable.ModifiableModel getModifiableProjectLibrariesModel();

  @NotNull
  @Override
  public Module[] getModules() {
    return getModifiableModuleModel().getModules();
  }

  protected void processExternalArtifactDependencies() {
    myArtifactExternalDependenciesImporter.applyChanges(getModifiableArtifactModel(), getPackagingElementResolvingContext());
  }

  @Override
  public PackagingElementResolvingContext getPackagingElementResolvingContext() {
    if (myPackagingElementResolvingContext == null) {
      myPackagingElementResolvingContext = new MyPackagingElementResolvingContext();
    }
    return myPackagingElementResolvingContext;
  }

  @NotNull
  @Override
  public OrderEntry[] getOrderEntries(@NotNull Module module) {
    return getRootModel(module).getOrderEntries();
  }

  @NotNull
  @Override
  public Module newModule(@NotNull final String filePath, final String moduleTypeId) {
    Module module = getModifiableModuleModel().newModule(filePath, moduleTypeId);
    final String moduleName = FileUtil.getNameWithoutExtension(new File(filePath));
    if (!module.getName().equals(moduleName)) {
      try {
        getModifiableModuleModel().renameModule(module, moduleName);
      }
      catch (ModuleWithNameAlreadyExists exists) {
        LOG.warn(exists);
      }
    }

    // set module type id explicitly otherwise it can not be set if there is an existing module (with the same filePath) and w/o 'type' attribute
    module.setModuleType(moduleTypeId);
    return module;
  }

  @NotNull
  @Override
  public Module newModule(@NotNull ModuleData moduleData) {
    String imlName = null;
    for (String candidate: suggestModuleNameCandidates(moduleData)) {
      Module module = findIdeModule(candidate);
      if (module == null) {
        imlName = candidate;
        break;
      }
    }
    assert imlName != null : "Too many duplicated module names";

    String filePath = toCanonicalPath(moduleData.getModuleFileDirectoryPath() + "/" + imlName + ModuleFileType.DOT_DEFAULT_EXTENSION);
    return newModule(filePath, moduleData.getModuleTypeId());
  }

  @Nullable
  @Override
  public Module findIdeModule(@NotNull String ideModuleName) {
    Module module = getModifiableModuleModel().findModuleByName(ideModuleName);
    return module == null ? getModifiableModuleModel().getModuleToBeRenamed(ideModuleName) : module;
  }

  @Nullable
  @Override
  public Library findIdeLibrary(@NotNull LibraryData libraryData) {
    final LibraryTable.ModifiableModel libraryTable = getModifiableProjectLibrariesModel();
    for (Library ideLibrary: libraryTable.getLibraries()) {
      if (isRelated(ideLibrary, libraryData)) return ideLibrary;
    }
    return null;
  }

  @Override
  @NotNull
  public VirtualFile[] getContentRoots(Module module) {
    return getRootModel(module).getContentRoots();
  }

  @NotNull
  @Override
  public VirtualFile[] getSourceRoots(Module module) {
    return getRootModel(module).getSourceRoots();
  }

  @NotNull
  @Override
  public VirtualFile[] getSourceRoots(Module module, boolean includingTests) {
    return getRootModel(module).getSourceRoots(includingTests);
  }

  @NotNull
  @Override
  public ModifiableModuleModel getModifiableModuleModel() {
    if (myModifiableModuleModel == null) {
      myModifiableModuleModel = doGetModifiableModuleModel();
    }
    return myModifiableModuleModel;
  }

  @Override
  @NotNull
  public ModifiableRootModel getModifiableRootModel(Module module) {
    return (ModifiableRootModel)getRootModel(module);
  }

  @NotNull
  private ModuleRootModel getRootModel(Module module) {
    return myModifiableRootModels.computeIfAbsent(module, k -> doGetModifiableRootModel(module));
  }

  @Override
  @NotNull
  public ModifiableFacetModel getModifiableFacetModel(Module module) {
    return myModifiableFacetModels.computeIfAbsent(module, k -> doGetModifiableFacetModel(module));
  }

  @Override
  @NotNull
  public ModifiableArtifactModel getModifiableArtifactModel() {
    if (myModifiableArtifactModel == null) {
      myModifiableArtifactModel = doGetModifiableArtifactModel();
    }
    return myModifiableArtifactModel;
  }

  @Override
  @NotNull
  public Library[] getAllLibraries() {
    return getModifiableProjectLibrariesModel().getLibraries();
  }

  @Override
  @Nullable
  public Library getLibraryByName(String name) {
    return getModifiableProjectLibrariesModel().getLibraryByName(name);
  }

  @Override
  public Library createLibrary(String name) {
    return getModifiableProjectLibrariesModel().createLibrary(name);
  }

  @Override
  public Library createLibrary(String name, @Nullable ProjectModelExternalSource externalSource) {
    return getModifiableProjectLibrariesModel().createLibrary(name, null, externalSource);
  }

  @Override
  public void removeLibrary(Library library) {
    getModifiableProjectLibrariesModel().removeLibrary(library);
  }

  @Override
  public Library.ModifiableModel getModifiableLibraryModel(Library library) {
    return myModifiableLibraryModels.computeIfAbsent(library, k -> doGetModifiableLibraryModel(library));
  }

  @Nullable
  public ModifiableWorkspace getModifiableWorkspace() {
    if (myModifiableWorkspace == null && ExternalProjectsWorkspaceImpl.isDependencySubstitutionEnabled()) {
      myModifiableWorkspace = doGetModifiableWorkspace();
    }
    return myModifiableWorkspace;
  }

  @NotNull
  @Override
  public String[] getLibraryUrls(@NotNull Library library, @NotNull OrderRootType type) {
    final Library.ModifiableModel model = myModifiableLibraryModels.get(library);
    if (model != null) {
      return model.getUrls(type);
    }
    return library.getUrls(type);
  }

  @Override
  public ModalityState getModalityStateForQuestionDialogs() {
    return ModalityState.NON_MODAL;
  }

  @Override
  public ArtifactExternalDependenciesImporter getArtifactExternalDependenciesImporter() {
    return myArtifactExternalDependenciesImporter;
  }

  @NotNull
  @Override
  public List<Module> getAllDependentModules(@NotNull Module module) {
    final ArrayList<Module> list = new ArrayList<>();
    final Graph<Module> graph = getModuleGraph();
    for (Iterator<Module> i = graph.getOut(module); i.hasNext(); ) {
      list.add(i.next());
    }
    return list;
  }

  private ModifiableWorkspace doGetModifiableWorkspace() {
    return ReadAction.compute(() ->
                                ServiceManager.getService(myProject, ExternalProjectsWorkspaceImpl.class)
                                              .createModifiableWorkspace(this));
  }

  private Graph<Module> getModuleGraph() {
    return GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<Module>() {
      @NotNull
      @Override
      public Collection<Module> getNodes() {
        return ContainerUtil.list(getModules());
      }

      @NotNull
      @Override
      public Iterator<Module> getIn(Module m) {
        Module[] dependentModules = getModifiableRootModel(m).getModuleDependencies(true);
        return Arrays.asList(dependentModules).iterator();
      }
    }));
  }

  private static class MyUserDataHolderBase extends UserDataHolderBase {
    void clear() {
      clearUserData();
    }
  }

  private class MyPackagingElementResolvingContext implements PackagingElementResolvingContext {
    private final ModulesProvider myModulesProvider = new MyModulesProvider();
    private final MyFacetsProvider myFacetsProvider = new MyFacetsProvider();
    private final ManifestFileProvider myManifestFileProvider = new DefaultManifestFileProvider(this);

    @Override
    @NotNull
    public Project getProject() {
      return myProject;
    }

    @Override
    @NotNull
    public ArtifactModel getArtifactModel() {
      return AbstractIdeModifiableModelsProvider.this.getModifiableArtifactModel();
    }

    @Override
    @NotNull
    public ModulesProvider getModulesProvider() {
      return myModulesProvider;
    }

    @Override
    @NotNull
    public FacetsProvider getFacetsProvider() {
      return myFacetsProvider;
    }

    @Override
    public Library findLibrary(@NotNull String level, @NotNull String libraryName) {
      if (level.equals(LibraryTablesRegistrar.PROJECT_LEVEL)) {
        return getLibraryByName(libraryName);
      }
      final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(level, myProject);
      return table != null ? table.getLibraryByName(libraryName) : null;
    }

    @NotNull
    @Override
    public ManifestFileProvider getManifestFileProvider() {
      return myManifestFileProvider;
    }
  }

  private class MyModulesProvider implements ModulesProvider {
    @Override
    @NotNull
    public Module[] getModules() {
      return AbstractIdeModifiableModelsProvider.this.getModules();
    }

    @Override
    public Module getModule(@NotNull String name) {
      return AbstractIdeModifiableModelsProvider.this.findIdeModule(name);
    }

    @Override
    public ModuleRootModel getRootModel(@NotNull Module module) {
      return AbstractIdeModifiableModelsProvider.this.getModifiableRootModel(module);
    }

    @NotNull
    @Override
    public FacetModel getFacetModel(@NotNull Module module) {
      return AbstractIdeModifiableModelsProvider.this.getModifiableFacetModel(module);
    }
  }

  private class MyFacetsProvider implements FacetsProvider {
    @Override
    @NotNull
    public Facet[] getAllFacets(Module module) {
      return getModifiableFacetModel(module).getAllFacets();
    }

    @Override
    @NotNull
    public <F extends Facet> Collection<F> getFacetsByType(Module module, FacetTypeId<F> type) {
      return getModifiableFacetModel(module).getFacetsByType(type);
    }

    @Override
    public <F extends Facet> F findFacet(Module module, FacetTypeId<F> type, String name) {
      return getModifiableFacetModel(module).findFacet(type, name);
    }
  }

  @Override
  public void commit() {
    ProjectRootManagerEx.getInstanceEx(myProject).mergeRootsChangesDuring(() -> {
      if (ExternalProjectsWorkspaceImpl.isDependencySubstitutionEnabled()) {
        updateSubstitutions();
      }
      processExternalArtifactDependencies();
      for (Map.Entry<Library, Library.ModifiableModel> entry: myModifiableLibraryModels.entrySet()) {
        Library fromLibrary = entry.getKey();
        Library.ModifiableModel modifiableModel = entry.getValue();
        // removed and (previously) not committed library is being disposed by LibraryTableBase.LibraryModel.removeLibrary
        // the modifiable model of such library shouldn't be committed
        if (fromLibrary instanceof LibraryEx && ((LibraryEx)fromLibrary).isDisposed()) {
          Disposer.dispose(modifiableModel);
        }
        else {
          modifiableModel.commit();
        }
      }
      getModifiableProjectLibrariesModel().commit();

      Collection<ModifiableRootModel> rootModels = myModifiableRootModels.values();
      ModifiableRootModel[] rootModels1 = rootModels.toArray(new ModifiableRootModel[0]);
      for (ModifiableRootModel model: rootModels1) {
        assert !model.isDisposed() : "Already disposed: " + model;
      }

      if (myModifiableModuleModel != null) {
        ModifiableModelCommitter.multiCommit(rootModels1, myModifiableModuleModel);
      }
      else {
        for (ModifiableRootModel model: rootModels1) {
          model.commit();
        }
      }
      for (Map.Entry<Module, String> entry: myProductionModulesForTestModules.entrySet()) {
        TestModuleProperties.getInstance(entry.getKey()).setProductionModuleName(entry.getValue());
      }

      for (Map.Entry<Module, ModifiableFacetModel> each: myModifiableFacetModels.entrySet()) {
        if (!each.getKey().isDisposed()) {
          each.getValue().commit();
        }
      }
      if (myModifiableArtifactModel != null) {
        myModifiableArtifactModel.commit();
      }
    });
    myUserData.clear();
  }

  @Override
  public void dispose() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    assert !myDisposed : "Already disposed!";
    myDisposed = true;

    for (ModifiableRootModel each: myModifiableRootModels.values()) {
      if (each.isDisposed()) continue;
      each.dispose();
    }
    Disposer.dispose(getModifiableProjectLibrariesModel());

    for (Library.ModifiableModel each: myModifiableLibraryModels.values()) {
      if (each instanceof LibraryEx && ((LibraryEx)each).isDisposed()) continue;
      Disposer.dispose(each);
    }

    if (myModifiableModuleModel != null && myModifiableModuleModel.isChanged()) {
      myModifiableModuleModel.dispose();
    }
    if (myModifiableArtifactModel != null) {
      myModifiableArtifactModel.dispose();
    }

    myModifiableRootModels.clear();
    myModifiableFacetModels.clear();
    myModifiableLibraryModels.clear();
    myUserData.clear();
  }

  @Override
  public void setTestModuleProperties(Module testModule, String productionModuleName) {
    myProductionModulesForTestModules.put(testModule, productionModuleName);
  }

  @Nullable
  @Override
  public String getProductionModuleName(Module module) {
    return myProductionModulesForTestModules.get(module);
  }

  @Override
  public ModuleOrderEntry trySubstitute(Module ownerModule, LibraryOrderEntry libraryOrderEntry, ProjectCoordinate publicationId) {
    String workspaceModuleCandidate = findModuleByPublication(publicationId);
    Module workspaceModule = workspaceModuleCandidate == null ? null : findIdeModule(workspaceModuleCandidate);
    if (workspaceModule == null) {
      return null;
    }
    else {
      ModifiableRootModel modifiableRootModel = getModifiableRootModel(ownerModule);
      ModuleOrderEntry moduleOrderEntry = modifiableRootModel.addModuleOrderEntry(workspaceModule);
      moduleOrderEntry.setScope(libraryOrderEntry.getScope());
      moduleOrderEntry.setExported(libraryOrderEntry.isExported());
      ModifiableWorkspace workspace = getModifiableWorkspace();
      assert workspace != null;
      workspace.addSubstitution(ownerModule.getName(),
                                workspaceModule.getName(),
                                libraryOrderEntry.getLibraryName(),
                                libraryOrderEntry.getScope());
      modifiableRootModel.removeOrderEntry(libraryOrderEntry);
      return moduleOrderEntry;
    }
  }

  @Override
  public void registerModulePublication(Module module, ProjectCoordinate modulePublication) {
    ModifiableWorkspace workspace = getModifiableWorkspace();
    if (workspace != null) {
      workspace.register(modulePublication, module);
    }
  }

  @Override
  public boolean isSubstituted(String libraryName) {
    ModifiableWorkspace workspace = getModifiableWorkspace();
    if (workspace == null) return false;
    return workspace.isSubstituted(libraryName);
  }

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return myUserData.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    myUserData.putUserData(key, value);
  }

  @Nullable
  @Override
  public String findModuleByPublication(ProjectCoordinate publicationId) {
    ModifiableWorkspace workspace = getModifiableWorkspace();
    return workspace == null ? null : workspace.findModule(publicationId);
  }

  private void updateSubstitutions() {
    ModifiableWorkspace workspace = getModifiableWorkspace();
    if (workspace == null) return;

    final List<String> oldModules = ContainerUtil.map(ModuleManager.getInstance(myProject).getModules(), module -> module.getName());
    final List<String> newModules = ContainerUtil.map(myModifiableModuleModel.getModules(), module -> module.getName());

    final Collection<String> removedModules = new THashSet<>(oldModules);
    removedModules.removeAll(newModules);


    Map<String, String> toSubstitute = ContainerUtil.newHashMap();
    for (ExternalSystemManager<?, ?, ?, ?, ?> manager: ExternalSystemApiUtil.getAllManagers()) {
      final Collection<ExternalProjectInfo> projectsData =
        ProjectDataManager.getInstance().getExternalProjectsData(myProject, manager.getSystemId());
      for (ExternalProjectInfo projectInfo: projectsData) {
        if (projectInfo.getExternalProjectStructure() == null) {
          continue;
        }

        Collection<DataNode<LibraryData>> libraryNodes =
          ExternalSystemApiUtil.findAll(projectInfo.getExternalProjectStructure(), ProjectKeys.LIBRARY);
        for (DataNode<LibraryData> libraryNode: libraryNodes) {
          String substitutionModuleCandidate = findModuleByPublication(libraryNode.getData());
          if (substitutionModuleCandidate != null) {
            toSubstitute.put(libraryNode.getData().getInternalName(), substitutionModuleCandidate);
          }
        }
      }
    }

    for (Module module: getModules()) {
      ModifiableRootModel modifiableRootModel = getModifiableRootModel(module);
      boolean changed = false;
      OrderEntry[] entries = modifiableRootModel.getOrderEntries();
      for (int i = 0, length = entries.length; i < length; i++) {
        OrderEntry orderEntry = entries[i];
        if (orderEntry instanceof ModuleOrderEntry) {
          String workspaceModule = ((ModuleOrderEntry)orderEntry).getModuleName();
          if (removedModules.contains(workspaceModule)) {
            DependencyScope scope = ((ModuleOrderEntry)orderEntry).getScope();
            if (workspace.isSubstitution(module.getName(), workspaceModule, scope)) {
              String libraryName = workspace.getSubstitutedLibrary(workspaceModule);
              if (libraryName != null) {
                Library library = getLibraryByName(libraryName);
                if (library != null) {
                  modifiableRootModel.removeOrderEntry(orderEntry);
                  entries[i] = modifiableRootModel.addLibraryEntry(library);
                  changed = true;
                  workspace.removeSubstitution(module.getName(), workspaceModule, libraryName, scope);
                }
              }
            }
          }
        }

        if (!(orderEntry instanceof LibraryOrderEntry)) continue;
        LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
        if (!libraryOrderEntry.isModuleLevel() && libraryOrderEntry.getLibraryName() != null) {
          String workspaceModule = toSubstitute.get(libraryOrderEntry.getLibraryName());
          if (workspaceModule != null) {
            Module ideModule = findIdeModule(workspaceModule);
            if (ideModule != null) {
              ModuleOrderEntry moduleOrderEntry = modifiableRootModel.addModuleOrderEntry(ideModule);
              moduleOrderEntry.setScope(libraryOrderEntry.getScope());
              modifiableRootModel.removeOrderEntry(orderEntry);
              entries[i] = moduleOrderEntry;
              changed = true;
              workspace.addSubstitution(module.getName(), workspaceModule,
                                        libraryOrderEntry.getLibraryName(),
                                        libraryOrderEntry.getScope());
            }
          }
        }
      }
      if (changed) {
        modifiableRootModel.rearrangeOrderEntries(entries);
      }
    }

    workspace.commit();
  }
}
