// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.facet;

import com.intellij.facet.impl.FacetLoadingErrorDescription;
import com.intellij.facet.impl.FacetModelBase;
import com.intellij.facet.impl.FacetModelImpl;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.facet.impl.invalid.InvalidFacet;
import com.intellij.facet.impl.invalid.InvalidFacetConfiguration;
import com.intellij.facet.impl.invalid.InvalidFacetManager;
import com.intellij.facet.impl.invalid.InvalidFacetType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ProjectLoadingErrorsNotifier;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectUtilCore;
import com.intellij.openapi.roots.ExternalProjectSystemRegistry;
import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownFeaturesCollector;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.facet.FacetManagerState;
import org.jetbrains.jps.model.serialization.facet.FacetState;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author nik
 */
@State(name = FacetManagerImpl.COMPONENT_NAME, useLoadedStateAsExisting = false)
public final class FacetManagerImpl extends FacetManager implements ModuleComponent, PersistentStateComponent<FacetManagerState> {
  private static final Logger LOG = Logger.getInstance(FacetManagerImpl.class);
  @NonNls public static final String COMPONENT_NAME = "FacetManager";

  private final Module myModule;
  private final FacetManagerModel myModel = new FacetManagerModel();
  private boolean myInsideCommit;
  private final MessageBus myMessageBus;
  private boolean myModuleAdded;
  private final FacetFromExternalSourcesStorage myExternalSourcesStorage;

  public FacetManagerImpl(final Module module, MessageBus messageBus, FacetFromExternalSourcesStorage externalSourcesStorage) {
    myModule = module;
    myMessageBus = messageBus;
    //explicit dependency on FacetFromExternalSourcesStorage is required to ensure that it'll initialized and its settings will be stored on save
    myExternalSourcesStorage = externalSourcesStorage;
  }

  @Override
  @NotNull
  public ModifiableFacetModel createModifiableModel() {
    FacetModelImpl model = new FacetModelImpl(this);
    model.addFacetsFromManager();
    return model;
  }

  @Override
  @NotNull
  public Facet<?>[] getAllFacets() {
    return myModel.getAllFacets();
  }

  @Override
  @Nullable
  public <F extends Facet<?>> F getFacetByType(FacetTypeId<F> typeId) {
    return myModel.getFacetByType(typeId);
  }

  @Override
  @Nullable
  public <F extends Facet<?>> F findFacet(final FacetTypeId<F> type, final String name) {
    return myModel.findFacet(type, name);
  }

  @Override
  @Nullable
  public <F extends Facet<?>> F getFacetByType(@NotNull final Facet<?> underlyingFacet, final FacetTypeId<F> typeId) {
    return myModel.getFacetByType(underlyingFacet, typeId);
  }

  @Override
  @NotNull
  public <F extends Facet<?>> Collection<F> getFacetsByType(@NotNull final Facet<?> underlyingFacet, final FacetTypeId<F> typeId) {
    return myModel.getFacetsByType(underlyingFacet, typeId);
  }


  @Override
  @NotNull
  public <F extends Facet<?>> Collection<F> getFacetsByType(FacetTypeId<F> typeId) {
    return myModel.getFacetsByType(typeId);
  }


  @Override
  @NotNull
  public Facet<?>[] getSortedFacets() {
    return myModel.getSortedFacets();
  }

  @Override
  @NotNull
  public String getFacetName(@NotNull Facet<?> facet) {
    return myModel.getFacetName(facet);
  }

  @Override
  @NotNull
  public <F extends Facet<?>, C extends FacetConfiguration> F createFacet(@NotNull final FacetType<F, C> type, @NotNull final String name, @NotNull final C configuration,
                                                                          @Nullable final Facet<?> underlying) {
    final F facet = type.createFacet(myModule, name, configuration, underlying);
    assertTrue(facet.getModule() == myModule, facet, "module");
    assertTrue(facet.getConfiguration() == configuration, facet, "configuration");
    assertTrue(Comparing.equal(facet.getName(), name), facet, "name");
    assertTrue(facet.getUnderlyingFacet() == underlying, facet, "underlyingFacet");
    return facet;
  }

