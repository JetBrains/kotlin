// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.build.*;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.console.DuplexConsoleView;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.FakeRerunAction;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.SerializationFilter;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;

import static com.intellij.openapi.util.text.StringUtil.nullize;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemRunConfiguration extends LocatableConfigurationBase implements SearchScopeProvidingRunProfile {
  static final ExtensionPointName<ExternalSystemRunConfigurationExtension> EP_NAME
    = ExtensionPointName.create("com.intellij.externalSystem.runConfigurationExtension");

  public static final Key<InputStream> RUN_INPUT_KEY = Key.create("RUN_INPUT_KEY");
  public static final Key<Class<? extends BuildProgressListener>> PROGRESS_LISTENER_KEY = Key.create("PROGRESS_LISTENER_KEY");

  static final Logger LOG = Logger.getInstance(ExternalSystemRunConfiguration.class);
  private ExternalSystemTaskExecutionSettings mySettings = new ExternalSystemTaskExecutionSettings();
  static final boolean DISABLE_FORK_DEBUGGER = Boolean.getBoolean("external.system.disable.fork.debugger");

  public ExternalSystemRunConfiguration(@NotNull ProjectSystemId externalSystemId,
                                        Project project,
                                        ConfigurationFactory factory,
                                        String name) {
    super(project, factory, name);
    mySettings.setExternalSystemIdString(externalSystemId.getId());
  }

  @Override
  public String suggestedName() {
    return AbstractExternalSystemTaskConfigurationType.generateName(getProject(), mySettings);
  }

  @Override
  public ExternalSystemRunConfiguration clone() {
    final Element element = new Element("toClone");
    try {
      writeExternal(element);
      RunConfiguration configuration = getFactory().createTemplateConfiguration(getProject());
      configuration.setName(getName());
      configuration.readExternal(element);
      return (ExternalSystemRunConfiguration)configuration;
    }
    catch (InvalidDataException | WriteExternalException e) {
      LOG.error(e);
      return null;
    }
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    Element e = element.getChild(ExternalSystemTaskExecutionSettings.TAG_NAME);
    if (e != null) {
      mySettings = XmlSerializer.deserialize(e, ExternalSystemTaskExecutionSettings.class);
    }
    EP_NAME.forEachExtensionSafe(extension -> extension.readExternal(this, element));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    element.addContent(XmlSerializer.serialize(mySettings, new SerializationFilter() {
      @Override
      public boolean accepts(@NotNull Accessor accessor, @NotNull Object bean) {
        // only these fields due to backward compatibility
        switch (accessor.getName()) {
          case "passParentEnvs":
            return !mySettings.isPassParentEnvs();
          case "env":
            return !mySettings.getEnv().isEmpty();
          default:
            return true;
        }
      }
    }));
    EP_NAME.forEachExtensionSafe(extension -> extension.writeExternal(this, element));
  }

  @NotNull
  public ExternalSystemTaskExecutionSettings getSettings() {
    return mySettings;
  }

  @NotNull
  @Override
  public SettingsEditor<ExternalSystemRunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup<ExternalSystemRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"),
                    new ExternalSystemRunConfigurationEditor(getProject(), mySettings.getExternalSystemId()));
    EP_NAME.forEachExtensionSafe(extension -> extension.appendEditors(this, group));
    group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
    return group;
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
    // DebugExecutor ID  - com.intellij.execution.executors.DefaultDebugExecutor.EXECUTOR_ID
    String debugExecutorId = ToolWindowId.DEBUG;
    ExternalSystemRunnableState
      runnableState = new ExternalSystemRunnableState(mySettings, getProject(), debugExecutorId.equals(executor.getId()), this, env);
    copyUserDataTo(runnableState);
    return runnableState;
  }

  @Nullable
  @Override
  public GlobalSearchScope getSearchScope() {
    GlobalSearchScope scope = null;
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(mySettings.getExternalSystemId());
    if (manager != null) {
      scope = manager.getSearchScope(getProject(), mySettings);
    }
    if (scope == null) {
      VirtualFile file = VfsUtil.findFileByIoFile(new File(mySettings.getExternalProjectPath()), false);
      if (file != null) {
        Module module = DirectoryIndex.getInstance(getProject()).getInfoForFile(file).getModule();
        if (module != null) {
          scope = GlobalSearchScopes.executionScope(Collections.singleton(module));
        }
      }
    }
    return scope;
  }

  /**
   * @deprecated Internal class {@link MyRunnableState} was turned into fully fledged class {@link ExternalSystemRunnableState}.
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
  public static class MyRunnableState extends ExternalSystemRunnableState {
    public MyRunnableState(@NotNull ExternalSystemTaskExecutionSettings settings,
                           @NotNull Project project,
                           boolean debug,
                           @NotNull ExternalSystemRunConfiguration configuration,
                           @NotNull ExecutionEnvironment env) {
      super(settings, project, debug, configuration, env);
    }
  }

  static void foldGreetingOrFarewell(@Nullable ExecutionConsole consoleView, String text, boolean isGreeting) {
    int limit = 100;
    if (text.length() < limit) {
      return;
    }
    final ConsoleViewImpl consoleViewImpl;
    if (consoleView instanceof ConsoleViewImpl) {
      consoleViewImpl = (ConsoleViewImpl)consoleView;
    }
    else if (consoleView instanceof DuplexConsoleView) {
      DuplexConsoleView duplexConsoleView = (DuplexConsoleView)consoleView;
      if (duplexConsoleView.getPrimaryConsoleView() instanceof ConsoleViewImpl) {
        consoleViewImpl = (ConsoleViewImpl)duplexConsoleView.getPrimaryConsoleView();
      }
      else if (duplexConsoleView.getSecondaryConsoleView() instanceof ConsoleViewImpl) {
        consoleViewImpl = (ConsoleViewImpl)duplexConsoleView.getSecondaryConsoleView();
      }
      else {
        consoleViewImpl = null;
      }
    }
    else {
      consoleViewImpl = null;
    }
    if (consoleViewImpl != null) {
      consoleViewImpl.performWhenNoDeferredOutput(() -> {
        if (!ApplicationManager.getApplication().isDispatchThread()) return;

        Document document = consoleViewImpl.getEditor().getDocument();
        int line = isGreeting ? 0 : document.getLineCount() - 2;
        if (CharArrayUtil.regionMatches(document.getCharsSequence(), document.getLineStartOffset(line), text)) {
          final FoldingModel foldingModel = consoleViewImpl.getEditor().getFoldingModel();
          foldingModel.runBatchFoldingOperation(() -> {
            FoldRegion region = foldingModel.addFoldRegion(document.getLineStartOffset(line),
                                                           document.getLineEndOffset(line) + 1,
                                                           StringUtil.trimLog(text, limit));
            if (region != null) {
              region.setExpanded(false);
            }
          });
        }
      });
    }
  }

  static class MyTaskRerunAction extends FakeRerunAction {
    private final BuildProgressListener myProgressListener;
    private final RunContentDescriptor myContentDescriptor;
    private final ExecutionEnvironment myEnvironment;

    MyTaskRerunAction(BuildProgressListener progressListener,
                      ExecutionEnvironment environment,
                      RunContentDescriptor contentDescriptor) {
      myProgressListener = progressListener;
      myContentDescriptor = contentDescriptor;
      myEnvironment = environment;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      Presentation presentation = event.getPresentation();
      ExecutionEnvironment environment = getEnvironment(event);
      if (environment != null) {
        presentation.setText(ExecutionBundle.message("rerun.configuration.action.name",
                                                     StringUtil.escapeMnemonics(environment.getRunProfile().getName())));
        Icon icon = ExecutionManagerImpl.isProcessRunning(getDescriptor(event))
                    ? AllIcons.Actions.Restart
                    : myProgressListener instanceof BuildViewManager
                      ? AllIcons.Actions.Compile
                      : environment.getExecutor().getIcon();
        presentation.setIcon(icon);
        presentation.setEnabled(isEnabled(event));
        return;
      }

      presentation.setEnabled(false);
    }

    @Nullable
    @Override
    protected RunContentDescriptor getDescriptor(AnActionEvent event) {
      return myContentDescriptor != null ? myContentDescriptor : super.getDescriptor(event);
    }

    @Override
    protected ExecutionEnvironment getEnvironment(@NotNull AnActionEvent event) {
      return myEnvironment;
    }
  }
}
