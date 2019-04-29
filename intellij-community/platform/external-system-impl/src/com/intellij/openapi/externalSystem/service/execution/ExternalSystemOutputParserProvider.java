// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.build.output.BuildOutputParser;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public interface ExternalSystemOutputParserProvider {
  ExtensionPointName<ExternalSystemOutputParserProvider> EP_NAME =
    ExtensionPointName.create("com.intellij.externalSystemOutputParserProvider");

  ProjectSystemId getExternalSystemId();

  List<BuildOutputParser> getBuildOutputParsers(ExternalSystemTask task);
}
