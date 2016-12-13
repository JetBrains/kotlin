/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.klint;

import com.android.annotations.NonNull;
import com.android.builder.model.*;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.android.tools.idea.gradle.util.GradleUtil;
import com.android.tools.idea.model.AndroidModel;
import com.android.tools.idea.model.AndroidModuleInfo;
import com.android.tools.klint.client.api.LintClient;
import com.android.tools.klint.detector.api.Project;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.graph.Graph;
import org.jetbrains.android.compiler.AndroidDexCompiler;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;
import org.jetbrains.android.facet.IdeaSourceProvider;
import org.jetbrains.android.sdk.AndroidPlatform;
import org.jetbrains.android.util.AndroidCommonUtils;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.android.model.impl.JpsAndroidModuleProperties;

import java.io.File;
import java.util.*;

import static com.android.SdkConstants.APPCOMPAT_LIB_ARTIFACT;
import static com.android.SdkConstants.SUPPORT_LIB_ARTIFACT;

/**
 * An {@linkplain IntellijLintProject} represents a lint project, which typically corresponds to a {@link Module},
 * but can also correspond to a library "project" such as an {@link AndroidLibrary}.
 */
class IntellijLintProject extends Project {
  /**
   * Whether we support running .class file checks. No class file checks are currently registered as inspections.
   * Since IntelliJ doesn't perform background compilation (e.g. only parsing, so there are no bytecode checks)
   * this might need some work before we enable it.
   */
  public static final boolean SUPPORT_CLASS_FILES = false;

  protected AndroidVersion mMinSdkVersion;
  protected AndroidVersion mTargetSdkVersion;

  IntellijLintProject(@NonNull LintClient client,
                      @NonNull File dir,
                      @NonNull File referenceDir) {
    super(client, dir, referenceDir);
  }

  /** Creates a set of projects for the given IntelliJ modules */
  @NonNull
  public static List<Project> create(@NonNull IntellijLintClient client, @Nullable List<VirtualFile> files, @NonNull Module... modules) {
    List<Project> projects = Lists.newArrayList();

    Map<Project,Module> projectMap = Maps.newHashMap();
    Map<Module,Project> moduleMap = Maps.newHashMap();
    Map<AndroidLibrary,Project> libraryMap = Maps.newHashMap();
    if (files != null && !files.isEmpty()) {
      // Wrap list with a mutable list since we'll be removing the files as we see them
      files = Lists.newArrayList(files);
    }
    for (Module module : modules) {
      addProjects(client, module, files, moduleMap, libraryMap, projectMap, projects);
    }

    client.setModuleMap(projectMap);

    if (projects.size() > 1) {
      // Partition the projects up such that we only return projects that aren't
      // included by other projects (e.g. because they are library projects)
      Set<Project> roots = new HashSet<Project>(projects);
      for (Project project : projects) {
        roots.removeAll(project.getAllLibraries());
      }
      return Lists.newArrayList(roots);
    } else {
      return projects;
    }
  }

  /**
   * Creates a project for a single file. Also optionally creates a main project for the file, if applicable.
   *
   * @param client the lint client
   * @param file the file to create a project for
   * @param module the module to create a project for
   * @return a project for the file, as well as a project (or null) for the main Android module
   */
  @NonNull
  public static Pair<Project,Project> createForSingleFile(@NonNull IntellijLintClient client, @Nullable VirtualFile file, @NonNull Module module) {
    // TODO: Can make this method even more lightweight: we don't need to initialize anything in the project (source paths etc)
    // other than the metadata necessary for this file's type
    LintModuleProject project = createModuleProject(client, module);
    LintModuleProject main = null;
    Map<Project,Module> projectMap = Maps.newHashMap();
    if (project != null) {
      project.setDirectLibraries(Collections.<Project>emptyList());
      if (file != null) {
        project.addFile(VfsUtilCore.virtualToIoFile(file));
      }
      projectMap.put(project, module);

      // Supply a main project too, such that when you for example edit a file in a Java library,
      // and lint asks for getMainProject().getMinSdk(), we return the min SDK of an application
      // using the library, not "1" (the default for a module without a manifest)
      if (!project.isAndroidProject()) {
        Module androidModule = findAndroidModule(module);
        if (androidModule != null) {
          main = createModuleProject(client, androidModule);
          if (main != null) {
            projectMap.put(main, androidModule);
            main.setDirectLibraries(Collections.<Project>singletonList(project));
          }
        }
      }
    }
    client.setModuleMap(projectMap);

    //noinspection ConstantConditions
    return Pair.<Project,Project>create(project,main);
  }

