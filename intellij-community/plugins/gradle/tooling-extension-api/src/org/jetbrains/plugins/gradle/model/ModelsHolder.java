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
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
* @author Vladislav.Soroka
*/
public abstract class ModelsHolder<K extends Model,V>  implements Serializable {

  @NotNull private final K myRootModel;
  @NotNull private final Map<String, Object> myModelsById = new HashMap<String, Object>();

  public ModelsHolder(@NotNull K rootModel) {
    myRootModel = rootModel;
  }

  @NotNull
  public K getRootModel() {
    return myRootModel;
  }

  @Nullable
  public <T> T getExtraProject(Class<T> modelClazz) {
    return getExtraProject(null, modelClazz);
  }

  @Nullable
  public <T> T getExtraProject(@Nullable V model, Class<T> modelClazz) {
    Object project = myModelsById.get(extractMapKey(modelClazz, model));
    if (modelClazz.isInstance(project)) {
      //noinspection unchecked
      return (T)project;
    }
    return null;
  }

  /**
   * Return collection path of modules provides the model
   *
   * @param modelClazz extra project model
   * @return modules path collection
   */
  @NotNull
  public Collection<String> findModulesWithModel(@NotNull Class modelClazz) {
    List<String> modules = new ArrayList<String>();
    for (Map.Entry<String, Object> set : myModelsById.entrySet()) {
      if (modelClazz.isInstance(set.getValue())) {
        modules.add(extractModulePath(modelClazz, set.getKey()));
      }
    }
    return modules;
  }

  public boolean hasModulesWithModel(@NotNull Class modelClazz) {
    for (Map.Entry<String, Object> set : myModelsById.entrySet()) {
      if (modelClazz.isInstance(set.getValue())) return true;
    }
    return false;
  }

  public void addExtraProject(@NotNull Object project, @NotNull Class modelClazz) {
    myModelsById.put(extractMapKey(modelClazz, null), project);
  }

  public void addExtraProject(@NotNull Object project, @NotNull Class modelClazz, @Nullable V subPropject) {
    myModelsById.put(extractMapKey(modelClazz, subPropject), project);
  }

  @NotNull
  protected abstract String extractMapKey(Class modelClazz, @Nullable V module);

  @NotNull
  private static String extractModulePath(Class modelClazz, String key) {
    return key.replaceFirst(modelClazz.getName() + '@', "");
  }

  @Override
  public String toString() {
    return "Models{" +
           "rootModel=" + myRootModel +
           ", myModelsById=" + myModelsById +
           '}';
  }
}
