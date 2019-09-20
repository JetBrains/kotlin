// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.SkipInHeadlessEnvironment;

/**
 * @author Dmitry Avdeev
 */
@SkipInHeadlessEnvironment
public abstract class ToolWindowManagerTestCase extends LightPlatformCodeInsightTestCase {
  protected ToolWindowManagerImpl myManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myManager = new ToolWindowManagerImpl(getProject()) {
      @Override
      protected void fireStateChanged() {
      }
    };
    Disposer.register(getTestRootDisposable(), myManager);
    ServiceContainerUtil.registerComponentInstance(getProject(), ToolWindowManager.class, myManager, getTestRootDisposable());
    myManager.init();
  }

  @Override
  public void tearDown() throws Exception {
    try {
      myManager.projectClosed();
      myManager = null;
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }
}
