// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManagerConfig;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

public class ExecutionManagerRerunSingletonTest extends LightPlatformTestCase {

  private boolean myRestartRequiresConfirmation;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    RunManagerConfig config = RunManagerImpl.getInstanceImpl(getProject()).getConfig();
    myRestartRequiresConfirmation = config.isRestartRequiresConfirmation();
    config.setRestartRequiresConfirmation(false);
  }

  @Override
  public void tearDown() throws Exception {
    try {
      ExecutionTestUtil.terminateAllRunningDescriptors(ExecutionManager.getInstance(getProject()));
      RunManagerConfig config = RunManagerImpl.getInstanceImpl(getProject()).getConfig();
      config.setRestartRequiresConfirmation(myRestartRequiresConfirmation);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testRerunSingleton() {
    Project project = getProject();
    ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);

    FakeRunConfiguration rc = new FakeRunConfiguration(project, true);
    RunnerAndConfigurationSettingsImpl settings = new RunnerAndConfigurationSettingsImpl(
      RunManagerImpl.getInstanceImpl(project), rc, false
    );

    ExecutionEnvironment env1 = createEnv(project, settings);
    executionManager.restartRunProfile(env1);
    UIUtil.dispatchAllInvocationEvents();
    ProcessHandler processHandler1 = getProcessHandler(executionManager);

    ExecutionEnvironment env2 = createEnv(project, settings);
    executionManager.restartRunProfile(env2);
    // Dispatching all events at this point will run into an endless cycle, because
    // runContentDescriptors of the same type are asked to terminate and then are awaited for termination:
    // com.intellij.execution.impl.ExecutionManagerImpl.awaitTermination
    //
    // However, the created processHandler is not willing to terminate on the first request (surviveSoftKill=true).
    // It will be terminated on the second request: executionManager.restartRunProfile(env3)

    ProcessHandler processHandler2 = getProcessHandler(executionManager);
    assertSame(processHandler1, processHandler2);
    assertTrue(processHandler1.isProcessTerminating());

    ExecutionEnvironment env3 = createEnv(project, settings);
    executionManager.restartRunProfile(env3);
    UIUtil.dispatchAllInvocationEvents();

    FakeProcessHandler processHandler3 = getProcessHandler(executionManager);
    assertNotSame(processHandler1, processHandler3);

    assertTrue(!processHandler3.isProcessTerminating() && !processHandler3.isProcessTerminated());
    processHandler3.killProcess();
  }

  @NotNull
  private static FakeProcessHandler getProcessHandler(@NotNull ExecutionManagerImpl executionManager) {
    RunContentDescriptor descriptor = ExecutionTestUtil.getSingleRunContentDescriptor(executionManager);
    ProcessHandler processHandler = descriptor.getProcessHandler();
    assertNotNull(processHandler);
    return (FakeProcessHandler)processHandler;
  }

  @NotNull
  private static ExecutionEnvironment createEnv(@NotNull Project project, @NotNull RunnerAndConfigurationSettings settings) {
    return new ExecutionEnvironmentBuilder(project, DefaultRunExecutor.getRunExecutorInstance())
      .runnerAndSettings(FakeProgramRunner.INSTANCE, settings)
      .build();
  }
}
