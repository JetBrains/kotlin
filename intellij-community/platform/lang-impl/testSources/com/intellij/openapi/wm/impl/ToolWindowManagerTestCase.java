// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.SkipInHeadlessEnvironment;

/**
 * @author Dmitry Avdeev
 */
@SkipInHeadlessEnvironment
public abstract class ToolWindowManagerTestCase extends LightPlatformCodeInsightTestCase {
  protected ToolWindowManagerImpl manager;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    manager = new ToolWindowManagerImpl(getProject()) {
      @Override
      protected void fireStateChanged() {
      }
    };
    ServiceContainerUtil.replaceService(getProject(), ToolWindowManager.class, manager, getTestRootDisposable());

    ProjectFrameHelper frame = new ProjectFrameHelper(new IdeFrameImpl(), null);
    frame.init();
    manager.init(frame);
  }

  @Override
  public void tearDown() throws Exception {
    try {
      manager.projectClosed();
      manager = null;
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }
}
