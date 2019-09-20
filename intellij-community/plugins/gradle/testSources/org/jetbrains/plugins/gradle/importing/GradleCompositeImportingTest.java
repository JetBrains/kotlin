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
package org.jetbrains.plugins.gradle.importing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.StdModuleTypes;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.junit.Test;

import static com.intellij.openapi.roots.DependencyScope.COMPILE;

/**
 * @author Vladislav.Soroka
 */
public class GradleCompositeImportingTest extends GradleImportingTestCase {
  @Test
  @TargetVersions("3.3+")
  public void testBasicCompositeBuild() throws Exception {
    createSettingsFile("rootProject.name='adhoc'\n" +
                       "\n" +
                       "includeBuild '../my-app'\n" +
                       "includeBuild '../my-utils'");

    createProjectSubFile("../my-app/settings.gradle", "rootProject.name = 'my-app'\n");
    createProjectSubFile("../my-app/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group 'org.sample'\n" +
                         "version '1.0'\n" +
                         "\n" +
                         "dependencies {\n" +
                         "  compile 'org.sample:number-utils:1.0'\n" +
                         "  compile 'org.sample:string-utils:1.0'\n" +
                         "}\n");

    createProjectSubFile("../my-utils/settings.gradle",
                         "rootProject.name = 'my-utils'\n" +
                         "include 'number-utils', 'string-utils' ");
    createProjectSubFile("../my-utils/build.gradle", injectRepo(
      "subprojects {\n" +
      "  apply plugin: 'java'\n" +
      "\n" +
      "  group 'org.sample'\n" +
      "  version '1.0'\n" +
      "}\n" +
      "\n" +
      "project(':string-utils') {\n" +
      "  dependencies {\n" +
      "    compile 'org.apache.commons:commons-lang3:3.4'\n" +
      "  }\n" +
      "} "));

    importProject();

    assertModules("adhoc",
                  "my-app", "my-app.main", "my-app.test",
                  "my-utils",
                  "my-utils.string-utils", "my-utils.string-utils.test", "my-utils.string-utils.main",
                  "my-utils.number-utils", "my-utils.number-utils.main", "my-utils.number-utils.test");

    String[] rootModules = new String[]{"adhoc", "my-app", "my-utils", "my-utils.string-utils", "my-utils.number-utils"};
    for (String rootModule : rootModules) {
      assertModuleLibDeps(rootModule);
      assertModuleModuleDeps(rootModule);
    }
    assertModuleModuleDeps("my-app.main", "my-utils.number-utils.main", "my-utils.string-utils.main");
    assertModuleModuleDepScope("my-app.main", "my-utils.number-utils.main", COMPILE);
    assertModuleModuleDepScope("my-app.main", "my-utils.string-utils.main", COMPILE);
    assertModuleLibDepScope("my-app.main", "Gradle: org.apache.commons:commons-lang3:3.4", COMPILE);
  }

  @Test
  @TargetVersions("3.3+")
  public void testCompositeBuildWithNestedModules() throws Exception {
    createSettingsFile("rootProject.name = 'app'\n" +
                       "includeBuild 'lib'");

    createProjectSubFile("lib/settings.gradle", "rootProject.name = 'lib'\n" +
                                                "include 'runtime'\n" +
                                                "include 'runtime:runtime-mod'");
    createProjectSubFile("lib/runtime/runtime-mod/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group = 'my.group'");

    importProject("apply plugin: 'java'\n" +
                  "dependencies {\n" +
                  "  compile 'my.group:runtime-mod'\n" +
                  "}");

    assertModules("app", "app.main", "app.test",
                  "lib",
                  "lib.runtime",
                  "lib.runtime.runtime-mod", "lib.runtime.runtime-mod.main", "lib.runtime.runtime-mod.test");

    assertModuleModuleDepScope("app.main", "lib.runtime.runtime-mod.main", COMPILE);
  }


