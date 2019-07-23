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
import java.util.HashMap;
import java.util.Map;

/**
* @author Vladislav.Soroka
*/
public abstract class ModelsHolder<K extends Model,V>  implements Serializable {

  @NotNull private final K myRootModel;
  @NotNull private final Map<String, Object> myModelsById = new HashMap<String, Object>();
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
      T deserializedData = deserialize(project, modelClazz);
      if (modelClazz.isInstance(deserializedData)) {
        myModelsById.put(key, deserializedData);
        return deserializedData;
      }
    }
    myModelsById.remove(key);
    return null;
  }

  @Nullable
  private <T> T deserialize(Object data, Class<T> modelClazz) {
    if (mySerializer == null || !(data instanceof byte[])) {
      return null;
    }
    try {
      return mySerializer.read((byte[])data, modelClazz);
    }
    catch (IllegalArgumentException ignore) {
      // related serialization service was not found
    }
    catch (IOException e) {
      //noinspection UseOfSystemOutOrSystemErr
      System.err.println(e.getMessage());
    }
    return null;
  }

  public boolean hasModelKeyStaringWith(@NotNull String keyPrefix) {
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
  protected abstract String extractMapKey(Class modelClazz, @Nullable V module);

  @Override
  public String toString() {
    return "Models{" +
           "rootModel=" + myRootModel +
           ", myModelsById=" + myModelsById +
           '}';
  }
}
