// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.StubIndexExtension;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.IndexInfrastructureVersion;
import com.intellij.util.io.PathKt;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jps.TimingLog.LOG;

public class OnDiskSharedIndexChunkLocator implements SharedIndexChunkLocator {
  public static final String ROOT_PROP = "on.disk.shared.index.root";

  @Override
  public void locateIndex(@NotNull Project project,
                          @NotNull Collection<? extends OrderEntry> entries,
                          @NotNull Consumer<? super ChunkDescriptor> descriptorProcessor,
                          @NotNull ProgressIndicator indicator) {
    String indexRoot = System.getProperty(ROOT_PROP);
    if (indexRoot == null) return;
    File indexZip = new File(indexRoot);
    if (!indexZip.exists() || !indexZip.isFile()) return;
    URI uri = URI.create("jar:" + indexZip.toURI());
    try {
      String name;
      try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
        name = getUniqueChunk(fs);
      }
      if (name == null) return;

      descriptorProcessor.consume(new ChunkDescriptor() {
        @Override
        public @NotNull String getChunkUniqueId() {
          return name;
        }

        @Override
        public @NotNull IndexInfrastructureVersion getSupportedInfrastructureVersion() {
          return IndexInfrastructureVersion.fromExtensions(FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList(),
                                                           StubIndexExtension.EP_NAME.getExtensionList());
        }

        @Override
        public @NotNull Collection<? extends OrderEntry> getOrderEntries() {
          return entries;
        }

        @Override
        public void downloadChunk(@NotNull Path targetFile, @NotNull ProgressIndicator indicator) throws IOException {
          PathKt.copy(indexZip.toPath(), targetFile);
        }
      });
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static String getUniqueChunk(@NotNull FileSystem fs) throws IOException {
    Iterable<Path> directories = fs.getRootDirectories();
    Path root = null;
    for (Path directory : directories) {
      if (root != null) {
        throw new IOException("multiple roots root for " + fs);
      }
      root = directory;
    }
    if (root == null) throw new IOException("no roots in " + fs);
    DirectoryStream<Path> paths = Files.newDirectoryStream(root);
    for (Path path : paths) {
      return StringUtil.trimEnd(path.getFileName().toString(), "/");
    }
    return null;
  }
}
