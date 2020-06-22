/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.importing;

import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager;
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManagerImpl;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.doWriteAction;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.getManager;

/**
 * @author Vladislav.Soroka
 */
public class GradleFoldersImportingTest extends GradleImportingTestCase {

  @Test
  public void testUnsupportedTypesInDsl() throws Exception {
    importProject(
      new GradleBuildScriptBuilderEx().addPostfix(
          "import org.gradle.api.internal.FactoryNamedDomainObjectContainer;",
          "import org.gradle.internal.reflect.Instantiator;",
          "class MyObj implements Named {",
          "  String myName;",
          "  public MyObj(String name) {",
          "    myName = namse",
          "  }",
          "  ",
          "  public String getName() {",
          "    return myName",
          "  }",
          "}",
          "project.extensions.create(",
          "                \"sourceSets\",",
          "                FactoryNamedDomainObjectContainer,",
          "                MyObj,",
          "                services.get(Instantiator),",
          "               {action -> }",
          "        ) ",
          "sourceSets {",
          " println \"Hello World!\"",
          "}"
      ).generate());
  }

  @Test
  public void testBaseJavaProject() throws Exception {
    getCurrentExternalProjectSettings().setDelegatedBuild(false);
    createDefaultDirs();
    importProject(
      "apply plugin: 'java'"
    );
    assertNotDelegatedBaseJavaProject();

    getCurrentExternalProjectSettings().setResolveModulePerSourceSet(false);
    importProject();
    assertNotDelegatedMergedBaseJavaProject();

    getCurrentExternalProjectSettings().setDelegatedBuild(true);
    importProject();
    assertDelegatedMergedBaseJavaProject();

    getCurrentExternalProjectSettings().setDelegatedBuild(false);
    // subscribe to the GradleSettings changes topic
    ((GradleManager)getManager(GradleConstants.SYSTEM_ID)).runActivity(myProject);
    GradleSettings.getInstance(myProject).getPublisher().onBuildDelegationChange(false, getProjectPath());
    assertNotDelegatedMergedBaseJavaProject();

    getCurrentExternalProjectSettings().setResolveModulePerSourceSet(true);
    importProject();
    assertNotDelegatedBaseJavaProject();
    getCurrentExternalProjectSettings().setDelegatedBuild(true);
    GradleSettings.getInstance(myProject).getPublisher().onBuildDelegationChange(true, getProjectPath());
    assertDelegatedBaseJavaProject();
  }

  private void assertNotDelegatedBaseJavaProject() {
    assertModules("project", "project.main", "project.test");
    assertContentRoots("project", getProjectPath());

    assertDefaultGradleJavaProjectFolders("project");


    final String mainClassesOutputPath = "/out/production/classes";
    assertModuleOutputs("project.main",
                        getProjectPath() + mainClassesOutputPath,
                        getProjectPath() + "/out/production/resources");
    String testClassesOutputPath = "/out/test/classes";
    assertModuleOutputs("project.test",
                        getProjectPath() + testClassesOutputPath,
                        getProjectPath() + "/out/test/resources");

    assertModuleOutput("project.main", getProjectPath() + mainClassesOutputPath, "");
    assertModuleOutput("project.test", "", getProjectPath() + testClassesOutputPath);
  }

  private void assertDelegatedBaseJavaProject() {
    assertModules("project", "project.main", "project.test");
    assertContentRoots("project", getProjectPath());

    if (isGradleNewerOrSameThen("4.0")) {
      assertModuleOutputs("project.main",
                          getProjectPath() + "/build/classes/java/main",
                          getProjectPath() + "/build/resources/main");
      assertModuleOutput("project.main", getProjectPath() + "/build/classes/java/main", "");

      assertModuleOutputs("project.test",
                          getProjectPath() + "/build/classes/java/test",
                          getProjectPath() + "/build/resources/test");
      assertModuleOutput("project.test", "", getProjectPath() + "/build/classes/java/test");
    } else {
      assertModuleOutputs("project.main",
                          getProjectPath() + "/build/classes/main",
                          getProjectPath() + "/build/resources/main");
      assertModuleOutput("project.main", getProjectPath() + "/build/classes/main", "");

      assertModuleOutputs("project.test",
                          getProjectPath() + "/build/classes/test",
                          getProjectPath() + "/build/resources/test");
      assertModuleOutput("project.test", "", getProjectPath() + "/build/classes/test");
    }
  }