  @Override
  @NotNull
  public <F extends Facet<?>, C extends FacetConfiguration> F createFacet(@NotNull final FacetType<F, C> type, @NotNull final String name, @Nullable final Facet<?> underlying) {
    C configuration = ProjectFacetManager.getInstance(myModule.getProject()).createDefaultConfiguration(type);
    return createFacet(type, name, configuration, underlying);
  }

  @Override
  @NotNull
  public <F extends Facet<?>, C extends FacetConfiguration> F addFacet(@NotNull final FacetType<F, C> type, @NotNull final String name, @Nullable final Facet<?> underlying) {
    final ModifiableFacetModel model = createModifiableModel();
    final F facet = createFacet(type, name, underlying);
    model.addFacet(facet);
    model.commit();
    return facet;
  }

  private static void assertTrue(final boolean value, final Facet<?> facet, final String parameter) {
    if (!value) {
      LOG.error("Facet type " + facet.getType().getClass().getName() + " violates the contract of FacetType.createFacet method about '" +
                parameter + "' parameter");
    }
  }

  private void addFacets(final List<? extends FacetState> facetStates, final Facet<?> underlyingFacet, ModifiableFacetModel model) {

    FacetTypeRegistry registry = FacetTypeRegistry.getInstance();
    for (FacetState child : facetStates) {
      final String typeId = child.getFacetType();
      if (typeId == null) {
        addInvalidFacet(child, model, underlyingFacet, ProjectBundle.message("error.message.facet.type.isn.t.specified"));
        continue;
      }

      final FacetType<?,?> type = registry.findFacetType(typeId);
      if (type == null) {
        addInvalidFacet(child, model, underlyingFacet, ProjectBundle.message("error.message.unknown.facet.type.0", typeId), typeId);
        continue;
      }

      ModuleType<?> moduleType = ModuleType.get(myModule);
      if (!type.isSuitableModuleType(moduleType)) {
        addInvalidFacet(child, model, underlyingFacet, ProjectBundle.message("error.message.0.facets.are.not.allowed.in.1",
                                                                      type.getPresentableName(), moduleType.getName()));
        continue;
      }

      FacetType<?,?> expectedUnderlyingType = null;
      FacetTypeId<?> underlyingTypeId = type.getUnderlyingFacetType();
      if (underlyingTypeId != null) {
        expectedUnderlyingType = registry.findFacetType(underlyingTypeId);
      }
      FacetType<?, ?> actualUnderlyingType = underlyingFacet != null ? underlyingFacet.getType() : null;
      if (expectedUnderlyingType != null) {
        if (!expectedUnderlyingType.equals(actualUnderlyingType)) {
          addInvalidFacet(child, model, underlyingFacet, ProjectBundle.message("error.message.0.facet.must.be.placed.under.1.facet",
                                                                        type.getPresentableName(),
                                                                        expectedUnderlyingType.getPresentableName()));
          continue;
        }
      }
      else if (actualUnderlyingType != null) {
        addInvalidFacet(child, model, underlyingFacet, ProjectBundle.message("error.message.0.cannot.be.placed.under.1",
                                                                      type.getPresentableName(), actualUnderlyingType.getPresentableName()));
        continue;
      }

      try {
        addFacet(type, child, underlyingFacet, model);
      }
      catch (InvalidDataException e) {
        LOG.info(e);
        addInvalidFacet(child, model, underlyingFacet, ProjectBundle.message("error.message.cannot.load.facet.configuration.0", e.getMessage()));
      }
    }
  }

  private void addInvalidFacet(final FacetState state,
                               ModifiableFacetModel model,
                               final Facet<?> underlyingFacet,
                               final String errorMessage) {
    addInvalidFacet(state, model, underlyingFacet, errorMessage, null);
  }

