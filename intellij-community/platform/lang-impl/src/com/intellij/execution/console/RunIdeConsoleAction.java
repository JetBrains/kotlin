// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.script.IdeConsoleScriptBindings;
import com.intellij.ide.script.IdeScriptEngine;
import com.intellij.ide.script.IdeScriptEngineManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.content.Content;
import com.intellij.util.Consumer;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author gregsh
 */
public class RunIdeConsoleAction extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(RunIdeConsoleAction.class);

  private static final String DEFAULT_FILE_NAME = "ide-scripting";

  private static final Key<IdeScriptEngineManager.EngineInfo> SELECTED_ENGINE_INFO_KEY = Key.create("SELECTED_ENGINE_INFO_KEY");
  private static final Key<Trinity<IdeScriptEngine, IdeScriptEngineManager.EngineInfo, VirtualFile>> SCRIPT_ENGINE_KEY =
    Key.create("SCRIPT_ENGINE_KEY");

  @Override
  public void update(@NotNull AnActionEvent e) {
    IdeScriptEngineManager manager = IdeScriptEngineManager.getInstance();
    e.getPresentation().setVisible(e.getProject() != null);
    e.getPresentation().setEnabled(manager.isInitialized() && !manager.getEngineInfos().isEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    List<IdeScriptEngineManager.EngineInfo> infos = IdeScriptEngineManager.getInstance().getEngineInfos();
    chooseScriptEngineAndRun(e, infos, engineInfo -> runConsole(project, engineInfo));
  }

  static void chooseScriptEngineAndRun(@NotNull AnActionEvent e,
                                       @NotNull List<IdeScriptEngineManager.EngineInfo> infos,
                                       @NotNull Consumer<? super IdeScriptEngineManager.EngineInfo> onChosen) {
    if (infos.size() == 1) {
      onChosen.consume(infos.iterator().next());
      return;
    }

    List<? extends AnAction> actions = JBIterable.from(infos).map(engineInfo -> {
      String lang = engineInfo.languageName;
      String eng = engineInfo.engineName;
      if (StringUtil.toLowerCase(lang).equals(lang)) lang = StringUtil.capitalize(lang);
      if (StringUtil.toLowerCase(eng).equals(eng)) eng = StringUtil.capitalize(eng);
      String name = lang + " (" + eng + ")";
      IdeaPluginDescriptor plugin = engineInfo.pluginId == null ? null : PluginManagerCore.getPlugin(engineInfo.pluginId);
      String description = lang + " (engine: " + eng +
                           (plugin == null ? "" : ", plugin: " + plugin.getName()) + ")";
      return new DumbAwareAction(name, description, null) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e1) {
          onChosen.consume(engineInfo);
        }
      };
    })
      .sort(Comparator.comparing(o -> o.getTemplatePresentation().getText()))
      .toList();
    DefaultActionGroup actionGroup = new DefaultActionGroup(actions);
    JBPopupFactory.getInstance().createActionGroupPopup(
      "Script Engine", actionGroup, e.getDataContext(), JBPopupFactory.ActionSelectionAid.NUMBERING, false)
      .showInBestPositionFor(e.getDataContext());
  }

  protected void runConsole(@NotNull Project project, @NotNull IdeScriptEngineManager.EngineInfo info) {
    List<String> extensions = info.fileExtensions;
    try {
      String pathName = PathUtil.makeFileName(DEFAULT_FILE_NAME, ContainerUtil.getFirstItem(extensions));
      VirtualFile virtualFile = IdeConsoleRootType.getInstance().findFile(project, pathName, ScratchFileService.Option.create_if_missing);
      if (virtualFile != null) {
        virtualFile.putUserData(SELECTED_ENGINE_INFO_KEY, info);
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
      }
    }
    catch (IOException ex) {
      LOG.error(ex);
    }
  }

  public static void configureConsole(@NotNull VirtualFile file, @NotNull FileEditorManager source) {
    MyRunAction runAction = new MyRunAction();
    for (FileEditor fileEditor : source.getEditors(file)) {
      if (!(fileEditor instanceof TextEditor)) continue;
      Editor editor = ((TextEditor)fileEditor).getEditor();
      runAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, editor.getComponent());
    }
  }

  private static void executeQuery(@NotNull Project project,
                                   @NotNull VirtualFile file,
                                   @NotNull Editor editor,
                                   @NotNull IdeScriptEngineManager.EngineInfo engineInfo) {
    String command = getCommandText(project, editor);
    if (StringUtil.isEmptyOrSpaces(command)) return;
    String profile = getProfileText(file);
    RunContentDescriptor descriptor = getConsoleView(project, file, engineInfo);
    IdeScriptEngine engine;
    Content content = descriptor.getAttachedContent();
    if (content == null) {
      LOG.error("Attached content expected");
      return;
    }
    else {
      Trinity<IdeScriptEngine, IdeScriptEngineManager.EngineInfo, VirtualFile> data = content.getUserData(SCRIPT_ENGINE_KEY);
      if (data != null) {
        engine = data.first;
      }
      else {
        engine = IdeScriptEngineManager.getInstance().getEngine(engineInfo, null);
        if (engine == null) {
          LOG.error("Script engine not found for: " + file.getName());
          return;
        }
        content.putUserData(SCRIPT_ENGINE_KEY, Trinity.create(engine, engineInfo, file));
      }
    }

    ConsoleViewImpl consoleView = (ConsoleViewImpl)descriptor.getExecutionConsole();

    prepareEngine(project, engine, descriptor);
    try {
      long ts = System.currentTimeMillis();
      //myHistoryController.getModel().addToHistory(command);
      consoleView.print("> " + command, ConsoleViewContentType.USER_INPUT);
      consoleView.print("\n", ConsoleViewContentType.USER_INPUT);
      String script = profile == null ? command : profile + "\n" + command;
      Object o = engine.eval(script);
      String prefix = "["+(StringUtil.formatDuration(System.currentTimeMillis() - ts))+"]";
      consoleView.print(prefix + "=> " + o, ConsoleViewContentType.NORMAL_OUTPUT);
      consoleView.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }
    catch (Throwable e) {
      Throwable ex = ExceptionUtil.getRootCause(e);
      String message = StringUtil.notNullize(StringUtil.nullize(ex.getMessage()), ex.toString());
      consoleView.print(ex.getClass().getSimpleName() + ": " + message, ConsoleViewContentType.ERROR_OUTPUT);
      consoleView.print("\n", ConsoleViewContentType.ERROR_OUTPUT);
    }
    selectContent(descriptor);
  }

  private static void prepareEngine(@NotNull Project project, @NotNull IdeScriptEngine engine, @NotNull RunContentDescriptor descriptor) {
    IdeConsoleScriptBindings.ensureIdeIsBound(project, engine);
    ensureOutputIsRedirected(engine, descriptor);
  }

  @Nullable
  private static String getProfileText(@NotNull VirtualFile file) {
    try {
      VirtualFile folder = file.getParent();
      VirtualFile profileChild = folder == null ? null : folder.findChild(".profile." + file.getExtension());
      return profileChild == null ? null : StringUtil.nullize(VfsUtilCore.loadText(profileChild));
    }
    catch (IOException ignored) {
    }
    return null;
  }

  @NotNull
  private static String getCommandText(@NotNull Project project, @NotNull Editor editor) {
    TextRange selectedRange = EditorUtil.getSelectionInAnyMode(editor);
    Document document = editor.getDocument();
    if (selectedRange.isEmpty()) {
      int line = document.getLineNumber(selectedRange.getStartOffset());
      selectedRange = TextRange.create(document.getLineStartOffset(line), document.getLineEndOffset(line));

      // try detect a non-trivial composite PSI element if there's a PSI file
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file != null && file.getFirstChild() != null && file.getFirstChild() != file.getLastChild()) {
        PsiElement e1 = file.findElementAt(selectedRange.getStartOffset());
        PsiElement e2 = file.findElementAt(selectedRange.getEndOffset());
        while (e1 != e2 && (e1 instanceof PsiWhiteSpace || e1 != null && StringUtil.isEmptyOrSpaces(e1.getText()))) {
          e1 = ObjectUtils.chooseNotNull(e1.getNextSibling(), PsiTreeUtil.getDeepestFirst(e1.getParent()));
        }
        while (e1 != e2 && (e2 instanceof PsiWhiteSpace || e2 != null && StringUtil.isEmptyOrSpaces(e2.getText()))) {
          e2 = ObjectUtils.chooseNotNull(e2.getPrevSibling(), PsiTreeUtil.getDeepestLast(e2.getParent()));
        }
        if (e1 instanceof LeafPsiElement) e1 = e1.getParent();
        if (e2 instanceof LeafPsiElement) e2 = e2.getParent();
        PsiElement parent = e1 == null ? e2 : e2 == null ? e1 : PsiTreeUtil.findCommonParent(e1, e2);
        if (parent != null && parent != file) {
          selectedRange = parent.getTextRange();
        }
      }
    }
    return document.getText(selectedRange);
  }

  private static void selectContent(RunContentDescriptor descriptor) {
    Executor executor = DefaultRunExecutor.getRunExecutorInstance();
    ConsoleViewImpl consoleView = ObjectUtils.assertNotNull((ConsoleViewImpl)descriptor.getExecutionConsole());
    ExecutionManager.getInstance(consoleView.getProject()).getContentManager().toFrontRunContent(executor, descriptor);
  }

  @NotNull
  private static RunContentDescriptor getConsoleView(@NotNull Project project,
                                                     @NotNull VirtualFile file,
                                                     @NotNull IdeScriptEngineManager.EngineInfo engineInfo) {
    for (RunContentDescriptor existing : ExecutionManager.getInstance(project).getContentManager().getAllDescriptors()) {
      Content content = existing.getAttachedContent();
      if (content == null) continue;
      Trinity<IdeScriptEngine, IdeScriptEngineManager.EngineInfo, VirtualFile> data = content.getUserData(SCRIPT_ENGINE_KEY);
      if (data == null) continue;
      if (!Comparing.equal(file, data.third)) continue;
      if (!Comparing.equal(engineInfo, data.second)) continue;
      return existing;
    }

    ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

    DefaultActionGroup toolbarActions = new DefaultActionGroup();
    JComponent panel = new JPanel(new BorderLayout());
    panel.add(consoleView.getComponent(), BorderLayout.CENTER);
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("RunIdeConsole", toolbarActions, false);
    toolbar.setTargetComponent(consoleView.getComponent());
    panel.add(toolbar.getComponent(), BorderLayout.WEST);

    RunContentDescriptor descriptor = new RunContentDescriptor(consoleView, null, panel, file.getName()) {
      @Override
      public boolean isContentReuseProhibited() {
        return true;
      }
    };
    Executor executor = DefaultRunExecutor.getRunExecutorInstance();
    toolbarActions.addAll(consoleView.createConsoleActions());
    toolbarActions.add(new CloseAction(executor, descriptor, project));
    ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, descriptor);

    return descriptor;
  }

  private static class MyRunAction extends DumbAwareAction {

    private IdeScriptEngineManager.EngineInfo engineInfo;

    @Override
    public void update(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      Editor editor = e.getData(CommonDataKeys.EDITOR);
      VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
      e.getPresentation().setEnabledAndVisible(
        project != null && editor != null && virtualFile != null && (
          engineInfo != null ||
          virtualFile.getUserData(SELECTED_ENGINE_INFO_KEY) != null ||
          virtualFile.getExtension() != null));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      Editor editor = e.getData(CommonDataKeys.EDITOR);
      VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
      if (project == null || editor == null || virtualFile == null) return;
      String extension = virtualFile.getExtension();
      IdeScriptEngineManager.EngineInfo engineInfo0 = virtualFile.getUserData(SELECTED_ENGINE_INFO_KEY);
      if (engineInfo == null && engineInfo0 == null && extension == null) return;

      PsiDocumentManager.getInstance(project).commitAllDocuments();

      if (engineInfo == null ||
          engineInfo0 != null && !Comparing.equal(engineInfo0, engineInfo) ||
          extension != null && !engineInfo.fileExtensions.contains(extension)) {
        virtualFile.putUserData(SELECTED_ENGINE_INFO_KEY, null);

        List<IdeScriptEngineManager.EngineInfo> infos =
          engineInfo0 != null
          ? Collections.singletonList(engineInfo0)
          : JBIterable.from(IdeScriptEngineManager.getInstance().getEngineInfos())
            .filter(o -> o.fileExtensions.contains(extension))
            .toList();

        chooseScriptEngineAndRun(e, infos, selectedInfo -> {
          engineInfo = selectedInfo;
          executeQuery(project, virtualFile, editor, engineInfo);
        });
      }
      else {
        executeQuery(project, virtualFile, editor, engineInfo);
      }
    }
  }

  private static void ensureOutputIsRedirected(@NotNull IdeScriptEngine engine, @NotNull RunContentDescriptor descriptor) {
    ConsoleWriter stdOutWriter = ObjectUtils.tryCast(engine.getStdOut(), ConsoleWriter.class);
    ConsoleWriter stdErrWriter = ObjectUtils.tryCast(engine.getStdErr(), ConsoleWriter.class);
    if (stdOutWriter != null && stdOutWriter.getDescriptor() == descriptor &&
        stdErrWriter != null && stdErrWriter.getDescriptor() == descriptor) {
      return;
    }

    WeakReference<RunContentDescriptor> ref = new WeakReference<>(descriptor);
    engine.setStdOut(new ConsoleWriter(ref, ConsoleViewContentType.NORMAL_OUTPUT));
    engine.setStdErr(new ConsoleWriter(ref, ConsoleViewContentType.ERROR_OUTPUT));
  }

  private static class ConsoleWriter extends Writer {
    private final WeakReference<RunContentDescriptor> myDescriptor;
    private final ConsoleViewContentType myOutputType;

    private ConsoleWriter(@NotNull WeakReference<RunContentDescriptor> descriptor, @NotNull ConsoleViewContentType outputType) {
      myDescriptor = descriptor;
      myOutputType = outputType;
    }

    @Nullable
    public RunContentDescriptor getDescriptor() {
      return myDescriptor.get();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      RunContentDescriptor descriptor = myDescriptor.get();
      ConsoleViewImpl console = ObjectUtils.tryCast(descriptor != null ? descriptor.getExecutionConsole() : null, ConsoleViewImpl.class);
      if (console == null) {
        //TODO ignore ?
        throw new IOException("The console is not available.");
      }
      console.print(new String(cbuf, off, len), myOutputType);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
  }
}
