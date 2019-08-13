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
package org.jetbrains.plugins.gradle.tooling.util;

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class ModuleComponentIdentifierImpl implements ModuleComponentIdentifier {
  @NotNull
  private final String group;
  @NotNull
  private final String module;
  @NotNull
  private final String version;
  @NotNull
  private final ModuleIdentifier moduleIdentifier;

  public ModuleComponentIdentifierImpl(@NotNull String group, @NotNull String module, @NotNull String version) {
    this.group = group;
    this.module = module;
    this.version = version;
    this.moduleIdentifier = new ModuleIdentifierImpl(group, module);
  }

  @Override
  public String getDisplayName() {
    return group + ":" + module + ":" + version;
  }

  @Override
  @NotNull
  public String getGroup() {
    return group;
  }

  @Override
  @NotNull
  public String getModule() {
    return module;
  }

  @Override
  @NotNull
  public String getVersion() {
    return version;
  }

  @NotNull
  public ModuleIdentifier getModuleIdentifier() {
    return moduleIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleComponentIdentifier)) return false;

    ModuleComponentIdentifier that = (ModuleComponentIdentifier)o;

    if (!group.equals(that.getGroup())) return false;
    if (!module.equals(that.getModule())) return false;
    if (!version.equals(that.getVersion())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = group.hashCode();
    result = 31 * result + module.hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }
}

class ModuleIdentifierImpl implements ModuleIdentifier {
  @NotNull private final String myGroup;
  @NotNull private final String myModule;

  ModuleIdentifierImpl(@NotNull String group, @NotNull String module) {
    myGroup = group;
    myModule = module;
  }

  @Override
  public String getGroup() {
    return myGroup;
  }

  @Override
  public String getName() {
    return myModule;
  }
}