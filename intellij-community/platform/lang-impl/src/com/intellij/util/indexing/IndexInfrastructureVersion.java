// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.psi.stubs.StubIndexExtension;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.io.DigestUtil;
import com.intellij.util.io.PersistentEnumeratorDelegate;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class IndexInfrastructureVersion {
  // base versions: it is required to have 100% match on that indexes in order to load it to an IDE.
  private final SortedMap<String, String> myBaseIndexes;

  // files-based indexes versions: indexes are loadable even if some indexes does not match
  private final SortedMap<String, String> myFileBasedIndexVersions;

  // stub indexes versions: it is on if some indexes does not match
  private final SortedMap<String, String> myStubIndexVersions;

  private IndexInfrastructureVersion(@NotNull SortedMap<String, String> baseIndexes,
                                     @NotNull SortedMap<String, String> fileBasedIndexVersions,
                                     @NotNull SortedMap<String, String> stubIndexVersions) {
    myBaseIndexes = ImmutableSortedMap.copyOf(baseIndexes);
    myFileBasedIndexVersions = ImmutableSortedMap.copyOf(fileBasedIndexVersions);
    myStubIndexVersions = ImmutableSortedMap.copyOf(stubIndexVersions);
  }

  @NotNull
  public static IndexInfrastructureVersion globalVersion() {
    return fromExtensions(
      FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList(),
      StubIndexExtension.EP_NAME.getExtensionList());
  }

  @NotNull
  private static SortedMap<String, String> globalIndexesVersion() {
    ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
    builder.put("shared_indexes_format", "0");
    builder.put("os", getOs().getOsName()); //do we really need the OS here?
    builder.put("vfs_version", String.valueOf(FSRecords.getVersion()));
    builder.put("persistent_enumerator_version", String.valueOf(PersistentEnumeratorDelegate.getVersion()));
    builder.put("use_btree", String.valueOf(PersistentEnumeratorDelegate.useBtree()));
    builder.put("hashes_enumerator_versions", String.valueOf(ContentHashEnumerator.getVersion()));
    builder.put("inverted_index_version", String.valueOf(VfsAwareMapReduceIndex.VERSION));

    //register your index here if that version is vital for shared indexes loading
    return builder.build();
  }

  @NotNull
  private static SortedMap<String, String> filedBasedIndexVersions(@NotNull List<FileBasedIndexExtension<?, ?>> fileBasedIndexExtensions) {
    ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();

    for (FileBasedIndexExtension<?, ?> fileBasedIndexExtension : fileBasedIndexExtensions) {
      builder.put(fileBasedIndexExtension.getName().getName(), String.valueOf(fileBasedIndexExtension.getVersion()));
    }

    return builder.build();
  }

  @NotNull
  private static SortedMap<String, String> stubIndexVersions(@NotNull List<StubIndexExtension<?, ?>> stubIndexExtensions) {
    ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();

    FileBasedIndexExtension<?, ?> stubUpdatingIndex =
      FileBasedIndexExtension.EXTENSION_POINT_NAME.findFirstSafe(ex -> ex.getName().equals(StubUpdatingIndex.INDEX_ID));

    if (stubUpdatingIndex == null) {
      throw new RuntimeException("Failed to find " + StubUpdatingIndex.INDEX_ID);
    }

    String commonPrefix = stubUpdatingIndex.getVersion() + ":";
    for (StubIndexExtension<?, ?> ex : stubIndexExtensions) {
      builder.put(ex.getKey().getName(), commonPrefix + ex.getVersion());
    }

    return builder.build();
  }

  @NotNull
  public static IndexInfrastructureVersion fromExtensions(@NotNull List<FileBasedIndexExtension<?, ?>> fileBasedIndexExtensions,
                                                          @NotNull List<StubIndexExtension<?, ?>> stubIndexExtensions) {
    return new IndexInfrastructureVersion(globalIndexesVersion(),
                                          filedBasedIndexVersions(fileBasedIndexExtensions),
                                          stubIndexVersions(stubIndexExtensions));
  }

  /**
   * @return a weak global version, to be used in file names or other
   * places to refer to the global state
   */
  @NotNull
  public String getWeakVersionHash() {
    Hasher hasher = Hashing.sha256().newHasher();
    for (SortedMap<String, String> versions : Arrays.asList(myBaseIndexes, myFileBasedIndexVersions, myStubIndexVersions)) {
      for (Map.Entry<String, String> entry : versions.entrySet()) {
        hasher.putString(entry.getKey(), StandardCharsets.UTF_8);
        hasher.putString(entry.getValue(), StandardCharsets.UTF_8);
      }
    }
    return StringUtil.first(hasher.hash().toString(), 8, false);
  }

  @NotNull
  public SortedMap<String, String> getBaseIndexes() {
    return myBaseIndexes;
  }

  @NotNull
  public SortedMap<String, String> getFileBasedIndexVersions() {
    return myFileBasedIndexVersions;
  }

  @NotNull
  public SortedMap<String, String> getStubIndexVersions() {
    return myStubIndexVersions;
  }

  public enum Os {
    WINDOWS, MAC, LINUX;

    @NotNull
    public String getOsName() {
      switch (this) {
        case WINDOWS:
          return "windows";
        case MAC:
          return "mac";
        default:
          return "linux";
      }
    }

    @Override
    public String toString() {
      return getOsName();
    }
  }

  @NotNull
  public static IndexInfrastructureVersion.Os getOs() {
    if (SystemInfo.isWindows) return Os.WINDOWS;
    if (SystemInfo.isMac) return Os.MAC;
    if (SystemInfo.isLinux) return Os.LINUX;
    throw new Error("Unknown OS. " + SystemInfo.getOsNameAndVersion());
  }
}
