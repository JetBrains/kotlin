// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util.resolve;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.*;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.component.Artifact;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.internal.impldep.com.google.common.collect.ArrayListMultimap;
import org.gradle.internal.impldep.com.google.common.collect.HashMultimap;
import org.gradle.internal.impldep.com.google.common.collect.Multimap;
import org.gradle.internal.resolve.ModuleVersionResolveException;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.model.FileCollectionDependency;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.tooling.util.DependencyResolver;
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder;
import org.jetbrains.plugins.gradle.tooling.util.resolve.deprecated.DeprecatedDependencyResolver;

import java.io.File;
import java.util.*;

import static java.util.Collections.*;
import static org.jetbrains.plugins.gradle.tooling.util.resolve.deprecated.DeprecatedDependencyResolver.findArtifactSources;
import static org.jetbrains.plugins.gradle.tooling.util.resolve.deprecated.DeprecatedDependencyResolver.toComponentIdentifier;

/**
 * @author Vladislav.Soroka
 */
public class DependencyResolverImpl implements DependencyResolver {
  private static final boolean IS_NEW_DEPENDENCY_RESOLUTION_APPLICABLE =
    GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.5")) >= 0;

  @NotNull
  private final Project myProject;
  private final boolean myDownloadJavadoc;
  private final boolean myDownloadSources;
  @NotNull
  private final SourceSetCachedFinder mySourceSetFinder;

  /**
   * @deprecated use constructor below
   */
  @SuppressWarnings("unused")
  @Deprecated
  public DependencyResolverImpl(@NotNull Project project,
                                boolean isPreview,
                                boolean downloadJavadoc,
                                boolean downloadSources,
                                SourceSetCachedFinder sourceSetFinder) {
    this(project, downloadJavadoc, downloadSources, sourceSetFinder);
  }

  public DependencyResolverImpl(@NotNull Project project,
                                boolean downloadJavadoc,
                                boolean downloadSources,
                                @NotNull
                                  SourceSetCachedFinder sourceSetFinder) {
    myProject = project;
    myDownloadJavadoc = downloadJavadoc;
    myDownloadSources = downloadSources;
    mySourceSetFinder = sourceSetFinder;
  }

  @ApiStatus.Internal
  public static boolean isIsNewDependencyResolutionApplicable() {
    return IS_NEW_DEPENDENCY_RESOLUTION_APPLICABLE;
  }

  @Override
  public Collection<ExternalDependency> resolveDependencies(@Nullable String configurationName) {
    if (!IS_NEW_DEPENDENCY_RESOLUTION_APPLICABLE) {
      return new DeprecatedDependencyResolver(myProject, false, myDownloadJavadoc, myDownloadSources, mySourceSetFinder)
        .resolveDependencies(configurationName);
    }
    if (configurationName == null) return emptyList();
    Collection<ExternalDependency> dependencies = resolveDependencies(myProject.getConfigurations().findByName(configurationName), null);
    int order = 0;
    for (ExternalDependency dependency : dependencies) {
      ((AbstractExternalDependency)dependency).setClasspathOrder(++order);
    }
    return dependencies;
  }

  @Override
  public Collection<ExternalDependency> resolveDependencies(@Nullable Configuration configuration) {
    if (!IS_NEW_DEPENDENCY_RESOLUTION_APPLICABLE) {
      return new DeprecatedDependencyResolver(myProject, false, myDownloadJavadoc, myDownloadSources, mySourceSetFinder)
        .resolveDependencies(configuration);
    }

    Collection<ExternalDependency> dependencies = resolveDependencies(configuration, null);
    int order = 0;
    for (ExternalDependency dependency : dependencies) {
      ((AbstractExternalDependency)dependency).setClasspathOrder(++order);
    }
    return dependencies;
  }

