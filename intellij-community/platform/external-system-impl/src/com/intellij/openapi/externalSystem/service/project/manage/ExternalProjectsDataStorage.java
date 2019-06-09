// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.configurationStore.SettingsSavingComponentJavaAdapter;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.openapi.application.PathManagerEx;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.*;
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.serialization.SerializationException;
import com.intellij.serialization.VersionedFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.io.PathKt;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;
import com.intellij.util.xmlb.annotations.XMap;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.externalSystem.model.ProjectKeys.MODULE;
import static com.intellij.openapi.externalSystem.model.ProjectKeys.PROJECT;

/**
 * @author Vladislav.Soroka
 */
@State(name = "ExternalProjectsData", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class ExternalProjectsDataStorage implements SettingsSavingComponentJavaAdapter, PersistentStateComponent<ExternalProjectsDataStorage.State> {
  private static final Logger LOG = Logger.getInstance(ExternalProjectsDataStorage.class);

  // exposed for tests
  public static final int STORAGE_VERSION = 6;

  @NotNull
  private final Project myProject;
  @NotNull
  private final Map<Pair<ProjectSystemId, File>, InternalExternalProjectInfo> myExternalRootProjects =
    ConcurrentCollectionFactory.createMap(ExternalSystemUtil.HASHING_STRATEGY);

  private final AtomicBoolean changed = new AtomicBoolean();
  private State myState = new State();

  public static ExternalProjectsDataStorage getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ExternalProjectsDataStorage.class);
  }

  public ExternalProjectsDataStorage(@NotNull Project project) {
    myProject = project;
  }

  public synchronized void load() {
    myExternalRootProjects.clear();
    long startTs = System.currentTimeMillis();
    long readEnd = startTs;
    try {
      List<InternalExternalProjectInfo> projectInfos = load(myProject);
      readEnd = System.currentTimeMillis();

      if (projectInfos == null || (projectInfos.isEmpty() &&
                                   myProject.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) != Boolean.TRUE &&
                                   hasLinkedExternalProjects())) {
        markDirtyAllExternalProjects();
      }

      for (InternalExternalProjectInfo projectInfo : ContainerUtil.notNullize(projectInfos)) {
        if (validate(projectInfo)) {
          myExternalRootProjects.put(
            Pair.create(projectInfo.getProjectSystemId(), new File(projectInfo.getExternalProjectPath())), projectInfo);
          if (projectInfo.getLastImportTimestamp() != projectInfo.getLastSuccessfulImportTimestamp()) {
            markDirty(projectInfo.getExternalProjectPath());
          }
        }
        else {
          String projectPath = projectInfo.getNullSafeExternalProjectPath();
          if (projectPath != null) {
            markDirty(projectPath);
          }
        }
      }
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.warn(e);
      markDirtyAllExternalProjects();
    }

    mergeLocalSettings();
    long finishTs = System.currentTimeMillis();
    LOG.info("Load external projects data in " + (finishTs - startTs) + " millis (read time: " + (readEnd - startTs) + ")");
  }

  private boolean hasLinkedExternalProjects() {
    return ExternalSystemApiUtil.getAllManagers().stream()
      .anyMatch(manager -> !manager.getSettingsProvider().fun(myProject).getLinkedProjectsSettings().isEmpty());
  }

  private void markDirtyAllExternalProjects() {
    ExternalProjectsManager.getInstance(myProject).getExternalProjectsWatcher().markDirtyAllExternalProjects();
  }

  private void markDirty(String projectPath) {
    ExternalProjectsManager.getInstance(myProject).getExternalProjectsWatcher().markDirty(projectPath);
  }

  private static boolean validate(InternalExternalProjectInfo externalProjectInfo) {
    try {
      final DataNode<ProjectData> projectStructure = externalProjectInfo.getExternalProjectStructure();
      if (projectStructure == null) return false;

      ProjectDataManagerImpl.getInstance().ensureTheDataIsReadyToUse(projectStructure);
      return externalProjectInfo.getExternalProjectPath().equals(projectStructure.getData().getLinkedExternalProjectPath());
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.warn(e);
    }
    return false;
  }

  @Override
  public void doSave() {
    if (!changed.compareAndSet(true, false)) {
      return;
    }

    try {
      //noinspection SynchronizeOnThis
      synchronized (this) {
        long start = System.currentTimeMillis();
        doSave(myProject, myExternalRootProjects.values());
        long duration = System.currentTimeMillis() - start;
        LOG.info("Save external projects data in " + duration + " ms");
      }
    }
    catch (IOException | SerializationException e) {
      LOG.error(e);
    }
  }

  synchronized void update(@NotNull ExternalProjectInfo externalProjectInfo) {
    restoreInclusionSettings(externalProjectInfo.getExternalProjectStructure());

    final ProjectSystemId projectSystemId = externalProjectInfo.getProjectSystemId();
    final String projectPath = externalProjectInfo.getExternalProjectPath();
    DataNode<ProjectData> externalProjectStructure = externalProjectInfo.getExternalProjectStructure();
    long lastSuccessfulImportTimestamp = externalProjectInfo.getLastSuccessfulImportTimestamp();
    long lastImportTimestamp = externalProjectInfo.getLastImportTimestamp();

    final Pair<ProjectSystemId, File> key = Pair.create(projectSystemId, new File(projectPath));
    final InternalExternalProjectInfo old = myExternalRootProjects.get(key);
    if (old != null) {
      lastImportTimestamp = externalProjectInfo.getLastImportTimestamp();
      if (lastSuccessfulImportTimestamp == -1) {
        lastSuccessfulImportTimestamp = old.getLastSuccessfulImportTimestamp();
      }
      if (externalProjectInfo.getExternalProjectStructure() == null) {
        externalProjectStructure = old.getExternalProjectStructure();
      }
      else {
        externalProjectStructure = externalProjectInfo.getExternalProjectStructure().graphCopy();
      }
    }
    else {
      externalProjectStructure = externalProjectStructure != null ? externalProjectStructure.graphCopy() : null;
    }

    InternalExternalProjectInfo merged = new InternalExternalProjectInfo(
      projectSystemId,
      projectPath,
      externalProjectStructure
    );
    merged.setLastImportTimestamp(lastImportTimestamp);
    merged.setLastSuccessfulImportTimestamp(lastSuccessfulImportTimestamp);
    myExternalRootProjects.put(key, merged);

    markAsChangedAndScheduleSave();
  }

  synchronized void restoreInclusionSettings(@Nullable DataNode<ProjectData> projectDataNode) {
    if (projectDataNode == null) {
      return;
    }

    String rootProjectPath = projectDataNode.getData().getLinkedExternalProjectPath();
    ProjectState projectState = myState.map.get(rootProjectPath);
    if (projectState == null) {
      return;
    }

    projectDataNode.visit(node -> {
      final DataNode<ExternalConfigPathAware> projectOrModuleNode = resolveProjectNode(node);
      assert projectOrModuleNode != null;
      final ModuleState moduleState = projectState.map.get(projectOrModuleNode.getData().getLinkedExternalProjectPath());
      node.setIgnored(isIgnored(projectState, moduleState, node.getKey()));
    });
  }

  synchronized void saveInclusionSettings(@Nullable DataNode<ProjectData> projectDataNode) {
    if (projectDataNode == null) return;

    final MultiMap<String, String> inclusionMap = MultiMap.createSmart();
    final MultiMap<String, String> exclusionMap = MultiMap.createSmart();
    projectDataNode.visit(dataNode -> {
      DataNode<ExternalConfigPathAware> projectNode = resolveProjectNode(dataNode);
      if (projectNode != null) {
        final String projectPath = projectNode.getData().getLinkedExternalProjectPath();
        if (projectNode.isIgnored() || dataNode.isIgnored()) {
          exclusionMap.putValue(projectPath, dataNode.getKey().getDataType());
        }
        else {
          inclusionMap.putValue(projectPath, dataNode.getKey().getDataType());
        }
      }
    });
    final MultiMap<String, String> map;
    ProjectState projectState = new ProjectState();
    if (inclusionMap.size() < exclusionMap.size()) {
      projectState.isInclusion = true;
      map = inclusionMap;
    }
    else {
      projectState.isInclusion = false;
      map = exclusionMap;
    }

    for (String path : map.keySet()) {
      projectState.map.put(path, new ModuleState(map.get(path)));
    }

    myState.map.put(projectDataNode.getData().getLinkedExternalProjectPath(), projectState);
    markAsChangedAndScheduleSave();
  }

  private void markAsChangedAndScheduleSave() {
    if (changed.compareAndSet(false, true)) {
      SaveAndSyncHandler.getInstance().scheduleSave(SaveAndSyncHandler.SaveTask.projectIncludingAllSettings(myProject), false);
    }
  }

  @Nullable
  synchronized ExternalProjectInfo get(@NotNull ProjectSystemId projectSystemId, @NotNull String externalProjectPath) {
    return myExternalRootProjects.get(Pair.create(projectSystemId, new File(externalProjectPath)));
  }

  synchronized void remove(@NotNull ProjectSystemId projectSystemId, @NotNull String externalProjectPath) {
    final InternalExternalProjectInfo removed = myExternalRootProjects.remove(Pair.create(projectSystemId, new File(externalProjectPath)));
    if (removed != null) {
      markAsChangedAndScheduleSave();
    }
  }

  @NotNull
  synchronized Collection<ExternalProjectInfo> list(@NotNull final ProjectSystemId projectSystemId) {
    return ContainerUtil.mapNotNull(myExternalRootProjects.values(),
                                    info -> projectSystemId.equals(info.getProjectSystemId()) ? info : null);
  }

  private void mergeLocalSettings() {
    for (ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
      final ProjectSystemId systemId = manager.getSystemId();

      AbstractExternalSystemLocalSettings<?> settings = manager.getLocalSettingsProvider().fun(myProject);
      final Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> availableProjects = settings.getAvailableProjects();

      for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : availableProjects.entrySet()) {
        final ExternalProjectPojo projectPojo = entry.getKey();
        final String externalProjectPath = projectPojo.getPath();
        final Pair<ProjectSystemId, File> key = Pair.create(systemId, new File(externalProjectPath));
        InternalExternalProjectInfo externalProjectInfo = myExternalRootProjects.get(key);
        if (externalProjectInfo == null) {
          final DataNode<ProjectData> dataNode = convert(systemId, projectPojo, entry.getValue());
          externalProjectInfo = new InternalExternalProjectInfo(systemId, externalProjectPath, dataNode);
          myExternalRootProjects.put(key, externalProjectInfo);
          ExternalProjectsManager.getInstance(myProject).getExternalProjectsWatcher().markDirty(externalProjectPath);

          markAsChangedAndScheduleSave();
        }

        // restore linked project sub-modules
        ExternalProjectSettings linkedProjectSettings =
          manager.getSettingsProvider().fun(myProject).getLinkedProjectSettings(externalProjectPath);
        if (linkedProjectSettings != null && ContainerUtil.isEmpty(linkedProjectSettings.getModules())) {

          final Set<String> modulePaths = ContainerUtil.map2Set(
            ExternalSystemApiUtil.findAllRecursively(externalProjectInfo.getExternalProjectStructure(), MODULE),
            node -> node.getData().getLinkedExternalProjectPath());
          linkedProjectSettings.setModules(modulePaths);
        }
      }
    }
  }

  private static DataNode<ProjectData> convert(@NotNull ProjectSystemId systemId,
                                               @NotNull ExternalProjectPojo rootProject,
                                               @NotNull Collection<? extends ExternalProjectPojo> childProjects) {
    ProjectData projectData = new ProjectData(systemId, rootProject.getName(), rootProject.getPath(), rootProject.getPath());
    DataNode<ProjectData> projectDataNode = new DataNode<>(PROJECT, projectData, null);

    for (ExternalProjectPojo childProject : childProjects) {
      String moduleConfigPath = childProject.getPath();
      ModuleData moduleData = new ModuleData(childProject.getName(), systemId,
                                             ModuleTypeId.JAVA_MODULE, childProject.getName(),
                                             moduleConfigPath, moduleConfigPath);
      projectDataNode.createChild(MODULE, moduleData);
    }
    return projectDataNode;
  }

  private static void doSave(@NotNull Project project, @NotNull Collection<InternalExternalProjectInfo> externalProjects) throws IOException {
    for (Iterator<InternalExternalProjectInfo> iterator = externalProjects.iterator(); iterator.hasNext(); ) {
      InternalExternalProjectInfo externalProject = iterator.next();
      if (!validate(externalProject)) {
        iterator.remove();
      }
    }

    getCacheFile(project).writeList(externalProjects, InternalExternalProjectInfo.class, SerializationKt.createCacheWriteConfiguration());
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static DataNode<ExternalConfigPathAware> resolveProjectNode(@NotNull DataNode node) {
    if ((MODULE.equals(node.getKey()) || PROJECT.equals(node.getKey())) && node.getData() instanceof ExternalConfigPathAware) {
      return (DataNode<ExternalConfigPathAware>)node;
    }
    DataNode parent = ExternalSystemApiUtil.findParent(node, MODULE);
    if (parent == null) {
      parent = ExternalSystemApiUtil.findParent(node, PROJECT);
    }
    return parent;
  }

  @Nullable("null indicates that cache was invalid")
  private static List<InternalExternalProjectInfo> load(@NotNull Project project) throws IOException {
    VersionedFile cacheFile = getCacheFile(project);
    BasicFileAttributes fileAttributes = PathKt.basicAttributesIfExists(cacheFile.getFile());
    if (fileAttributes == null || !fileAttributes.isRegularFile()) {
      return Collections.emptyList();
    }

    if (isInvalidated(cacheFile.getFile(), fileAttributes)) {
      LOG.debug("External projects data storage was invalidated");
      return null;
    }
    return cacheFile.readList(InternalExternalProjectInfo.class, SerializationKt.createCacheReadConfiguration(LOG));
  }

  private static boolean isInvalidated(@NotNull Path configurationFile, @NotNull BasicFileAttributes fileAttributes) throws IOException {
    if (!Registry.is("external.system.invalidate.storage", true)) return false;

    long lastModified = fileAttributes.lastModifiedTime().toMillis();
    if (lastModified == 0) {
      return true;
    }

    File brokenMarkerFile = getBrokenMarkerFile();
    if (brokenMarkerFile.exists() && lastModified < brokenMarkerFile.lastModified()) {
      Files.delete(configurationFile);
      return true;
    }
    return false;
  }

  @NotNull
  private static VersionedFile getCacheFile(@NotNull Project project) {
    return new VersionedFile(getProjectConfigurationDir(project).resolve("project.dat"), STORAGE_VERSION);
  }

  @NotNull
  public static Path getProjectConfigurationDir(@NotNull Project project) {
    return ProjectUtil.getExternalConfigurationDir(project);
  }

  @Nullable
  @Override
  public synchronized State getState() {
    return myState;
  }

  @Override
  public synchronized void loadState(@NotNull State state) {
    myState = state;
  }

  synchronized void setIgnored(@NotNull DataNode<?> dataNode, boolean isIgnored) {
    //noinspection unchecked
    final DataNode<ProjectData> projectDataNode =
      PROJECT.equals(dataNode.getKey()) ? (DataNode<ProjectData>)dataNode : ExternalSystemApiUtil.findParent(dataNode, PROJECT);
    if (projectDataNode == null) {
      return;
    }

    dataNode.visit(node -> node.setIgnored(isIgnored));

    saveInclusionSettings(projectDataNode);
  }

  synchronized boolean isIgnored(@NotNull String rootProjectPath, @NotNull String modulePath, @SuppressWarnings("SameParameterValue") @NotNull Key key) {
    final ProjectState projectState = myState.map.get(rootProjectPath);
    if (projectState == null) return false;

    final ModuleState moduleState = projectState.map.get(modulePath);
    return isIgnored(projectState, moduleState, key);
  }

  private static boolean isIgnored(@NotNull ProjectState projectState, @Nullable ModuleState moduleState, @NotNull Key<?> key) {
    return projectState.isInclusion ^ (moduleState != null && moduleState.set.contains(key.getDataType()));
  }

  public static synchronized void invalidateCaches() {
    if (!Registry.is("external.system.invalidate.storage", true)) return;

    File markerFile = getBrokenMarkerFile();
    try {
      FileUtil.writeToFile(markerFile, String.valueOf(System.currentTimeMillis()));
    }
    catch (IOException e) {
      LOG.warn("Cannot update the invalidation marker file", e);
    }
  }

  @NotNull
  private static File getBrokenMarkerFile() {
    return PathManagerEx.getAppSystemDir().resolve("external_build_system").resolve(".broken").toFile();
  }

  static final class State {
    @Property(surroundWithTag = false)
    @MapAnnotation(surroundWithTag = false, surroundValueWithTag = false, surroundKeyWithTag = false, keyAttributeName = "path", entryTagName = "projectState")
    public final Map<String, ProjectState> map = new THashMap<>();
  }

  static class ProjectState {
    @Property(surroundWithTag = false)
    @XMap(keyAttributeName = "path", entryTagName = "dataType")
    public final Map<String, ModuleState> map = new THashMap<>();
    public boolean isInclusion;
  }

  static class ModuleState {
    @Property(surroundWithTag = false)
    @XCollection(elementName = "id")
    public final Set<String> set;

    @SuppressWarnings("unused")
    ModuleState() {
      set = new THashSet<>();
    }

    ModuleState(@NotNull Collection<String> values) {
      set = new THashSet<>(values);
    }
  }
}
