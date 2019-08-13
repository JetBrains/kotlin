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
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.util.CollectionUtils;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExtIdeaContentRoot;
import org.jetbrains.plugins.gradle.model.ModuleExtendedModel;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.internal.IdeaCompilerOutputImpl;
import org.jetbrains.plugins.gradle.tooling.internal.IdeaContentRootImpl;
import org.jetbrains.plugins.gradle.tooling.internal.IdeaSourceDirectoryImpl;
import org.jetbrains.plugins.gradle.tooling.internal.ModuleExtendedModelImpl;
import org.jetbrains.plugins.gradle.tooling.util.ReflectionUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @deprecated to be removed in 2018.1
 *
 * @author Vladislav.Soroka
 */
@Deprecated
public class ModuleExtendedModelBuilderImpl implements ModelBuilderService {

  private static final boolean is4OorBetter = GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.0")) >= 0;
  private static final boolean is22orBetter = GradleVersion.current().compareTo(GradleVersion.version("2.2")) >= 0;
  public static final boolean is50OrBetter = GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;

  private static final String SOURCE_SETS_PROPERTY = "sourceSets";
  private static final String TEST_SRC_DIRS_PROPERTY = "testSrcDirs";

  @Override
  public boolean canBuild(String modelName) {
    return ModuleExtendedModel.class.getName().equals(modelName);
  }

  @Nullable
  @Override
  public Object buildAll(String modelName, Project project) {

    final String moduleName = project.getName();
    final String moduleGroup = project.getGroup().toString();
    final String moduleVersion = project.getVersion().toString();
    final File buildDir = project.getBuildDir();

    String javaSourceCompatibility = null;
    for (Task task : project.getTasks()) {
      if (task instanceof JavaCompile) {
        JavaCompile javaCompile = (JavaCompile)task;
        javaSourceCompatibility = javaCompile.getSourceCompatibility();
        if(task.getName().equals("compileJava")) break;
      }
    }

    final ModuleExtendedModelImpl moduleVersionModel =
      new ModuleExtendedModelImpl(moduleName, moduleGroup, moduleVersion, buildDir, javaSourceCompatibility);

    final List<File> artifacts = new ArrayList<File>();
    for (Task task : project.getTasks()) {
      if (task instanceof Jar) {
        Jar jar = (Jar)task;
        try {
          artifacts.add(jar.getArchivePath());
        }
        catch (Exception e) {
          project.getLogger().error("warning: [task " + jar.getPath() + "] " + e.getMessage());
        }
      }
    }

    moduleVersionModel.setArtifacts(artifacts);

    final IdeaModuleDirectorySet directorySet = new IdeaModuleDirectorySet();

    final List<File> testClassesDirs = new ArrayList<File>();
    for (Task task : project.getTasks()) {
      if (task instanceof Test) {
        Test test = (Test)task;
        if (is4OorBetter) {
          testClassesDirs.addAll(test.getTestClassesDirs().getFiles());
        }
        else {
          testClassesDirs.add(getTestClassesDirOld(test));
        }

        if (test.hasProperty(TEST_SRC_DIRS_PROPERTY)) {
          Object testSrcDirs = test.property(TEST_SRC_DIRS_PROPERTY);
          if (testSrcDirs instanceof Iterable) {
            for (Object dir : (Iterable)testSrcDirs) {
              addFilePath(directorySet.getTestDirectories(), dir);
            }
          }
        }
      }
    }

    IdeaCompilerOutputImpl compilerOutput = new IdeaCompilerOutputImpl();

    if (project.hasProperty(SOURCE_SETS_PROPERTY)) {
      Object sourceSets = project.property(SOURCE_SETS_PROPERTY);
      if (sourceSets instanceof SourceSetContainer) {
        SourceSetContainer sourceSetContainer = (SourceSetContainer)sourceSets;
        for (SourceSet sourceSet : sourceSetContainer) {

          SourceSetOutput output = sourceSet.getOutput();
          if (SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSet.getName())) {
            if (is4OorBetter) {
              File firstClassesDir = CollectionUtils.findFirst(output.getClassesDirs().getFiles(), Specs.SATISFIES_ALL);
              compilerOutput.setTestClassesDir(firstClassesDir);
            }
            else {
              compilerOutput.setTestClassesDir(getClassesDirOld(output));
            }
            compilerOutput.setTestResourcesDir(output.getResourcesDir());
          }
          if (SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) {
            if (is4OorBetter) {
              File firstClassesDir = CollectionUtils.findFirst(output.getClassesDirs().getFiles(), Specs.SATISFIES_ALL);
              compilerOutput.setMainClassesDir(firstClassesDir);
            }
            else {
              compilerOutput.setMainClassesDir(getClassesDirOld(output));
            }
            compilerOutput.setMainResourcesDir(output.getResourcesDir());
          }

          for (File javaSrcDir : sourceSet.getAllJava().getSrcDirs()) {
            boolean isTestDir = isTestDir(sourceSet, testClassesDirs);
            addFilePath(isTestDir ? directorySet.getTestDirectories() : directorySet.getSourceDirectories(), javaSrcDir);
          }
          for (File resourcesSrcDir : sourceSet.getResources().getSrcDirs()) {
            boolean isTestDir = isTestDir(sourceSet, testClassesDirs);
            addFilePath(isTestDir ? directorySet.getTestResourceDirectories() : directorySet.getResourceDirectories(), resourcesSrcDir);
          }
        }
      }
    }