  @Override
  public Collection<ExternalDependency> resolveDependencies(@NotNull SourceSet sourceSet) {
    if (!IS_NEW_DEPENDENCY_RESOLUTION_APPLICABLE) {
      return new DeprecatedDependencyResolver(myProject, false, myDownloadJavadoc, myDownloadSources, mySourceSetFinder)
        .resolveDependencies(sourceSet);
    }

    Collection<ExternalDependency> result = new ArrayList<ExternalDependency>();

    // resolve compile dependencies
    FileCollection compileClasspath = sourceSet.getCompileClasspath();
    Collection<ExternalDependency> compileDependencies = getDependencies(compileClasspath, COMPILE_SCOPE);
    // resolve runtime dependencies
    FileCollection runtimeClasspath = sourceSet.getRuntimeClasspath();
    Collection<ExternalDependency> runtimeDependencies = getDependencies(runtimeClasspath, RUNTIME_SCOPE);

    filterRuntimeAndMarkCompileOnlyAsProvided(compileDependencies, runtimeDependencies);
    result.addAll(compileDependencies);
    result.addAll(runtimeDependencies);

    addAdditionalProvidedDependencies(sourceSet, result);

    int order = 0;
    for (ExternalDependency dependency : result) {
      ((AbstractExternalDependency)dependency).setClasspathOrder(++order);
    }
    return result;
  }

