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

import com.intellij.util.download.DownloadableFileDescription;
import com.intellij.util.download.DownloadableFileSetDescription;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author nik
 */
public class DownloadableFileSetDescriptionImpl<F extends DownloadableFileDescription> implements DownloadableFileSetDescription {
  protected final List<F> myFiles;
  protected final String myVersionString;
  private final String myName;

  public DownloadableFileSetDescriptionImpl(@NotNull String name,
                                            @NotNull String versionString,
                                            @NotNull List<F> files) {
    myName = name;
    myVersionString = versionString;
    myFiles = files;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @NotNull
  @Override
  public String getVersionString() {
    return myVersionString;
  }

  @NotNull
  @Override
  public List<F> getFiles() {
    return myFiles;
  }
}
