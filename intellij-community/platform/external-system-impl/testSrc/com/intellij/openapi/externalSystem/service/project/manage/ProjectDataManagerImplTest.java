// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ProjectDataManagerImplTest extends HeavyPlatformTestCase {
  public void testDataServiceIsCalledIfNoNodes() {
    final List<String> callTrace = new ArrayList<>();

    maskProjectDataServices(new TestDataService(callTrace));
    new ProjectDataManagerImpl().importData(
      Collections.singletonList(
        new DataNode<>(ProjectKeys.PROJECT, new ProjectData(ProjectSystemId.IDE,
                                                            "externalName",
                                                            "externalPath",
                                                            "linkedPath"), null)), myProject, true);

    assertContainsElements(callTrace, "computeOrphanData");
  }

  public void testDataServiceKeyOrdering() {
    final List<String> callTrace = new ArrayList<>();

    maskProjectDataServices(new RunAfterTestDataService(callTrace), new TestDataService(callTrace));
    new ProjectDataManagerImpl().importData(
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

  public void testConcurrentDataServiceAccess() {
    int n = 100;
    final List<String> callTrace = new ArrayList<>();
    TestDataService[] dataServiceArray = new TestDataService[n];
    for (int i = 0; i < n; i++) {
      dataServiceArray[i] = new TestDataService(callTrace);
    }
    maskProjectDataServices(dataServiceArray);

    final CountDownLatch latch = new CountDownLatch(1);
    final Ref<Throwable> caughtCME = new Ref<>(null);

    Thread iterating = new Thread(() -> {
      await(latch);
      try {
        List<ProjectDataService<?, ?>> services = ProjectDataManagerImpl.getInstance().findService(TestDataService.TEST_KEY);
        for (ProjectDataService<?, ?> service : services) {
          Thread.yield();
        }
      } catch (ConcurrentModificationException e) {
        caughtCME.set(e);
      }
    }, "Iterating over services");

    Thread lookup = new Thread(() -> {
      await(latch);
      try {
        for (int i = 0; i < n; i++) {
          Thread.yield();
          ProjectDataManagerImpl.getInstance().findService(TestDataService.TEST_KEY);
        }
      } catch (ConcurrentModificationException e) {
        caughtCME.set(e);
      }
    }, "Lookup service with sorting");

    iterating.start();
    lookup.start();
    latch.countDown();
    ConcurrencyUtil.joinAll(iterating, lookup);

    assertNull(caughtCME.get());
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    }
    catch (InterruptedException e) {
      // do nothing
    }
  }

  private void maskProjectDataServices(TestDataService... services) {
    ((ExtensionPointImpl<ProjectDataService<?,?>>)ProjectDataService.EP_NAME.getPoint()).maskAll(Arrays.asList(services),
                                                                                                 getTestRootDisposable(),
                                                                                                 false);
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
  static class TestDataService implements ProjectDataService<Object, Object> {
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