    File projectDir = project.getProjectDir();
    IdeaContentRootImpl contentRoot = new IdeaContentRootImpl(projectDir);


    final IdeaModuleDirectorySet ideaPluginConfig = extractDataFromIdeaPlugin(project);

    directorySet.mergeFrom(ideaPluginConfig);
    directorySet.fill(contentRoot);

    moduleVersionModel.setContentRoots(Collections.<ExtIdeaContentRoot>singleton(contentRoot));
    moduleVersionModel.setCompilerOutput(compilerOutput);

    ConfigurationContainer configurations = project.getConfigurations();
    SortedMap<String, Configuration> configurationsByName = configurations.getAsMap();

    Map<String, Set<File>> artifactsByConfiguration = new HashMap<String, Set<File>>();
    for (Map.Entry<String, Configuration> configurationEntry : configurationsByName.entrySet()) {
      Set<File> files = configurationEntry.getValue().getAllArtifacts().getFiles().getFiles();
      artifactsByConfiguration.put(configurationEntry.getKey(), new LinkedHashSet<File>(files));
    }
    moduleVersionModel.setArtifactsByConfiguration(artifactsByConfiguration);

    return moduleVersionModel;
  }

  private static File getTestClassesDirOld(Test test) {
    return (File)ReflectionUtil.callByReflection(test, "getTestClassesDir");
  }

  private static File getClassesDirOld(SourceSetOutput output) {
    return (File)ReflectionUtil.callByReflection(output, "getClassesDir");
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "Other"
    ).withDescription("Unable to resolve all content root directories");
  }

  private static boolean isTestDir(SourceSet sourceSet, List<File> testClassesDirs) {
    if (SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSet.getName())) return true;
    if (SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) return false;

    File sourceSetClassesDir;
    if (is4OorBetter) {
      sourceSetClassesDir = CollectionUtils.findFirst(sourceSet.getOutput().getClassesDirs().getFiles(), Specs.SATISFIES_ALL);
    }
    else {
      sourceSetClassesDir = getClassesDirOld(sourceSet.getOutput());
    }
    for (File testClassesDir : testClassesDirs) {
      do {
        if (sourceSetClassesDir.getPath().equals(testClassesDir.getPath())) return true;
      }
      while ((testClassesDir = testClassesDir.getParentFile()) != null);
    }

    return false;
  }

  private static void addFilePath(Set<String> filePathSet, Object file) {
    if (file instanceof File) {
      try {
        filePathSet.add(((File)file).getCanonicalPath());
      }
      catch (IOException ignore) {
      }
    }
  }

  private static IdeaModuleDirectorySet extractDataFromIdeaPlugin(Project project) {
    final IdeaModuleDirectorySet result = new IdeaModuleDirectorySet();

    final IdeaPlugin ideaPlugin = project.getPlugins().findPlugin(IdeaPlugin.class);
    if (ideaPlugin == null) {
      return result;
    }

    final IdeaModel ideaModel = ideaPlugin.getModel();
    if (ideaModel == null) {
      return result;
    }

    final IdeaModule module = ideaModel.getModule();
    if (module == null) {
      return result;
    }

    result.getExcludedDirectories().addAll(module.getExcludeDirs());
    for (File file : module.getSourceDirs()) {
      result.getSourceDirectories().add(file.getPath());
    }

    for (File file : module.getTestSourceDirs()) {
      result.getTestDirectories().add(file.getPath());
    }

    if (is50OrBetter) {
      for (File file : module.getResourceDirs()) {
        result.getResourceDirectories().add(file.getPath());
      }

      for (File file : module.getTestResourceDirs()) {
        result.getTestResourceDirectories().add(file.getPath());
      }
    }

    if (is22orBetter) {
      for (File file : module.getGeneratedSourceDirs()) {
        result.getGeneratedSourceDirectories().add(file.getPath());
      }
    }

    return result;
  }
}

