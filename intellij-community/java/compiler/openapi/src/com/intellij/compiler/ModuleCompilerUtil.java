// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Couple;
import com.intellij.util.Chunk;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.*;
import com.intellij.util.modules.CircularModuleDependenciesDetector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.*;

/**
 * @author dsl
 */
public final class ModuleCompilerUtil {
  private static final Logger LOG = Logger.getInstance(ModuleCompilerUtil.class);
  private ModuleCompilerUtil() { }

  @NotNull
  public static Module[] getDependencies(Module module) {
    return ModuleRootManager.getInstance(module).getDependencies();
  }

  @NotNull
  private static Graph<Module> createModuleGraph(@NotNull Module[] modules) {
    return GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<Module>() {
      @NotNull
      @Override
      public Collection<Module> getNodes() {
        return Arrays.asList(modules);
      }

      @NotNull
      @Override
      public Iterator<Module> getIn(Module module) {
        return Arrays.asList(getDependencies(module)).iterator();
      }
    }));
  }

  @NotNull
  public static List<Chunk<Module>> getSortedModuleChunks(@NotNull Project project, @NotNull List<? extends Module> modules) {
    final Module[] allModules = ModuleManager.getInstance(project).getModules();
    final List<Chunk<Module>> chunks = getSortedChunks(createModuleGraph(allModules));

    final Set<Module> modulesSet = new HashSet<>(modules);
    // leave only those chunks that contain at least one module from modules
    chunks.removeIf(chunk -> !ContainerUtil.intersects(chunk.getNodes(), modulesSet));
    return chunks;
  }

  @NotNull 
  private static <Node> List<Chunk<Node>> getSortedChunks(@NotNull Graph<Node> graph) {
    final Graph<Chunk<Node>> chunkGraph = toChunkGraph(graph);
    final List<Chunk<Node>> chunks = new ArrayList<>(chunkGraph.getNodes());
    DFSTBuilder<Chunk<Node>> builder = new DFSTBuilder<>(chunkGraph);
    if (!builder.isAcyclic()) {
      LOG.error("Acyclic graph expected");
      return null;
    }

    Collections.sort(chunks, builder.comparator());
    return chunks;
  }

  @NotNull
  public static <Node> Graph<Chunk<Node>> toChunkGraph(@NotNull Graph<Node> graph) {
    return GraphAlgorithms.getInstance().computeSCCGraph(graph);
  }

  public static void sortModules(final Project project, final List<? extends Module> modules) {
    ApplicationManager.getApplication().runReadAction(() -> {
      Comparator<Module> comparator = ModuleManager.getInstance(project).moduleDependencyComparator();
      Collections.sort(modules, comparator);
    });
  }

  /**
   * @deprecated Use {@link CircularModuleDependenciesDetector#addingDependencyFormsCircularity(Module, Module)} instead.
   *             To be removed in IDEA 2018.2.
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2018.2")
  @Deprecated
  public static Couple<Module> addingDependencyFormsCircularity(@NotNull Module currentModule, @NotNull Module toDependOn) {
    return CircularModuleDependenciesDetector.addingDependencyFormsCircularity(currentModule, toDependOn);
  }

  @NotNull
  public static List<Chunk<ModuleSourceSet>> getCyclicDependencies(@NotNull Project project, @NotNull List<? extends Module> modules) {
    Collection<Chunk<ModuleSourceSet>> chunks = computeSourceSetCycles(new DefaultModulesProvider(project));
    final Set<Module> modulesSet = new HashSet<>(modules);
    return ContainerUtil.filter(chunks, chunk -> {
      for (ModuleSourceSet sourceSet : chunk.getNodes()) {
        if (modulesSet.contains(sourceSet.getModule())) {
          return true;
        }
      }
      return false;
    });
  }

  @NotNull
  private static Graph<ModuleSourceSet> createModuleSourceDependenciesGraph(@NotNull RootModelProvider provider) {
    return GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<ModuleSourceSet>() {
      @NotNull
      @Override
      public Collection<ModuleSourceSet> getNodes() {
        Module[] modules = provider.getModules();
        List<ModuleSourceSet> result = new ArrayList<>(modules.length * 2);
        for (Module module : modules) {
          result.add(new ModuleSourceSet(module, ModuleSourceSet.Type.PRODUCTION));
          result.add(new ModuleSourceSet(module, ModuleSourceSet.Type.TEST));
        }
        return result;
      }

      @NotNull
      @Override
      public Iterator<ModuleSourceSet> getIn(final ModuleSourceSet n) {
        ModuleRootModel model = provider.getRootModel(n.getModule());
        OrderEnumerator enumerator = model.orderEntries().compileOnly();
        if (n.getType() == ModuleSourceSet.Type.PRODUCTION) {
          enumerator = enumerator.productionOnly();
        }
        final List<ModuleSourceSet> deps = new ArrayList<>();
        enumerator.forEachModule(module -> {
          deps.add(new ModuleSourceSet(module, n.getType()));
          return true;
        });
        if (n.getType() == ModuleSourceSet.Type.TEST) {
          deps.add(new ModuleSourceSet(n.getModule(), ModuleSourceSet.Type.PRODUCTION));
        }
        return deps.iterator();
      }
    }));
  }

  @NotNull
  public static List<Chunk<ModuleSourceSet>> computeSourceSetCycles(@NotNull ModulesProvider provider) {
    Graph<ModuleSourceSet> graph = createModuleSourceDependenciesGraph(provider);
    Collection<Chunk<ModuleSourceSet>> chunks = GraphAlgorithms.getInstance().computeStronglyConnectedComponents(graph);
    return removeSingleElementChunks(removeDummyNodes(filterDuplicates(removeSingleElementChunks(chunks)), provider));
  }

  private static List<Chunk<ModuleSourceSet>> removeDummyNodes(List<? extends Chunk<ModuleSourceSet>> chunks, ModulesProvider modulesProvider) {
    List<Chunk<ModuleSourceSet>> result = new ArrayList<>(chunks.size());
    for (Chunk<ModuleSourceSet> chunk : chunks) {
      Set<ModuleSourceSet> nodes = new LinkedHashSet<>();
      for (ModuleSourceSet sourceSet : chunk.getNodes()) {
        if (!isDummy(sourceSet, modulesProvider)) {
          nodes.add(sourceSet);
        }
      }
      result.add(new Chunk<>(nodes));
    }
    return result;
  }

  private static boolean isDummy(ModuleSourceSet set, ModulesProvider modulesProvider) {
    JavaSourceRootType type = set.getType() == ModuleSourceSet.Type.PRODUCTION ? JavaSourceRootType.SOURCE : JavaSourceRootType.TEST_SOURCE;
    ModuleRootModel rootModel = modulesProvider.getRootModel(set.getModule());
    for (ContentEntry entry : rootModel.getContentEntries()) {
      if (!entry.getSourceFolders(type).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static List<Chunk<ModuleSourceSet>> removeSingleElementChunks(Collection<? extends Chunk<ModuleSourceSet>> chunks) {
    return ContainerUtil.filter(chunks, chunk -> chunk.getNodes().size() > 1);
  }

  /**
   * Remove cycles in tests included in cycles between production parts
   */
  @NotNull
  private static List<Chunk<ModuleSourceSet>> filterDuplicates(@NotNull Collection<? extends Chunk<ModuleSourceSet>> sourceSetCycles) {
    final List<Set<Module>> productionCycles = new ArrayList<>();

    for (Chunk<ModuleSourceSet> cycle : sourceSetCycles) {
      ModuleSourceSet.Type type = getCommonType(cycle);
      if (type == ModuleSourceSet.Type.PRODUCTION) {
        productionCycles.add(ModuleSourceSet.getModules(cycle.getNodes()));
      }
    }

    return ContainerUtil.filter(sourceSetCycles, chunk -> {
      if (getCommonType(chunk) != ModuleSourceSet.Type.TEST) return true;
      for (Set<Module> productionCycle : productionCycles) {
        if (productionCycle.containsAll(ModuleSourceSet.getModules(chunk.getNodes()))) return false;
      }
      return true;
    });
  }

  @Nullable
  private static ModuleSourceSet.Type getCommonType(@NotNull Chunk<? extends ModuleSourceSet> cycle) {
    ModuleSourceSet.Type type = null;
    for (ModuleSourceSet set : cycle.getNodes()) {
      if (type == null) {
        type = set.getType();
      }
      else if (type != set.getType()) {
        return null;
      }
    }
    return type;
  }
}