  /** Find an Android module that depends on this module; prefer app modules over library modules */
  @Nullable
  private static Module findAndroidModule(@NonNull final Module module) {
    // Search for dependencies of this module
    Graph<Module> graph = ApplicationManager.getApplication().runReadAction(new Computable<Graph<Module>>() {
      @Override
      public Graph<Module> compute() {
        com.intellij.openapi.project.Project project = module.getProject();
        if (project.isDisposed()) {
          return null;
        }
        return ModuleManager.getInstance(project).moduleGraph();
      }
    });

    if (graph == null) {
      return null;
    }

    Set<AndroidFacet> facets = Sets.newHashSet();
    HashSet<Module> seen = Sets.newHashSet();
    seen.add(module);
    addAndroidModules(facets, seen, graph, module);

    // Prefer Android app modules
    for (AndroidFacet facet : facets) {
      if (!facet.isLibraryProject()) {
        return facet.getModule();
      }
    }

    // Resort to library modules if no app module depends directly on it
    if (!facets.isEmpty()) {
      return facets.iterator().next().getModule();
    }

    return null;
  }

  private static void addAndroidModules(Set<AndroidFacet> androidFacets, Set<Module> seen, Graph<Module> graph, Module module) {
    Iterator<Module> iterator = graph.getOut(module);
    while (iterator.hasNext()) {
      Module dep = iterator.next();
      AndroidFacet facet = AndroidFacet.getInstance(dep);
      if (facet != null) {
        androidFacets.add(facet);
      }

      if (!seen.contains(dep)) {
        seen.add(dep);
        addAndroidModules(androidFacets, seen, graph, dep);
      }
    }
  }

  /**
   * Recursively add lint projects for the given module, and any other module or library it depends on, and also
   * populate the reverse maps so we can quickly map from a lint project to a corresponding module/library (used
   * by the lint client
   */
  private static void addProjects(@NonNull LintClient client,
                                  @NonNull Module module,
                                  @Nullable List<VirtualFile> files,
                                  @NonNull Map<Module,Project> moduleMap,
                                  @NonNull Map<AndroidLibrary, Project> libraryMap,
                                  @NonNull Map<Project,Module> projectMap,
                                  @NonNull List<Project> projects) {
    if (moduleMap.containsKey(module)) {
      return;
    }

    LintModuleProject project = createModuleProject(client, module);

    if (project == null) {
      // It's possible for the module to *depend* on Android code, e.g. in a Gradle
      // project there will be a top-level non-Android module
      List<AndroidFacet> dependentFacets = AndroidUtils.getAllAndroidDependencies(module, false);
      for (AndroidFacet dependentFacet : dependentFacets) {
        addProjects(client, dependentFacet.getModule(), files, moduleMap, libraryMap, projectMap, projects);
      }
      return;
    }

    projects.add(project);
    moduleMap.put(module, project);
    projectMap.put(project, module);

    if (processFileFilter(module, files, project)) {
      // No need to process dependencies when doing single file analysis
      return;
    }

    List<Project> dependencies = Lists.newArrayList();
    // No, this shouldn't use getAllAndroidDependencies; we may have non-Android dependencies that this won't include
    // (e.g. Java-only modules)
    List<AndroidFacet> dependentFacets = AndroidUtils.getAllAndroidDependencies(module, true);
    for (AndroidFacet dependentFacet : dependentFacets) {
      Project p = moduleMap.get(dependentFacet.getModule());
      if (p != null) {
        dependencies.add(p);
      } else {
        addProjects(client, dependentFacet.getModule(), files, moduleMap, libraryMap, projectMap, dependencies);
      }
    }

    AndroidFacet facet = AndroidFacet.getInstance(module);
    if (facet != null) {
      AndroidModuleModel androidModuleModel = AndroidModuleModel.get(facet);
      if (androidModuleModel != null) {
        addGradleLibraryProjects(client, files, libraryMap, projects, facet, androidModuleModel, project, projectMap, dependencies);
      }
    }

    project.setDirectLibraries(dependencies);
  }

