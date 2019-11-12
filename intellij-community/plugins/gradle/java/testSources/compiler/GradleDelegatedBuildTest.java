// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskListener;
import com.intellij.task.ProjectTaskManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.util.PathUtil.toSystemDependentName;
import static com.intellij.util.containers.ContainerUtil.newArrayList;
import static java.util.Arrays.asList;

public class GradleDelegatedBuildTest extends GradleDelegatedBuildTestCase {
  @Test
  public void testDependentModulesOutputRefresh() throws Exception {
    createSettingsFile("include 'api', 'impl' ");

    createProjectSubFile("src/main/resources/dir/file.properties");
    createProjectSubFile("src/test/resources/dir/file-test.properties");

    createProjectSubFile("api/src/main/resources/dir/file-api.properties");
    createProjectSubFile("api/src/test/resources/dir/file-api-test.properties");

    createProjectSubFile("impl/src/main/resources/dir/file-impl.properties");
    createProjectSubFile("impl/src/test/resources/dir/file-impl-test.properties");
    importProject(
      "allprojects {\n" +
      "  apply plugin: 'java'\n" +
      "}\n" +
      "\n" +
      "dependencies {\n" +
      "  compile project(':api')\n" +
      "}\n" +
      "configure(project(':api')) {\n" +
      "  dependencies {\n" +
      "    compile project(':impl')\n" +
      "  }\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test",
                  "project.api", "project.api.main", "project.api.test",
                  "project.impl", "project.impl.main", "project.impl.test");


    EdtTestUtil.runInEdtAndWait(() -> VirtualFileManager.getInstance().syncRefresh());
    PathsList pathsBeforeMake = new PathsList();
    OrderEnumerator.orderEntries(getModule("project.main")).withoutSdk().recursively().runtimeOnly().classes()
      .collectPaths(pathsBeforeMake);
    assertSameElements(pathsBeforeMake.getPathList(), Collections.emptyList());

    compileModules("project.main");
    PathsList pathsAfterMake = new PathsList();
    OrderEnumerator.orderEntries(getModule("project.main")).withoutSdk().recursively().runtimeOnly().classes().collectPaths(pathsAfterMake);
    assertSameElements(pathsAfterMake.getPathList(),
                       toSystemDependentName(path("build/resources/main")),
                       toSystemDependentName(path("api/build/resources/main")),
                       toSystemDependentName(path("impl/build/resources/main")));

    assertCopied("build/resources/main/dir/file.properties");
    assertNotCopied("build/resources/test/dir/file-test.properties");

    assertCopied("api/build/resources/main/dir/file-api.properties");
    assertNotCopied("api/build/resources/test/dir/file-api-test.properties");

    assertCopied("impl/build/resources/main/dir/file-impl.properties");
    assertNotCopied("impl/build/resources/test/dir/file-impl-test.properties");
  }