  private void assertNotDelegatedMergedBaseJavaProject() {
    assertModules("project");
    assertDefaultGradleJavaProjectFoldersForMergedModule("project");

    assertModuleOutputs("project",
                        getProjectPath() + "/out/production/classes",
                        getProjectPath() + "/out/production/resources",
                        getProjectPath() + "/out/test/classes",
                        getProjectPath() + "/out/test/resources");

    assertModuleOutput("project", getProjectPath() + "/out/production/classes", getProjectPath() + "/out/test/classes");
  }

  private void assertDelegatedMergedBaseJavaProject() {
    if (isGradleNewerOrSameThen("4.0")) {
      assertModuleOutputs("project",
                          getProjectPath() + "/build/classes/java/main",
                          getProjectPath() + "/build/resources/main",
                          getProjectPath() + "/build/classes/java/test",
                          getProjectPath() + "/build/resources/test");
    } else {
      assertModuleOutputs("project",
                          getProjectPath() + "/build/classes/main",
                          getProjectPath() + "/build/resources/main",
                          getProjectPath() + "/build/classes/test",
                          getProjectPath() + "/build/resources/test");
    }
  }

  @Test
  public void testCompileOutputPathCustomizedWithIdeaPlugin() throws Exception {
    createDefaultDirs();
    importProject(
      "apply plugin: 'java'\n" +
      "apply plugin: 'idea'\n" +
      "idea {\n" +
      "  module {\n" +
      "    outputDir = file(buildDir)\n" +
      "  }\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertContentRoots("project", getProjectPath());

    assertDefaultGradleJavaProjectFolders("project");

    assertModuleOutput("project.main", getProjectPath() + "/build", "");
    String testClassesOutputPath = isGradleNewerOrSameThen("4.0") ? "/build/classes/java/test" : "/build/classes/test";
    assertModuleOutput("project.test", "", getProjectPath() + testClassesOutputPath);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");
    assertContentRoots("project", getProjectPath());

    assertDefaultGradleJavaProjectFoldersForMergedModule("project");

    assertModuleOutput("project", getProjectPath() + "/build", getProjectPath() + testClassesOutputPath);
  }

  @Test
  @TargetVersions("2.2+")
  public void testSourceGeneratedFoldersWithIdeaPlugin() throws Exception {
    createDefaultDirs();
    importProject(
      "apply plugin: 'java'\n" +
      "apply plugin: 'idea'\n" +
      "idea {\n" +
      "  module {\n" +
      "    generatedSourceDirs += file('src/main/java')\n" +
      "    generatedSourceDirs += file('src/test/java')\n" +
      "  }\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertContentRoots("project", getProjectPath());

    assertDefaultGradleJavaProjectFolders("project");
    assertGeneratedSources("project.main", "java");
    assertGeneratedTestSources("project.test", "java");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");
    assertContentRoots("project", getProjectPath());

    assertDefaultGradleJavaProjectFoldersForMergedModule("project");
    assertGeneratedSources("project", "src/main/java");
    assertGeneratedTestSources("project", "src/test/java");
  }

  @Test
  public void testCustomSourceSetsAreImported() throws Exception {
    createDefaultDirs();
    createProjectSubFile("src/generated/java/G.java");

    importProject("" +
                  "apply plugin: 'java'\n" +
                  "apply plugin: 'idea'\n" +
                  "\n" +
                  "sourceSets {\n" +
                  "  generated\n" +
                  "}");

    assertModules("project", "project.main", "project.test", "project.generated");

    importProjectUsingSingeModulePerGradleProject();
    assertSources("project", "src/generated/java", "src/main/java");
    assertTestSources("project", "src/test/java");
  }

  @Test
  @TargetVersions("4.7+")
  public void testResourceFoldersWithIdeaPlugin() throws Exception {
    createProjectSubDirs("src/main/java",
                         "src/main/src2",
                         "src/main/resources",
                         "src/main/resources2",
                         "src/test/java",
                         "src/test/src2",
                         "src/test/resources",
                         "src/test/resources2");
    importProject(
      "apply plugin: 'java'\n" +
      "apply plugin: 'idea'\n" +
      "idea {\n" +
      "  module {\n" +
      "    sourceDirs += file('src/main/src2')\n" +
      "    resourceDirs += file('src/main/resources2')\n" +
      "    testSourceDirs += file('src/test/src2')\n" +
      "    testResourceDirs += file('src/test/resources2')\n" +
      "  }\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertContentRoots("project", getProjectPath());
    assertExcludes("project", ".gradle", "build");
    assertContentRoots("project.main", getProjectPath() + "/src/main");
    assertSources("project.main", "java", "src2");
    assertResources("project.main", "resources", "resources2");
    assertContentRoots("project.test", getProjectPath() + "/src/test");
    assertTestSources("project.test", "java", "src2");
    assertTestResources("project.test", "resources", "resources2");

    importProjectUsingSingeModulePerGradleProject();

    assertModules("project");
    assertContentRoots("project", getProjectPath());

    assertExcludes("project", ".gradle", "build");
    assertSources("project", "src/main/java", "src/main/src2");
    assertResources("project", "src/main/resources", "src/main/resources2");
    assertTestSources("project", "src/test/java", "src/test/src2");
    assertTestResources("project", "src/test/resources", "src/test/resources2");
  }

  @Test
  @TargetVersions("4.7+")
  public void testSourceFoldersTypeAfterReimport() throws Exception {
    createProjectSubDirs("src/main/java",
                         "src/main/resources",
                         "src/test/java",
                         "src/test/src2",
                         "src/test/resources",
                         "src/test/resources2",
                         "src/customSourceSet/java",
                         "src/customSourceSet/resources");
    importProject(
      "apply plugin: 'java'\n" +
      "apply plugin: 'idea'\n" +
      "sourceSets {\n" +
      "    customSourceSet\n" +
      "}\n" +
      "idea {\n" +
      "  module {\n" +
      "    testSourceDirs += file('src/test/src2')\n" +
      "    testResourceDirs += file('src/test/resources2')\n" +
      "    testSourceDirs += project.sourceSets.customSourceSet.java.srcDirs\n" +
      "    testResourceDirs += project.sourceSets.customSourceSet.resources.srcDirs\n" +
      "  }\n" +
      "}"
    );

    Runnable check = () -> {
      assertModules("project", "project.main", "project.test", "project.customSourceSet");
      assertContentRoots("project", getProjectPath());
      assertExcludes("project", ".gradle", "build");
      assertContentRoots("project.main", getProjectPath() + "/src/main");
      assertSources("project.main", "java");
      assertResources("project.main", "resources");
      assertContentRoots("project.test", getProjectPath() + "/src/test");
      assertTestSources("project.test", "java", "src2");
      assertTestResources("project.test", "resources", "resources2");

      assertContentRoots("project.customSourceSet", getProjectPath() + "/src/customSourceSet");
      assertTestSources("project.customSourceSet", "java");
      assertTestResources("project.customSourceSet", "resources");
    };

    check.run();
    markModuleSourceFolders("project.customSourceSet", JavaSourceRootType.SOURCE);
    importProject();
    check.run();
  }

  private void markModuleSourceFolders(@NotNull String moduleName, @NotNull JpsModuleSourceRootType<?> rootType) {
    doWriteAction(() -> {
      Module module = getModule(moduleName);
      final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
      ContentEntry[] contentEntries = model.getContentEntries();
      for (ContentEntry contentEntry : contentEntries) {
        final SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
        for (SourceFolder sourceFolder : sourceFolders) {
          VirtualFile folderFile = sourceFolder.getFile();
          contentEntry.removeSourceFolder(sourceFolder);
          contentEntry.addSourceFolder(folderFile, rootType);
        }
      }
      model.commit();
    });
  }

  @Test
  public void testProjectWithInheritedOutputDirs() throws Exception {

    createDefaultDirs();
    importProject(
      "apply plugin: 'java'\n" +
      "apply plugin: 'idea'\n" +
      "idea {\n" +
      "  module {\n" +
      "    inheritOutputDirs = true\n" +
      "  }\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertContentRoots("project", getProjectPath());

    assertDefaultGradleJavaProjectFolders("project");

    assertModuleInheritedOutput("project");
    assertModuleInheritedOutput("project.main");
    assertModuleInheritedOutput("project.test");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");
    assertContentRoots("project", getProjectPath());

    assertDefaultGradleJavaProjectFoldersForMergedModule("project");

    assertModuleInheritedOutput("project");
  }

  @Test
  public void testSourceFoldersMerge() throws Exception {

    createDefaultDirs();
    createProjectSubFile("src/B.java");
    createProjectSubFile("src/resources/res.properties");
    createProjectSubFile("test/BTest.java");
    createProjectSubFile("test/resources/res_test.properties");

    importProject(
      "apply plugin: 'java'\n" +
      "sourceSets {\n" +
      "  main {\n" +
      "    resources.srcDir 'src/resources'\n" +
      "    java.srcDir 'src'\n" +
      "  }\n" +
      "  test {\n" +
      "    resources.srcDir 'test/resources'\n" +
      "    java.srcDir 'test'\n" +
      "  }\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertContentRoots("project", getProjectPath());

    assertExcludes("project", ".gradle", "build");
    final String mainSourceSetModuleName = "project.main";
    assertContentRoots(mainSourceSetModuleName, getProjectPath() + "/src");
    assertSources(mainSourceSetModuleName, "", "main/java");
    assertResources(mainSourceSetModuleName, "main/resources", "resources");
    final String testSourceSetModuleName = "project.test";
    assertContentRoots(testSourceSetModuleName, getProjectPath() + "/test", getProjectPath() + "/src/test");
    assertTestSources(testSourceSetModuleName, "src/test/java", "test");
    assertTestResources(testSourceSetModuleName, "src/test/resources", "test/resources");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");
    assertContentRoots("project", getProjectPath());
    assertExcludes("project", ".gradle", "build");
    assertSources("project", "src", "src/main/java");
    assertResources("project", "src/main/resources", "src/resources");
    assertTestSources("project", "src/test/java", "test");
    assertTestResources("project", "src/test/resources", "test/resources");
  }

  @Test
  public void testRootsAreNotCreatedIfFilesAreMissing() throws Exception {
    createProjectSubFile("src/main/java/A.java");
    createProjectSubFile("src/test/resources/res.properties");
    importProjectUsingSingeModulePerGradleProject(
      "apply plugin: 'java'\n" +
      "apply plugin: 'idea'\n" +
      "sourceSets.main.java.srcDirs file('src/generated/java')\n" +
      "idea.module {\n" +
      "  generatedSourceDirs += file('src/generated/java')\n" +
      "}"
    );

    assertModules("project");
    assertExcludes("project", ".gradle", "build");
    assertSources("project", "src/main/java");
    assertResources("project");
    assertTestSources("project");
    assertGeneratedSources("project");
    assertTestResources("project", "src/test/resources");
  }

  @Test
  public void testRootsAreAddedWhenAFolderCreated() throws Exception {
    createProjectSubFile("src/main/java/A.java");
    importProjectUsingSingeModulePerGradleProject(      "apply plugin: 'java'\n" +
                                                        "apply plugin: 'idea'\n" +
                                                        "sourceSets.main.java.srcDirs file('src/generated/java')\n" +
                                                        "idea.module {\n" +
                                                        "  generatedSourceDirs += file('src/generated/java')\n" +
                                                        "}");

    assertModules("project");
    assertSources("project", "src/main/java");
    assertTestSources("project");

    createProjectSubFile("src/test/java/ATest.java");
    waitForModulesUpdate();
    assertTestSources("project", "src/test/java");

    createProjectSubFile("src/main/resources/res.txt");
    waitForModulesUpdate();
    assertResources("project", "src/main/resources");

    createProjectSubFile("src/generated/java/Generated.java");
    waitForModulesUpdate();
    assertGeneratedSources("project","src/generated/java");
  }

  @Test
  public void testRootsListenersAreUpdatedWithProjectModel() throws Exception {
    createProjectSubFile("src/main/java/A.java");
    importProjectUsingSingeModulePerGradleProject("apply plugin: 'java'");

    assertModules("project");

    importProjectUsingSingeModulePerGradleProject(
      "apply plugin: 'java'\n" +
      "sourceSets {\n" +
      " test {\n" +
      "    java.srcDirs = [file('test-src/java')]" +
      "  }\n" +
      "}");

    createProjectSubFile("src/test/java/ATest.java");
    createProjectSubFile("test-src/java/BTest.java");
    waitForModulesUpdate();
    assertTestSources("project", "test-src/java");
  }


  @Test
  public void testSourceAndResourceFoldersCollision() throws Exception {
    createProjectSubFile("src/A.java");
    createProjectSubFile("src/production.properties");
    createProjectSubFile("test/Test.java");
    createProjectSubFile("test/test.properties");

    importProject("apply plugin: 'java'\n" +
                  "sourceSets {\n" +
                  "  main {\n" +
                  "    java {\n" +
                  "      srcDir 'src'\n" +

                  "    }\n" +
                  "    resources {\n" +
                  "      srcDir 'src'\n" +
                  "    }\n" +
                  "  }\n" +
                  "  test {\n" +
                  "    java {\n" +
                  "      srcDir 'test'\n" +
                  "    }\n" +
                  "    resources {\n" +
                  "      srcDir 'test'\n" +
                  "    }\n" +
                  "  }\n" +
                  "}\n");
    assertModules("project", "project.main", "project.test");
    assertSources("project.main", "");
    // assert relative to linked project path because several content roots are created for "project.test" module
    assertTestSources("project.test", "test");

    importProjectUsingSingeModulePerGradleProject();

    assertModules("project");
    assertContentRoots("project", getProjectPath());
    assertSources("project", "src");
    assertTestSources("project", "test");
    assertResources("project");
    assertTestResources("project");
  }

  @Test
  public void testModuleGroupingFollowGradleProjectStructure() throws Exception {
    /*
    - Gradle project hierarchy
    project
       \--- project1
              \--- project2
              \--- project3

    - Folder hierarchy
     project
        \--- project1
        |       \--- project2
        \--- project3
                \--- src
                       \--- main
                       \--- test
     */
    createProjectSubFile("settings.gradle"
      , "include (':project1', ':project1:project2', ':project1:project3')\n" +
        "project(':project1:project3').projectDir = file('project3')\n" +
        "rootProject.name = 'rootName'");

    createProjectSubFile("build.gradle",
                         "project(':').group = 'my.test.rootProject.group'\n" +
                         "project(':project1').group = 'my.test.project1.group'\n" +
                         "project(':project1:project2').group = 'my.test.project2.group'\n" +
                         "project(':project1:project3').group = 'my.test.project3.group'");

    createProjectSubFile("project1/build.gradle");
    createProjectSubFile("project1/project2/build.gradle");
    createProjectSubFile("project3/build.gradle", "apply plugin: 'java'");
    createProjectSubFile("project3/src/main/java/AClass.java");
    createProjectSubFile("project3/src/test/java/AClassTest.java");

    importProject();

    assertModules("rootName",
                  "rootName.project1",
                  "rootName.project1.project2",
                  "rootName.project1.project3",
                  "rootName.project1.project3.main",
                  "rootName.project1.project3.test");
    assertContentRoots("rootName.project1.project3",
                       FileUtil.toSystemIndependentName(new File(getProjectPath(), "project3").getAbsolutePath()));

    getCurrentExternalProjectSettings().setUseQualifiedModuleNames(false);

    importProject();
    assertModules("rootName",
                  "project1",
                  "project2",
                  "project3",
                  "project3_main",
                  "project3_test");

    assertContentRoots("project3",
                       FileUtil.toSystemIndependentName(new File(getProjectPath(), "project3").getAbsolutePath()));

    assertModuleGroupPath("rootName", "rootName");
    assertModuleGroupPath("project1", "rootName", "project1");
    assertModuleGroupPath("project2", "rootName", "project1", "project2");
    assertModuleGroupPath("project3", "rootName", "project1", "project3");
  }

  @Test
  public void testSourceFoldersOutOfContentRootWithModuleResolving() throws Exception {
    createProjectSubFile("src/main/java/A.java", "class A {}");
    createProjectSubFile("../outer1/src/main/java/A.java", "class A {}");
    createProjectSubFile("../outer1/src/main/kotlin/A.kt", "class A {}");
    createProjectSubFile("../outer2/src/main/java/A.java", "class A {}");
    createProjectSubFile("../outer3/A.java", "class A {}");
    createProjectSubFile("build/generated/A.java", "class A {}");
    createProjectSubFile("../outer4/generated/A.java", "class A {}");
    GradleBuildScriptBuilder buildScript = new GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withIdeaPlugin()
      .addPrefix("sourceSets {")
      .addPrefix("  generated.java.srcDirs += \"${buildDir}/generated\"")
      .addPrefix("  generated.java.srcDirs += '../outer4/generated'")
      .addPrefix("  main.java.srcDirs += '../outer1/src/main/java'")
      .addPrefix("  main.java.srcDirs += '../outer1/src/main/kotlin'")
      .addPrefix("  main.java.srcDirs += '../outer2/src/main/java'")
      .addPrefix("  main.java.srcDirs += '../outer3'")
      .addPrefix("}")
      .addPrefix("idea {")
      .addPrefix("  module {")
      .addPrefix("    inheritOutputDirs = true")
      .addPrefix("    generatedSourceDirs += file(\"${buildDir}/generated\")")
      .addPrefix("    generatedSourceDirs += file('../outer4/generated')")
      .addPrefix("  }")
      .addPrefix("}");
    importPerSourceSet(true);
    importProject(buildScript.generate());
    assertModules("project", "project.main", "project.test", "project.generated");
    assertContentEntryExists("project", "");
    assertContentEntryExists("project.main",
                             "../outer1",
                             "../outer2",
                             "../outer3");
    assertContentEntryExists("project.generated",
                             "build/generated",
                             "../outer4");
    assertSourceExists("project.main",
                       "src/main/java",
                       "../outer1/src/main/java",
                       "../outer1/src/main/kotlin",
                       "../outer2/src/main/java",
                       "../outer3");
    assertSourceExists("project.generated",
                       "build/generated",
                       "../outer4/generated");
  }

  @Test
  public void testSourceFoldersOutOfContentRootWithoutModuleResolving() throws Exception {
    createProjectSubFile("src/main/java/A.java", "class A {}");
    createProjectSubFile("../outer1/src/main/java/A.java", "class A {}");
    createProjectSubFile("../outer1/src/main/kotlin/A.kt", "class A {}");
    createProjectSubFile("../outer2/src/main/java/A.java", "class A {}");
    createProjectSubFile("../outer3/A.java", "class A {}");
    createProjectSubFile("build/generated/A.java", "class A {}");
    createProjectSubFile("../outer4/generated/A.java", "class A {}");
    GradleBuildScriptBuilder buildScript = new GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withIdeaPlugin()
      .addPrefix("sourceSets {")
      .addPrefix("  generated.java.srcDirs += \"${buildDir}/generated\"")
      .addPrefix("  generated.java.srcDirs += '../outer4/generated'")
      .addPrefix("  main.java.srcDirs += '../outer1/src/main/java'")
      .addPrefix("  main.java.srcDirs += '../outer1/src/main/kotlin'")
      .addPrefix("  main.java.srcDirs += '../outer2/src/main/java'")
      .addPrefix("  main.java.srcDirs += '../outer3'")
      .addPrefix("}")
      .addPrefix("idea {")
      .addPrefix("  module {")
      .addPrefix("    inheritOutputDirs = true")
      .addPrefix("    generatedSourceDirs += file(\"${buildDir}/generated\")")
      .addPrefix("    generatedSourceDirs += file('../outer4/generated')")
      .addPrefix("  }")
      .addPrefix("}");
    importPerSourceSet(false);
    importProject(buildScript.generate());
    assertModules("project");
    assertContentEntryExists("project",
                             "",
                             "../outer1/src/main/java",
                             "../outer1/src/main/kotlin",
                             "../outer2",
                             "../outer3",
                             "build/generated",
                             "../outer4");
    assertSourceExists("project",
                       "src/main/java",
                       "../outer1/src/main/java",
                       "../outer1/src/main/kotlin",
                       "../outer2/src/main/java",
                       "../outer3",
                       "build/generated",
                       "../outer4/generated");
  }

  @Test
  public void testSharedSourceFolders() throws Exception {
    createProjectSubFile("settings.gradle", "include 'app1', 'app2'");
    createProjectSubFile("shared/resources/resource.txt");
    createProjectSubFile("app1/build.gradle", new GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .addPostfix(
        "sourceSets {",
        "  main.resources.srcDir '../shared/resources'",
        "  }"
      )
      .generate());
    createProjectSubFile("app2/build.gradle", new GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .addPostfix(
        "sourceSets {",
        "  main.resources.srcDir '../shared/resources'",
        "  }"
      )
      .generate());

    importPerSourceSet(false);
    importProject("");

    assertModules("project", "project.app1", "project.app2");

    if (isGradleOlderThen("3.4")) {
      assertResources("project.app1");
      assertResources("project.app2", getProjectPath() + "/shared/resources");
    } else {
      assertResources("project.app1", getProjectPath() + "/shared/resources");
      assertResources("project.app2");
    }
  }

  protected void assertDefaultGradleJavaProjectFolders(@NotNull String mainModuleName) {
    boolean isDelegatedBuild = GradleProjectSettings.isDelegatedBuildEnabled(myProject, getProjectPath());
    String[] excludes = isDelegatedBuild ? new String[]{".gradle", "build"} : new String[]{".gradle", "build", "out"};
    assertExcludes(mainModuleName, excludes);
    final String mainSourceSetModuleName = mainModuleName + ".main";
    assertContentRoots(mainSourceSetModuleName, getProjectPath() + "/src/main");
    assertSources(mainSourceSetModuleName, "java");
    assertResources(mainSourceSetModuleName, "resources");
    final String testSourceSetModuleName = mainModuleName + ".test";
    assertContentRoots(testSourceSetModuleName, getProjectPath() + "/src/test");
    assertTestSources(testSourceSetModuleName, "java");
    assertTestResources(testSourceSetModuleName, "resources");
  }

  protected void assertDefaultGradleJavaProjectFoldersForMergedModule(@NotNull String moduleName) {
    assertContentRoots(moduleName, getProjectPath());
    boolean isDelegatedBuild = GradleProjectSettings.isDelegatedBuildEnabled(myProject, getProjectPath());
    String[] excludes = isDelegatedBuild ? new String[]{".gradle", "build"} : new String[]{".gradle", "build", "out"};
    assertExcludes(moduleName, excludes);
    assertSources(moduleName, "src/main/java");
    assertResources(moduleName, "src/main/resources");
    assertTestSources(moduleName, "src/test/java");
    assertTestResources(moduleName, "src/test/resources");
  }

  private void createDefaultDirs() throws IOException {
    createProjectSubFile("src/main/java/A.java");
    createProjectSubFile("src/test/java/A.java");
    createProjectSubFile("src/main/resources/resource.properties");
    createProjectSubFile("src/test/resources/test_resource.properties");
  }

  private void assertTestSources(Project project, String moduleName, String... expected) {
    final Module fooModule = getModule(project, moduleName);
    final ContentEntry[] contentRoots = ModuleRootManager.getInstance(fooModule).getContentEntries();
    String rootUrl = contentRoots.length > 1 ? ExternalSystemApiUtil.getExternalProjectPath(fooModule) : null;
    doAssertContentFolders(rootUrl, contentRoots, JavaSourceRootType.TEST_SOURCE, expected);
  }

  @Nullable
  protected ContentEntry findContentEntry(@NotNull String moduleName, @NotNull String contentPath) {
    ModuleRootManager moduleRootManager = getRootManager(moduleName);
    Module module = moduleRootManager.getModule();
    String rootPath = getAbsolutePath(ExternalSystemApiUtil.getExternalProjectPath(module));
    String expectedContentPath = getAbsolutePath(rootPath + "/" + contentPath);
    ContentEntry[] contentEntries = moduleRootManager.getContentEntries();
    for (ContentEntry contentEntry : contentEntries) {
      String actualContentPath = getAbsolutePath(contentEntry.getUrl());
      if (actualContentPath.equals(expectedContentPath)) return contentEntry;
    }
    return null;
  }

  protected void assertContentEntryExists(@NotNull String moduleName, String @NotNull ... contentPaths) {
    for (String contentPath : contentPaths) {
      ContentEntry contentEntry = findContentEntry(moduleName, contentPath);
      assertNotNull("Content entry " + contentPath + " not found in module " + moduleName, contentEntry);
    }
  }

  protected void assertSourceExists(@NotNull String moduleName, String @NotNull ... sourcePaths) {
    for (String sourcePath : sourcePaths) {
      SourceFolder sourceFolder = findSource(moduleName, sourcePath);
      assertNotNull("Source folder " + sourcePath + " not found in module " + moduleName, sourceFolder);
    }
  }

  protected void importPerSourceSet(boolean b) {
    getCurrentExternalProjectSettings().setResolveModulePerSourceSet(b);
  }

  protected void waitForModulesUpdate() throws Exception {
    SourceFolderManagerImpl instance = (SourceFolderManagerImpl)SourceFolderManager.getInstance(myProject);
    Future<?> state = instance.getBulkOperationState();
    assertNotNull(state);
    state.get(5, TimeUnit.SECONDS);
  }
}
