// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.compiler;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.util.SmartList;
import com.intellij.util.containers.CollectionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;
import org.jetbrains.jps.incremental.artifacts.ArtifactBuildTargetType;

import java.util.List;
import java.util.Map;

public final class ArtifactCompilerUtil {
  private ArtifactCompilerUtil() {
  }

  public static boolean containsArtifacts(List<TargetTypeBuildScope> scopes) {
    for (TargetTypeBuildScope scope : scopes) {
      if (ArtifactBuildTargetType.INSTANCE.getTypeId().equals(scope.getTypeId())) {
        return true;
      }
    }
    return false;
  }

  public static @NotNull Map<String, List<Artifact>> createOutputToArtifactMap(@NotNull Project project) {
    Map<String, List<Artifact>> result = CollectionFactory.createFilePathMap();
    ReadAction.run(() -> {
      for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
        String outputPath = artifact.getOutputFilePath();
        if (!StringUtil.isEmpty(outputPath)) {
          result.computeIfAbsent(outputPath, __ -> new SmartList<>()).add(artifact);
        }
      }
    });
    return result;
  }
}
