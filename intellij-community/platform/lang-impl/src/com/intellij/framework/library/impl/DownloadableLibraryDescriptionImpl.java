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

import com.intellij.framework.library.DownloadableLibraryDescription;
import com.intellij.framework.library.FrameworkLibraryVersion;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author nik
 */
public class DownloadableLibraryDescriptionImpl implements DownloadableLibraryDescription {
  private final List<FrameworkLibraryVersion> myVersions;

  public DownloadableLibraryDescriptionImpl(List<FrameworkLibraryVersion> versions) {
    myVersions = versions;
  }

  public List<? extends FrameworkLibraryVersion> getVersions() {
    return myVersions;
  }

  @Override
  public void fetchVersions(@NotNull FileSetVersionsCallback<FrameworkLibraryVersion> callback) {
    callback.onSuccess(myVersions);
  }

  @NotNull
  @Override
  public List<FrameworkLibraryVersion> fetchVersions() {
    return myVersions;
  }
}