  private void addInvalidFacet(final FacetState state,
                               ModifiableFacetModel model,
                               final Facet<?> underlyingFacet,
                               final String errorMessage,
                               final String typeId) {
    final InvalidFacetManager invalidFacetManager = InvalidFacetManager.getInstance(myModule.getProject());
    final InvalidFacetType type = InvalidFacetType.getInstance();
    final InvalidFacetConfiguration configuration = new InvalidFacetConfiguration(state, errorMessage);
    final InvalidFacet facet = createFacet(type, StringUtil.notNullize(state.getName()), configuration, underlyingFacet);
    model.addFacet(facet);
    if (!invalidFacetManager.isIgnored(facet)) {
      FacetLoadingErrorDescription description = new FacetLoadingErrorDescription(facet);
      ProjectLoadingErrorsNotifier.getInstance(myModule.getProject()).registerError(description);
      if (typeId != null) {
        UnknownFeaturesCollector.getInstance(myModule.getProject()).registerUnknownFeature("com.intellij.facetType", typeId, "Facet");
      }
    }
  }

  private <F extends Facet<C>, C extends FacetConfiguration> void addFacet(final FacetType<F, C> type, final FacetState state, final Facet<?> underlyingFacet,
                                                                           final ModifiableFacetModel model) throws InvalidDataException {
    Collection<F> facetsOfThisType = underlyingFacet == null ? model.getFacetsByType(type.getId())
                                                             : model.getFacetsByType(underlyingFacet, type.getId());
    if (type.isOnlyOneFacetAllowed() && !facetsOfThisType.isEmpty() && facetsOfThisType.stream().anyMatch(f -> !f.getName().equals(state.getName()))) {
      LOG.info("'" + state.getName() + "' facet removed from module " + myModule.getName() + ", because only one "
               + type.getPresentableName() + " facet allowed");
      return;
    }

    F facet = null;
    if (!facetsOfThisType.isEmpty() && ProjectUtilCore.isExternalStorageEnabled(myModule.getProject())) {
      facet = facetsOfThisType.stream().filter(f -> f.getName().equals(state.getName())).findFirst().orElse(null);
      if (facet != null) {
        Element newConfiguration = state.getConfiguration();
        //There may be two states of the same facet if configuration is stored in one file but configuration of its sub-facet is stored in another.
        //In that case only one of the states will have the real configuration and we'll merge them here.
        if (newConfiguration != null) {
          FacetUtil.loadFacetConfiguration(facet.getConfiguration(), newConfiguration);
        }
      }
    }

    if (facet == null) {
      final C configuration = type.createDefaultConfiguration();
      final Element config = state.getConfiguration();
      FacetUtil.loadFacetConfiguration(configuration, config);
      String name = state.getName();
      facet = createFacet(type, name, configuration, underlyingFacet);
      if (facet instanceof JDOMExternalizable) {
        //todo[nik] remove
        ((JDOMExternalizable)facet).readExternal(config);
      }
      String externalSystemId = state.getExternalSystemId();
      if (externalSystemId != null) {
        facet.setExternalSource(ExternalProjectSystemRegistry.getInstance().getSourceById(externalSystemId));
      }
      model.addFacet(facet);
    }
    addFacets(state.getSubFacets(), facet, model);
  }

  @Override
  public void noStateLoaded() {
    doLoadState(null);
  }

  @Override
  public void loadState(@NotNull FacetManagerState state) {
    doLoadState(state);
  }

  protected void doLoadState(@Nullable FacetManagerState state) {
    ModifiableFacetModel model = new FacetModelImpl(this);
    FacetManagerState importedFacetsState = myExternalSourcesStorage.getLoadedState();
    addFacets(ContainerUtil.concat(state == null ? Collections.emptyList() : state.getFacets(), importedFacetsState.getFacets()), null, model);
    commit(model, false);
  }

  @Override
  @NotNull
  public FacetManagerState getState() {
    return saveState(getImportedFacetPredicate(myModule.getProject()).negate());
  }

