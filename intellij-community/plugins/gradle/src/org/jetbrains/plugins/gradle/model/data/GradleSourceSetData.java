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
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @author Vladislav.Soroka
 */
public class GradleSourceSetData extends ModuleData {

  @NotNull
  public static final Key<GradleSourceSetData> KEY = Key.create(GradleSourceSetData.class, ProjectKeys.MODULE.getProcessingWeight() + 1);

  public GradleSourceSetData(@NotNull String id,
                             @NotNull String externalName,
                             @NotNull String internalName,
                             @NotNull String moduleFileDirectoryPath,
                             @NotNull String externalConfigPath) {
    super(id, GradleConstants.SYSTEM_ID, GradleProjectResolverUtil.getDefaultModuleTypeId(),
          externalName, internalName,
          moduleFileDirectoryPath, externalConfigPath);
  }

  @Nullable
  @Override
  public String getIdeGrouping() {
    return super.getIdeGrouping() + ":" + getSourceSetName();
  }

  @Override
  @Nullable
  public String getIdeParentGrouping() {
    return super.getIdeGrouping();
  }

  private String getSourceSetName() {
    return StringUtil.substringAfterLast(getExternalName(), ":");
  }
}
