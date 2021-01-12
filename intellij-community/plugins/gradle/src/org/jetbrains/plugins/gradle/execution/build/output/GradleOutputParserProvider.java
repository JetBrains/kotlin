// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build.output;

import com.intellij.build.output.BuildOutputParser;
import com.intellij.build.output.JavacOutputParser;
import com.intellij.build.output.KotlincOutputParser;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemOutputParserProvider;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleOutputParserProvider implements ExternalSystemOutputParserProvider {
  @Override
  public ProjectSystemId getExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  @Override
  public List<BuildOutputParser> getBuildOutputParsers(@NotNull ExternalSystemTaskId taskId) {
    List<BuildOutputParser> parsers = new SmartList<>();
    if (taskId.getType().equals(ExternalSystemTaskType.RESOLVE_PROJECT)) {
      parsers.add(new GradleSyncOutputParser());
    }
    parsers.add(new GradleBuildScriptErrorParser());
    parsers.add(new JavacOutputParser("java", "scala"));
    parsers.add(new KotlincOutputParser());
    return parsers;
  }
}
