// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.download.impl;

import com.intellij.facet.frameworks.LibrariesDownloadAssistant;
import com.intellij.facet.frameworks.beans.Artifact;
import com.intellij.facet.frameworks.beans.ArtifactItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.download.DownloadableFileDescription;
import com.intellij.util.download.DownloadableFileSetDescription;
import com.intellij.util.download.DownloadableFileSetVersions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class FileSetVersionsFetcherBase<FS extends DownloadableFileSetDescription, F extends DownloadableFileDescription> implements DownloadableFileSetVersions<FS> {
  private static final Comparator<DownloadableFileSetDescription> VERSIONS_COMPARATOR =
    (o1, o2) -> -StringUtil.compareVersionNumbers(o1.getVersionString(), o2.getVersionString());
  protected final String myGroupId;
  private final URL[] myLocalUrls;

  public FileSetVersionsFetcherBase(@Nullable String groupId, URL @NotNull [] localUrls) {
    myLocalUrls = localUrls;
    myGroupId = groupId;
  }

  @Override
  public void fetchVersions(@NotNull final FileSetVersionsCallback<FS> callback) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> callback.onSuccess(fetchVersions()));
  }

  @NotNull
  @Override
  public List<FS> fetchVersions() {
    ApplicationManagerEx.getApplicationEx().assertTimeConsuming();
    final Artifact[] versions;
    if (myGroupId != null) {
      versions = LibrariesDownloadAssistant.getVersions(myGroupId, myLocalUrls);
    }
    else {
      versions = LibrariesDownloadAssistant.getVersions(myLocalUrls);
    }
    final List<FS> result = new ArrayList<>();
    for (Artifact version : versions) {
      final ArtifactItem[] items = version.getItems();
      final List<F> files = new ArrayList<>();
      for (ArtifactItem item : items) {
        String url = item.getUrl();
        final String prefix = version.getUrlPrefix();
        if (url == null) {
          if (prefix != null) {
            url = prefix + item.getName();
          }
        } else {
          url = prependPrefix(url, prefix);
        }
        assert url != null;

        files.add(createFileDescription(item, url, prefix));
      }
      result.add(createVersion(version, files));
    }
    result.sort(VERSIONS_COMPARATOR);
    return result;
  }

  @NotNull
  protected static String prependPrefix(@NotNull String url, @Nullable String prefix) {
    if (!url.startsWith("http://") && prefix != null) {
      url = prefix + url;
    }
    return url;
  }

  protected abstract F createFileDescription(ArtifactItem item, String url, @Nullable String prefix);

  protected abstract FS createVersion(Artifact version, List<F> files);
}
