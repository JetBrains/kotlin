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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vladislav.Soroka
 */
@Tag("filter")
public class ResourceRootFilter {
  @Tag("filterType")
  @NotNull
  public String filterType;
  @Tag("properties")
  @NotNull
  public String properties;

  private transient Map<Object, Object> propertiesMap;

  public int computeConfigurationHash() {
    int result = filterType.hashCode();
    result = 31 * result + getProperties().hashCode();
    return result;
  }

  @NotNull
  public Map<Object, Object> getProperties() {
    if (propertiesMap == null) {
      try {
        Gson gson = new GsonBuilder().create();
        propertiesMap = gson.fromJson(
          properties,
          new TypeToken<Map<Object, Object>>() {
          }.getType());

        if("RenamingCopyFilter".equals(filterType)) {
          final Object pattern = propertiesMap.get("pattern");
          final Matcher matcher = Pattern.compile(pattern instanceof String ? (String)pattern : "").matcher("");
          propertiesMap.put("matcher", matcher);
        }
      }
      catch (JsonParseException e) {
        throw new RuntimeException("Unsupported filter: " + properties , e);
      }
      if(propertiesMap == null) {
        propertiesMap = new HashMap<>();
      }
    }
    return propertiesMap;
  }
}
