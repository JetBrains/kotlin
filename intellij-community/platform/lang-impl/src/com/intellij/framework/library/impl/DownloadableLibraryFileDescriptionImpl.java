/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.framework.library.impl;

import com.intellij.framework.library.DownloadableLibraryFileDescription;
import com.intellij.util.download.impl.DownloadableFileDescriptionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class DownloadableLibraryFileDescriptionImpl extends DownloadableFileDescriptionImpl implements DownloadableLibraryFileDescription {
  private final DownloadableFileDescriptionImpl mySourceDownloadUrl;
  private final DownloadableFileDescriptionImpl myDocumentationDownloadUrl;
  private final boolean myOptional;

  public DownloadableLibraryFileDescriptionImpl(@NotNull String downloadUrl,
                                                @NotNull String fileName,
                                                @NotNull String fileExtension,
                                                @Nullable String sourceDownloadUrl,
                                                @Nullable String documentationDownloadUrl,
                                                boolean optional) {
    super(downloadUrl, fileName, fileExtension);
    mySourceDownloadUrl = sourceDownloadUrl != null ? new DownloadableFileDescriptionImpl(sourceDownloadUrl, fileName +"-sources", fileExtension) : null;
    myDocumentationDownloadUrl = documentationDownloadUrl != null ? new DownloadableFileDescriptionImpl(documentationDownloadUrl, fileName+"-javadoc", fileExtension) : null;
    myOptional = optional;
  }

  @Override
  public DownloadableFileDescriptionImpl getSourcesDescription() {
    return mySourceDownloadUrl;
  }

  @Override
  public DownloadableFileDescriptionImpl getDocumentationDescription() {
    return myDocumentationDownloadUrl;
  }

  @Override
  public boolean isOptional() {
    return myOptional;
  }
}
