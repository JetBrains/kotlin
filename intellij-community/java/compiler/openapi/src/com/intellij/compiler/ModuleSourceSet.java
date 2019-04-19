/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.compiler;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author nik
*/
public class ModuleSourceSet {
  public enum Type { PRODUCTION, TEST }

  private final Module myModule;
  private final Type myType;

  public ModuleSourceSet(@NotNull Module module, @NotNull Type type) {
    myModule = module;
    myType = type;
  }

  @NotNull
  public Module getModule() {
    return myModule;
  }

  @NotNull
  public Type getType() {
    return myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModuleSourceSet set = (ModuleSourceSet)o;
    return myModule.equals(set.myModule) && myType == set.myType;
  }

  @Override
  public int hashCode() {
    return 31 * myModule.hashCode() + myType.hashCode();
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

  @NotNull
  public String getDisplayName() {
    return (myType == Type.PRODUCTION ? "" : "Tests of ") + "'" + myModule.getName() + "' module";
  }

  @NotNull
  public static Set<Module> getModules(@NotNull Collection<? extends ModuleSourceSet> sourceSets) {
    return sourceSets.stream().map(ModuleSourceSet::getModule).collect(Collectors.toSet());
  }
}