class IdeaModuleDirectorySet {
  private final Set<String> sourceDirectories = new HashSet<String>();
  private final Set<String> testDirectories = new HashSet<String>();
  private final Set<String> resourceDirectories = new HashSet<String>();
  private final Set<String> testResourceDirectories = new HashSet<String>();
  private final Set<String> generatedSourceDirectories = new HashSet<String>();
  private final Set<File> excludedDirectories = new HashSet<File>();

  public Set<String> getSourceDirectories() {
    return sourceDirectories;
  }

  public Set<String> getTestDirectories() {
    return testDirectories;
  }

  public Set<String> getResourceDirectories() {
    return resourceDirectories;
  }

  public Set<String> getTestResourceDirectories() {
    return testResourceDirectories;
  }

  public Set<String> getGeneratedSourceDirectories() {
    return generatedSourceDirectories;
  }

  public Set<File> getExcludedDirectories() {
    return excludedDirectories;
  }

  public void mergeFrom(IdeaModuleDirectorySet other) {
    if (ModuleExtendedModelBuilderImpl.is50OrBetter) {
      if (other.getSourceDirectories().isEmpty()) {
        sourceDirectories.clear();
      }
      if (other.getResourceDirectories().isEmpty()) {
        resourceDirectories.clear();
      }
      if (other.getTestDirectories().isEmpty()) {
        testDirectories.clear();
      }
      if (other.getTestResourceDirectories().isEmpty()) {
        testResourceDirectories.clear();
      }
    } else {
      if (other.getSourceDirectories().isEmpty()) {
        sourceDirectories.clear();
        resourceDirectories.clear();
      }
      if (other.getTestDirectories().isEmpty()) {
        testDirectories.clear();
        testResourceDirectories.clear();
      }
    }

    final Set<String> otherSourceDirectories = new HashSet<String>(other.getSourceDirectories());
    final Set<String> otherTestDirectories = new HashSet<String>(other.getTestDirectories());
    otherSourceDirectories.removeAll(resourceDirectories);
    sourceDirectories.removeAll(otherTestDirectories);
    sourceDirectories.addAll(otherSourceDirectories);
    otherTestDirectories.removeAll(testResourceDirectories);
    testDirectories.addAll(otherTestDirectories);

    // ensure disjoint directories with different type
    resourceDirectories.removeAll(sourceDirectories);
    testDirectories.removeAll(sourceDirectories);
    testResourceDirectories.removeAll(testDirectories);

    generatedSourceDirectories.addAll(other.getGeneratedSourceDirectories());
    excludedDirectories.addAll(other.getExcludedDirectories());
  }

  public void fill(IdeaContentRootImpl contentRoot) {
    for (String javaDir : sourceDirectories) {
      contentRoot.addSourceDirectory(new IdeaSourceDirectoryImpl(new File(javaDir), generatedSourceDirectories.contains(javaDir)));
    }
    for (String testDir : testDirectories) {
      contentRoot.addTestDirectory(new IdeaSourceDirectoryImpl(new File(testDir), generatedSourceDirectories.contains(testDir)));
    }
    for (String resourceDir : resourceDirectories) {
      contentRoot.addResourceDirectory(new IdeaSourceDirectoryImpl(new File(resourceDir)));
    }
    for (String testResourceDir : testResourceDirectories) {
      contentRoot.addTestResourceDirectory(new IdeaSourceDirectoryImpl(new File(testResourceDir)));
    }
    for (File excludeDir : excludedDirectories) {
      contentRoot.addExcludeDirectory(excludeDir);
    }
  }
}
