// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.statistics.BaseTestConfigurationFactory.FirstBaseTestConfigurationFactory;
import com.intellij.execution.impl.statistics.BaseTestConfigurationFactory.SecondBaseTestConfigurationFactory;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUSUsageContext;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.lang.String.valueOf;

public class RunConfigurationUsageCollectorTest extends LightPlatformTestCase {

  private static void doTest(@NotNull List<RunnerAndConfigurationSettings> configurations,
                             @NotNull Set<TestUsageDescriptor> expected, boolean withTemporary) {
    final Project project = getProject();
    final RunManager manager = RunManager.getInstance(project);
    try {
      for (RunnerAndConfigurationSettings configuration : configurations) {
        manager.addConfiguration(configuration);
      }

      final RunConfigurationTypeUsagesCollector collector = new RunConfigurationTypeUsagesCollector();
      final TemporaryRunConfigurationTypeUsagesCollector temporaryCollector = new TemporaryRunConfigurationTypeUsagesCollector();

      Set<UsageDescriptor> temporaryUsages = temporaryCollector.getUsages(project);
      assertTrue(temporaryUsages.isEmpty());

      Set<UsageDescriptor> usages = collector.getUsages(project);
      assertEquals(expected.size(), usages.size());
      assertEquals(expected, toTestUsageDescriptor(usages));

      if (withTemporary) {
        for (RunnerAndConfigurationSettings configuration : configurations) {
          configuration.setTemporary(true);
        }

        temporaryUsages = temporaryCollector.getUsages(project);
        assertEquals(expected.size(), temporaryUsages.size());
        assertEquals(expected, toTestUsageDescriptor(temporaryUsages));

        usages = collector.getUsages(project);
        assertTrue(usages.isEmpty());
      }
    }
    finally {
      for (RunnerAndConfigurationSettings configuration : configurations) {
        manager.removeConfiguration(configuration);
      }
    }
  }

