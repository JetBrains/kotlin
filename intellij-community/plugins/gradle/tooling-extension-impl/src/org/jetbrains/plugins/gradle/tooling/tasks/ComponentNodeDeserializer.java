// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ComponentNodeDeserializer implements JsonDeserializer<ComponentNode> {

  @Override
  public ComponentNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();
    if (jsonObject.get("configurationName") != null) {
      return context.deserialize(json, ConfigurationNode.class);
    }
    else if (jsonObject.get("projectPath") != null) {
      return context.deserialize(json, ProjectComponentNode.class);
    }
    else if (jsonObject.get("module") != null) {
      return context.deserialize(json, ArtifactComponentNode.class);
    }
    else if (jsonObject.size() == 1 && jsonObject.get("id") != null) {
      return context.deserialize(json, ReferenceNode.class);
    }
    else {
      return context.deserialize(json, BaseComponentNode.class);
    }
  }
}