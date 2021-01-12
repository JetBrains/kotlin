// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.PathUtil;
import org.gradle.util.GradleVersion;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*;
import static com.intellij.openapi.util.text.StringUtil.*;
import static com.intellij.util.containers.ContainerUtil.ar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getSourceSetName;

/**
 * @author Vladislav.Soroka
 */
public class GradleDependenciesImportingTest extends GradleImportingTestCase {

  @Override
  protected void importProject(@NonNls @Language("Groovy") String config) throws IOException {
    config += "\n" +
              "allprojects {\n" +
              "  afterEvaluate {\n" +
              "    if(convention.findPlugin(JavaPluginConvention)) {\n" +
              "      sourceSets.each { SourceSet sourceSet ->\n" +
              "        tasks.create(name: 'print'+ sourceSet.name.capitalize() +'CompileDependencies') {\n" +
              "          doLast { println sourceSet.compileClasspath.files.collect {it.name}.join(' ') }\n" +
              "        }\n" +
              "      }\n" +
              "    }\n" +
              "  }\n" +
              "}\n";
    super.importProject(config);
  }

  protected void assertCompileClasspathOrdering(String moduleName) {
    Module module = getModule(moduleName);
    String sourceSetName = getSourceSetName(module);
    assertNotNull("Can not find the sourceSet for the module", sourceSetName);

    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalProjectPath(getExternalProjectPath(module));
    String id = getExternalProjectId(module);
    String gradlePath = id.startsWith(":") ? trimEnd(trimEnd(id, sourceSetName), ":") : "";
    settings.setTaskNames(Collections.singletonList(gradlePath + ":print" + capitalize(sourceSetName) + "CompileDependencies"));
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
    settings.setScriptParameters("--quiet");
    ExternalSystemProgressNotificationManager notificationManager =
      ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    StringBuilder gradleClasspath = new StringBuilder();
    ExternalSystemTaskNotificationListenerAdapter listener = new ExternalSystemTaskNotificationListenerAdapter() {
      @Override
      public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
        gradleClasspath.append(text);
      }
    };
    notificationManager.addNotificationListener(listener);
    try {
      ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, myProject, GradleConstants.SYSTEM_ID, null,
                                 ProgressExecutionMode.NO_PROGRESS_SYNC);
    }
    finally {
      notificationManager.removeNotificationListener(listener);
    }

    List<String> ideClasspath = new ArrayList<>();
    ModuleRootManager.getInstance(module).orderEntries().withoutSdk().withoutModuleSourceEntries().compileOnly().productionOnly().forEach(
      entry -> {
        if (entry instanceof ModuleOrderEntry) {
          Module moduleDep = ((ModuleOrderEntry)entry).getModule();
          String sourceSetDepName = getSourceSetName(moduleDep);
          // for simplicity, only project dependency on 'default' configuration allowed here
          assert sourceSetDepName != "main";

          String gradleProjectDepName = trimStart(trimEnd(getExternalProjectId(moduleDep), ":main"), ":");
          String version = getExternalProjectVersion(moduleDep);
          version = "unspecified".equals(version) ? "" : "-" + version;
          ideClasspath.add(gradleProjectDepName + version + ".jar");
        }
        else {
          ideClasspath.add(entry.getFiles(OrderRootType.CLASSES)[0].getName());
        }
        return true;
      });

    assertEquals(join(ideClasspath, " "), gradleClasspath.toString().trim());
  }

  @Test
  public void testDependencyScopeMerge() throws Exception {
    createSettingsFile("include 'api', 'impl' ");

    importProject(
      "allprojects {\n" +
      "  apply plugin: 'java'\n" +
      "\n" +
      "  sourceCompatibility = 1.5\n" +
      "  version = '1.0'\n" +
      "}\n" +
      "\n" +
      "dependencies {\n" +
      "  compile project(':api')\n" +
      "  testCompile project(':impl'), 'junit:junit:4.11'\n" +
      "  runtime project(':impl')\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test",
                  "project.api", "project.api.main", "project.api.test",
                  "project.impl", "project.impl.main", "project.impl.test");
    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.api.test", "project.api.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.impl.test", "project.impl.main", DependencyScope.COMPILE);

    assertModuleModuleDepScope("project.main", "project.api.main", DependencyScope.COMPILE);

    assertModuleModuleDepScope("project.main", "project.impl.main", DependencyScope.RUNTIME);
    assertModuleModuleDepScope("project.test", "project.impl.main", DependencyScope.COMPILE);

    assertModuleLibDepScope("project.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);

    assertCompileClasspathOrdering("project.main");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.api", "project.impl");

    if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("1.12")) < 0) {
      assertModuleModuleDepScope("project", "project.impl", DependencyScope.RUNTIME);
    }
    else {
      assertModuleModuleDepScope("project", "project.impl", DependencyScope.RUNTIME, DependencyScope.TEST);
    }

    assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.TEST);
    assertModuleLibDepScope("project", "Gradle: junit:junit:4.11", DependencyScope.TEST);
  }

  @Test
  @TargetVersions("2.0+")
  public void testTransitiveNonTransitiveDependencyScopeMerge() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "project(':project1') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile 'junit:junit:4.11'\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies.ext.strict = { projectPath ->\n" +
      "    dependencies.compile dependencies.project(path: projectPath, transitive: false)\n" +
      "    dependencies.runtime dependencies.project(path: projectPath, transitive: true)\n" +
      "    dependencies.testRuntime dependencies.project(path: projectPath, transitive: true)\n" +
      "  }\n" +
      "\n" +
      "  dependencies {\n" +
      "    strict ':project1'\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDeps("project.project2.main", "project.project1.main");
    assertModuleModuleDepScope("project.project2.main", "project.project1.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.RUNTIME);
    assertModuleLibDepScope("project.project2.main", "Gradle: junit:junit:4.11", DependencyScope.RUNTIME);

    if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("2.5")) >= 0) {
      boolean gradleOlderThen_3_4 = isGradleOlderThen("3.4");
      importProjectUsingSingeModulePerGradleProject();
      assertModules("project", "project.project1", "project.project2");
      assertMergedModuleCompileModuleDepScope("project.project2", "project.project1");
      assertModuleLibDepScope("project.project2", "Gradle: org.hamcrest:hamcrest-core:1.3",
                              gradleOlderThen_3_4 ? ar(DependencyScope.RUNTIME)
                                                  : ar(DependencyScope.RUNTIME, DependencyScope.TEST));
      assertModuleLibDepScope("project.project2", "Gradle: junit:junit:4.11",
                              gradleOlderThen_3_4 ? ar(DependencyScope.RUNTIME)
                                                  : ar(DependencyScope.RUNTIME, DependencyScope.TEST));
    }
  }

  @Test
  @TargetVersions("2.0+")
  public void testProvidedDependencyScopeMerge() throws Exception {
    createSettingsFile("include 'web'\n" +
                       "include 'user'");

    importProject(
      "subprojects {\n" +
      "  apply plugin: 'java'\n" +
      "  configurations {\n" +
      "    provided\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':web') {\n" +
      "  dependencies {\n" +
      "    provided 'junit:junit:4.11'\n" +
      "  }\n" +
      "}\n" +
      "project(':user') {\n" +
      "  apply plugin: 'war'\n" +
      "  dependencies {\n" +
      "    compile project(':web')\n" +
      "    providedCompile project(path: ':web', configuration: 'provided')\n" +
      "  }\n" +
      "}"
    );

    assertModules("project",
                  "project.web", "project.web.main", "project.web.test",
                  "project.user", "project.user.main", "project.user.test");

    assertModuleLibDeps("project.web");
    assertModuleLibDeps("project.web.main");
    assertModuleLibDeps("project.web.test");

    assertModuleModuleDeps("project.user.main", "project.web.main");
    assertModuleModuleDepScope("project.user.main", "project.web.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.user.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.PROVIDED);
    assertModuleLibDepScope("project.user.main", "Gradle: junit:junit:4.11", DependencyScope.PROVIDED);

    createProjectSubDirs("web", "user");
    assertCompileClasspathOrdering("project.user.main");
  }

  @Test
  public void testCustomSourceSetsDependencies() throws Exception {
    createSettingsFile("include 'api', 'impl' ");

    importProject(
      "allprojects {\n" +
      "  apply plugin: 'java'\n" +
      "\n" +
      "  sourceCompatibility = 1.5\n" +
      "  version = '1.0'\n" +
      "}\n" +
      "\n" +
      "project(\"impl\") {\n" +
      "  sourceSets {\n" +
      "    myCustomSourceSet\n" +
      "    myAnotherSourceSet\n" +
      "  }\n" +
      "  \n" +
      "  dependencies {\n" +
      "    myCustomSourceSetCompile sourceSets.main.output\n" +
      "    myCustomSourceSetCompile project(\":api\")\n" +
      "    myCustomSourceSetRuntime 'junit:junit:4.11'\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test",
                  "project.api", "project.api.main", "project.api.test",
                  "project.impl", "project.impl.main", "project.impl.test",
                  "project.impl.myCustomSourceSet", "project.impl.myAnotherSourceSet");

    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.api.test", "project.api.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.impl.test", "project.impl.main", DependencyScope.COMPILE);

    assertModuleModuleDepScope("project.impl.myCustomSourceSet", "project.impl.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.impl.myCustomSourceSet", "project.api.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.impl.myCustomSourceSet", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.RUNTIME);
    assertModuleLibDepScope("project.impl.myCustomSourceSet", "Gradle: junit:junit:4.11", DependencyScope.RUNTIME);
  }

  @Test
  public void testDependencyWithDifferentClassifiers() throws Exception {
    final VirtualFile depJar = createProjectJarSubFile("lib/dep/dep/1.0/dep-1.0.jar");
    final VirtualFile depTestsJar = createProjectJarSubFile("lib/dep/dep/1.0/dep-1.0-tests.jar");
    final VirtualFile depNonJar = createProjectSubFile("lib/dep/dep/1.0/dep-1.0.someExt");

    createProjectSubFile("lib/dep/dep/1.0/dep-1.0.pom", "" +
                                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                        "<project\n" +
                                                        "  xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                                                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                                        "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                                                        "  <groupId>dep</groupId>\n" +
                                                        "  <artifactId>dep</artifactId>\n" +
                                                        "  <version>1.0</version>\n" +
                                                        "\n" +
                                                        "</project>\n");
    importProject(
      "allprojects {\n" +
      "  apply plugin: 'java'\n" +
      "  sourceCompatibility = 1.5\n" +
      "  version = '1.0'\n" +
      "\n" +
      "  repositories {\n" +
      "    maven{ url file('lib') }\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "dependencies {\n" +
      "  compile 'dep:dep:1.0'\n" +
      "  testCompile 'dep:dep:1.0:tests'\n" +
      "  runtime 'dep:dep:1.0@someExt'\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");

    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);

    final String depName = "Gradle: dep:dep:1.0";
    assertModuleLibDep("project.main", depName, depJar.getUrl());
    assertModuleLibDepScope("project.main", depName, DependencyScope.COMPILE);
    assertModuleLibDep("project.test", depName, depJar.getUrl());
    assertModuleLibDepScope("project.test", depName, DependencyScope.COMPILE);

    final boolean isArtifactResolutionQuerySupported = GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("2.0")) >= 0;
    final String depTestsName =
      isArtifactResolutionQuerySupported ? "Gradle: dep:dep:tests:1.0" : PathUtil.toPresentableUrl(depTestsJar.getUrl());
    assertModuleLibDep("project.test", depTestsName, depTestsJar.getUrl());
    assertModuleLibDepScope("project.test", depTestsName, DependencyScope.COMPILE);

    final String depNonJarName =
      isArtifactResolutionQuerySupported ? "Gradle: dep:dep:someExt:1.0" : PathUtil.toPresentableUrl(depNonJar.getUrl());
    assertModuleLibDep("project.main", depNonJarName, depNonJar.getUrl());
    assertModuleLibDepScope("project.main", depNonJarName, DependencyScope.RUNTIME);
    assertModuleLibDep("project.test", depNonJarName, depNonJar.getUrl());
    assertModuleLibDepScope("project.test", depNonJarName, DependencyScope.RUNTIME);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");

    assertModuleLibDep("project", depName, depJar.getUrl());
    assertMergedModuleCompileLibDepScope("project", depName);

    assertModuleLibDep("project", "Gradle: dep:dep:1.0:tests", depTestsJar.getUrl());
    assertModuleLibDepScope("project", "Gradle: dep:dep:1.0:tests", DependencyScope.TEST);

    assertModuleLibDep("project", "Gradle: dep:dep:1.0:someExt", depNonJar.getUrl());
    if (isGradleOlderThen("3.4")) {
      assertModuleLibDepScope("project", "Gradle: dep:dep:1.0:someExt", DependencyScope.RUNTIME);
    }
    else {
      assertModuleLibDepScope("project", "Gradle: dep:dep:1.0:someExt", DependencyScope.RUNTIME, DependencyScope.TEST);
    }
  }


  @Test
  public void testGlobalFileDepsImportedAsProjectLibraries() throws Exception {
    final VirtualFile depJar = createProjectJarSubFile("lib/dep.jar");
    final VirtualFile dep2Jar = createProjectJarSubFile("lib_other/dep.jar");
    createSettingsFile("include 'p1'\n" +
                       "include 'p2'");

    importProjectUsingSingeModulePerGradleProject("allprojects {\n" +
                                                  "apply plugin: 'java'\n" +
                                                  "  dependencies {\n" +
                                                  "     compile rootProject.files('lib/dep.jar', 'lib_other/dep.jar')\n" +
                                                  "  }\n" +
                                                  "}");

    assertModules("project", "project.p1", "project.p2");
    Set<Library> libs = new HashSet<>();
    final List<LibraryOrderEntry> moduleLibDeps = getModuleLibDeps("project.p1", "Gradle: dep");
    moduleLibDeps.addAll(getModuleLibDeps("project.p1", "Gradle: dep_1"));
    moduleLibDeps.addAll(getModuleLibDeps("project.p2", "Gradle: dep"));
    moduleLibDeps.addAll(getModuleLibDeps("project.p2", "Gradle: dep_1"));
    for (LibraryOrderEntry libDep : moduleLibDeps) {
      libs.add(libDep.getLibrary());
      assertFalse("Dependency be project level: " + libDep.toString(), libDep.isModuleLevel());
    }

    assertProjectLibraries("Gradle: dep", "Gradle: dep_1");
    assertEquals("No duplicates of libraries are expected", 2, libs.size());
    assertContain(libs.stream().map(l -> l.getUrls(OrderRootType.CLASSES)[0]).collect(Collectors.toList()),
                  depJar.getUrl(), dep2Jar.getUrl());
  }

  @Test
  public void testLocalFileDepsImportedAsModuleLibraries() throws Exception {
    final VirtualFile depP1Jar = createProjectJarSubFile("p1/lib/dep.jar");
    final VirtualFile depP2Jar = createProjectJarSubFile("p2/lib/dep.jar");
    createSettingsFile("include 'p1'\n" +
                       "include 'p2'");

    importProjectUsingSingeModulePerGradleProject("allprojects { p ->\n" +
                                                  "apply plugin: 'java'\n" +
                                                  "  dependencies {\n" +
                                                  "     compile p.files('lib/dep.jar')\n" +
                                                  "  }\n" +
                                                  "}");

    assertModules("project", "project.p1", "project.p2");

    final List<LibraryOrderEntry> moduleLibDepsP1 = getModuleLibDeps("project.p1", "Gradle: dep");
    final boolean isGradleNewerThen_2_4 = GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("2.4")) > 0;
    for (LibraryOrderEntry libDep : moduleLibDepsP1) {
      assertEquals("Dependency must be " + (isGradleNewerThen_2_4 ? "module" : "project") + " level: " + libDep.toString(),
                   isGradleNewerThen_2_4, libDep.isModuleLevel());
      assertEquals("Wrong library dependency", depP1Jar.getUrl(), libDep.getLibrary().getUrls(OrderRootType.CLASSES)[0]);
    }

    final List<LibraryOrderEntry> moduleLibDepsP2 = getModuleLibDeps("project.p2", "Gradle: dep");
    for (LibraryOrderEntry libDep : moduleLibDepsP2) {
      assertEquals("Dependency must be " + (isGradleNewerThen_2_4 ? "module" : "project") + " level: " + libDep.toString(),
                   isGradleNewerThen_2_4, libDep.isModuleLevel());
      assertEquals("Wrong library dependency", depP2Jar.getUrl(), libDep.getLibrary().getUrls(OrderRootType.CLASSES)[0]);
    }
  }

  @Test
  public void testProjectWithUnresolvedDependency() throws Exception {
    final VirtualFile depJar = createProjectJarSubFile("lib/dep/dep/1.0/dep-1.0.jar");
    createProjectSubFile("lib/dep/dep/1.0/dep-1.0.pom", "" +
                                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                        "<project\n" +
                                                        "  xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                                                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                                        "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                                                        "  <groupId>dep</groupId>\n" +
                                                        "  <artifactId>dep</artifactId>\n" +
                                                        "  <version>1.0</version>\n" +
                                                        "\n" +
                                                        "</project>\n");
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "repositories {\n" +
      "  maven { url file('lib') }\n" +
      "}\n" +
      "dependencies {\n" +
      "  compile 'dep:dep:1.0'\n" +
      "  compile 'some:unresolvable-lib:0.1'\n" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test");

    final String depName = "Gradle: dep:dep:1.0";
    assertModuleLibDep("project.main", depName, depJar.getUrl());
    assertModuleLibDepScope("project.main", depName, DependencyScope.COMPILE);
    assertModuleLibDepScope("project.main", "Gradle: some:unresolvable-lib:0.1", DependencyScope.COMPILE);

    List<LibraryOrderEntry> unresolvableDep = getModuleLibDeps("project.main", "Gradle: some:unresolvable-lib:0.1");
    assertEquals(1, unresolvableDep.size());
    LibraryOrderEntry unresolvableEntry = unresolvableDep.iterator().next();
    assertEquals(DependencyScope.COMPILE, unresolvableEntry.getScope());
    String[] unresolvableEntryUrls = unresolvableEntry.getUrls(OrderRootType.CLASSES);
    assertEquals(1, unresolvableEntryUrls.length);
    assertUnresolvedEntryUrl(unresolvableEntryUrls[0], "some:unresolvable-lib:0.1");

    assertModuleLibDep("project.test", depName, depJar.getUrl());
    assertModuleLibDepScope("project.test", depName, DependencyScope.COMPILE);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");

    assertModuleLibDep("project", depName, depJar.getUrl());
    assertMergedModuleCompileLibDepScope("project", depName);
    assertMergedModuleCompileLibDepScope("project", "Gradle: some:unresolvable-lib:0.1");

    unresolvableDep = getModuleLibDeps("project", "Gradle: some:unresolvable-lib:0.1");
    if (isGradleOlderThen("3.4") || isGradleNewerThen("4.5")) {
      assertEquals(1, unresolvableDep.size());
      unresolvableEntry = unresolvableDep.iterator().next();
      assertTrue(unresolvableEntry.isModuleLevel());
      assertEquals(DependencyScope.COMPILE, unresolvableEntry.getScope());
    }
    else {
      assertEquals(3, unresolvableDep.size());
      unresolvableEntry = unresolvableDep.iterator().next();
      assertTrue(unresolvableEntry.isModuleLevel());
    }
    unresolvableEntryUrls = unresolvableEntry.getUrls(OrderRootType.CLASSES);
    assertEquals(0, unresolvableEntryUrls.length);
  }

  private static void assertUnresolvedEntryUrl(String entryUrl, String artifactNotation) {
    assertTrue(entryUrl.contains("Could not find " + artifactNotation) || entryUrl.contains("Could not resolve " + artifactNotation));
  }

  @Test
  @TargetVersions("3.3+") // org.gradle.api.artifacts.ConfigurationPublications was introduced since 3.3
  public void testSourceSetOutputDirsAsArtifactDependencies() throws Exception {
    createSettingsFile("rootProject.name = 'server'\n" +
                       "include 'api'\n" +
                       "include 'modules:X'\n" +
                       "include 'modules:Y'");
    importProject(
      "configure(subprojects - project(':modules')) {\n" +
      "    group 'server'\n" +
      "    version '1.0-SNAPSHOT'\n" +
      "    apply plugin: 'java'\n" +
      "    sourceCompatibility = 1.8\n" +
      "}\n" +
      "\n" +
      "project(':api') {\n" +
      "    sourceSets {\n" +
      "        webapp\n" +
      "    }\n" +
      "    configurations {\n" +
      "        webappConf {\n" +
      "            afterEvaluate {\n" +
      "                sourceSets.webapp.output.each {\n" +
      "                    outgoing.artifact(it) {\n" +
      "                        builtBy(sourceSets.webapp.output)\n" +
      "                    }\n" +
      "                }\n" +
      "            }\n" +
      "        }\n" +
      "    }\n" +
      "}\n" +
      "\n" +
      "def webProjects = [project(':modules:X'), project(':modules:Y')]\n" +
      "configure(webProjects) {\n" +
      "    dependencies {\n" +
      "        compile project(path: ':api', configuration: 'webappConf')\n" +
      "    }\n" +
      "}"
    );

    assertModules("server", "server.modules",
                  "server.modules.X", "server.modules.X.main", "server.modules.X.test",
                  "server.modules.Y", "server.modules.Y.main", "server.modules.Y.test",
                  "server.api", "server.api.main", "server.api.test", "server.api.webapp");

    assertModuleModuleDeps("server.modules.X.main", "server.api.webapp");
    assertModuleModuleDepScope("server.modules.X.main", "server.api.webapp", DependencyScope.COMPILE);

    assertModuleModuleDeps("server.modules.Y.main", "server.api.webapp");
    assertModuleModuleDepScope("server.modules.Y.main", "server.api.webapp", DependencyScope.COMPILE);
  }

  @Test
  public void testSourceSetOutputDirsAsRuntimeDependencies() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "sourceSets.main.output.dir file(\"$buildDir/generated-resources/main\")"
    );

    assertModules("project", "project.main", "project.test");
    final String path = path("build/generated-resources/main");
    final String depName = PathUtil.toPresentableUrl(path);
    String root = "file://" + path;
    assertModuleLibDep("project.main", depName, root);
    assertModuleLibDepScope("project.main", depName, DependencyScope.RUNTIME);

    String[] excludedRoots = isNewDependencyResolutionApplicable() ? new String[]{root} : ArrayUtil.EMPTY_STRING_ARRAY;
    assertLibraryExcludedRoots("project.main", depName, excludedRoots);

    VirtualFile depJar = createProjectJarSubFile("lib/dep.jar");
    importProject(
      "apply plugin: 'java'\n" +
      "sourceSets.main.output.dir file(\"$buildDir/generated-resources/main\")\n" +
      "dependencies {\n" +
      " runtime 'junit:junit:4.11'\n" +
      " runtime files('lib/dep.jar')\n" +
      "}\n"
    );

    assertLibraryExcludedRoots("project.main", depName, excludedRoots);
    assertLibraryExcludedRoots("project.main", depJar.getPresentableUrl(), ArrayUtil.EMPTY_STRING_ARRAY);
    assertLibraryExcludedRoots("project.main", "Gradle: junit:junit:4.11", ArrayUtil.EMPTY_STRING_ARRAY);
  }

  private void assertLibraryExcludedRoots(String moduleName, String depName, String ... roots) {
    List<LibraryOrderEntry> deps = getModuleLibDeps(moduleName, depName);
    assertThat(deps).hasSize(1);
    LibraryEx library = (LibraryEx)deps.get(0).getLibrary();

    assertThat(library.getUrls(OrderRootType.CLASSES)).hasSize(1);

    String[] excludedRootUrls = library.getExcludedRootUrls();
    assertThat(excludedRootUrls).containsExactly(roots);
  }

  @Test
  public void testSourceSetOutputDirsAsRuntimeDependenciesOfDependantModules() throws Exception {
    createSettingsFile("include 'projectA', 'projectB', 'projectC' ");
    importProject(
      "project(':projectA') {\n" +
      "  apply plugin: 'java'\n" +
      "  sourceSets.main.output.dir file(\"$buildDir/generated-resources/main\")\n" +
      "}\n" +
      "project(':projectB') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile project(':projectA')\n" +
      "  }\n" +
      "}\n" +
      "project(':projectC') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    runtime project(':projectB')\n" +
      "  }\n" +
      "}"
    );

    assertModules("project",
                  "project.projectA", "project.projectA.main", "project.projectA.test",
                  "project.projectB", "project.projectB.main", "project.projectB.test",
                  "project.projectC", "project.projectC.main", "project.projectC.test");

    assertModuleModuleDepScope("project.projectB.main", "project.projectA.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.projectC.main", "project.projectA.main", DependencyScope.RUNTIME);
    assertModuleModuleDepScope("project.projectC.main", "project.projectB.main", DependencyScope.RUNTIME);

    final String path = path("projectA/build/generated-resources/main");
    final String classesPath = "file://" + path;
    final String depName = PathUtil.toPresentableUrl(path);
    assertModuleLibDep("project.projectA.main", depName, classesPath);
    assertModuleLibDepScope("project.projectA.main", depName, DependencyScope.RUNTIME);
    assertModuleLibDep("project.projectB.main", depName, classesPath);
    assertModuleLibDepScope("project.projectB.main", depName,
                            isNewDependencyResolutionApplicable() ? DependencyScope.RUNTIME : DependencyScope.COMPILE);
    assertModuleLibDep("project.projectC.main", depName, classesPath);
    assertModuleLibDepScope("project.projectC.main", depName, DependencyScope.RUNTIME);
  }

  @Test
  @TargetVersions("3.4+")
  public void testSourceSetOutputDirsAsDependenciesOfDependantModules() throws Exception {
    createSettingsFile("include 'projectA', 'projectB', 'projectC' ");
    importProject(
      "subprojects { \n" +
      "    apply plugin: \"java\" \n" +
      "}\n" +
      "project(':projectA') {\n" +
      "  sourceSets.main.output.dir file('generated/projectA')\n" +
      "}\n" +
      "project(':projectB') {\n" +
      "  sourceSets.main.output.dir file('generated/projectB')\n" +
      "  dependencies {\n" +
      "    implementation project(':projectA')\n" +
      "  }\n" +
      "}\n" +
      "project(':projectC') {\n" +
      "  dependencies {\n" +
      "    implementation project(':projectB')\n" +
      "  }\n" +
      "}"
    );

    assertModules("project",
                  "project.projectA", "project.projectA.main", "project.projectA.test",
                  "project.projectB", "project.projectB.main", "project.projectB.test",
                  "project.projectC", "project.projectC.main", "project.projectC.test");

    assertModuleModuleDepScope("project.projectB.main", "project.projectA.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.projectC.main", "project.projectA.main", DependencyScope.RUNTIME);
    assertModuleModuleDepScope("project.projectC.main", "project.projectB.main", DependencyScope.COMPILE);

    final String pathA =
      FileUtil.toSystemIndependentName(new File(getProjectPath(), "projectA/generated/projectA").getAbsolutePath());
    final String classesPathA = "file://" + pathA;
    final String depNameA = PathUtil.toPresentableUrl(pathA);

    final String pathB =
      FileUtil.toSystemIndependentName(new File(getProjectPath(), "projectB/generated/projectB").getAbsolutePath());
    final String classesPathB = "file://" + pathB;
    final String depNameB = PathUtil.toPresentableUrl(pathB);

    assertModuleLibDep("project.projectA.main", depNameA, classesPathA);
    assertModuleLibDepScope("project.projectA.main", depNameA, DependencyScope.RUNTIME);

    assertModuleLibDep("project.projectB.main", depNameA, classesPathA);
    assertModuleLibDepScope("project.projectB.main", depNameA,
                            isNewDependencyResolutionApplicable() ? DependencyScope.RUNTIME : DependencyScope.COMPILE);
    assertModuleLibDep("project.projectB.main", depNameB, classesPathB);
    assertModuleLibDepScope("project.projectB.main", depNameB, DependencyScope.RUNTIME);

    assertModuleLibDep("project.projectC.main", depNameA, classesPathA);
    assertModuleLibDepScope("project.projectC.main", depNameA, DependencyScope.RUNTIME);
    assertModuleLibDep("project.projectC.main", depNameB, classesPathB);
    assertModuleLibDepScope("project.projectC.main", depNameB,
                            isNewDependencyResolutionApplicable() ? DependencyScope.RUNTIME : DependencyScope.COMPILE);
  }

  @Test
  public void testProjectArtifactDependencyInTestAndArchivesConfigurations() throws Exception {
    createSettingsFile("include 'api', 'impl' ");

    importProject(
      "allprojects {\n" +
      "  apply plugin: 'java'\n" +
      "}\n" +
      "\n" +
      "project(\"api\") {\n" +
      "  configurations {\n" +
      "    tests\n" +
      "  }\n" +
      "  task testJar(type: Jar, dependsOn: testClasses, description: \"archive the testClasses\") {\n" +
      "    baseName = \"${project.archivesBaseName}-tests\"\n" +
      "    classifier = \"tests\"\n" +
      "    from sourceSets.test.output\n" +
      "  }\n" +
      "  artifacts {\n" +
      "    tests testJar\n" +
      "    archives testJar\n" +
      "  }\n" +
      "}\n" +
      "project(\"impl\") {\n" +
      "  dependencies {\n" +
      "    testCompile  project(path: ':api', configuration: 'tests')\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test",
                  "project.api", "project.api.main", "project.api.test",
                  "project.impl", "project.impl.main", "project.impl.test");

    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);
    assertProductionOnTestDependencies("project.test", ArrayUtilRt.EMPTY_STRING_ARRAY);

    assertModuleModuleDepScope("project.api.test", "project.api.main", DependencyScope.COMPILE);
    assertProductionOnTestDependencies("project.api.test", ArrayUtilRt.EMPTY_STRING_ARRAY);

    assertModuleModuleDepScope("project.impl.test", "project.impl.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.impl.test", "project.api.test", DependencyScope.COMPILE);
    assertProductionOnTestDependencies("project.impl.test", "project.api.test");

    assertModuleModuleDeps("project.impl.main", ArrayUtilRt.EMPTY_STRING_ARRAY);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.api", "project.impl");

    assertModuleModuleDepScope("project.impl", "project.api", DependencyScope.TEST);
  }

  @Test
  public void testCompileAndRuntimeConfigurationsTransitiveDependencyMerge() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n" +
                       "include 'project-tests'");

    importProject(
      "subprojects {\n" +
      "  apply plugin: \"java\"\n" +
      "}\n" +
      "\n" +
      "project(\":project1\") {\n" +
      "  dependencies {\n" +
      "      compile 'org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.0'\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(\":project2\") {\n" +
      "  dependencies {\n" +
      "      runtime 'org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.1.1'\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(\":project-tests\") {\n" +
      "  dependencies {\n" +
      "      compile project(':project1')\n" +
      "      runtime project(':project2')\n" +
      "      compile 'junit:junit:4.11'\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test",
                  "project.project-tests", "project.project-tests.main", "project.project-tests.test");

    assertModuleModuleDepScope("project.project-tests.main", "project.project1.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.project-tests.main", "project.project2.main", DependencyScope.RUNTIME);
    assertModuleLibDepScope("project.project-tests.main", "Gradle: org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.0",
                            isNewDependencyResolutionApplicable() ? DependencyScope.PROVIDED : DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project-tests.main", "Gradle: org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.1.1",
                            DependencyScope.RUNTIME);

    createProjectSubDirs("project1", "project2", "project-tests");
    assertCompileClasspathOrdering("project.project-tests.main");

    importProjectUsingSingeModulePerGradleProject();

    assertMergedModuleCompileModuleDepScope("project.project-tests", "project.project1");

    boolean gradleOlderThen_3_4 = isGradleOlderThen("3.4");
    if (gradleOlderThen_3_4) {
      assertModuleModuleDepScope("project.project-tests", "project.project2", DependencyScope.RUNTIME);
    }
    else {
      assertModuleModuleDepScope("project.project-tests", "project.project2", DependencyScope.RUNTIME, DependencyScope.TEST);
    }
    if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("2.0")) > 0) {
      if (isGradleNewerThen("4.5")) {
        assertModuleLibDepScope("project.project-tests", "Gradle: org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.0",
                                ar(DependencyScope.PROVIDED));
        assertModuleLibDepScope("project.project-tests", "Gradle: org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.1.1",
                                ar(DependencyScope.RUNTIME, DependencyScope.TEST));
      }
      else {
        assertModuleLibDepScope("project.project-tests", "Gradle: org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.0",
                                gradleOlderThen_3_4 ? ar(DependencyScope.COMPILE) : ar(DependencyScope.PROVIDED, DependencyScope.TEST));
        assertModuleLibDepScope("project.project-tests", "Gradle: org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.1.1",
                                gradleOlderThen_3_4 ? ar(DependencyScope.RUNTIME) : ar(DependencyScope.RUNTIME, DependencyScope.TEST));
      }
    }
  }

  @Test
  public void testNonDefaultProjectConfigurationDependency() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "project(':project1') {\n" +
      "  configurations {\n" +
      "    myConf {\n" +
      "      description = 'My Conf'\n" +
      "      transitive = true\n" +
      "    }\n" +
      "  }\n" +
      "  dependencies {\n" +
      "    myConf 'junit:junit:4.11'\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile project(path: ':project1', configuration: 'myConf')\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project", "project.project1", "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDeps("project.project2.main");
    assertModuleLibDepScope("project.project2.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.main", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.project1", "project.project2");
    if (isGradleNewerThen("4.5")) {
      assertModuleModuleDepScope("project.project2", "project.project1");
    }
    else if (isGradleOlderThen("3.4")) {
      assertModuleModuleDepScope("project.project2", "project.project1", DependencyScope.COMPILE);
    }
    else {
      assertModuleModuleDepScope("project.project2", "project.project1", DependencyScope.PROVIDED, DependencyScope.TEST,
                                 DependencyScope.RUNTIME);
    }
    if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("2.0")) > 0) {
      assertMergedModuleCompileLibDepScope("project.project2", "Gradle: org.hamcrest:hamcrest-core:1.3");
      assertMergedModuleCompileLibDepScope("project.project2", "Gradle: junit:junit:4.11");
    }
  }

  @Test
  public void testNonDefaultProjectConfigurationDependencyWithMultipleArtifacts() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "project(':project1') {\n" +
      "  apply plugin: 'java'\n" +
      "  configurations {\n" +
      "    tests.extendsFrom testRuntime\n" +
      "  }\n" +
      "  task testJar(type: Jar) {\n" +
      "    classifier 'test'\n" +
      "    from project.sourceSets.test.output\n" +
      "  }\n" +
      "\n" +
      "  artifacts {\n" +
      "    tests testJar\n" +
      "    archives testJar\n" +
      "  }\n" +
      "\n" +
      "  dependencies {\n" +
      "    testCompile 'junit:junit:4.11'\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    testCompile project(path: ':project1', configuration: 'tests')\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDeps("project.project1.main", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertModuleLibDeps("project.project1.main", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertModuleLibDepScope("project.project1.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project1.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);

    assertModuleModuleDeps("project.project2.main", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertModuleLibDeps("project.project2.main", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertModuleLibDepScope("project.project2.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);

    assertModuleModuleDeps("project.project2.test", "project.project2.main", "project.project1.main", "project.project1.test");
    assertModuleModuleDepScope("project.project2.test", "project.project2.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.project2.test", "project.project1.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.project2.test", "project.project1.test", DependencyScope.COMPILE);
    assertProductionOnTestDependencies("project.project2.test", "project.project1.test");
  }

  @Test
  public void testDependencyOnDefaultConfigurationWithAdditionalArtifact() throws Exception {
    createSettingsFile("include 'project1', 'project2'");
    createProjectSubFile("project1/build.gradle",
                         new GradleBuildScriptBuilderEx()
                           .withJavaPlugin()
                           .addPostfix(
                             "configurations {",
                             "  aParentCfg",
                             "  compile.extendsFrom aParentCfg",
                             "}",
                             "sourceSets {",
                             "  aParentSrc { java.srcDirs = ['src/aParent/java'] }",
                             "  main { java { compileClasspath += aParentSrc.output } }",
                             "}",
                             "task aParentSrcJar(type:Jar) {",
                             "    appendix 'parent'",
                             "    from sourceSets.aParentSrc.output",
                             "}",
                             "artifacts {",
                             "  aParentCfg aParentSrcJar",
                             "}"
                           )
                           .generate()
    );

    createProjectSubFile("project2/build.gradle",
                         new GradleBuildScriptBuilderEx().withJavaPlugin().addPostfix(
                           "dependencies {",
                           "  compile project(':project1')",
                           "}"
                         ).generate());

    importProject("");

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test", "project.project1.aParentSrc",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDeps("project.project2.main", "project.project1.main", "project.project1.aParentSrc");
  }


  @Test
  @TargetVersions("2.0+")
  public void testTestModuleDependencyAsArtifactFromTestSourceSetOutput() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "project(':project1') {\n" +
      "  apply plugin: 'java'\n" +
      "  configurations {\n" +
      "    testArtifacts\n" +
      "  }\n" +
      "\n" +
      "  task testJar(type: Jar) {\n" +
      "    classifier = 'tests'\n" +
      "    from sourceSets.test.output\n" +
      "  }\n" +
      "\n" +
      "  artifacts {\n" +
      "    testArtifacts testJar\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    testCompile project(path: ':project1', configuration: 'testArtifacts')\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDeps("project.project2.main", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertModuleModuleDeps("project.project2.test", "project.project2.main", "project.project1.test");
    assertProductionOnTestDependencies("project.project2.test", "project.project1.test");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.project1", "project.project2");
    assertModuleModuleDeps("project.project2", "project.project1");
  }

  @Test
  @TargetVersions("2.0+")
  public void testTestModuleDependencyAsArtifactFromTestSourceSetOutput2() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "project(':project1') {\n" +
      "  apply plugin: 'java'\n" +
      "  configurations {\n" +
      "    testArtifacts\n" +
      "  }\n" +
      "\n" +
      "  task testJar(type: Jar) {\n" +
      "    classifier = 'tests'\n" +
      "    from sourceSets.test.output\n" +
      "  }\n" +
      "\n" +
      "  artifacts {\n" +
      "    testArtifacts testJar\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile project(path: ':project1')\n" +
      "    testCompile project(path: ':project1', configuration: 'testArtifacts')\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDeps("project.project2.main", "project.project1.main");
    assertProductionOnTestDependencies("project.project2.main", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertModuleModuleDeps("project.project2.test", "project.project2.main", "project.project1.main", "project.project1.test");
    assertProductionOnTestDependencies("project.project2.test", "project.project1.test");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.project1", "project.project2");
  }

  @Test
  @TargetVersions("2.0+")
  public void testTestModuleDependencyAsArtifactFromTestSourceSetOutput3() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "allprojects {\n" +
      "  apply plugin: 'idea'\n" +
      "  idea {\n" +
      "    module {\n" +
      "      inheritOutputDirs = false\n" +
      "      outputDir = file(\"buildIdea/main\")\n" +
      "      testOutputDir = file(\"buildIdea/test\")\n" +
      "      excludeDirs += file('buildIdea')\n" +
      "    }\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project1') {\n" +
      "  apply plugin: 'java'\n" +
      "  configurations {\n" +
      "    testArtifacts\n" +
      "  }\n" +
      "\n" +
      "  task testJar(type: Jar) {\n" +
      "    classifier = 'tests'\n" +
      "    from sourceSets.test.output\n" +
      "  }\n" +
      "\n" +
      "  artifacts {\n" +
      "    testArtifacts testJar\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    testCompile project(path: ':project1', configuration: 'testArtifacts')\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleOutput("project.project1.main", getProjectPath() + "/project1/buildIdea/main", "");
    assertModuleOutput("project.project1.test", "", getProjectPath() + "/project1/buildIdea/test");

    assertModuleOutput("project.project2.main", getProjectPath() + "/project2/buildIdea/main", "");
    assertModuleOutput("project.project2.test", "", getProjectPath() + "/project2/buildIdea/test");

    assertModuleModuleDeps("project.project2.main", ArrayUtilRt.EMPTY_STRING_ARRAY);
    assertModuleModuleDeps("project.project2.test", "project.project2.main", "project.project1.test");
    assertProductionOnTestDependencies("project.project2.test", "project.project1.test");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.project1", "project.project2");
    assertModuleModuleDeps("project.project2", "project.project1");
  }

  @Test
  @TargetVersions("2.6+")
  public void testProjectSubstitutions() throws Exception {
    createSettingsFile("include 'core'\n" +
                       "include 'service'\n" +
                       "include 'util'\n");

    importProject(
      "subprojects {\n" +
      "  apply plugin: 'java'\n" +
      "  configurations.all {\n" +
      "    resolutionStrategy.dependencySubstitution {\n" +
      "      substitute module('mygroup:core') with project(':core')\n" +
      "      substitute project(':util') with module('junit:junit:4.11')\n" +
      "    }\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':core') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile project(':util')\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':service') {\n" +
      "  dependencies {\n" +
      "    compile 'mygroup:core:latest.release'\n" +
      "  }\n" +
      "}\n"
    );

    assertModules("project",
                  "project.core", "project.core.main", "project.core.test",
                  "project.service", "project.service.main", "project.service.test",
                  "project.util", "project.util.main", "project.util.test");

    assertModuleModuleDeps("project.service.main", "project.core.main");
    assertModuleModuleDepScope("project.service.main", "project.core.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.service.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.service.main", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.core", "project.service", "project.util");

    assertMergedModuleCompileModuleDepScope("project.service", "project.core");
    assertMergedModuleCompileLibDepScope("project.service", "Gradle: org.hamcrest:hamcrest-core:1.3");
    assertMergedModuleCompileLibDepScope("project.service", "Gradle: junit:junit:4.11");
  }

  @Test
  @TargetVersions("2.6+")
  public void testProjectSubstitutionsWithTransitiveDeps() throws Exception {
    createSettingsFile("include 'modA'\n" +
                       "include 'modB'\n" +
                       "include 'app'\n");

    importProject(
      "subprojects {\n" +
      "  apply plugin: 'java'\n" +
      "  version '1.0.0'\n" +
      "}\n" +
      "project(':app') {\n" +
      "  dependencies {\n" +
      "    runtime 'org.hamcrest:hamcrest-core:1.3'\n" +
      "    testCompile 'project:modA:1.0.0'\n" +
      "  }\n" +
      "\n" +
      "  configurations.all {\n" +
      "    resolutionStrategy.dependencySubstitution {\n" +
      "      substitute module('project:modA:1.0.0') with project(':modA')\n" +
      "      substitute module('project:modB:1.0.0') with project(':modB')\n" +
      "    }\n" +
      "  }\n" +
      "}\n" +
      "project(':modA') {\n" +
      "  dependencies {\n" +
      "    compile project(':modB')\n" +
      "  }\n" +
      "}\n" +
      "project(':modB') {\n" +
      "  dependencies {\n" +
      "    compile 'org.hamcrest:hamcrest-core:1.3'\n" +
      "  }\n" +
      "}"
    );

    assertModules("project", "project.app", "project.app.main", "project.app.test",
                  "project.modA", "project.modA.main", "project.modA.test",
                  "project.modB", "project.modB.main", "project.modB.test");

    assertModuleLibDepScope("project.app.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.RUNTIME);
    assertModuleModuleDeps("project.app.main");
    assertModuleLibDepScope("project.app.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleModuleDeps("project.app.test", "project.app.main", "project.modA.main", "project.modB.main");

    assertModuleLibDepScope("project.modA.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleModuleDeps("project.modA.main", "project.modB.main");
    assertModuleLibDepScope("project.modA.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleModuleDeps("project.modA.test", "project.modA.main", "project.modB.main");

    assertModuleLibDepScope("project.modB.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleModuleDeps("project.modB.main");
    assertModuleLibDepScope("project.modB.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleModuleDeps("project.modB.test", "project.modB.main");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.app", "project.modA", "project.modB");

    assertModuleModuleDeps("project.app", "project.modA", "project.modB");
    assertModuleModuleDepScope("project.app", "project.modA", DependencyScope.TEST);
    assertModuleModuleDepScope("project.app", "project.modB", DependencyScope.TEST);
    assertModuleLibDepScope("project.app", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.RUNTIME, DependencyScope.TEST);

    assertMergedModuleCompileModuleDepScope("project.modA", "project.modB");
    assertMergedModuleCompileLibDepScope("project.modA", "Gradle: org.hamcrest:hamcrest-core:1.3");

    assertModuleModuleDeps("project.modB");
    assertMergedModuleCompileLibDepScope("project.modB", "Gradle: org.hamcrest:hamcrest-core:1.3");
  }

  @Test
  @TargetVersions("2.12+")
  public void testCompileOnlyScope() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "dependencies {\n" +
      "  compileOnly 'junit:junit:4.11'\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);

    assertModuleLibDepScope("project.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.PROVIDED);
    assertModuleLibDepScope("project.main", "Gradle: junit:junit:4.11", DependencyScope.PROVIDED);

    assertModuleLibDeps("project.test");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");

    assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.PROVIDED);
    assertModuleLibDepScope("project", "Gradle: junit:junit:4.11", DependencyScope.PROVIDED);
  }

  @Test
  @TargetVersions("2.12+")
  public void testCompileOnlyAndRuntimeScope() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "dependencies {\n" +
      "  runtime 'org.hamcrest:hamcrest-core:1.3'\n" +
      "  compileOnly 'org.hamcrest:hamcrest-core:1.3'\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);

    assertModuleLibDepScope("project.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.RUNTIME);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");

    if (isGradleNewerThen("4.5")) {
      assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    }
    else if (isGradleOlderThen("3.4")) {
      assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.PROVIDED, DependencyScope.RUNTIME);
    }
    else {
      assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.TEST, DependencyScope.PROVIDED,
                              DependencyScope.RUNTIME);
    }
  }

  @Test
  @TargetVersions("2.12+")
  public void testCompileOnlyAndCompileScope() throws Exception {
    createSettingsFile("include 'app'\n");
    importProject(
      "apply plugin: 'java'\n" +
      "dependencies {\n" +
      "  compileOnly project(':app')\n" +
      "  compile 'junit:junit:4.11'\n" +
      "}\n" +
      "project(':app') {\n" +
      "  apply plugin: 'java'\n" +
      "  repositories {\n" +
      "    mavenCentral()\n" +
      "  }\n" +
      "  dependencies {\n" +
      "    compile 'junit:junit:4.11'\n" +
      "  }\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test",
                  "project.app", "project.app.main", "project.app.test");

    assertModuleModuleDepScope("project.main", "project.app.main", DependencyScope.PROVIDED);
    assertModuleLibDepScope("project.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.main", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);

    assertModuleModuleDeps("project.test", "project.main");
    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
  }

  @Test
  @TargetVersions("3.4+")
  public void testJavaLibraryPluginConfigurations() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "project(':project1') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile project(path: ':project2')\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java-library'\n" +
      "  dependencies {\n" +
      "    implementation group: 'junit', name: 'junit', version: '4.11'\n" +
      "    api group: 'org.hamcrest', name: 'hamcrest-core', version: '1.3'\n" +
      "  }\n" +
      "\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDepScope("project.project1.main", "project.project2.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project1.main", "Gradle: junit:junit:4.11", DependencyScope.RUNTIME);
    assertModuleLibDepScope("project.project1.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);

    assertModuleModuleDepScope("project.project1.test", "project.project1.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.project1.test", "project.project2.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project1.test", "Gradle: junit:junit:4.11", DependencyScope.RUNTIME);
    assertModuleLibDepScope("project.project1.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);

    assertModuleLibDepScope("project.project2.main", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.project2.test", "project.project2.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
  }


  @Test
  @TargetVersions("2.12+")
  public void testNonTransitiveConfiguration() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "configurations {\n" +
      "  compile.transitive = false\n" +
      "}\n" +
      "\n" +
      "dependencies {\n" +
      "  compile 'junit:junit:4.11'\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");
    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);

    assertModuleLibDepScope("project.main", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);

    assertModuleLibDepScope("project.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");

    assertMergedModuleCompileLibDepScope("project", "Gradle: junit:junit:4.11");

    if (isGradleOlderThen("3.4")) {
      assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.PROVIDED, DependencyScope.RUNTIME);
    }
    else if (isGradleNewerThen("4.5")) {
      assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    }
    else {
      assertModuleLibDepScope("project", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.PROVIDED, DependencyScope.RUNTIME,
                              DependencyScope.TEST);
    }
  }

  @Test
  @TargetVersions("2.0+")
  public void testProvidedTransitiveDependencies() throws Exception {
    createSettingsFile("include 'projectA', 'projectB', 'projectC' ");
    importProject(
      "project(':projectA') {\n" +
      "  apply plugin: 'java'\n" +
      "}\n" +
      "project(':projectB') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile project(':projectA')\n" +
      "  }\n" +
      "}\n" +
      "project(':projectC') {\n" +
      "  apply plugin: 'war'\n" +
      "  dependencies {\n" +
      "    providedCompile project(':projectB')\n" +
      "  }\n" +
      "}"
    );

    assertModules("project",
                  "project.projectA", "project.projectA.main", "project.projectA.test",
                  "project.projectB", "project.projectB.main", "project.projectB.test",
                  "project.projectC", "project.projectC.main", "project.projectC.test");

    assertModuleModuleDepScope("project.projectB.main", "project.projectA.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.projectC.main", "project.projectA.main", DependencyScope.PROVIDED);
    assertModuleModuleDepScope("project.projectC.main", "project.projectB.main", DependencyScope.PROVIDED);

    createProjectSubDirs("projectA", "projectB", "projectC");
    assertCompileClasspathOrdering("project.projectC.main");

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project", "project.projectA", "project.projectB", "project.projectC");
    assertMergedModuleCompileModuleDepScope("project.projectB", "project.projectA");
    if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("2.5")) >= 0) {
      assertModuleModuleDepScope("project.projectC", "project.projectA", DependencyScope.PROVIDED);
    }
    assertModuleModuleDepScope("project.projectC", "project.projectB", DependencyScope.PROVIDED);
  }

  @Test
  public void testProjectConfigurationDependencyWithDependencyOnTestOutput() throws Exception {
    createSettingsFile("include 'project1'\n" +
                       "include 'project2'\n");

    importProject(
      "project(':project1') {\n" +
      "  apply plugin: 'java'\n" +
      "  configurations {\n" +
      "    testOutput\n" +
      "    testOutput.extendsFrom (testCompile)\n" +
      "  }\n" +
      "\n" +
      "  dependencies {\n" +
      "    testOutput sourceSets.test.output\n" +
      "    testCompile group: 'junit', name: 'junit', version: '4.11'\n" +
      "  }\n" +
      "}\n" +
      "\n" +
      "project(':project2') {\n" +
      "  apply plugin: 'java'\n" +
      "  dependencies {\n" +
      "    compile project(path: ':project1')\n" +
      "\n" +
      "    testCompile group: 'junit', name: 'junit', version: '4.11'\n" +
      "    testCompile project(path: ':project1', configuration: 'testOutput')\n" +
      "  }\n" +
      "\n" +
      "}\n"
    );

    assertModules("project",
                  "project.project1", "project.project1.main", "project.project1.test",
                  "project.project2", "project.project2.main", "project.project2.test");

    assertModuleModuleDepScope("project.project1.test", "project.project1.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project1.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project1.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);

    assertModuleModuleDepScope("project.project2.main", "project.project1.main", DependencyScope.COMPILE);

    assertModuleModuleDepScope("project.project2.test", "project.project2.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.project2.test", "project.project1.test", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.project2.test", "project.project1.main", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.project2.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
  }

  @TargetVersions("2.5+")
  @Test
  public void testJavadocAndSourcesForDependencyWithMultipleArtifacts() throws Exception {
    createProjectSubFile("repo/depGroup/depArtifact/1.0-SNAPSHOT/ivy-1.0-SNAPSHOT.xml",
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                         "<ivy-module version=\"2.0\" xmlns:m=\"http://ant.apache.org/ivy/maven\">\n" +
                         "  <info organisation=\"depGroup\" module=\"depArtifact\" revision=\"1.0-SNAPSHOT\" status=\"integration\" publication=\"20170817121528\"/>\n" +
                         "  <configurations>\n" +
                         "    <conf name=\"compile\" visibility=\"public\"/>\n" +
                         "    <conf name=\"default\" visibility=\"public\" extends=\"compile\"/>\n" +
                         "    <conf name=\"sources\" visibility=\"public\"/>\n" +
                         "    <conf name=\"javadoc\" visibility=\"public\"/>\n" +
                         "  </configurations>\n" +
                         "  <publications>\n" +
                         "    <artifact name=\"depArtifact\" type=\"jar\" ext=\"jar\" conf=\"compile\"/>\n" +
                         "    <artifact name=\"depArtifact\" type=\"source\" ext=\"jar\" conf=\"sources\" m:classifier=\"sources\"/>\n" +
                         "    <artifact name=\"depArtifact\" type=\"javadoc\" ext=\"jar\" conf=\"javadoc\" m:classifier=\"javadoc\"/>\n" +
                         "    <artifact name=\"depArtifact-api\" type=\"javadoc\" ext=\"jar\" conf=\"javadoc\" m:classifier=\"javadoc\"/>\n" +
                         "    <artifact name=\"depArtifact-api\" type=\"source\" ext=\"jar\" conf=\"sources\" m:classifier=\"sources\"/>\n" +
                         "  </publications>\n" +
                         "  <dependencies/>\n" +
                         "</ivy-module>\n");
    VirtualFile classesJar = createProjectJarSubFile("repo/depGroup/depArtifact/1.0-SNAPSHOT/depArtifact-1.0-SNAPSHOT.jar");
    VirtualFile javadocJar = createProjectJarSubFile("repo/depGroup/depArtifact/1.0-SNAPSHOT/depArtifact-1.0-SNAPSHOT-javadoc.jar");
    VirtualFile sourcesJar = createProjectJarSubFile("repo/depGroup/depArtifact/1.0-SNAPSHOT/depArtifact-1.0-SNAPSHOT-sources.jar");
    createProjectJarSubFile("repo/depGroup/depArtifact/1.0-SNAPSHOT/depArtifact-api-1.0-SNAPSHOT.jar");
    createProjectJarSubFile("repo/depGroup/depArtifact/1.0-SNAPSHOT/depArtifact-api-1.0-SNAPSHOT-javadoc.jar");
    createProjectJarSubFile("repo/depGroup/depArtifact/1.0-SNAPSHOT/depArtifact-api-1.0-SNAPSHOT-sources.jar");

    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "repositories {\n" +
      "  ivy { url file('repo') }\n" +
      "}\n" +
      "\n" +
      "dependencies {\n" +
      "  compile 'depGroup:depArtifact:1.0-SNAPSHOT'\n" +
      "}\n" +
      "apply plugin: 'idea'\n" +
      "idea.module.downloadJavadoc true"
    );

    assertModules("project", "project.main", "project.test");

    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);

    final String depName = "Gradle: depGroup:depArtifact:1.0-SNAPSHOT";
    assertModuleLibDep("project.main", depName, classesJar.getUrl(), sourcesJar.getUrl(), javadocJar.getUrl());
    assertModuleLibDepScope("project.main", depName, DependencyScope.COMPILE);
    assertModuleLibDep("project.test", depName, classesJar.getUrl(), sourcesJar.getUrl(), javadocJar.getUrl());
    assertModuleLibDepScope("project.test", depName, DependencyScope.COMPILE);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("project");

    // Gradle built-in models has been fixed since 2.3 version, https://issues.gradle.org/browse/GRADLE-3170
    if (GradleVersion.version(gradleVersion).compareTo(GradleVersion.version("2.3")) >= 0) {
      assertModuleLibDep("project", depName, classesJar.getUrl(), sourcesJar.getUrl(), javadocJar.getUrl());
    }
    assertMergedModuleCompileLibDepScope("project", depName);
  }

  @Test
  @TargetVersions("4.6+")
  public void testAnnotationProcessorDependencies() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "dependencies {\n" +
      "    compileOnly 'org.projectlombok:lombok:1.16.2'\n" +
      "    testCompileOnly 'org.projectlombok:lombok:1.16.2'\n" +
      "    annotationProcessor 'org.projectlombok:lombok:1.16.2'\n" +
      "}\n");

    final String depName = "Gradle: org.projectlombok:lombok:1.16.2";
    assertModuleLibDepScope("project.main", depName, DependencyScope.PROVIDED);
  }

  @Test // https://youtrack.jetbrains.com/issue/IDEA-223152
  @TargetVersions("5.3+")
  public void testTransformedProjectDependency() throws Exception {
    createSettingsFile("include 'lib-1'\n" +
                       "include 'lib-2'\n");

    importProject(
      "import java.nio.file.Files\n" +
      "import java.util.zip.ZipEntry\n" +
      "import java.util.zip.ZipException\n" +
      "import java.util.zip.ZipFile\n" +
      "import org.gradle.api.artifacts.transform.TransformParameters\n" +
      "\n" +
      "abstract class Unzip implements TransformAction<TransformParameters.None> {\n" +
      "    @InputArtifact\n" +
      "    abstract Provider<FileSystemLocation> getInputArtifact()\n" +
      "\n" +
      "    @Override\n" +
      "    void transform(TransformOutputs outputs) {\n" +
      "        def input = inputArtifact.get().asFile\n" +
      "        def unzipDir = outputs.dir(input.name)\n" +
      "        unzipTo(input, unzipDir)\n" +
      "    }\n" +
      "\n" +
      "    private static void unzipTo(File zipFile, File unzipDir) {\n" +
      "        new ZipFile(zipFile).withCloseable { zip ->\n" +
      "            def outputDirectoryCanonicalPath = unzipDir.canonicalPath\n" +
      "            for (entry in zip.entries()) {\n" +
      "                unzipEntryTo(unzipDir, outputDirectoryCanonicalPath, zip, entry)\n" +
      "            }\n" +
      "        }\n" +
      "    }\n" +
      "\n" +
      "    private static unzipEntryTo(File outputDirectory, String outputDirectoryCanonicalPath, ZipFile zip, ZipEntry entry) {\n" +
      "        def output = new File(outputDirectory, entry.name)\n" +
      "        if (!output.canonicalPath.startsWith(outputDirectoryCanonicalPath)) {\n" +
      "            throw new ZipException(\"Zip entry '${entry.name}' is outside of the output directory\")\n" +
      "        }\n" +
      "        if (entry.isDirectory()) {\n" +
      "            output.mkdirs()\n" +
      "        } else {\n" +
      "            output.parentFile.mkdirs()\n" +
      "            zip.getInputStream(entry).withCloseable { Files.copy(it, output.toPath()) }\n" +
      "        }\n" +
      "    }\n" +
      "}\n" +
      "\n" +
      "allprojects {\n" +
      "    apply plugin: 'java'\n" +
      "\n" +
      "    repositories {\n" +
      "        jcenter()\n" +
      "    }\n" +
      "}\n" +
      "\n" +
      "def processed = Attribute.of('processed', Boolean)\n" +
      "def artifactType = Attribute.of('artifactType', String)\n" +
      "\n" +
      "\n" +
      "dependencies {\n" +
      "    attributesSchema {\n" +
      "        attribute(processed)\n" +
      "    }\n" +
      "\n" +
      "    artifactTypes.getByName(\"jar\") {\n" +
      "        attributes.attribute(processed, false) \n" +
      "    }\n" +
      "\n" +
      "    registerTransform(Unzip) {\n" +
      "        from.attribute(artifactType, 'jar').attribute(processed, false)\n" +
      "        to.attribute(artifactType, 'java-classes-directory').attribute(processed, true)\n" +
      "    }\n" +
      "\n" +
      "    implementation project(':lib-1')\n" +
      "    implementation project(':lib-2')\n" +
      "}\n" +
      "\n" +
      "\n" +
      "configurations.all {\n" +
      "    afterEvaluate {\n" +
      "        if (canBeResolved) {\n" +
      "            attributes.attribute(processed, true)\n" +
      "        }\n" +
      "    }\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test",
                  "project.lib-1", "project.lib-1.main", "project.lib-1.test",
                  "project.lib-2", "project.lib-2.main", "project.lib-2.test");

    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.lib-1.test", "project.lib-1.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.lib-2.test", "project.lib-2.main", DependencyScope.COMPILE);

    assertModuleModuleDeps("project.main", "project.lib-1.main", "project.lib-2.main");

    runTask("build");
    importProject();

    assertModules("project", "project.main", "project.test",
                  "project.lib-1", "project.lib-1.main", "project.lib-1.test",
                  "project.lib-2", "project.lib-2.main", "project.lib-2.test");

    assertModuleModuleDepScope("project.test", "project.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.lib-1.test", "project.lib-1.main", DependencyScope.COMPILE);
    assertModuleModuleDepScope("project.lib-2.test", "project.lib-2.main", DependencyScope.COMPILE);

    assertModuleModuleDeps("project.main", ArrayUtil.EMPTY_STRING_ARRAY);

    assertModuleLibDeps((actual, expected) -> {
      return actual.contains("build/.transforms/") && new File(actual).getName().equals(new File(expected).getName());
    }, "project.main", "lib-1.jar", "lib-2.jar");
  }

  @Test
  public void testSourcesJavadocAttachmentFromGradleCache() throws Exception {
    importProject(new GradleBuildScriptBuilderEx()
                    .withJavaPlugin()
                    .withJUnit("4.12") // download classes and sources - the default import settings
                    .generate());
    assertModules("project", "project.main", "project.test");

    WriteAction.runAndWait(() -> {
      LibraryOrderEntry regularLibFromGradleCache = assertSingleLibraryOrderEntry("project.test", "Gradle: junit:junit:4.12");
      Library library = regularLibFromGradleCache.getLibrary();
      ApplicationManager.getApplication().runWriteAction(() -> library.getTable().removeLibrary(library));
    });

    importProject(new GradleBuildScriptBuilderEx()
                    .withJavaPlugin()
                    .withIdeaPlugin()
                    .withJUnit("4.12")
                    .addPrefix("idea.module {\n" +
                               "  downloadJavadoc = true\n" +
                               "  downloadSources = false\n" + // should be already available in Gradle cache
                               "}")
                    .generate());

    assertModules("project", "project.main", "project.test");

    LibraryOrderEntry regularLibFromGradleCache = assertSingleLibraryOrderEntry("project.test", "Gradle: junit:junit:4.12");
    assertThat(regularLibFromGradleCache.getRootFiles(OrderRootType.CLASSES))
      .hasSize(1)
      .allSatisfy(file -> assertEquals("junit-4.12.jar", file.getName()));

    String binaryPath = PathUtil.getLocalPath(regularLibFromGradleCache.getRootFiles(OrderRootType.CLASSES)[0]);
    Ref<Boolean> sourceFound = Ref.create(false);
    Ref<Boolean> docFound = Ref.create(false);
    checkIfSourcesOrJavadocsCanBeAttached(binaryPath, sourceFound, docFound);

    if (sourceFound.get()) {
      assertThat(regularLibFromGradleCache.getRootFiles(OrderRootType.SOURCES))
        .hasSize(1)
        .allSatisfy(file -> assertEquals("junit-4.12-sources.jar", file.getName()));
    }
    if (docFound.get()) {
      assertThat(regularLibFromGradleCache.getRootFiles(JavadocOrderRootType.getInstance()))
        .hasSize(1)
        .allSatisfy(file -> assertEquals("junit-4.12-javadoc.jar", file.getName()));
    }
  }

  @Test
  @TargetVersions("6.1+")
  public void testSourcesJavadocAttachmentFromClassesFolder() throws Exception {
    createSettingsFile("include 'aLib'");
    createProjectSubFile("aLib/build.gradle",
                         "plugins {\n" +
                         "    id 'java-library'\n" +
                         "    id 'maven-publish'\n" +
                         "}\n" +
                         "java {\n" +
                         "    withJavadocJar()\n" +
                         "    withSourcesJar()\n" +
                         "}\n" +
                         "publishing {\n" +
                         "    publications {\n" +
                         "        mavenJava(MavenPublication) {\n" +
                         "            artifactId = 'aLib'\n" +
                         "            groupId = 'test'\n" +
                         "            version = '1.0-SNAPSHOT'\n" +
                         "            from components.java\n" +
                         "        }\n" +
                         "        mavenJava1(MavenPublication) {\n" +
                         "            artifactId = 'aLib'\n" +
                         "            groupId = 'test'\n" +
                         "            version = '1.0-SNAPSHOT-1'\n" +
                         "            from components.java\n" +
                         "        }\n" +
                         "        mavenJava2(MavenPublication) {\n" +
                         "            artifactId = 'aLib'\n" +
                         "            groupId = 'test'\n" +
                         "            version = '1.0-SNAPSHOT-2'\n" +
                         "            from components.java\n" +
                         "        }\n" +
                         "    }\n" +
                         "}\n" +
                         "configurations {\n" +
                         "    libConf\n" +
                         "}\n" +
                         "dependencies {\n" +
                         "    libConf 'test:aLib:1.0-SNAPSHOT'\n" +
                         "}\n" +
                         "task moveALibToGradleUserHome() {\n" +
                         "    dependsOn publishToMavenLocal\n" +
                         "    doLast {\n" +
                         "        repositories.add(repositories.mavenLocal())\n" +
                         "        def libArtifact = configurations.libConf.singleFile\n" +
                         "        def libRepoFolder = libArtifact.parentFile.parentFile\n" +
                         "        ant.move file: libRepoFolder,\n" +
                         "                 todir: new File(gradle.gradleUserHomeDir, '/caches/ij_test_repo/test')\n" +
                         "    }\n" +
                         "}\n" +
                         "task removeALibFromGradleUserHome(type: Delete) {\n" +
                         "    delete new File(gradle.gradleUserHomeDir, '/caches/ij_test_repo/test')\n" +
                         "    followSymlinks = true" +
                         "}");
    importProject(new GradleBuildScriptBuilderEx()
                    .generate());
    assertModules("project",
                  "project.aLib", "project.aLib.main", "project.aLib.test");

    runTask(":aLib:moveALibToGradleUserHome");
    try {
      importProject(new GradleBuildScriptBuilderEx()
                      .withJavaPlugin()
                      .withIdeaPlugin()
                      .addRepository(" maven { url new File(gradle.gradleUserHomeDir, 'caches/ij_test_repo')} ")
                      .addDependency("implementation 'test:aLib:1.0-SNAPSHOT-1'")
                      .addPrefix("idea.module {\n" +
                                 "  downloadJavadoc = true\n" +
                                 "  downloadSources = false\n" +
                                 "}")
                      .generate());
    }
    finally {
      runTask(":aLib:removeALibFromGradleUserHome");
    }

    assertModules("project", "project.main", "project.test",
                  "project.aLib", "project.aLib.main", "project.aLib.test");

    LibraryOrderEntry aLib = assertSingleLibraryOrderEntry("project.test", "Gradle: test:aLib:1.0-SNAPSHOT-1");
    assertThat(aLib.getRootFiles(OrderRootType.CLASSES))
      .hasSize(1)
      .allSatisfy(file -> assertEquals("aLib-1.0-SNAPSHOT-1.jar", file.getName()));
    assertThat(aLib.getRootFiles(OrderRootType.SOURCES))
      .hasSize(1)
      .allSatisfy(file -> assertEquals("aLib-1.0-SNAPSHOT-1-sources.jar", file.getName()));
    assertThat(aLib.getRootFiles(JavadocOrderRootType.getInstance()))
      .hasSize(1)
      .allSatisfy(file -> assertEquals("aLib-1.0-SNAPSHOT-1-javadoc.jar", file.getName()));
  }

  @Test
  public void testModifiedSourceSetClasspathFileCollectionDependencies() throws Exception {
    importProject(
      "apply plugin: 'java'\n" +
      "dependencies {\n" +
      "  compile 'junit:junit:4.11'\n" +
      "}\n" +
      "afterEvaluate {\n" +
      "    def mainSourceSet = sourceSets['main']\n" +
      "    def mainClassPath = mainSourceSet.compileClasspath\n" +
      "    def exclusion = mainClassPath.filter { it.name.contains('junit') }\n" +
      "    mainSourceSet.compileClasspath = mainClassPath - exclusion\n" +
      "}"
    );

    assertModules("project", "project.main", "project.test");

    assertModuleLibDeps("project.main", "Gradle: junit:junit:4.11", "Gradle: org.hamcrest:hamcrest-core:1.3");
    assertModuleLibDepScope("project.main", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.main", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);

    assertModuleLibDeps("project.test", "Gradle: junit:junit:4.11", "Gradle: org.hamcrest:hamcrest-core:1.3");
    assertModuleLibDepScope("project.test", "Gradle: org.hamcrest:hamcrest-core:1.3", DependencyScope.COMPILE);
    assertModuleLibDepScope("project.test", "Gradle: junit:junit:4.11", DependencyScope.COMPILE);
  }

  @Test
  public void testCompilationTaskClasspathDependencies() throws Exception {
    importProject(
      new GradleBuildScriptBuilderEx()
        .withJavaPlugin()
        .addPostfix(
          "  configurations {",
          "    custom1",
          "    custom2",
          "  }",
          "  sourceSets {",
          "    customSrc",
          "  }",
          "  dependencies {",
          "    custom1 'junit:junit:4.12'",
          "    custom2 'org.hamcrest:hamcrest-core:1.3'",
          "  }",
          "  compileJava { classpath += configurations.custom1 }",
          "  compileCustomSrcJava {classpath += configurations.custom2 }"
        )
        .generate()
    );

    assertModules("project", "project.main", "project.test", "project.customSrc");
    assertModuleLibDeps("project.main", "Gradle: junit:junit:4.12", "Gradle: org.hamcrest:hamcrest-core:1.3");
    assertModuleLibDeps("project.test");
    assertModuleLibDeps("project.customSrc", "Gradle: org.hamcrest:hamcrest-core:1.3");
  }

  @SuppressWarnings("SameParameterValue")
  private LibraryOrderEntry assertSingleLibraryOrderEntry(String moduleName, String depName) {
    List<LibraryOrderEntry> moduleLibDeps = getModuleLibDeps(moduleName, depName);
    assertThat(moduleLibDeps).hasSize(1);
    return moduleLibDeps.iterator().next();
  }

  private void runTask(String task) {
    ExternalSystemTaskId taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, myProject);
    String projectPath = getProjectPath();
    GradleExecutionSettings settings = new GradleManager().getExecutionSettingsProvider().fun(new Pair<>(myProject, projectPath));
    new GradleTaskManager().executeTasks(
      taskId, Collections.singletonList(task), projectPath, settings, null,
      new ExternalSystemTaskNotificationListenerAdapter() {
        @Override
        public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
          if (stdOut) {
            System.out.print(text);
          }
          else {
            System.err.print(text);
          }
        }
      });
  }

  private static void checkIfSourcesOrJavadocsCanBeAttached(String binaryPath,
                                                            Ref<Boolean> sourceFound,
                                                            Ref<Boolean> docFound) throws IOException {
    Path binaryFileParent = Paths.get(binaryPath).getParent();
    Path grandParentFile = binaryFileParent.getParent();
    Files.walkFileTree(grandParentFile, EnumSet.noneOf(FileVisitOption.class), 2, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (binaryFileParent.equals(dir)) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        return super.preVisitDirectory(dir, attrs);
      }

      @Override
      public FileVisitResult visitFile(Path sourceCandidate, BasicFileAttributes attrs) throws IOException {
        if (!sourceCandidate.getParent().getParent().equals(grandParentFile)) {
          return FileVisitResult.SKIP_SIBLINGS;
        }
        if (attrs.isRegularFile()) {
          String candidateFileName = sourceCandidate.getFileName().toString();
          if (!sourceFound.get() && endsWith(candidateFileName, "-sources.jar")) {
            sourceFound.set(true);
          }
          else if (!docFound.get() && endsWith(candidateFileName, "-javadoc.jar")) {
            docFound.set(true);
          }
        }
        if (sourceFound.get() && docFound.get()) {
          return FileVisitResult.TERMINATE;
        }
        return super.visitFile(sourceCandidate, attrs);
      }
    });
  }
}
