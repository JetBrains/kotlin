// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.psi.stubs.StubIndexExtension;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.io.PersistentEnumeratorDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class IndexInfrastructureVersion {

  // base version

  // file format version
  private final int myFormatVersion;

  // OS version
  private final OsName myOs;

  // vfs
  private final int myVfsVersion;

  // persistent maps
  private final int myPersistentEnumeratorVersion;
  private final boolean myUseBTree;

  // content hash enumerator
  private final int myHashesEnumeratorVersion;

  // index storage
  private final int myInvertedIndexVersion;

  // index implementation versions

  // files-based indexes versions
  private final SortedMap<String, Integer> myFileBasedIndexVersions;

  // stub indexes versions
  private final SortedMap<String, StubIndexVersion> myStubIndexVersions;

  public IndexInfrastructureVersion() {
    this(FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList(), StubIndexExtension.EP_NAME.getExtensionList());
  }

  public IndexInfrastructureVersion(@NotNull List<FileBasedIndexExtension<?, ?>> fileBasedIndexExtensions,
                                    @NotNull List<StubIndexExtension<?, ?>> stubIndexExtensions) {
    myFormatVersion = 0;
    myOs = getOsName();
    myVfsVersion = FSRecords.getVersion();
    myPersistentEnumeratorVersion = PersistentEnumeratorDelegate.getVersion();
    myUseBTree = PersistentEnumeratorDelegate.useBtree();
    myHashesEnumeratorVersion = ContentHashEnumerator.getVersion();
    myInvertedIndexVersion = VfsAwareMapReduceIndex.VERSION;

    myFileBasedIndexVersions = new TreeMap<>();
    for (FileBasedIndexExtension<?, ?> fileBasedIndexExtension : fileBasedIndexExtensions) {
      myFileBasedIndexVersions.put(fileBasedIndexExtension.getName().getName(), fileBasedIndexExtension.getVersion());
    }

    FileBasedIndexExtension<?, ?> stubUpdatingIndex =
      FileBasedIndexExtension.EXTENSION_POINT_NAME.findFirstSafe(ex -> ex.getName().equals(StubUpdatingIndex.INDEX_ID));

    int stubUpdatingIndexVersion = stubUpdatingIndex == null ? 0 : stubUpdatingIndex.getVersion();
    myStubIndexVersions = new TreeMap<>();
    for (StubIndexExtension<?, ?> ex : stubIndexExtensions) {
      myStubIndexVersions.put(ex.getKey().getName(), new StubIndexVersion(stubUpdatingIndexVersion, ex.getVersion()));
    }
  }

  @NotNull
  public SortedMap<String, Integer> getFileBasedIndexVersions() {
    return myFileBasedIndexVersions;
  }

  @NotNull
  public SortedMap<String, StubIndexVersion> getStubIndexVersions() {
    return myStubIndexVersions;
  }

  public static class StubIndexVersion {
    final int stubUpdatingIndexVersion;
    final int version;

    private StubIndexVersion(int stubUpdatingIndexVersion, int version) {
      this.stubUpdatingIndexVersion = stubUpdatingIndexVersion;
      this.version = version;
    }

    @Override
    public String toString() {
      return stubUpdatingIndexVersion + ":" + version;
    }
  }

  private enum OsName {
    windows, mac, linux
  }

  @NotNull
  public static OsName getOsName() {
    if (SystemInfo.isWindows) return OsName.windows;
    if (SystemInfo.isMac) return OsName.mac;
    if (SystemInfo.isLinux) return OsName.linux;
    throw new Error("Unknown OS. " + SystemInfo.getOsNameAndVersion());
  }
}
