// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.JavaApplicationRunConfigurationImporter;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager;
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManagerImpl;
import com.intellij.openapi.externalSystem.service.project.settings.FacetConfigurationImporter;
import com.intellij.openapi.externalSystem.service.project.settings.RunConfigurationImporter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.openapi.vfs.encoding.EncodingProjectManagerImpl;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.TestRunner;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Nikita.Skvortsov
 */
public class GradleSettingsImportingTest extends GradleSettingsImportingTestCase {
  @Test
  public void testInspectionSettingsImport() throws Exception {
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    inspections {\n" +
        "      myInspection { enabled = false }\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final InspectionProfileImpl profile = InspectionProfileManager.getInstance(myProject).getCurrentProfile();
    assertEquals("Gradle Imported", profile.getName());
  }

  @Test
  public void testApplicationRunConfigurationSettingsImport() throws Exception {
    TestRunConfigurationImporter testExtension = new TestRunConfigurationImporter("application");
    maskRunImporter(testExtension);

    createSettingsFile("rootProject.name = 'moduleName'");
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    runConfigurations {\n" +
        "       app1(Application) {\n" +
        "           mainClass = 'my.app.Class'\n" +
        "           jvmArgs =   '-Xmx1g'\n" +
        "           moduleName = 'moduleName'\n" +
        "       }\n" +
        "       app2(Application) {\n" +
        "           mainClass = 'my.app.Class2'\n" +
        "           moduleName = 'moduleName'\n" +
        "       }\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final Map<String, Map<String, Object>> configs = testExtension.getConfigs();

    assertContain(new ArrayList<>(configs.keySet()), "app1", "app2");
    Map<String, Object> app1Settings = configs.get("app1");
    Map<String, Object> app2Settings = configs.get("app2");

    assertEquals("my.app.Class", app1Settings.get("mainClass"));
    assertEquals("my.app.Class2", app2Settings.get("mainClass"));
    assertEquals("-Xmx1g", app1Settings.get("jvmArgs"));
    assertNull(app2Settings.get("jvmArgs"));
  }

  @Test
  @Ignore
  public void testGradleRunConfigurationSettingsImport() throws Exception {
    TestRunConfigurationImporter testExtension = new TestRunConfigurationImporter("gradle");
    maskRunImporter(testExtension);

    createSettingsFile("rootProject.name = 'moduleName'");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPluginIfCan("0.5.1")
        .addPostfix(
          "import org.jetbrains.gradle.ext.*",
          "idea.project.settings {",
          "  runConfigurations {",
          "    gr1(Gradle) {",
          "      project = rootProject",
          "      taskNames = [':cleanTest', ':test']",
          "      envs = ['env_key':'env_val']",
          "      jvmArgs = '-DvmKey=vmVal'",
          "      scriptParameters = '-PscriptParam'",
          "    }",
          "  }",
          "}"
        ).generate());


    final Map<String, Map<String, Object>> configs = testExtension.getConfigs();

    assertContain(new ArrayList<>(configs.keySet()), "gr1");
    Map<String, Object> gradleSettings = configs.get("gr1");

    assertEquals(myProjectRoot.getPath(), ((String)gradleSettings.get("projectPath")).replace('\\', '/'));
    assertTrue(((List)gradleSettings.get("taskNames")).contains(":cleanTest"));
    assertEquals("-DvmKey=vmVal", gradleSettings.get("jvmArgs"));
    assertTrue(((Map)gradleSettings.get("envs")).containsKey("env_key"));
  }

  private void maskRunImporter(@NotNull RunConfigurationImporter testExtension) {
    ExtensionTestUtil.maskExtensions(RunConfigurationImporter.EP_NAME, Collections.singletonList(testExtension), getTestRootDisposable());
  }

  @Test
  public void testDefaultRCSettingsImport() throws Exception {
    RunConfigurationImporter appcConfigImporter = new JavaApplicationRunConfigurationImporter();
    maskRunImporter(appcConfigImporter);

    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    runConfigurations {\n" +
        "       defaults(Application) {\n" +
        "           jvmArgs = '-DmyKey=myVal'\n" +
        "       }\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final RunManager runManager = RunManager.getInstance(myProject);
    final RunnerAndConfigurationSettings template = runManager.getConfigurationTemplate(appcConfigImporter.getConfigurationFactory());
    final String parameters = ((ApplicationConfiguration)template.getConfiguration()).getVMParameters();

    assertNotNull(parameters);
    assertTrue(parameters.contains("-DmyKey=myVal"));
  }

  @Test
  public void testDefaultsAreUsedDuringImport() throws Exception {
    RunConfigurationImporter appcConfigImporter = new JavaApplicationRunConfigurationImporter();
    maskRunImporter(appcConfigImporter);

    createSettingsFile("rootProject.name = 'moduleName'");
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    runConfigurations {\n" +
        "       defaults(Application) {\n" +
        "           jvmArgs = '-DmyKey=myVal'\n" +
        "       }\n" +
        "       'My Run'(Application) {\n" +
        "           mainClass = 'my.app.Class'\n" +
        "           moduleName = 'moduleName'\n" +
        "       }\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final RunManager runManager = RunManager.getInstance(myProject);
    final RunnerAndConfigurationSettings template = runManager.getConfigurationTemplate(appcConfigImporter.getConfigurationFactory());
    final String parameters = ((ApplicationConfiguration)template.getConfiguration()).getVMParameters();

    assertNotNull(parameters);
    assertTrue(parameters.contains("-DmyKey=myVal"));

    final ApplicationConfiguration myRun = (ApplicationConfiguration)runManager.findConfigurationByName("My Run").getConfiguration();
    assertNotNull(myRun);
    final String actualParams = myRun.getVMParameters();
    assertNotNull(actualParams);
    assertTrue(actualParams.contains("-DmyKey=myVal"));
    assertEquals("my.app.Class", myRun.getMainClassName());
  }

  @Test
  public void testBeforeRunTaskImport() throws Exception {
    RunConfigurationImporter appcConfigImporter = new JavaApplicationRunConfigurationImporter();
    maskRunImporter(appcConfigImporter);

    createSettingsFile("rootProject.name = 'moduleName'");
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    runConfigurations {\n" +
        "       'My Run'(Application) {\n" +
        "           mainClass = 'my.app.Class'\n" +
        "           moduleName = 'moduleName'\n" +
        "           beforeRun {\n" +
        "               gradle(GradleTask) { task = tasks['projects'] }\n" +
        "           }\n" +
        "       }\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
    final ApplicationConfiguration myRun = (ApplicationConfiguration)runManager.findConfigurationByName("My Run").getConfiguration();
    assertNotNull(myRun);

    final List<BeforeRunTask> tasks = runManager.getBeforeRunTasks(myRun);
    assertSize(2, tasks);
    final BeforeRunTask gradleBeforeRunTask = tasks.get(1);
    assertInstanceOf(gradleBeforeRunTask, ExternalSystemBeforeRunTask.class);
    final ExternalSystemTaskExecutionSettings settings = ((ExternalSystemBeforeRunTask)gradleBeforeRunTask).getTaskExecutionSettings();
    assertContain(settings.getTaskNames(), "projects");
    assertEquals(FileUtil.toSystemIndependentName(getProjectPath()),
                 FileUtil.toSystemIndependentName(settings.getExternalProjectPath()));
  }


  @Test
  public void testFacetSettingsImport() throws Exception {
    TestFacetConfigurationImporter testExtension = new TestFacetConfigurationImporter("spring");
    ExtensionTestUtil
      .maskExtensions(FacetConfigurationImporter.EP_NAME, Collections.<FacetConfigurationImporter>singletonList(testExtension),
                      getTestRootDisposable());
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  module.settings {\n" +
        "    facets {\n" +
        "       spring(SpringFacet) {\n" +
        "         contexts {\n" +
        "            myParent {\n" +
        "              file = 'parent_ctx.xml'\n" +
        "            }\n" +
        "            myChild {\n" +
        "              file = 'child_ctx.xml'\n" +
        "              parent = 'myParent'" +
        "            }\n" +
        "         }\n" +
        "       }\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final Map<String, Map<String, Object>> facetConfigs = testExtension.getConfigs();

    assertContain(new ArrayList<>(facetConfigs.keySet()), "spring");
    List<Map<String, Object>> springCtxConfigs = (List<Map<String, Object>>)facetConfigs.get("spring").get("contexts");

    assertContain(springCtxConfigs.stream().map((Map m) -> m.get("name")).collect(Collectors.toList()), "myParent", "myChild");

    Map<String, Object> parentSettings = springCtxConfigs.stream()
      .filter((Map m) -> m.get("name").equals("myParent"))
      .findFirst()
      .get();
    Map<String, Object> childSettings = springCtxConfigs.stream()
      .filter((Map m) -> m.get("name").equals("myChild"))
      .findFirst()
      .get();

    assertEquals("parent_ctx.xml", parentSettings.get("file"));
    assertEquals("child_ctx.xml", childSettings.get("file"));
    assertEquals("myParent", childSettings.get("parent"));
  }

  @Test
  public void testTaskTriggersImport() throws Exception {
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    taskTriggers {\n" +
        "      beforeSync tasks.getByName('projects'), tasks.getByName('tasks')\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final List<ExternalProjectsManagerImpl.ExternalProjectsStateProvider.TasksActivation> activations =
      ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider().getAllTasksActivation();

    assertSize(1, activations);

    final ExternalProjectsManagerImpl.ExternalProjectsStateProvider.TasksActivation activation = activations.get(0);
    assertEquals(GradleSettings.getInstance(myProject).getLinkedProjectsSettings().iterator().next().getExternalProjectPath(),
                 activation.projectPath);
    final List<String> beforeSyncTasks = activation.state.getTasks(ExternalSystemTaskActivator.Phase.BEFORE_SYNC);

    if (extPluginVersionIsAtLeast("0.5")) {
      assertContain(beforeSyncTasks, "projects", "tasks");
    }
    else {
      assertContain(beforeSyncTasks, ":projects", ":tasks");
    }
  }

  @Test
  public void testImportEncodingSettings() throws IOException {
    {
      importProject(
        new GradleBuildScriptBuilderEx()
          .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
          .addImport("org.jetbrains.gradle.ext.EncodingConfiguration.BomPolicy")
          .addPostfix("idea {")
          .addPostfix("  project {")
          .addPostfix("    settings {")
          .addPostfix("      encodings {")
          .addPostfix("        encoding = 'IBM-Thai'")
          .addPostfix("        bomPolicy = BomPolicy.WITH_NO_BOM")
          .addPostfix("        properties {")
          .addPostfix("          encoding = 'GB2312'")
          .addPostfix("          transparentNativeToAsciiConversion = true")
          .addPostfix("        }")
          .addPostfix("      }")
          .addPostfix("    }")
          .addPostfix("  }")
          .addPostfix("}")
          .generate());
      EncodingProjectManagerImpl encodingManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
      assertEquals("IBM-Thai", encodingManager.getDefaultCharset().name());
      assertEquals("GB2312", encodingManager.getDefaultCharsetForPropertiesFiles(null).name());
      assertTrue(encodingManager.isNative2AsciiForPropertiesFiles());
      assertFalse(encodingManager.shouldAddBOMForNewUtf8File());
    }
    {
      importProject(
        new GradleBuildScriptBuilderEx()
          .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
          .addImport("org.jetbrains.gradle.ext.EncodingConfiguration.BomPolicy")
          .addPostfix("idea {")
          .addPostfix("  project {")
          .addPostfix("    settings {")
          .addPostfix("      encodings {")
          .addPostfix("        encoding = 'UTF-8'")
          .addPostfix("        bomPolicy = BomPolicy.WITH_BOM")
          .addPostfix("        properties {")
          .addPostfix("          encoding = 'UTF-8'")
          .addPostfix("          transparentNativeToAsciiConversion = false")
          .addPostfix("        }")
          .addPostfix("      }")
          .addPostfix("    }")
          .addPostfix("  }")
          .addPostfix("}")
          .generate());
      EncodingProjectManagerImpl encodingManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
      assertEquals("UTF-8", encodingManager.getDefaultCharset().name());
      assertEquals("UTF-8", encodingManager.getDefaultCharsetForPropertiesFiles(null).name());
      assertFalse(encodingManager.isNative2AsciiForPropertiesFiles());
      assertTrue(encodingManager.shouldAddBOMForNewUtf8File());
    }
  }

  @Test
  public void testImportFileEncodingSettings() throws IOException {
    VirtualFile aDir = createProjectSubDir("src/main/java/a");
    VirtualFile bDir = createProjectSubDir("src/main/java/b");
    VirtualFile cDir = createProjectSubDir("src/main/java/c");
    VirtualFile mainDir = createProjectSubDir("../sub-project/src/main/java");
    createProjectSubFile("src/main/java/a/A.java");
    createProjectSubFile("src/main/java/c/C.java");
    createProjectSubFile("../sub-project/src/main/java/Main.java");
    {
      importProject(
        new GradleBuildScriptBuilderEx()
          .withJavaPlugin()
          .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
          .addImport("org.jetbrains.gradle.ext.EncodingConfiguration.BomPolicy")
          .addPostfix("sourceSets {")
          .addPostfix("  main.java.srcDirs += '../sub-project/src/main/java'")
          .addPostfix("}")
          .addPostfix("idea {")
          .addPostfix("  project {")
          .addPostfix("    settings {")
          .addPostfix("      encodings {")
          .addPostfix("        mapping['src/main/java/a'] = 'ISO-8859-9'")
          .addPostfix("        mapping['src/main/java/b'] = 'x-EUC-TW'")
          .addPostfix("        mapping['src/main/java/c'] = 'UTF-8'")
          .addPostfix("        mapping['../sub-project/src/main/java'] = 'KOI8-R'")
          .addPostfix("      }")
          .addPostfix("    }")
          .addPostfix("  }")
          .addPostfix("}")
          .generate());
      EncodingProjectManagerImpl encodingManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
      Map<String, String> allMappings = encodingManager.getAllMappings().entrySet().stream()
        .collect(Collectors.toMap(it -> it.getKey().getCanonicalPath(), it -> it.getValue().name()));
      assertEquals("ISO-8859-9", allMappings.get(aDir.getCanonicalPath()));
      assertEquals("x-EUC-TW", allMappings.get(bDir.getCanonicalPath()));
      assertEquals("UTF-8", allMappings.get(cDir.getCanonicalPath()));
      assertEquals("KOI8-R", allMappings.get(mainDir.getCanonicalPath()));
    }
    {
      importProject(
        new GradleBuildScriptBuilderEx()
          .withJavaPlugin()
          .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
          .addImport("org.jetbrains.gradle.ext.EncodingConfiguration.BomPolicy")
          .addPostfix("sourceSets {")
          .addPostfix("  main.java.srcDirs += '../sub-project/src/main/java'")
          .addPostfix("}")
          .addPostfix("idea {")
          .addPostfix("  project {")
          .addPostfix("    settings {")
          .addPostfix("      encodings {")
          .addPostfix("        mapping['src/main/java/a'] = '<System Default>'")
          .addPostfix("        mapping['src/main/java/b'] = '<System Default>'")
          .addPostfix("        mapping['../sub-project/src/main/java'] = '<System Default>'")
          .addPostfix("      }")
          .addPostfix("    }")
          .addPostfix("  }")
          .addPostfix("}")
          .generate());
      EncodingProjectManagerImpl encodingManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
      Map<String, String> allMappings = encodingManager.getAllMappings().entrySet().stream()
        .collect(Collectors.toMap(it -> it.getKey().getCanonicalPath(), it -> it.getValue().name()));
      assertNull(allMappings.get(aDir.getCanonicalPath()));
      assertNull(allMappings.get(bDir.getCanonicalPath()));
      assertEquals("UTF-8", allMappings.get(cDir.getCanonicalPath()));
      assertNull(allMappings.get(mainDir.getCanonicalPath()));
    }
  }

  @Test
  public void testActionDelegationImport() throws Exception {
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "import static org.jetbrains.gradle.ext.ActionDelegationConfig.TestRunner.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    delegateActions {\n" +
        "      delegateBuildRunToGradle = true\n" +
        "      testRunner = CHOOSE_PER_TEST\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    String projectPath = getCurrentExternalProjectSettings().getExternalProjectPath();
    assertTrue(GradleProjectSettings.isDelegatedBuildEnabled(myProject, projectPath));
    assertEquals(TestRunner.CHOOSE_PER_TEST, GradleProjectSettings.getTestRunner(myProject, projectPath));
  }

  @Test
  public void testSavePackagePrefixAfterReOpenProject() throws IOException {
    @Language("Groovy") String buildScript = new GradleBuildScriptBuilderEx().withJavaPlugin().generate();
    createProjectSubFile("src/main/java/Main.java", "");
    importProject(buildScript);
    Application application = ApplicationManager.getApplication();
    IdeModifiableModelsProvider modelsProvider = new IdeModifiableModelsProviderImpl(myProject);
    try {
      Module module = modelsProvider.findIdeModule("project.main");
      ModifiableRootModel modifiableRootModel = modelsProvider.getModifiableRootModel(module);
      SourceFolder sourceFolder = findSource(modifiableRootModel, "src/main/java");
      sourceFolder.setPackagePrefix("prefix.package.some");
      application.invokeAndWait(() -> application.runWriteAction(() -> modelsProvider.commit()));
    }
    finally {
      application.invokeAndWait(() -> modelsProvider.dispose());
    }
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.some");
    importProject(buildScript);
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.some");
  }

  @Test
  public void testRemovingSourceFolderManagerMemLeaking() throws IOException {
    SourceFolderManagerImpl sourceFolderManager = (SourceFolderManagerImpl)SourceFolderManager.getInstance(myProject);
    String javaSourcePath = FileUtil.toCanonicalPath(myProjectRoot.getPath() + "/java");
    String javaSourceUrl = VfsUtilCore.pathToUrl(javaSourcePath);
    {
      importProject(
        new GradleBuildScriptBuilderEx()
          .withJavaPlugin()
          .addPostfix("sourceSets {")
          .addPostfix("  main.java.srcDirs += 'java'")
          .addPostfix("}")
          .generate());
      Set<String> sourceFolders = sourceFolderManager.getSourceFolders("project.main");
      assertTrue(sourceFolders.contains(javaSourceUrl));
    }
    {
      importProject(
        new GradleBuildScriptBuilderEx()
          .withJavaPlugin()
          .generate());
      Set<String> sourceFolders = sourceFolderManager.getSourceFolders("project.main");
      assertFalse(sourceFolders.contains(javaSourceUrl));
    }
  }

  @Test
  public void testSourceFolderIsDisposedAfterProjectDisposing() throws IOException {
    importProject(new GradleBuildScriptBuilder().generate());
    Application application = ApplicationManager.getApplication();
    Ref<Project> projectRef = new Ref<>();
    application.invokeAndWait(() -> projectRef.set(ProjectUtil.openOrImport(myProjectRoot.toNioPath())));
    Project project = projectRef.get();
    SourceFolderManagerImpl sourceFolderManager = (SourceFolderManagerImpl)SourceFolderManager.getInstance(project);
    try {
      assertFalse(project.isDisposed());
      assertFalse(sourceFolderManager.isDisposed());
    }
    finally {
      PlatformTestUtil.forceCloseProjectWithoutSaving(project);
    }
    assertTrue(project.isDisposed());
    assertTrue(sourceFolderManager.isDisposed());
  }

  @Test
  public void testPostponedImportPackagePrefix() throws IOException {
    createProjectSubFile("src/main/java/Main.java", "");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .withJavaPlugin()
        .withKotlinPlugin("1.3.50")
        .addPostfix("idea {")
        .addPostfix("  module {")
        .addPostfix("    settings {")
        .addPostfix("      packagePrefix['src/main/java'] = 'prefix.package.some'")
        .addPostfix("      packagePrefix['src/main/kotlin'] = 'prefix.package.other'")
        .addPostfix("      packagePrefix['src/test/java'] = 'prefix.package.some.test'")
        .addPostfix("    }")
        .addPostfix("  }")
        .addPostfix("}")
        .generate());
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.some");
    assertSourceNotExists("project.main", "src/main/kotlin");
    assertSourceNotExists("project.test", "src/test/java");
    createProjectSubFile("src/main/kotlin/Main.kt", "");
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.some");
    assertSourcePackagePrefix("project.main", "src/main/kotlin", "prefix.package.other");
    assertSourceNotExists("project.test", "src/test/java");
  }

  @Test
  public void testPartialImportPackagePrefix() throws IOException {
    createProjectSubFile("src/main/java/Main.java", "");
    createProjectSubFile("src/main/kotlin/Main.kt", "");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .withJavaPlugin()
        .withKotlinPlugin("1.3.50")
        .addPostfix("idea {")
        .addPostfix("  module {")
        .addPostfix("    settings {")
        .addPostfix("      packagePrefix['src/main/java'] = 'prefix.package.some'")
        .addPostfix("    }")
        .addPostfix("  }")
        .addPostfix("}")
        .generate());
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.some");
    assertSourcePackagePrefix("project.main", "src/main/kotlin", "");
  }

  @Test
  public void testImportPackagePrefixWithRemoteSourceRoot() throws IOException {
    createProjectSubFile("src/test/java/Main.java", "");
    createProjectSubFile("../subproject/src/test/java/Main.java", "");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .withJavaPlugin()
        .addPostfix("sourceSets {")
        .addPostfix("  test.java.srcDirs += '../subproject/src/test/java'")
        .addPostfix("}")
        .addPostfix("idea {")
        .addPostfix("  module {")
        .addPostfix("    settings {")
        .addPostfix("      packagePrefix['src/test/java'] = 'prefix.package.some'")
        .addPostfix("      packagePrefix['../subproject/src/test/java'] = 'prefix.package.other'")
        .addPostfix("    }")
        .addPostfix("  }")
        .addPostfix("}")
        .generate());
    printProjectStructure();
    assertSourcePackagePrefix("project.test", "src/test/java", "prefix.package.some");
    assertSourcePackagePrefix("project.test", "../subproject/src/test/java", "prefix.package.other");
  }

  @Test
  public void testImportPackagePrefix() throws IOException {
    createProjectSubFile("src/main/java/Main.java", "");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .withJavaPlugin()
        .addPostfix("idea {")
        .addPostfix("  module {")
        .addPostfix("    settings {")
        .addPostfix("      packagePrefix['src/main/java'] = 'prefix.package.some'")
        .addPostfix("    }")
        .addPostfix("  }")
        .addPostfix("}")
        .generate());
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.some");
  }

  @Test
  public void testChangeImportPackagePrefix() throws IOException {
    createProjectSubFile("src/main/java/Main.java", "");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .withJavaPlugin()
        .addPostfix("idea {")
        .addPostfix("  module {")
        .addPostfix("    settings {")
        .addPostfix("      packagePrefix['src/main/java'] = 'prefix.package.some'")
        .addPostfix("    }")
        .addPostfix("  }")
        .addPostfix("}")
        .generate());
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.some");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .withJavaPlugin()
        .addPostfix("idea {")
        .addPostfix("  module {")
        .addPostfix("    settings {")
        .addPostfix("      packagePrefix['src/main/java'] = 'prefix.package.other'")
        .addPostfix("    }")
        .addPostfix("  }")
        .addPostfix("}")
        .generate());
    assertSourcePackagePrefix("project.main", "src/main/java", "prefix.package.other");
  }
}
