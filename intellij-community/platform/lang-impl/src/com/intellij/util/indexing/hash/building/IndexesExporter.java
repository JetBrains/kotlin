// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.concurrency.JobLauncher;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.IndexableSetContributor;
import com.intellij.util.indexing.hash.HashBasedIndexGenerator;
import com.intellij.util.indexing.hash.StubHashBasedIndexGenerator;
import com.intellij.util.io.PathKt;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class IndexesExporter {
  private static final Logger LOG = Logger.getInstance(IndexesExporter.class);

  public static void exportIndices(@NotNull Project project, @NotNull Path temp, @NotNull Path outZipFile, @NotNull ProgressIndicator indicator) {
    List<IndexChunk> chunks = ReadAction.compute(() -> buildChunks(project));
    exportIndices(project, chunks, temp, outZipFile, indicator);
  }

  public static void exportSingleIndexChunk(@NotNull Project project,
                                            @NotNull IndexChunk chunk,
                                            @NotNull Path temp,
                                            @NotNull Path outZipFile,
                                            @NotNull ProgressIndicator indicator) {
    exportIndices(project, Collections.singletonList(chunk), temp, outZipFile, indicator);
  }

  @NotNull
  private static List<IndexChunk> buildChunks(@NotNull Project project) {
    Collection<IndexChunk> projectChunks = Arrays
      .stream(ModuleManager.getInstance(project).getModules())
      .flatMap(m -> IndexChunk.generate(m))
      .collect(Collectors.toMap(ch -> ch.getName(), ch -> ch, IndexChunk::mergeUnsafe))
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

  public static void exportIndices(@NotNull Project project,
                                   @NotNull List<IndexChunk> chunks,
                                   @NotNull Path out,
                                   @NotNull Path zipFile,
                                   @NotNull ProgressIndicator indicator) {
    Path indexRoot = PathKt.createDirectories(out.resolve("unpacked"));

    indicator.setIndeterminate(false);
    AtomicInteger idx = new AtomicInteger();
    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(chunks, indicator, chunk -> {
      indicator.setText("Indexing chunk " + chunk.getName());
      Path chunkRoot = indexRoot.resolve(chunk.getName());
      ReadAction.run(() -> {
        List<HashBasedIndexGenerator<?, ?>> fileBasedGenerators = FileBasedIndexExtension
          .EXTENSION_POINT_NAME
          .extensions()
          .filter(ex -> ex.dependsOnFileContent())
          .filter(ex -> !(ex instanceof StubUpdatingIndex))
          .map(extension -> getGenerator(chunkRoot, extension))
          .collect(Collectors.toList());

        StubHashBasedIndexGenerator stubGenerator = new StubHashBasedIndexGenerator(chunkRoot);

        List<HashBasedIndexGenerator<?, ?>> allGenerators = new ArrayList<>(fileBasedGenerators);
        allGenerators.add(stubGenerator);

        generate(chunk, project, allGenerators, chunkRoot);

        printStatistics(chunk, fileBasedGenerators, stubGenerator);
        deleteEmptyIndices(fileBasedGenerators, chunkRoot.resolve("empty-indices.txt"));
        deleteEmptyIndices(stubGenerator.getStubGenerators(), chunkRoot.resolve("empty-stub-indices.txt"));
      });
      indicator.setFraction(((double) idx.incrementAndGet()) / chunks.size());
      return true;
    })) {
      throw new AssertionError();
    }

    zipIndexOut(indexRoot, zipFile, indicator);
  }


  @NotNull
  private static <K, V> HashBasedIndexGenerator<K, V> getGenerator(Path chunkRoot, FileBasedIndexExtension<K, V> extension) {
    return new HashBasedIndexGenerator<>(extension, chunkRoot);
  }

  private static void deleteEmptyIndices(@NotNull List<HashBasedIndexGenerator<?, ?>> generators,
                                         @NotNull Path dumpEmptyIndicesNamesFile) {
    Set<String> emptyIndices = new TreeSet<>();
    for (HashBasedIndexGenerator<?, ?> generator : generators) {
      if (generator.isEmpty()) {
        emptyIndices.add(generator.getExtension().getName().getName());
        Path indexRoot = generator.getIndexRoot();
        PathKt.delete(indexRoot);
      }
    }
    if (!emptyIndices.isEmpty()) {
      String emptyIndicesText = String.join("\n", emptyIndices);
      try {
        PathKt.write(dumpEmptyIndicesNamesFile, emptyIndicesText);
      }
      catch (IOException e) {
        throw new RuntimeException("Failed to write indexes file " + dumpEmptyIndicesNamesFile + ". " + e.getMessage(), e);
      }
    }
  }

  private static void zipIndexOut(@NotNull Path indexRoot, @NotNull Path zipFile, @NotNull ProgressIndicator indicator) {
    indicator.setIndeterminate(true);
    indicator.setText("Zipping index pack");

    try (JBZipFile file = new JBZipFile(zipFile.toFile())) {
      Files.walk(indexRoot).forEach(p -> {
        if (Files.isDirectory(p)) return;
        String relativePath = indexRoot.relativize(p).toString();
        try {
          JBZipEntry entry = file.getOrCreateEntry(relativePath);
          entry.setMethod(ZipEntry.STORED);
          entry.setDataFromFile(p.toFile());
        }
        catch (IOException e) {
          LOG.error(e);
        }
      });
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static void generate(@NotNull IndexChunk chunk,
                               @NotNull Project project,
                               @NotNull Collection<HashBasedIndexGenerator<?, ?>> generators,
                               @NotNull Path chunkOut) {
    try {
      ContentHashEnumerator hashEnumerator = new ContentHashEnumerator(chunkOut.resolve("hashes"));
      try {
        for (HashBasedIndexGenerator<?, ?> generator : generators) {
          generator.openIndex();
        }

        for (VirtualFile root : chunk.getRoots()) {
          VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Boolean>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {

              if (!file.isDirectory() && !SingleRootFileViewProvider.isTooLargeForIntelligence(file)) {
                for (HashBasedIndexGenerator<?, ?> generator : generators) {
                  generator.indexFile(file, project, hashEnumerator);
                }
              }

              return true;
            }
          });
        }
      }
      finally {
        hashEnumerator.close();
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      try {
        for (HashBasedIndexGenerator<?, ?> generator : generators) {
          generator.closeIndex();
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void printStatistics(@NotNull IndexChunk chunk,
                                      @NotNull List<HashBasedIndexGenerator<?, ?>> fileBasedGenerators,
                                      @NotNull StubHashBasedIndexGenerator stubGenerator) {
    StringBuilder stats = new StringBuilder();
    stats.append("Statistics for index chunk ").append(chunk.getName()).append("\n");

    stats.append("File based indices (").append(fileBasedGenerators.size()).append(")").append("\n");
    for (HashBasedIndexGenerator<?, ?> generator : fileBasedGenerators) {
      appendGeneratorStatistics(stats, generator);
    }

    Collection<HashBasedIndexGenerator<?, ?>> stubGenerators = stubGenerator.getStubGenerators();
    stats.append("Stub indices (").append(stubGenerators.size()).append(")").append("\n");
    for (HashBasedIndexGenerator<?, ?> generator : stubGenerators) {
      appendGeneratorStatistics(stats, generator);
    }

    LOG.warn("Statistics\n" + stats);
  }

  private static void appendGeneratorStatistics(@NotNull StringBuilder stringBuilder,
                                                @NotNull HashBasedIndexGenerator<?, ?> generator) {
    stringBuilder
      .append("    ")
      .append("Generator for ")
      .append(generator.getExtension().getName().getName())
      .append(" indexed ")
      .append(generator.getIndexedFilesNumber())
      .append(" files");

    if (generator.isEmpty()) {
      stringBuilder.append(" (empty result)");
    }
    stringBuilder.append("\n");
  }
}
