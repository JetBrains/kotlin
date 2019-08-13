// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.model;

import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.serialization.PropertyMapping;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.JsonReaderEx;
import org.jetbrains.io.JsonUtil;

import java.util.Map;

@ApiStatus.Experimental
public final class ConfigurationDataImpl extends AbstractExternalEntityData implements ConfigurationData {
  @Language("JSON") @NotNull private final String data;
  @Nullable transient private volatile Object myJsonObject;

  @PropertyMapping({"owner", "data"})
  public ConfigurationDataImpl(@NotNull ProjectSystemId owner, @Language("JSON") @NotNull String data) {
    super(owner);

    this.data = data;
  }

  @Language("JSON")
  @NotNull
  public String getJsonString() {
    return data;
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
      JsonReaderEx reader = new JsonReaderEx(data);
      reader.setLenient(true);
      myJsonObject = JsonUtil.nextAny(reader);
    }
    return myJsonObject;
  }
}
