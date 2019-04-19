/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.model;

import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.JsonReaderEx;
import org.jetbrains.io.JsonUtil;

import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class ConfigurationDataImpl extends AbstractExternalEntityData implements ConfigurationData {

  private static final long serialVersionUID = 1L;

  @Language("JSON") @NotNull private final String myData;
  @Nullable transient private volatile Object myJsonObject;

  public ConfigurationDataImpl(@NotNull ProjectSystemId owner, @Language("JSON") @NotNull String data) {
    super(owner);
    myData = data;
  }

  @Language("JSON")
  @NotNull
  public String getJsonString() {
    return myData;
  }

  @Override
  public Object find(@NotNull String query) {
    if (StringUtil.isEmpty(query)) return null;

    Object jsonObject = getJsonObject();
    for (String part : StringUtil.split(query, ".")) {
      if (jsonObject instanceof Map) {
        jsonObject = ((Map)jsonObject).get(part);
      }
      else {
        return null;
      }
    }
    return jsonObject;
  }

  public Object getJsonObject() {
    if (myJsonObject == null) {
      JsonReaderEx reader = new JsonReaderEx(myData);
      reader.setLenient(true);
      myJsonObject = JsonUtil.nextAny(reader);
    }
    return myJsonObject;
  }
}
