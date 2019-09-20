// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.diagnostic.logging;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleBuffer;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.FilterComponent;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class LogConsoleBase extends AdditionalTabComponent implements LogConsole, LogFilterListener {
  private static final Logger LOG = Logger.getInstance(LogConsoleBase.class);
  @NonNls public static final String APPLYING_FILTER_TITLE = "Applying filter...";

  private JPanel mySearchComponent;
  private JComboBox myLogFilterCombo;
  private JPanel myTextFilterWrapper;

  private boolean myDisposed;
  private ConsoleView myConsole;
  private final LightProcessHandler myProcessHandler = new LightProcessHandler();
  private ReaderThread myReaderThread;
  private StringBuffer myOriginalDocument = null;
  private String myLineUnderSelection = null;
  private int myLineOffset = -1;
  private LogContentPreprocessor myContentPreprocessor;
  private final Project myProject;
  private String myTitle = null;
  private boolean myWasInitialized;
  private final JPanel myTopComponent = new JPanel(new BorderLayout());
  private ActionGroup myActions;
  private final boolean myBuildInActions;
  private LogFilterModel myModel;
  private final LogFormatter myFormatter;

  private final List<LogConsoleListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<? extends LogFilter> myFilters;

  private FilterComponent myFilter = new FilterComponent("LOG_FILTER_HISTORY", 5) {
    @Override
    public void filter() {
      final Task.Backgroundable task = new Task.Backgroundable(myProject, APPLYING_FILTER_TITLE) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          myModel.updateCustomFilter(getFilter());
        }
      };
      ProgressManager.getInstance().run(task);
    }
  };

  public LogConsoleBase(@NotNull Project project, @Nullable Reader reader, String title, final boolean buildInActions, LogFilterModel model) {
    this(project, reader, title, buildInActions, model, GlobalSearchScope.allScope(project));
  }

  public LogConsoleBase(@NotNull Project project, @Nullable Reader reader, String title, final boolean buildInActions, LogFilterModel model,
                        @NotNull GlobalSearchScope scope){
    this(project, reader, title, buildInActions, model, scope, new DefaultLogFormatter());
  }

  public LogConsoleBase(@NotNull Project project, @Nullable Reader reader, String title, final boolean buildInActions, LogFilterModel model,
                        @NotNull GlobalSearchScope scope, LogFormatter formatter) {
    super(new BorderLayout());
    myProject = project;
    myTitle = title;
    myModel = model;
    myFormatter = formatter;
    myFilters = myModel.getLogFilters();
    myReaderThread = new ReaderThread(reader);
    myBuildInActions = buildInActions;
    TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project, scope);
    myConsole = builder.getConsole();
    myConsole.attachToProcess(myProcessHandler);
    myDisposed = false;
    myModel.addFilterListener(this);
  }

  @Override
  public void setFilterModel(LogFilterModel model) {
    if (myModel != null) {
      myModel.removeFilterListener(this);
    }
    myModel = model;
    myModel.addFilterListener(this);
  }

  @Override
  public LogFilterModel getFilterModel() {
    return myModel;
  }

  /**
   * @deprecated use {@link #getFilterModel()} instead
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Override
  public LogContentPreprocessor getContentPreprocessor() {
    return myContentPreprocessor;
  }

  /**
   * @deprecated use {@link #setFilterModel(LogFilterModel)} instead and
   *             customize log entry in {@link LogFilterModel#processLine(String)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Override
  public void setContentPreprocessor(final LogContentPreprocessor contentPreprocessor) {
    myContentPreprocessor = contentPreprocessor;
  }

  @Nullable
  protected BufferedReader updateReaderIfNeeded(@Nullable BufferedReader reader) throws IOException {
    return reader;
  }

  private JComponent createToolbar() {
    String customFilter = myModel.getCustomFilter();

    myFilter.reset();
    myFilter.setSelectedItem(customFilter != null ? customFilter : "");
    // Don't override Shift-TAB if screen reader is active. It is unclear why overriding
    // Shift-TAB was necessary in the first place.
    // See https://github.com/JetBrains/intellij-community/commit/a36a3a00db97e4d5b5c112bb4136a41d9435f667
    if (!ScreenReader.isActive()) {
      new AnAction() {
        {
          registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)),
                                    LogConsoleBase.this);
        }

        @Override
        public void actionPerformed(@NotNull final AnActionEvent e) {
          myFilter.requestFocusInWindow();
        }
      };
    }

    if (myBuildInActions) {
      final JComponent tbComp =
        ActionManager.getInstance().createActionToolbar("LogConsole", getOrCreateActions(), true).getComponent();
      myTopComponent.add(tbComp, BorderLayout.CENTER);
      myTopComponent.add(getSearchComponent(), BorderLayout.EAST);
    }


    return myTopComponent;
  }

  public ActionGroup getOrCreateActions() {
    if (myActions != null) return myActions;
    DefaultActionGroup group = new DefaultActionGroup();

    final AnAction[] actions = getConsoleNotNull().createConsoleActions();
    for (AnAction action : actions) {
      group.add(action);
    }

    group.addSeparator();

    /*for (final LogFilter filter : filters) {
      group.add(new ToggleAction(filter.getName(), filter.getName(), filter.getIcon()) {
        public boolean isSelected(AnActionEvent e) {
          return prefs.isFilterSelected(filter);
        }

        public void setSelected(AnActionEvent e, boolean state) {
          prefs.setFilterSelected(filter, state);
        }
      });
    }*/

    myActions = group;

    return myActions;
  }

  @Override
  public void onFilterStateChange(@NotNull final LogFilter filter) {
    filterConsoleOutput();
  }

  @Override
  public void onTextFilterChange() {
    filterConsoleOutput();
  }

  @Override
  @NotNull
  public JComponent getComponent() {
    if (!myWasInitialized) {
      myWasInitialized = true;
      add(getConsoleNotNull().getComponent(), BorderLayout.CENTER);
      add(createToolbar(), BorderLayout.NORTH);
    }
    return this;
  }

  public abstract boolean isActive();

  public void activate() {
    final ReaderThread readerThread = myReaderThread;
    if (readerThread == null) {
      return;
    }
    if (isActive() && !readerThread.myRunning) {
      resetLogFilter();
      myFilter.setSelectedItem(myModel.getCustomFilter());
      readerThread.startRunning();
      ApplicationManager.getApplication().executeOnPooledThread(readerThread);
    }
    else if (!isActive() && readerThread.myRunning) {
      readerThread.stopRunning();
    }
  }

  @NotNull
  @Override
  public String getTabTitle() {
    return myTitle;
  }

  @Override
  public void dispose() {
    myModel.removeFilterListener(this);
    stopRunning(false);
    synchronized (this) {
      myDisposed = true;
      if (myConsole != null) {
        Disposer.dispose(myConsole);
        myConsole = null;
      }
    }
    if (myFilter != null) {
      myFilter.dispose();
      myFilter = null;
    }
    myOriginalDocument = null;
  }

  private void stopRunning(boolean checkActive) {
    if (!checkActive) {
      fireLoggingWillBeStopped();
    }

    final ReaderThread readerThread = myReaderThread;
    if (readerThread != null && readerThread.myReader != null) {
      if (!checkActive) {
        readerThread.stopRunning();
        try {
          readerThread.myReader.close();
        }
        catch (IOException e) {
          LOG.warn(e);
        }
        readerThread.myReader = null;
        myReaderThread = null;
      }
      else {
        try {
          final BufferedReader reader = readerThread.myReader;
          List<String> lines = new ArrayList<>();
          while (reader.ready()) {
            lines.add(reader.readLine());
          }
          if (!lines.isEmpty()) {
            // If another thread holds the write lock and waits for the process termination
            // (i.e. inside `processHandler.waitFor()`), then acquiring the read lock inside
            // `ProcessListener.processTerminated` listener method will lead to a deadlock (IDEA-216297).
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
              //ensure have read lock before requiring for sync, otherwise dispose() under write action would lead to deadlock
              ReadAction.run(() -> {
                for (String line : lines) {
                  addMessage(line);
                }
              });
            });
          }
        }
        catch (IOException ignore) {}
        stopRunning(false);
      }
    }
  }

  protected synchronized void addMessage(final String text) {
    if (myDisposed) return;
    if (text == null) return;
    if (myContentPreprocessor != null) {
      final List<LogFragment> fragments = myContentPreprocessor.parseLogLine(text + "\n");
      myOriginalDocument = getOriginalDocument();
      for (LogFragment fragment : fragments) {
        String formattedMessage = myFormatter.formatMessage(fragment.getText());
        myProcessHandler.notifyTextAvailable(formattedMessage, fragment.getOutputType());
        if (myOriginalDocument != null) {
          myOriginalDocument.append(fragment.getText());
        }
      }
    }
    else {
      final LogFilterModel.MyProcessingResult processingResult = myModel.processLine(text);
      if (processingResult.isApplicable()) {
        final Key key = processingResult.getKey();
        if (key != null) {
          final String messagePrefix = processingResult.getMessagePrefix();
          if (messagePrefix != null) {
            String formattedPrefix = myFormatter.formatPrefix(messagePrefix);
            myProcessHandler.notifyTextAvailable(formattedPrefix, key);
          }
          String formattedMessage = myFormatter.formatMessage(text);
          myProcessHandler.notifyTextAvailable(formattedMessage + "\n", key);
        }
      }
      myOriginalDocument = getOriginalDocument();
      if (myOriginalDocument != null) {
        myOriginalDocument.append(text).append("\n");
      }
    }
  }

  public void attachStopLogConsoleTrackingListener(final ProcessHandler process) {
    if (process != null) {
      final ProcessAdapter stopListener = new ProcessAdapter() {
        @Override
        public void processTerminated(@NotNull final ProcessEvent event) {
          process.removeProcessListener(this);
          stopRunning(true);
        }
      };
      process.addProcessListener(stopListener);
    }
  }

  public StringBuffer getOriginalDocument() {
    if (myOriginalDocument == null) {
      final Editor editor = getEditor();
      if (editor != null) {
        myOriginalDocument = new StringBuffer(editor.getDocument().getText());
      }
    } else {
      if (ConsoleBuffer.useCycleBuffer()) {
        resizeBuffer(myOriginalDocument, ConsoleBuffer.getCycleBufferSize());
      }
    }
    return myOriginalDocument;
  }

  static void resizeBuffer(@NotNull StringBuffer buffer, int size) {
    final int toRemove = buffer.length() - size;
    if (toRemove > 0) {

      int indexOfNewline = buffer.indexOf("\n", toRemove);

      if (indexOfNewline == -1) {
        buffer.delete(0, toRemove);
      }
      else {
        buffer.delete(0, indexOfNewline + 1);
      }
    }

  }

  @Nullable
  private Editor getEditor() {
    final ConsoleView console = getConsole();
    return console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
  }

  private void filterConsoleOutput() {
    ApplicationManager.getApplication().invokeLater(() -> computeSelectedLineAndFilter());
  }

  private synchronized void computeSelectedLineAndFilter() {
    // we have to do this in dispatch thread, because ConsoleViewImpl can flush something to document otherwise
    myOriginalDocument = getOriginalDocument();
    if (myOriginalDocument != null) {
      final Editor editor = getEditor();
      LOG.assertTrue(editor != null);
      final Document document = editor.getDocument();
      final int caretOffset = editor.getCaretModel().getOffset();
      myLineUnderSelection = null;
      myLineOffset = -1;
      if (caretOffset > -1 && caretOffset < document.getTextLength()) {
        int line;
        try {
          line = document.getLineNumber(caretOffset);
        }
        catch (IllegalStateException e) {
          throw new IllegalStateException("document.length=" + document.getTextLength() + ", caret offset = " + caretOffset + "; " + e.getMessage(), e);
        }
        if (line > -1 && line < document.getLineCount()) {
          final int startOffset = document.getLineStartOffset(line);
          myLineUnderSelection = document.getText().substring(startOffset, document.getLineEndOffset(line));
          myLineOffset = caretOffset - startOffset;
        }
      }
    }
    ApplicationManager.getApplication().executeOnPooledThread(() -> doFilter());
  }

  private synchronized void doFilter() {
    if (myDisposed) {
      return;
    }
    final ConsoleView console = getConsoleNotNull();
    console.clear();
    myModel.processingStarted();

    final String[] lines = myOriginalDocument != null ? myOriginalDocument.toString().split("\n") : ArrayUtilRt.EMPTY_STRING_ARRAY;
    int offset = 0;
    boolean caretPositioned = false;
    AnsiEscapeDecoder decoder = new AnsiEscapeDecoder();

    for (String line : lines) {
      @SuppressWarnings("CodeBlock2Expr")
      final int printed = printMessageToConsole(line, (text, key) -> {
        decoder.escapeText(text, key, (chunk, attributes) -> {
          console.print(chunk, ConsoleViewContentType.getConsoleViewType(attributes));
        });
      });
      if (printed > 0) {
        if (!caretPositioned) {
          if (Comparing.strEqual(myLineUnderSelection, line)) {
            caretPositioned = true;
            offset += myLineOffset != -1 ? myLineOffset : 0;
          }
          else {
            offset += printed;
          }
        }
      }
    }

    // we need this, because, document can change before actual scrolling, so offset may be already not at the end
    if (caretPositioned) {
      console.scrollTo(offset);
    }
    else {
      ((ConsoleViewImpl)console).requestScrollingToEnd();
    }
  }

  private int printMessageToConsole(@NotNull String line, @NotNull BiConsumer<? super String, ? super Key> printer) {
    if (myContentPreprocessor != null) {
      List<LogFragment> fragments = myContentPreprocessor.parseLogLine(line + '\n');
      for (LogFragment fragment : fragments) {
        printer.accept(myFormatter.formatMessage(fragment.getText()), fragment.getOutputType());
      }
      return line.length() + 1;
    }
    else {
      final LogFilterModel.MyProcessingResult processingResult = myModel.processLine(line);
      if (processingResult.isApplicable()) {
        final Key key = processingResult.getKey();
        if (key != null) {
          final String messagePrefix = processingResult.getMessagePrefix();
          if (messagePrefix != null) {
            printer.accept(myFormatter.formatPrefix(messagePrefix), key);
          }
          printer.accept(myFormatter.formatMessage(line) + "\n", key);
          return (messagePrefix != null ? messagePrefix.length() : 0) + line.length() + 1;
        }
      }
      return 0;
    }
  }

  @Nullable
  public synchronized ConsoleView getConsole() {
    return myConsole;
  }

  /**
   * A shortcut for "getConsole()+assert console != null"
   * Use this method when you are sure that console must not be null.
   * If we get the assertion then it is a time to revisit logic of caller ;)
   */

  @NotNull
  private synchronized ConsoleView getConsoleNotNull() {
    final ConsoleView console = getConsole();
    assert console != null: "it looks like console has been disposed";
    return console;
  }

  @Override
  public ActionGroup getToolbarActions() {
    return getOrCreateActions();
  }

  @Override
  public String getToolbarPlace() {
    return ActionPlaces.UNKNOWN;
  }

  @Override
  @Nullable
  public JComponent getToolbarContextComponent() {
    final ConsoleView console = getConsole();
    return console == null ? null : console.getComponent();
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return getConsoleNotNull().getPreferredFocusableComponent();
  }

  public String getTitle() {
    return myTitle;
  }

  public synchronized void clear() {
    getConsoleNotNull().clear();
    myOriginalDocument = null;
  }

  @Override
  public JComponent getSearchComponent() {
    myLogFilterCombo.setModel(new DefaultComboBoxModel(myFilters.toArray(new LogFilter[0])));
    resetLogFilter();
    myLogFilterCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final LogFilter filter = (LogFilter)myLogFilterCombo.getSelectedItem();
        final Task.Backgroundable task = new Task.Backgroundable(myProject, APPLYING_FILTER_TITLE) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            myModel.selectFilter(filter);
          }
        };
        ProgressManager.getInstance().run(task);
      }
    });
    AccessibleContextUtil.setName(myLogFilterCombo, "Message severity filter");
    myTextFilterWrapper.removeAll();
    myTextFilterWrapper.add(getTextFilterComponent());
    return mySearchComponent;
  }

  private void resetLogFilter() {
    for (LogFilter filter : myFilters) {
      if (myModel.isFilterSelected(filter)) {
        if (myLogFilterCombo.getSelectedItem() != filter) {
          myLogFilterCombo.setSelectedItem(filter);
          break;
        }
      }
    }
  }

  @NotNull
  protected Component getTextFilterComponent() {
    return myFilter;
  }

  @Override
  public boolean isContentBuiltIn() {
    return myBuildInActions;
  }

  public void writeToConsole(String text, Key outputType) {
    myProcessHandler.notifyTextAvailable(text, outputType);
  }

  public void addListener(LogConsoleListener listener) {
    myListeners.add(listener);
  }

  private void fireLoggingWillBeStopped() {
    for (LogConsoleListener listener : myListeners) {
      listener.loggingWillBeStopped();
    }
  }

  private static class LightProcessHandler extends ProcessHandler {

    private final AnsiEscapeDecoder myDecoder = new AnsiEscapeDecoder();

    @Override
    public void notifyTextAvailable(@NotNull String text, @NotNull Key outputType) {
      myDecoder.escapeText(text, outputType, (chunk, attributes) -> super.notifyTextAvailable(chunk, attributes));
    }

    @Override
    protected void destroyProcessImpl() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected void detachProcessImpl() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean detachIsDefault() {
      return false;
    }

    @Override
    @Nullable
    public OutputStream getProcessInput() {
      return null;
    }
  }

  private class ReaderThread implements Runnable {
    private BufferedReader myReader;
    private boolean myRunning = false;
    private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, LogConsoleBase.this);

    ReaderThread(@Nullable Reader reader) {
      myReader = reader != null ? new BufferedReader(reader) : null;
    }

    @Override
    public void run() {
      if (myReader == null) return;
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (myRunning) {
            try {

              myReader = updateReaderIfNeeded(myReader);

              int i = 0;
              while (i++ < 1000) {
                final BufferedReader reader = myReader;
                if (myRunning && reader != null && reader.ready()) {
                  addMessage(reader.readLine());
                }
                else {
                  break;
                }
              }
            }
            catch (IOException e) {
              LOG.info(e);
              addMessage("I/O Error" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
              return;
            }
          }
          if (myAlarm.isDisposed()) return;
          myAlarm.addRequest(this, 100);
        }
      };
      if (myAlarm.isDisposed()) return;
      myAlarm.addRequest(runnable, 10);
    }

    public void startRunning() {
      myRunning = true;
    }

    public void stopRunning() {
      myRunning = false;
      synchronized (this) {
        notifyAll();
      }
    }
  }
}
