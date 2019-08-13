// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;

public final class ScalaModelData extends AbstractExternalEntityData {
  private static final int PROCESSING_AFTER_BUILTIN_SERVICES = ProjectKeys.TASK.getProcessingWeight() + 1;

  @NotNull
  public static final Key<ScalaModelData> KEY = Key.create(ScalaModelData.class, PROCESSING_AFTER_BUILTIN_SERVICES);
  private Set<File> scalaClasspath;
  private Set<File> zincClasspath;
  private ScalaCompileOptionsData scalaCompileOptions;
  private String sourceCompatibility;
  private String targetCompatibility;

  @PropertyMapping("owner")
  public ScalaModelData(@NotNull ProjectSystemId owner) {
    super(owner);
  }

  public Set<File> getScalaClasspath() {
    return scalaClasspath;
  }

  public void setScalaClasspath(Set<File> scalaClasspath) {
    this.scalaClasspath = scalaClasspath;
  }

  public Set<File> getZincClasspath() {
    return zincClasspath;
  }

  public void setZincClasspath(Set<File> zincClasspath) {
    this.zincClasspath = zincClasspath;
  }

  public ScalaCompileOptionsData getScalaCompileOptions() {
    return scalaCompileOptions;
  }

  public void setScalaCompileOptions(ScalaCompileOptionsData scalaCompileOptions) {
    this.scalaCompileOptions = scalaCompileOptions;
  }

  public String getSourceCompatibility() {
    return sourceCompatibility;
  }

  public void setSourceCompatibility(String sourceCompatibility) {
    this.sourceCompatibility = sourceCompatibility;
  }

  public String getTargetCompatibility() {
    return targetCompatibility;
  }

  public void setTargetCompatibility(String targetCompatibility) {
    this.targetCompatibility = targetCompatibility;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScalaModelData)) return false;
    if (!super.equals(o)) return false;

    ScalaModelData data = (ScalaModelData)o;

    if (scalaClasspath != null ? !scalaClasspath.equals(data.scalaClasspath) : data.scalaClasspath != null) return false;
    if (scalaCompileOptions != null ? !scalaCompileOptions.equals(data.scalaCompileOptions) : data.scalaCompileOptions != null) {
      return false;
    }
    if (sourceCompatibility != null ? !sourceCompatibility.equals(data.sourceCompatibility) : data.sourceCompatibility != null) {
      return false;
    }
    if (targetCompatibility != null ? !targetCompatibility.equals(data.targetCompatibility) : data.targetCompatibility != null) {
      return false;
    }
    if (zincClasspath != null ? !zincClasspath.equals(data.zincClasspath) : data.zincClasspath != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (scalaClasspath != null ? scalaClasspath.hashCode() : 0);
    result = 31 * result + (zincClasspath != null ? zincClasspath.hashCode() : 0);
    result = 31 * result + (scalaCompileOptions != null ? scalaCompileOptions.hashCode() : 0);
    result = 31 * result + (sourceCompatibility != null ? sourceCompatibility.hashCode() : 0);
    result = 31 * result + (targetCompatibility != null ? targetCompatibility.hashCode() : 0);
    return result;
  }
}