  /**
   * Checks whether we have a file filter (e.g. a set of specific files to check in the module rather than all files,
   * and if so, and if all the files have been found, returns true)
   */
  private static boolean processFileFilter(@NonNull Module module, @Nullable List<VirtualFile> files, @NonNull LintModuleProject project) {
    if (files != null && !files.isEmpty()) {
      ListIterator<VirtualFile> iterator = files.listIterator();
      while (iterator.hasNext()) {
        VirtualFile file = iterator.next();
        if (module.getModuleContentScope().accept(file)) {
          project.addFile(VfsUtilCore.virtualToIoFile(file));
          iterator.remove();
        }
      }
      if (files.isEmpty()) {
        // We're only scanning a subset of files (typically the current file in the editor);
        // in that case, don't initialize all the libraries etc
        project.setDirectLibraries(Collections.<Project>emptyList());
        return true;
      }
    }
    return false;
  }

  /** Creates a new module project */
  @Nullable
  private static LintModuleProject createModuleProject(@NonNull LintClient client, @NonNull Module module) {
    AndroidFacet facet = AndroidFacet.getInstance(module);
    File dir;

    if (facet != null) {
      final VirtualFile mainContentRoot = AndroidRootUtil.getMainContentRoot(facet);

      if (mainContentRoot == null) {
        return null;
      }
      dir = new File(FileUtil.toSystemDependentName(mainContentRoot.getPath()));
    } else {
      String moduleDirPath = AndroidRootUtil.getModuleDirPath(module);
      if (moduleDirPath == null) {
        return null;
      }
      dir = new File(FileUtil.toSystemDependentName(moduleDirPath));
    }
    LintModuleProject project = null;
    if (facet == null) {
      project = new LintModuleProject(client, dir, dir, module);
      AndroidFacet f = findAndroidFacetInProject(module.getProject());
      if (f != null) {
        project.mGradleProject = f.requiresAndroidModel();
      }
    }
    else if (facet.requiresAndroidModel()) {
      AndroidModel androidModel = facet.getAndroidModel();
      if (androidModel instanceof AndroidModuleModel) {
        project = new LintGradleProject(client, dir, dir, facet, (AndroidModuleModel)androidModel);
      } else {
        project = new LintAndroidModelProject(client, dir, dir, facet, androidModel);
      }
    }
    else {
      project = new LintAndroidProject(client, dir, dir, facet);
    }
    if (project != null) {
      client.registerProject(dir, project);
    }
    return project;
  }

  public static boolean hasAndroidModule(@NonNull com.intellij.openapi.project.Project project) {
    return findAndroidFacetInProject(project) != null;
  }

  @Nullable
  private static AndroidFacet findAndroidFacetInProject(@NonNull com.intellij.openapi.project.Project project) {
    ModuleManager moduleManager = ModuleManager.getInstance(project);
    for (Module module : moduleManager.getModules()) {
      AndroidFacet facet = AndroidFacet.getInstance(module);
      if (facet != null) {
        return facet;
      }
    }

    return null;
  }

  /** Adds any gradle library projects to the dependency list */
  private static void addGradleLibraryProjects(@NonNull LintClient client,
                                               @Nullable List<VirtualFile> files,
                                               @NonNull Map<AndroidLibrary, Project> libraryMap,
                                               @NonNull List<Project> projects,
                                               @NonNull AndroidFacet facet,
                                               @NonNull AndroidModuleModel AndroidModuleModel,
                                               @NonNull LintModuleProject project,
                                               @NonNull Map<Project,Module> projectMap,
                                               @NonNull List<Project> dependencies) {
    Collection<AndroidLibrary> libraries = AndroidModuleModel.getMainArtifact().getDependencies().getLibraries();
    for (AndroidLibrary library : libraries) {
      Project p = libraryMap.get(library);
      if (p == null) {
        File dir = library.getFolder();
        p = new LintGradleLibraryProject(client, dir, dir, library);
        libraryMap.put(library, p);
        projectMap.put(p, facet.getModule());
        projects.add(p);

        if (files != null) {
          VirtualFile libraryDir = LocalFileSystem.getInstance().findFileByIoFile(dir);
          if (libraryDir != null) {
            ListIterator<VirtualFile> iterator = files.listIterator();
            while (iterator.hasNext()) {
              VirtualFile file = iterator.next();
              if (VfsUtilCore.isAncestor(libraryDir, file, false)) {
                project.addFile(VfsUtilCore.virtualToIoFile(file));
                iterator.remove();
              }
            }
          }
          if (files.isEmpty()) {
            files = null; // No more work in other modules
          }
        }
      }
      dependencies.add(p);
    }
  }

