// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IndexInfrastructureVersion;
import com.intellij.util.indexing.IndexableSetContributor;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectIndexesExporter {
  private final Project myProject;
  public ProjectIndexesExporter(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  public static ProjectIndexesExporter getInstance(@NotNull Project project) {
    return project.getService(ProjectIndexesExporter.class);
  }

  public @NotNull IndexInfrastructureVersion exportIndices(@NotNull Path outZipFile,
                                                           @NotNull ProgressIndicator indicator) {
    List<IndexChunk> chunks = ReadAction.compute(() -> buildChunks());
    return IndexesExporter.getInstance(myProject).exportIndices(chunks, outZipFile, indicator);
  }

  @NotNull
  private List<IndexChunk> buildChunks() {
    Collection<IndexChunk> projectChunks = Arrays
      .stream(ModuleManager.getInstance(myProject).getModules())
      .flatMap(m -> generateIndexChunk(m))
      .collect(Collectors.toMap(ch -> ch.getName(), ch -> ch, ProjectIndexesExporter::mergeUnsafe))
      .values();

    Set<VirtualFile>
      additionalRoots = IndexableSetContributor.EP_NAME.extensions().flatMap(contributor -> Stream
      .concat(IndexableSetContributor.getRootsToIndex(contributor).stream(),
              IndexableSetContributor.getProjectRootsToIndex(contributor, myProject).stream())).collect(
      Collectors.toSet());

    Set<VirtualFile> synthRoots = new THashSet<>();
    for (AdditionalLibraryRootsProvider provider : AdditionalLibraryRootsProvider.EP_NAME.getExtensionList()) {
      for (SyntheticLibrary library : provider.getAdditionalProjectLibraries(myProject)) {
        for (VirtualFile root : library.getAllRoots()) {
          // do not try to visit under-content-roots because the first task took care of that already
          if (!ProjectFileIndex.getInstance(myProject).isInContent(root)) {
            synthRoots.add(root);
          }
        }
      }
    }

    return Stream.concat(projectChunks.stream(),
                         Stream.of(new IndexChunk(additionalRoots, "ADDITIONAL"),
                                   new IndexChunk(synthRoots, "SYNTH"))).collect(Collectors.toList());
  }

  @NotNull
  static IndexChunk mergeUnsafe(@NotNull IndexChunk ch1, @NotNull IndexChunk ch2) {
    ch1.getRoots().addAll(ch2.getRoots());
    return ch1;
  }

  @NotNull
  private static Stream<IndexChunk> generateIndexChunk(@NotNull Module module) {
    Stream<IndexChunk> libChunks = Arrays.stream(ModuleRootManager.getInstance(module).getOrderEntries())
      .map(orderEntry -> {
        if (orderEntry instanceof LibraryOrSdkOrderEntry) {
          VirtualFile[] sources = orderEntry.getFiles(OrderRootType.SOURCES);
          VirtualFile[] classes = orderEntry.getFiles(OrderRootType.CLASSES);
          String name = null;
          if (orderEntry instanceof JdkOrderEntry) {
            name = ((JdkOrderEntry)orderEntry).getJdkName();
          }
          else if (orderEntry instanceof LibraryOrderEntry) {
            name = ((LibraryOrderEntry)orderEntry).getLibraryName();
          }
          if (name == null) {
            name = "unknown";
          }
          return new IndexChunk(ContainerUtil.union(Arrays.asList(sources), Arrays.asList(classes)), reducePath(splitByDots(name)));
        }
        return null;
      })
      .filter(Objects::nonNull);

    Set<VirtualFile> roots =
      ContainerUtil.union(ContainerUtil.newTroveSet(ModuleRootManager.getInstance(module).getContentRoots()),
                          ContainerUtil.newTroveSet(ModuleRootManager.getInstance(module).getSourceRoots()));
    Stream<IndexChunk> srcChunks = Stream.of(new IndexChunk(roots, getChunkName(module)));

    return Stream.concat(libChunks, srcChunks);
  }

  @NotNull
  private static String getChunkName(@NotNull Module module) {
    ModuleManager moduleManager = ModuleManager.getInstance(module.getProject());
    String[] path;
    if (moduleManager.hasModuleGroups()) {
      path = moduleManager.getModuleGroupPath(module);
      assert path != null;
    }
    else {
      path = splitByDots(module.getName());
    }
    return reducePath(path);
  }

  @NotNull
  private static String reducePath(String[] path) {
    String[] reducedPath = Arrays.copyOfRange(path, 0, Math.min(1, path.length));
    return StringUtil.join(reducedPath, ".");
  }

  private static String[] splitByDots(String name) {
    return name.split("[-|:.]");
  }
}
