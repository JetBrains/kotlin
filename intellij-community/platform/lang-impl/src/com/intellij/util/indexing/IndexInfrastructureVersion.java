// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.psi.stubs.StubIndexExtension;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.snapshot.IndexedHashesSupport;
import com.intellij.util.io.PersistentEnumeratorDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class IndexInfrastructureVersion {
  private static final String SHARED_INDEXES_VERSIONS_FORMAT_VERSION = "0";

  // base versions: it is required to have 100% match on that indexes in order to load it to an IDE.
  private final SortedMap<String, String> myBaseIndexes;

  // files-based indexes versions: indexes are loadable even if some indexes does not match
  private final SortedMap<String, String> myFileBasedIndexVersions;

  // stub indexes versions: it is on if some indexes does not match
  private final SortedMap<String, String> myStubIndexVersions;

  public IndexInfrastructureVersion(@NotNull Map<String, String> baseIndexes,
                                    @NotNull Map<String, String> fileBasedIndexVersions,
                                    @NotNull Map<String, String> stubIndexVersions) {
    myBaseIndexes = ImmutableSortedMap.copyOf(baseIndexes);
    myFileBasedIndexVersions = ImmutableSortedMap.copyOf(fileBasedIndexVersions);
    myStubIndexVersions = ImmutableSortedMap.copyOf(stubIndexVersions);
  }

  @Override
  public String toString() {
    return "IndexInfrastructureVersion{" +
           "myBaseIndexes=" + myBaseIndexes +
           ", myFileBasedIndexVersions=" + myFileBasedIndexVersions +
           ", myStubIndexVersions=" + myStubIndexVersions +
           '}';
  }

  @NotNull
  public static IndexInfrastructureVersion getIdeVersion() {
    return fromExtensions(
      FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList(),
      StubIndexExtension.EP_NAME.getExtensionList());
  }

  @NotNull
  @SuppressWarnings("HardCodedStringLiteral")
  private static SortedMap<String, String> globalIndexesVersion() {
    ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
    builder.put("shared_indexes_format", SHARED_INDEXES_VERSIONS_FORMAT_VERSION);
    builder.put("os", Os.getOs().getOsName()); //do we really need the OS here?
    builder.put("vfs_version", String.valueOf(FSRecords.getVersion()));
    builder.put("persistent_enumerator_version", String.valueOf(PersistentEnumeratorDelegate.getVersion()));
    builder.put("use_btree", String.valueOf(PersistentEnumeratorDelegate.useBtree()));
    builder.put("hashes_enumerator_version", String.valueOf(ContentHashEnumerator.getVersion()));
    builder.put("hashes_algorithm_version", String.valueOf(IndexedHashesSupport.getVersion()));
    builder.put("inverted_index_version", String.valueOf(VfsAwareMapReduceIndex.VERSION));

    //register your index here if that version is vital for shared indexes loading
    return builder.build();
  }

  @NotNull
  private static SortedMap<String, String> filedBasedIndexVersions(@NotNull List<FileBasedIndexExtension<?, ?>> fileBasedIndexExtensions) {
    ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();

    for (FileBasedIndexExtension<?, ?> fileBasedIndexExtension : fileBasedIndexExtensions) {
      builder.put(getFileBasedIndexNameName(fileBasedIndexExtension.getName()), getFileBasedIndexVersion(fileBasedIndexExtension));
    }

    return builder.build();
  }

  @NotNull
  private static String getFileBasedIndexVersion(@NotNull FileBasedIndexExtension<?, ?> fileBasedIndexExtension) {
    return String.valueOf(fileBasedIndexExtension.getVersion());
  }

  @NotNull
  private static String getFileBasedIndexNameName(@NotNull ID<?, ?> fileBasedIndexId) {
    return fileBasedIndexId.getName();
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
  public Map<String, String> getBaseIndexes() {
    return myBaseIndexes;
  }

  @NotNull
  public Map<String, String> getFileBasedIndexVersions() {
    return myFileBasedIndexVersions;
  }

  @NotNull
  public Map<String, String> getStubIndexVersions() {
    return myStubIndexVersions;
  }


  /**
   * Selects the versions from the given list {@oaram versions}'s that has the best similar versions
   * for shared indexes to apply. We try to pick the version with would allow us to make the minimal
   * extra work to get to the fully indexed state.
   *
   * @return It returns the best matching element from {@oaram versions} or {@code null} if none are
   * good enough or suitable
   */
  @Nullable
  public IndexInfrastructureVersion pickBestSuitableVersion(@NotNull Collection<IndexInfrastructureVersion> versions) {
    Integer bestScore = null;
    IndexInfrastructureVersion bestVersion = null;

    for (IndexInfrastructureVersion version : versions) {
      //we make sure the base index versions are exactly the same
      //it is required to be able to read indexes files on the IDE
      if (!version.getBaseIndexes().equals(getBaseIndexes())) continue;

      // stub indexes has to be grouped by file types
      // because they can only be rebuild if a stub tree for
      // a given file is openned or re-built
      // right now we simply ignore that fact
      int commonStubIndexesCount = Sets.intersection(
        version.getStubIndexVersions().entrySet(),
        getStubIndexVersions().entrySet()
      ).size();

      //a magic number - we skip loading if there are too few indexes matched
      //should we check for the StubUpdatingIndex here as well?
      if (commonStubIndexesCount < 3) continue;

      //how may of included in the remote index versions are different?
      int stubIndexesDiff = Sets.difference(
        version.getStubIndexVersions().entrySet(),
        getStubIndexVersions().entrySet()
      ).size();


      //how many common indexes are there?
      int commonFileIndexesCount = Sets.intersection(
        version.getFileBasedIndexVersions().entrySet(),
        getFileBasedIndexVersions().entrySet()
      ).size();

      //a magic number - we skip loading if there are too few indexes matched
      //should we check for the StubUpdatingIndex here as well?
      if (commonFileIndexesCount < 3) continue;

      // how many of included in the downloadable index versions are different?
      int fileBasedDiff = Sets.difference(
        version.getFileBasedIndexVersions().entrySet(),
        getFileBasedIndexVersions().entrySet()
      ).size();

      int negativeScope = fileBasedDiff + stubIndexesDiff;

      //computing min of the score
      if (bestScore == null || bestScore > negativeScope) {
        bestScore = negativeScope;
        bestVersion = version;
      }
    }

    return bestVersion;
  }

  @NotNull
  public IndexInfrastructureVersion pickOnlyRestrictedIndexes(@NotNull IndexInfrastructureVersion targetVersion) {
    if (!getBaseIndexes().equals(targetVersion.getBaseIndexes())) {
      throw new IllegalArgumentException("Base versions should be exactly the same");
    }

    Map<String, String> targetFbiVersions = targetVersion.getFileBasedIndexVersions();
    Map<String, String> fbiVersions = getFileBasedIndexVersions();
    Map<String, String> restrictedFbiIndexes = Maps.filterEntries(fbiVersions, e -> {
      assert e != null;
      return !e.getValue().equals(targetFbiVersions.get(e.getKey()));
    });

    Map<String, String> restrictedStubIndexes;
    if (restrictedFbiIndexes.containsKey(getFileBasedIndexNameName(StubUpdatingIndex.INDEX_ID))) {
      Map<String, String> targetStubVersions = targetVersion.getStubIndexVersions();
      Map<String, String> stubVersions = getStubIndexVersions();
      restrictedStubIndexes = Maps.filterEntries(stubVersions, e -> {
        assert e != null;
        return !e.getValue().equals(targetStubVersions.get(e.getKey()));
      });
    } else {
      restrictedStubIndexes = getStubIndexVersions();
    }

    return new IndexInfrastructureVersion(myBaseIndexes, restrictedFbiIndexes, restrictedStubIndexes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IndexInfrastructureVersion version = (IndexInfrastructureVersion)o;
    return myBaseIndexes.equals(version.myBaseIndexes) &&
           myFileBasedIndexVersions.equals(version.myFileBasedIndexVersions) &&
           myStubIndexVersions.equals(version.myStubIndexVersions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myBaseIndexes, myFileBasedIndexVersions, myStubIndexVersions);
  }

  public enum Os {
    windows, mac, linux;

    @NotNull
    public String getOsName() {
      return toString();
    }

    @NotNull
    public static IndexInfrastructureVersion.Os getOs() {
      if (SystemInfo.isWindows) return windows;
      if (SystemInfo.isMac) return mac;
      if (SystemInfo.isLinux) return linux;
      throw new Error("Unknown OS. " + SystemInfo.getOsNameAndVersion());
    }
  }
}