  @Test
  @TargetVersions("3.3+")
  public void testCompositeBuildWithNestedModulesSingleModulePerProject() throws Exception {
    createSettingsFile("rootProject.name = 'app'\n" +
                       "includeBuild 'lib'");

    createProjectSubFile("lib/settings.gradle", "rootProject.name = 'lib'\n" +
                                                "include 'runtime'\n" +
                                                "include 'runtime:runtime-mod'");
    createProjectSubFile("lib/runtime/runtime-mod/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group = 'my.group'");

    importProjectUsingSingeModulePerGradleProject("apply plugin: 'java'\n" +
                                                  "dependencies {\n" +
                                                  "  compile 'my.group:runtime-mod'\n" +
                                                  "}");

    assertModules("app",
                  "lib",
                  "lib.runtime",
                  "lib.runtime.runtime-mod");

    assertMergedModuleCompileModuleDepScope("app", "lib.runtime.runtime-mod");
  }


  @Test
  @TargetVersions("4.0+")
  public void testCompositeBuildWithGradleProjectDuplicates() throws Exception {
    createSettingsFile("rootProject.name = 'app'\n" +
                       "include 'runtime'\n" +
                       "includeBuild 'lib1'\n" +
                       "includeBuild 'lib2'");

    createProjectSubFile("runtime/build.gradle",
                         "apply plugin: 'java'");


    createProjectSubFile("lib1/settings.gradle", "rootProject.name = 'lib1'\n" +
                                                 "include 'runtime'");
    createProjectSubFile("lib1/runtime/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group = 'my.group.lib_1'");


    createProjectSubFile("lib2/settings.gradle", "rootProject.name = 'lib2'\n" +
                                                 "include 'runtime'");
    createProjectSubFile("lib2/runtime/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group = 'my.group.lib_2'");


    importProjectUsingSingeModulePerGradleProject("apply plugin: 'java'\n" +
                                                  "dependencies {\n" +
                                                  "  compile project(':runtime')\n" +
                                                  "  compile 'my.group.lib_1:runtime'\n" +
                                                  "  compile 'my.group.lib_2:runtime'\n" +
                                                  "}");

    assertModules("app", "app.runtime",
                  "lib1", "lib1.runtime",
                  "lib2", "lib2.runtime");

    assertMergedModuleCompileModuleDepScope("app", "app.runtime");
    assertMergedModuleCompileModuleDepScope("app", "lib1.runtime");
    assertMergedModuleCompileModuleDepScope("app", "lib2.runtime");
  }


  @Test
  @TargetVersions("3.3+")
  public void testCompositeBuildWithGradleProjectDuplicatesModulePerSourceSet() throws Exception {
    createSettingsFile("rootProject.name = 'app'\n" +
                       "include 'runtime'\n" +
                       "includeBuild 'lib1'\n" +
                       "includeBuild 'lib2'");

    createProjectSubFile("runtime/build.gradle",
                         "apply plugin: 'java'");


    createProjectSubFile("lib1/settings.gradle", "rootProject.name = 'lib1'\n" +
                                                 "include 'runtime'");
    createProjectSubFile("lib1/runtime/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group = 'my.group.lib_1'");


    createProjectSubFile("lib2/settings.gradle", "rootProject.name = 'lib2'\n" +
                                                 "include 'runtime'");
    createProjectSubFile("lib2/runtime/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group = 'my.group.lib_2'");

    // check for non-qualified module names
    getCurrentExternalProjectSettings().setUseQualifiedModuleNames(false);
    importProject("apply plugin: 'java'\n" +
                  "dependencies {\n" +
                  "  compile project(':runtime')\n" +
                  "  compile 'my.group.lib_1:runtime'\n" +
                  "  compile 'my.group.lib_2:runtime'\n" +
                  "}");

    if (isGradle40orNewer()) {
      assertModules("app", "app_main", "app_test",
                    "app-runtime", "app-runtime_main", "app-runtime_test",
                    "lib1", "lib1-runtime", "lib1-runtime_main", "lib1-runtime_test",
                    "lib2", "lib2-runtime", "lib2-runtime_main", "lib2-runtime_test");
    }
    else {
      assertModules("app", "app_main", "app_test",
                    "runtime", "runtime_main", "runtime_test",
                    "lib1", "my.group.lib_1-runtime", "my.group.lib_1-runtime_main", "my.group.lib_1-runtime_test",
                    "lib2", "my.group.lib_2-runtime", "my.group.lib_2-runtime_main", "my.group.lib_2-runtime_test");
    }

    if (isGradle40orNewer()) {
      assertModuleModuleDepScope("app_main", "app-runtime_main", COMPILE);
      assertModuleModuleDepScope("app_main", "lib1-runtime_main", COMPILE);
      assertModuleModuleDepScope("app_main", "lib2-runtime_main", COMPILE);
    }
    else {
      assertModuleModuleDepScope("app_main", "runtime_main", COMPILE);
      assertModuleModuleDepScope("app_main", "my.group.lib_1-runtime_main", COMPILE);
      assertModuleModuleDepScope("app_main", "my.group.lib_2-runtime_main", COMPILE);
    }
  }


