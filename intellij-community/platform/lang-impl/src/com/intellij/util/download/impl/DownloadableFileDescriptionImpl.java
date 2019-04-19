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

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.util.download.DownloadableFileDescription;
import com.intellij.util.text.UniqueNameGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class DownloadableFileDescriptionImpl implements DownloadableFileDescription {
  private final String myFileName;
  private final String myFileExtension;
  private final String myDownloadUrl;

  public DownloadableFileDescriptionImpl(@NotNull String downloadUrl, @NotNull String fileName, @NotNull String fileExtension) {
    myFileName = fileName;
    myFileExtension = fileExtension.length() > 0 && !fileExtension.startsWith(".") ? "." + fileExtension : fileExtension;
    myDownloadUrl = downloadUrl;
  }

  @NotNull
  @Override
  public String getDownloadUrl() {
    return myDownloadUrl;
  }

  @NotNull
  @Override
  public String getPresentableFileName() {
    return myFileName + myFileExtension;
  }

  @NotNull
  @Override
  public String getPresentableDownloadUrl() {
    return myDownloadUrl;
  }

  @NotNull
  @Override
  public String getDefaultFileName() {
    return generateFileName(Conditions.alwaysTrue());
  }

  @NotNull
  @Override
  public String generateFileName(@NotNull Condition<String> validator) {
    return UniqueNameGenerator.generateUniqueName("", myFileName, myFileExtension, "_", "", validator);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DownloadableFileDescriptionImpl that = (DownloadableFileDescriptionImpl)o;
    return myDownloadUrl.equals(that.myDownloadUrl);
  }

  @Override
  public int hashCode() {
    return myDownloadUrl.hashCode();
  }
}
