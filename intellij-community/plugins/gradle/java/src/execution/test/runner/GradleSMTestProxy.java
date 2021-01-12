// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.test.runner.events.TestEventResult;

/**
 * @author Vladislav.Soroka
 */
public class GradleSMTestProxy extends SMTestProxy {

  @Nullable private final String myClassName;
  @Nullable private String myParentId;
  @Nullable private TestEventResult myLastResult;

  public GradleSMTestProxy(String testName, boolean isSuite, @Nullable String locationUrl, @Nullable String className) {
    super(testName, isSuite, locationUrl);
    myClassName = className;
  }

  @Nullable
  public String getParentId() {
    return myParentId;
  }

  public void setParentId(@Nullable String parentId) {
    myParentId = parentId;
  }

  @Nullable
  public String getClassName() {
    return myClassName;
  }

  @ApiStatus.Experimental
  @Nullable
  public TestEventResult getLastResult() {
    return myLastResult;
  }

  @ApiStatus.Experimental
  public void setLastResult(@Nullable TestEventResult lastResult) {
    myLastResult = lastResult;
  }
}
