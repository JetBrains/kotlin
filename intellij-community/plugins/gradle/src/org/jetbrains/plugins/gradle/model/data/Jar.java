/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.model.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public class Jar implements Serializable {
  private static final long serialVersionUID = 1L;

  @NotNull
  private final String myName;
  @Nullable
  private File myArchivePath;

  @Nullable
  private String myManifestContent;

  public Jar(@NotNull String name) {
    myName = name;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @Nullable
  public String getManifestContent() {
    return myManifestContent;
  }

  public void setManifestContent(@Nullable String manifestContent) {
    myManifestContent = manifestContent;
  }

  @Nullable
  public File getArchivePath() {
    return myArchivePath;
  }

  public void setArchivePath(@Nullable File archivePath) {
    this.myArchivePath = archivePath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Jar that = (Jar)o;
    if (!myName.equals(that.myName)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Jar{'" + myName + "'}";
  }
}
