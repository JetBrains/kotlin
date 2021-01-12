// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl.vcs;

import com.intellij.CommonBundle;
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.PairConsumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class UnloadedModulesCompilationCheckinHandler extends CheckinHandler {
  private final Project myProject;
  private final CheckinProjectPanel myCheckinPanel;

  public UnloadedModulesCompilationCheckinHandler(Project project, CheckinProjectPanel checkinPanel) {
    myProject = project;
    myCheckinPanel = checkinPanel;
  }

  @Nullable
  @Override
  public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
    if (ModuleManager.getInstance(myProject).getUnloadedModuleDescriptions().isEmpty()) {
      return null;
    }

    return new BooleanCommitOption(myCheckinPanel, JavaCompilerBundle.message("checkbox.text.compile.affected.unloaded.modules"), false,
                                   () -> getSettings().COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT,
                                   value -> getSettings().COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT = value);
  }

  @Override
  public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
    if (!getSettings().COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT ||
        ModuleManager.getInstance(myProject).getUnloadedModuleDescriptions().isEmpty()) {
      return ReturnResult.COMMIT;
    }

    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(myProject);
    CompilerManager compilerManager = CompilerManager.getInstance(myProject);
    Set<Module> affectedModules = new LinkedHashSet<>();
    for (VirtualFile file : myCheckinPanel.getVirtualFiles()) {
      if (compilerManager.isCompilableFileType(file.getFileType())) {
        ContainerUtil.addIfNotNull(affectedModules, fileIndex.getModuleForFile(file));
      }
    }

    Set<String> affectedUnloadedModules = new LinkedHashSet<>();
    for (Module module : affectedModules) {
      affectedUnloadedModules.addAll(DirectoryIndex.getInstance(myProject).getDependentUnloadedModules(module));
    }

    if (affectedUnloadedModules.isEmpty()) {
      return ReturnResult.COMMIT;
    }

    AtomicReference<BuildResult> result = new AtomicReference<>();
    compilerManager.makeWithModalProgress(new ModuleCompileScope(myProject, affectedModules, affectedUnloadedModules, true, false),
                                          new CompileStatusNotification() {
                                            @Override
                                            public void finished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
                                              result.set(
                                                aborted ? BuildResult.CANCELED : errors > 0 ? BuildResult.FAILED : BuildResult.SUCCESSFUL);
                                            }
                                          });

    if (result.get() == BuildResult.SUCCESSFUL) {
      return ReturnResult.COMMIT;
    }
    String message = JavaCompilerBundle.message("dialog.message.compilation.of.unloaded.modules.failed");
    int answer = Messages.showYesNoCancelDialog(myProject, XmlStringUtil.wrapInHtml(message), JavaCompilerBundle
                                                  .message("dialog.title.compilation.failed"),
                                                JavaCompilerBundle.message("button.text.checkin.handler.commit"),
                                                JavaCompilerBundle.message("button.text.checkin.handler.show.errors"),
                                                CommonBundle.getCancelButtonText(), null);

    if (answer == Messages.CANCEL) {
      return ReturnResult.CANCEL;
    }
    else if (answer == Messages.YES) {
      return ReturnResult.COMMIT;
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (toolWindow != null) {
          toolWindow.activate(null, false);
        }
      }, ModalityState.NON_MODAL);
      return ReturnResult.CLOSE_WINDOW;
    }
  }

  @NotNull
  private CompilerWorkspaceConfiguration getSettings() {
    return CompilerWorkspaceConfiguration.getInstance(myProject);
  }

  private enum BuildResult { SUCCESSFUL, FAILED, CANCELED }

  public static class Factory extends CheckinHandlerFactory {
    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
      return new UnloadedModulesCompilationCheckinHandler(panel.getProject(), panel);
    }
  }
}