  @Override
  protected void initialize() {
    // NOT calling super: super performs ADT/ant initialization. Here we want to use
    // the gradle data instead
  }

  protected static boolean depsDependsOn(@NonNull Project project, @NonNull String artifact) {
    // Checks project dependencies only; used when there is no model
    for (Project dependency : project.getDirectLibraries()) {
      Boolean b = dependency.dependsOn(artifact);
      if (b != null && b) {
        return true;
      }
    }

    return false;
  }

  private static class LintModuleProject extends IntellijLintProject {
    private Module myModule;

    public void setDirectLibraries(List<Project> libraries) {
      mDirectLibraries = libraries;
    }

    private LintModuleProject(@NonNull LintClient client, @NonNull File dir, @NonNull File referenceDir, Module module) {
      super(client, dir, referenceDir);
      myModule = module;
    }

    @Override
    public boolean isAndroidProject() {
      return false;
    }

    @NonNull
    @Override
    public List<File> getJavaSourceFolders() {
      if (mJavaSourceFolders == null) {
        VirtualFile[] sourceRoots = ModuleRootManager.getInstance(myModule).getSourceRoots(false);
        List<File> dirs = new ArrayList<File>(sourceRoots.length);
        for (VirtualFile root : sourceRoots) {
          dirs.add(new File(root.getPath()));
        }
        mJavaSourceFolders = dirs;
      }

      return mJavaSourceFolders;
    }

    @NonNull
    @Override
    public List<File> getTestSourceFolders() {
      if (mTestSourceFolders == null) {
        ModuleRootManager manager = ModuleRootManager.getInstance(myModule);
        VirtualFile[] sourceRoots = manager.getSourceRoots(false);
        VirtualFile[] sourceAndTestRoots = manager.getSourceRoots(true);
        List<File> dirs = new ArrayList<File>(sourceAndTestRoots.length);
        for (VirtualFile root : sourceAndTestRoots) {
          if (!ArrayUtil.contains(root, sourceRoots)) {
            dirs.add(new File(root.getPath()));
          }
        }
        mTestSourceFolders = dirs;
      }
      return mTestSourceFolders;
    }

    @NonNull
    @Override
    public List<File> getJavaClassFolders() {
      if (SUPPORT_CLASS_FILES) {
        if (mJavaClassFolders == null) {
          VirtualFile folder = AndroidDexCompiler.getOutputDirectoryForDex(myModule);
          if (folder != null) {
            mJavaClassFolders = Collections.singletonList(VfsUtilCore.virtualToIoFile(folder));
          } else {
            mJavaClassFolders = Collections.emptyList();
          }
        }

        return mJavaClassFolders;
      }

      return Collections.emptyList();
    }

    @NonNull
    @Override
    public List<File> getJavaLibraries(boolean includeProvided) {
      if (SUPPORT_CLASS_FILES) {
        if (mJavaLibraries == null) {
          mJavaLibraries = Lists.newArrayList();

          final OrderEntry[] entries = ModuleRootManager.getInstance(myModule).getOrderEntries();
          // loop in the inverse order to resolve dependencies on the libraries, so that if a library
          // is required by two higher level libraries it can be inserted in the correct place

          for (int i = entries.length - 1; i >= 0; i--) {
            final OrderEntry orderEntry = entries[i];
            if (orderEntry instanceof LibraryOrderEntry) {
              LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
              VirtualFile[] classes = libraryOrderEntry.getRootFiles(OrderRootType.CLASSES);
              if (classes != null) {
                for (VirtualFile file : classes) {
                  mJavaLibraries.add(VfsUtilCore.virtualToIoFile(file));
                }
              }
            }
          }
        }

        return mJavaLibraries;
      }

      return Collections.emptyList();
    }
  }

  /** Wraps an Android module */
  private static class LintAndroidProject extends LintModuleProject {
    protected final AndroidFacet myFacet;

    private LintAndroidProject(@NonNull LintClient client, @NonNull File dir, @NonNull File referenceDir, @NonNull AndroidFacet facet) {
      super(client, dir, referenceDir, facet.getModule());
      myFacet = facet;

      mGradleProject = false;
      mLibrary = myFacet.isLibraryProject();

      AndroidPlatform platform = AndroidPlatform.getInstance(myFacet.getModule());
      if (platform != null) {
        mBuildSdk = platform.getApiLevel();
      }
    }

