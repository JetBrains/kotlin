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
package com.intellij.framework.library.impl;

import com.intellij.framework.library.DownloadableLibraryFileDescription;
import com.intellij.framework.FrameworkAvailabilityCondition;
import com.intellij.framework.library.FrameworkLibraryVersion;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.download.impl.DownloadableFileSetDescriptionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public class FrameworkLibraryVersionImpl extends DownloadableFileSetDescriptionImpl<DownloadableLibraryFileDescription> implements FrameworkLibraryVersion {
  @Nullable private final String myLibraryName;
  @NotNull private final FrameworkAvailabilityCondition myAvailabilityCondition;
  private final String myLibraryCategory;

  public FrameworkLibraryVersionImpl(@Nullable String libraryName,
                                     @NotNull String versionString,
                                     @NotNull FrameworkAvailabilityCondition availabilityCondition,
                                     @NotNull List<DownloadableLibraryFileDescription> libraryFiles,
                                     @NotNull String category) {
    super(category, versionString, libraryFiles);
    myLibraryName = libraryName;
    myAvailabilityCondition = availabilityCondition;
    myLibraryCategory = category;
  }

  @NotNull
  public FrameworkAvailabilityCondition getAvailabilityCondition() {
    return myAvailabilityCondition;
  }

  @NotNull
  @Override
  public String getDefaultLibraryName() {
    String libName = StringUtil.isEmptyOrSpaces(myLibraryName) ? myLibraryCategory : myLibraryName;
    return myVersionString.length() > 0 ? libName + "-" + myVersionString : myLibraryCategory;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return getDefaultLibraryName();
  }

  @Override
  public String getVersionNumber() {
    return getVersionString();
  }
}