  private Collection<ExternalDependency> resolveDependencies(@Nullable Configuration configuration, @Nullable String scope) {
    if (configuration == null) {
      return emptySet();
    }
    LenientConfiguration lenientConfiguration = configuration.getResolvedConfiguration().getLenientConfiguration();

    List<ComponentIdentifier> components = new ArrayList<ComponentIdentifier>();
    Map<ResolvedDependency, Set<ResolvedArtifact>> resolvedArtifacts = new LinkedHashMap<ResolvedDependency, Set<ResolvedArtifact>>();
    for (ResolvedDependency dependency : lenientConfiguration.getAllModuleDependencies()) {
      try {
        Set<ResolvedArtifact> moduleArtifacts = dependency.getModuleArtifacts();
        for (ResolvedArtifact artifact : moduleArtifacts) {
          if ((artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier)) continue;
          components.add(toComponentIdentifier(artifact.getModuleVersion().getId()));
        }
        resolvedArtifacts.put(dependency, moduleArtifacts);
      }
      catch (Exception ignore) {
        // ignore org.gradle.internal.resolve.ArtifactResolveException
      }
    }
    Map<ComponentIdentifier, ComponentArtifactsResult> auxiliaryArtifactsMap = buildAuxiliaryArtifactsMap(configuration, components);
    Collection<FileCollectionDependency> sourceSetsOutputDirsRuntimeFileDependencies = new LinkedHashSet<FileCollectionDependency>();
    Collection<ExternalDependency> artifactDependencies = new LinkedHashSet<ExternalDependency>();
    Set<String> resolvedFiles = new HashSet<String>();
    Map<String, DefaultExternalProjectDependency> resolvedProjectDependencies = new HashMap<String, DefaultExternalProjectDependency>();
    for (Map.Entry<ResolvedDependency, Set<ResolvedArtifact>> resolvedDependencySetEntry : resolvedArtifacts.entrySet()) {
      ResolvedDependency resolvedDependency = resolvedDependencySetEntry.getKey();
      Set<ResolvedArtifact> artifacts = resolvedDependencySetEntry.getValue();
      for (ResolvedArtifact artifact : artifacts) {
        File artifactFile = artifact.getFile();
        if (resolvedFiles.contains(artifactFile.getPath())) {
          continue;
        }
        resolvedFiles.add(artifactFile.getPath());
        String artifactPath = mySourceSetFinder.findArtifactBySourceSetOutputDir(artifactFile.getPath());
        if (artifactPath != null) {
          artifactFile = new File(artifactPath);
          if (resolvedFiles.contains(artifactFile.getPath())) {
            continue;
          }
          resolvedFiles.add(artifactFile.getPath());
        }

        AbstractExternalDependency dependency;
        ModuleVersionIdentifier moduleVersionIdentifier = artifact.getModuleVersion().getId();
        if (artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier) {
          if (scope == RUNTIME_SCOPE) {
            SourceSet sourceSet = mySourceSetFinder.findByArtifact(artifactFile.getPath());
            if (sourceSet != null) {
              FileCollectionDependency outputDirsRuntimeFileDependency =
                resolveSourceSetOutputDirsRuntimeFileDependency(sourceSet.getOutput());
              if (outputDirsRuntimeFileDependency != null) {
                sourceSetsOutputDirsRuntimeFileDependencies.add(outputDirsRuntimeFileDependency);
              }
            }
          }

          ProjectComponentIdentifier projectComponentIdentifier = (ProjectComponentIdentifier)artifact.getId().getComponentIdentifier();
          String projectName = projectComponentIdentifier.getProjectName(); // since 4.5
          String key = projectName + "_" + resolvedDependency.getConfiguration();
          DefaultExternalProjectDependency projectDependency = resolvedProjectDependencies.get(key);
          if (projectDependency != null) {
            Set<File> projectDependencyArtifacts = new LinkedHashSet<File>(projectDependency.getProjectDependencyArtifacts());
            projectDependencyArtifacts.add(artifactFile);
            projectDependency.setProjectDependencyArtifacts(projectDependencyArtifacts);
            Set<File> artifactSources = new LinkedHashSet<File>(projectDependency.getProjectDependencyArtifactsSources());
            artifactSources.addAll(findArtifactSources(singleton(artifactFile), mySourceSetFinder));
            projectDependency.setProjectDependencyArtifactsSources(artifactSources);
            continue;
          }
          else {
            projectDependency = new DefaultExternalProjectDependency();
            resolvedProjectDependencies.put(key, projectDependency);
          }
          dependency = projectDependency;
          projectDependency.setName(projectName);
          projectDependency.setGroup(resolvedDependency.getModuleGroup());
          projectDependency.setVersion(resolvedDependency.getModuleVersion());
          projectDependency.setScope(scope);
          projectDependency.setProjectPath(projectComponentIdentifier.getProjectPath());
          projectDependency.setConfigurationName(resolvedDependency.getConfiguration());
          Set<File> projectArtifacts = singleton(artifactFile);
          projectDependency.setProjectDependencyArtifacts(projectArtifacts);
          projectDependency.setProjectDependencyArtifactsSources(findArtifactSources(projectArtifacts, mySourceSetFinder));
        }
        else {
          DefaultExternalLibraryDependency libraryDependency = new DefaultExternalLibraryDependency();
          libraryDependency.setName(moduleVersionIdentifier.getName());
          libraryDependency.setGroup(moduleVersionIdentifier.getGroup());
          libraryDependency.setVersion(moduleVersionIdentifier.getVersion());
          libraryDependency.setFile(artifactFile);
          ComponentArtifactsResult artifactsResult = auxiliaryArtifactsMap.get(artifact.getId().getComponentIdentifier());
          if (artifactsResult != null) {
            Set<ArtifactResult> sourceArtifactResults = artifactsResult.getArtifacts(SourcesArtifact.class);
            for (ArtifactResult sourceArtifactResult : sourceArtifactResults) {
              if (sourceArtifactResult instanceof ResolvedArtifactResult) {
                libraryDependency.setSource(((ResolvedArtifactResult)sourceArtifactResult).getFile());
                break;
              }
            }
            Set<ArtifactResult> javadocArtifactResults = artifactsResult.getArtifacts(JavadocArtifact.class);
            for (ArtifactResult javadocArtifactResult : javadocArtifactResults) {
              if (javadocArtifactResult instanceof ResolvedArtifactResult) {
                libraryDependency.setJavadoc(((ResolvedArtifactResult)javadocArtifactResult).getFile());
                break;
              }
            }
          }
          libraryDependency.setPackaging(artifact.getExtension());
          libraryDependency.setScope(scope);
          libraryDependency.setClassifier(artifact.getClassifier());

          dependency = libraryDependency;
        }
        artifactDependencies.add(dependency);
      }
    }

    Collection<FileCollectionDependency> otherFileDependencies = resolveOtherFileDependencies(resolvedFiles, configuration, scope);

    Collection<ExternalDependency> result = new LinkedHashSet<ExternalDependency>();
    result.addAll(sourceSetsOutputDirsRuntimeFileDependencies);
    result.addAll(otherFileDependencies);
    result.addAll(artifactDependencies);
    addUnresolvedDependencies(result, lenientConfiguration, scope);
    return result;
  }