    @Override
    public boolean isAndroidProject() {
      return true;
    }

    @NonNull
    @Override
    public String getName() {
      return myFacet.getModule().getName();
    }

    @Override
    @NonNull
    public List<File> getManifestFiles() {
      if (mManifestFiles == null) {
        VirtualFile manifestFile = AndroidRootUtil.getPrimaryManifestFile(myFacet);
        if (manifestFile != null) {
          mManifestFiles = Collections.singletonList(VfsUtilCore.virtualToIoFile(manifestFile));
        } else {
          mManifestFiles = Collections.emptyList();
        }
      }

      return mManifestFiles;
    }

    @NonNull
    @Override
    public List<File> getProguardFiles() {
      if (mProguardFiles == null) {
        final JpsAndroidModuleProperties properties = myFacet.getProperties();

        if (properties.RUN_PROGUARD) {
          final List<String> urls = properties.myProGuardCfgFiles;

          if (!urls.isEmpty()) {
            mProguardFiles = new ArrayList<File>();

            for (String osPath : AndroidUtils.urlsToOsPaths(urls, null)) {
              if (!osPath.contains(AndroidCommonUtils.SDK_HOME_MACRO)) {
                mProguardFiles.add(new File(osPath));
              }
            }
          }
        }

        if (mProguardFiles == null) {
          mProguardFiles = Collections.emptyList();
        }
      }

      return mProguardFiles;
    }

    @NonNull
    @Override
    public List<File> getResourceFolders() {
      if (mResourceFolders == null) {
        List<VirtualFile> folders = myFacet.getResourceFolderManager().getFolders();
        List<File> dirs = Lists.newArrayListWithExpectedSize(folders.size());
        for (VirtualFile folder : folders) {
          dirs.add(VfsUtilCore.virtualToIoFile(folder));
        }
        mResourceFolders = dirs;
      }

      return mResourceFolders;
    }

    @Nullable
    @Override
    public Boolean dependsOn(@NonNull String artifact) {
      if (SUPPORT_LIB_ARTIFACT.equals(artifact)) {
        if (mSupportLib == null) {
          final OrderEntry[] entries = ModuleRootManager.getInstance(myFacet.getModule()).getOrderEntries();
          libraries:
          for (int i = entries.length - 1; i >= 0; i--) {
            final OrderEntry orderEntry = entries[i];
            if (orderEntry instanceof LibraryOrderEntry) {
              LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
              VirtualFile[] classes = libraryOrderEntry.getRootFiles(OrderRootType.CLASSES);
              if (classes != null) {
                for (VirtualFile file : classes) {
                  if (file.getName().equals("android-support-v4.jar")) {
                    mSupportLib = true;
                    break libraries;

                  }
                }
              }
            }
          }
          if (mSupportLib == null) {
            mSupportLib = depsDependsOn(this, artifact);
          }
        }
        return mSupportLib;
      } else if (APPCOMPAT_LIB_ARTIFACT.equals(artifact)) {
        if (mSupportLib == null) {
          final OrderEntry[] entries = ModuleRootManager.getInstance(myFacet.getModule()).getOrderEntries();
          libraries:
          for (int i = entries.length - 1; i >= 0; i--) {
            final OrderEntry orderEntry = entries[i];
            if (orderEntry instanceof LibraryOrderEntry) {
              LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
              VirtualFile[] classes = libraryOrderEntry.getRootFiles(OrderRootType.CLASSES);
              if (classes != null) {
                for (VirtualFile file : classes) {
                  if (file.getName().equals("appcompat-v7.jar")) {
                    mSupportLib = true;
                    break libraries;

                  }
                }
              }
            }
          }
          if (mSupportLib == null) {
            mSupportLib = depsDependsOn(this, artifact);
          }
        }
        return mSupportLib;
      } else {
        return super.dependsOn(artifact);
      }
    }
  }

  private static class LintAndroidModelProject extends LintAndroidProject {
    private final AndroidModel myAndroidModel;

    private LintAndroidModelProject(
      @NonNull LintClient client,
      @NonNull File dir,
      @NonNull File referenceDir,
      @NonNull AndroidFacet facet,
      @NonNull AndroidModel androidModel) {
      super(client, dir, referenceDir, facet);
      myAndroidModel = androidModel;
    }

