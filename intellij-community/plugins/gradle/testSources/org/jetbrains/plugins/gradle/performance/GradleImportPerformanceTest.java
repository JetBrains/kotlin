// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.performance;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.PerformanceTrace;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.hamcrest.core.IsNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.intellij.testFramework.PlatformTestUtil.assertTiming;
import static org.junit.Assume.assumeThat;

public class GradleImportPerformanceTest extends GradleImportPerformanceTestCase {

  public static final String TEST_DATA_PATH = System.getenv("gradle.performance.test.data.path");

  @Override
  public void setUp() throws Exception {
    assumeThat(TEST_DATA_PATH, IsNull.notNullValue());
    super.setUp();
  }

  @Override
  protected void collectAllowedRoots(List<String> roots) {
    super.collectAllowedRoots(roots);
    roots.add(TEST_DATA_PATH);
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    File projectDir = new File(TEST_DATA_PATH);
    FileUtil.ensureExists(projectDir);
    myProjectRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir);
  }

  @Test
  public void testImportTiming() {
    GradleSystemSettings.getInstance().setGradleVmOptions("-Dorg.gradle.jvmargs=-Xmx2g");
    importProjectUsingSingeModulePerGradleProject();
    long startTime = System.currentTimeMillis();
    importProjectUsingSingeModulePerGradleProject();
    long consumedTime = System.currentTimeMillis() - startTime;

    ProjectDataManager manager = ProjectDataManager.getInstance();
    final Collection<ExternalProjectInfo> data = manager.getExternalProjectsData(myProject, GradleConstants.SYSTEM_ID);
    final DataNode<ProjectData> rootNode = data.iterator().next().getExternalProjectStructure();
    final DataNode<PerformanceTrace> traceDataNode = ExternalSystemApiUtil.find(rootNode, PerformanceTrace.TRACE_NODE_KEY);
    final Map<String, Long> trace = traceDataNode.getData().getPerformanceTrace();

    assertTracedTimePercentAtLeast(trace, consumedTime, 90);

    final long gradleModelsTrace = sumByPrefix(trace, "Get model ");
    final long resolverChainTrace = sumByPrefix(trace, "Resolver chain ");
    final long dataServiceTrace = sumByPrefix(trace, "Data import ");

    reportTiming("gradleModelsTrace", gradleModelsTrace);
    reportTiming("resolverChainTrace", resolverChainTrace);
    reportTiming("dataServiceTrace", dataServiceTrace);

    assertTiming("gradleModelsTrace", 30000, gradleModelsTrace);
    assertTiming("resolverChainTrace", 2000, resolverChainTrace);
    assertTiming("dataServiceTrace", 9000, dataServiceTrace);
  }

  @Test
  public void testImportPerSourceSetTiming() {
    GradleSystemSettings.getInstance().setGradleVmOptions("-Dorg.gradle.jvmargs=-Xmx2g");
    importProject();
    long startTime = System.currentTimeMillis();
    importProject();
    long consumedTime = System.currentTimeMillis() - startTime;

    ProjectDataManager manager = ProjectDataManager.getInstance();
    final Collection<ExternalProjectInfo> data = manager.getExternalProjectsData(myProject, GradleConstants.SYSTEM_ID);
    final DataNode<ProjectData> rootNode = data.iterator().next().getExternalProjectStructure();
    final DataNode<PerformanceTrace> traceDataNode = ExternalSystemApiUtil.find(rootNode, PerformanceTrace.TRACE_NODE_KEY);
    final Map<String, Long> trace = traceDataNode.getData().getPerformanceTrace();

    assertTracedTimePercentAtLeast(trace, consumedTime, 95);

    final long gradleModelsTrace = sumByPrefix(trace, "Get model ");
    final long resolverChainTrace = sumByPrefix(trace, "Resolver chain ");
    final long dataServiceTrace = sumByPrefix(trace, "Data import ");

    reportTiming("gradleModelsTrace", gradleModelsTrace);
    reportTiming("resolverChainTrace", resolverChainTrace);
    reportTiming("dataServiceTrace", dataServiceTrace);

    assertTiming("gradleModelsTrace", 70000, gradleModelsTrace);
    assertTiming("resolverChainTrace", 1200, resolverChainTrace);
    assertTiming("dataServiceTrace", 9000, dataServiceTrace);
  }

  private void reportTiming(@NotNull final String traceName, long actual) {
    int indexOfBrace = name.getMethodName().indexOf("[");
    final String methodName = indexOfBrace > -1 ? name.getMethodName().substring(0, indexOfBrace) : name.getMethodName();
    final String serviceMessage = String.format("##teamcity[buildStatisticValue key='trace.%s.%s.gradle-%s' value='%d']",
                                                methodName, traceName, gradleVersion, actual);
    System.out.println(serviceMessage);
  }

  protected long sumByPrefix(Map<String, Long> trace, String prefix) {
    return trace.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .mapToLong(Map.Entry::getValue)
                .sum();
  }
}