  @Test
  public void testDirtyOutputPathsCollection() throws Exception {
    createSettingsFile("include 'api', 'impl' ");

    createProjectSubFile("src/main/java/my/pack/App.java",
                         "package my.pack;\n" +
                         "public class App {\n" +
                         "  public int method() { return 42; }" +
                         "}");
    createProjectSubFile("src/test/java/my/pack/AppTest.java",
                         "package my.pack;\n" +
                         "public class AppTest {\n" +
                         "  public void test() { new App().method(); }" +
                         "}");

    createProjectSubFile("api/src/main/java/my/pack/Api.java",
                         "package my.pack;\n" +
                         "public class Api {\n" +
                         "  public int method() { return 42; }" +
                         "}");
    createProjectSubFile("api/src/test/java/my/pack/ApiTest.java",
                         "package my.pack;\n" +
                         "public class ApiTest {}");

    createProjectSubFile("impl/src/main/java/my/pack/Impl.java",
                         "package my.pack;\n" +
                         "import my.pack.Api;\n" +
                         "public class Impl extends Api {}");
    createProjectSubFile("impl/src/test/java/my/pack/ImplTest.java",
                         "package my.pack;\n" +
                         "import my.pack.ApiTest;\n" +
                         "public class ImplTest extends ApiTest {}");

    importProject(
      "allprojects {\n" +
      "  apply plugin: 'java'\n" +
      "}\n" +
      "\n" +
      "dependencies {\n" +
      "  compile project(':impl')\n" +
      "}\n" +
      "configure(project(':impl')) {\n" +
      "  dependencies {\n" +
      "    compile project(':api')\n" +
      "  }\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test",
                  "project.api", "project.api.main", "project.api.test",
                  "project.impl", "project.impl.main", "project.impl.test");

    List<String> dirtyOutputRoots = new ArrayList<>();

    MessageBusConnection connection = myProject.getMessageBus().connect(getTestRootDisposable());
    connection.subscribe(ProjectTaskListener.TOPIC, new ProjectTaskListener() {
      @Override
      public void started(@NotNull ProjectTaskContext context) {
        context.enableCollectionOfGeneratedFiles();
      }

      @Override
      public void finished(@NotNull ProjectTaskManager.Result result) {
        result.getContext().getDirtyOutputPaths()
          .ifPresent(paths -> dirtyOutputRoots.addAll(paths.map(PathUtil::toSystemIndependentName).collect(Collectors.toList())));
      }
    });

    compileModules("project.main");

    String langPart = isGradleOlderThen("4.0") ? "build/classes" : "build/classes/java";
    List<String> expected = newArrayList(path(langPart + "/main"),
                                         path("api/" + langPart + "/main"),
                                         path("impl/" + langPart + "/main"),
                                         path("api/build/libs/api.jar"),
                                         path("impl/build/libs/impl.jar"));

    if (isGradleOlderThen("3.3")) {
      expected.addAll(asList(path("build/dependency-cache"),
                             path("api/build/dependency-cache"),
                             path("impl/build/dependency-cache")));
    }

    if (!isGradleOlderThen("5.2")) {
      expected.addAll(asList(path("build/generated/sources/annotationProcessor/java/main"),
                             path("api/build/generated/sources/annotationProcessor/java/main"),
                             path("impl/build/generated/sources/annotationProcessor/java/main")));
    }

    assertSameElements(dirtyOutputRoots, expected);

    assertCopied(langPart + "/main/my/pack/App.class");
    assertNotCopied(langPart + "/test/my/pack/AppTest.class");

    assertCopied("api/" + langPart + "/main/my/pack/Api.class");
    assertNotCopied("api/" + langPart + "/test/my/pack/ApiTest.class");

    assertCopied("impl/" + langPart + "/main/my/pack/Impl.class");
    assertNotCopied("impl/" + langPart + "/test/my/pack/ImplTest.class");

    //----check incremental make and build dependant module----//
    dirtyOutputRoots.clear();
    createProjectSubFile("src/main/java/my/pack/App.java",
                         "package my.pack;\n" +
                         "public class App {\n" +
                         "  public int method() { return 42; }" +
                         "  public int methodX() { return 42; }" +
                         "}");
    compileModules("project.test");

    expected = newArrayList(path(langPart + "/main"),
                            path(langPart + "/test"));

    if (isGradleOlderThen("3.3")) {
      expected.add(path("build/dependency-cache"));
    }
    if (!isGradleOlderThen("5.2")) {
      expected.addAll(asList(path("build/generated/sources/annotationProcessor/java/main"),
                             path("build/generated/sources/annotationProcessor/java/test")));
    }
    assertUnorderedElementsAreEqual(dirtyOutputRoots, expected);

    assertCopied(langPart + "/main/my/pack/App.class");
    assertCopied(langPart + "/test/my/pack/AppTest.class");

    assertCopied("api/" + langPart + "/main/my/pack/Api.class");
    assertNotCopied("api/" + langPart + "/test/my/pack/ApiTest.class");

    assertCopied("impl/" + langPart + "/main/my/pack/Impl.class");
    assertNotCopied("impl/" + langPart + "/test/my/pack/ImplTest.class");
  }
}