  @Test
  @TargetVersions("3.3+")
  public void testCompositeBuildWithProjectNameDuplicates() throws Exception {
    IdeModifiableModelsProvider modelsProvider = new IdeModifiableModelsProviderImpl(myProject);
    modelsProvider.newModule(getProjectPath() + "/api.iml", StdModuleTypes.JAVA.getId());
    modelsProvider.newModule(getProjectPath() + "/api_main.iml", StdModuleTypes.JAVA.getId());
    modelsProvider.newModule(getProjectPath() + "/my-app-api.iml", StdModuleTypes.JAVA.getId());
    modelsProvider.newModule(getProjectPath() + "/my-app-api_main.iml", StdModuleTypes.JAVA.getId());
    modelsProvider.newModule(getProjectPath() + "/my-utils-api.iml", StdModuleTypes.JAVA.getId());
    modelsProvider.newModule(getProjectPath() + "/my-utils-api_main.iml", StdModuleTypes.JAVA.getId());
    edt(() -> ApplicationManager.getApplication().runWriteAction(modelsProvider::commit));

    createSettingsFile("rootProject.name='adhoc'\n" +
                       "\n" +
                       "includeBuild '../my-app'\n" +
                       "includeBuild '../my-utils'");

    createProjectSubFile("../my-app/settings.gradle", "rootProject.name = 'my-app'\n" +
                                                      "include 'api'\n");
    createProjectSubFile("../my-app/build.gradle",
                         "apply plugin: 'java'\n" +
                         "group 'org.sample'\n" +
                         "version '1.0'\n" +
                         "\n" +
                         "dependencies {\n" +
                         "  compile 'org.sample:number-utils:1.0'\n" +
                         "  compile 'org.sample:string-utils:1.0'\n" +
                         "}\n" +
                         "project(':api') {\n" +
                         "  apply plugin: 'java'\n" +
                         "  dependencies {\n" +
                         "    compile 'commons-lang:commons-lang:2.6'\n" +
                         "  }\n" +
                         "}\n");

    createProjectSubFile("../my-utils/settings.gradle",
                         "rootProject.name = 'my-utils'\n" +
                         "include 'number-utils', 'string-utils', 'api'");
    createProjectSubFile("../my-utils/build.gradle", injectRepo(
      "subprojects {\n" +
      "  apply plugin: 'java'\n" +
      "\n" +
      "  group 'org.sample'\n" +
      "  version '1.0'\n" +
      "}\n" +
      "\n" +
      "project(':string-utils') {\n" +
      "  dependencies {\n" +
      "    compile 'org.apache.commons:commons-lang3:3.4'\n" +
      "  }\n" +
      "}\n" +
      "project(':api') {\n" +
      "  dependencies {\n" +
      "    compile 'junit:junit:4.11'\n" +
      "  }\n" +
      "}"));

    // check for non-qualified module names
    getCurrentExternalProjectSettings().setUseQualifiedModuleNames(false);
    importProject();

    String myAppApiModuleName = myTestDir.getName() + "-my-app-api";
    String myAppApiMainModuleName = myTestDir.getName() + "-my-app-api_main";
    String myUtilsApiMainModuleName = isGradle40orNewer() ? "org.sample-my-utils-api_main" : "org.sample-api_main";
    if (isGradle40orNewer()) {
      assertModules(
        // non-gradle modules
        "api", "api_main", "my-app-api", "my-app-api_main", "my-utils-api", "my-utils-api_main",
        // generated modules by gradle import
        "adhoc",
        "my-app", "my-app_main", "my-app_test",
        myAppApiModuleName, myAppApiMainModuleName, "my-app-api_test",
        "my-utils",
        "org.sample-my-utils-api", myUtilsApiMainModuleName, "my-utils-api_test",
        "string-utils", "string-utils_main", "string-utils_test",
        "number-utils", "number-utils_main", "number-utils_test"
      );
    }
    else {
      assertModules(
        // non-gradle modules
        "api", "api_main", "my-app-api", "my-app-api_main", "my-utils-api", "my-utils-api_main",
        // generated modules by gradle import
        "adhoc",
        "my-app", "my-app_main", "my-app_test",
        myAppApiModuleName, myAppApiMainModuleName, "org.sample-api_test",
        "my-utils",
        "org.sample-api", myUtilsApiMainModuleName, "api_test",
        "string-utils", "string-utils_main", "string-utils_test",
        "number-utils", "number-utils_main", "number-utils_test"
      );
    }

    String[] emptyModules =
      new String[]{
        // non-gradle modules
        "api", "api_main", "my-app-api", "my-app-api_main", "my-utils-api", "my-utils-api_main",
        // generated modules by gradle import
        "adhoc", "my-app", myAppApiModuleName, "my-utils", "string-utils", "number-utils"};
    for (String rootModule : emptyModules) {
      assertModuleLibDeps(rootModule);
      assertModuleModuleDeps(rootModule);
    }
    assertModuleModuleDeps("my-app_main", "number-utils_main", "string-utils_main");
    assertModuleModuleDepScope("my-app_main", "number-utils_main", COMPILE);
    assertModuleModuleDepScope("my-app_main", "string-utils_main", COMPILE);
    assertModuleLibDepScope("my-app_main", "Gradle: org.apache.commons:commons-lang3:3.4", COMPILE);

    // my-app api project
    assertModuleModuleDeps(myAppApiMainModuleName);
    assertModuleLibDeps(myAppApiMainModuleName, "Gradle: commons-lang:commons-lang:2.6");
    assertModuleLibDepScope(myAppApiMainModuleName, "Gradle: commons-lang:commons-lang:2.6", COMPILE);

    assertModuleModuleDeps(myUtilsApiMainModuleName);
    //assertModuleLibDeps("my-utils-api_main", "Gradle: junit:junit:4.11");
    assertModuleLibDepScope(myUtilsApiMainModuleName, "Gradle: junit:junit:4.11", COMPILE);
    //assertModuleLibDepScope("my-utils-api_main", "Gradle: org.hamcrest:hamcrest-core:1.3", COMPILE);
  }