    @Nullable
    @Override
    public String getPackage() {
      String manifestPackage = super.getPackage();
      // For now, lint only needs the manifest package; not the potentially variant specific
      // package. As part of the Gradle work on the Lint API we should make two separate
      // package lookup methods -- one for the manifest package, one for the build package
      if (manifestPackage != null) {
        return manifestPackage;
      }

      return myAndroidModel.getApplicationId();
    }

    @NonNull
    @Override
    public AndroidVersion getMinSdkVersion() {
      if (mMinSdkVersion == null) {
        mMinSdkVersion = AndroidModuleInfo.get(myFacet).getMinSdkVersion();
      }
      return mMinSdkVersion;
    }

    @NonNull
    @Override
    public AndroidVersion getTargetSdkVersion() {
      if (mTargetSdkVersion == null) {
        mTargetSdkVersion = AndroidModuleInfo.get(myFacet).getTargetSdkVersion();
      }

      return mTargetSdkVersion;
    }
  }

  private static class LintGradleProject extends LintAndroidModelProject {
    private final AndroidModuleModel myAndroidModuleModel;

    /**
     * Creates a new Project. Use one of the factory methods to create.
     */
    private LintGradleProject(
      @NonNull LintClient client,
      @NonNull File dir,
      @NonNull File referenceDir,
      @NonNull AndroidFacet facet,
      @NonNull AndroidModuleModel AndroidModuleModel) {
      super(client, dir, referenceDir, facet, AndroidModuleModel);
      mGradleProject = true;
      mMergeManifests = true;
      myAndroidModuleModel = AndroidModuleModel;
    }

    @NonNull
    @Override
    public List<File> getManifestFiles() {
      if (mManifestFiles == null) {
        mManifestFiles = Lists.newArrayList();
        File mainManifest = myFacet.getMainSourceProvider().getManifestFile();
        if (mainManifest.exists()) {
          mManifestFiles.add(mainManifest);
        }

        List<SourceProvider> flavorSourceProviders = myAndroidModuleModel.getFlavorSourceProviders();
        if (flavorSourceProviders != null) {
          for (SourceProvider provider : flavorSourceProviders) {
            File manifestFile = provider.getManifestFile();
            if (manifestFile.exists()) {
              mManifestFiles.add(manifestFile);
            }
          }
        }

        SourceProvider multiProvider = myAndroidModuleModel.getMultiFlavorSourceProvider();
        if (multiProvider != null) {
          File manifestFile = multiProvider.getManifestFile();
          if (manifestFile.exists()) {
            mManifestFiles.add(manifestFile);
          }
        }

        SourceProvider buildTypeSourceProvider = myAndroidModuleModel.getBuildTypeSourceProvider();
        if (buildTypeSourceProvider != null) {
          File manifestFile = buildTypeSourceProvider.getManifestFile();
          if (manifestFile.exists()) {
            mManifestFiles.add(manifestFile);
          }
        }

        SourceProvider variantProvider = myAndroidModuleModel.getVariantSourceProvider();
        if (variantProvider != null) {
          File manifestFile = variantProvider.getManifestFile();
          if (manifestFile.exists()) {
            mManifestFiles.add(manifestFile);
          }
        }
      }

      return mManifestFiles;
    }

    @NonNull
    @Override
    public List<File> getAssetFolders() {
      if (mAssetFolders == null) {
        mAssetFolders = Lists.newArrayList();
        for (SourceProvider provider : IdeaSourceProvider.getAllSourceProviders(myFacet)) {
          Collection<File> dirs = provider.getAssetsDirectories();
          for (File dir : dirs) {
            if (dir.exists()) { // model returns path whether or not it exists
              mAssetFolders.add(dir);
            }
          }
        }
      }

      return mAssetFolders;
    }

    @NonNull
    @Override
    public List<File> getProguardFiles() {
      if (mProguardFiles == null) {
        if (myFacet.requiresAndroidModel()) {
          // TODO: b/22928250
          AndroidModuleModel androidModel = AndroidModuleModel.get(myFacet);
          if (androidModel != null) {
            ProductFlavor flavor = androidModel.getAndroidProject().getDefaultConfig().getProductFlavor();
            mProguardFiles = Lists.newArrayList();
            for (File file : flavor.getProguardFiles()) {
              if (file.exists()) {
                mProguardFiles.add(file);
              }
            }
            try {
              for (File file : flavor.getConsumerProguardFiles()) {
                if (file.exists()) {
                  mProguardFiles.add(file);
                }
              }
            } catch (Throwable t) {
              // On some models, this threw
              //   org.gradle.tooling.model.UnsupportedMethodException: Unsupported method: BaseConfig.getConsumerProguardFiles().
              // Playing it safe for a while.
            }
          }
        }

        if (mProguardFiles == null) {
          mProguardFiles = Collections.emptyList();
        }
      }

      return mProguardFiles;
    }

