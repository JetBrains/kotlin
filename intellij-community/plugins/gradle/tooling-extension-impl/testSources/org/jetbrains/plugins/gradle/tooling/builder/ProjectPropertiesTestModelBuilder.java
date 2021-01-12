// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ProjectPropertiesTestModelBuilder implements ModelBuilderService {
  @Override
  public boolean canBuild(String modelName) {
    return ProjectProperties.class.getName().equals(modelName);
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    Map<String, String> propertiesMap = new LinkedHashMap<>();
    for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
      propertiesMap.put(entry.getKey(), Objects.toString(entry.getValue(), null));
    }
    return new ProjectPropertiesImpl(propertiesMap);
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder
      .create(project, e, "Test model import errors")
      .withDescription("Unable to import Test model");
  }

  public interface ProjectProperties {
    Map<String, String> getPropertiesMap();
  }

  public static class ProjectPropertiesImpl implements ProjectProperties, Serializable {
    private final Map<String, String> myPropertiesMap;

    public ProjectPropertiesImpl(Map<String, String> propertiesMap) {
      myPropertiesMap = propertiesMap;
    }

    @Override
    public Map<String, String> getPropertiesMap() {
      return myPropertiesMap;
    }
  }
}
