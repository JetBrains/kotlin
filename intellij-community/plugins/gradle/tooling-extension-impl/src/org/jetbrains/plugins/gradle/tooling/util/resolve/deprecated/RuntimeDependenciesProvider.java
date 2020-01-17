// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util.resolve.deprecated;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.tooling.util.DependencyResolver;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * @deprecated use org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl
 */
@Deprecated
public class RuntimeDependenciesProvider {
  public static final String SCOPE = DependencyResolver.RUNTIME_SCOPE;

  private final SourceSet mySourceSet;
  private final Project myProject;
  private Configuration myConfiguration;
  private Collection<ExternalDependency> myDependencies;
  private Collection<File> myFiles;
  private Set<File> myConfigurationFiles = null;

  public RuntimeDependenciesProvider(SourceSet sourceSet,
                                     Project project) {
    mySourceSet = sourceSet;
    myProject = project;
  }

  public Configuration getConfiguration() {
    return myConfiguration;
  }

  public Set<File> getConfigurationFiles() {
    if (myConfigurationFiles == null) {
      myConfigurationFiles = myConfiguration.getResolvedConfiguration().getLenientConfiguration().getFiles(Specs.SATISFIES_ALL);
    }
    return myConfigurationFiles;
  }

  public Collection<ExternalDependency> getDependencies() {
    return myDependencies;
  }

  public Collection<File> getFiles() {
    return myFiles;
  }

  public RuntimeDependenciesProvider resolve(DeprecatedDependencyResolver resolver) {
    String runtimeConfigurationName = mySourceSet.getRuntimeConfigurationName();
    Configuration runtimeClasspathConfiguration = myProject.getConfigurations().findByName(runtimeConfigurationName + "Classpath");
    Configuration originRuntimeConfiguration = myProject.getConfigurations().findByName(runtimeConfigurationName);
    myConfiguration = runtimeClasspathConfiguration != null ? runtimeClasspathConfiguration : originRuntimeConfiguration;

    ExternalDepsResolutionResult externalDepsResolutionResult = resolver.resolveDependencies(myConfiguration, SCOPE);
    myDependencies = externalDepsResolutionResult.getExternalDeps();
    myFiles = externalDepsResolutionResult.getResolvedFiles();
    return this;
  }
}
