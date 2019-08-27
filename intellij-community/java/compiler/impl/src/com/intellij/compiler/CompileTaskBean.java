// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ProjectExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public final class CompileTaskBean extends AbstractExtensionPointBean {
  public static final ProjectExtensionPointName<CompileTaskBean> EP_NAME = new ProjectExtensionPointName<>("com.intellij.compiler.task");
  public enum CompileTaskExecutionPhase { BEFORE, AFTER }

  @Attribute("execute")
  public CompileTaskExecutionPhase myExecutionPhase = CompileTaskExecutionPhase.BEFORE;

  @Attribute("implementation")
  public String myImplementation;

  @Nullable
  private volatile CompileTask myInstance;

  @NotNull
  public CompileTask getTaskInstance(@NotNull Project project) {
    CompileTask result = myInstance;
    if (result == null) {
      //noinspection SynchronizeOnThis
      synchronized (this) {
        result = myInstance;
        if (result == null) {
          //noinspection NonPrivateFieldAccessedInSynchronizedContext
          result = project.instantiateExtensionWithPicoContainerOnlyIfNeeded(myImplementation, myPluginDescriptor);
          myInstance = result;
        }
      }
    }
    return result;
  }
}
