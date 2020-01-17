// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.framework.library.impl;

import com.intellij.facet.frameworks.beans.Artifact;
import com.intellij.facet.frameworks.beans.ArtifactItem;
import com.intellij.framework.FrameworkAvailabilityCondition;
import com.intellij.framework.library.DownloadableLibraryDescription;
import com.intellij.framework.library.DownloadableLibraryFileDescription;
import com.intellij.framework.library.FrameworkLibraryVersion;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.download.impl.FileSetVersionsFetcherBase;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.List;

public class LibraryVersionsFetcher extends FileSetVersionsFetcherBase<FrameworkLibraryVersion, DownloadableLibraryFileDescription> implements DownloadableLibraryDescription {

  public LibraryVersionsFetcher(@NotNull String groupId, URL @NotNull [] localUrls) {
    super(groupId, localUrls);
  }

  @Override
  protected FrameworkLibraryVersion createVersion(Artifact version, List<DownloadableLibraryFileDescription> files) {
    return new FrameworkLibraryVersionImpl(version.getName(), version.getVersion(), createAvailabilityCondition(version), files, myGroupId);
  }

  @NotNull
  protected FrameworkAvailabilityCondition createAvailabilityCondition(Artifact version) {
    return FrameworkAvailabilityCondition.ALWAYS_TRUE;
  }

  @Override
  protected DownloadableLibraryFileDescription createFileDescription(ArtifactItem item, String url, String prefix) {
    String sourceUrl = item.getSourceUrl();
    if (sourceUrl != null) {
      sourceUrl = prependPrefix(sourceUrl, prefix);
    }
    String docUrl = item.getDocUrl();
    if (docUrl != null) {
      docUrl = prependPrefix(docUrl, prefix);
    }
    final String name = item.getName();
    return new DownloadableLibraryFileDescriptionImpl(url, FileUtilRt.getNameWithoutExtension(name), FileUtilRt.getExtension(name), sourceUrl, docUrl, item.isOptional());
  }
}
