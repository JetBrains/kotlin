/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class GradleBuildParticipant implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String myProjectPath;
  private final Map<File, ModuleData> moduleArtifactMap = ContainerUtil.newTroveMap(new TObjectHashingStrategy<File>() {
    @Override
    public int computeHashCode(File file) {
      return ExternalSystemUtil.fileHashCode(file);
    }

    @Override
    public boolean equals(File o1, File o2) {
      return ExternalSystemUtil.filesEqual(o1, o2);
    }
  });
  private final Map<String, ModuleData> moduleNameMap = ContainerUtil.newHashMap();

  public GradleBuildParticipant(String projectPath) {
    myProjectPath = projectPath;
  }

  public String getProjectPath() {
    return myProjectPath;
  }

  public void addModule(ModuleData moduleData) {
    List<File> artifacts = moduleData.getArtifacts();
    if (!artifacts.isEmpty()) {
      for (File artifact : artifacts) {
        moduleArtifactMap.put(artifact, moduleData);
      }
    }
    moduleNameMap.put(moduleData.getExternalName(), moduleData);
    if (moduleData instanceof GradleSourceSetData) {
      String mainModuleName = StringUtil.trimEnd(moduleData.getExternalName(), ":main");
      moduleNameMap.put(mainModuleName, moduleData);
    }
  }

  @Nullable
  public ModuleData findModuleDataByArtifacts(Collection<File> artifacts) {
    ModuleData moduleData = null;
    for (File artifact : artifacts) {
      moduleData = moduleArtifactMap.get(artifact);
      if (moduleData != null) break;
    }
    return moduleData;
  }

  @Nullable
  public ModuleData findModuleDataByName(String moduleName) {
    return moduleNameMap.get(moduleName);
  }
}