  @NotNull
  static Predicate<Facet<?>> getImportedFacetPredicate(@NotNull Project project) {
    if (ProjectUtilCore.isExternalStorageEnabled(project)) {
      //we can store imported facets in a separate component only if that component will be stored separately, otherwise we will get modified *.iml files
      return facet -> facet.getExternalSource() != null;
    }
    return facet -> false;
  }

  @NotNull
  FacetManagerState saveState(Predicate<? super Facet<?>> filter) {
    FacetManagerState managerState = new FacetManagerState();

    final Facet<?>[] facets = getSortedFacets();

    Map<Facet<?>, List<FacetState>> states = new HashMap<>();
    states.put(null, managerState.getFacets());

    for (Facet<?> facet : facets) {
      if (!filter.test(facet)) continue;
      final Facet<?> underlyingFacet = facet.getUnderlyingFacet();

      FacetState facetState = createFacetState(facet, myModule.getProject());
      if (!(facet instanceof InvalidFacet)) {
        final Element config;
        try {
          FacetConfiguration configuration = facet.getConfiguration();
          config = FacetUtil.saveFacetConfiguration(configuration);
          if (facet instanceof JDOMExternalizable) {
            //todo[nik] remove
            ((JDOMExternalizable)facet).writeExternal(config);
          }
        }
        catch (WriteExternalException e) {
          continue;
        }
        facetState.setConfiguration(config);
      }

      getOrCreateTargetFacetList(underlyingFacet, states, myModule.getProject()).add(facetState);
      states.put(facet, facetState.getSubFacets());
    }
    return managerState;
  }

  /**
   * Configuration of some facet may be stored in one file, but configuration of its underlying facet may be stored in another file. For such
   * sub-facets we create parent elements which don't store configuration but only name and type.
   */
  private static List<FacetState> getOrCreateTargetFacetList(Facet<?> underlyingFacet, Map<Facet<?>, List<FacetState>> states, @NotNull Project project) {
    List<FacetState> facetStateList = states.get(underlyingFacet);
    if (facetStateList == null) {
      FacetState state = createFacetState(underlyingFacet, project);
      getOrCreateTargetFacetList(underlyingFacet.getUnderlyingFacet(), states, project).add(state);
      facetStateList = state.getSubFacets();
      states.put(underlyingFacet, facetStateList);
    }
    return facetStateList;
  }

  private static FacetState createFacetState(@NotNull Facet<?> facet, @NotNull Project project) {
    if (facet instanceof InvalidFacet) {
      return ((InvalidFacet)facet).getConfiguration().getFacetState();
    }
    else {
      FacetState facetState = new FacetState();
      ProjectModelExternalSource externalSource = facet.getExternalSource();
      if (externalSource != null && ProjectUtilCore.isExternalStorageEnabled(project)) {
        //set this attribute only if such facets will be stored separately, otherwise we will get modified *.iml files
        facetState.setExternalSystemId(externalSource.getId());
      }
      facetState.setFacetType(facet.getType().getStringId());
      facetState.setName(facet.getName());
      return facetState;
    }
  }


