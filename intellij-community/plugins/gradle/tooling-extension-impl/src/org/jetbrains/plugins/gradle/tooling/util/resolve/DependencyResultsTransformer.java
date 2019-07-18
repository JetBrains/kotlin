// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util.resolve;

import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.*;
import org.gradle.api.artifacts.result.*;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.component.Artifact;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.internal.impldep.com.google.common.collect.Multimap;
import org.gradle.internal.impldep.com.google.common.io.Files;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

import static org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl.*;

public class DependencyResultsTransformer {

  private static final boolean is31orBetter = GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("3.1")) >= 0;
  private static final boolean is46rBetter = is31orBetter && GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.6")) >= 0;


  private final Project myProject;
  private final SourceSetCachedFinder mySourceSetFinder;
  private final Multimap<ModuleVersionIdentifier, ResolvedArtifact> artifactMap;
  private final Map<ComponentIdentifier, ComponentArtifactsResult> componentResultsMap;
  private final Multimap<ModuleComponentIdentifier, ProjectDependency> configurationProjectDependencies;
  private final String scope;
  private final Set<File> resolvedDepsFiles = new HashSet<File>();

  private final Set<DependencyResult> handledDependencyResults = new HashSet<DependencyResult>();
  private final Set<ComponentResultKey> myVisitedComponentResults = new HashSet<ComponentResultKey>();

  public DependencyResultsTransformer(@NotNull final Project project,
                               @NotNull final SourceSetCachedFinder sourceSetFinder,
                               @NotNull final Multimap<ModuleVersionIdentifier, ResolvedArtifact> artifactMap,
                               @NotNull final Map<ComponentIdentifier, ComponentArtifactsResult> auxiliaryArtifactsMap,
                               @NotNull final Multimap<ModuleComponentIdentifier, ProjectDependency> configurationProjectDependencies,
                               @Nullable final String scope) {
    myProject = project;
    mySourceSetFinder = sourceSetFinder;

    this.artifactMap = artifactMap;
    componentResultsMap = auxiliaryArtifactsMap;
    this.configurationProjectDependencies = configurationProjectDependencies;
    this.scope = scope;
  }

  public Set<File> getResolvedDepsFiles() {
    return resolvedDepsFiles;
  }

  Set<ExternalDependency> buildExternalDependencies(Collection<? extends DependencyResult> gradleDependencies) {

    Set<ExternalDependency> dependencies = new LinkedHashSet<ExternalDependency>();
    for (DependencyResult dependencyResult : gradleDependencies) {

      // dependency cycles check
      if (handledDependencyResults.add(dependencyResult)) {
        if (dependencyResult instanceof ResolvedDependencyResult) {
          dependencies.addAll(processResolvedResult((ResolvedDependencyResult)dependencyResult));
        }

        if (dependencyResult instanceof UnresolvedDependencyResult) {
          ComponentSelector attempted = ((UnresolvedDependencyResult)dependencyResult).getAttempted();
          if (attempted instanceof ModuleComponentSelector) {
            final ModuleComponentSelector attemptedMCSelector = (ModuleComponentSelector)attempted;
            final DefaultUnresolvedExternalDependency dependency = new DefaultUnresolvedExternalDependency();
            dependency.setName(attemptedMCSelector.getModule());
            dependency.setGroup(attemptedMCSelector.getGroup());
            dependency.setVersion(attemptedMCSelector.getVersion());
            dependency.setScope(scope);
            dependency.setFailureMessage(((UnresolvedDependencyResult)dependencyResult).getFailure().getMessage());

            dependencies.add(dependency);
          }
        }
      }
    }

    return dependencies;
  }

  private Set<ExternalDependency> processResolvedResult(ResolvedDependencyResult dependencyResult) {
    Set<ExternalDependency> result = new LinkedHashSet<ExternalDependency>();

    final ResolvedComponentResult componentResult = dependencyResult.getSelected();

    if (!myVisitedComponentResults.add(getKey(componentResult))) {
      return Collections.emptySet();
    }

    final ComponentIdentifier resultId = componentResult.getId();
    ModuleComponentIdentifier componentIdentifier = resultId instanceof ModuleComponentIdentifier
                                                    ? (ModuleComponentIdentifier)resultId
                                                    : toComponentIdentifier(componentResult.getModuleVersion());

    String name = componentResult.getModuleVersion().getName();
    String group = componentResult.getModuleVersion().getGroup();
    String version = componentResult.getModuleVersion().getVersion();
    String selectionReason = componentResult.getSelectionReason().getDescription();

    boolean resolveFromArtifacts = resultId instanceof ModuleComponentIdentifier;

    if (resultId instanceof ProjectComponentIdentifier) {
      ProjectComponentIdentifier projectComponentIdentifier = (ProjectComponentIdentifier)resultId;
      Collection<ProjectDependency> projectDependencies = configurationProjectDependencies.get(componentIdentifier);
      Collection<Configuration> dependencyConfigurations;
      String projectPath = projectComponentIdentifier.getProjectPath();
      boolean currentBuild;
      if (is31orBetter) {
        currentBuild = projectComponentIdentifier.getBuild().isCurrentBuild();
      }
      else {
        currentBuild = true;
      }

      if (projectDependencies.isEmpty()) {
        Project dependencyProject = myProject.findProject(projectPath);
        if (dependencyProject != null && currentBuild) {
          Configuration dependencyProjectConfiguration =
            dependencyProject.getConfigurations().findByName(Dependency.DEFAULT_CONFIGURATION);
          if (dependencyProjectConfiguration != null) {
            dependencyConfigurations = Collections.singleton(dependencyProjectConfiguration);
          } else {
            dependencyConfigurations = Collections.emptySet();
          }
        }
        else {
          dependencyConfigurations = Collections.emptySet();
          resolveFromArtifacts = true;
          selectionReason = "composite build substitution";
        }
      }
      else {
        dependencyConfigurations = new ArrayList<Configuration>();
        for (ProjectDependency dependency : projectDependencies) {
          Configuration targetConfiguration = getTargetConfiguration(dependency);
          if(targetConfiguration != null) {
            dependencyConfigurations.add(targetConfiguration);
          }
        }
      }

      for (Configuration it : dependencyConfigurations) {
        DefaultExternalProjectDependency dependency =
          createProjectDependency(dependencyResult, componentResult, projectPath, it);

        if (!componentResult.equals(dependencyResult.getFrom())) {
          dependency.getDependencies().addAll(
            buildExternalDependencies(componentResult.getDependencies())
          );
        }
        result.add(dependency);
        resolvedDepsFiles.addAll(dependency.getProjectDependencyArtifacts());

        if (!it.getName().equals(Dependency.DEFAULT_CONFIGURATION)) {
          List<File> files = new ArrayList<File>();
          PublishArtifactSet artifacts = it.getArtifacts();
          if (artifacts != null && !artifacts.isEmpty()) {
            PublishArtifact artifact = artifacts.iterator().next();
            final MetaProperty taskProperty = DefaultGroovyMethods.hasProperty(artifact, "archiveTask");
            if (taskProperty != null && (taskProperty.getProperty(artifact) instanceof AbstractArchiveTask)) {

              AbstractArchiveTask archiveTask = (AbstractArchiveTask)taskProperty.getProperty(artifact);
              resolvedDepsFiles.add(new File(archiveTask.getDestinationDir(), archiveTask.getArchiveName()));


              try {
                final Method mainSpecGetter = AbstractCopyTask.class.getDeclaredMethod("getMainSpec");
                mainSpecGetter.setAccessible(true);
                Object mainSpec = mainSpecGetter.invoke(archiveTask);

                final List<MetaMethod> sourcePathGetters =
                  DefaultGroovyMethods.respondsTo(mainSpec, "getSourcePaths", new Object[]{});
                if (!sourcePathGetters.isEmpty()) {
                  Set<Object> sourcePaths = (Set<Object>)sourcePathGetters.get(0).doMethodInvoke(mainSpec, new Object[]{});
                  if (sourcePaths != null) {
                    for (Object path : sourcePaths) {
                      if (path instanceof String) {
                        File file = new File((String)path);
                        if (file.isAbsolute()) {
                          files.add(file);
                        }
                      }
                      else if (path instanceof SourceSetOutput) {
                        files.addAll(((SourceSetOutput)path).getFiles());
                      }
                    }
                  }
                }
              }
              catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          }

          if (!files.isEmpty()) {
            final DefaultFileCollectionDependency fileCollectionDependency = new DefaultFileCollectionDependency(files);
            fileCollectionDependency.setScope(scope);
            result.add(fileCollectionDependency);
            resolvedDepsFiles.addAll(files);
          }
        }
      }
    }

    if (resolveFromArtifacts) {
      Collection<ResolvedArtifact> artifacts = artifactMap.get(componentResult.getModuleVersion());

      if (artifacts != null && artifacts.isEmpty()) {
        result.addAll(
          buildExternalDependencies(componentResult.getDependencies())
        );
      }

      boolean first = true;

      if (artifacts != null) {
        for (ResolvedArtifact artifact : artifacts) {
          String packaging = artifact.getExtension() != null ? artifact.getExtension() : "jar";
          String classifier = artifact.getClassifier();
          final ExternalDependency dependency;
          if (isProjectDependencyArtifact(artifact)) {
            ProjectComponentIdentifier artifactComponentIdentifier =
              (ProjectComponentIdentifier)artifact.getId().getComponentIdentifier();

            dependency = new DefaultExternalProjectDependency();
            DefaultExternalProjectDependency dDep = (DefaultExternalProjectDependency)dependency;
            dDep.setName(name);
            dDep.setGroup(group);
            dDep.setVersion(version);
            dDep.setScope(scope);
            dDep.setSelectionReason(selectionReason);
            dDep.setProjectPath(artifactComponentIdentifier.getProjectPath());
            dDep.setConfigurationName(Dependency.DEFAULT_CONFIGURATION);

            Collection<ResolvedArtifact> resolvedArtifacts = artifactMap.get(componentResult.getModuleVersion());
            List<File> files = new ArrayList<File>(resolvedArtifacts.size());
            for (ResolvedArtifact resolvedArtifact : resolvedArtifacts) {
              files.add(resolvedArtifact.getFile());
            }
            dDep.setProjectDependencyArtifacts(files);
            setProjectDependencyArtifactsSources(dDep, files, mySourceSetFinder);
            resolvedDepsFiles.addAll(dDep.getProjectDependencyArtifacts());
          }
          else {
            dependency = new DefaultExternalLibraryDependency();
            DefaultExternalLibraryDependency dDep = (DefaultExternalLibraryDependency)dependency;
            dDep.setName(name);
            dDep.setGroup(group);
            dDep.setPackaging(packaging);
            dDep.setClassifier(classifier);
            dDep.setVersion(version);
            dDep.setScope(scope);
            dDep.setSelectionReason(selectionReason);
            dDep.setFile(artifact.getFile());

            ComponentArtifactsResult artifactsResult = componentResultsMap.get(componentIdentifier);
            if (artifactsResult != null) {
              ResolvedArtifactResult sourcesResult = findMatchingArtifact(artifact, artifactsResult, SourcesArtifact.class);
              if (sourcesResult != null) {
                ((DefaultExternalLibraryDependency)dependency).setSource(sourcesResult.getFile());
              }

              ResolvedArtifactResult javadocResult = findMatchingArtifact(artifact, artifactsResult, JavadocArtifact.class);
              if (javadocResult != null) {
                ((DefaultExternalLibraryDependency)dependency).setJavadoc(javadocResult.getFile());
              }
            }
          }

          if (first) {
            dependency.getDependencies().addAll(
              buildExternalDependencies(componentResult.getDependencies())
            );
            first = false;
          }

          result.add(dependency);
          resolvedDepsFiles.add(artifact.getFile());
        }
      }
    }

    return result;
  }

  private ComponentResultKey getKey(ResolvedComponentResult result) {
    if (is46rBetter) {
      return new AttributesBasedKey(result.getId(), result.getVariant().getAttributes());
    } else {
      return new ComponentIdKey(result.getId());
    }
  }

  private interface ComponentResultKey{}

  private static class ComponentIdKey implements ComponentResultKey {
    private final ComponentIdentifier myId;

    ComponentIdKey(ComponentIdentifier id) {
      myId = id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ComponentIdKey key = (ComponentIdKey)o;

      if (!myId.equals(key.myId)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myId.hashCode();
    }
  }

  private static class AttributesBasedKey implements ComponentResultKey {
    private final ComponentIdentifier myId;
    private final AttributeContainer myAttributes;

    AttributesBasedKey(ComponentIdentifier id, AttributeContainer attributes) {
      myId = id;
      myAttributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AttributesBasedKey key = (AttributesBasedKey)o;

      if (!myId.equals(key.myId)) return false;
      if (!myAttributes.equals(key.myAttributes)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myId.hashCode();
      result = 31 * result + myAttributes.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "AttributesBasedKey{" +
             "myId=" + myId +
             ", myAttributes=" + myAttributes +
             '}';
    }
  }


  @NotNull
  private DefaultExternalProjectDependency createProjectDependency(DependencyResult dependencyResult,
                                                                   ResolvedComponentResult componentResult,
                                                                   String projectPath,
                                                                   Configuration it) {
    String name = componentResult.getModuleVersion().getName();
    String group = componentResult.getModuleVersion().getGroup();
    String version = componentResult.getModuleVersion().getVersion();
    String selectionReason = componentResult.getSelectionReason().getDescription();

    DefaultExternalProjectDependency dependency = new DefaultExternalProjectDependency();
    dependency.setName(name);
    dependency.setGroup(group);
    dependency.setVersion(version);
    dependency.setScope(scope);
    dependency.setSelectionReason(selectionReason);
    dependency.setProjectPath(projectPath);
    dependency.setConfigurationName(it.getName());
    Set<File> artifactsFiles = new LinkedHashSet<File>(it.getAllArtifacts().getFiles().getFiles());
    dependency.setProjectDependencyArtifacts(artifactsFiles);
    setProjectDependencyArtifactsSources(dependency, artifactsFiles, mySourceSetFinder);


    if (it.getArtifacts().size() == 1) {
      PublishArtifact publishArtifact = it.getAllArtifacts().iterator().next();
      dependency.setClassifier(publishArtifact.getClassifier());
      dependency.setPackaging(publishArtifact.getExtension() != null ? publishArtifact.getExtension() : "jar");
    }
    return dependency;
  }

  @Nullable
  private static ResolvedArtifactResult findMatchingArtifact(ResolvedArtifact artifact,
                                                             ComponentArtifactsResult componentArtifacts,
                                                             Class<? extends Artifact> artifactType) {
    String baseName = Files.getNameWithoutExtension(artifact.getFile().getName());
    Set<ArtifactResult> artifactResults = componentArtifacts.getArtifacts(artifactType);

    if (artifactResults.size() == 1) {
      ArtifactResult artifactResult = artifactResults.iterator().next();
      return artifactResult instanceof ResolvedArtifactResult ? (ResolvedArtifactResult)artifactResult : null;
    }

    for (ArtifactResult result : artifactResults) {
      if (result instanceof ResolvedArtifactResult && ((ResolvedArtifactResult)result).getFile().getName().startsWith(baseName)) {
        return (ResolvedArtifactResult)result;
      }
    }
    return null;
  }

  private static void setProjectDependencyArtifactsSources(DefaultExternalProjectDependency projectDependency,
                                                           Collection<File> artifactFiles,
                                                           SourceSetCachedFinder sourceSetFinder) {
    List<File> artifactSources = new ArrayList<File>();
    for (File artifactFile : artifactFiles) {
      Set<File> sources = sourceSetFinder.findSourcesByArtifact(artifactFile.getPath());
      if (sources != null) {
        artifactSources.addAll(sources);
      }
    }
    projectDependency.setProjectDependencyArtifactsSources(artifactSources);
  }
}
