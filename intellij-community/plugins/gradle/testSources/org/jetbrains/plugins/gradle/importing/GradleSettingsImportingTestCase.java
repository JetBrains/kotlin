// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.UnknownConfigurationType;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.settings.FacetConfigurationImporter;
import com.intellij.openapi.externalSystem.service.project.settings.RunConfigurationImporter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.util.registry.Registry;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;

import java.util.*;

import static com.intellij.openapi.externalSystem.service.project.settings.ConfigurationDataService.EXTERNAL_SYSTEM_CONFIGURATION_IMPORT_ENABLED;

public abstract class GradleSettingsImportingTestCase extends GradleImportingTestCase {
  public static final String IDEA_EXT_PLUGIN_VERSION = "0.5";

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    Registry.get(EXTERNAL_SYSTEM_CONFIGURATION_IMPORT_ENABLED).setValue(true);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    try {
      Registry.get(EXTERNAL_SYSTEM_CONFIGURATION_IMPORT_ENABLED).resetToDefault();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  /**
   * This method needed for printing debug information about project
   */
  @SuppressWarnings("unused")
  protected void printProjectStructure() {
    ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    for (Module module : moduleManager.getModules()) {
      System.out.println(module);
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      for (ContentEntry contentEntry : moduleRootManager.getContentEntries()) {
        System.out.println("content root = " + contentEntry.getUrl());
        for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
          System.out.println("source root = " + sourceFolder);
          String packagePrefix = sourceFolder.getPackagePrefix();
          if (packagePrefix.isEmpty()) continue;
          System.out.println("package prefix = " + packagePrefix);
        }
      }
    }
  }

  protected boolean extPluginVersionIsAtLeast(@NotNull final String version) {
    return Version.parseVersion(IDEA_EXT_PLUGIN_VERSION).compareTo(Version.parseVersion(version)) >= 0;
  }

  @NotNull
  @Override
  protected String injectRepo(String config) {
    return config; // Do not inject anything
  }

  @NotNull
  protected String withGradleIdeaExtPlugin(@NonNls @Language("Groovy") String script) {
    return
      "plugins {\n" +
      "  id \"org.jetbrains.gradle.plugin.idea-ext\" version \"" + IDEA_EXT_PLUGIN_VERSION + "\"\n" +
      "}\n" +
      script;
  }

  protected void assertSourceNotExists(@NotNull String moduleName, @NotNull String sourcePath) {
    SourceFolder sourceFolder = findSource(moduleName, sourcePath);
    assertNull("Source folder " + sourcePath + " found in module " + moduleName + "but shouldn't", sourceFolder);
  }

  protected void assertSourcePackagePrefix(@NotNull String moduleName, @NotNull String sourcePath, @NotNull String packagePrefix) {
    SourceFolder sourceFolder = findSource(moduleName, sourcePath);
    assertNotNull("Source folder " + sourcePath + " not found in module " + moduleName, sourceFolder);
    assertEquals(packagePrefix, sourceFolder.getPackagePrefix());
  }

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{BASE_GRADLE_VERSION}});
  }
}


class TestRunConfigurationImporter implements RunConfigurationImporter {

  private final String myTypeName;
  private final Map<String, Map<String, Object>> myConfigs = new HashMap<>();

  TestRunConfigurationImporter(@NotNull String typeName) {
    myTypeName = typeName;
  }

  @Override
  public void process(@NotNull Project project, @NotNull RunConfiguration runConfig, @NotNull Map<String, Object> cfg,
                      @NotNull IdeModifiableModelsProvider modelsProvider) {
    myConfigs.put(runConfig.getName(), cfg);
  }

  @Override
  public boolean canImport(@NotNull String typeName) {
    return myTypeName.equals(typeName);
  }

  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return UnknownConfigurationType.getInstance();
  }

  public Map<String, Map<String, Object>> getConfigs() {
    return myConfigs;
  }
}

class TestFacetConfigurationImporter implements FacetConfigurationImporter<Facet> {

  private final String myTypeName;

  private final Map<String, Map<String, Object>> myConfigs = new HashMap<>();

  TestFacetConfigurationImporter(@NotNull String typeName) {
    myTypeName = typeName;
  }

  @NotNull
  @Override
  public Collection<Facet> process(@NotNull Module module,
                                   @NotNull String name,
                                   @NotNull Map<String, Object> cfg,
                                   @NotNull FacetManager facetManager) {
    myConfigs.put(name, cfg);
    return Collections.emptySet();
  }

  @Override
  public boolean canHandle(@NotNull String typeName) {
    return myTypeName.equals(typeName);
  }

  public Map<String, Map<String, Object>> getConfigs() {
    return myConfigs;
  }
}
