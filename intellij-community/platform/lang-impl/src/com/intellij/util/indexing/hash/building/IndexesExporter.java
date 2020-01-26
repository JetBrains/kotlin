// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.intellij.concurrency.JobLauncher;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.stubs.StubIndexExtension;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.IndexInfrastructureVersion;
import com.intellij.util.indexing.hash.HashBasedIndexGenerator;
import com.intellij.util.indexing.hash.StubHashBasedIndexGenerator;
import com.intellij.util.io.PathKt;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class IndexesExporter {
  private static final Logger LOG = Logger.getInstance(IndexesExporter.class);

  private final Project myProject;

  public IndexesExporter(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  public static IndexesExporter getInstance(@NotNull Project project) {
    return project.getService(IndexesExporter.class);
  }

  @SuppressWarnings("HardCodedStringLiteral")
  public void exportIndices(@NotNull List<IndexChunk> chunks,
                            @NotNull Path temp,
                            @NotNull Path zipFile,
                            @NotNull ProgressIndicator indicator) {
    Path indexRoot = PathKt.createDirectories(temp.resolve("unpacked"));

    indicator.setIndeterminate(false);
    AtomicInteger idx = new AtomicInteger();
    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(chunks, indicator, chunk -> {
      indicator.setText("Indexing chunk " + chunk.getName());
      Path chunkRoot = indexRoot.resolve(chunk.getName());
      ReadAction.run(() -> processChunkUnderReadAction(chunkRoot, chunk));
      indicator.setFraction(((double) idx.incrementAndGet()) / chunks.size());
      return true;
    })) {
      throw new RuntimeException("Failed to execute indexing jobs");
    }

    zipIndexOut(indexRoot, zipFile, indicator);
  }

  public void exportIndexesChunk(@NotNull IndexChunk chunk,
                                 @NotNull Path temp,
                                 @NotNull Path zipFile) {
    Path chunkRoot = PathKt.createDirectories(temp.resolve("unpacked"));
    ReadAction.run(() -> processChunkUnderReadAction(chunkRoot, chunk));
    zipIndexOut(chunkRoot, zipFile, new EmptyProgressIndicator());
  }

  private void processChunkUnderReadAction(@NotNull Path chunkRoot, @NotNull IndexChunk chunk) {
    List<FileBasedIndexExtension<?, ?>> exportableFileBasedIndexExtensions = FileBasedIndexExtension
      .EXTENSION_POINT_NAME
      .extensions()
      .filter(ex -> ex.dependsOnFileContent())
      .filter(ex -> !(ex instanceof StubUpdatingIndex))
      .collect(Collectors.toList());
    List<HashBasedIndexGenerator<?, ?>> fileBasedGenerators = ContainerUtil.map(exportableFileBasedIndexExtensions, ex -> getGenerator(chunkRoot, ex));

    List<StubIndexExtension<?, ?>> exportableStubIndexExtensions = StubIndexExtension.EP_NAME.getExtensionList();

    StubHashBasedIndexGenerator stubGenerator = new StubHashBasedIndexGenerator(chunkRoot, exportableStubIndexExtensions);

    List<HashBasedIndexGenerator<?, ?>> allGenerators = new ArrayList<>(fileBasedGenerators);
    allGenerators.add(stubGenerator);

    generate(chunk, allGenerators, chunkRoot);

    deleteEmptyIndices(fileBasedGenerators, chunkRoot.resolve("empty-indices.txt"));
    deleteEmptyIndices(stubGenerator.getStubGenerators(), chunkRoot.resolve("empty-stub-indices.txt"));

    IndexInfrastructureVersion indexInfrastructureVersion = new IndexInfrastructureVersion(exportableFileBasedIndexExtensions,
                                                                                           exportableStubIndexExtensions);
    Path metadataFile = chunkRoot.resolve("metadata.json");
    writeIndexVersionsMetadata(metadataFile, chunk, indexInfrastructureVersion);
    printStatistics(chunk, fileBasedGenerators, stubGenerator);
    printMetadata(metadataFile);
  }

  @NotNull
  public static String getOsNameForIndexVersions() {
    if (SystemInfo.isWindows) return "windows";
    if (SystemInfo.isMac) return "mac";
    if (SystemInfo.isLinux) return "linux";
    throw new Error("Unknown OS. " + SystemInfo.getOsNameAndVersion());
  }

  private static void writeIndexVersionsMetadata(@NotNull Path metadataFile,
                                                 @NotNull IndexChunk indexChunk,
                                                 @NotNull IndexInfrastructureVersion infrastructureVersion) {

    StringWriter sw = new StringWriter();
    try(JsonWriter writer = new Gson().newBuilder().setPrettyPrinting().create().newJsonWriter(sw)) {
      writer.beginObject();

      writer.name("metadata_version");
      writer.value("1");

      writer.name("os");
      writer.value(getOsNameForIndexVersions());

      writer.name("index_kind");
      writer.value(indexChunk.getKind());

      writer.name("index_name");
      writer.value(indexChunk.getName());

      writer.name("sources");
      writer.beginObject();
      writer.name("hash");
      writer.value(indexChunk.getContentsHash());
      writer.name("os");
      writer.value(getOsNameForIndexVersions());
      writer.endObject();

      writer.name("build");
      writer.beginObject();
      writer.name("os");
      writer.value(SystemInfo.getOsNameAndVersion());
      writer.name("intellij_version");
      writer.value(ApplicationInfo.getInstance().getFullVersion());
      writer.name("intellij_build");
      writer.value(ApplicationInfo.getInstance().getBuild().toString());
      writer.name("intellij_product_code");
      writer.value(ApplicationInfo.getInstance().getBuild().getProductCode());
      writer.endObject();


      writer.name("indexes");
      writer.beginObject();
      writer.name("os");
      writer.value(getOsNameForIndexVersions());
      //what root indexes to be included here?
      writer.name("versions");
      writer.beginObject();
      Map<String, String> allIndexVersions = new LinkedHashMap<>();
      allIndexVersions.putAll(ContainerUtil.map2Map(infrastructureVersion.getFileBasedIndexVersions().entrySet(), e -> Pair.create(e.getKey(), String.valueOf(e.getValue()))));
      allIndexVersions.putAll(ContainerUtil.map2Map(infrastructureVersion.getStubIndexVersions().entrySet(), e -> Pair.create(e.getKey(), String.valueOf(e.getValue()))));
      for (Map.Entry<String, String> e : allIndexVersions.entrySet()) {
        writer.name(e.getKey());
        writer.value(e.getValue());
      }
      writer.endObject();
      writer.endObject();

      writer.endObject();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate versions JSON. " + e.getMessage(), e);
    }

    try {
      PathKt.write(metadataFile, sw.toString());
    } catch (IOException e) {
      throw new RuntimeException("Failed to write versions JSON to " + metadataFile + ". " + e.getMessage(), e);
    }
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
          throw new RuntimeException("Failed to add " + relativePath + " entry to the target archive. " + e.getMessage(), e);
        }
      });
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate indexes archive at " + zipFile + ". " + e.getMessage(), e);
    }
  }

  private void generate(@NotNull IndexChunk chunk,
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
                  generator.indexFile(file, myProject, hashEnumerator);
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

  private static void printMetadata(@NotNull Path metadataFile) {
    try {
      String text = PathKt.readText(metadataFile);
      LOG.warn(metadataFile.getFileName().toString() + ":\n" + text + "\n\n");
    } catch (IOException e){
      throw new RuntimeException("Failed to read " + metadataFile + ". " + e.getMessage(), e);
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
