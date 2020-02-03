// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.jetbrains.jps.TimingLog.LOG;

public class OnDiskSharedIndexChunkLocator implements SharedIndexChunkLocator {
  public static final String ROOT_PROP = "on.disk.shared.index.root";

  @Override
  public void locateIndex(@NotNull Project project,
                          @NotNull Set<OrderEntry> entries,
                          @NotNull ProgressIndicator indicator,
                          @NotNull SharedIndexHandler handler) {
    loadZip(handler, indicator);
  }

  private static void loadZip(@NotNull SharedIndexChunkLocator.SharedIndexHandler handler,
                              @NotNull ProgressIndicator indicator) {
    String indexRoot = System.getProperty(ROOT_PROP);
    if (indexRoot == null) return;
    File indexZip = new File(indexRoot);
    if (!indexZip.exists() || !indexZip.isFile()) return;
    URI uri = URI.create("jar:" + indexZip.toURI());
    try {
      ChunkDescriptor name = null;
      try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
        name = getUniqueChunk(fs);
      }
      if (name == null || !handler.onIndexAvailable(name)) return;
      indicator.checkCanceled();

      handler.onIndexReceived(name, Files.newInputStream(indexZip.toPath()));
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static ChunkDescriptor getUniqueChunk(@NotNull FileSystem fs) throws IOException {
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
      String chunkName = StringUtil.trimEnd(path.getFileName().toString(), "/");
      return new ChunkDescriptor(chunkName);
    }
    return null;
  }
}
