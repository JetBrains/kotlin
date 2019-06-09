// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectDataManagerImplTest extends PlatformTestCase {
  public void testDataServiceIsCalledIfNoNodes() {
    final List<String> callTrace = new ArrayList<>();

    // use test constructor to avoid data services caching in the application component instance
    new ProjectDataManagerImpl(new TestDataService(callTrace)).importData(
      Collections.singletonList(
        new DataNode<>(ProjectKeys.PROJECT, new ProjectData(ProjectSystemId.IDE,
                                                            "externalName",
                                                            "externalPath",
                                                            "linkedPath"), null)), myProject, true);

    assertContainsElements(callTrace, "computeOrphanData");
  }

  public void testDataServiceKeyOrdering() {
    final List<String> callTrace = new ArrayList<>();

    // use test constructor to avoid data services caching in the application component instance
    new ProjectDataManagerImpl(new RunAfterTestDataService(callTrace), new TestDataService(callTrace)).importData(
      Collections.singletonList(
        new DataNode<>(ProjectKeys.PROJECT, new ProjectData(ProjectSystemId.IDE,
                                                            "externalName",
                                                            "externalPath",
                                                            "linkedPath"), null)), myProject, true);

    assertOrderedEquals(callTrace,
                        "importData",
                        "computeOrphanData",
                        "removeData",
                        "importDataAfter",
                        "computeOrphanDataAfter",
                        "removeDataAfter");
  }

  @Order(1)
  static class RunAfterTestDataService extends TestDataService {
    static class MyObject {
    }

    static final Key<MyObject> RUN_AFTER_KEY = Key.create(MyObject.class, TEST_KEY.getProcessingWeight() + 1);

    RunAfterTestDataService(List<String> trace) {
      super(trace);
    }

    @Override
    public void removeData(@NotNull Computable toRemove,
                           @NotNull Collection toIgnore,
                           @NotNull ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
      myTrace.add("removeDataAfter");
    }

    @NotNull
    @Override
    public Computable<Collection> computeOrphanData(@NotNull Collection toImport,
                                                    @NotNull ProjectData projectData,
                                                    @NotNull Project project,
                                                    @NotNull IdeModifiableModelsProvider modelsProvider) {
      myTrace.add("computeOrphanDataAfter");
      return () -> Collections.emptyList();
    }

    @Override
    public void importData(@NotNull Collection toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
      myTrace.add("importDataAfter");
    }

    @NotNull
    @Override
    public Key getTargetDataKey() {
      return RUN_AFTER_KEY;
    }
  }

  @Order(2)
  static class TestDataService implements ProjectDataService {
    public static final Key<Object> TEST_KEY = Key.create(Object.class, 0);

    protected final List<String> myTrace;

    TestDataService(List<String> trace) {
      myTrace = trace;
    }

    @NotNull
    @Override
    public Key getTargetDataKey() {
      return TEST_KEY;
    }

    @Override
    public void removeData(@NotNull Computable toRemove,
                           @NotNull Collection toIgnore,
                           @NotNull ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
      myTrace.add("removeData");
    }

    @NotNull
    @Override
    public Computable<Collection> computeOrphanData(@NotNull Collection toImport,
                                                    @NotNull ProjectData projectData,
                                                    @NotNull Project project,
                                                    @NotNull IdeModifiableModelsProvider modelsProvider) {
      myTrace.add("computeOrphanData");
      return () -> Collections.emptyList();
    }

    @Override
    public void importData(@NotNull Collection toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
      myTrace.add("importData");
    }
  }

}