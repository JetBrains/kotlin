// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ContentRootData.SourceRoot;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.vfs.VfsUtilCore.pathToUrl;

/**
 * @author Denis Zhdanov
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public final class ContentRootDataService extends AbstractProjectDataService<ContentRootData, ContentEntry> {
  public static final com.intellij.openapi.util.Key<Boolean> CREATE_EMPTY_DIRECTORIES =
    com.intellij.openapi.util.Key.create("createEmptyDirectories");

  private static final Logger LOG = Logger.getInstance(ContentRootDataService.class);

  @NotNull
  @Override
  public Key<ContentRootData> getTargetDataKey() {
    return ProjectKeys.CONTENT_ROOT;
  }

  @Override
  public void importData(@NotNull Collection<DataNode<ContentRootData>> toImport,
                         @Nullable ProjectData projectData,
                         @NotNull Project project,
                         @NotNull IdeModifiableModelsProvider modelsProvider) {
    logUnitTest("Importing data. Data size is [" + toImport.size() + "]");
    if (toImport.isEmpty()) {
      return;
    }

    final SourceFolderManager sourceFolderManager = SourceFolderManager.getInstance(project);

    boolean isNewlyImportedProject = project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == Boolean.TRUE;
    boolean forceDirectoriesCreation = false;
    DataNode<ProjectData> projectDataNode = ExternalSystemApiUtil.findParent(toImport.iterator().next(), ProjectKeys.PROJECT);
    if (projectDataNode != null) {
      forceDirectoriesCreation = projectDataNode.getUserData(CREATE_EMPTY_DIRECTORIES) == Boolean.TRUE;
    }

    Set<Module> modulesToExpand = new ObjectOpenHashSet<>();
    MultiMap<DataNode<ModuleData>, DataNode<ContentRootData>> byModule = ExternalSystemApiUtil.groupBy(toImport, ModuleData.class);

    filterAndReportDuplicatingContentRoots(byModule, project);

    for (Map.Entry<DataNode<ModuleData>, Collection<DataNode<ContentRootData>>> entry : byModule.entrySet()) {
      Module module = entry.getKey().getUserData(AbstractModuleDataService.MODULE_KEY);
      module = module != null ? module : modelsProvider.findIdeModule(entry.getKey().getData());
      if (module == null) {
        LOG.warn(String.format(
          "Can't import content roots. Reason: target module (%s) is not found at the ide. Content roots: %s",
          entry.getKey(), entry.getValue()
        ));
        continue;
      }
      importData(modelsProvider, sourceFolderManager, entry.getValue(), module, forceDirectoriesCreation);
      if (forceDirectoriesCreation ||
          (isNewlyImportedProject &&
           projectData != null &&
           projectData.getLinkedExternalProjectPath().equals(ExternalSystemApiUtil.getExternalProjectPath(module)))) {
        modulesToExpand.add(module);
      }
    }
    if (!ApplicationManager.getApplication().isHeadlessEnvironment() && !modulesToExpand.isEmpty()) {
      for (Module module : modulesToExpand) {
        String productionModuleName = modelsProvider.getProductionModuleName(module);
        if (productionModuleName == null || !modulesToExpand.contains(modelsProvider.findIdeModule(productionModuleName))) {
          VirtualFile[] roots = modelsProvider.getModifiableRootModel(module).getContentRoots();
          if (roots.length > 0) {
            VirtualFile virtualFile = roots[0];
            StartupManager.getInstance(project).runAfterOpened(() -> {
              ApplicationManager.getApplication().invokeLater(() -> {
                final ProjectView projectView = ProjectView.getInstance(project);
                projectView.changeViewCB(ProjectViewPane.ID, null).doWhenProcessed(() -> projectView.selectCB(null, virtualFile, false));
              }, ModalityState.NON_MODAL, project.getDisposed());
            });
          }
        }
      }
    }
  }

  private static void importData(@NotNull IdeModifiableModelsProvider modelsProvider,
                                 @NotNull SourceFolderManager sourceFolderManager,
                                 @NotNull final Collection<? extends DataNode<ContentRootData>> data,
                                 @NotNull final Module module, boolean forceDirectoriesCreation) {
    logUnitTest("Import data for module [" + module.getName() + "], data size [" + data.size() + "]");
    final ModifiableRootModel modifiableRootModel = modelsProvider.getModifiableRootModel(module);
    final ContentEntry[] contentEntries = modifiableRootModel.getContentEntries();
    final Map<String, ContentEntry> contentEntriesMap = new HashMap<>();
    for (ContentEntry contentEntry : contentEntries) {
      contentEntriesMap.put(contentEntry.getUrl(), contentEntry);
    }

    sourceFolderManager.removeSourceFolders(module);

    final Set<ContentEntry> importedContentEntries = new ReferenceOpenHashSet<>();
    for (final DataNode<ContentRootData> node : data) {
      final ContentRootData contentRoot = node.getData();

      final ContentEntry contentEntry = findOrCreateContentRoot(modifiableRootModel, contentRoot.getRootPath());
      if (!importedContentEntries.contains(contentEntry)) {
        removeSourceFoldersIfAbsent(contentEntry, contentRoot);
        importedContentEntries.add(contentEntry);
      }
      logDebug("Importing content root '%s' for module '%s' forceDirectoriesCreation=[%b]",
               contentRoot.getRootPath(), module.getName(), forceDirectoriesCreation);

      Set<String> updatedSourceRoots = new HashSet<>();
      for (ExternalSystemSourceType externalSrcType : ExternalSystemSourceType.values()) {
        final JpsModuleSourceRootType<?> type = getJavaSourceRootType(externalSrcType);
        if (type != null) {
          for (SourceRoot sourceRoot : contentRoot.getPaths(externalSrcType)) {
            String sourceRootPath = sourceRoot.getPath();
            boolean createSourceFolder = !updatedSourceRoots.contains(sourceRootPath);
            if (createSourceFolder) {
              createOrReplaceSourceFolder(sourceFolderManager, contentEntry, sourceRoot, module, type, forceDirectoriesCreation);
              if (externalSrcType == ExternalSystemSourceType.SOURCE || externalSrcType == ExternalSystemSourceType.TEST) {
                updatedSourceRoots.add(sourceRootPath);
              }
            }
            configureSourceFolder(sourceFolderManager, contentEntry, sourceRoot, createSourceFolder, externalSrcType.isGenerated());
          }
        }
      }

      for (SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.EXCLUDED)) {
        createExcludedRootIfAbsent(contentEntry, path, module.getName(), module.getProject());
      }
      contentEntriesMap.remove(contentEntry.getUrl());
    }
    for (ContentEntry contentEntry : contentEntriesMap.values()) {
      modifiableRootModel.removeContentEntry(contentEntry);
    }
  }

  @Nullable
  private static JpsModuleSourceRootType<?> getJavaSourceRootType(ExternalSystemSourceType type) {
    switch (type) {
      case SOURCE:
      case SOURCE_GENERATED:
        return JavaSourceRootType.SOURCE;
      case TEST:
      case TEST_GENERATED:
        return JavaSourceRootType.TEST_SOURCE;
      case EXCLUDED:
        return null;
      case RESOURCE:
      case RESOURCE_GENERATED:
        return JavaResourceRootType.RESOURCE;
      case TEST_RESOURCE:
      case TEST_RESOURCE_GENERATED:
        return JavaResourceRootType.TEST_RESOURCE;
    }
    return null;
  }

  @NotNull
  private static ContentEntry findOrCreateContentRoot(@NotNull ModifiableRootModel model, @NotNull String path) {
    ContentEntry[] entries = model.getContentEntries();

    for (ContentEntry entry : entries) {
      VirtualFile file = entry.getFile();
      if (file == null) {
        continue;
      }
      if (ExternalSystemApiUtil.getLocalFileSystemPath(file).equals(path)) {
        return entry;
      }
    }
    return model.addContentEntry(pathToUrl(path));
  }

  private static Set<String> getSourceRoots(@NotNull ContentRootData contentRoot) {
    Set<String> sourceRoots = CollectionFactory.createFilePathSet();
    for (ExternalSystemSourceType externalSrcType : ExternalSystemSourceType.values()) {
      final JpsModuleSourceRootType<?> type = getJavaSourceRootType(externalSrcType);
      if (type == null) continue;
      for (SourceRoot path : contentRoot.getPaths(externalSrcType)) {
        if (path == null) continue;
        sourceRoots.add(path.getPath());
      }
    }
    return sourceRoots;
  }

  private static void removeSourceFoldersIfAbsent(@NotNull ContentEntry contentEntry, @NotNull ContentRootData contentRoot) {
    SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
    if (sourceFolders.length == 0) return;
    Set<String> sourceRoots = getSourceRoots(contentRoot);
    for (SourceFolder sourceFolder : sourceFolders) {
      String url = sourceFolder.getUrl();
      String path = VfsUtilCore.urlToPath(url);
      if (!sourceRoots.contains(path)) {
        contentEntry.removeSourceFolder(sourceFolder);
      }
    }
  }

  private static void createOrReplaceSourceFolder(@NotNull SourceFolderManager sourceFolderManager,
                                                  @NotNull ContentEntry contentEntry,
                                                  @NotNull final SourceRoot sourceRoot,
                                                  @NotNull Module module,
                                                  @NotNull JpsModuleSourceRootType<?> sourceRootType,
                                                  boolean createEmptyContentRootDirectories) {

    String path = sourceRoot.getPath();
    if (createEmptyContentRootDirectories) {
      createEmptyDirectory(path);
    }

    SourceFolder folder = findSourceFolder(contentEntry, sourceRoot);
    if (folder != null) {
      final JpsModuleSourceRootType<?> folderRootType = folder.getRootType();
      if (sourceRootType.equals(folderRootType)) {
        return;
      }
      contentEntry.removeSourceFolder(folder);
    }

    String url = pathToUrl(path);

    if (!FileUtil.exists(path)) {
      logDebug("Source folder [%s] does not exist and will not be created, will add when dir is created", url);
      logUnitTest("Adding source folder listener to watch [%s] for creation in project [hashCode=%d]", url, module.getProject().hashCode());
      sourceFolderManager.addSourceFolder(module, url, sourceRootType);
    }
    else {
      contentEntry.addSourceFolder(url, sourceRootType);
    }
  }

  private static void configureSourceFolder(@NotNull SourceFolderManager sourceFolderManager,
                                            @NotNull ContentEntry contentEntry,
                                            @NotNull SourceRoot sourceRoot,
                                            boolean updatePackagePrefix,
                                            boolean generated) {
    String packagePrefix = sourceRoot.getPackagePrefix();
    String url = pathToUrl(sourceRoot.getPath());

    logDebug("Importing root '%s' with packagePrefix=[%s] generated=[%b]", sourceRoot, packagePrefix, generated);

    SourceFolder folder = findSourceFolder(contentEntry, sourceRoot);
    if (folder == null) {
      if (updatePackagePrefix) {
        sourceFolderManager.setSourceFolderPackagePrefix(url, packagePrefix);
      }
      if (generated) {
        sourceFolderManager.setSourceFolderGenerated(url, true);
      }
    }
    else {
      if (updatePackagePrefix && StringUtil.isNotEmpty(packagePrefix)) {
        folder.setPackagePrefix(packagePrefix);
      }
      if (generated) {
        setForGeneratedSources(folder, true);
      }
    }
  }

  private static void createEmptyDirectory(@NotNull String path) {
    if (FileUtil.exists(path)) return;
    ExternalSystemApiUtil.doWriteAction(() -> {
      try {
        VfsUtil.createDirectoryIfMissing(path);
      }
      catch (IOException e) {
        LOG.warn(String.format("Unable to create directory for the path: %s", path), e);
      }
    });
  }

  @Nullable
  private static SourceFolder findSourceFolder(@NotNull ContentEntry contentEntry, @NotNull SourceRoot sourceRoot) {
    for (SourceFolder folder : contentEntry.getSourceFolders()) {
      VirtualFile file = folder.getFile();
      if (file == null) continue;
      String folderPath = ExternalSystemApiUtil.getLocalFileSystemPath(file);
      String rootPath = sourceRoot.getPath();
      if (folderPath.equals(rootPath)) return folder;
    }
    return null;
  }

  private static void setForGeneratedSources(@NotNull SourceFolder folder, boolean generated) {
    JpsModuleSourceRoot jpsElement = folder.getJpsElement();
    JavaSourceRootProperties properties = jpsElement.getProperties(JavaModuleSourceRootTypes.SOURCES);
    if (properties != null) properties.setForGeneratedSources(generated);
  }

  private static void logUnitTest(@NotNull String format, Object... args) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.info(String.format(format, args));
    }
  }

  private static void logDebug(@NotNull String format, Object... args) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format(format, args));
    }
  }

  private static void createExcludedRootIfAbsent(@NotNull ContentEntry entry, @NotNull SourceRoot root, @NotNull String moduleName, @NotNull Project project) {
    String rootPath = root.getPath();
    for (VirtualFile file : entry.getExcludeFolderFiles()) {
      if (ExternalSystemApiUtil.getLocalFileSystemPath(file).equals(rootPath)) {
        return;
      }
    }
    logDebug("Importing excluded root '%s' for content root '%s' of module '%s'", root, entry.getUrl(), moduleName);
    entry.addExcludeFolder(pathToUrl(rootPath));
  }


  private static void filterAndReportDuplicatingContentRoots(@NotNull MultiMap<DataNode<ModuleData>, DataNode<ContentRootData>> moduleNodeToRootNodes,
                                                             @NotNull Project project) {
    Map<String, DuplicateModuleReport> filter = new LinkedHashMap<>();

    for (Map.Entry<DataNode<ModuleData>, Collection<DataNode<ContentRootData>>> entry : moduleNodeToRootNodes.entrySet()) {
      ModuleData moduleData = entry.getKey().getData();
      Collection<DataNode<ContentRootData>> crDataNodes = entry.getValue();
      for (Iterator<DataNode<ContentRootData>> iterator = crDataNodes.iterator(); iterator.hasNext(); ) {
        DataNode<ContentRootData> crDataNode = iterator.next();
        String rootPath = crDataNode.getData().getRootPath();
        DuplicateModuleReport report = filter.putIfAbsent(rootPath, new DuplicateModuleReport(moduleData));
        if (report != null) {
          report.addDuplicate(moduleData);
          iterator.remove();
          crDataNode.clear(true);
        }
      }
    }

    Map<String, DuplicateModuleReport> toReport = filter.entrySet().stream()
      .filter(e -> e.getValue().hasDuplicates())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (r1, r2) -> {
        LOG.warn("Unexpected duplicates in keys while collecting filtered reports");
        return r2;
      }, LinkedHashMap::new));

    if (!toReport.isEmpty()) {
      String notificationMessage = prepareMessageAndLogWarnings(toReport);
      if (notificationMessage != null) {
        showNotificationsPopup(project, toReport.size(), notificationMessage);
      }
    }
  }

  @Nullable
  private static String prepareMessageAndLogWarnings(@NotNull Map<String, DuplicateModuleReport> toReport) {
    String firstMessage = null;
    LOG.warn("Duplicating content roots detected.");
    for (Map.Entry<String, DuplicateModuleReport> entry : toReport.entrySet()) {
      String path = entry.getKey();
      DuplicateModuleReport report = entry.getValue();
      String message = String.format("Path [%s] of module [%s] was removed from modules [%s]", path, report.getOriginalName(),
                                     StringUtil.join(report.getDuplicatesNames(), ", "));
      if (firstMessage == null) {
        firstMessage = message;
      }
      LOG.warn(message);
    }
    return firstMessage;
  }

  private static void showNotificationsPopup(@NotNull Project project,
                                             int reportsCount,
                                             @NotNull String notificationMessage) {
    int extraReportsCount = reportsCount - 1;
    if (extraReportsCount > 0) {
      notificationMessage += "<br>Also " + extraReportsCount + " more "
                             + StringUtil.pluralize("path", extraReportsCount)
                             + " " + (extraReportsCount == 1 ? "was" : "were") +
                             " deduplicated. See idea log for details";
    }

    Notification notification = new Notification("Content root duplicates",
                                                 ExternalSystemBundle.message("duplicate.content.roots.detected"),
                                                 notificationMessage,
                                                 NotificationType.WARNING);
    Notifications.Bus.notify(notification, project);
  }


  private static final class DuplicateModuleReport {
    private final ModuleData myOriginal;
    private final List<ModuleData> myDuplicates = new ArrayList<>();

    private DuplicateModuleReport(@NotNull ModuleData original) {
      myOriginal = original;
    }

    public void addDuplicate(@NotNull ModuleData duplicate) {
      myDuplicates.add(duplicate);
    }

    public boolean hasDuplicates() {
      return !myDuplicates.isEmpty();
    }

    public String getOriginalName() {
      return myOriginal.getInternalName();
    }

    public Collection<String> getDuplicatesNames() {
      return ContainerUtil.map(myDuplicates, ModuleData::getInternalName);
    }
  }
}