  @Test
  @TargetVersions("3.3+")
  public void testApiDependenciesAreImported() throws Exception {
    createSettingsFile("rootProject.name = \"project-b\"\n" +
                       "includeBuild 'project-a'");

    createProjectSubFile("project-a/settings.gradle",
                                                      "rootProject.name = \"project-a\"\n" +
                                                      "include 'core', 'ext'");

    createProjectSubFile("project-a/core/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .withKotlinPlugin("1.3.0")
                           .addRepository(" maven { url 'http://maven.labs.intellij.net/repo1' }")
                           .addPrefix("apply plugin: 'java-library'").generate());

    createProjectSubFile("project-a/ext/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .withKotlinPlugin("1.3.0")
                           .addRepository(" maven { url 'http://maven.labs.intellij.net/repo1' }")
                           .addPrefix(
                             "apply plugin: 'java-library'",
                             "group = 'myGroup.projectA'",
                             "version = '1.0-SNAPSHOT'",
                             "dependencies {",
                             " api project(':core')",
                             "}"
                         ).generate());

    createProjectSubFile("project-a/build.gradle", "");

    importProject(new GradleBuildScriptBuilderEx()
                    .addPostfix("apply plugin: 'java-library'",
                                "group = 'myGroup'",
                                "version = '1.0-SNAPSHOT'",
                                "dependencies {",
                                "    api group: 'myGroup.projectA', name: 'ext', version: '1.0-SNAPSHOT'",
                                "}"
                    )
                    .generate());

    assertModules("project-a",
                  "project-a.core", "project-a.core.main", "project-a.core.test",
                  "project-a.ext", "project-a.ext.main", "project-a.ext.test",
                  "project-b", "project-b.main", "project-b.test");

    assertModuleModuleDeps("project-b.main", "project-a.ext.main", "project-a.core.main");
  }


