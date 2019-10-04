// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.test;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManagerImpl;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.BooleanFunction;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.intellij.testFramework.EdtTestUtil.runInEdtAndGet;

/**
 * @author Vladislav.Soroka
 */
public abstract class ExternalSystemImportingTestCase extends ExternalSystemTestCase {
  protected void assertModulesContains(@NotNull Project project, String... expectedNames) {
    Module[] actual = ModuleManager.getInstance(project).getModules();
    List<String> actualNames = new ArrayList<>();

    for (Module m : actual) {
      actualNames.add(m.getName());
    }

    assertContain(actualNames, expectedNames);
  }

  protected void assertModulesContains(String... expectedNames) {
    assertModulesContains(myProject, expectedNames);
  }

  protected void assertModules(@NotNull Project project, String... expectedNames) {
    Module[] actual = ModuleManager.getInstance(project).getModules();
    List<String> actualNames = new ArrayList<>();

    for (Module m : actual) {
      actualNames.add(m.getName());
    }

    assertUnorderedElementsAreEqual(actualNames, expectedNames);
  }

  protected void assertModules(String... expectedNames) {
    assertModules(myProject, expectedNames);
  }

  protected void assertContentRoots(String moduleName, String... expectedRoots) {
    List<String> actual = new ArrayList<>();
    for (ContentEntry e : getContentRoots(moduleName)) {
      actual.add(e.getUrl());
    }

    for (int i = 0; i < expectedRoots.length; i++) {
      expectedRoots[i] = VfsUtilCore.pathToUrl(expectedRoots[i]);
    }

    assertUnorderedPathsAreEqual(actual, Arrays.asList(expectedRoots));
  }

