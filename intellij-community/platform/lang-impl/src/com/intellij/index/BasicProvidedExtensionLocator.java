// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.psi.stubs.provided.StubProvidedIndexExtension;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import com.intellij.util.indexing.provided.ProvidedIndexExtensionLocator;
import com.intellij.util.indexing.zipFs.UncompressedZipFileSystem;
import com.intellij.util.indexing.zipFs.UncompressedZipFileSystemProvider;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class BasicProvidedExtensionLocator implements ProvidedIndexExtensionLocator {
  private static final String PREBUILT_INDEX_ZIP_PROP = "prebuilt.hash.index.zip";
  private static final Logger LOG = Logger.getInstance(BasicProvidedExtensionLocator.class);

  private static UncompressedZipFileSystem ourFs;

  @NotNull
  @Override
  public <K, V> Stream<ProvidedIndexExtension<K, V>> findProvidedIndexExtension(@NotNull FileBasedIndexExtension<K, V> originalExtension) {
    if (!originalExtension.dependsOnFileContent()) return Stream.empty();
    Path root = getPrebuiltIndexPath();
    if (root == null || !Files.exists(root)) return Stream.empty();

    try {
      // TODO properly close it
      UncompressedZipFileSystem fs = getFs(root);

      Path fsRoot = fs.getPath("/").getRoot();
      return Files
              .list(fsRoot)
              .sorted(Comparator.comparing(p -> p.getFileName().toString()))
              .map(p -> p.resolve(StringUtil.toLowerCase(originalExtension.getName().getName())))
              .map(p -> {
                return originalExtension.getName().equals(StubUpdatingIndex.INDEX_ID)
                        ? (ProvidedIndexExtension<K, V>)new StubProvidedIndexExtension(p)
                        : new ProvidedIndexExtensionImpl<>(p, originalExtension);
              });
    } catch (IOException e) {
      LOG.error(e);
      return Stream.empty();
    }
  }

  private synchronized static UncompressedZipFileSystem getFs(@NotNull Path root) throws IOException {
    if (ourFs == null) {
      ourFs = new UncompressedZipFileSystem(root, new UncompressedZipFileSystemProvider());
      ShutDownTracker.getInstance().registerShutdownTask(() -> {
        try {
          ourFs.close();
        }
        catch (IOException e) {
          LOG.error(e);
        }
      });
    }
    return ourFs;
  }

  @Nullable
  private static Path getPrebuiltIndexPath() {
    String path = System.getProperty(PREBUILT_INDEX_ZIP_PROP);
    if (path == null) return null;
    Path file = Paths.get(path);
    return Files.exists(file) ? file : null;
  }

  private static class ProvidedIndexExtensionImpl<K, V> implements ProvidedIndexExtension<K, V> {
    @NotNull
    private final Path myIndexFile;
    @NotNull
    private final ID<K, V> myIndexId;
    @NotNull
    private final KeyDescriptor<K> myKeyDescriptor;
    @NotNull
    private final DataExternalizer<V> myValueExternalizer;

    private ProvidedIndexExtensionImpl(@NotNull Path file,
                                       @NotNull FileBasedIndexExtension<K, V> originalExtension) {
      myIndexFile = file;
      myIndexId = originalExtension.getName();
      myKeyDescriptor = originalExtension.getKeyDescriptor();
      myValueExternalizer = originalExtension.getValueExternalizer();
    }

    @NotNull
    @Override
    public Path getIndexPath() {
      return myIndexFile;
    }

    @NotNull
    @Override
    public ID<K, V> getIndexId() {
      return myIndexId;
    }

    @NotNull
    @Override
    public KeyDescriptor<K> createKeyDescriptor() {
      return myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<V> createValueExternalizer() {
      return myValueExternalizer;
    }
  }
}