  @Test
  // todo should this be fixed for Gradle versions [3.1, 4.9)?
  @TargetVersions("4.9+")
  public void testTransitiveSourceSetDependenciesAreImported() throws Exception {
    createSettingsFile("rootProject.name = \"project-b\"\n" +
                       "includeBuild 'project-a'");

    createProjectSubFile("project-a/settings.gradle", "rootProject.name = \"project-a\"");
    createProjectSubFile("project-a/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .withIdeaPlugin()
                           .withJavaPlugin()
                           .addPostfix(
                             "group = 'myGroup'",
                             "version = '1.0-SNAPSHOT'",
                             "sourceSets {",
                             "    util {",
                             "        java.srcDir 'src/util/java'",
                             "        resources.srcDir 'src/util/resources'",
                             "    }",
                             "}",
                             "configurations {",
                             "  compile {",
                             "    extendsFrom utilCompile",
                             "  }",
                             "}",
                             "dependencies {",
                             "   compile sourceSets.util.output",
                             "}",
                             "jar {",
                             "  from sourceSets.util.output",
                             "}",
                             "compileJava {",
                             "    dependsOn(compileUtilJava)",
                             "}").generate());
    createProjectSubFile("project-a/src/main/java/my/pack/Clazz.java", "package my.pack; public class Clazz{};");
    createProjectSubFile("project-a/src/main/util/my/pack/Util.java", "package my.pack; public class Util{};");

    createProjectSubFile("src/main/java/my/pack/ClazzB.java", "package my.pack; public class CLazzB{};");
    importProject(new GradleBuildScriptBuilderEx()
                    .withIdeaPlugin()
                    .withJavaPlugin()
                    .addPostfix("group = 'myGroup'",
                                "version = '1.0-SNAPSHOT'",
                                "dependencies {",
                                "    compile group: 'myGroup', name: 'project-a', version: '1.0-SNAPSHOT'",
                                "}"
                                )
                    .generate());

    assertModules("project-a",
                  "project-a.main", "project-a.test", "project-a.util",
                  "project-b", "project-b.main", "project-b.test");

    assertModuleModuleDeps("project-b.main", "project-a.util", "project-a.main");
  }

