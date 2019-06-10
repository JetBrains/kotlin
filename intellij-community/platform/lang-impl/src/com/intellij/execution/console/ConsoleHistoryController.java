// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.AppTopics;
import com.intellij.CommonBundle;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.execution.console.ConsoleHistoryModel.Entry;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.idea.ActionsBundle;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoConstants;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.ContentChooser;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.SafeWriteRequestor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingRegistry;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author gregsh
 */
public class ConsoleHistoryController implements Disposable {
  private static final Logger LOG = Logger.getInstance("com.intellij.execution.console.ConsoleHistoryController");

  private final static Map<LanguageConsoleView, ConsoleHistoryController> ourControllers =
    ContainerUtil.createConcurrentWeakMap(ContainerUtil.identityStrategy());

  private final LanguageConsoleView myConsole;
  private final AnAction myHistoryNext = new MyAction(true, getKeystrokesUpDown(true));
  private final AnAction myHistoryPrev = new MyAction(false, getKeystrokesUpDown(false));
  private final AnAction myBrowseHistory = new MyBrowseAction();
  private boolean myMultiline;
  private ModelHelper myHelper;
  private long myLastSaveStamp;

  @Deprecated
  public ConsoleHistoryController(@NotNull String type, @Nullable String persistenceId, @NotNull LanguageConsoleView console) {
    this(new ConsoleRootType(type, null) { }, persistenceId, console);
  }

  public ConsoleHistoryController(@NotNull ConsoleRootType rootType, @Nullable String persistenceId, @NotNull LanguageConsoleView console) {
    this(rootType, fixNullPersistenceId(persistenceId, console), console,
         ConsoleHistoryModelProvider.findModelForConsole(fixNullPersistenceId(persistenceId, console), console));
  }

  private ConsoleHistoryController(@NotNull ConsoleRootType rootType, @NotNull String persistenceId,
                                   @NotNull LanguageConsoleView console, @NotNull ConsoleHistoryModel model) {
    myHelper = new ModelHelper(rootType, persistenceId, model);
    myConsole = console;
  }

  @TestOnly
  public void setModel(@NotNull ConsoleHistoryModel model){
    myHelper = new ModelHelper(myHelper.myRootType, myHelper.myId, model);
  }

  @Override
  public void dispose() {
  }

  //@Nullable
  public static ConsoleHistoryController getController(@NotNull LanguageConsoleView console) {
    return ourControllers.get(console);
  }

  public static void addToHistory(@NotNull LanguageConsoleView consoleView, @Nullable String command) {
    ConsoleHistoryController controller = getController(consoleView);
    if (controller != null) {
      controller.addToHistory(command);
    }
  }

  public void addToHistory(@Nullable String command) {
    getModel().addToHistory(command);
  }

  public boolean hasHistory() {
    return !getModel().isEmpty();
  }

  @NotNull
  private static String fixNullPersistenceId(@Nullable String persistenceId, @NotNull LanguageConsoleView console) {
    if (StringUtil.isNotEmpty(persistenceId)) return persistenceId;
    String url = console.getProject().getPresentableUrl();
    return StringUtil.isNotEmpty(url) ? url : "default";
  }

  public boolean isMultiline() {
    return myMultiline;
  }

  public ConsoleHistoryController setMultiline(boolean  multiline) {
    myMultiline = multiline;
    return this;
  }

  ConsoleHistoryModel getModel() {
    return myHelper.getModel();
  }

  public void install() {
    MessageBusConnection busConnection = myConsole.getProject().getMessageBus().connect(myConsole);
    busConnection.subscribe(ProjectEx.ProjectSaved.TOPIC, new ProjectEx.ProjectSaved() {
      @Override
      public void duringSave(@NotNull Project project) {
        saveHistory();
      }
    });
    busConnection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {
      @Override
      public void beforeDocumentSaving(@NotNull Document document) {
        if (document == myConsole.getEditorDocument()) {
          saveHistory();
        }
      }
    });