  @NotNull
  private Map<ComponentIdentifier, ComponentArtifactsResult> buildAuxiliaryArtifactsMap(@NotNull Configuration configuration,
                                                                                        List<ComponentIdentifier> components) {
    Map<ComponentIdentifier, ComponentArtifactsResult> artifactsResultMap;
    if (!components.isEmpty()) {
      List<Class<? extends Artifact>> artifactTypes = new ArrayList<Class<? extends Artifact>>(2);
      if (myDownloadSources) {
        artifactTypes.add(SourcesArtifact.class);
      }
      if (myDownloadJavadoc) {
        artifactTypes.add(JavadocArtifact.class);
      }
      boolean isBuildScriptConfiguration = myProject.getBuildscript().getConfigurations().contains(configuration);
      DependencyHandler dependencyHandler =
        isBuildScriptConfiguration ? myProject.getBuildscript().getDependencies() : myProject.getDependencies();
      Set<ComponentArtifactsResult> componentResults = dependencyHandler.createArtifactResolutionQuery()
        .forComponents(components)
        .withArtifacts(JvmLibrary.class, artifactTypes)
        .execute()
        .getResolvedComponents();

      artifactsResultMap = new HashMap<ComponentIdentifier, ComponentArtifactsResult>(componentResults.size());
      for (ComponentArtifactsResult artifactsResult : componentResults) {
        artifactsResultMap.put(artifactsResult.getId(), artifactsResult);
      }
    }
    else {
      artifactsResultMap = emptyMap();
    }
    return artifactsResultMap;
  }

  private static Collection<FileCollectionDependency> resolveOtherFileDependencies(@NotNull Set<String> resolvedFiles,
                                                                                   @NotNull Configuration configuration,
                                                                                   @Nullable String scope) {
    ArtifactView artifactView = configuration.getIncoming().artifactView(new Action<ArtifactView.ViewConfiguration>() {
      @Override
      public void execute(@SuppressWarnings("NullableProblems") ArtifactView.ViewConfiguration configuration) {
        configuration.setLenient(true);
        configuration.componentFilter(new Spec<ComponentIdentifier>() {
          @Override
          public boolean isSatisfiedBy(ComponentIdentifier identifier) {
            return !(identifier instanceof ProjectComponentIdentifier || identifier instanceof ModuleComponentIdentifier);
          }
        });
      }
    });
    Set<ResolvedArtifactResult> artifactResults = artifactView.getArtifacts().getArtifacts();
    Collection<FileCollectionDependency> result = new LinkedHashSet<FileCollectionDependency>();
    for (ResolvedArtifactResult artifactResult : artifactResults) {
      File file = artifactResult.getFile();
      if (!resolvedFiles.contains(file.getPath())) {
        DefaultFileCollectionDependency fileCollectionDependency = new DefaultFileCollectionDependency(singleton(file));
        fileCollectionDependency.setScope(scope);
        result.add(fileCollectionDependency);
      }
    }
    return result;
  }

