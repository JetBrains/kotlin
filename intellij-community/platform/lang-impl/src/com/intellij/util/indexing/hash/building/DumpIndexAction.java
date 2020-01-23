// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IndexableSetContributor;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DumpIndexAction extends AnAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) return;

    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    descriptor.withTitle("Select Index Dump Directory");
    VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
    if (file != null) {
      ProgressManager.getInstance().run(new Task.Modal(project, "Exporting Indexes..." , true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          File out = VfsUtilCore.virtualToIoFile(file);
          FileUtil.delete(out);
          exportIndices(project, out.toPath(), new File(out, "index.zip").toPath(), indicator);
        }
      });
    }
  }

  private static void exportIndices(@NotNull Project project,
                                    @NotNull Path temp,
                                    @NotNull Path outZipFile,
                                    @NotNull ProgressIndicator indicator) {
    List<IndexChunk> chunks = ReadAction.compute(() -> buildChunks(project));
    IndexesExporter.getInstance(project).exportIndices(chunks, temp, outZipFile, indicator);
  }

  @NotNull
  private static List<IndexChunk> buildChunks(@NotNull Project project) {
    Collection<IndexChunk> projectChunks = Arrays
      .stream(ModuleManager.getInstance(project).getModules())
      .flatMap(m -> generateIndexChunk(m))
      .collect(Collectors.toMap(ch -> ch.getName(), ch -> ch, DumpIndexAction::mergeUnsafe))
      .values();

    Set<VirtualFile>
      additionalRoots = IndexableSetContributor.EP_NAME.extensions().flatMap(contributor -> Stream
      .concat(IndexableSetContributor.getRootsToIndex(contributor).stream(),
              IndexableSetContributor.getProjectRootsToIndex(contributor, project).stream())).collect(
      Collectors.toSet());

    Set<VirtualFile> synthRoots = new THashSet<>();
    for (AdditionalLibraryRootsProvider provider : AdditionalLibraryRootsProvider.EP_NAME.getExtensionList()) {
      for (SyntheticLibrary library : provider.getAdditionalProjectLibraries(project)) {
        for (VirtualFile root : library.getAllRoots()) {
          // do not try to visit under-content-roots because the first task took care of that already
          if (!ProjectFileIndex.getInstance(project).isInContent(root)) {
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
    } else {
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
