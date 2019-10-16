// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.


package org.jetbrains.plugins.gradle.tooling.util.resolve;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.internal.impldep.com.google.common.base.Function;
import org.gradle.internal.impldep.com.google.common.base.Predicate;
import org.gradle.internal.impldep.com.google.common.collect.ArrayListMultimap;
import org.gradle.internal.impldep.com.google.common.collect.HashMultimap;
import org.gradle.internal.impldep.com.google.common.collect.Lists;
import org.gradle.internal.impldep.com.google.common.collect.Multimap;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.ExternalDependencyId;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.model.FileCollectionDependency;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.tooling.util.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;
import static org.gradle.internal.impldep.com.google.common.base.Predicates.isNull;
import static org.gradle.internal.impldep.com.google.common.base.Predicates.not;
import static org.gradle.internal.impldep.com.google.common.collect.Iterables.filter;

/**
 * @author Vladislav.Soroka
 */
public class DependencyResolverImpl implements DependencyResolver {

  private static final boolean is4OrBetter = GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.0")) >= 0;
  static final boolean isJavaLibraryPluginSupported = is4OrBetter ||
                                                              (GradleVersion.current().compareTo(GradleVersion.version("3.4")) >= 0);
  static final boolean is31OrBetter = isJavaLibraryPluginSupported ||
                                      (GradleVersion.current().compareTo(GradleVersion.version("3.1")) >= 0);
  private static final boolean isDependencySubstitutionsSupported = is31OrBetter ||
                                                                    (GradleVersion.current().compareTo(GradleVersion.version("2.5")) > 0);
  private static final boolean isArtifactResolutionQuerySupported = isDependencySubstitutionsSupported ||
                                                                    (GradleVersion.current().compareTo(GradleVersion.version("2.0")) >= 0);

  @NotNull
  private final Project myProject;
  private final boolean myIsPreview;
  private final boolean myDownloadJavadoc;
  private final boolean myDownloadSources;
  @NotNull
  private final SourceSetCachedFinder mySourceSetFinder;
  public static final String PROVIDED_SCOPE = "PROVIDED";
  public static final String COMPILE_SCOPE = CompileDependenciesProvider.SCOPE;
  public static final String RUNTIME_SCOPE = RuntimeDependenciesProvider.SCOPE;

  public DependencyResolverImpl(@NotNull Project project,
                                boolean isPreview,
                                boolean downloadJavadoc,
                                boolean downloadSources,
                                @NotNull SourceSetCachedFinder sourceSetFinder) {
    myProject = project;
    myIsPreview = isPreview;
    myDownloadJavadoc = downloadJavadoc;
    myDownloadSources = downloadSources;
    mySourceSetFinder = sourceSetFinder;
  }

  @Override
  public Collection<ExternalDependency> resolveDependencies(@Nullable String configurationName) {
    return resolveDependencies(configurationName, null);
  }

  public Collection<ExternalDependency> resolveDependencies(@Nullable String configurationName, @Nullable String scope) {
    if (configurationName == null) return Collections.emptyList();
    return resolveDependencies(myProject.getConfigurations().findByName(configurationName), scope).getExternalDeps();
  }

  @Override
  public Collection<ExternalDependency> resolveDependencies(@Nullable Configuration configuration) {
    return resolveDependencies(configuration, null).getExternalDeps();
  }

  public ExternalDepsResolutionResult resolveDependencies(@Nullable Configuration configuration, @Nullable String scope) {
    if (configuration == null || configuration.getAllDependencies().isEmpty()) {
      return ExternalDepsResolutionResult.EMPTY;
    }

    final ExternalDepsResolutionResult result;

    if (!myIsPreview && isArtifactResolutionQuerySupported) {
      result = new ArtifactQueryResolver(configuration, scope, myProject, myDownloadJavadoc, myDownloadSources, mySourceSetFinder).resolve();
    } else {
      result = new ExternalDepsResolutionResult(findDependencies(configuration, configuration.getAllDependencies(), scope),
                                                new ArrayList<File>());
    }

    Set<ExternalDependency> fileDependencies = findAllFileDependencies(configuration.getAllDependencies(), scope);
    result.getExternalDeps().addAll(fileDependencies);
    return result;
  }


  protected static Multimap<ModuleComponentIdentifier, ProjectDependency> collectProjectDeps(@NotNull final Configuration configuration) {
    return projectDeps(configuration,
                       ArrayListMultimap.<ModuleComponentIdentifier, ProjectDependency>create(),
                       new HashSet<Configuration>());
  }