  private static void addUnresolvedDependencies(@NotNull Collection<ExternalDependency> result,
                                                @NotNull LenientConfiguration lenientConfiguration,
                                                @Nullable String scope) {
    Set<UnresolvedDependency> unresolvedModuleDependencies = lenientConfiguration.getUnresolvedModuleDependencies();
    for (UnresolvedDependency unresolvedDependency : unresolvedModuleDependencies) {
      MyModuleVersionSelector myModuleVersionSelector = null;
      Throwable problem = unresolvedDependency.getProblem();
      if (problem.getCause() != null) {
        problem = problem.getCause();
      }
      try {
        if (problem instanceof ModuleVersionResolveException) {
          ComponentSelector componentSelector = ((ModuleVersionResolveException)problem).getSelector();
          if (componentSelector instanceof ModuleComponentSelector) {
            ModuleComponentSelector moduleComponentSelector = (ModuleComponentSelector)componentSelector;
            myModuleVersionSelector = new MyModuleVersionSelector(moduleComponentSelector.getModule(),
                                                                  moduleComponentSelector.getGroup(),
                                                                  moduleComponentSelector.getVersion());
          }
        }
      }
      catch (Throwable ignore) {
      }
      if (myModuleVersionSelector == null) {
        problem = unresolvedDependency.getProblem();
        ModuleVersionSelector selector = unresolvedDependency.getSelector();
        myModuleVersionSelector = new MyModuleVersionSelector(selector.getName(), selector.getGroup(), selector.getVersion());
      }
      DefaultUnresolvedExternalDependency dependency = new DefaultUnresolvedExternalDependency();
      dependency.setName(myModuleVersionSelector.name);
      dependency.setGroup(myModuleVersionSelector.group);
      dependency.setVersion(myModuleVersionSelector.version);
      dependency.setScope(scope);
      dependency.setFailureMessage(problem.getMessage());
      result.add(dependency);
    }
  }

  private static Collection<ExternalDependency> resolveSourceOutputFileDependencies(@NotNull SourceSetOutput sourceSetOutput,
                                                                                    @Nullable String scope) {
    Collection<ExternalDependency> result = new ArrayList<ExternalDependency>(2);
    List<File> files = new ArrayList<File>(sourceSetOutput.getClassesDirs().getFiles());
    files.add(sourceSetOutput.getResourcesDir());
    if (!files.isEmpty()) {
      DefaultFileCollectionDependency fileCollectionDependency = new DefaultFileCollectionDependency(files);
      fileCollectionDependency.setScope(scope);
      result.add(fileCollectionDependency);
    }

    if (scope == RUNTIME_SCOPE) {
      ExternalDependency outputDirsRuntimeFileDependency = resolveSourceSetOutputDirsRuntimeFileDependency(sourceSetOutput);
      if (outputDirsRuntimeFileDependency != null) {
        result.add(outputDirsRuntimeFileDependency);
      }
    }
    return result;
  }

  @Nullable
  private static FileCollectionDependency resolveSourceSetOutputDirsRuntimeFileDependency(@NotNull SourceSetOutput sourceSetOutput) {
    List<File> runtimeOutputDirs = new ArrayList<File>(sourceSetOutput.getDirs().getFiles());
    if (!runtimeOutputDirs.isEmpty()) {
      DefaultFileCollectionDependency runtimeOutputDirsDependency = new DefaultFileCollectionDependency(runtimeOutputDirs);
      runtimeOutputDirsDependency.setScope(RUNTIME_SCOPE);
      return runtimeOutputDirsDependency;
    }
    return null;
  }

  private static void filterRuntimeAndMarkCompileOnlyAsProvided(@NotNull Collection<ExternalDependency> compileDependencies,
                                                                @NotNull Collection<ExternalDependency> runtimeDependencies) {
    Multimap<Collection<File>, ExternalDependency> filesToRuntimeDependenciesMap = HashMultimap.create();
    for (ExternalDependency runtimeDependency : runtimeDependencies) {
      final Collection<File> resolvedFiles = getFiles(runtimeDependency);
      filesToRuntimeDependenciesMap.put(resolvedFiles, runtimeDependency);
    }

    for (ExternalDependency compileDependency : compileDependencies) {
      final Collection<File> resolvedFiles = getFiles(compileDependency);

      Collection<ExternalDependency> dependencies = filesToRuntimeDependenciesMap.get(resolvedFiles);
      final boolean hasRuntimeDependencies = dependencies != null && !dependencies.isEmpty();

      if (hasRuntimeDependencies) {
        runtimeDependencies.removeAll(dependencies);
      }
      else {
        ((AbstractExternalDependency)compileDependency).setScope(PROVIDED_SCOPE);
      }
    }
  }

