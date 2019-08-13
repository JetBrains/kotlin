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
package org.jetbrains.plugins.gradle;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalDependencyId implements ExternalDependencyId, Serializable {

  private static final long serialVersionUID = 1L;

  private String group;
  private String name;
  private String version;
  @NotNull
  private String packaging = "jar";
  @Nullable
  private String classifier;

  public DefaultExternalDependencyId() {
  }

  public DefaultExternalDependencyId(String group, String name, String version) {
    this.group = group;
    this.name = name;
    this.version = version;
  }

  public DefaultExternalDependencyId(ExternalDependencyId dependencyId) {
    this(dependencyId.getGroup(), dependencyId.getName(), dependencyId.getVersion());
    packaging = dependencyId.getPackaging();
    classifier = dependencyId.getClassifier();
  }

  @Override
  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @NotNull
  @Override
  public String getPackaging() {
    return packaging;
  }

  public void setPackaging(@NotNull String packaging) {
    this.packaging = packaging;
  }

  @Nullable
  @Override
  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(@Nullable String classifier) {
    this.classifier = classifier;
  }

  @Override
  @NotNull
  public String getPresentableName() {
    final StringBuilder buf = new StringBuilder();
    if (group != null) {
      buf.append(group).append(':');
    }
    if(name != null) {
      buf.append(name);
    }
    if (!"jar".equals(packaging)) {
      buf.append(':').append(packaging);
    }
    if (classifier != null) {
      buf.append(':').append(classifier);
    }
    if (version != null) {
      buf.append(':').append(version);
    }
    return buf.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultExternalDependencyId)) return false;
    DefaultExternalDependencyId that = (DefaultExternalDependencyId)o;
    return Objects.equal(group, that.group) &&
           Objects.equal(name, that.name) &&
           Objects.equal(packaging, that.packaging) &&
           Objects.equal(classifier, that.classifier) &&
           Objects.equal(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(group, name, packaging, classifier, version);
  }

  @Override
  public String toString() {
    return getPresentableName();
  }
}
