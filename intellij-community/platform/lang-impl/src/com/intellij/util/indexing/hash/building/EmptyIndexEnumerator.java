// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.util.io.PathKt;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@VisibleForTesting
public class EmptyIndexEnumerator {
  public static void writeEmptyIndexes(@NotNull Path chunkRoot, @NotNull Set<String> names) throws IOException {
    writeEmptyIndexesToFile(getEmptyIndexFile(chunkRoot), names);
  }

  @NotNull
  public static Set<String> readEmptyIndexes(@NotNull Path chunkRoot) throws IOException {
    return readEmptyIndexesFile(getEmptyIndexFile(chunkRoot));
  }

  public static void writeEmptyStubIndexes(@NotNull Path chunkRoot, @NotNull Set<String> names) throws IOException {
    writeEmptyIndexesToFile(getEmptyStubIndexFile(chunkRoot), names);
  }

  @NotNull
  public static Set<String> readEmptyStubIndexes(@NotNull Path chunkRoot) throws IOException {
    return readEmptyIndexesFile(getEmptyStubIndexFile(chunkRoot));
  }

  private static void writeEmptyIndexesToFile(@NotNull Path path, @NotNull Set<String> names) throws IOException {
    if (!names.isEmpty()) {
      PathKt.write(path, names.stream().sorted().collect(Collectors.joining("\n")));
    }
  }

  @NotNull
  private static Set<String> readEmptyIndexesFile(@NotNull Path path) throws IOException {
    if (!Files.exists(path)) return Collections.emptySet();
    return new LinkedHashSet<>(Files.readAllLines(path));
  }

  @NotNull
  private static Path getEmptyStubIndexFile(@NotNull Path chunkRoot) {
    return chunkRoot.resolve("empty-stub-indices.txt");
  }

  @NotNull
  private static Path getEmptyIndexFile(@NotNull Path chunkRoot) {
    return chunkRoot.resolve("empty-indices.txt");
  }
}
