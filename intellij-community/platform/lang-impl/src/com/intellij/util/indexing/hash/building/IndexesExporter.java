// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.concurrency.JobLauncher;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.stubs.SerializationManagerImpl;
import com.intellij.psi.stubs.StubIndexExtension;
import com.intellij.psi.stubs.StubSharedIndexExtension;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContentImpl;
import com.intellij.util.indexing.IndexInfrastructureVersion;
import com.intellij.util.indexing.snapshot.IndexedHashesSupport;
import com.intellij.util.io.PathKt;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

@SuppressWarnings("HardCodedStringLiteral")
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

  @NotNull
  public IndexInfrastructureVersion exportIndexesChunk(@NotNull IndexChunk chunk,
                                                       @NotNull Path zipFile,
                                                       @NotNull ProgressIndicator indicator) {
    ErrorsCollector errorsCollector = new OptimisticErrorsCollector();
    Disposable rootDisposable = Disposer.newDisposable();
    Path chunkRoot = zipFile.resolveSibling(zipFile.getFileName().toString() + ".unpacked");
    try {
      PathKt.delete(chunkRoot);
      PathKt.delete(zipFile);
      PathKt.createDirectories(chunkRoot);
      IndexInfrastructureVersion version = exportIndexesChunkImpl(chunk, chunkRoot, zipFile, indicator, errorsCollector);
      errorsCollector.report();
      return version;
    } catch (Exception e) {
      try {
        PathKt.delete(chunkRoot);
        PathKt.delete(zipFile);
      } catch (Exception ignore) {
        //nop
      }
      throw new RuntimeException("Failed to generate shared indexes. " + e.getMessage(), e);
    } finally {
      Disposer.dispose(rootDisposable);
    }
  }

  @NotNull
  private IndexInfrastructureVersion exportIndexesChunkImpl(@NotNull IndexChunk chunk,
                                                            @NotNull Path chunkRoot,
                                                            @NotNull Path zipFile,
                                                            @NotNull ProgressIndicator indicator,
                                                            @NotNull ErrorsCollector errorsCollector) throws Exception {

    List<FileBasedIndexExtension<?, ?>> exportableFileBasedIndexExtensions = FileBasedIndexExtension
      .EXTENSION_POINT_NAME
      .extensions()
      .filter(FileBasedIndexExtension::dependsOnFileContent)
      .filter(ex -> !(ex instanceof StubUpdatingIndex))
      .collect(Collectors.toList());

    List<HashBasedIndexGenerator<?, ?>> fileBasedGenerators = ContainerUtil.map(
      exportableFileBasedIndexExtensions,
      ex -> new HashBasedIndexGenerator<>(ex, chunkRoot, ex.needsForwardIndexWhenSharing())
    );

    List<StubIndexExtension<?, ?>> exportableStubIndexExtensions = StubIndexExtension.EP_NAME.getExtensionList();

    Path stubIndexesRoot = chunkRoot.resolve(StubUpdatingIndex.INDEX_ID.getName());
    Path serializerNamesStorageFile = StubSharedIndexExtension.getStubSerializerNamesStorageFile(stubIndexesRoot);
    SerializationManagerImpl chunkSerializationManager = new SerializationManagerImpl(serializerNamesStorageFile, false);

    StubHashBasedIndexGenerator stubGenerator = StubHashBasedIndexGenerator.create(stubIndexesRoot,
                                                                                   chunkSerializationManager,
                                                                                   exportableStubIndexExtensions);

    List<HashBasedIndexGenerator<?, ?>> allGenerators = new ArrayList<>(fileBasedGenerators);
    allGenerators.add(stubGenerator);

    indicator.pushState();
    try(ContentHashEnumerator hashEnumerator = new ContentHashEnumerator(chunkRoot.resolve("hashes"))) {
      openGenerators(allGenerators);

      ReadAction.run(() -> {
        List<VirtualFile> allFiles = collectAllFilesForIndexing(chunk.getRoots(), indicator);

        indicator.setText("Indexing files...");
        indicator.setIndeterminate(false);

        AtomicInteger completedFiles = new AtomicInteger();
        boolean isOk = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
          new ArrayList<>(allFiles),
          indicator,
          f -> {
            double count = completedFiles.incrementAndGet() * 1.0d / allFiles.size();
            if (indicator.getFraction() + 0.007 < count) {
              indicator.setFraction(count);
            }

            generateIndexesForFile(f, allGenerators, hashEnumerator, errorsCollector);
            return true;
          });

        if (!isOk) {
          throw new RuntimeException("Failed to generate indexes in parallel. JobLauncher returned false");
        }
      });
    } finally {
      Disposer.dispose(chunkSerializationManager);
      closeGenerators(allGenerators);
      indicator.popState();
    }

    deleteEmptyIndices(fileBasedGenerators, chunkRoot, false);
    deleteEmptyIndices(stubGenerator.getStubGenerators(), chunkRoot, true);

    zipIndexOut(chunkRoot, zipFile, indicator);
    printStatistics(chunk, fileBasedGenerators, stubGenerator);

    return IndexInfrastructureVersion.fromExtensions(exportableFileBasedIndexExtensions,
                                                     exportableStubIndexExtensions);
  }

  private static void openGenerators(@NotNull List<HashBasedIndexGenerator<?, ?>> allGenerators) {
    for (HashBasedIndexGenerator<?, ?> generator : allGenerators) {
      try {
        generator.openIndex();
      } catch (Exception e) {
        throw new RuntimeException("Failed to open " + generator.getExtension().getName().getName() + ". " + e.getMessage(), e);
      }
    }
  }

  private static void closeGenerators(@NotNull List<HashBasedIndexGenerator<?, ?>> allGenerators) {
    for (HashBasedIndexGenerator<?, ?> generator : allGenerators) {
      try {
        generator.closeIndex();
      } catch (Exception e) {
        throw new RuntimeException("Failed to close " + generator.getExtension().getName().getName() + ". " + e.getMessage(), e);
      }
    }
  }

  private static void deleteEmptyIndices(@NotNull List<HashBasedIndexGenerator<?, ?>> generators,
                                         @NotNull Path chunkRoot,
                                         boolean stubs) {
    Set<String> emptyIndices = new TreeSet<>();
    for (HashBasedIndexGenerator<?, ?> generator : generators) {
      if (generator.isEmpty()) {
        emptyIndices.add(generator.getExtension().getName().getName());
        Path indexRoot = generator.getIndexRoot();
        PathKt.delete(indexRoot);
      }
    }
    try {
      if (stubs) {
        EmptyIndexEnumerator.writeEmptyStubIndexes(chunkRoot, emptyIndices);
      }
      else {
        EmptyIndexEnumerator.writeEmptyIndexes(chunkRoot, emptyIndices);
      }
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to deleteEmptyIndices. " + e.getMessage(), e);
    }
  }

  private static void zipIndexOut(@NotNull Path indexRoot,
                                  @NotNull Path zipFile,
                                  @NotNull ProgressIndicator indicator) {
    indicator.pushState();
    indicator.setIndeterminate(true);
    indicator.setText("Zipping index pack");
    try (JBZipFile file = new JBZipFile(zipFile.toFile())) {
      indicator.setText2("Scanning files...");
      List<Path> allFiles = Files.walk(indexRoot).filter(f -> !Files.isDirectory(f)).collect(Collectors.toList());

      indicator.setIndeterminate(false);
      double count = 0;
      for (Path p : allFiles) {
        String relativePath = indexRoot.relativize(p).toString();

        indicator.setFraction(count++ / allFiles.size());
        indicator.setText2(relativePath);

        try {
          JBZipEntry entry = file.getOrCreateEntry(relativePath);
          entry.setMethod(ZipEntry.STORED);
          entry.setDataFromFile(p.toFile());
        }
        catch (Exception e) {
          throw new RuntimeException("Failed to add " + relativePath + " entry to the target archive. " + e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate indexes archive at " + zipFile + ". " + e.getMessage(), e);
    } finally {
      indicator.popState();
    }
  }

  @NotNull
  private static List<VirtualFile> collectAllFilesForIndexing(@NotNull Set<VirtualFile> roots,
                                                              @NotNull ProgressIndicator indicator) {
    indicator.pushState();
    try {
      indicator.setText("Collecting files to index...");
      indicator.setIndeterminate(true);

      Set<VirtualFile> files = new THashSet<>();
      AtomicLong totalSize = new AtomicLong();

      double count = 0;
      for (VirtualFile root : roots) {
        indicator.setFraction(count ++ / roots.size());
        indicator.setText2(root.getPath());

        VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Boolean>() {
          @Override
          public boolean visitFile(@NotNull VirtualFile file) {
            if (!file.isDirectory() && !SingleRootFileViewProvider.isTooLargeForIntelligence(file)) {
              totalSize.addAndGet(file.getLength());
              files.add(file);
            }
            return true;
          }
        });
      }

      LOG.warn("Collected " + files.size() + " files (" + StringUtil.formatFileSize(totalSize.get()) + ") to index");
      return new ArrayList<>(files);
    } finally {
      indicator.popState();
    }
  }

  private void generateIndexesForFile(@NotNull VirtualFile file,
                                      @NotNull Collection<HashBasedIndexGenerator<?, ?>> generators,
                                      @NotNull ContentHashEnumerator hashEnumerator,
                                      @NotNull ErrorsCollector errorsCollector) {
    FileContentImpl fc;
    try {
      fc = (FileContentImpl)FileContentImpl.createByFile(file, myProject);
    } catch (Exception e) {
      errorsCollector.fileContentError(file, e);
      return;
    }

    int hashId;
    try {
      byte[] hash = IndexedHashesSupport.getOrInitIndexedHash(fc, false);
      if (SystemProperties.getBooleanProperty("shared.index.skip.same.hash", false) && hashEnumerator.hasHashFor(hash)) {
        LOG.info("Duplicate content hash found for: " + file);
        return;
      }

      hashId = Math.abs(hashEnumerator.enumerate(hash));
    } catch (Exception e) {
      errorsCollector.fileHashError(file, e);
      return;
    }

    for (HashBasedIndexGenerator<?, ?> generator : generators) {
      try {
        generator.indexFile(hashId, fc);
      } catch (Exception e) {
        errorsCollector.fileIndexError(file, generator, e);
        //we continue further!
      }
    }
  }

  private static void printStatistics(@NotNull IndexChunk chunk,
                                      @NotNull List<HashBasedIndexGenerator<?, ?>> fileBasedGenerators,
                                      @NotNull StubHashBasedIndexGenerator stubGenerator) {
    StringBuilder stats = new StringBuilder();

    for (Boolean mustBeEmpty : Arrays.asList(true, false)) {
      stats.append("Statistics for index chunk ").append(chunk.getName());
      if (mustBeEmpty) {
        stats.append(" EMPTY ONLY INDEXES ");
      } else {
        stats.append(" NON EMPTY ONLY INDEXES ");
      }
      stats.append("\n");

      stats.append("File based indices (").append(fileBasedGenerators.size()).append("):").append("\n");
      for (HashBasedIndexGenerator<?, ?> generator : fileBasedGenerators) {
        if (mustBeEmpty != (generator.getIndexedFilesNumber() == 0)) continue;
        appendGeneratorStatistics(stats, generator);
      }

      Collection<HashBasedIndexGenerator<?, ?>> stubGenerators = stubGenerator.getStubGenerators();
      stats.append("Stub indices (").append(stubGenerators.size()).append("):").append("\n");
      for (HashBasedIndexGenerator<?, ?> generator : stubGenerators) {
        if (mustBeEmpty != (generator.getIndexedFilesNumber() == 0)) continue;
        appendGeneratorStatistics(stats, generator);
      }

      stats.append("\n\n");
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

  private static class OptimisticErrorsCollector extends ErrorsCollector {
    private final List<RuntimeException> myReports = new ArrayList<>();

    @Override
    protected void logReport(@NotNull RuntimeException e) {
      synchronized (myReports) {
        myReports.add(e);
      }

      LOG.warn(e.getMessage(), e);
    }

    @Override
    public void report() {
      if (myReports.isEmpty()) return;

      LOG.warn("There were " + myReports.size() + " reports collected");
    }
  }

  private static abstract class ErrorsCollector {
    protected abstract void logReport(@NotNull RuntimeException e);

    public abstract void report();

    public void fileContentError(@NotNull VirtualFile file, @NotNull Throwable e) {
      logReport(new RuntimeException("Failed to create FileContent for " + file + ". " + e.getMessage(), e));
    }

    public void fileHashError(@NotNull VirtualFile file, @NotNull Throwable e) {
      logReport(new RuntimeException("Failed to compute hash for file " + file + ". " + e.getMessage(), e));
    }

    public void fileIndexError(VirtualFile file, HashBasedIndexGenerator<?, ?> generator, @NotNull Throwable e) {
      logReport(new RuntimeException("Failed to build " + generator.getExtension().getName().getName() + " index for file " + file + ". " + e.getMessage(), e));
    }
  }
}
