// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.util.Consumer;
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx;
import org.jetbrains.plugins.gradle.importing.GroovyBuilder;
import org.junit.Test;

import java.io.IOException;

public class GradleJpsJavaCompilationTest extends GradleJpsCompilingTestCase {
  @Test
  public void testCustomSourceSetDependencies() throws IOException {
    ExternalProjectsManagerImpl.getInstance(myProject).setStoreExternally(true);
    createProjectSubFile("src/intTest/java/DepTest.java", "class DepTest extends CommonTest {}");
    createProjectSubFile("src/test/java/CommonTest.java", "public class CommonTest {}");
    importProject("apply plugin: 'java'\n" +
                  "sourceSets {\n" +
                  "  intTest {\n" +
                  "     compileClasspath += main.output + test.output" +
                  "  }\n" +
                  "}");
    compileModules("project.main", "project.test", "project.intTest");
  }

  @Test
  public void testDifferentTargetCompatibilityForProjectAndModules() throws IOException {
    ExternalProjectsManagerImpl.getInstance(myProject).setStoreExternally(true);
    createProjectSubFile(
      "src/main/java/Main.java",
      "public class Main {\n" +
      "    public static void main(String[] args) {\n" +
      "        run(() -> System.out.println(\"Hello Home!\"));\n" +
      "    }\n" +
      "\n" +
      "    public static void run(Runnable runnable) {\n" +
      "        runnable.run();\n" +
      "    }\n" +
      "}\n");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withJavaPlugin()
        .withPrefix((Consumer<GroovyBuilder>)it -> it
          .property("sourceCompatibility", 7)
          .property("targetCompatibility", 7))
        .withTaskConfiguration("compileJava", (Consumer<GroovyBuilder>)it -> it
          .property("sourceCompatibility", 8)
          .property("targetCompatibility", 8))
        .generate()
    );
    compileModules("project.main");
  }

  @Override
  protected boolean useDirectoryBasedStorageFormat() {
    return true;
  }
}