  private void addAdditionalProvidedDependencies(@NotNull SourceSet sourceSet, @NotNull Collection<ExternalDependency> result) {
    final Set<Configuration> providedConfigurations = new LinkedHashSet<Configuration>();
    if (sourceSet.getName().equals("main") && myProject.getPlugins().findPlugin(WarPlugin.class) != null) {
      Configuration providedCompile = myProject.getConfigurations().findByName("providedCompile");
      if (providedCompile != null) {
        providedConfigurations.add(providedCompile);
      }
      Configuration providedRuntime = myProject.getConfigurations().findByName("providedRuntime");
      if (providedRuntime != null) {
        providedConfigurations.add(providedRuntime);
      }
    }
    if (providedConfigurations.isEmpty()) {
      return;
    }

    Multimap<Object, ExternalDependency> filesToDependenciesMap = ArrayListMultimap.create();
    for (ExternalDependency dep : result) {
      filesToDependenciesMap.put(getFiles(dep), dep);
    }

    for (Configuration configuration : providedConfigurations) {
      Collection<ExternalDependency> providedDependencies = resolveDependencies(configuration, PROVIDED_SCOPE);
      for (Iterator<ExternalDependency> iterator = providedDependencies.iterator(); iterator.hasNext(); ) {
        ExternalDependency providedDependency = iterator.next();
        Collection<File> files = getFiles(providedDependency);
        Collection<ExternalDependency> dependencies = filesToDependenciesMap.get(files);
        if (!dependencies.isEmpty()) {
          for (ExternalDependency depForScope : dependencies) {
            ((AbstractExternalDependency)depForScope).setScope(PROVIDED_SCOPE);
          }
          iterator.remove();
        }
      }
      result.addAll(providedDependencies);
    }
  }

  @NotNull
  private Collection<ExternalDependency> getDependencies(@NotNull FileCollection fileCollection, String scope) {
    if (fileCollection instanceof ConfigurableFileCollection) {
      return getDependencies(((ConfigurableFileCollection)fileCollection).getFrom(), scope);
    }
    else if (fileCollection instanceof UnionFileCollection) {
      return getDependencies(((UnionFileCollection)fileCollection).getSources(), scope);
    }
    else if (fileCollection instanceof Configuration) {
      return resolveDependencies((Configuration)fileCollection, scope);
    }
    else if (fileCollection instanceof SourceSetOutput) {
      return resolveSourceOutputFileDependencies((SourceSetOutput)fileCollection, scope);
    }
    return emptySet();
  }

  private Collection<ExternalDependency> getDependencies(@NotNull Iterable<?> fileCollections, String scope) {
    Collection<ExternalDependency> result = new ArrayList<ExternalDependency>();
    for (Object fileCollection : fileCollections) {
      if (fileCollection instanceof FileCollection) {
        result.addAll(getDependencies((FileCollection)fileCollection, scope));
      }
    }
    return result;
  }

  @NotNull
  public static Collection<File> getFiles(ExternalDependency dependency) {
    if (dependency instanceof ExternalLibraryDependency) {
      return singleton(((ExternalLibraryDependency)dependency).getFile());
    }
    else if (dependency instanceof FileCollectionDependency) {
      return ((FileCollectionDependency)dependency).getFiles();
    }
    else if (dependency instanceof ExternalMultiLibraryDependency) {
      return ((ExternalMultiLibraryDependency)dependency).getFiles();
    }
    else if (dependency instanceof ExternalProjectDependency) {
      return ((ExternalProjectDependency)dependency).getProjectDependencyArtifacts();
    }
    return emptySet();
  }

  private static class MyModuleVersionSelector {
    private final String name;
    private final String group;
    private final String version;

    private MyModuleVersionSelector(String name, String group, String version) {
      this.name = name;
      this.group = group;
      this.version = version;
    }
  }
}
