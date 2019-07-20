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
package org.jetbrains.plugins.gradle.tooling.builder;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.internal.BuildScriptClasspathModelImpl;
import org.jetbrains.plugins.gradle.tooling.internal.ClasspathEntryModelImpl;
import org.jetbrains.plugins.gradle.tooling.util.DependencyTraverser;
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder;
import org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vladislav.Soroka
 */
public class ModelBuildScriptClasspathBuilderImpl extends AbstractModelBuilderService {

  private static final String CLASSPATH_CONFIGURATION_NAME = "classpath";
  private final Map<String, BuildScriptClasspathModelImpl> cache = new ConcurrentHashMap<String, BuildScriptClasspathModelImpl>();
  private SourceSetCachedFinder mySourceSetFinder = null;

  @Override
  public boolean canBuild(String modelName) {
    return BuildScriptClasspathModel.class.getName().equals(modelName);
  }

  @Nullable
  @Override
  public Object buildAll(@NotNull final String modelName, @NotNull final Project project, @NotNull ModelBuilderContext context) {
    BuildScriptClasspathModelImpl buildScriptClasspath = cache.get(project.getPath());
    if (buildScriptClasspath != null) return buildScriptClasspath;

    if (mySourceSetFinder == null) mySourceSetFinder = new SourceSetCachedFinder(context);

    buildScriptClasspath = new BuildScriptClasspathModelImpl();
    final File gradleHomeDir = project.getGradle().getGradleHomeDir();
    buildScriptClasspath.setGradleHomeDir(gradleHomeDir);
    buildScriptClasspath.setGradleVersion(GradleVersion.current().getVersion());

    boolean downloadJavadoc = false;
    boolean downloadSources = true;

    final IdeaPlugin ideaPlugin = project.getPlugins().findPlugin(IdeaPlugin.class);
    if (ideaPlugin != null) {
      final IdeaModule ideaModule = ideaPlugin.getModel().getModule();
      downloadJavadoc = ideaModule.isDownloadJavadoc();
      downloadSources = ideaModule.isDownloadSources();
    }

    Project parent = project.getParent();
    if (parent != null) {
      BuildScriptClasspathModelImpl parentBuildScriptClasspath = (BuildScriptClasspathModelImpl)buildAll(modelName, parent, context);
      if (parentBuildScriptClasspath != null) {
        for (ClasspathEntryModel classpathEntryModel : parentBuildScriptClasspath.getClasspath()) {
          buildScriptClasspath.add(classpathEntryModel);
        }
      }
    }
    Configuration classpathConfiguration = project.getBuildscript().getConfigurations().findByName(CLASSPATH_CONFIGURATION_NAME);
    if (classpathConfiguration == null) return null;

    Collection<ExternalDependency> dependencies = new DependencyResolverImpl(project, false, downloadJavadoc, downloadSources, mySourceSetFinder).resolveDependencies(classpathConfiguration);

    for (ExternalDependency dependency : new DependencyTraverser(dependencies)) {
      if (dependency instanceof ExternalProjectDependency) {
        ExternalProjectDependency projectDependency = (ExternalProjectDependency)dependency;
        Collection<File> projectDependencyArtifacts = projectDependency.getProjectDependencyArtifacts();
        Collection<File> projectDependencyArtifactsSources = projectDependency.getProjectDependencyArtifactsSources();
        buildScriptClasspath.add(new ClasspathEntryModelImpl(
          pathSet(projectDependencyArtifacts),
          pathSet(projectDependencyArtifactsSources),
          Collections.<String>emptySet()
        ));
      }
      else if (dependency instanceof ExternalLibraryDependency) {
        final ExternalLibraryDependency libraryDep = (ExternalLibraryDependency)dependency;
        buildScriptClasspath.add(new ClasspathEntryModelImpl(
          pathSet(libraryDep.getFile()),
          pathSet(libraryDep.getSource()),
          pathSet(libraryDep.getJavadoc())
        ));
      }
      else if (dependency instanceof ExternalMultiLibraryDependency) {
        ExternalMultiLibraryDependency multiLibraryDependency = (ExternalMultiLibraryDependency)dependency;
        buildScriptClasspath.add(new ClasspathEntryModelImpl(
          pathSet(multiLibraryDependency.getFiles()),
          pathSet(multiLibraryDependency.getSources()),
          pathSet(multiLibraryDependency.getJavadoc())
        ));
      }
      else if (dependency instanceof FileCollectionDependency) {
        FileCollectionDependency fileCollectionDependency = (FileCollectionDependency)dependency;
        buildScriptClasspath.add(new ClasspathEntryModelImpl(
          pathSet(fileCollectionDependency.getFiles()),
          Collections.<String>emptySet(),
          Collections.<String>emptySet()
        ));
      }
    }

    cache.put(project.getPath(), buildScriptClasspath);
    return buildScriptClasspath;
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "Project build classpath resolve errors"
    ).withDescription("Unable to resolve additional buildscript classpath dependencies");
  }

  private static Set<String> pathSet(Collection<File> files) {
    if (files.isEmpty()) return Collections.emptySet();
    Set<String> set = new HashSet<String>(files.size());
    for (File file : files) {
      if(file != null) {
        set.add(file.getPath());
      }
    }
    if (set.isEmpty()) return Collections.emptySet();
    if (set.size() == 1) return Collections.singleton(set.iterator().next());
    return set;
  }

  private static Set<String> pathSet(File... files) {
    return pathSet(Arrays.asList(files));
  }
}
