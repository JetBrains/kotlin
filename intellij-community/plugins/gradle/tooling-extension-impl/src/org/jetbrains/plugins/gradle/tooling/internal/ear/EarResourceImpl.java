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
package org.jetbrains.plugins.gradle.tooling.internal.ear;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ear.EarConfiguration;

import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class EarResourceImpl implements EarConfiguration.EarResource {
  @NotNull
  private final String myEarDirectory;
  @NotNull
  private final String myRelativePath;
  @NotNull
  private final File file;

  public EarResourceImpl(@NotNull String earDirectory, @NotNull String relativePath, @NotNull File file) {
    myEarDirectory = earDirectory;
    this.myRelativePath = relativePath;
    this.file = file;
  }

  @NotNull
  @Override
  public String getEarDirectory() {
    return myEarDirectory;
  }

  @NotNull
  @Override
  public String getRelativePath() {
    return myRelativePath;
  }

  @NotNull
  @Override
  public File getFile() {
    return file;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EarResourceImpl)) return false;

    EarResourceImpl resource = (EarResourceImpl)o;
    if (!file.getPath().equals(resource.file.getPath())) return false;
    if (myEarDirectory != resource.myEarDirectory) return false;
    if (!myRelativePath.equals(resource.myRelativePath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myEarDirectory.hashCode();
    result = 31 * result + myRelativePath.hashCode();
    result = 31 * result + file.getPath().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "EarResource{" +
           "earDirectory=" + myEarDirectory +
           ", warRelativePath='" + myRelativePath + '\'' +
           ", file=" + file +
           '}';
  }
}
