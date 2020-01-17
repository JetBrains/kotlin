// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author Eugene Zhuravlev
 */
package com.intellij.compiler;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "CompilerWorkspaceConfiguration", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class CompilerWorkspaceConfiguration implements PersistentStateComponent<CompilerWorkspaceConfiguration> {
  private static final Logger LOG = Logger.getInstance(CompilerWorkspaceConfiguration.class);

  static {
    LOG.info("Available processors: " + Runtime.getRuntime().availableProcessors());
  }

  public boolean AUTO_SHOW_ERRORS_IN_EDITOR = true;
  public boolean DISPLAY_NOTIFICATION_POPUP = true;
  @Deprecated public boolean CLOSE_MESSAGE_VIEW_IF_SUCCESS = true;
  public boolean CLEAR_OUTPUT_DIRECTORY = true;
  public boolean MAKE_PROJECT_ON_SAVE = false; // until we fix problems with several open projects (IDEA-104064), daemon slowness (IDEA-104666)
  public boolean PARALLEL_COMPILATION = false;
  /**
   * @deprecated. Use corresponding value from CompilerConfiguration
   * This field is left here for compatibility with older projects
   */
  public int COMPILER_PROCESS_HEAP_SIZE = 700;
  public String COMPILER_PROCESS_ADDITIONAL_VM_OPTIONS = "";
  public boolean REBUILD_ON_DEPENDENCY_CHANGE = true;
  public boolean COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT = true;

  public static CompilerWorkspaceConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, CompilerWorkspaceConfiguration.class);
  }

  @Override
  public CompilerWorkspaceConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull CompilerWorkspaceConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean allowAutoMakeWhileRunningApplication() {
    return Registry.is("compiler.automake.allow.when.app.running", false);/*ALLOW_AUTOMAKE_WHILE_RUNNING_APPLICATION*/
  }
}
