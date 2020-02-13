// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.util.indexing.IndexInfrastructureVersion;
import com.intellij.util.indexing.provided.SharedIndexChunkLocator;
import com.intellij.util.indexing.zipFs.UncompressedZipFileSystem;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.zip.ZipEntry;

public class SharedIndexStorageUtil {
  /**
   * Appends a chunk {@param fromChunk} to shared index storage {@param appendStorage}
   * and filter out indexes with mismatched versions.
   */
  public static void appendToSharedIndexStorage(@NotNull Path fromChunk,
                                                @NotNull Path appendStorage,
                                                @NotNull SharedIndexChunkLocator.ChunkDescriptor descriptor,
                                                @NotNull IndexInfrastructureVersion targetVersion) throws IOException {
    IndexInfrastructureVersion onlyRestrictedVersions = descriptor.getSupportedInfrastructureVersion().pickOnlyRestrictedIndexes(targetVersion);

    try (@NotNull JBZipFile chunkStorage = new JBZipFile(appendStorage.toFile());
         @NotNull UncompressedZipFileSystem tempChunkFs = UncompressedZipFileSystem.create(fromChunk)) {

      String targetBasePath = descriptor.getChunkUniqueId() + "/";
      Path chunkRoot = tempChunkFs.getRootDirectory();
      Set<String> restrictedFbiIndexes = onlyRestrictedVersions.getFileBasedIndexVersions().keySet();

      Files.walkFileTree(chunkRoot, new SimpleFileVisitor<Path>() {
        int fbiLevelIndex = 0;
        int stubLevelIndex = 1;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (restrictedFbiIndexes.contains(file.getName(fbiLevelIndex).toString())) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          if (!Files.isDirectory(file)) {
            JBZipEntry createdEntry = chunkStorage.getOrCreateEntry(targetBasePath + file.toString());
            createdEntry.setMethod(ZipEntry.STORED);
            createdEntry.setData(Files.readAllBytes(file));
          }

          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
}