    @NonNull
    @Override
    public List<File> getJavaClassFolders() {
      if (SUPPORT_CLASS_FILES) {
        if (mJavaClassFolders == null) {
          // Overridden because we don't synchronize the gradle output directory to
          // the AndroidDexCompiler settings the way java source roots are mapped into
          // the module content root settings
          File dir = myAndroidModuleModel.getMainArtifact().getClassesFolder();
          if (dir != null) {
            mJavaClassFolders = Collections.singletonList(dir);
          } else {
            mJavaClassFolders = Collections.emptyList();
          }
        }

        return mJavaClassFolders;
      }

      return Collections.emptyList();
    }

    private static boolean sProvidedAvailable = true;

    @NonNull
    @Override
    public List<File> getJavaLibraries(boolean includeProvided) {
      if (SUPPORT_CLASS_FILES) {
        if (mJavaLibraries == null) {
          if (myFacet.requiresAndroidModel() && myFacet.getAndroidModel() != null) {
            Collection<JavaLibrary> libs = myAndroidModuleModel.getMainArtifact().getDependencies().getJavaLibraries();
            mJavaLibraries = Lists.newArrayListWithExpectedSize(libs.size());
            for (JavaLibrary lib : libs) {
              if (!includeProvided) {
                if (sProvidedAvailable) {
                  // Method added in 1.4-rc1; gracefully handle running with
                  // older plugins
                  try {
                    if (lib.isProvided()) {
                      continue;
                    }
                  }
                  catch (Throwable t) {
                    //noinspection AssignmentToStaticFieldFromInstanceMethod
                    sProvidedAvailable = false; // don't try again
                  }
                }
              }

              File jar = lib.getJarFile();
              if (jar.exists()) {
                mJavaLibraries.add(jar);
              }
            }
          } else {
            mJavaLibraries = super.getJavaLibraries(includeProvided);
          }
        }
        return mJavaLibraries;
      }

      return Collections.emptyList();
    }

    @Override
    public int getBuildSdk() {
      // TODO: b/22928250
      AndroidModuleModel androidModel = AndroidModuleModel.get(myFacet);
      if (androidModel != null) {
        String compileTarget = androidModel.getAndroidProject().getCompileTarget();
        AndroidVersion version = AndroidTargetHash.getPlatformVersion(compileTarget);
        if (version != null) {
          return version.getFeatureLevel();
        }
      }

      AndroidPlatform platform = AndroidPlatform.getInstance(myFacet.getModule());
      if (platform != null) {
        return platform.getApiVersion().getFeatureLevel();
      }

      return super.getBuildSdk();
    }

    @Nullable
    @Override
    public AndroidProject getGradleProjectModel() {
      // TODO: b/22928250
      AndroidModuleModel androidModel = AndroidModuleModel.get(myFacet);
      if (androidModel != null) {
        return androidModel.getAndroidProject();
      }

      return null;
    }

    @Nullable
    @Override
    public Variant getCurrentVariant() {
      // TODO: b/22928250
      AndroidModuleModel androidModel = AndroidModuleModel.get(myFacet);
      if (androidModel != null) {
        return androidModel.getSelectedVariant();
      }

      return null;
    }

    @Nullable
    @Override
    public AndroidLibrary getGradleLibraryModel() {
      return null;
    }

    @Nullable
    @Override
    public Boolean dependsOn(@NonNull String artifact) {
      // TODO: b/22928250
      AndroidModuleModel androidModel = AndroidModuleModel.get(myFacet);

      if (SUPPORT_LIB_ARTIFACT.equals(artifact)) {
        if (mSupportLib == null) {
          if (myFacet.requiresAndroidModel() && myFacet.getAndroidModel() != null) {
            mSupportLib = GradleUtil.dependsOn(androidModel, artifact);
          } else {
            mSupportLib = depsDependsOn(this, artifact);
          }
        }
        return mSupportLib;
      } else if (APPCOMPAT_LIB_ARTIFACT.equals(artifact)) {
        if (mAppCompat == null) {
          if (myFacet.requiresAndroidModel() && myFacet.getAndroidModel() != null) {
            mAppCompat = GradleUtil.dependsOn(androidModel, artifact);
          } else {
            mAppCompat = depsDependsOn(this, artifact);
          }
        }
        return mAppCompat;
      } else {
        // Some other (not yet directly cached result)
        if (myFacet.requiresAndroidModel() && myFacet.getAndroidModel() != null
           && GradleUtil.dependsOn(androidModel, artifact)) {
          return true;
        }

        return super.dependsOn(artifact);
      }
    }
  }