  public void testSingleRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, false, false))
    );
    doTest(configurations, expected, true);
  }

  public void testMultipleRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, false));
    configurations.add(createFirst(instance, 2, false, false, false, false));
    configurations.add(createFirst(instance, 3, false, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 3,
      create(false, false, false, false))
    );
    doTest(configurations, expected, true);
  }

  public void testSharedRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, true, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(true, false, false, false))
    );
    doTest(configurations, expected, false);
  }

  public void testEditBeforeRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, true, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, true, false, false))
    );
    doTest(configurations, expected, true);
  }

  public void testActivateRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, true, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, true, false))
    );
    doTest(configurations, expected, true);
  }

  public void testParallelRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, true));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, false, true))
    );
    doTest(configurations, expected, true);
  }

  public void testDifferentRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, false));
    configurations.add(createSecond(instance, 2, false, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 1,
      create(false, false, false, false))
    );
    doTest(configurations, expected, true);
  }

  public void testRunConfigurationsWithDifferentShared() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, true, false, false, false));
    configurations.add(createFirst(instance, 2, false, false, false, false));
    configurations.add(createFirst(instance, 3, true, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 2,
      create(true, false, false, false))
    );
    doTest(configurations, expected, false);
  }

  public void testRunConfigurationsWithDifferentEditBeforeRun() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, true, false, false));
    configurations.add(createFirst(instance, 2, false, true, false, false));
    configurations.add(createFirst(instance, 3, false, false, false, false));
    configurations.add(createFirst(instance, 4, false, true, false, false));
    configurations.add(createFirst(instance, 5, false, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 3,
      create(false, true, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 2,
      create(false, false, false, false))
    );
    doTest(configurations, expected, true);
  }

  public void testRunConfigurationsWithDifferentActivate() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, false));
    configurations.add(createFirst(instance, 2, false, false, true, false));
    configurations.add(createFirst(instance, 3, false, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, true, false))
    );
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 2,
      create(false, false, false, false))
    );
    doTest(configurations, expected, false);
  }

  public void testRunConfigurationsWithDifferentParallel() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, false));
    configurations.add(createFirst(instance, 2, false, false, false, true));
    configurations.add(createFirst(instance, 3, false, false, false, true));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 2,
      create(false, false, false, true))
    );
    doTest(configurations, expected, false);
  }

  public void testMultipleDifferentRunConfiguration() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, false));
    configurations.add(createFirst(instance, 2, false, false, false, false));
    configurations.add(createSecond(instance, 3, false, false, false, false));
    configurations.add(createSecond(instance, 4, false, false, false, false));
    configurations.add(createSecond(instance, 5, false, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 2,
      create(false, false, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 3,
      create(false, false, false, false))
    );
    doTest(configurations, expected, true);
  }

  public void testDifferentRunConfigurationWithDifferentContext() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, true, false, false, false));
    configurations.add(createFirst(instance, 2, false, false, false, false));
    configurations.add(createFirst(instance, 3, true, true, false, false));
    configurations.add(createFirst(instance, 4, true, true, false, false));
    configurations.add(createFirst(instance, 5, true, true, true, true));
    configurations.add(createSecond(instance, 6, true, false, false, false));
    configurations.add(createSecond(instance, 7, false, false, true, false));
    configurations.add(createSecond(instance, 8, false, false, true, false));
    configurations.add(createSecond(instance, 9, false, false, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(true, false, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(false, false, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 2,
      create(true, true, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 1,
      create(true, true, true, true))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 1,
      create(true, false, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 2,
      create(false, false, true, false))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 1,
      create(false, false, false, false))
    );
    doTest(configurations, expected, false);
  }

  public void testDifferentRunConfigurationWithDifferentContextNotShared() {
    final List<RunnerAndConfigurationSettings> configurations = new ArrayList<>();
    final RunManager instance = RunManager.getInstance(getProject());
    configurations.add(createFirst(instance, 1, false, false, false, true));
    configurations.add(createFirst(instance, 2, false, false, false, true));
    configurations.add(createSecond(instance, 3, false, false, false, true));
    configurations.add(createSecond(instance, 4, false, true, false, false));
    configurations.add(createSecond(instance, 5, false, true, true, true));
    configurations.add(createSecond(instance, 6, false, true, false, false));

    final Set<TestUsageDescriptor> expected = new HashSet<>();
    expected.add(new TestUsageDescriptor(
      "FirstTestRunConfigurationType", 2,
      create(false, false, false, true))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 1,
      create(false, false, false, true))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 2,
      create(false, true, false, false))
    );
    expected.add(new TestUsageDescriptor(
      "SecondTestRunConfigurationType", 1,
      create(false, true, true, true))
    );
    doTest(configurations, expected, true);
  }

  @NotNull
  private static FeatureUsageData create(boolean isShared, boolean isEditBeforeRun, boolean isActivate, boolean isParallel) {
    return new FeatureUsageData().
      addData("plugin_type", "PLATFORM").
      addData("edit_before_run", isEditBeforeRun).
      addData("activate_before_run", isActivate).
      addData("shared", isShared).
      addData("parallel", isParallel);
  }

  @NotNull
  private static Set<TestUsageDescriptor> toTestUsageDescriptor(@NotNull Set<UsageDescriptor> descriptors) {
    final Set<TestUsageDescriptor> result = new HashSet<>();
    for (UsageDescriptor descriptor : descriptors) {
      result.add(new TestUsageDescriptor(descriptor));
    }
    return result;
  }

  private static class TestUsageDescriptor {
    private final String myKey;
    private final int myValue;
    private final FeatureUsageData myData;

    private TestUsageDescriptor(@NotNull String key, int value, @NotNull FeatureUsageData data) {
      myKey = key;
      myData = data;
      myValue = value;
    }

    private TestUsageDescriptor(@NotNull UsageDescriptor descriptor) {
      myKey = descriptor.getKey();
      myData = descriptor.getData();
      myValue = descriptor.getValue();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TestUsageDescriptor that = (TestUsageDescriptor)o;

      return myValue == that.myValue &&
             Objects.equals(myKey, that.myKey) &&
             Objects.equals(myData, that.myData);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myKey, myValue, myData);
    }

    @Override
    public String toString() {
      return "'" + myKey + "' " + myData.build() + " : " + myValue;
    }
  }

  private static RunnerAndConfigurationSettings createFirst(@NotNull RunManager manager, int index,
                                                            boolean isShared, boolean isEditBeforeRun,
                                                            boolean isActivate, boolean isParallel) {
    return configure(manager.createConfiguration("Test_" + index, FirstBaseTestConfigurationFactory.INSTANCE),
                     isShared, isEditBeforeRun, isActivate, isParallel
    );
  }

  private static RunnerAndConfigurationSettings createSecond(@NotNull RunManager manager, int index,
                                                             boolean isShared, boolean isEditBeforeRun,
                                                             boolean isActivate, boolean isParallel) {
    return configure(manager.createConfiguration("Test_" + index, SecondBaseTestConfigurationFactory.INSTANCE),
                     isShared, isEditBeforeRun, isActivate, isParallel
    );
  }

  @NotNull
  private static RunnerAndConfigurationSettings configure(@NotNull RunnerAndConfigurationSettings configuration,
                                                          boolean isShared, boolean isEditBeforeRun,
                                                          boolean isActivate, boolean isParallel) {
    configuration.setShared(isShared);
    configuration.setEditBeforeRun(isEditBeforeRun);
    configuration.setActivateToolWindowBeforeRun(isActivate);
    configuration.getConfiguration().setAllowRunningInParallel(isParallel);
    return configuration;
  }
}