  public void commit(final ModifiableFacetModel model) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    commit(model, true);
  }

  private void commit(final ModifiableFacetModel model, final boolean fireEvents) {
    LOG.assertTrue(!myInsideCommit, "Recursive commit");

    Set<Facet<?>> toRemove = ContainerUtil.set(getAllFacets());
    List<Facet<?>> toAdd = new ArrayList<>();
    List<FacetRenameInfo> toRename = new ArrayList<>();

    final FacetManagerListener publisher = myMessageBus.syncPublisher(FACETS_TOPIC);

    try {
      myInsideCommit = true;

      for (Facet<?> facet : model.getAllFacets()) {
        boolean isNew = !toRemove.remove(facet);
        if (isNew) {
          toAdd.add(facet);
        }
      }

      List<Facet<?>> newFacets = new ArrayList<>();
      for (Facet<?> facet : getAllFacets()) {
        if (!toRemove.contains(facet)) {
          newFacets.add(facet);
        }
      }
      newFacets.addAll(toAdd);

      for (Facet<?> facet : newFacets) {
        final String newName = model.getNewName(facet);
        if (newName != null && !newName.equals(facet.getName())) {
          toRename.add(new FacetRenameInfo(facet, facet.getName(), newName));
        }
      }

      if (fireEvents) {
        for (Facet<?> facet : toAdd) {
          publisher.beforeFacetAdded(facet);
        }
        for (Facet<?> facet : toRemove) {
          publisher.beforeFacetRemoved(facet);
        }
        for (FacetRenameInfo info : toRename) {
          publisher.beforeFacetRenamed(info.myFacet);
        }
      }

      for (FacetRenameInfo info : toRename) {
        info.myFacet.setName(info.myNewName);
      }
      myModel.setAllFacets(newFacets.toArray(Facet.EMPTY_ARRAY));
    }
    finally {
      myInsideCommit = false;
    }

    if (myModuleAdded) {
      for (Facet<?> facet : toAdd) {
        facet.initFacet();
      }
    }
    for (Facet<?> facet : toRemove) {
      Disposer.dispose(facet);
    }

    if (fireEvents) {
      for (Facet<?> facet : toAdd) {
        publisher.facetAdded(facet);
      }
      for (Facet<?> facet : toRemove) {
        publisher.facetRemoved(facet);
      }
      for (FacetRenameInfo info : toRename) {
        publisher.facetRenamed(info.myFacet, info.myOldName);
      }
    }
    for (Facet<?> facet : toAdd) {
      final Module module = facet.getModule();
      if (!module.equals(myModule)) {
        LOG.error(facet + " is created for module " + module + " but added to module " + myModule);
      }
      final FacetType<?,?> type = facet.getType();
      if (type.isOnlyOneFacetAllowed()) {
        if (type.getUnderlyingFacetType() == null) {
          final Collection<?> facets = getFacetsByType(type.getId());
          if (facets.size() > 1) {
            LOG.error("Only one '" + type.getPresentableName() + "' facet per module allowed, but " + facets.size() + " facets found in module '" +
                      myModule.getName() + "'");
          }
        }
        else {
          final Facet<?> underlyingFacet = facet.getUnderlyingFacet();
          LOG.assertTrue(underlyingFacet != null, "Underlying facet is not specified for '" + facet.getName() + "'");
          final Collection<?> facets = getFacetsByType(underlyingFacet, type.getId());
          if (facets.size() > 1) {
            LOG.error("Only one '" + type.getPresentableName() + "' facet per parent facet allowed, but " + facets.size() + " sub-facets found in facet " + underlyingFacet.getName());
          }
        }
      }
    }
  }

  public void setExternalSource(Facet<?> facet, ProjectModelExternalSource externalSource) {
    facet.setExternalSource(externalSource);
  }

  Set<ProjectModelExternalSource> getExternalSources() {
    return myModel.myExternalSources;
  }

  @Override
  public void moduleAdded() {
    if (myModuleAdded) return;

    for (Facet<?> facet : getAllFacets()) {
      facet.initFacet();
    }
    myModuleAdded = true;
  }

  private static class FacetManagerModel extends FacetModelBase {
    private Facet<?>[] myAllFacets = Facet.EMPTY_ARRAY;
    private final Set<ProjectModelExternalSource> myExternalSources = new LinkedHashSet<>();

    @Override
    @NotNull
    public Facet<?>[] getAllFacets() {
      return myAllFacets;
    }

    void setAllFacets(final Facet<?>[] allFacets) {
      myExternalSources.clear();
      for (Facet<?> facet : allFacets) {
        ContainerUtil.addIfNotNull(myExternalSources, facet.getExternalSource());
      }
      myAllFacets = allFacets;
      facetsChanged();
    }
  }

  private static class FacetRenameInfo {
    private final Facet<?> myFacet;
    private final String myOldName;
    private final String myNewName;

    FacetRenameInfo(final Facet<?> facet, final String oldName, final String newName) {
      myFacet = facet;
      myOldName = oldName;
      myNewName = newName;
    }
  }
}