  private static class LintGradleLibraryProject extends IntellijLintProject {
    private final AndroidLibrary myLibrary;

    private LintGradleLibraryProject(@NonNull LintClient client,
                                     @NonNull File dir,
                                     @NonNull File referenceDir,
                                     @NonNull AndroidLibrary library) {
      super(client, dir, referenceDir);
      myLibrary = library;

      mLibrary = true;
      mMergeManifests = true;
      mReportIssues = false;
      mGradleProject = true;
      mDirectLibraries = Collections.emptyList();
    }

    @NonNull
    @Override
    public List<File> getManifestFiles() {
      if (mManifestFiles == null) {
        File manifest = myLibrary.getManifest();
        if (manifest.exists()) {
          mManifestFiles = Collections.singletonList(manifest);
        } else {
          mManifestFiles = Collections.emptyList();
        }
      }

      return mManifestFiles;
    }

    @NonNull
    @Override
    public List<File> getProguardFiles() {
      if (mProguardFiles == null) {
        File proguardRules = myLibrary.getProguardRules();
        if (proguardRules.exists()) {
          mProguardFiles = Collections.singletonList(proguardRules);
        } else {
          mProguardFiles = Collections.emptyList();
        }
      }

      return mProguardFiles;
    }

    @NonNull
    @Override
    public List<File> getResourceFolders() {
      if (mResourceFolders == null) {
        File folder = myLibrary.getResFolder();
        if (folder.exists()) {
          mResourceFolders = Collections.singletonList(folder);
        } else {
          mResourceFolders = Collections.emptyList();
        }
      }

      return mResourceFolders;
    }

    @NonNull
    @Override
    public List<File> getJavaSourceFolders() {
      return Collections.emptyList();
    }

    @NonNull
    @Override
    public List<File> getJavaClassFolders() {
      return Collections.emptyList();
    }

    private static boolean sOptionalAvailable = true;

    @NonNull
    @Override
    public List<File> getJavaLibraries(boolean includeProvided) {
      if (SUPPORT_CLASS_FILES) {
        if (!includeProvided) {
          if (sOptionalAvailable) {
            // Method added in 1.4-rc1; gracefully handle running with
            // older plugins
            try {
              if (myLibrary.isOptional()) {
                return Collections.emptyList();
              }
            }
            catch (Throwable t) {
              //noinspection AssignmentToStaticFieldFromInstanceMethod
              sOptionalAvailable = false; // don't try again
            }
          }
        }

        if (mJavaLibraries == null) {
          mJavaLibraries = Lists.newArrayList();
          File jarFile = myLibrary.getJarFile();
          if (jarFile.exists()) {
            mJavaLibraries.add(jarFile);
          }

          for (File local : myLibrary.getLocalJars()) {
            if (local.exists()) {
              mJavaLibraries.add(local);
            }
          }
        }

        return mJavaLibraries;
      }

      return Collections.emptyList();
    }

    @Nullable
    @Override
    public AndroidProject getGradleProjectModel() {
      return null;
    }

    @Nullable
    @Override
    public AndroidLibrary getGradleLibraryModel() {
      return myLibrary;
    }

    @Nullable
    @Override
    public Boolean dependsOn(@NonNull String artifact) {
      if (SUPPORT_LIB_ARTIFACT.equals(artifact)) {
        if (mSupportLib == null) {
          mSupportLib = GradleUtil.dependsOn(myLibrary, artifact, true);
        }
        return mSupportLib;
      } else if (APPCOMPAT_LIB_ARTIFACT.equals(artifact)) {
        if (mAppCompat == null) {
          mAppCompat = GradleUtil.dependsOn(myLibrary, artifact, true);
        }
        return mAppCompat;
      } else {
        // Some other (not yet directly cached result)
        if (GradleUtil.dependsOn(myLibrary, artifact, true)) {
          return true;
        }

        return super.dependsOn(artifact);
      }
    }
  }
}
