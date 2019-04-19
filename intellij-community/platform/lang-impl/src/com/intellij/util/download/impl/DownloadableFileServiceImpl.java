/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.download.impl;

import com.intellij.facet.frameworks.beans.Artifact;
import com.intellij.facet.frameworks.beans.ArtifactItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.download.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.URL;
import java.util.List;

/**
 * @author nik
 */
public class DownloadableFileServiceImpl extends DownloadableFileService {
  @NotNull
  @Override
  public DownloadableFileDescription createFileDescription(@NotNull String downloadUrl, @NotNull String fileName) {
    return new DownloadableFileDescriptionImpl(downloadUrl, FileUtil.getNameWithoutExtension(fileName), FileUtilRt.getExtension(fileName));
  }

  @NotNull
  @Override
  public DownloadableFileSetVersions<DownloadableFileSetDescription> createFileSetVersions(@Nullable String groupId,
                                                                                           @NotNull URL... localUrls) {
    return new FileSetVersionsFetcherBase<DownloadableFileSetDescription, DownloadableFileDescription>(groupId, localUrls) {
      @Override
      protected DownloadableFileSetDescription createVersion(Artifact version, List<DownloadableFileDescription> files) {
        return new DownloadableFileSetDescriptionImpl<>(version.getName(), version.getVersion(), files);
      }

      @Override
      protected DownloadableFileDescription createFileDescription(ArtifactItem item, String url, String prefix) {
        return getInstance().createFileDescription(url, item.getName());
      }
    };
  }

  @NotNull
  @Override
  public FileDownloader createDownloader(@NotNull DownloadableFileSetDescription description) {
    return createDownloader(description.getFiles(), description.getName());
  }

  @NotNull
  @Override
  public FileDownloader createDownloader(@NotNull List<? extends DownloadableFileDescription> fileDescriptions,
                                         @NotNull String presentableDownloadName) {
    return new FileDownloaderImpl(fileDescriptions, null, null, presentableDownloadName);
  }

  @NotNull
  @Override
  public FileDownloader createDownloader(@NotNull DownloadableFileSetDescription description,
                                         @Nullable Project project,
                                         JComponent parent) {
    return createDownloader(description.getFiles(), project, parent, description.getName());
  }

  @Override
  @NotNull
  public FileDownloader createDownloader(final List<? extends DownloadableFileDescription> fileDescriptions,
                                         final @Nullable Project project,
                                         JComponent parent, @NotNull String presentableDownloadName) {
    return new FileDownloaderImpl(fileDescriptions, project, parent, presentableDownloadName);
  }
}