  @Test
  @TargetVersions("4.4+")
  public void testProjectWithCompositePluginDependencyImported() throws Exception {
    createSettingsFile("includeBuild('plugin'); includeBuild('consumer')");
    createProjectSubFile("plugin/settings.gradle", "rootProject.name = 'test-plugin'");
    createProjectSubFile("plugin/build.gradle", new GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .addPrefix("group = 'myGroup'",
                 "version = '1.0'")
      .generate());

    // consumer need to be complicated to display the issue
    createProjectSubFile("consumer/settings.gradle",
                         "pluginManagement {\n" +
                         "  resolutionStrategy {\n" +
                         "    eachPlugin {\n" +
                         "      println \"resolving ${requested.id.id} dependency\"\n" +
                         "      if(requested.id.id == \"test-plugin\") {\n" +
                         "        useModule(\"myGroup:test-plugin:1.0\")\n" +
                         "      }\n" +
                         "    }\n" +
                         "  }\n" +
                         "}\n"
                         + "include 'library'");
    createProjectSubFile("consumer/build.gradle", new GradleBuildScriptBuilderEx()
      .addPostfix(
        "plugins {",
        " id 'test-plugin' apply false",
        "}",
        "subprojects {",
        "  apply plugin: 'java'",
        "}"
      )
      .generate());
    // sourceSets here will fail to evaluate if parent project was not evaluated successfully
    // because of missing test-plugin, caused by bad included build evaluation order.
    createProjectSubFile("consumer/library/build.gradle", new GradleBuildScriptBuilderEx()
      .addPostfix(
        "sourceSets {",
        "  integrationTest ",
        "}"
      )
      .generate());

    importProject("");

    assertModules("project",
                  "test-plugin", "test-plugin.main", "test-plugin.test",
                  "consumer", "consumer.library", "consumer.library.main", "consumer.library.test", "consumer.library.integrationTest");
  }



  @Test
  @TargetVersions("3.1+")
  public void testSubstituteDependencyWithRootProject() throws Exception {
    createSettingsFile("rootProject.name = \"root-project\"\n" +
                       "include 'sub-project'\n" +
                       "includeBuild('included-project') { dependencySubstitution { substitute module('my.grp:myId') with project(':') } }");


    createProjectSubFile("sub-project/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .withJavaPlugin()
                           .addDependency("implementation 'my.grp:myId:1.0'")
                           .generate());

    createProjectSubFile("included-project/settings.gradle", "rootProject.name = 'myId'");
    createProjectSubFile("included-project/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .withJavaPlugin()
                           .group("my.grp")
                           .version("1.0")
                           .generate());

    importProject("");

    assertModules("root-project",
                  "root-project.sub-project", "root-project.sub-project.main", "root-project.sub-project.test",
                  "myId", "myId.main", "myId.test");

    assertModuleModuleDeps("root-project.sub-project.main", "myId.main");
  }

  @Test
  @TargetVersions("3.1+")
  public void testScopeUpdateForSubstituteDependency() throws Exception {
    createSettingsFile("rootProject.name = 'pA'\n" +
                       "include 'pA-1', 'pA-2'\n" +
                       "includeBuild('pB')\n" +
                       "includeBuild('pC')");

    createProjectSubFile("pB/settings.gradle");
    createProjectSubFile("pC/settings.gradle");

    createProjectSubFile("pA-1/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .applyPlugin("'java-library'")
                           .addDependency("implementation 'group:pC'")
                           .generate());

    createProjectSubFile("pA-2/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .applyPlugin("'java-library'")
                           .addDependency("implementation project(':pA-1')")
                           .addDependency("implementation 'group:pB'")
                           .generate());

    createProjectSubFile("pB/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .addPostfix("group = 'group'")
                           .applyPlugin("'java-library'")
                           .addDependency("api 'group:pC'")
                           .generate());

    createProjectSubFile("pC/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .addPostfix("group = 'group'")
                           .applyPlugin("'java-library'")
                           .generate());

    //enableGradleDebugWithSuspend();
    importProject("");

    assertModules("pA",
                  "pA.pA-1", "pA.pA-1.main", "pA.pA-1.test",
                  "pA.pA-2", "pA.pA-2.main", "pA.pA-2.test",
                  "pB", "pB.main", "pB.test",
                  "pC", "pC.main", "pC.test");

    assertModuleModuleDepScope("pA.pA-2.main", "pC.main", COMPILE);
  }
}
