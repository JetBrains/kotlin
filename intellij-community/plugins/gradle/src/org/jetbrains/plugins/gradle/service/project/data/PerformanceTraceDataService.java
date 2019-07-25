// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.externalSystem.service.project.PerformanceTrace;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

@Order(ExternalSystemConstants.UNORDERED + 1)
public class PerformanceTraceDataService extends AbstractProjectDataService<PerformanceTrace, Object> {
  private static final Logger LOG = Logger.getInstance(PerformanceTraceDataService.class);
  @NotNull
  @Override
  public Key<PerformanceTrace> getTargetDataKey() {
    return PerformanceTrace.TRACE_NODE_KEY;
  }

  @Override
  public void onSuccessImport(@NotNull Collection<DataNode<PerformanceTrace>> imported,
                              @Nullable ProjectData projectData,
                              @NotNull Project project,
                              @NotNull IdeModelsProvider modelsProvider) {
    if (imported.size() > 0) {
      final PerformanceTrace trace = imported.iterator().next().getData();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Gradle successful import performance trace");
        for (Map.Entry<String, Long> entry : trace.getPerformanceTrace().entrySet()) {
          LOG.debug(entry.getKey() + " : " + entry.getValue() + " ms.");
        }
      }
    }
  }
}