    ConsoleHistoryController original = ourControllers.put(myConsole, this);
    LOG.assertTrue(original == null,
                   "History controller already installed for: " + myConsole.getTitle());
    Disposer.register(myConsole, this);
    Disposer.register(this, new Disposable() {
      @Override
      public void dispose() {
        ConsoleHistoryController controller = getController(myConsole);
        if (controller == ConsoleHistoryController.this) {
          ourControllers.remove(myConsole);
        }
        saveHistory();
      }
    });
    if (myHelper.getModel().getHistorySize() == 0) {
      loadHistory(myHelper.getId());
    }
    configureActions();
    myLastSaveStamp = getCurrentTimeStamp();
  }

  private long getCurrentTimeStamp() {
    return getModel().getModificationCount() + myConsole.getEditorDocument().getModificationStamp();
  }

  private void configureActions() {
    EmptyAction.setupAction(myHistoryNext, "Console.History.Next", null);
    EmptyAction.setupAction(myHistoryPrev, "Console.History.Previous", null);
    EmptyAction.setupAction(myBrowseHistory, "Console.History.Browse", null);
    if (!myMultiline) {
      addShortcuts(myHistoryNext, getShortcutUpDown(true));
      addShortcuts(myHistoryPrev, getShortcutUpDown(false));
    }
    myHistoryNext.registerCustomShortcutSet(myHistoryNext.getShortcutSet(), myConsole.getCurrentEditor().getComponent());
    myHistoryPrev.registerCustomShortcutSet(myHistoryPrev.getShortcutSet(), myConsole.getCurrentEditor().getComponent());
    myBrowseHistory.registerCustomShortcutSet(myBrowseHistory.getShortcutSet(), myConsole.getCurrentEditor().getComponent());
  }

  /**
   * Use this method if you decided to change the id for your console but don't want your users to loose their current histories
   * @param id previous id id
   * @return true if some text has been loaded; otherwise false
   */
  public boolean loadHistory(String id) {
    CharSequence prev = myHelper.getContent();
    boolean result = myHelper.loadHistory(id);
    CharSequence userValue = myHelper.getContent();
    if (prev != userValue && userValue != null) {
      setConsoleText(new Entry(userValue, -1), false, false);
    }
    return result;
  }

  private void saveHistory() {
    if (myLastSaveStamp == getCurrentTimeStamp()) {
      return;
    }

    myHelper.setContent(myConsole.getEditorDocument().getText());
    myHelper.saveHistory();
    myLastSaveStamp = getCurrentTimeStamp();
  }

  public AnAction getHistoryNext() {
    return myHistoryNext;
  }

  public AnAction getHistoryPrev() {
    return myHistoryPrev;
  }

  public AnAction getBrowseHistory() {
    return myBrowseHistory;
  }

  protected void setConsoleText(final Entry command, final boolean storeUserText, final boolean regularMode) {
    if (regularMode && myMultiline && StringUtil.isEmptyOrSpaces(command.getText())) return;
    final Editor editor = myConsole.getCurrentEditor();
    final Document document = editor.getDocument();
    WriteCommandAction.writeCommandAction(myConsole.getProject()).run(() -> {
      if (storeUserText) {
        String text = document.getText();
        if (Comparing.equal(command.getText(), text) && myHelper.getContent() != null) return;
        myHelper.setContent(text);
        myHelper.getModel().setContent(text);
      }
      CharSequence text = ObjectUtils.chooseNotNull(command.getText(), "");
      int offset;
      if (regularMode) {
        if (myMultiline) {
          offset = insertTextMultiline(text, editor, document);
        }
        else {
          document.setText(text);
          offset = command.getOffset() == -1 ? document.getTextLength() : command.getOffset();
        }
      }
      else {
        offset = 0;
        try {
          document.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
          document.setText(text);
        }
        finally {
          document.putUserData(UndoConstants.DONT_RECORD_UNDO, null);
        }
      }
      editor.getCaretModel().moveToOffset(offset);
      editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    });
  }

  protected int insertTextMultiline(CharSequence text, Editor editor, Document document) {
    TextRange selection = EditorUtil.getSelectionInAnyMode(editor);

    int start = document.getLineStartOffset(document.getLineNumber(selection.getStartOffset()));
    int end = document.getLineEndOffset(document.getLineNumber(selection.getEndOffset()));

    document.replaceString(start, end, text);
    editor.getSelectionModel().setSelection(start, start + text.length());
    return start;
  }

  private class MyAction extends DumbAwareAction {
    private final boolean myNext;

    @NotNull
    private final Collection<KeyStroke> myUpDownKeystrokes;

    MyAction(final boolean next, @NotNull Collection<KeyStroke> upDownKeystrokes) {
      myNext = next;
      myUpDownKeystrokes = upDownKeystrokes;
      getTemplatePresentation().setVisible(false);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      boolean hasHistory = getModel().hasHistory(); // need to check before next line's side effect
      Entry command = myNext ? getModel().getHistoryNext() : getModel().getHistoryPrev();
      if (!myMultiline && command == null || !hasHistory && !myNext) return;
      setConsoleText(command, !hasHistory, true);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      super.update(e);
      boolean enabled = myMultiline || !isUpDownKey(e) || canMoveInEditor(myNext);
      //enabled &= getModel().hasHistory(myNext);
      e.getPresentation().setEnabled(enabled);
    }

    private boolean isUpDownKey(@NotNull AnActionEvent e) {
      final InputEvent event = e.getInputEvent();
      if (!(event instanceof KeyEvent)) {
        return false;
      }
      final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent((KeyEvent)event);
      return myUpDownKeystrokes.contains(keyStroke);
    }
  }

  private boolean canMoveInEditor(final boolean next) {
    final Editor consoleEditor = myConsole.getCurrentEditor();
    final Document document = consoleEditor.getDocument();
    final CaretModel caretModel = consoleEditor.getCaretModel();

    if (LookupManager.getActiveLookup(consoleEditor) != null) return false;

    if (next) {
      return document.getLineNumber(caretModel.getOffset()) == 0;
    }
    else {
      final int lineCount = document.getLineCount();
      return (lineCount == 0 || document.getLineNumber(caretModel.getOffset()) == lineCount - 1) &&
             (StringUtil.isEmptyOrSpaces(document.getText().substring(caretModel.getOffset())) || myHelper.getModel().prevOnLastLine());
    }
  }



  private class MyBrowseAction extends DumbAwareAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
      boolean enabled = hasHistory();
      e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      String title = myConsole.getTitle() + " History";
      final ContentChooser<String> chooser = new ContentChooser<String>(myConsole.getProject(), title, true, true) {
        {
          setOKButtonText(ActionsBundle.actionText(IdeActions.ACTION_EDITOR_PASTE));
          setOKButtonMnemonic('P');
          setCancelButtonText(CommonBundle.getCloseButtonText());
          setUseNumbering(false);
        }

        @Override
        protected void removeContentAt(String content) {
          getModel().removeFromHistory(content);
        }

        @Override
        protected String getStringRepresentationFor(String content) {
          return content;
        }

        @NotNull
        @Override
        protected List<String> getContents() {
          return getModel().getEntries();
        }

        @Override
        protected Editor createIdeaEditor(String text) {
          PsiFile consoleFile = myConsole.getFile();
          Language language = consoleFile.getLanguage();
          Project project = consoleFile.getProject();

          PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(
            "a." + consoleFile.getFileType().getDefaultExtension(),
            language,
            StringUtil.convertLineSeparators(text), false, true);
          VirtualFile virtualFile = psiFile.getViewProvider().getVirtualFile();
          if (virtualFile instanceof LightVirtualFile) ((LightVirtualFile)virtualFile).setWritable(false);
          Document document = ObjectUtils.notNull(FileDocumentManager.getInstance().getDocument(virtualFile));
          EditorFactory editorFactory = EditorFactory.getInstance();
          EditorEx editor = (EditorEx)editorFactory.createViewer(document, project);
          editor.getSettings().setFoldingOutlineShown(false);
          editor.getSettings().setLineMarkerAreaShown(false);
          editor.getSettings().setIndentGuidesShown(false);

          SyntaxHighlighter highlighter =
            SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, psiFile.getViewProvider().getVirtualFile());
          editor.setHighlighter(new LexerEditorHighlighter(highlighter, editor.getColorsScheme()));
          return editor;
        }
      };
      chooser.setContentIcon(null);
      chooser.setSplitterOrientation(false);
      chooser.setSelectedIndex(Math.max(0, getModel().getHistorySize() - 1));

      if (chooser.showAndGet() && myConsole.getCurrentEditor().getComponent().isShowing()) {
        setConsoleText(new Entry(chooser.getSelectedText(), -1), false, true);
      }
    }
  }

  public static final class ModelHelper implements SafeWriteRequestor {
    private final ConsoleRootType myRootType;
    private final String myId;
    private final ConsoleHistoryModel myModel;
    private CharSequence myContent;

    public ModelHelper(ConsoleRootType rootType, String id, ConsoleHistoryModel model) {
      myRootType = rootType;
      myId = id;
      myModel = model;
    }

    public ConsoleHistoryModel getModel() {
      return myModel;
    }

    public void setContent(String userValue) {
      myContent = userValue;
    }

    public String getId() {
      return myId;
    }

    public CharSequence getContent() {
      return myContent;
    }

    @Nullable
    File getFile(String id) {
      if (myRootType.isHidden()) return null;
      String rootPath = ScratchFileService.getInstance().getRootPath(HistoryRootType.getInstance());
      return new File(FileUtil.toSystemDependentName(rootPath + "/" + getHistoryName(myRootType, id)));
    }

    @NotNull
    Charset getCharset() {
      return EncodingRegistry.getInstance().getDefaultCharset();
    }

    public boolean loadHistory(String id) {
      try {
        File file = getFile(id);
        if (file == null || !file.exists()) {
          return false;
        }
        String[] split = FileUtil.loadFile(file, getCharset()).split(myRootType.getEntrySeparator());
        getModel().resetEntries(Arrays.asList(split));
        return true;
      }
      catch (Exception ignored) {
        return false;
      }
    }

    private void saveHistory() {
      if (getModel().isEmpty()) return;
      File file = getFile(myId);
      if (file == null) return;
      File dir = file.getParentFile();
      if (dir == null || dir.exists() && !dir.isDirectory() || !dir.exists() && !dir.mkdirs()) {
        LOG.error("Unable to create " + file.getPath());
        return;
      }
      try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), getCharset()))) {
        boolean first = true;
        for (String entry : getModel().getEntries()) {
          if (first) first = false;
          else out.write(myRootType.getEntrySeparator());
          out.write(entry);
        }
        out.flush();
      }
      catch (Exception ex) {
        LOG.error(ex);
      }
    }
  }

  @NotNull
  private static String getHistoryName(@NotNull ConsoleRootType rootType, @NotNull String id) {
    return rootType.getConsoleTypeId() + "/" +
           PathUtil.makeFileName(rootType.getHistoryPathName(id), rootType.getDefaultFileExtension());
  }

  @Nullable
  public static VirtualFile getContentFile(@NotNull final ConsoleRootType rootType, @NotNull String id, ScratchFileService.Option option) {
    final String pathName = PathUtil.makeFileName(rootType.getContentPathName(id), rootType.getDefaultFileExtension());
    try {
      return rootType.findFile(null, pathName, option);
    }
    catch (final IOException e) {
      LOG.warn(e);
      ApplicationManager.getApplication().invokeLater(() -> {
        String message = String.format("Unable to open '%s/%s'\nReason: %s", rootType.getId(), pathName, e.getLocalizedMessage());
        Messages.showErrorDialog(message, "Unable to Open File");
      });
      return null;
    }
  }

  private static ShortcutSet getShortcutUpDown(boolean isUp) {
    AnAction action = ActionManager.getInstance().getActionOrStub(isUp ?
                                                                  IdeActions.ACTION_EDITOR_MOVE_CARET_UP :
                                                                  IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN);
    if (action != null) {
      return action.getShortcutSet();
    }
    return new CustomShortcutSet(KeyStroke.getKeyStroke(isUp ? KeyEvent.VK_UP : KeyEvent.VK_DOWN, 0));
  }

  private static void addShortcuts(@NotNull AnAction action, @NotNull ShortcutSet newShortcuts) {
    if (action.getShortcutSet().getShortcuts().length == 0) {
      action.registerCustomShortcutSet(newShortcuts, null);
    }
    else {
      action.registerCustomShortcutSet(new CompositeShortcutSet(action.getShortcutSet(), newShortcuts), null);
    }
  }

  private static Collection<KeyStroke> getKeystrokesUpDown(boolean isUp) {
    Collection<KeyStroke> result = new ArrayList<>();

    final ShortcutSet shortcutSet = getShortcutUpDown(isUp);
    for (Shortcut shortcut : shortcutSet.getShortcuts()) {
      if (shortcut.isKeyboard() && ((KeyboardShortcut)shortcut).getSecondKeyStroke() == null) {
        result.add(((KeyboardShortcut)shortcut).getFirstKeyStroke());
      }
    }

    return result;
  }

}
