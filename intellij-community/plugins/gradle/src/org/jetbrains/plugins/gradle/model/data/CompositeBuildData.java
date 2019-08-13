// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class CompositeBuildData implements Serializable {

  @NotNull
  public static final Key<CompositeBuildData> KEY = Key.create(CompositeBuildData.class, ProjectKeys.PROJECT.getProcessingWeight() + 1);

  private final String rootProjectPath;
  @NotNull private final List<BuildParticipant> myCompositeParticipants = new ArrayList<>();

  @PropertyMapping({"rootProjectPath"})
  public CompositeBuildData(String rootProjectPath) {
    this.rootProjectPath = rootProjectPath;
  }

  public String getRootProjectPath() {
    return rootProjectPath;
  }

  @NotNull
  public List<BuildParticipant> getCompositeParticipants() {
    return myCompositeParticipants;
  }
}
