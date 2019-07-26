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
import org.jetbrains.plugins.gradle.tooling.serialization.ToolingSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public abstract class ModelsHolder<K extends Model, V> implements Serializable {

  @NotNull private final K myRootModel;
  @NotNull private final Map<String, Object> myModelsById = new LinkedHashMap<String, Object>();
  @Nullable private ToolingSerializer mySerializer;

  public ModelsHolder(@NotNull K rootModel) {
    myRootModel = rootModel;
  }

  public void initToolingSerializer() {
    mySerializer = new ToolingSerializer();
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
    String key = extractMapKey(modelClazz, model);
    Object project = myModelsById.get(key);
    if (project == null) return null;
    if (modelClazz.isInstance(project)) {
      //noinspection unchecked
      return (T)project;
    }
    else {
      deserializeAllDataOfTheType(modelClazz);
      //noinspection unchecked
      return (T)myModelsById.get(key);
    }
  }

  /**
   * Deserialize all data of the model type to ensure the same order which has been used when adding the data.
   */
  private <T> void deserializeAllDataOfTheType(@NotNull Class<T> modelClazz) {
    String keyPrefix = getModelKeyPrefix(modelClazz);
    for (Iterator<Map.Entry<String, Object>> iterator = myModelsById.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry<String, Object> entry = iterator.next();
      String key = entry.getKey();
      if (key.startsWith(keyPrefix)) {
        if (mySerializer == null || !(entry.getValue() instanceof byte[])) {
          iterator.remove();
          continue;
        }
        try {
          T deserializedData = mySerializer.read((byte[])entry.getValue(), modelClazz);
          if (modelClazz.isInstance(deserializedData)) {
            myModelsById.put(key, deserializedData);
          }
          else {
            iterator.remove();
          }
        }
        catch (IOException e) {
          //noinspection UseOfSystemOutOrSystemErr
          System.err.println(e.getMessage());
          iterator.remove();
        }
      }
    }
  }

  public boolean hasModulesWithModel(@NotNull Class modelClazz) {
    String keyPrefix = getModelKeyPrefix(modelClazz);
    for (String key : myModelsById.keySet()) {
      if (key.startsWith(keyPrefix)) return true;
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
  protected String getModelKeyPrefix(@NotNull Class modelClazz) {
    return modelClazz.getSimpleName() + modelClazz.getName().hashCode();
  }

  @NotNull
  protected abstract String extractMapKey(@NotNull Class modelClazz, @Nullable V module);

  @Override
  public String toString() {
    return "Models{" +
           "rootModel=" + myRootModel +
           ", myModelsById=" + myModelsById +
           '}';
  }
}
