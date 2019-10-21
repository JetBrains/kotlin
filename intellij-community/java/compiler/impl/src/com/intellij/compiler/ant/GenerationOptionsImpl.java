// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.application.options.ReplacePathToMacroMap;
import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.Chunk;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphGenerator;
import com.intellij.util.graph.InboundSemiGraph;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * Implementation class for Ant generation options
 *
 * @author Eugene Zhuravlev
 */
public class GenerationOptionsImpl extends GenerationOptions {

  /**
   * from absolute path to macro substitutions
   */
  private final ReplacePathToMacroMap myMacroReplacementMap;
  /**
   * from absolute path to macro substitutions
   */
  private final Map<String, String> myOutputUrlToPropertyRefMap;
  /**
   * module chunks
   */
  private final ModuleChunk[] myModuleChunks;
  /**
   * the project to be converted
   */
  private final Project myProject;
  private final boolean myGenerateIdeaHomeProperty;
  private final String myOutputFileName;
  private Set<String> myJdkUrls;
  /**
   * Custom compilers used in the ant build.
   */
  private final Set<ChunkCustomCompilerExtension> myCustomCompilers = new HashSet<>();
  /**
   * map from modules to chunks
   */
  private final Map<Module, ModuleChunk> myModuleToChunkMap = new HashMap<>();

  /**
   * A constructor
   *
   * @param project                        a project to generate
   * @param generateSingleFile             a value of corresponding option
   * @param enableFormCompiler             a value of corresponding option
   * @param backupPreviouslyGeneratedFiles a value of corresponding option
   * @param forceTargetJdk                 a value of corresponding option
   * @param inlineRuntimeClasspath         if true a runtiem classpaths are inlined
   * @param representativeModuleNames      a module name that represents module chunks.
   * @param outputFileName                 a name for the output file
   */
  public GenerationOptionsImpl(Project project,
                               boolean generateSingleFile,
                               boolean enableFormCompiler,
                               boolean backupPreviouslyGeneratedFiles,
                               boolean forceTargetJdk,
                               boolean inlineRuntimeClasspath,
                               boolean generateIdeaHomeProperty,
                               String[] representativeModuleNames, String outputFileName) {
    super(forceTargetJdk, generateSingleFile, enableFormCompiler, backupPreviouslyGeneratedFiles, inlineRuntimeClasspath);
    myProject = project;
    myGenerateIdeaHomeProperty = generateIdeaHomeProperty;
    myOutputFileName = outputFileName;
    myMacroReplacementMap = createReplacementMap();
    myModuleChunks = createModuleChunks(representativeModuleNames);
    myOutputUrlToPropertyRefMap = createOutputUrlToPropertyRefMap(myModuleChunks);
  }