  private static Multimap<ModuleComponentIdentifier, ProjectDependency> projectDeps(Configuration conf,
                                                                                    Multimap<ModuleComponentIdentifier, ProjectDependency> map,
                                                                                    Set<Configuration> processedConfigurations) {
    if(!processedConfigurations.add(conf)) {
      return map;
    }

    for (Dependency dep : conf.getIncoming().getDependencies()) {
      if (dep instanceof ProjectDependency) {
        Configuration targetConfiguration = getTargetConfiguration((ProjectDependency)dep);
        // TODO handle broken dependencies
        if(targetConfiguration == null) continue;

        map.put(toComponentIdentifier(dep.getGroup(), dep.getName(), dep.getVersion()), (ProjectDependency)dep);
        projectDeps(targetConfiguration, map, processedConfigurations);
      }
    }
    return map;
  }

  boolean containsAll(Configuration cfg, Collection<File> files) {
    for (File file : files) {
      if (!cfg.contains(file)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Collection<ExternalDependency> resolveDependencies(@NotNull SourceSet sourceSet) {
    Collection<ExternalDependency> result = new ArrayList<ExternalDependency>();

    // resolve compile dependencies
    CompileDependenciesProvider compileDependenciesProvider = new CompileDependenciesProvider(sourceSet, myProject).resolve(this);
    Collection<ExternalDependency> compileDependencies = compileDependenciesProvider.getDependencies();

    // resolve runtime dependencies
    RuntimeDependenciesProvider runtimeDependenciesProvider = new RuntimeDependenciesProvider(sourceSet, myProject).resolve(this);
    Collection<ExternalDependency> runtimeDependencies = runtimeDependenciesProvider.getDependencies();

    Multimap<Object, ExternalDependency> filesToDependenciesMap =
      collectCompileDependencies(compileDependenciesProvider, runtimeDependenciesProvider);

    filterCompileDepsFromRuntime(runtimeDependencies, filesToDependenciesMap);

    result.addAll(compileDependencies);
    result.addAll(runtimeDependencies);

    result = Lists.newArrayList(filter(result, not(isNull())));


    // merge file dependencies
    Set<File> compileClasspathFiles = getCompileClasspathFiles(sourceSet, "Java", "Groovy", "Scala");
    Map<File, Integer> compileClasspathOrder = addIterationOrder(compileClasspathFiles);

    Set<File> runtimeClasspathFiles = getRuntimeClasspathFiles(sourceSet);
    Map<File, Integer> runtimeClasspathOrder = addIterationOrder(runtimeClasspathFiles);

    runtimeClasspathFiles.removeAll(compileClasspathFiles);
    runtimeClasspathFiles.removeAll(sourceSet.getOutput().getFiles());
    compileClasspathFiles.removeAll(sourceSet.getOutput().getFiles());

    Multimap<String, File> resolvedDependenciesMap = ArrayListMultimap.create();
    resolvedDependenciesMap.putAll(CompileDependenciesProvider.SCOPE, compileDependenciesProvider.getFiles());
    resolvedDependenciesMap.putAll(RuntimeDependenciesProvider.SCOPE, runtimeDependenciesProvider.getFiles());
    Project rootProject = myProject.getRootProject();

    for (ExternalDependency dependency : new DependencyTraverser(result)) {
      updateDependencyOrder(compileClasspathOrder, runtimeClasspathOrder, dependency);

      resolvedDependenciesMap.putAll(dependency.getScope(), getFiles(dependency));

      if (dependency instanceof ExternalProjectDependency) {
        ExternalProjectDependency projectDependency = (ExternalProjectDependency)dependency;
        Project project = rootProject.findProject(projectDependency.getProjectPath());
        SourceSet mainSourceSet;
        if ((mainSourceSet = findSourceSet(project, "main")) != null) {
          result.addAll(collectSourceSetOutputDirsAsSingleEntryLibraries(mainSourceSet, runtimeClasspathOrder, dependency.getScope()));
        }
      }
    }

    // remove processed files
    compileClasspathFiles.removeAll(resolvedDependenciesMap.get(CompileDependenciesProvider.SCOPE));
    compileClasspathFiles.removeAll(resolvedDependenciesMap.get(PROVIDED_SCOPE));

    runtimeClasspathFiles.removeAll(resolvedDependenciesMap.get(RuntimeDependenciesProvider.SCOPE));
    runtimeClasspathFiles.removeAll(resolvedDependenciesMap.get(CompileDependenciesProvider.SCOPE));
    runtimeClasspathFiles.removeAll(resolvedDependenciesMap.get(PROVIDED_SCOPE));

    // try to map to libraries
    Collection<ExternalDependency> fileDependencies = new ArrayList<ExternalDependency>();
    fileDependencies.addAll(createLibraryDependenciesForFiles(compileClasspathFiles, CompileDependenciesProvider.SCOPE));
    fileDependencies.addAll(createLibraryDependenciesForFiles(runtimeClasspathFiles, RuntimeDependenciesProvider.SCOPE));

    for (ExternalDependency dependency : fileDependencies) {
      updateDependencyOrder(compileClasspathOrder, runtimeClasspathOrder, dependency);
    }

    result.addAll(fileDependencies);

    result.addAll(createFileCollectionDependencies(compileClasspathFiles, compileClasspathOrder, CompileDependenciesProvider.SCOPE));
    result.addAll(createFileCollectionDependencies(runtimeClasspathFiles, runtimeClasspathOrder, RuntimeDependenciesProvider.SCOPE));

    result.addAll(collectSourceSetOutputDirsAsSingleEntryLibraries(sourceSet, runtimeClasspathOrder, RuntimeDependenciesProvider.SCOPE));

    collectProvidedDependencies(sourceSet, result);

    return removeDuplicates(result);
  }

  public void collectProvidedDependencies(@NotNull SourceSet sourceSet,
                                          @NotNull Collection<ExternalDependency> result) {
    Multimap<Object, ExternalDependency> filesToDependenciesMap;// handle provided dependencies
    final Set<Configuration> providedConfigurations = new LinkedHashSet<Configuration>();
    filesToDependenciesMap = ArrayListMultimap.create();
    for (ExternalDependency dep : new DependencyTraverser(result)) {
      filesToDependenciesMap.put(getFiles(dep), dep);
    }

    if (sourceSet.getName().equals("main") && myProject.getPlugins().findPlugin(WarPlugin.class) != null) {
      providedConfigurations.add(myProject.getConfigurations().findByName("providedCompile"));
      providedConfigurations.add(myProject.getConfigurations().findByName("providedRuntime"));
    }

    final IdeaPlugin ideaPlugin = myProject.getPlugins().findPlugin(IdeaPlugin.class);

    if (ideaPlugin != null) {
      Map<String, Map<String, Collection<Configuration>>> scopes = ideaPlugin.getModel().getModule().getScopes();
      Map<String, Collection<Configuration>> providedPlusScopes = scopes.get(PROVIDED_SCOPE);

      if (providedPlusScopes != null && providedPlusScopes.get("plus") != null) {
        Iterable<Configuration> ideaPluginProvidedConfigurations = filter(providedPlusScopes.get("plus"), new Predicate<Configuration>() {
          @Override

          public boolean apply(Configuration cfg) {
            // filter default 'compileClasspath' for slight optimization since it has been already processed as compile dependencies
            return !cfg.getName().equals("compileClasspath")
                   // since gradle 3.4 'idea' plugin PROVIDED scope.plus contains 'providedCompile' and 'providedRuntime' configurations
                   // see https://github.com/gradle/gradle/commit/c46897ae840c5ebb32946009c83d861ee194ab96#diff-0fa13ec419e839ef2d355b7feb88b815R432
                   && !providedConfigurations.contains(cfg);
          }
        });

        for (Configuration configuration : ideaPluginProvidedConfigurations) {
          Collection<ExternalDependency> providedDependencies = resolveDependencies(configuration, PROVIDED_SCOPE).getExternalDeps();
          for(ExternalDependency it : new DependencyTraverser(providedDependencies)) {
            filesToDependenciesMap.put(getFiles(it), it);
          }
          result.addAll(providedDependencies);
        }
      }
    }

    for (Configuration cfg : providedConfigurations) {
      Collection<ExternalDependency> providedDependencies = resolveDependencies(cfg, PROVIDED_SCOPE).getExternalDeps();
      for (ExternalDependency dep : new DependencyTraverser(providedDependencies)) {
        Collection<ExternalDependency> dependencies = filesToDependenciesMap.get(getFiles(dep));
        if (!dependencies.isEmpty()) {
          if (dep.getDependencies().isEmpty()) {
            providedDependencies.remove(dep);
          }
          for (ExternalDependency depForScope: dependencies) {
            ((AbstractExternalDependency)depForScope).setScope(PROVIDED_SCOPE);
          }
        } else {
          filesToDependenciesMap.put(getFiles(dep), dep);
        }
      }
      result.addAll(providedDependencies);
    }
  }

  public void updateDependencyOrder(Map<File, Integer> compileClasspathOrder,
                                      Map<File, Integer> runtimeClasspathOrder,
                                      ExternalDependency dependency) {
    String scope = dependency.getScope();
    Map<File, Integer> classpathOrderMap = scope == CompileDependenciesProvider.SCOPE ? compileClasspathOrder :
                                           scope == RuntimeDependenciesProvider.SCOPE ? runtimeClasspathOrder : null;
    final Collection<File> depFiles = getFiles(dependency);
    int order = getOrder(classpathOrderMap, depFiles);
    if (dependency instanceof AbstractExternalDependency) {
      ((AbstractExternalDependency)dependency).setClasspathOrder(order);
    }
  }

  @NotNull
  private Collection<ExternalDependency> createFileCollectionDependencies(@NotNull final Set<File> files,
                                                                          @NotNull final Map<File, Integer> classPathOrder,
                                                                          @Nullable final String scope) {
    final List<ExternalDependency> result = new ArrayList<ExternalDependency>();
    if (files.isEmpty()) {
      return result;
    }

    final DefaultFileCollectionDependency fileCollectionDependency = new DefaultFileCollectionDependency(files);
    fileCollectionDependency.setScope(scope);
    fileCollectionDependency.setClasspathOrder(getOrder(classPathOrder, files));

    result.add(fileCollectionDependency);

    for (File file : files) {
      SourceSet outputDirSourceSet = mySourceSetFinder.findByArtifact(file.getPath());
      if (outputDirSourceSet != null) {
        result.addAll(
          collectSourceSetOutputDirsAsSingleEntryLibraries(outputDirSourceSet,
                                                           classPathOrder,
                                                           scope));
      }
    }

    return result;
  }


  public SourceSet findSourceSet(@Nullable final Project project,
                                 @NotNull final String name) {
    if (project == null) {
      return null;
    }

    SourceSetContainer sourceSets = JavaPluginUtil.getSourceSetContainer(project);
    return sourceSets == null ? null : sourceSets.findByName(name);
  }


  public int getOrder(@Nullable Map<File, Integer> classpathOrderMap,
                      @NotNull Collection<File> files) {
    int order = -1;
    for (File file : files) {
      if (classpathOrderMap != null) {
        Integer fileOrder = classpathOrderMap.get(file);
        if (fileOrder != null && (order == -1 || fileOrder < order)) {
          order = fileOrder;
        }
        if (order == 0) break;
      }
    }
    return order;
  }

  @NotNull
  public Map<File, Integer> addIterationOrder(Set<File> files) {
    int order = 0;
    Map<File, Integer> fileToOrder = new LinkedHashMap<File, Integer>();
    for (File file : files) {
      fileToOrder.put(file, order++);
    }
    return fileToOrder;
  }

  @NotNull
  public Set<File> getCompileClasspathFiles(@NotNull SourceSet sourceSet, String... languages) {
    List<String> jvmLanguages = Lists.newArrayList(languages);
    final String sourceSetCompileTaskPrefix = sourceSet.getName() == "main" ? "" : sourceSet.getName();

    List<String> compileTaskNames = Lists.transform(jvmLanguages, new Function<String, String>() {
      @Override
      public String apply(String s) {
        return "compile" + capitalize(sourceSetCompileTaskPrefix) + s;
      }
    });

    Set<File> compileClasspathFiles = new LinkedHashSet<File>();

    for (String task : compileTaskNames) {
      Task compileTask = myProject.getTasks().findByName(task);
      if (compileTask instanceof AbstractCompile) {
        try {
          List<File> files = new ArrayList<File>(((AbstractCompile)compileTask).getClasspath().getFiles());
          // TODO is this due to ordering?
          files.removeAll(compileClasspathFiles);
          compileClasspathFiles.addAll(files);
        } catch (Exception e) {
          // ignore
        }
      }
    }

    try {
      compileClasspathFiles = compileClasspathFiles.isEmpty() ? new LinkedHashSet<File>(sourceSet.getCompileClasspath().getFiles()) : compileClasspathFiles;
    } catch (Exception e) {
      // ignore
    }
    return compileClasspathFiles;
  }

  public Set<File> getRuntimeClasspathFiles(@NotNull SourceSet sourceSet) {
    Set<File> result = new LinkedHashSet<File>();
    try {
      result.addAll(sourceSet.getRuntimeClasspath().getFiles());
    } catch (Exception e) {
      // ignore
    }
    return result;
  }

  @NotNull
  public Multimap<Object, ExternalDependency> collectCompileDependencies(CompileDependenciesProvider compileDependenciesProvider,
                                                                         RuntimeDependenciesProvider runtimeDependenciesProvider) {
    Multimap<Object, ExternalDependency> filesToDependenciesMap = HashMultimap.create();
    Collection<ExternalDependency> compileDependencies = compileDependenciesProvider.getDependencies();

    for (ExternalDependency dep : new DependencyTraverser(compileDependencies)) {
      final Collection<File> resolvedFiles = getFiles(dep);
      filesToDependenciesMap.put(resolvedFiles, dep);

      markAsProvidedIfNeeded(compileDependenciesProvider,
                             runtimeDependenciesProvider,
                             (AbstractExternalDependency)dep);
    }
    return filesToDependenciesMap;
  }

  @NotNull
  public Multimap<Object, ExternalDependency> filterCompileDepsFromRuntime(Collection<ExternalDependency> runtimeDependencies,
                                                                           Multimap<Object, ExternalDependency> filesToCompileDependenciesMap) {
    Multimap<Object, ExternalDependency> resolvedRuntimeMap = ArrayListMultimap.create();

    for (ExternalDependency dep : new DependencyTraverser(runtimeDependencies)) {
      final Collection<File> resolvedFiles = getFiles(dep);

      Collection<ExternalDependency> dependencies = filesToCompileDependenciesMap.get(resolvedFiles);
      final boolean hasCompileDependencies = dependencies != null && !dependencies.isEmpty();

      if (hasCompileDependencies && dep.getDependencies().isEmpty()) {
        runtimeDependencies.remove(dep);
        for (ExternalDependency dependency : dependencies) {
          ((AbstractExternalDependency)dependency).setScope(CompileDependenciesProvider.SCOPE);
        }
      }
      else {
        if (hasCompileDependencies) {
          ((AbstractExternalDependency)dep).setScope(CompileDependenciesProvider.SCOPE);
        }
        resolvedRuntimeMap.put(resolvedFiles, dep);
      }
    }
    return resolvedRuntimeMap;
  }


  public void markAsProvidedIfNeeded(@NotNull final CompileDependenciesProvider compileDependenciesProvider,
                                     @NotNull final RuntimeDependenciesProvider runtimeDependenciesProvider,
                                     @NotNull final AbstractExternalDependency dep) {
    Collection<File> resolvedFiles = getFiles(dep);

    boolean checkCompileOnlyDeps = compileDependenciesProvider.getCompileClasspathConfiguration() != null;

    // since version 3.4 compileOnly no longer extends compile
    // so, we can use compileOnly configuration for the check
    if (isJavaLibraryPluginSupported) {
      final Set<File> compileOnlyConfigurationFiles = compileDependenciesProvider.getCompileOnlyConfigurationFiles();
      if (compileOnlyConfigurationFiles != null && compileOnlyConfigurationFiles.containsAll(resolvedFiles)) {
        // deprecated 'compile' configuration still can be used
        final Set<File> deprecatedCompileConfigurationFiles = compileDependenciesProvider.getDeprecatedCompileConfigurationFiles();
        if (deprecatedCompileConfigurationFiles == null || !deprecatedCompileConfigurationFiles.containsAll(resolvedFiles)) {
          dep.setScope(PROVIDED_SCOPE);
        }
      }
    }
    else {
      if (checkCompileOnlyDeps
          && !compileDependenciesProvider.getCompileConfigurationFiles().containsAll(resolvedFiles)
          && !runtimeDependenciesProvider.getConfigurationFiles().containsAll(resolvedFiles)) {
        dep.setScope(PROVIDED_SCOPE);
      }
    }
  }

  // visible for tests
  @NotNull
  public static List<ExternalDependency> removeDuplicates(Collection<ExternalDependency> result) {
    new DeduplicationVisitor().visit(result);
    return Lists.newArrayList(filter(result, not(isNull())));
  }

  private static class DeduplicationVisitor {

    private final Map<ExternalDependencyId, ExternalDependency> seenDependencies = new HashMap<ExternalDependencyId, ExternalDependency>();

    public void visit(@NotNull Collection<ExternalDependency> dependencies) {
      for (Iterator<ExternalDependency> iter = dependencies.iterator(); iter.hasNext(); ) {
        ExternalDependency nextDependency = iter.next();
        ExternalDependencyId nextId = nextDependency.getId();
        ExternalDependency seenDependency = seenDependencies.get(nextId);
        Collection<ExternalDependency> childDeps = nextDependency.getDependencies();
        if (seenDependency == null) {
          seenDependencies.put(nextId, nextDependency);
          visit(childDeps);
        } else {
          upgradeScopeIfNeeded(seenDependency, nextDependency.getScope());
          visit(childDeps);
          seenDependency.getDependencies().addAll(childDeps);
          if (seenAllFiles(seenDependency, nextDependency)) {
            iter.remove();
          }
        }
      }
    }
  }

  private static boolean seenAllFiles(@NotNull ExternalDependency seenDependency,
                                      @NotNull ExternalDependency nextDependency) {
    Collection<File> seenFiles = getFiles(seenDependency);
    Collection<File> nextFiles = getFiles(nextDependency);
    boolean filesSeen = seenFiles.containsAll(nextFiles);
    boolean projectDependencySeen = compareAsProjectDependencies(seenDependency, nextDependency);

    return filesSeen || projectDependencySeen;
  }

  private static boolean compareAsProjectDependencies(@NotNull ExternalDependency seenDependency,
                                               @NotNull ExternalDependency newDependency) {
    if (seenDependency instanceof ExternalProjectDependency
    && newDependency instanceof ExternalProjectDependency) {
      String seenConfiguration = ((ExternalProjectDependency)seenDependency).getConfigurationName();
      String newConfiguration = ((ExternalProjectDependency)newDependency).getConfigurationName();
      return
        seenDependency.getId().equals(newDependency.getId())
      && ((seenConfiguration == newConfiguration) || (seenConfiguration != null && seenConfiguration.equals(newConfiguration)));
    }
    return false;
  }

  private static void upgradeScopeIfNeeded(@NotNull ExternalDependency targetDependency, @NotNull String newScope) {
    if (targetDependency.getScope().equals(COMPILE_SCOPE) || !(targetDependency instanceof AbstractExternalDependency)) {
      return;
    }

    final AbstractExternalDependency dep = ((AbstractExternalDependency)targetDependency);

    if (newScope.equals(COMPILE_SCOPE)) {
      dep.setScope(COMPILE_SCOPE);
    }

     if ((dep.getScope().equals(RUNTIME_SCOPE) && newScope.equals(PROVIDED_SCOPE))
         || (dep.getScope().equals(PROVIDED_SCOPE) && newScope.equals(RUNTIME_SCOPE))) {
       dep.setScope(COMPILE_SCOPE);
    }
  }


  @NotNull
  static Collection<File> getFiles(ExternalDependency dependency) {
    if (dependency instanceof ExternalLibraryDependency) {
      return Collections.singleton(((ExternalLibraryDependency)dependency).getFile());
    } else if (dependency instanceof FileCollectionDependency) {
      return ((FileCollectionDependency)dependency).getFiles();
    } else if (dependency instanceof ExternalMultiLibraryDependency) {
      return ((ExternalMultiLibraryDependency)dependency).getFiles();
    } else if (dependency instanceof ExternalProjectDependency) {
      return ((ExternalProjectDependency)dependency).getProjectDependencyArtifacts();
    }
    return Collections.emptySet();
  }

  private static Collection<ExternalDependency> collectSourceSetOutputDirsAsSingleEntryLibraries(
    @NotNull final SourceSet sourceSet,
    @NotNull final Map<File, Integer> classpathOrder,
    @Nullable final String scope) {
    @NotNull final Collection<ExternalDependency> result = new LinkedHashSet<ExternalDependency>();
    Set<File> runtimeOutputDirs = sourceSet.getOutput().getDirs().getFiles();
    for (File dir : runtimeOutputDirs) {
      DefaultFileCollectionDependency runtimeOutputDirsDependency = new DefaultFileCollectionDependency(Collections.singleton(dir));
      runtimeOutputDirsDependency.setScope(scope);
      Integer fileOrder = classpathOrder.get(dir);
      runtimeOutputDirsDependency.setClasspathOrder(fileOrder != null ? fileOrder : -1);
      result.add(runtimeOutputDirsDependency);
    }
    return result;
  }


  @Nullable
  ExternalLibraryDependency resolveLibraryByPath(File file, String scope) {
    File modules2Dir = new File(myProject.getGradle().getGradleUserHomeDir(), "caches/modules-2/files-2.1");
    return resolveLibraryByPath(file, modules2Dir, scope);
  }

  @Nullable
  static ExternalLibraryDependency resolveLibraryByPath(File file, File modules2Dir, String scope) {
    File sourcesFile = null;
    try {
      String modules2Path = modules2Dir.getCanonicalPath();
      String filePath = file.getCanonicalPath();

      if (filePath.startsWith(modules2Path)) {
        List<File> parents = new ArrayList<File>();
        File parent = file.getParentFile();
        while(parent != null && !parent.getName().equals(modules2Dir.getName())) {
          parents.add(parent);
          parent = parent.getParentFile();
        }

        File groupDir = parents.get(parents.size() - 1);
        File artifactDir = parents.get(parents.size() - 2);
        File versionDir = parents.get(parents.size() - 3);

        if (versionDir != null) {
          File[] hashDirs = versionDir.listFiles();
          if (hashDirs != null) {
            for (File hashDir : hashDirs) {
              File[] sourcesJars = hashDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                  return name.endsWith("sources.jar");
                }
              });

              if (sourcesJars != null && sourcesJars.length > 0) {
                sourcesFile = sourcesJars[0];
                break;
              }
            }

            String packaging = resolvePackagingType(file);
            String classifier = resolveClassifier(artifactDir.getName(), versionDir.getName(), file);

            DefaultExternalLibraryDependency defaultDependency = new DefaultExternalLibraryDependency();

            defaultDependency.setName(artifactDir.getName());
            defaultDependency.setGroup(groupDir.getName());
            defaultDependency.setPackaging(packaging);
            defaultDependency.setClassifier(classifier);
            defaultDependency.setVersion(versionDir.getName());
            defaultDependency.setFile(file);
            defaultDependency.setSource(sourcesFile);
            defaultDependency.setScope(scope);

            return defaultDependency;
          }
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  Collection<ExternalDependency> createLibraryDependenciesForFiles(@NotNull final Set<File> fileDependencies,
                                                                   @Nullable final String scope) {
    Collection<ExternalDependency> result = new LinkedHashSet<ExternalDependency>();

    File modules2Dir = new File(myProject.getGradle().getGradleUserHomeDir(), "caches/modules-2/files-2.1");
    List<File> toRemove = new ArrayList<File>();
    for (File file : fileDependencies) {
      ExternalLibraryDependency libraryDependency = resolveLibraryByPath(file, modules2Dir, scope);
      if (libraryDependency != null) {
        result.add(libraryDependency);
        toRemove.add(file);
      } else {
        //noinspection GrUnresolvedAccess
        String name = getNameWithoutExtension(file);
        File sourcesFile = new File(file.getParentFile(), name + "-sources.jar");
        if (sourcesFile.exists()) {
          libraryDependency = new DefaultExternalLibraryDependency();
          DefaultExternalLibraryDependency defLD = (DefaultExternalLibraryDependency)libraryDependency;
          defLD.setFile(file);
          defLD.setSource(sourcesFile);
          defLD.setScope(scope);

          result.add(libraryDependency);
          toRemove.add(file);
        }
      }
    }
    fileDependencies.removeAll(toRemove);
    return result;
  }

  @NotNull
  static String resolvePackagingType(File file) {
    if (file == null) return "jar";
    String path = file.getPath();
    int index = path.lastIndexOf('.');
    if (index < 0) return "jar";
    return path.substring(index + 1);
  }

  @Nullable
  static String resolveClassifier(String name, String version, File file) {
    String libraryFileName = getNameWithoutExtension(file);
    final String mavenLibraryFileName = name + "-" + version;
    if (!mavenLibraryFileName.equals(libraryFileName)) {
      Matcher matcher = Pattern.compile(name + "-" + version + "-(.*)").matcher(libraryFileName);
      if (matcher.matches()) {
        return matcher.group(1);
      }
    }
    return null;
  }

  static String getNameWithoutExtension(File file) {
    if (file == null) return null;
    String name = file.getName();
    int i = name.lastIndexOf('.');
    if (i != -1) {
      name = name.substring(0, i);
    }
    return name;
  }

  public static ModuleComponentIdentifier toComponentIdentifier(ModuleVersionIdentifier id) {
    return new ModuleComponentIdentifierImpl(id.getGroup(), id.getName(), id.getVersion());
  }

  public static ModuleComponentIdentifier toComponentIdentifier(@NotNull String group, @NotNull String module, @NotNull String version) {
    return new ModuleComponentIdentifierImpl(group, module, version);
  }

  private static Set<ExternalDependency> findAllFileDependencies(Collection<Dependency> dependencies, String scope) {
    Set<ExternalDependency> result = new LinkedHashSet<ExternalDependency>();

    for (Dependency dep : dependencies) {
      try {
        if (dep instanceof SelfResolvingDependency && !(dep instanceof ProjectDependency)) {
          Set<File> files = ((SelfResolvingDependency)dep).resolve();
          if (files != null && !files.isEmpty()) {
            AbstractExternalDependency dependency = new DefaultFileCollectionDependency(files);
            dependency.setScope(scope);
            result.add(dependency);
          }
        }
      }
      catch (Exception e) {
        // ignore
      }
    }

    return result;
  }

  private Set<ExternalDependency> findDependencies(@NotNull final  Configuration configuration,
                                                   @NotNull final  Collection<Dependency> dependencies,
                                                   @Nullable final String scope) {
    Set<ExternalDependency> result = new LinkedHashSet<ExternalDependency>();

    Set<ResolvedArtifact> resolvedArtifacts = myIsPreview ? Collections.<ResolvedArtifact>emptySet() :
                                              configuration.getResolvedConfiguration().getLenientConfiguration()
                                                .getArtifacts(Specs.SATISFIES_ALL);

    Multimap<MyModuleIdentifier, ResolvedArtifact> artifactMap = ArrayListMultimap.create();
    for (ResolvedArtifact artifact : resolvedArtifacts) {
      artifactMap.put(toMyModuleIdentifier(artifact.getModuleVersion().getId()), artifact);
    }

    for (Dependency it : dependencies) {
      try {
        if (it instanceof ProjectDependency) {
          Project project = ((ProjectDependency)it).getDependencyProject();
          Configuration targetConfiguration = getTargetConfiguration((ProjectDependency)it);

          DefaultExternalProjectDependency projectDependency = new DefaultExternalProjectDependency();
          projectDependency.setName(project.getName());
          projectDependency.setGroup(project.getGroup().toString());
          projectDependency.setVersion(project.getVersion().toString());
          projectDependency.setScope(scope);
          projectDependency.setProjectPath(project.getPath());
          projectDependency.setConfigurationName(targetConfiguration == null ? "default" : targetConfiguration.getName());
          Set<File> artifacts = new LinkedHashSet<File>(targetConfiguration == null ? Collections.<File>emptySet() :
                                                        targetConfiguration.getAllArtifacts().getFiles().getFiles());
          projectDependency.setProjectDependencyArtifacts(artifacts);
          projectDependency.setProjectDependencyArtifactsSources(findArtifactSources(artifacts, mySourceSetFinder));

          result.add(projectDependency);
        } else if (it != null) {
          Collection<ResolvedArtifact> artifactsResult = artifactMap.get(toMyModuleIdentifier(it.getName(), it.getGroup()));
          if (artifactsResult != null && !artifactsResult.isEmpty()) {
            ResolvedArtifact artifact = artifactsResult.iterator().next();
            String packaging = artifact.getExtension() != null ? artifact.getExtension() : "jar";
            String classifier = artifact.getClassifier();
            final ExternalLibraryDependency resolvedDep = resolveLibraryByPath(artifact.getFile(), scope);
            File sourcesFile = resolvedDep == null ? null : resolvedDep.getSource();

            DefaultExternalLibraryDependency libraryDependency = new DefaultExternalLibraryDependency();
            libraryDependency.setName(it.getName());
            libraryDependency.setGroup(it.getGroup());
            libraryDependency.setPackaging(packaging);
            libraryDependency.setClassifier(classifier);
            libraryDependency.setVersion(artifact.getModuleVersion().getId().getVersion());
            libraryDependency.setScope(scope);
            libraryDependency.setFile(artifact.getFile());
            libraryDependency.setSource(sourcesFile);

            result.add(libraryDependency);
          } else {
            if (!(it instanceof SelfResolvingDependency) && !myIsPreview) {
              final DefaultUnresolvedExternalDependency dependency = new DefaultUnresolvedExternalDependency();
              dependency.setName(it.getName());
              dependency.setGroup(it.getGroup());
              dependency.setVersion(it.getVersion());
              dependency.setScope(scope);
              dependency.setFailureMessage("Could not find " + it.getGroup() + ":" + it.getName() + ":" + it.getVersion());
              result.add(dependency);
            }
          }
        }
      }
      catch (Exception e) {
        // ignore
      }
    }

    return result;
  }

  @Nullable
  public static Configuration getTargetConfiguration(ProjectDependency projectDependency) {

    try {
      return !is4OrBetter ?  (Configuration)projectDependency.getClass().getMethod("getProjectConfiguration").invoke(projectDependency) :
             projectDependency.getDependencyProject().getConfigurations().findByName(projectDependency.getTargetConfiguration() != null
                                                                                    ? projectDependency.getTargetConfiguration()
                                                                                    : "default");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @NotNull
  public static List<File> findArtifactSources(Collection<? extends File> artifactFiles, SourceSetCachedFinder sourceSetFinder) {
    List<File> artifactSources = new ArrayList<File>();
    for (File artifactFile : artifactFiles) {
      Set<File> sources = sourceSetFinder.findSourcesByArtifact(artifactFile.getPath());
      if (sources != null) {
        artifactSources.addAll(sources);
      }
    }
    return artifactSources;
  }

  public static boolean isProjectDependencyArtifact(ResolvedArtifact artifact) {
    return isDependencySubstitutionsSupported && artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier;
  }

  private static MyModuleIdentifier toMyModuleIdentifier(ModuleVersionIdentifier id) {
    return new MyModuleIdentifier(id.getName(), id.getGroup());
  }

  private static MyModuleIdentifier toMyModuleIdentifier(String name, String group) {
    return new MyModuleIdentifier(name, group);
  }
}
