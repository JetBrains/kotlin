/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.util.xmlb.annotations.Tag;

/**
 * @author Vladislav.Soroka
 */
public class ModuleVersion {
  @Tag("groupId")
  public String groupId;

  @Tag("artifactId")
  public String artifactId;

  @Tag("version")
  public String version;

  public ModuleVersion() {
  }

  public ModuleVersion(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModuleVersion bean = (ModuleVersion)o;

    if (artifactId != null ? !artifactId.equals(bean.artifactId) : bean.artifactId != null) return false;
    if (groupId != null ? !groupId.equals(bean.groupId) : bean.groupId != null) return false;
    if (version != null ? !version.equals(bean.version) : bean.version != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = groupId != null ? groupId.hashCode() : 0;
    result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }
}