  /**
   * A constructor
   *
   * @param project                        a project to generate
   * @param forceTargetJdk                 a value of corresponding option
   * @param generateSingleFile             a value of corresponding option
   * @param enableFormCompiler             a value of corresponding option
   * @param backupPreviouslyGeneratedFiles a value of corresponding option
   * @param representativeModuleNames      a module name that represents module chunks.
   */
  @Deprecated
  public GenerationOptionsImpl(Project project,
                               boolean generateSingleFile,
                               boolean enableFormCompiler,
                               boolean backupPreviouslyGeneratedFiles,
                               boolean forceTargetJdk,
                               String[] representativeModuleNames) {
    this(project, forceTargetJdk, generateSingleFile, enableFormCompiler, backupPreviouslyGeneratedFiles, false, false,
         representativeModuleNames, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isIdeaHomeGenerated() {
    return myGenerateIdeaHomeProperty;
  }

  @Override
  public String getBuildFileName() {
    return getOutputFileName() + ".xml";
  }

  @Override
  public String getPropertiesFileName() {
    return getOutputFileName() + ".properties";
  }

  private String getOutputFileName() {
    if (myOutputFileName == null || myOutputFileName.length() == 0) {
      return BuildProperties.getProjectBuildFileName(myProject);
    }
    return myOutputFileName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModuleChunk getChunkByModule(final Module module) {
    if (myModuleToChunkMap.isEmpty()) {
      for (ModuleChunk c : myModuleChunks) {
        for (Module m : c.getModules()) {
          myModuleToChunkMap.put(m, c);
        }
      }
    }
    return myModuleToChunkMap.get(module);
  }

  @Override
  public String subsitutePathWithMacros(@NotNull String path) {
    return myMacroReplacementMap.substitute(path, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public String getPropertyRefForUrl(String url) {
    return myOutputUrlToPropertyRefMap.get(url);
  }

  @NotNull
  private static ReplacePathToMacroMap createReplacementMap() {
    Map<String, String> pathMacros = PathMacros.getInstance().getUserMacros();
    ReplacePathToMacroMap map = new ReplacePathToMacroMap();
    for (String macroName : pathMacros.keySet()) {
      map.put(FileUtilRt.toSystemIndependentName(pathMacros.get(macroName)), BuildProperties.propertyRef(BuildProperties.getPathMacroProperty(macroName)));
    }
    map.put(FileUtilRt.toSystemIndependentName(PathManager.getHomePath()), BuildProperties.propertyRef(BuildProperties.PROPERTY_IDEA_HOME));
    return map;
  }

  private static Map<String, String> createOutputUrlToPropertyRefMap(ModuleChunk[] chunks) {
    final Map<String, String> map = new HashMap<>();

    for (final ModuleChunk chunk : chunks) {
      final String outputPathRef = BuildProperties.propertyRef(BuildProperties.getOutputPathProperty(chunk.getName()));
      final String testsOutputPathRef = BuildProperties.propertyRef(BuildProperties.getOutputPathForTestsProperty(chunk.getName()));

      final Module[] modules = chunk.getModules();
      for (final Module module : modules) {
        final String outputPathUrl = CompilerModuleExtension.getInstance(module).getCompilerOutputUrl();
        if (outputPathUrl != null) {
          map.put(outputPathUrl, outputPathRef);
        }
        final String outputPathForTestsUrl = CompilerModuleExtension.getInstance(module).getCompilerOutputUrlForTests();
        if (outputPathForTestsUrl != null) {
          if (outputPathUrl == null || !outputPathForTestsUrl.equals(outputPathUrl)) {
            map.put(outputPathForTestsUrl, testsOutputPathRef);
          }
        }
      }
    }
    return map;
  }

  @Override
  public ModuleChunk[] getModuleChunks() {
    return myModuleChunks;
  }

  private ModuleChunk[] createModuleChunks(String[] representativeModuleNames) {
    final Set<String> mainModuleNames = ContainerUtil.set(representativeModuleNames);
    final Graph<Chunk<Module>> chunkGraph = ModuleCompilerUtil.toChunkGraph(ModuleManager.getInstance(myProject).moduleGraph());
    final Map<Chunk<Module>, ModuleChunk> map = new HashMap<>();
    final Map<ModuleChunk, Chunk<Module>> reverseMap = new HashMap<>();
    for (final Chunk<Module> chunk : chunkGraph.getNodes()) {
      final Set<Module> modules = chunk.getNodes();
      final ModuleChunk moduleChunk = new ModuleChunk(modules.toArray(Module.EMPTY_ARRAY));
      for (final Module module : modules) {
        if (mainModuleNames.contains(module.getName())) {
          moduleChunk.setMainModule(module);
          break;
        }
      }
      map.put(chunk, moduleChunk);
      reverseMap.put(moduleChunk, chunk);
    }

    final Graph<ModuleChunk> moduleChunkGraph = GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<ModuleChunk>() {
      @Override
      @NotNull
      public Collection<ModuleChunk> getNodes() {
        return map.values();
      }

      @NotNull
      @Override
      public Iterator<ModuleChunk> getIn(ModuleChunk n) {
        final Chunk<Module> chunk = reverseMap.get(n);
        final Iterator<Chunk<Module>> in = chunkGraph.getIn(chunk);
        return new Iterator<ModuleChunk>() {
          @Override
          public boolean hasNext() {
            return in.hasNext();
          }

          @Override
          public ModuleChunk next() {
            return map.get(in.next());
          }

          @Override
          public void remove() {
            throw new IncorrectOperationException("Method is not supported");
          }
        };
      }
    }));
    final Collection<ModuleChunk> nodes = moduleChunkGraph.getNodes();
    final ModuleChunk[] moduleChunks = nodes.toArray(new ModuleChunk[0]);
    for (ModuleChunk moduleChunk : moduleChunks) {
      final Iterator<ModuleChunk> depsIterator = moduleChunkGraph.getIn(moduleChunk);
      List<ModuleChunk> deps = new ArrayList<>();
      while (depsIterator.hasNext()) {
        deps.add(depsIterator.next());
      }
      moduleChunk.setDependentChunks(deps.toArray(new ModuleChunk[0]));
      ContainerUtil.addAll(myCustomCompilers, moduleChunk.getCustomCompilers());
    }
    Arrays.sort(moduleChunks, new ChunksComparator());
    if (generateSingleFile) {
      final File baseDir = BuildProperties.getProjectBaseDir(myProject);
      for (ModuleChunk chunk : moduleChunks) {
        chunk.setBaseDir(baseDir);
      }
    }
    return moduleChunks;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ChunkCustomCompilerExtension[] getCustomCompilers() {
    ChunkCustomCompilerExtension[] sorted = myCustomCompilers.toArray(new ChunkCustomCompilerExtension[0]);
    Arrays.sort(sorted, ChunkCustomCompilerExtension.COMPARATOR);
    return sorted;
  }

  Set<String> getAllJdkUrls() {
    if (myJdkUrls != null) {
      return myJdkUrls;
    }
    final Sdk[] projectJdks = ProjectJdkTable.getInstance().getAllJdks();
    myJdkUrls = new HashSet<>();
    for (Sdk jdk : projectJdks) {
      ContainerUtil.addAll(myJdkUrls, jdk.getRootProvider().getUrls(OrderRootType.CLASSES));
    }
    return myJdkUrls;
  }

  private static class ChunksComparator implements Comparator<ModuleChunk> {
    final Map<ModuleChunk, Integer> myCachedLevels = new HashMap<>();

    @Override
    public int compare(final ModuleChunk o1, final ModuleChunk o2) {
      final int level1 = getChunkLevel(o1);
      final int level2 = getChunkLevel(o2);
      return (level1 == level2) ? o1.getName().compareToIgnoreCase(o2.getName()) : (level1 - level2);
    }

    private int getChunkLevel(ModuleChunk chunk) {
      Integer level = myCachedLevels.get(chunk);
      if (level == null) {
        final ModuleChunk[] chunks = chunk.getDependentChunks();
        if (chunks.length > 0) {
          int maxLevel = 0;
          for (ModuleChunk dependent : chunks) {
            maxLevel = Math.max(maxLevel, getChunkLevel(dependent));
          }
          level = 1 + maxLevel;
        }
        else {
          level = 0;
        }
        myCachedLevels.put(chunk, level);
      }
      return level.intValue();
    }
  }
}
