/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.jps.model.java.impl;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsElementContainer;
import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.*;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;
import org.jetbrains.jps.model.java.impl.compiler.JpsJavaCompilerConfigurationImpl;
import org.jetbrains.jps.model.java.impl.runConfiguration.JpsApplicationRunConfigurationPropertiesImpl;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationProperties;
import org.jetbrains.jps.model.java.runConfiguration.JpsApplicationRunConfigurationState;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleReference;
import org.jetbrains.jps.model.module.JpsTestModuleProperties;
import org.jetbrains.jps.model.module.impl.JpsTestModulePropertiesImpl;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class JpsJavaExtensionServiceImpl extends JpsJavaExtensionService {
  @NotNull
  @Override
  public JpsJavaProjectExtension getOrCreateProjectExtension(@NotNull JpsProject project) {
    return project.getContainer().getOrSetChild(JavaProjectExtensionRole.INSTANCE);
  }

  @Nullable
  @Override
  public JpsJavaProjectExtension getProjectExtension(@NotNull JpsProject project) {
    return project.getContainer().getChild(JavaProjectExtensionRole.INSTANCE);
  }

  @NotNull
  @Override
  public JpsJavaModuleExtension getOrCreateModuleExtension(@NotNull JpsModule module) {
    return module.getContainer().getOrSetChild(JavaModuleExtensionRole.INSTANCE);
  }

  @NotNull
  @Override
  public JpsJavaDependencyExtension getOrCreateDependencyExtension(@NotNull JpsDependencyElement dependency) {
    return dependency.getContainer().getOrSetChild(JpsJavaDependencyExtensionRole.INSTANCE);
  }

  @Override
  public JpsJavaDependencyExtension getDependencyExtension(@NotNull JpsDependencyElement dependency) {
    return dependency.getContainer().getChild(JpsJavaDependencyExtensionRole.INSTANCE);
  }

  @Override
  @Nullable
  public JpsJavaModuleExtension getModuleExtension(@NotNull JpsModule module) {
    return module.getContainer().getChild(JavaModuleExtensionRole.INSTANCE);
  }

  @Override
  @NotNull
  public ExplodedDirectoryModuleExtension getOrCreateExplodedDirectoryExtension(@NotNull JpsModule module) {
    return module.getContainer().getOrSetChild(ExplodedDirectoryModuleExtensionImpl.ExplodedDirectoryModuleExtensionRole.INSTANCE);
  }

  @Override
  @Nullable
  public ExplodedDirectoryModuleExtension getExplodedDirectoryExtension(@NotNull JpsModule module) {
    return module.getContainer().getChild(ExplodedDirectoryModuleExtensionImpl.ExplodedDirectoryModuleExtensionRole.INSTANCE);
  }

  @NotNull
  @Override
  public List<JpsDependencyElement> getDependencies(JpsModule module, JpsJavaClasspathKind classpathKind, boolean exportedOnly) {
    final List<JpsDependencyElement> result = new ArrayList<>();
    for (JpsDependencyElement dependencyElement : module.getDependenciesList().getDependencies()) {
      final JpsJavaDependencyExtension extension = getDependencyExtension(dependencyElement);
      if (extension == null || extension.getScope().isIncludedIn(classpathKind) && (!exportedOnly || extension.isExported())) {
        result.add(dependencyElement);
      }
    }
    return result;
  }

  @Override
  public LanguageLevel getLanguageLevel(JpsModule module) {
    final JpsJavaModuleExtension moduleExtension = getModuleExtension(module);
    if (moduleExtension == null) return null;
    final LanguageLevel languageLevel = moduleExtension.getLanguageLevel();
    if (languageLevel != null) return languageLevel;
    final JpsJavaProjectExtension projectExtension = getProjectExtension(module.getProject());
    return projectExtension != null ? projectExtension.getLanguageLevel() : null;
  }

  @Override
  public String getOutputUrl(JpsModule module, boolean forTests) {
    final JpsJavaModuleExtension extension = getModuleExtension(module);
    if (extension == null) return null;
    if (extension.isInheritOutput()) {
      JpsJavaProjectExtension projectExtension = getProjectExtension(module.getProject());
      if (projectExtension == null) return null;
      final String url = projectExtension.getOutputUrl();
      if (url == null) return null;
      return url + "/" + (forTests ? "test" : "production") + "/" + module.getName();
    }
    return forTests ? extension.getTestOutputUrl() : extension.getOutputUrl();
  }

  @Nullable
  @Override
  public File getOutputDirectory(JpsModule module, boolean forTests) {
    String url = getOutputUrl(module, forTests);
    return url != null ? JpsPathUtil.urlToFile(url) : null;
  }

  @Override
  public JpsTypedLibrary<JpsSdk<JpsDummyElement>> addJavaSdk(@NotNull JpsGlobal global, @NotNull String name, @NotNull String homePath) {
    JdkVersionDetector.JdkVersionInfo jdkInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(homePath);
    assert jdkInfo != null : homePath;
    String version = JdkVersionDetector.formatVersionString(jdkInfo.version);
    JpsTypedLibrary<JpsSdk<JpsDummyElement>> sdk = global.addSdk(name, homePath, version, JpsJavaSdkType.INSTANCE);
    File homeDir = new File(FileUtil.toSystemDependentName(homePath));
    List<File> roots = JavaSdkUtil.getJdkClassesRoots(homeDir, false);
    for (File root : roots) {
      sdk.addRoot(root, JpsOrderRootType.COMPILED);
    }
    return sdk;
  }

  @Nullable
  @Override
  public JpsJavaCompilerConfiguration getCompilerConfiguration(@NotNull JpsProject project) {
    return project.getContainer().getChild(JpsJavaCompilerConfigurationImpl.ROLE);
  }

  @NotNull
  @Override
  public JpsJavaCompilerConfiguration getOrCreateCompilerConfiguration(@NotNull JpsProject project) {
    JpsJavaCompilerConfiguration configuration = getCompilerConfiguration(project);
    if (configuration == null) {
      configuration = project.getContainer().setChild(JpsJavaCompilerConfigurationImpl.ROLE, new JpsJavaCompilerConfigurationImpl());
    }
    return configuration;
  }

  @Nullable
  @Override
  public JpsTestModuleProperties getTestModuleProperties(@NotNull JpsModule module) {
    return module.getContainer().getChild(JpsTestModulePropertiesImpl.ROLE);
  }

  @Override
  public void setTestModuleProperties(@NotNull JpsModule module, @NotNull JpsModuleReference productionModuleReference) {
    module.getContainer().setChild(JpsTestModulePropertiesImpl.ROLE, new JpsTestModulePropertiesImpl(productionModuleReference));
  }

  @NotNull
  @Override
  public JpsSdkReference<JpsDummyElement> createWrappedJavaSdkReference(@NotNull JpsJavaSdkTypeWrapper sdkType,
                                                                        @NotNull JpsSdkReference<?> wrapperReference) {
    return new JpsWrappedJavaSdkReferenceImpl(sdkType, wrapperReference);
  }

  @NotNull
  @Override
  public JpsApplicationRunConfigurationProperties createRunConfigurationProperties(JpsApplicationRunConfigurationState state) {
    return new JpsApplicationRunConfigurationPropertiesImpl(state);
  }

  @NotNull
  @Override
  public JavaSourceRootProperties createSourceRootProperties(@NotNull String packagePrefix, boolean isGenerated) {
    return new JavaSourceRootProperties(packagePrefix, isGenerated);
  }

  @NotNull
  @Override
  public JavaSourceRootProperties createSourceRootProperties(@NotNull String packagePrefix) {
    return createSourceRootProperties(packagePrefix, false);
  }

  @NotNull
  @Override
  public JavaResourceRootProperties createResourceRootProperties(@NotNull String relativeOutputPath, boolean forGeneratedResource) {
    return new JavaResourceRootProperties(relativeOutputPath, forGeneratedResource);
  }

  @Override
  @NotNull
  public JpsProductionModuleOutputPackagingElement createProductionModuleOutput(@NotNull JpsModuleReference moduleReference) {
    return new JpsProductionModuleOutputPackagingElementImpl(moduleReference);
  }

  @Override
  @NotNull
  public JpsProductionModuleSourcePackagingElement createProductionModuleSource(@NotNull JpsModuleReference moduleReference) {
    return new JpsProductionModuleSourcePackagingElementImpl(moduleReference);
  }

  @Override
  @NotNull
  public JpsTestModuleOutputPackagingElement createTestModuleOutput(@NotNull JpsModuleReference moduleReference) {
    return new JpsTestModuleOutputPackagingElementImpl(moduleReference);
  }

  @Override
  public JpsJavaDependenciesEnumerator enumerateDependencies(Collection<JpsModule> modules) {
    return new JpsJavaDependenciesEnumeratorImpl(modules);
  }

  @Override
  protected JpsJavaDependenciesEnumerator enumerateDependencies(JpsProject project) {
    return new JpsJavaDependenciesEnumeratorImpl(project.getModules());
  }

  @Override
  protected JpsJavaDependenciesEnumerator enumerateDependencies(JpsModule module) {
    return new JpsJavaDependenciesEnumeratorImpl(Collections.singletonList(module));
  }

  @NotNull
  @Override
  public JavaModuleIndex getJavaModuleIndex(@NotNull JpsProject project) {
    JpsElementContainer container = project.getContainer();
    JavaModuleIndex index = container.getChild(JavaModuleIndexRole.INSTANCE);
    if (index == null) {
      index = container.setChild(JavaModuleIndexRole.INSTANCE, getOrCreateCompilerConfiguration(project).getCompilerExcludes());
    }
    return index;
  }
}