// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.TestModuleProperties;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.*;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("GrUnresolvedAccess") // ignore unresolved code for injected Groovy Gradle DSL
public class GradleMiscImportingTest extends GradleJavaImportingTestCase {

  /**
   * It's sufficient to run the test against one gradle version
   */
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{BASE_GRADLE_VERSION}});
  }

  @Test
  public void testTestModuleProperties() throws Exception {
    importProject(
      "apply plugin: 'java'"
    );

    assertModules("project", "project.main", "project.test");

    final Module testModule = getModule("project.test");
    TestModuleProperties testModuleProperties = TestModuleProperties.getInstance(testModule);
    assertEquals("project.main", testModuleProperties.getProductionModuleName());

    final Module productionModule = getModule("project.main");
    assertSame(productionModule, testModuleProperties.getProductionModule());
  }

  @Test
  public void testTestModulePropertiesForModuleWithHyphenInName() throws Exception {
    createSettingsFile("rootProject.name='my-project'");
    importProject(
      "apply plugin: 'java'"
    );

    assertModules("my-project", "my-project.main", "my-project.test");

    final Module testModule = getModule("my-project.test");
    TestModuleProperties testModuleProperties = TestModuleProperties.getInstance(testModule);
    assertEquals("my-project.main", testModuleProperties.getProductionModuleName());
  }

  @Test
  public void testInheritProjectJdkForModules() throws Exception {
    importProject(
      "apply plugin: 'java'"
    );

    assertModules("project", "project.main", "project.test");
    assertTrue(ModuleRootManager.getInstance(getModule("project")).isSdkInherited());
    assertTrue(ModuleRootManager.getInstance(getModule("project.main")).isSdkInherited());
    assertTrue(ModuleRootManager.getInstance(getModule("project.test")).isSdkInherited());
  }

  @Test
  public void testLanguageLevel() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "sourceCompatibility = 1.5\n" +
      "compileTestJava {\n" +
      "  sourceCompatibility = 1.8\n" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test");
    assertEquals(LanguageLevel.JDK_1_5, getLanguageLevelForModule("project"));
    assertEquals(LanguageLevel.JDK_1_5, getLanguageLevelForModule("project.main"));
    assertEquals(LanguageLevel.JDK_1_8, getLanguageLevelForModule("project.test"));
  }

  @Test
  public void testPreviewLanguageLevel() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "sourceCompatibility = 14\n" +
      "apply plugin: 'java'\n" +
      "compileTestJava {\n" +
      "  sourceCompatibility = 14\n" +
      "  options.compilerArgs << '--enable-preview'" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test");
    assertEquals(LanguageLevel.JDK_14, getLanguageLevelForModule("project"));
    assertEquals(LanguageLevel.JDK_14, getLanguageLevelForModule("project.main"));
    assertEquals(LanguageLevel.JDK_14_PREVIEW, getLanguageLevelForModule("project.test"));
  }

  @Test
  public void testTargetLevel() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "targetCompatibility = 1.8\n" +
      "compileJava {\n" +
      "  targetCompatibility = 1.5\n" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test");
    assertEquals("1.5", getBytecodeTargetLevelForModule("project.main"));
    assertEquals("1.8", getBytecodeTargetLevelForModule("project.test"));

  }

  @Test
  @TargetVersions("3.4+")
  public void testJdkName() throws Exception {
    Sdk myJdk = createJdk("MyJDK");
    edt(() -> ApplicationManager.getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().addJdk(myJdk, myProject)));
    importProject(
      "apply plugin: 'java'\n" +
      "apply plugin: 'idea'\n" +
      "idea {\n" +
      "  module {\n" +
      "    jdkName = 'MyJDK'\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test");
    assertSame(getSdkForModule("project.main"), myJdk);
    assertSame(getSdkForModule("project.test"), myJdk);
  }

  @Test
  public void testUnloadedModuleImport() throws Exception {
    importProject(
      "apply plugin: 'java'"
    );
    assertModules("project", "project.main", "project.test");

    edt(() -> ModuleManager.getInstance(myProject).setUnloadedModules(Collections.singletonList("project.main")));
    assertModules("project", "project.test");

    importProject();
    assertModules("project", "project.test");
  }

  @Test
  public void testESLinkedProjectIds() throws Exception {
    // main build
    createSettingsFile("rootProject.name = 'multiproject'\n" +
                       "include ':app'\n" +
                       "include ':util'\n" +
                       "includeBuild 'included-build'");
    createProjectSubFile("build.gradle", "allprojects { apply plugin: 'java' }");

    // main buildSrc
    createProjectSubFile("buildSrc/settings.gradle", "include ':buildSrcSubProject'\n" +
                                                     "include ':util'");
    createProjectSubFile("buildSrc/build.gradle", "allprojects { apply plugin: 'java' }");

    // included build with buildSrc
    createProjectSubFile("included-build/settings.gradle", "rootProject.name = 'inc-build'\n" +
                                                           "include ':util'");
    createProjectSubFile("included-build/buildSrc/settings.gradle", "include ':util'");

    importProject();
    assertModules(
      "multiproject",
      "multiproject.main",
      "multiproject.test",

      "multiproject.buildSrc",
      "multiproject.buildSrc.main",
      "multiproject.buildSrc.test",

      "multiproject.buildSrc.buildSrcSubProject",
      "multiproject.buildSrc.buildSrcSubProject.main",
      "multiproject.buildSrc.buildSrcSubProject.test",

      "multiproject.buildSrc.util",
      "multiproject.buildSrc.util.main",
      "multiproject.buildSrc.util.test",

      "multiproject.app",
      "multiproject.app.main",
      "multiproject.app.test",

      "multiproject.util",
      "multiproject.util.main",
      "multiproject.util.test",

      "inc-build",
      "inc-build.util",

      "inc-build.buildSrc",
      "inc-build.buildSrc.util",
      "inc-build.buildSrc.main",
      "inc-build.buildSrc.test"
    );

    assertExternalProjectId("multiproject", "multiproject");
    assertExternalProjectId("multiproject.main", "multiproject:main");
    assertExternalProjectId("multiproject.test", "multiproject:test");

    assertExternalProjectId("multiproject.buildSrc", "multiproject:buildSrc");
    assertExternalProjectId("multiproject.buildSrc.main", "multiproject:buildSrc:main");
    assertExternalProjectId("multiproject.buildSrc.test", "multiproject:buildSrc:test");

    assertExternalProjectId("multiproject.buildSrc.buildSrcSubProject", "multiproject:buildSrc:buildSrcSubProject");
    assertExternalProjectId("multiproject.buildSrc.buildSrcSubProject.main", "multiproject:buildSrc:buildSrcSubProject:main");
    assertExternalProjectId("multiproject.buildSrc.buildSrcSubProject.test", "multiproject:buildSrc:buildSrcSubProject:test");

    assertExternalProjectId("multiproject.buildSrc.util", "multiproject:buildSrc:util");
    assertExternalProjectId("multiproject.buildSrc.util.main", "multiproject:buildSrc:util:main");
    assertExternalProjectId("multiproject.buildSrc.util.test", "multiproject:buildSrc:util:test");

    assertExternalProjectId("multiproject.app", ":app");
    assertExternalProjectId("multiproject.app.main", ":app:main");
    assertExternalProjectId("multiproject.app.test", ":app:test");

    assertExternalProjectId("multiproject.util", ":util");
    assertExternalProjectId("multiproject.util.main", ":util:main");
    assertExternalProjectId("multiproject.util.test", ":util:test");

    assertExternalProjectId("inc-build", "inc-build");
    assertExternalProjectId("inc-build.util", "inc-build:util");

    assertExternalProjectId("inc-build.buildSrc", "inc-build:buildSrc");
    assertExternalProjectId("inc-build.buildSrc.util", "inc-build:buildSrc:util");
    assertExternalProjectId("inc-build.buildSrc.main", "inc-build:buildSrc:main");
    assertExternalProjectId("inc-build.buildSrc.test", "inc-build:buildSrc:test");

    Map<String, ExternalProject> projectMap = getExternalProjectsMap();
    assertExternalProjectIds(projectMap, "multiproject", "multiproject:main", "multiproject:test");
    assertExternalProjectIds(projectMap, ":app", ":app:main", ":app:test");
    assertExternalProjectIds(projectMap, ":util", ":util:main", ":util:test");
    assertExternalProjectIds(projectMap, "inc-build", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertExternalProjectIds(projectMap, "inc-build:util", ArrayUtilRt.EMPTY_STRING_ARRAY);

    // Note, currently ExternalProject models are not exposed for "buildSrc" projects
  }

  @Test
  public void testSourceSetModuleNamesForDeduplicatedMainModule() throws Exception {
    IdeModifiableModelsProvider modelsProvider = new IdeModifiableModelsProviderImpl(myProject);
    modelsProvider.newModule(getProjectPath() + "/app.iml", StdModuleTypes.JAVA.getId());
    modelsProvider.newModule(getProjectPath() + "/my_group.app.main.iml", StdModuleTypes.JAVA.getId());
    edt(() -> ApplicationManager.getApplication().runWriteAction(modelsProvider::commit));

    createSettingsFile("rootProject.name = 'app'");
    importProject("apply plugin: 'java'\n" +
                  "group 'my_group'");

    assertModules("app", "my_group.app.main",
                  "my_group.app", "my_group.app.main~1", "my_group.app.test");

    assertNull(ExternalSystemApiUtil.getExternalProjectPath(getModule("app")));
    assertNull(ExternalSystemApiUtil.getExternalProjectPath(getModule("my_group.app.main")));
    assertEquals(getProjectPath(), ExternalSystemApiUtil.getExternalProjectPath(getModule("my_group.app")));
    assertEquals(getProjectPath(), ExternalSystemApiUtil.getExternalProjectPath(getModule("my_group.app.main~1")));
    assertEquals(getProjectPath(), ExternalSystemApiUtil.getExternalProjectPath(getModule("my_group.app.test")));
  }

  private static void assertExternalProjectIds(Map<String, ExternalProject> projectMap, String projectId, String... sourceSetModulesIds) {
    ExternalProject externalProject = projectMap.get(projectId);
    assertEquals(projectId, externalProject.getId());
    List<String> actualSourceSetModulesIds = ContainerUtil.map(
      externalProject.getSourceSets().values(), sourceSet -> GradleProjectResolverUtil.getModuleId(externalProject, sourceSet));
    assertSameElements(actualSourceSetModulesIds, sourceSetModulesIds);
  }

  @NotNull
  private Map<String, ExternalProject> getExternalProjectsMap() {
    ExternalProject rootExternalProject = ExternalProjectDataCache.getInstance(myProject).getRootExternalProject(getProjectPath());
    final Map<String, ExternalProject> externalProjectMap = new THashMap<>();
    if (rootExternalProject == null) return externalProjectMap;
    ArrayDeque<ExternalProject> queue = new ArrayDeque<>();
    queue.add(rootExternalProject);
    ExternalProject externalProject;
    while ((externalProject = queue.pollFirst()) != null) {
      queue.addAll(externalProject.getChildProjects().values());
      externalProjectMap.put(externalProject.getId(), externalProject);
    }
    return externalProjectMap;
  }

  private void assertExternalProjectId(String moduleName, String expectedId) {
    assertEquals(expectedId, ExternalSystemApiUtil.getExternalProjectId(getModule(moduleName)));
  }
}
