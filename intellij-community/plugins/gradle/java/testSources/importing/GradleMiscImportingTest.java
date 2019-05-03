// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelModuleExtensionImpl;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.TestModuleProperties;
import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class GradleMiscImportingTest extends GradleImportingTestCase {

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
      "apply plugin: 'java'\n" +
      "compileTestJava {\n" +
      "  sourceCompatibility = 1.8\n" +
      "}\n"
    );

    assertModules("project", "project.main", "project.test");
    assertEquals(LanguageLevel.JDK_1_5, getLanguageLevelForModule("project.main"));
    assertEquals(LanguageLevel.JDK_1_8, getLanguageLevelForModule("project.test"));
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
    assertEquals("1.5", getBytecodeTargetLevel("project.main"));
    assertEquals("1.8", getBytecodeTargetLevel("project.test"));

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
    assertTrue(getSdkForModule("project.main") == myJdk);
    assertTrue(getSdkForModule("project.test") == myJdk);
  }

  @Test
  public void testUnloadedModuleImport() throws Exception {
    importProject(
      "apply plugin: 'java'"
    );
    assertModules("project", "project.main", "project.test");

    edt(() -> ModuleManager.getInstance(myProject).setUnloadedModules(Arrays.asList("project.main")));
    assertModules("project", "project.test");

    importProject();
    assertModules("project", "project.test");
  }

  private LanguageLevel getLanguageLevelForModule(final String moduleName) {
    return LanguageLevelModuleExtensionImpl.getInstance(getModule(moduleName)).getLanguageLevel();
  }

  private String getBytecodeTargetLevel(String moduleName) {
    return CompilerConfiguration.getInstance(myProject).getBytecodeTargetLevel(getModule(moduleName));
  }

  private Sdk getSdkForModule(final String moduleName) {
    return ModuleRootManager.getInstance(getModule(moduleName)).getSdk();
  }
}