  protected void assertSources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaSourceRootType.SOURCE, expectedSources);
  }

  protected void assertGeneratedSources(String moduleName, String... expectedSources) {
    assertGeneratedSources(moduleName, JavaSourceRootType.SOURCE, expectedSources);
  }

  protected void assertGeneratedTestSources(String moduleName, String... expectedSources) {
    assertGeneratedSources(moduleName, JavaSourceRootType.TEST_SOURCE, expectedSources);
  }

  private void assertGeneratedSources(String moduleName, JavaSourceRootType type, String... expectedSources) {
    final ContentEntry[] contentRoots = getContentRoots(moduleName);
    String rootUrl = contentRoots.length > 1 ? ExternalSystemApiUtil.getExternalProjectPath(getModule(moduleName)) : null;
    List<String> actual = new ArrayList<>();

    for (ContentEntry contentRoot : contentRoots) {
      rootUrl = VirtualFileManager.extractPath(rootUrl == null ? contentRoot.getUrl() : rootUrl);
      for (SourceFolder f : contentRoot.getSourceFolders(type)) {
        String folderUrl = VirtualFileManager.extractPath(f.getUrl());

        if (folderUrl.startsWith(rootUrl)) {
          int length = rootUrl.length() + 1;
          folderUrl = folderUrl.substring(Math.min(length, folderUrl.length()));
        }

        JavaSourceRootProperties properties = f.getJpsElement().getProperties(type);
        if (properties != null && properties.isForGeneratedSources()) {
          actual.add(folderUrl);
        }
      }
    }

    assertOrderedElementsAreEqual(actual, Arrays.asList(expectedSources));
  }

  protected void assertResources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaResourceRootType.RESOURCE, expectedSources);
  }

  protected void assertTestSources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaSourceRootType.TEST_SOURCE, expectedSources);
  }

  protected void assertTestResources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaResourceRootType.TEST_RESOURCE, expectedSources);
  }

  protected void assertExcludes(String moduleName, String... expectedExcludes) {
    ContentEntry contentRoot = getContentRoot(moduleName);
    doAssertContentFolders(contentRoot, Arrays.asList(contentRoot.getExcludeFolders()), expectedExcludes);
  }

  protected void assertContentRootExcludes(String moduleName, String contentRoot, String... expectedExcudes) {
    ContentEntry root = getContentRoot(moduleName, contentRoot);
    doAssertContentFolders(root, Arrays.asList(root.getExcludeFolders()), expectedExcudes);
  }

  private void doAssertContentFolders(String moduleName, @NotNull JpsModuleSourceRootType<?> rootType, String... expected) {
    final ContentEntry[] contentRoots = getContentRoots(moduleName);
    final String rootUrl = contentRoots.length > 1 ? ExternalSystemApiUtil.getExternalProjectPath(getModule(moduleName)) : null;
    doAssertContentFolders(rootUrl, contentRoots, rootType, expected);
  }

  protected static List<SourceFolder> doAssertContentFolders(@Nullable String rootUrl,
                                                             ContentEntry[] contentRoots,
                                                             @NotNull JpsModuleSourceRootType<?> rootType,
                                                             String... expected) {
    List<SourceFolder> result = new ArrayList<>();
    List<String> actual = new ArrayList<>();
    for (ContentEntry contentRoot : contentRoots) {
      for (SourceFolder f : contentRoot.getSourceFolders(rootType)) {
        rootUrl = VirtualFileManager.extractPath(rootUrl == null ? contentRoot.getUrl() : rootUrl);
        String folderUrl = VirtualFileManager.extractPath(f.getUrl());
        if (folderUrl.startsWith(rootUrl)) {
          int length = rootUrl.length() + 1;
          folderUrl = folderUrl.substring(Math.min(length, folderUrl.length()));
        }

        actual.add(folderUrl);
        result.add(f);
      }
    }

    assertOrderedElementsAreEqual(actual, Arrays.asList(expected));
    return result;
  }

  private static void doAssertContentFolders(ContentEntry e, final List<? extends ContentFolder> folders, String... expected) {
    List<String> actual = new ArrayList<>();
    for (ContentFolder f : folders) {
      String rootUrl = e.getUrl();
      String folderUrl = f.getUrl();

      if (folderUrl.startsWith(rootUrl)) {
        int length = rootUrl.length() + 1;
        folderUrl = folderUrl.substring(Math.min(length, folderUrl.length()));
      }

      actual.add(folderUrl);
    }

    assertOrderedElementsAreEqual(actual, Arrays.asList(expected));
  }

  protected void assertModuleOutputs(String moduleName, String... outputs) {
    String[] outputPaths = ContainerUtil.map2Array(CompilerPaths.getOutputPaths(new Module[]{getModule(moduleName)}), String.class,
                                                   s -> getAbsolutePath(s));
    assertUnorderedElementsAreEqual(outputPaths, outputs);
  }

  protected void assertModuleOutput(String moduleName, String output, String testOutput) {
    CompilerModuleExtension e = getCompilerExtension(moduleName);

    assertFalse(e.isCompilerOutputPathInherited());
    assertEquals(output, getAbsolutePath(e.getCompilerOutputUrl()));
    assertEquals(testOutput, getAbsolutePath(e.getCompilerOutputUrlForTests()));
  }

  protected void assertModuleInheritedOutput(String moduleName) {
    CompilerModuleExtension e = getCompilerExtension(moduleName);
    assertTrue(e.isCompilerOutputPathInherited());
  }

  protected static String getAbsolutePath(String path) {
    path = VfsUtilCore.urlToPath(path);
    path = PathUtil.getCanonicalPath(path);
    return FileUtil.toSystemIndependentName(path);
  }

  protected void assertProjectOutput(String module) {
    assertTrue(getCompilerExtension(module).isCompilerOutputPathInherited());
  }

  protected CompilerModuleExtension getCompilerExtension(String module) {
    return CompilerModuleExtension.getInstance(getModule(module));
  }

  protected void assertModuleLibDep(String moduleName, String depName) {
    assertModuleLibDep(moduleName, depName, null);
  }

  protected void assertModuleLibDep(String moduleName, String depName, String classesPath) {
    assertModuleLibDep(moduleName, depName, classesPath, null, null);
  }

  protected void assertModuleLibDep(String moduleName, String depName, String classesPath, String sourcePath, String javadocPath) {
    LibraryOrderEntry lib = ContainerUtil.getFirstItem(getModuleLibDeps(moduleName, depName));
    final String errorMessage = "Failed to find dependency with name [" + depName + "] in module [" + moduleName + "]\n" +
                                "Available dependencies: " + collectModuleDepsNames(moduleName, LibraryOrderEntry.class);
    assertNotNull(errorMessage, lib);
    assertModuleLibDepPath(lib, OrderRootType.CLASSES, classesPath == null ? null : Collections.singletonList(classesPath));
    assertModuleLibDepPath(lib, OrderRootType.SOURCES, sourcePath == null ? null : Collections.singletonList(sourcePath));
    assertModuleLibDepPath(lib, JavadocOrderRootType.getInstance(), javadocPath == null ? null : Collections.singletonList(javadocPath));
  }

  protected void assertModuleLibDep(String moduleName,
                                    String depName,
                                    List<String> classesPaths,
                                    List<String> sourcePaths,
                                    List<String> javadocPaths) {
    LibraryOrderEntry lib = ContainerUtil.getFirstItem(getModuleLibDeps(moduleName, depName));

    assertModuleLibDepPath(lib, OrderRootType.CLASSES, classesPaths);
    assertModuleLibDepPath(lib, OrderRootType.SOURCES, sourcePaths);
    assertModuleLibDepPath(lib, JavadocOrderRootType.getInstance(), javadocPaths);
  }

  private static void assertModuleLibDepPath(LibraryOrderEntry lib, OrderRootType type, List<String> paths) {
    assertNotNull(lib);
    if (paths == null) return;
    assertUnorderedPathsAreEqual(Arrays.asList(lib.getRootUrls(type)), paths);
    // also check the library because it may contain slight different set of urls (e.g. with duplicates)
    final Library library = lib.getLibrary();
    assertNotNull(library);
    assertUnorderedPathsAreEqual(Arrays.asList(library.getUrls(type)), paths);
  }

  protected void assertModuleLibDepScope(String moduleName, String depName, DependencyScope... scopes) {
    List<LibraryOrderEntry> deps = getModuleLibDeps(moduleName, depName);
    assertUnorderedElementsAreEqual(ContainerUtil.map2Array(deps, entry -> entry.getScope()), scopes);
  }

  protected List<LibraryOrderEntry> getModuleLibDeps(String moduleName, String depName) {
    return getModuleDep(moduleName, depName, LibraryOrderEntry.class);
  }

  protected void assertModuleLibDeps(String moduleName, String... expectedDeps) {
    assertModuleDeps(moduleName, LibraryOrderEntry.class, expectedDeps);
  }

  protected void assertExportedDeps(String moduleName, String... expectedDeps) {
    final List<String> actual = new ArrayList<>();

    getRootManager(moduleName).orderEntries().withoutSdk().withoutModuleSourceEntries().exportedOnly().process(new RootPolicy<Object>() {
      @Override
      public Object visitModuleOrderEntry(@NotNull ModuleOrderEntry e, Object value) {
        actual.add(e.getModuleName());
        return null;
      }

      @Override
      public Object visitLibraryOrderEntry(@NotNull LibraryOrderEntry e, Object value) {
        actual.add(e.getLibraryName());
        return null;
      }
    }, null);

    assertOrderedElementsAreEqual(actual, expectedDeps);
  }

  protected void assertModuleModuleDeps(String moduleName, String... expectedDeps) {
    assertModuleDeps(moduleName, ModuleOrderEntry.class, expectedDeps);
  }

  private void assertModuleDeps(String moduleName, Class clazz, String... expectedDeps) {
    assertOrderedElementsAreEqual(collectModuleDepsNames(moduleName, clazz), expectedDeps);
  }

  protected void assertProductionOnTestDependencies(String moduleName, String... expectedDeps) {
    assertOrderedElementsAreEqual(collectModuleDepsNames(
      moduleName, entry -> entry instanceof ModuleOrderEntry && ((ModuleOrderEntry)entry).isProductionOnTestDependency()
    ), expectedDeps);
  }

  protected void assertModuleModuleDepScope(String moduleName, String depName, DependencyScope... scopes) {
    List<ModuleOrderEntry> deps = getModuleModuleDeps(moduleName, depName);
    assertUnorderedElementsAreEqual(ContainerUtil.map2Array(deps, entry -> entry.getScope()), scopes);
  }

  @NotNull
  private List<ModuleOrderEntry> getModuleModuleDeps(@NotNull String moduleName, @NotNull String depName) {
    return getModuleDep(moduleName, depName, ModuleOrderEntry.class);
  }

  private List<String> collectModuleDepsNames(String moduleName, Predicate<OrderEntry> predicate) {
    List<String> actual = new ArrayList<>();

    for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
      if (predicate.test(e)) {
        actual.add(e.getPresentableName());
      }
    }
    return actual;
  }

  private List<String> collectModuleDepsNames(String moduleName, Class clazz) {
    return collectModuleDepsNames(moduleName, entry -> clazz.isInstance(entry));
  }

  @NotNull
  private <T> List<T> getModuleDep(@NotNull String moduleName, @NotNull String depName, @NotNull Class<T> clazz) {
    List<T> deps = new ArrayList<>();

    for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
      if (clazz.isInstance(e) && e.getPresentableName().equals(depName)) {
        deps.add((T)e);
      }
    }
    assertNotNull("Dependency not found: " + depName + "\namong: " + collectModuleDepsNames(moduleName, clazz), deps);
    return deps;
  }

  public void assertProjectLibraries(String... expectedNames) {
    List<String> actualNames = new ArrayList<>();
    for (Library each : LibraryTablesRegistrar.getInstance().getLibraryTable(myProject).getLibraries()) {
      String name = each.getName();
      actualNames.add(name == null ? "<unnamed>" : name);
    }
    assertUnorderedElementsAreEqual(actualNames, expectedNames);
  }

  protected void assertModuleGroupPath(String moduleName, String... expected) {
    String[] path = ModuleManager.getInstance(myProject).getModuleGroupPath(getModule(moduleName));

    if (expected.length == 0) {
      assertNull(path);
    }
    else {
      assertNotNull(path);
      assertOrderedElementsAreEqual(Arrays.asList(path), expected);
    }
  }

  protected void assertArtifacts(String... expectedNames) {
    final List<String> actualNames = ContainerUtil.map(
      ArtifactManager.getInstance(myProject).getAllArtifactsIncludingInvalid(),
      (Function<Artifact, String>)artifact -> artifact.getName());

    assertUnorderedElementsAreEqual(actualNames, expectedNames);
  }

  private ContentEntry getContentRoot(String moduleName) {
    ContentEntry[] ee = getContentRoots(moduleName);
    List<String> roots = new ArrayList<>();
    for (ContentEntry e : ee) {
      roots.add(e.getUrl());
    }

    String message = "Several content roots found: [" + StringUtil.join(roots, ", ") + "]";
    assertEquals(message, 1, ee.length);

    return ee[0];
  }

  private ContentEntry getContentRoot(String moduleName, String path) {
    for (ContentEntry e : getContentRoots(moduleName)) {
      if (e.getUrl().equals(VfsUtilCore.pathToUrl(path))) return e;
    }
    throw new AssertionError("content root not found");
  }

  public ContentEntry[] getContentRoots(String moduleName) {
    return getRootManager(moduleName).getContentEntries();
  }

  protected ModuleRootManager getRootManager(String module) {
    return ModuleRootManager.getInstance(getModule(module));
  }

  protected void ignoreData(BooleanFunction<DataNode<?>> booleanFunction, final boolean ignored) {
    final ExternalProjectInfo externalProjectInfo = ProjectDataManagerImpl.getInstance().getExternalProjectData(
      myProject, getExternalSystemId(), getCurrentExternalProjectSettings().getExternalProjectPath());
    assertNotNull(externalProjectInfo);

    final DataNode<ProjectData> projectDataNode = externalProjectInfo.getExternalProjectStructure();
    assertNotNull(projectDataNode);

    final Collection<DataNode<?>> nodes = ExternalSystemApiUtil.findAllRecursively(projectDataNode, booleanFunction);
    for (DataNode<?> node : nodes) {
      node.visit(dataNode -> dataNode.setIgnored(ignored));
    }
    ServiceManager.getService(ProjectDataManager.class).importData(projectDataNode, myProject, true);
  }

  protected void importProject(@NonNls String config) throws IOException {
    createProjectConfig(config);
    importProject();
  }

  protected void importProject() {
    doImportProject();
  }

  private void doImportProject() {
    AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(myProject, getExternalSystemId());
    final ExternalProjectSettings projectSettings = getCurrentExternalProjectSettings();
    projectSettings.setExternalProjectPath(getProjectPath());
    //noinspection unchecked
    Set<ExternalProjectSettings> projects = new HashSet<>(systemSettings.getLinkedProjectsSettings());
    projects.remove(projectSettings);
    projects.add(projectSettings);
    //noinspection unchecked
    systemSettings.setLinkedProjectsSettings(projects);

    final Ref<Couple<String>> error = Ref.create();
    ImportSpec importSpec = createImportSpec();
    ExternalProjectRefreshCallback callback = importSpec.getCallback();
    if (callback == null || callback instanceof ImportSpecBuilder.DefaultProjectRefreshCallback) {
      importSpec = new ImportSpecBuilder(importSpec).callback(new ExternalProjectRefreshCallback() {
        @Override
        public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
          if (externalProject == null) {
            System.err.println("Got null External project after import");
            return;
          }
          ServiceManager.getService(ProjectDataManager.class).importData(externalProject, myProject, true);
          System.out.println("External project was successfully imported");
        }

        @Override
        public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
          error.set(Couple.of(errorMessage, errorDetails));
        }
      }).build();
    }

    ExternalSystemProgressNotificationManager notificationManager =
      ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    ExternalSystemTaskNotificationListenerAdapter listener = new ExternalSystemTaskNotificationListenerAdapter() {
      @Override
      public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
        if (StringUtil.isEmptyOrSpaces(text)) return;
        (stdOut ? System.out : System.err).print(text);
      }
    };
    notificationManager.addNotificationListener(listener);
    try {
      ExternalSystemUtil.refreshProjects(importSpec);
    }
    finally {
      notificationManager.removeNotificationListener(listener);
    }

    if (!error.isNull()) {
      handleImportFailure(error.get().first, error.get().second);
    }
  }

  protected void handleImportFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
    String failureMsg = "Import failed: " + errorMessage;
    if (StringUtil.isNotEmpty(errorDetails)) {
      failureMsg += "\nError details: \n" + errorDetails;
    }
    fail(failureMsg);
  }

  protected ImportSpec createImportSpec() {
    ImportSpecBuilder importSpecBuilder = new ImportSpecBuilder(myProject, getExternalSystemId())
      .use(ProgressExecutionMode.MODAL_SYNC)
      .forceWhenUptodate();
    return importSpecBuilder.build();
  }

  protected abstract ExternalProjectSettings getCurrentExternalProjectSettings();

  protected abstract ProjectSystemId getExternalSystemId();

  protected void setupJdkForModules(String... moduleNames) {
    for (String each : moduleNames) {
      setupJdkForModule(each);
    }
  }

  @Override
  protected Sdk setupJdkForModule(final String moduleName) {
    final Sdk sdk = true ? JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk() : createJdk("Java 1.5");
    ModuleRootModificationUtil.setModuleSdk(getModule(moduleName), sdk);
    return sdk;
  }

  protected static Sdk createJdk(String versionName) {
    return IdeaTestUtil.getMockJdk17(versionName);
  }

  protected static AtomicInteger configConfirmationForYesAnswer() {
    final AtomicInteger counter = new AtomicInteger();
    Messages.setTestDialog(new TestDialog() {
      @Override
      public int show(@NotNull String message) {
        counter.set(counter.get() + 1);
        return 0;
      }
    });
    return counter;
  }

  protected static AtomicInteger configConfirmationForNoAnswer() {
    final AtomicInteger counter = new AtomicInteger();
    Messages.setTestDialog(new TestDialog() {
      @Override
      public int show(@NotNull String message) {
        counter.set(counter.get() + 1);
        return 1;
      }
    });
    return counter;
  }

  protected static Collection<UsageInfo> findUsages(@NotNull PsiElement element) throws Exception {
    return ProgressManager.getInstance().run(new Task.WithResult<Collection<UsageInfo>, Exception>(null, "", false) {
      @Override
      protected Collection<UsageInfo> compute(@NotNull ProgressIndicator indicator) {
        return runInEdtAndGet(() -> {
          FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(element.getProject())).getFindUsagesManager();
          FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, false);
          assertNotNull(handler);
          final FindUsagesOptions options = handler.getFindUsagesOptions();
          final CommonProcessors.CollectProcessor<UsageInfo> processor = new CommonProcessors.CollectProcessor<>();
          for (PsiElement element : handler.getPrimaryElements()) {
            handler.processElementUsages(element, processor, options);
          }
          for (PsiElement element : handler.getSecondaryElements()) {
            handler.processElementUsages(element, processor, options);
          }
          return processor.getResults();
        });
      }
    });
  }

  @Nullable
  protected SourceFolder findSource(@NotNull String moduleName, @NotNull String sourcePath) {
    return findSource(getRootManager(moduleName), sourcePath);
  }

  @Nullable
  protected SourceFolder findSource(@NotNull ModuleRootModel moduleRootManager, @NotNull String sourcePath) {
    ContentEntry[] contentRoots = moduleRootManager.getContentEntries();
    Module module = moduleRootManager.getModule();
    String rootUrl = getAbsolutePath(ExternalSystemApiUtil.getExternalProjectPath(module));
    for (ContentEntry contentRoot : contentRoots) {
      for (SourceFolder f : contentRoot.getSourceFolders()) {
        String folderPath = getAbsolutePath(f.getUrl());
        String rootPath = getAbsolutePath(rootUrl + "/" + sourcePath);
        if (folderPath.equals(rootPath)) return f;
      }
    }
    return null;
  }

  //protected void assertProblems(String... expectedProblems) {
  //  final List<String> actualProblems = new ArrayList<String>();
  //  UIUtil.invokeAndWaitIfNeeded(new Runnable() {
  //    @Override
  //    public void run() {
  //      final NewErrorTreeViewPanel messagesView = ExternalSystemNotificationManager.getInstance(myProject)
  //        .prepareMessagesView(getExternalSystemId(), NotificationSource.PROJECT_SYNC, false);
  //      final ErrorViewStructure treeStructure = messagesView.getErrorViewStructure();
  //
  //      ErrorTreeElement[] elements = treeStructure.getChildElements(treeStructure.getRootElement());
  //      for (ErrorTreeElement element : elements) {
  //        if (element.getKind() == ErrorTreeElementKind.ERROR ||
  //            element.getKind() == ErrorTreeElementKind.WARNING) {
  //          actualProblems.add(StringUtil.join(element.getText(), "\n"));
  //        }
  //      }
  //    }
  //  });
  //
  //  assertOrderedElementsAreEqual(actualProblems, expectedProblems);
  //}
}
